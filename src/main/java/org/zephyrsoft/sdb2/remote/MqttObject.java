/*
 * This file is part of the Song Database (SDB).
 *
 * SDB is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License 3.0 as published by
 * the Free Software Foundation.
 *
 * SDB is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License 3.0 for more details.
 *
 * You should have received a copy of the GNU General Public License 3.0
 * along with SDB. If not, see <http://www.gnu.org/licenses/>.
 */
package org.zephyrsoft.sdb2.remote;

import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiPredicate;
import java.util.function.Function;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttTopic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MqttObject<T> {
	
	private static final Logger LOG = LoggerFactory.getLogger(MqttObject.class);

	private final Integer[] wildcardPositions;
	private T object;
    private final String subscriptionTopic;
    private final String publishTopic;
    private final int qos;
    private final boolean retained;
	private MQTT mqtt;
    private final Function<String, T> toObject;
    private final Function<T, String> toString;
    private final BiPredicate<T, T> takeObject;
    private final BiPredicate<T, T> objectEquals;
    private final CopyOnWriteArrayList<OnChangeListener<T>> onChangeListeners = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<OnChangeListener<T>> onRemoteChangeListeners = new CopyOnWriteArrayList<>();
    private static final String TAG = MQTT.class.getName();
    private boolean setDirect = false;
    private final MQTT.OnMessageListener onMessageListener;
	
	/**
	 * A simple MQTT based property for synchronizing java objects.
     * <p>
     * It can be used for Publish or Subscribe only, Publish&Subscribe on the same topic, or Publish&Subscribe on
     * separate topics.
     * <p>
     * A initially set object which is not null will be published before subscribe is called. Null objects are
     * supported,
     * if toObject, takeObject, toString and objectEquals are supporting it. Use case might be a retained message which
     * can be null, if not set. toString must create a empty String to unset it. Attention: subcribers may not get the
     * empty string.
     *
     * @param mqtt
     * @param subscriptionTopic
     * @param toObject
     * @param toString
     * @param qos
     * @param retained
     * @param objectEquals
     * @throws MqttException
     */
    public MqttObject(MQTT mqtt,
                      String subscriptionTopic,
                      Function<String, T> toObject,
                      Function<T, String> toString,
                      int qos,
                      boolean retained,
                      BiPredicate<T, T> objectEquals) throws MqttException {
        this(mqtt,
                null,
                subscriptionTopic,
                toObject,
                null,
                null,
                toString,
                qos,
                retained,
                objectEquals,
                false);
    }

    /**
     * A simple MQTT based property for synchronizing java objects.
     * <p>
	 * It can be used for Publish or Subscribe only, Publish&Subscribe on the same topic, or Publish&Subscribe on
	 * separate topics.
     * <p>
	 * A initially set object which is not null will be published before subscribe is called. Null objects are
	 * supported,
	 * if toObject, takeObject, toString and objectEquals are supporting it. Use case might be a retained message which
	 * can be null, if not set. toString must create a empty String to unset it. Attention: subcribers may not get the
	 * empty string.
	 *
     * @param mqtt
     * @param publishTopic
     * @param qos
     * @param retained
	 * @throws MqttException
	 */
    public MqttObject(MQTT mqtt,
                      String publishTopic,
                      Function<T, String> toString,
                      int qos,
                      boolean retained) throws MqttException {
        this(mqtt,
                null,
                null,
                null,
                null,
                publishTopic,
                toString,
                qos,
                retained,
                null,
                false);
	}
	
	/**
	 * A simple MQTT based property for synchronizing java objects.
     * <p>
	 * It can be used for Publish or Subscribe only, Publish&Subscribe on the same topic, or Publish&Subscribe on
	 * separate topics.
     * <p>
	 * A initially set object which is not null will be published before subscribe is called. Null objects are
	 * supported,
	 * if toObject, takeObject, toString and objectEquals are supporting it. Use case might be a retained message which
	 * can be null, if not set. toString must create a empty String to unset it. Attention: subcribers may not get the
	 * empty string.
	 *
	 * @throws MqttException
	 */
	public MqttObject(MQTT mqtt,
					  T object,
					  String subscriptionTopic,
					  Function<String, T> toObject,
					  BiPredicate<T, T> takeObject,
					  String publishTopic,
					  Function<T, String> toString,
					  int qos,
					  boolean retained,
					  BiPredicate<T, T> objectEquals,
					  boolean setDirect) throws MqttException {
		LOG.trace("new MqttObject: S: {} P: {}", subscriptionTopic, publishTopic);
		this.subscriptionTopic = subscriptionTopic;
		this.toObject = toObject;
		this.takeObject = takeObject;
        this.toString = toString != null ? toString : Object::toString;
		this.qos = qos;
		this.retained = retained;
        this.objectEquals = objectEquals != null ? objectEquals : (a, b) -> a == null ? b == null : a.equals(b);
		this.object = object;
        this.setDirect = setDirect;

        if (this.subscriptionTopic != null && (this.subscriptionTopic.contains("+") || this.subscriptionTopic.contains("#"))) {
            String[] subTopicSplit = this.subscriptionTopic.split("/");
            ArrayList<Integer> wildcardPositions = new ArrayList<>(subTopicSplit.length);
            for (int i = 0; i < subTopicSplit.length; i++) {
                if (subTopicSplit[i].equals("+") || subTopicSplit[i].equals("#"))
                    wildcardPositions.add(i);
            }
            this.wildcardPositions = wildcardPositions.toArray(new Integer[0]);
        } else {
            wildcardPositions = new Integer[]{};
        }

        publishTopic = publishTopic != null ? publishTopic : this.subscriptionTopic;
        if (publishTopic.contains("+") || publishTopic.contains("#")) {
            String[] pubTopicSplit = publishTopic.split("/");
            for (int i = 0; i < pubTopicSplit.length; i++) {
                if (pubTopicSplit[i].equals("+") || pubTopicSplit[i].equals("#"))
                    pubTopicSplit[i] = "%s";
            }
            publishTopic = String.join("/", pubTopicSplit);
        }
        this.publishTopic = publishTopic;

        onMessageListener = (topic, message) -> {
            if (this.subscriptionTopic != null && MqttTopic.isMatched(this.subscriptionTopic, topic)) {
                T newObject = this.toObject == null ? (T) message : this.toObject.apply(message);
                if (this.takeObject != null && !this.takeObject.test(this.object, newObject))
                    return;
                set(newObject, true, (Object[]) getArgsFromTopic(topic));
            }
        };
		
		connectTo(mqtt);
	}
	
	public void connectTo(MQTT pMqtt) throws MqttException {
		mqtt = pMqtt;
		if (object != null)
			publish();
		subscribe();
	}
	
    public void set(T pObject, Object... args) {
        set(pObject, false, args);
	}
	
    private void set(T pObject, boolean fromRemote, Object... args) {
		if (pObject == object || objectEquals.test(pObject, object))
			return;
		if (!fromRemote && !setDirect) {
            publishChange(pObject, args);
		} else {
			object = pObject;
            onChangeListeners.forEach((ocl) -> ocl.onChange(pObject, args));
			if (fromRemote)
                onRemoteChangeListeners.forEach((ocl) -> ocl.onChange(pObject, args));
			else
                publish(args);
		}
	}
	
    public String[] getArgsFromTopic(String topic) {
        String[] topicSplit = topic.split("/");
        String[] args = new String[wildcardPositions.length];
        for (int i = 0; i < wildcardPositions.length; i++)
            args[i] = topicSplit[wildcardPositions[i]];
        return args;
    }
	public T get() {
		return object;
	}
	
    private void publish(Object... args) {
        publishChange(object, args);
	}
	
    private void publishChange(T pObject, Object... args) {
        if (publishTopic != null) {
            new Thread(() -> {
                mqtt.publish(String.format(publishTopic, args),
                        toString.apply(pObject),
                        qos,
                        retained);
            }).start();
        }
	}
	
	private void subscribe() throws MqttException {
		if (subscriptionTopic != null) {
			mqtt.onMessage(onMessageListener);
			mqtt.subscribe(subscriptionTopic, qos);
		}
	}
	
    public void removeOnChangeListener(OnChangeListener<T> onChangeListener) {
        onChangeListeners.remove(onChangeListener);
    }
	public void onChange(OnChangeListener<T> onChangeListener) {
		onChangeListeners.add(onChangeListener);
	}
	
    public void removeOnRemoteChangeListener(OnChangeListener<T> onChangeListener) {
        onRemoteChangeListeners.remove(onChangeListener);
    }
	public void onRemoteChange(OnChangeListener<T> onChangeListener) {
		onRemoteChangeListeners.add(onChangeListener);
	}
	
	public interface OnChangeListener<T> {
        void onChange(T object, Object... args);
	}
	
}

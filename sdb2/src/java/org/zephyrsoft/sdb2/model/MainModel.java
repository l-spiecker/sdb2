package org.zephyrsoft.sdb2.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamImplicit;
import com.thoughtworks.xstream.annotations.XStreamOmitField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zephyrsoft.sdb2.gui.MainWindow;
import org.zephyrsoft.sdb2.gui.TransparentListModel;

/**
 * Model for {@link MainWindow}.
 * 
 * @author Mathis Dirksen-Thedens
 */
@XStreamAlias("songs")
public class MainModel implements Iterable<Song>, Serializable {
	
	private static final long serialVersionUID = -2503516988752281994L;
	
	private static Logger LOG = LoggerFactory.getLogger(MainModel.class);
	
	@XStreamImplicit(itemFieldName = "song")
	private List<Song> songs = null;
	
	@XStreamOmitField
	private List<TransparentListModel<Song>> createdListModels = null;
	
	// TODO add list of observers + possibility to add/delete observing objects?
	// ===> also see Observable or PropertyChangeSupport
	
	/**
	 * Is called from {@link XMLConverter} and {@link MainWindow} to ensure a valid inner state after conversion from
	 * XML and after creation via constructor. This is in this method because XStream might overwrite the value set
	 * inside the constructor with {@code null}.
	 */
	public void initIfNecessary() {
		if (songs == null) {
			songs = new ArrayList<Song>();
		}
		if (createdListModels == null) {
			createdListModels = new ArrayList<TransparentListModel<Song>>();
		}
	}
	
	public TransparentListModel<Song> getListModel() {
		TransparentListModel<Song> createdListModel = new TransparentListModel<Song>(songs);
		createdListModels.add(createdListModel);
		return createdListModel;
	}
	
	public int getSize() {
		return songs.size();
	}
	
	public boolean isEmpty() {
		return songs.isEmpty();
	}
	
	public boolean addSong(Song e) {
		boolean b = songs.add(e);
		notifyListModelListeners();
		return b;
	}
	
	public boolean removeSong(Song o) {
		boolean b = songs.remove(o);
		notifyListModelListeners();
		return b;
	}
	
	private void notifyListModelListeners() {
		LOG.debug("notifyListModelListeners");
		for (TransparentListModel<Song> model : createdListModels) {
			ListDataListener[] listeners = model.getListDataListeners();
			for (ListDataListener listener : listeners) {
				listener.contentsChanged(new ListDataEvent(this, ListDataEvent.CONTENTS_CHANGED, 0, songs.size() - 1));
			}
		}
	}
	
	public Song getSong(int index) {
		return songs.get(index);
	}
	
	@Override
	public Iterator<Song> iterator() {
		return songs.iterator();
	}
	
}

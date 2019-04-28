package org.zephyrsoft.sdb2.util;

import static org.junit.Assert.assertEquals;

import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import org.junit.Test;

import com.itextpdf.text.Font;
import com.itextpdf.text.pdf.BaseFont;

public class ChordSpaceCorrectorTest {
	
	private static final String CHORDS_INPUT = "A       B            C            D                    Em              F     A     F A";
	private static final String TEXT_INPUT = "This is a Test which only should demonstrate that the service works as expected.";
	private static final String EXPECTED_OUTPUT_PDF = "A        B                   C                   D                              Em                    F        A  F A";
	private static final String EXPECTED_OUTPUT_PRESENTATION = "A        B                   C                   D                                Em                    F         A  F A";
	
	@Test
	public void correctChordSpacesForPdf() throws Exception {
		BaseFont baseFont = BaseFont.createFont(BaseFont.TIMES_ROMAN, BaseFont.WINANSI, BaseFont.EMBEDDED);
		Font lyricsFont = new Font(baseFont, 12);
		ChordSpaceCorrector chordSpaceCorrector = new ChordSpaceCorrector(
			text -> (int) lyricsFont.getBaseFont().getWidthPointKerned(text, lyricsFont.getSize()));
		
		String result = chordSpaceCorrector.correctChordSpaces(CHORDS_INPUT, TEXT_INPUT);
		assertEquals(EXPECTED_OUTPUT_PDF, result);
	}
	
	@Test
	public void correctChordSpacesForPresentation() {
		java.awt.Font lyricsFont = new java.awt.Font("Dialog", java.awt.Font.BOLD + java.awt.Font.ITALIC, 36);
		
		BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = img.createGraphics();
		FontMetrics fm = g.getFontMetrics(lyricsFont);
		
		ChordSpaceCorrector chordSpaceCorrector = new ChordSpaceCorrector(text -> fm.stringWidth(text));
		String result = chordSpaceCorrector.correctChordSpaces(CHORDS_INPUT, TEXT_INPUT);
		assertEquals(EXPECTED_OUTPUT_PRESENTATION, result);
	}
	
}

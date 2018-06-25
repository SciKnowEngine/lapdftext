package edu.isi.bmkeg.lapdf.xml.model;

import java.io.Serializable;
import java.util.Map;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import edu.isi.bmkeg.lapdf.model.WordBlock;

@XmlRootElement(name="word")
public class LapdftextXMLWord extends LapdftextXMLRectangle implements Serializable {
	static final long serialVersionUID = 8047039304729208683L;

	private String t;

	private String font;

	private int fId;

	private int sId;

	public LapdftextXMLWord() throws Exception {}
	
	public LapdftextXMLWord(WordBlock word, int id, Map<String, Integer> fontStyles) throws Exception {
		
		if( word.getWord() != null ) {
			this.setT(word.getWord());							
		} else {
			throw new Exception("Can't generate Word XML if words are not set.");
		}
		
		this.setId( id++ );
		this.setW( word.getX2() - word.getX1() );
		this.setH( word.getY2() - word.getY1() );
		this.setX( word.getX1() );
		this.setY( word.getY1() );
		this.setI( word.getOrder() );
		
		int fsCount = fontStyles.size();
		if( !fontStyles.containsKey( word.getFont() ) ) {
			fontStyles.put(word.getFont(), fsCount++);
		} 
		this.setfId(fontStyles.get( word.getFont() ) );
							
		if( !fontStyles.containsKey( word.getFontStyle() ) ) {
			fontStyles.put(word.getFontStyle(), fsCount++);
		} 
		this.setsId(fontStyles.get( word.getFontStyle() ) );
		
		
	}
	

	

	@XmlAttribute	
	public String getT() {
		return t;
	}

	public void setT(String t) {
		this.t = t;
	}

	@XmlAttribute	
	public int getfId() {
		return fId;
	}

	public void setfId(int fId) {
		this.fId = fId;
	}

	@XmlAttribute	
	public int getsId() {
		return sId;
	}

	public void setsId(int sId) {
		this.sId = sId;
	}

	@XmlAttribute	
	public String getFont() {
		return font;
	}

	public void setFont(String font) {
		this.font = font;
	}
	
	public String toString() {
		return this.t;
	}
	
}

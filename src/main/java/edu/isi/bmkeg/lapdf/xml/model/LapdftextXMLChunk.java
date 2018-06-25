package edu.isi.bmkeg.lapdf.xml.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

import edu.isi.bmkeg.lapdf.model.ChunkBlock;
import edu.isi.bmkeg.lapdf.model.PageBlock;
import edu.isi.bmkeg.lapdf.model.WordBlock;
import edu.isi.bmkeg.lapdf.model.ordering.SpatialOrdering;
import edu.isi.bmkeg.lapdf.model.spatial.SpatialEntity;

@XmlRootElement(name="chunk")
public class LapdftextXMLChunk extends LapdftextXMLRectangle implements Serializable {
	static final long serialVersionUID = 8047039304729208683L;

	private String font;
	private int fontSize;
	private String type;
	private List<LapdftextXMLWord> words = new ArrayList<LapdftextXMLWord>();

	public LapdftextXMLChunk() {}
	
	public LapdftextXMLChunk(ChunkBlock chunk, int id, Map<String, Integer> fontStyles ) throws Exception {
		
		if( chunk.getChunkType() != null )
			this.setType( chunk.getChunkType() );
		
		this.setFont( chunk.getMostPopularWordFont() );
		this.setFont( chunk.getMostPopularWordFont() );
		this.setFontSize( chunk.getMostPopularWordHeight() );
	
		this.setId( id++ );
		this.setW( chunk.getX2() - chunk.getX1() );
		this.setH( chunk.getY2() - chunk.getY1() );
		this.setX( chunk.getX1() );
		this.setY( chunk.getY1() );
		this.setI( chunk.getOrder() );
		
		List<SpatialEntity> wbList = ((PageBlock) chunk.getPage()).containsByType(chunk,
				SpatialOrdering.ORIGINAL_MODE, 
				WordBlock.class);
		if( wbList != null ) {					
			for( SpatialEntity se : wbList ) {
				WordBlock word = (WordBlock) se;

				LapdftextXMLWord xmlWord = new LapdftextXMLWord(word, id, fontStyles);
				this.getWords().add( xmlWord ); 
			}
			
		}
		
	}
	
	
	@XmlAttribute
	public String getFont() {
		return font;
	}

	public void setFont(String font) {
		this.font = font;
	}

	@XmlAttribute
	public int getFontSize() {
		return fontSize;
	}

	public void setFontSize(int fontSize) {
		this.fontSize = fontSize;
	}

	@XmlAttribute
	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	@XmlElementWrapper( name="words" )
    @XmlElement( name="wd" )
	public List<LapdftextXMLWord> getWords() {
		return words;
	}

	public void setWords(List<LapdftextXMLWord> words) {
		this.words = words;
	}
	
	public String toString() {
		String s = "";
		for( LapdftextXMLWord word : this.words ) {
			if( s.length() > 0 )
				s += " ";
			s += word.getT();
		}
		return s;
	}

}

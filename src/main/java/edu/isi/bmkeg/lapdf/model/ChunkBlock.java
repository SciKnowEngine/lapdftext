package edu.isi.bmkeg.lapdf.model;

import java.util.List;

import edu.isi.bmkeg.lapdf.extraction.exceptions.InvalidPopularSpaceValueException;
import edu.isi.bmkeg.lapdf.model.RTree.RTChunkBlock;
import edu.isi.bmkeg.lapdf.model.spatial.SpatialEntity;

/**
 * A contiguous block of text that sits within a PageBlock object and contains
 * a list of WordBlock objects.
 * 
 * @author burns
 */
public interface ChunkBlock extends Block, SpatialEntity {

	public static final String TYPE_TITLE = "title";
	public static final String TYPE_AUTHORS = "authors";
	public static final String TYPE_BODY = "body";
	public static final String TYPE_HEADING = "heading";
	public static final String TYPE_SUBTITLE = "subtitle";
	public static final String TYPE_FIGURE_LEGEND = "figureLegend";
	public static final String TYPE_FIGURE = "figure";
	public static final String TYPE_TABLE = "table";
	public static final String TYPE_TABLE_LEGEND = "table";
	public static final String TYPE_UNCLASSIFIED = "type_unclassified";
	public static final String TYPE_PAGE = "page";
	public static final String TYPE_HEADER = "header";
	public static final String TYPE_FOOTER = "footer";
	public static final String TYPE_KEYWORDS = "keywords";
	public static final String TYPE_CITATION = "citation";
	public static final String TYPE_ABSTRACT = "abstract";
	
	public static final String SECTION_METHODS = "methods";
	public static final String SECTION_RESULTS = "results";
	public static final String SECTION_REFERENCES = "references";
	public static final String SECTION_DISCUSSION = "discussion";
	public static final String SECTION_CONCLUSIONS = "conclusions";
	public static final String SECTION_ACKNOWLEDGEMENTS = "acknowledgements";
	public static final String SECTION_AFFLIATION = "affliation";
	public static final String SECTION_INTRODUCTION = "introduction";
	public static final String SECTION_SUPPORTING_INFORMATION = "supportingInformation";
	public static final String SECTION_UNCLASSIFIED = "section_unclassified";

	public static final String CENTER = "center";
	public static final String NORTH = "north";
	public static final String SOUTH = "south";
	public static final String EAST = "east";
	public static final String WEST = "west";
	public static final String NORTH_SOUTH = "north/south";
	public static final String EAST_WEST = "east/west";
	
	public int getMostPopularWordHeight();

	public int getMostPopularWordSpaceWidth();

	public String getMostPopularWordFont();

	public String getMostPopularWordStyle();

	public void setMostPopularWordHeight(int height);

	public void setMostPopularWordSpaceWidth(int spaceWidth);

	public void setMostPopularWordStyle(String style);

	public void setMostPopularWordFont(String font);

	public int readNumberOfLine();

	public String readChunkText();

	public Boolean isHeaderOrFooter();

	public void setHeaderOrFooter(boolean headerOrFooter);

	public ChunkBlock readLastChunkBlock();

	boolean isMatchingRegularExpression(String regex);

	boolean isUnderOneLineFlushNeighboursOfType(String type);

	boolean hasNeighboursOfType(String type, String nsew);

	List<WordBlock> getRotatedWords();

	void setRotatedWords(List<WordBlock> rotatedWords);

	public double readDensity();

	public void setChunkType(String type);

	public String getChunkType();

	public void setChunkSection(String section);

	public String getChunkSection();
	
	public boolean isMostPopularFontInDocument();
	public boolean isNextMostPopularFontInDocument();
	public int getHeightDifferenceBetweenChunkWordAndDocumentWord();	
	public boolean isAlignedLeft();
	public boolean isInTopHalf();
	public int getMostPopularFontSize();
	public boolean isAlignedRight();
	public boolean isAlignedMiddle();
	public boolean isAllCapitals();
	public boolean isMostPopularFontModifierBold();
	public boolean isMostPopularFontModifierItalic();
	public boolean isContainingFirstLineOfPage();
	public boolean isContainingLastLineOfPage();
	public boolean isOutlier();
	public int getChunkTextLength();
	public double getDensity();
	public boolean isAlignedWithColumnBoundaries();
	public String getlastClassification();
	public String getSection() throws InvalidPopularSpaceValueException;
	public int getPageNumber();
	public boolean isColumnCentered();
	public boolean isWithinBodyTextFrame();
	public boolean isTallerThanWide();
		
	
}

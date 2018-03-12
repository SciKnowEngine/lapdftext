package edu.isi.bmkeg.lapdf.model.RTree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.isi.bmkeg.lapdf.extraction.exceptions.InvalidPopularSpaceValueException;
import edu.isi.bmkeg.lapdf.model.Block;
import edu.isi.bmkeg.lapdf.model.ChunkBlock;
import edu.isi.bmkeg.lapdf.model.LapdfDocument;
import edu.isi.bmkeg.lapdf.model.PageBlock;
import edu.isi.bmkeg.lapdf.model.WordBlock;
import edu.isi.bmkeg.lapdf.model.ordering.SpatialOrdering;
import edu.isi.bmkeg.lapdf.model.spatial.SpatialEntity;

public class RTChunkBlock extends RTSpatialEntity implements ChunkBlock {

	private static final long serialVersionUID = 1L;

	private Block container;
	private int mostPopularWordHeight;
	private int mostPopularWordSpaceWidth;
	private String mostPopularWordFont = "";
	private String mostPopularWordStyle = "";

	private String alignment = null;
	private String type = ChunkBlock.TYPE_UNCLASSIFIED;
	private String paperSection = ChunkBlock.SECTION_UNCLASSIFIED;
	private Boolean headerOrFooter = null;

	private double density = -1;

	private List<WordBlock> rotatedWords = new ArrayList<WordBlock>();
	
	private static Pattern geneticPatt = Pattern.compile("^[AGCTU]+$");

	public RTChunkBlock() {
		super();
	}

	public RTChunkBlock(int x1, int y1, int x2, int y2, int order) {
		super(x1, y1, x2, y2, order);
	}

	@Override
	public int getId() {
		return super.getId();
	}

	@Override
	public Block getContainer() {
		return container;
	}

	@Override
	public int getMostPopularWordHeight() {

		return mostPopularWordHeight;
	}

	public int getMostPopularWordSpaceWidth() {
		return mostPopularWordSpaceWidth;
	}

	public void setMostPopularWordSpaceWidth(int mostPopularWordSpaceWidth) {
		this.mostPopularWordSpaceWidth = mostPopularWordSpaceWidth;
	}

	public String getMostPopularWordFont() {
		return mostPopularWordFont;
	}

	public void setMostPopularWordFont(String mostPopularWordFont) {
		this.mostPopularWordFont = mostPopularWordFont;
	}

	public void setMostPopularWordHeight(int height) {
		this.mostPopularWordHeight = height;
	}

	@Override
	public String getMostPopularWordStyle() {
		return mostPopularWordStyle;
	}

	@Override
	public void setMostPopularWordStyle(String style) {
		this.mostPopularWordStyle = style;
	}

	@Override
	public Boolean isHeaderOrFooter() {
		return headerOrFooter;
	}

	@Override
	public void setHeaderOrFooter(boolean headerOrFooter) {
		this.headerOrFooter = headerOrFooter;
	}

	@Override
	public void setContainer(Block block) {
		this.container = (PageBlock) block;
	}

	@Override
	public PageBlock getPage() {
		return (PageBlock) this.container;
	}

	@Override
	public void setPage(PageBlock page) {
		this.container = page;
	}

	@Override
	public String getChunkType() {
		return type;
	}

	@Override
	public void setChunkType(String type) {
		this.type = type;
	}

	public List<WordBlock> getRotatedWords() {
		return rotatedWords;
	}

	public void setRotatedWords(List<WordBlock> rotatedWords) {
		this.rotatedWords = rotatedWords;
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public String readLeftRightMidLine() {

		if (this.alignment != null)
			return this.alignment;

		PageBlock page = (PageBlock) this.getContainer();
		int median = page.getMedian();
		int X1 = this.getX1();
		int width = this.getWidth();

		if (X1 + width < median) {

			this.alignment = LEFT;

		} else if (X1 > median) {

			this.alignment = RIGHT;

		} else {

			/*
			 * int left = median - X1; int right = X1 + width - median;
			 * 
			 * double leftIsToRight = (double) left / (double) right; double
			 * rightIsToLeft = (double) right / (double) left; if (leftIsToRight
			 * < 0.05) this.alignment = RIGHT; else if (rightIsToLeft < 0.05)
			 * this.alignment = LEFT; else
			 */
			this.alignment = MIDLINE;
		}

		return this.alignment;

	}

	public boolean isFlush(String condition, int value) {
		PageBlock parent = (PageBlock) this.getContainer();
		int median = parent.getMedian();
		String leftRightMidline = this.readLeftRightMidLine();

		int x1 = this.getX1();
		int x2 = this.getX2();
		int marginX1 = parent.getMargin()[0];
		int marginX2 = parent.getMargin()[3];

		if (condition.equals(MIDLINE)) {
			if (leftRightMidline.equals(MIDLINE))
				return false;
			else if (leftRightMidline.equals(LEFT) && Math.abs(x2 - median) < value)
				return true;
			else if (leftRightMidline.equals(RIGHT) && Math.abs(x1 - median) < value)
				return true;
		} else if (condition.equals(LEFT)) {
			if (leftRightMidline.equals(MIDLINE) && Math.abs(x1 - marginX1) < value)
				return true;
			else if (leftRightMidline.equals(LEFT) && Math.abs(x1 - marginX1) < value)
				return true;
			else if (leftRightMidline.equals(RIGHT))
				return false;
		} else if (condition.equals(RIGHT)) {
			if (leftRightMidline.equals(MIDLINE) && Math.abs(x2 - marginX2) < value)
				return true;
			else if (leftRightMidline.equals(LEFT))
				return false;
			else if (leftRightMidline.equals(RIGHT) && Math.abs(x2 - marginX2) < value)
				return true;
		}
		return false;
	}

	@Override
	public int readNumberOfLine() {
		PageBlock parent = (PageBlock) this.container;
		List<SpatialEntity> wordBlockList = parent.containsByType(this, SpatialOrdering.MIXED_MODE, WordBlock.class);
		if (wordBlockList.size() == 0)
			return 0;
		WordBlock block = (WordBlock) wordBlockList.get(0);
		int numberOfLines = 1;
		int lastY = block.getY1() + block.getHeight() / 2;
		int currentY = lastY;
		for (SpatialEntity entity : wordBlockList) {
			lastY = currentY;
			block = (WordBlock) entity;
			currentY = block.getY1() + block.getHeight() / 2;
			if (currentY > lastY + block.getHeight() / 2)
				numberOfLines++;

		}
		return numberOfLines;
	}

	@Override
	public String readChunkText() {

		List<SpatialEntity> wordBlockList = ((PageBlock) container).containsByType(this, SpatialOrdering.MIXED_MODE,
				WordBlock.class);

		StringBuilder builder = new StringBuilder();
		for (SpatialEntity entity : wordBlockList) {
			builder.append(((WordBlock) entity).getWord());

			if (((WordBlock) entity).getWord() == null)
				continue;

			if (!((WordBlock) entity).getWord().endsWith("-"))
				builder.append(" ");

		}

		return builder.toString().trim();

	}

	@Override
	public ChunkBlock readLastChunkBlock() {

		List<ChunkBlock> sortedChunkBlockList = ((PageBlock) this.getContainer())
				.getAllChunkBlocks(SpatialOrdering.MIXED_MODE);

		int index = Collections.binarySearch(sortedChunkBlockList, this,
				new SpatialOrdering(SpatialOrdering.MIXED_MODE));

		return (index <= 0) ? null : sortedChunkBlockList.get(index - 1);
	}

	/**
	 * returns true if the chunk block contains text that matches the input
	 * regex
	 * 
	 * @param regex
	 * @return
	 */
	@Override
	public boolean isMatchingRegularExpression(String regex) {
		Pattern pattern = Pattern.compile(regex);

		String text = this.readChunkText();
		Matcher matcher = pattern.matcher(text);

		if (matcher.find())
			return true;

		return false;
	}

	/**
	 * returns true if chunk block has neighbors of specific type within
	 * specified distance
	 * 
	 * @param type
	 * @param nsew
	 * @return
	 */
	@Override
	public boolean hasNeighboursOfType(String type, String nsew) {

		List<ChunkBlock> list = getOverlappingNeighbors(nsew, (PageBlock) this.getContainer(), (ChunkBlock) this);

		for (ChunkBlock chunky : list)
			if (chunky.getChunkType().equalsIgnoreCase(type))
				return true;

		return false;

	}

	@Override
	public boolean isUnderOneLineFlushNeighboursOfType(String type) {

		List<ChunkBlock> list = getOverlappingNeighbors(ChunkBlock.NORTH, (PageBlock) this.getContainer(),
				(ChunkBlock) this);

		double threshold = this.getMostPopularWordHeight() * 2;

		for (ChunkBlock chunky : list) {

			int delta1 = Math.abs(chunky.getX1() - this.getX1());
			int delta2 = Math.abs(chunky.getX2() - this.getX2());

			if (delta1 < threshold && delta2 < threshold && chunky.readNumberOfLine() == 1
					&& chunky.getChunkType().equalsIgnoreCase(type)) {
				return true;
			}
		}

		return false;

	}

	public List<ChunkBlock> getOverlappingNeighbors(String nsew, PageBlock parent, ChunkBlock chunkBlock) {

		int topX = chunkBlock.getX1();
		int topY = chunkBlock.getY1();
		int width = chunkBlock.getWidth();
		int height = chunkBlock.getHeight();

		if (nsew == ChunkBlock.NORTH) {
			height = height / 2;
			topY = topY - height;
		} else if (nsew == ChunkBlock.SOUTH) {
			topY = topY + height;
			height = height / 2;
		} else if (nsew == ChunkBlock.EAST) {
			topX = topX + width;
			width = width / 2;
		} else if (nsew == ChunkBlock.WEST) {
			width = width / 2;
			topX = topX - width;
		} else if (nsew == ChunkBlock.NORTH_SOUTH) {
			topY = topY - height / 2;
			height = height * 2;
		} else if (nsew == ChunkBlock.EAST_WEST) {
			topX = topX - width / 2;
			width = width * 2;

		}

		SpatialEntity entity = new RTChunkBlock(topX, topY, topX + width, topY + height, -1);

		List<ChunkBlock> l = new ArrayList<ChunkBlock>();
		Iterator<SpatialEntity> it = parent.intersectsByType(entity, null, ChunkBlock.class).iterator();
		while (it.hasNext()) {
			l.add((ChunkBlock) it.next());
		}

		return l;

	}

	public ChunkBlock readNearestNeighborChunkBlock(String nsew) throws Exception {
		return readNearestNeighborChunkBlock(nsew, 0); 
	}

	/**
	 * Iterate reading box in the desired direction nearest dense, long block or
	 * leave the page.
	 * 
	 * @param nsew
	 * @return
	 * @throws Exception
	 */
	public ChunkBlock readNearestNeighborChunkBlock(String nsew, int nWordsMinimum) throws Exception {

		LapdfDocument doc = ((RTPageBlock) this.container).getDocument();
		SpatialEntity frame = doc.getBodyTextFrame();
		int frame_left = frame.getX1();
		int frame_right = frame.getX2();
		int frame_top = frame.getY1();
		int frame_bottom = frame.getY2();
		
		int x = this.getX1();
		int y = this.getY1();
		int w = this.getWidth();
		int h = this.getHeight();
		int dx = 0, dy = 0;

		if (nsew == ChunkBlock.NORTH) {

			h = 10;
			y = y - h - 1;
			dy = -h;

		} else if (nsew == ChunkBlock.SOUTH) {

			y = y + h + 1;
			h = 10;
			dy = h;

		} else if (nsew == ChunkBlock.EAST) {

			x = x + w + 1;
			w = 10;
			dx = w;

		} else if (nsew == ChunkBlock.WEST) {

			w = 10;
			x = x - w - 1;
			dx = -w;

		} else {

			throw new Exception("NearestNeighborChunkBlock only works with North/South/East/West");

		}

		double coverage = computeWordCoverageInTestBlock(x, y, w, h); 

		while (coverage < 0.6 &&
				x >= frame_left - 10 && 
				y >= frame_top - 10 && 
				(x+w) <= frame_right + 10 && 
				(y+h) <= frame_bottom + 10) {
			x += dx;
			y += dy;
			coverage = computeWordCoverageInTestBlock(x, y, w, h);
		}
		
		return new RTChunkBlock(x, y, x+w, y+h, -1);
		
	}
	
	public double computeWordCoverageInTestBlock(ChunkBlock cb, String nsew) {
		return computeWordCoverageInTestBlock(cb.getX1(), cb.getY1(), cb.getWidth(), cb.getHeight());
	}
	
	public double computeWordCoverageInTestBlock(int x, int y, int w, int h) {
		SpatialEntity testBlock = new RTChunkBlock(x, y, x + w, y + h, -1);
		Set<WordBlock> wordsInTestBlock = new HashSet<WordBlock>();
		for( SpatialEntity se : this.getPage().intersectsByType(testBlock, null, WordBlock.class) ) 
			wordsInTestBlock.add((WordBlock) se);
		for( SpatialEntity se : this.getPage().intersectsByType(this, null, WordBlock.class) ) 
			wordsInTestBlock.remove((WordBlock) se);
		int x1,x2,y1,y2,wordArea=0;
		for(WordBlock tw : wordsInTestBlock) {
			
			Matcher m = geneticPatt.matcher(tw.getWord());
			if(m.find())
				continue;
			
			if( tw.getX1() < x ) x1 = x;
			else x1 = tw.getX1();
			
			if(tw.getX2()>x+w) x2 = x+w;
			else x2 = tw.getX2();
				
			if(tw.getY1()<y) y1 = y;
			else y1 = tw.getY1();

			if(tw.getY2()>y+h) y2 = y+h;
			else y2 = tw.getY2();
			
			wordArea += Math.abs(x1-x2)*Math.abs(y2-y1);
		
		}
		int testArea = w * h;	
		
		double coverage = ((double) wordArea) / ((double) testArea); 
		
		if( coverage > 1.0 ) {
			int argh = 0;
		}
		
		return coverage;
				
	}
	
	public ChunkBlock computeWordEdgeForTestBlock(ChunkBlock testBlock) {
		List<SpatialEntity> blocks = this.getPage().intersectsByType(testBlock, null, WordBlock.class); 
		if(blocks.size() == 0)
			return testBlock;
		int x1=10000, x2=-10000, y1=10000, y2=-10000;
		for( SpatialEntity se : blocks) {
			if(se.getX1()<=x1) x1 = se.getX1();
			if(se.getX2()>x2) x2 = se.getX2();
			if(se.getY1()<y1) y1 = se.getY1();
			if(se.getY2()>y2) y2 = se.getY2();		
		}
		
		return new RTChunkBlock(x1, y1, x2, y2, -1);		
	}


	private void removeLowDensity(double density, List<SpatialEntity> list) {
		List<SpatialEntity> toRemove = new ArrayList<SpatialEntity>();
		for (SpatialEntity se : list) {
			ChunkBlock candidate = (ChunkBlock) se;
			if(candidate.readDensity() < density)
				toRemove.add(candidate);
		}
		list.removeAll(toRemove);
	}
	
	@Override
	public double readDensity() {

		if (this.density < 0.0) {

			PageBlock page = (PageBlock) this.getContainer();
			List<SpatialEntity> wordBlockList = page.containsByType(this, SpatialOrdering.MIXED_MODE, WordBlock.class);
			double chunkSize = this.width() * this.height();
			double wordCoverage = 0.0;
			for (int i = 0; i < wordBlockList.size(); i++) {
				WordBlock wordBlock = (WordBlock) wordBlockList.get(i);
				wordCoverage += wordBlock.getHeight() * wordBlock.getWidth();
			}

			this.density = wordCoverage / chunkSize;

		}

		return this.density;

	}

	/*
	 * Need to implement these functions to handle issues with serialization
	 * private void writeObject(java.io.ObjectOutputStream out) throws
	 * IOException private void readObject(java.io.ObjectInputStream in) throws
	 * IOException, ClassNotFoundException; private void readObjectNoData()
	 * throws ObjectStreamException;
	 */

	public List<ChunkBlock> getOverlappingChunks(PageBlock parent) {

		int topX = this.getX1();
		int topY = this.getY1();
		int width = this.getWidth();
		int height = this.getHeight();

		SpatialEntity entity = new RTChunkBlock(topX, topY, topX + width, topY + height, -1);

		List<ChunkBlock> l = new ArrayList<ChunkBlock>();
		Iterator<SpatialEntity> it = parent.intersectsByType(entity, null, ChunkBlock.class).iterator();
		while (it.hasNext()) {
			l.add((ChunkBlock) it.next());
		}

		return l;

	}
	

	public boolean isMostPopularFontInDocument() {

		PageBlock page = (PageBlock) this.getContainer();

		String ds = page.getDocument().getMostPopularFontStyle();
		
		String s = this.getMostPopularWordFont() 
				+ ";" + this.getMostPopularWordStyle();
		
		if( s.equals(ds) )
			return true;
		
		return false;
		
	}	
	
	/**
	 * Note that we screen out the most popular font on the last page 
	 * from this calculation since we expect that to be the font of the 
	 * references.
	 * @return
	 */
	public boolean isNextMostPopularFontInDocument() {
		
		PageBlock page = (PageBlock) this.getContainer();

		String ds = page.getDocument().getNextMostPopularFontStyle();
		
		String s = this.getMostPopularWordFont() 
				+ ";" + this.getMostPopularWordStyle();
		
		if( s.equals(ds) )
			return true;
		
		return false;
		
	}
	
	/**
	 * returns the difference between the most popular font size in the in the current chunk 
	 * and the most popular font size in the document.
	 * @return
	 */
	public int getHeightDifferenceBetweenChunkWordAndDocumentWord() {
		
		PageBlock page = (PageBlock) this.getContainer();

		int i = this.getMostPopularWordHeight();
		int j = page.getDocument().readMostPopularWordHeight();
		
		return (i-j);
	}
	
	/**
	 * returns true if chunk block is left aligned
	 * @return
	 */
	public boolean isAlignedLeft() {
		if (Block.LEFT.equalsIgnoreCase(this.readLeftRightMidLine()))
			return true;
		return false;
	}
	
	/**
	 * returns true if chunk block starts in the top half of the page
	 * @return
	 */
	public boolean isInTopHalf() {
		
		PageBlock page = (PageBlock) this.getContainer();

		// x1, y1, x2, y2
		int top = page.getMargin()[1];
		int bottom = page.getMargin()[3];
		double middle = (top + bottom) / 2.0;
		
		if( this.getY1() < middle )
			return true;
	
		return false;
	
	}	
	
	/**
	 * returns the most popular font size in the chunk block
	 * @return
	 */
	public int getMostPopularFontSize() {
		
		int fontSize = this.getMostPopularWordHeight();
		return fontSize;
		
		/*String fontStyle = chunk.getMostPopularWordStyle();
		if(fontStyle==null)
			return chunk.getMostPopularWordHeight();
		int fontSizeIndex = fontStyle.indexOf("font-size");
		int colonIndex = fontStyle.indexOf(":", fontSizeIndex);
		int ptIndex = fontStyle.indexOf("pt", colonIndex);
		
		return Integer.parseInt(fontStyle.substring(colonIndex + 1, ptIndex));*/
	
	}
	
	/**
	 * returns true if chunk block is right aligned
	 * @return
	 */
	public boolean isAlignedRight() {
	
		if (Block.RIGHT.equalsIgnoreCase(this.readLeftRightMidLine()))
			return true;
		
		return false;
	
	}
	
	/**
	 * returns true if chunk block is center aligned
	 * @return
	 */
	public boolean isAlignedMiddle() {
		if (Block.MIDLINE.equalsIgnoreCase(this.readLeftRightMidLine()))
			return true;
		return false;
	}
	
	/**
	 * returns true if chunk block contains mostly capitalized text
	 * @return
	 */
	public boolean isAllCapitals() {
		String chunkText = this.readChunkText();
		if(chunkText.toUpperCase().equals(chunkText))
			return true;
		else 
			return false;
	}
	
	/**
	 * returns true if chunk block contains mostly bold face text
	 * @return
	 */
	public boolean isMostPopularFontModifierBold() {

		if ((this.getMostPopularWordStyle() != null && this
				.getMostPopularWordStyle().indexOf("Bold") != -1)
				|| (this.getMostPopularWordFont() != null && (this
						.getMostPopularWordFont().indexOf("Bold") != -1 || this
						.getMostPopularWordFont().indexOf("-B") != -1))) {
			return true;
		}
		return false;
	}
	
	/**
	 * returns true if chunk block contains mostly italicized  text
	 * @return
	 */
	public boolean isMostPopularFontModifierItalic() {
		if ((this.getMostPopularWordStyle() != null && this
				.getMostPopularWordStyle().indexOf("Italic") != -1)
				|| (this.getMostPopularWordFont() != null && this
						.getMostPopularWordFont().indexOf("Italic") != -1)) {
			return true;
		}
		return false;
	}
	
	/**
	 * returns true if chunk block contains the first line of a page's text
	 * @return
	 */
	public boolean isContainingFirstLineOfPage() {

		PageBlock page = (PageBlock) this.getContainer();
		
		if (Math.abs(this.getY1() - page.getMargin()[1]) < page
				.getDocument().readMostPopularWordHeight())
			return true;
		else
			return false;
	}

	/**
	 * returns true if chunk block contains the last line of a page's text
	 * @return
	 */
	public boolean isContainingLastLineOfPage() {

		PageBlock page = (PageBlock) this.getContainer();

		if (Math.abs(this.getY2() - page.getMargin()[3]) < page
				.getDocument().readMostPopularWordHeight())
			return true;
		else
			return false;
	}

	/**
	 * returns true if chunk block is an outlier or stray block
	 * @return
	 */
	public boolean isOutlier() {
		
		PageBlock page = (PageBlock) this.getContainer();

		ChunkBlock block = new RTChunkBlock(this.getX1(),
				this.getY1() - 30, this.getX2(),
				this.getY2() + 60, 0);
		
		int neighbouringChunksCount = page.intersectsByType(block, null,
				ChunkBlock.class).size();
		
		int wordBlockCount = page.containsByType(this, null, WordBlock.class).size();
		
		int sizeAfterTrunc = this.readChunkText().
				replaceAll("[A-Za-z0-9]", "").length();
		
		if ( (wordBlockCount < 10 && neighbouringChunksCount < 10)
				|| (sizeAfterTrunc < 10 && neighbouringChunksCount < 10)
				|| this.getMostPopularWordHeight() > 50)
			return true;
		
		return false;
	
	}

	public int getChunkTextLength() {
		return this.readChunkText().length();
	}

	/**
	 * returns the word block density in a chunk block
	 * @return
	 */
	public double getDensity() {
		
		PageBlock page = (PageBlock) this.getContainer();

		List<SpatialEntity> wordBlockList = page.containsByType(this, null,
				WordBlock.class);
		double areaCoveredByWordBlocks = 0;
		for (SpatialEntity entity : wordBlockList)
			areaCoveredByWordBlocks = areaCoveredByWordBlocks
			+ (entity.getHeight() * entity.getWidth());
		return areaCoveredByWordBlocks / (this.getHeight() * this.getWidth());
	}

	/**
	 * returns true if the chunk block is aligned with column boundaries
	 * @return
	 */
	public boolean isAlignedWithColumnBoundaries() {
		
		PageBlock page = (PageBlock) this.getContainer();

		String lrm = this.readLeftRightMidLine();
		int columnLeft = 0;
		int columnRight = 0;
//		double threshold = chunk.getMostPopularWordHeight() * 1.5;
		double threshold = this.getMostPopularWordHeight() * 3;
		
		int l = page.getDocument().getBodyTextFrame().getX1();
		int r = page.getDocument().getBodyTextFrame().getX2();
		int m = (int) Math.round( (l+r)/2.0);
		
		if (Block.MIDLINE.equalsIgnoreCase(lrm)) {
		
			return false;
		
		} else if (Block.LEFT.equalsIgnoreCase(lrm)) {
		
			columnLeft = l;
			columnRight = m;

		} else if (Block.RIGHT.equalsIgnoreCase(lrm)) {
		
			columnLeft = m;
//			columnRight = parent.getMargin()[2];
			columnRight = r;
			
		}
		
		int leftDiff = Math.abs(this.getX1() - columnLeft);
		int rightDiff = Math.abs(this.getX2() - columnRight);

		if (this.readNumberOfLine() > 1
				&& leftDiff < threshold
				&& rightDiff < threshold) {

			return true;
		
		} else if (this.readNumberOfLine() == 1
				&& leftDiff < threshold) {
		
			return true;
		
		}
		
		return false;
	
	}

	/**
	 * returns the classification assigned to previous chunk block
	 * @return
	 */
	public String getlastClassification() {

		ChunkBlock lastBlock = this.readLastChunkBlock();

		return (lastBlock == null) ? null : lastBlock.getChunkType();

	}

	/**
	 * returns the section label of chunk
	 * @return
	 * @throws InvalidPopularSpaceValueException
	 */
	public String getSection() throws InvalidPopularSpaceValueException {
		
		PageBlock page = (PageBlock) this.getContainer();

		ChunkBlock lastBlock = null;
		lastBlock = page.getDocument().getLastChunkBlock(this);

		/*String section = (lastBlock == null) ? null : (lastBlock.getType()
				.contains(".")) ? lastBlock.getType().substring(0,
				lastBlock.getType().indexOf(".")) : lastBlock.getType();*/
		String section;
		if(lastBlock==null){ 
			section=null;
		}else if(lastBlock.getChunkType().contains(".")){
			section= lastBlock.getChunkType().substring(0,lastBlock.getChunkType().indexOf("."));
		}else{
			section=lastBlock.getChunkType();
		}
		if (section == null)
			return null;
		else if (isMainSection(section))
			return section;

		ChunkBlock prev = null;
		while (section != null) {

			/**
			 * introducing a special check to see if the call to getLastChunkBlock returns
			 * the same block i.e. lastBlock if so we break the loop and exit with section = lastBlock.getType()
			 */
			prev = lastBlock;
			lastBlock = page.getDocument().getLastChunkBlock(lastBlock);
			/*if (lastBlock!=null)
			{
				System.out.println(prev.getchunkText());
				System.out.println(lastBlock.getchunkText());
				System.out.println("---------------");
			}
			section = (lastBlock == null) ? null : (lastBlock.getType()
					.contains(".")) ? lastBlock.getType().substring(0,
							lastBlock.getType().indexOf(".")) : lastBlock.getType();*/
			if(lastBlock==null){
				section=null;
			}else if(lastBlock.getChunkType().contains(".")){
				section= lastBlock.getChunkType().substring(0,lastBlock.getChunkType().indexOf("."));
				if(lastBlock.equals(prev)){
					break;
				}
			}else{
				section=lastBlock.getChunkType();
				if(lastBlock.equals(prev)){
					break;
				}
			}
			if (isMainSection(section))
				return section;

		}

		return section;
	}

	private boolean isMainSection(String section) {
		boolean result = !(this.SECTION_AFFLIATION.equals(section)
				|| this.TYPE_CITATION.equals(section)
				|| this.TYPE_FIGURE_LEGEND.equals(section)
				|| this.TYPE_FOOTER.equals(section)
				|| this.TYPE_HEADER.equals(section)
				|| this.TYPE_KEYWORDS.equals(section)
				|| this.TYPE_TABLE.equals(section) || this.TYPE_UNCLASSIFIED
				.equals(section));

		return result;
	}

    
	/**
	 * returns the page number where the block is located
	 * @return
	 */
	public int getPageNumber() {
		PageBlock page = (PageBlock) this.getContainer();
		return page.getPageNumber();
	}

	/**
	 * returns true if the chunk is a single column centered on the page else returns false
	 * @return
	 */
	public boolean isColumnCentered() {

		PageBlock page = (PageBlock) this.getContainer();
		int chunkMedian = this.getX1() + this.getWidth() / 2;
		int pageMedian = page.getMedian();
		String lrm = this.readLeftRightMidLine();

		if (this.MIDLINE.equalsIgnoreCase(lrm)) {
			if (Math.abs(pageMedian - chunkMedian) < page.getDocument()
					.readMostPopularWordHeight() * 2)
				return true;
			return false;
		}
		
		int pageMedianLeftRight = 0;
		
		if (this.LEFT.equalsIgnoreCase(lrm)) {
		
			pageMedianLeftRight = page.getMargin()[0]
			                                         + (pageMedian - page.getMargin()[0]) / 2;
		} else if (this.RIGHT.equalsIgnoreCase(lrm)) {
			
			pageMedianLeftRight = pageMedian
			+ (page.getMargin()[2] - pageMedian) / 2;
		
		}

		if (Math.abs(chunkMedian - pageMedianLeftRight) < page.getDocument()
				.readMostPopularWordHeight() * 2)
			return true;
		
		return false;
	}
	
	public boolean isWithinBodyTextFrame() {

		PageBlock page = (PageBlock) this.getContainer();

		SpatialEntity btf = page.getDocument().getBodyTextFrame();
		double threshold = this.getMostPopularWordHeight() * 3;

		if( this.getX1() + threshold > btf.getX1() &&
				this.getX2() - threshold < btf.getX2() &&
				this.getY1() + threshold > btf.getY1() &&
				this.getY2() - threshold < btf.getY2() ) {
			return true;
		} else {
			return false;
		}
		
	}
	
	public boolean isTallerThanWide() {
		
		if( this.getHeight() > this.getWidth() ) {
			return true;
		} else {
			return false;
		}
		
	}
	
	

	@Override
	public void setChunkSection(String section) {
		this.paperSection = section;		
	}

	@Override
	public String getChunkSection() {
		return this.paperSection ;
	}

}

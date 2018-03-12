package edu.isi.bmkeg.lapdf.controller;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import org.apache.log4j.Logger;
import org.ghost4j.document.PDFDocument;
import org.ghost4j.renderer.SimpleRenderer;
import org.jpedal.exception.PdfException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import edu.isi.bmkeg.lapdf.classification.ruleBased.RuleBasedChunkClassifier;
import edu.isi.bmkeg.lapdf.extraction.exceptions.AccessException;
import edu.isi.bmkeg.lapdf.extraction.exceptions.ClassificationException;
import edu.isi.bmkeg.lapdf.extraction.exceptions.EncryptionException;
import edu.isi.bmkeg.lapdf.model.ChunkBlock;
import edu.isi.bmkeg.lapdf.model.LapdfDocument;
import edu.isi.bmkeg.lapdf.model.PageBlock;
import edu.isi.bmkeg.lapdf.model.WordBlock;
import edu.isi.bmkeg.lapdf.model.RTree.RTChunkBlock;
import edu.isi.bmkeg.lapdf.model.RTree.RTModelFactory;
import edu.isi.bmkeg.lapdf.model.factory.AbstractModelFactory;
import edu.isi.bmkeg.lapdf.model.ordering.SpatialOrdering;
import edu.isi.bmkeg.lapdf.model.spatial.SpatialEntity;
import edu.isi.bmkeg.lapdf.parser.RuleBasedParser;
import edu.isi.bmkeg.lapdf.text.SectionsTextWriter;
import edu.isi.bmkeg.lapdf.text.SpatialLayoutFeaturesReportGenerator;
import edu.isi.bmkeg.lapdf.text.SpatiallyOrderedChunkTextWriter;
import edu.isi.bmkeg.lapdf.text.SpatiallyOrderedChunkTypeFilteredTextWriter;
import edu.isi.bmkeg.lapdf.utils.JPedalPDFRenderer;
import edu.isi.bmkeg.lapdf.utils.PageImageOutlineRenderer;
import edu.isi.bmkeg.lapdf.xml.OpenAccessXMLWriter;
import edu.isi.bmkeg.lapdf.xml.SpatialXMLWriter;
import edu.isi.bmkeg.utils.Converters;

/**
 * Basic Java API to high-level LAPDFText functionality, including:
 * 
 * 1) Gathering layout statistics for the PDF file 2) Running Block-based
 * spatial chunker on PDF. 3) Classifying texts of blocks in the file to
 * categories based on a rule file. 4) Outputting text or XML to file 5)
 * Rendering pages images of text layout or the original PDF file as PNG files
 * 6) Serializing LAPDFText object to a VPDMf database record.
 * 
 * @author burns
 * 
 */
public class LapdfEngine {

	private static Logger logger = Logger.getLogger(LapdfEngine.class);

	private RuleBasedParser parser;

	private File ruleFile;
	private RuleBasedChunkClassifier classfier;

	private boolean imgFlag = false;

	private JPedalPDFRenderer imagifier = new JPedalPDFRenderer();

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public LapdfEngine() throws Exception {

		this.parser = new RuleBasedParser(new RTModelFactory());

	}

	public LapdfEngine(File ruleFile) throws Exception {

		this.parser = new RuleBasedParser(new RTModelFactory());
		this.setRuleFile(ruleFile);

	}

	public LapdfEngine(boolean imgFlag) throws Exception {

		this.parser = new RuleBasedParser(new RTModelFactory());
		URL u = this.getClass().getClassLoader().getResource("rules/general.drl");
		this.setRuleFile(new File(u.getPath()));
		this.setImgFlag(imgFlag);

	}

	public LapdfEngine(File ruleFile, boolean imgFlag) throws Exception {

		this.parser = new RuleBasedParser(new RTModelFactory());
		this.setRuleFile(ruleFile);
		this.setImgFlag(imgFlag);

	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public RuleBasedParser getParser() {
		return parser;
	}

	public void setParser(RuleBasedParser parser) {
		this.parser = parser;
	}

	public boolean isImgFlag() {
		return imgFlag;
	}

	public void setImgFlag(boolean imgFlag) {
		this.imgFlag = imgFlag;
	}

	public File getRuleFile() {
		return ruleFile;
	}

	public void setRuleFile(File ruleFile) throws IOException, ClassificationException {
		this.ruleFile = ruleFile;
		this.classfier = new RuleBasedChunkClassifier(ruleFile.getPath());
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public void processBlocks(File inFile, File outDir, boolean reportBlocks, boolean extractUnclassified)
			throws Exception {

		String stem = inFile.getName();
		stem = stem.substring(0, stem.lastIndexOf("."));

		this.parser.setPath(outDir.getPath());

		LapdfDocument doc = blockifyFile(inFile);

		if (doc == null) {
			logger.info("Error encountered while performing block detection." + " Skipping " + inFile.getPath()
					+ " because doc is null");
			return;
		}

		logger.info("Writing spatial block xml to " + outDir.getPath() + "/" + stem + "_spatial.xml");

		if (this.isImgFlag())
			this.dumpWordOrderImageOutlinesToFiles(doc, outDir, stem);

		SpatialXMLWriter sxw = new SpatialXMLWriter();
		sxw.write(doc, outDir.getPath() + "/" + stem + "_spatial.xml");

		if (reportBlocks) {

			logger.info("Running block feature reporter on " + inFile.getPath());

			SpatialLayoutFeaturesReportGenerator slfrg = new SpatialLayoutFeaturesReportGenerator();
			slfrg.write(doc, outDir.getPath() + "/" + stem + "_spatialFeatures.dat");

		}

		if (extractUnclassified) {

			SpatiallyOrderedChunkTextWriter soctw = new SpatiallyOrderedChunkTextWriter();
			soctw.write(doc, outDir.getPath() + "/" + stem + "_unclassifiedFlowAwareText.dat");

		}

	}

	public void processClassify(File inFile, File outDir, boolean reportBlocks, boolean extractUnclassified)
			throws Exception {

		String stem = inFile.getName();
		stem = stem.substring(0, stem.lastIndexOf("."));

		this.parser.setPath(outDir.getPath());

		LapdfDocument doc = blockifyFile(inFile);
		if (doc == null) {

			logger.info("Error encountered while performing block detection. Skipping " + inFile.getPath()
					+ " because doc is null");

			return;

		}

		logger.info("Writing spatial block xml to " + outDir.getPath() + "/" + stem + "_spatial.xml");

		SpatialXMLWriter sxw = new SpatialXMLWriter();
		sxw.write(doc, outDir.getPath() + "/" + stem + "_spatial.xml");

		logger.info("Running block classification on " + inFile.getPath());
		classifyDocument(doc, this.getRuleFile());

		if (this.isImgFlag())
			this.dumpWordOrderImageOutlinesToFiles(doc, outDir, stem);

		logger.info("Writing block classified XML in OpenAccess format " + outDir.getPath() + "/" + stem
				+ "_rhetorical.xml");

		OpenAccessXMLWriter oaxw = new OpenAccessXMLWriter();
		oaxw.write(doc, outDir.getPath() + "/" + stem + "_rhetorical.xml");

		if (reportBlocks) {

			logger.info("Running block feature reporter on " + inFile.getPath());

			SpatialLayoutFeaturesReportGenerator slfrg = new SpatialLayoutFeaturesReportGenerator();

			slfrg.write(doc, outDir.getPath() + "/" + stem + "_spatialFeatures.dat");

		}

		if (extractUnclassified) {

			SpatiallyOrderedChunkTextWriter soctw = new SpatiallyOrderedChunkTextWriter();
			soctw.write(doc, outDir.getPath() + "/" + stem + "_unclassifiedFlowAwareText.dat");

		}

	}

	public void processSectionFilter(File inFile, File outDir, boolean reportBlocks, boolean extractUnclassified)
			throws Exception {

		String stem = inFile.getName();
		stem = stem.substring(0, stem.lastIndexOf("."));

		this.parser.setPath(outDir.getPath());

		logger.info("Running block detection on " + inFile.getPath());

		LapdfDocument doc = blockifyFile(inFile);

		if (doc == null) {
			logger.info("Error encountered while performing block detection. Skipping " + inFile.getPath()
					+ " because doc is null");
			return;
		}

		logger.info("Running block classification on " + inFile.getPath());

		classifyDocument(doc, this.getRuleFile());

		if (this.isImgFlag())
			this.dumpWordOrderImageOutlinesToFiles(doc, outDir, stem);

		SpatiallyOrderedChunkTypeFilteredTextWriter soctftw = new SpatiallyOrderedChunkTypeFilteredTextWriter(true,
				true);
		soctftw.write(doc, outDir.getPath() + "/" + stem + "_spatialFiltered.txt");

		logger.info("Writing block classified XML in OpenAccess format " + outDir.getPath() + "/" + stem
				+ "_rhetorical.xml");

		if (reportBlocks) {
			logger.info("Running block feature reporter on " + inFile.getPath());
			SpatialLayoutFeaturesReportGenerator slfrg = new SpatialLayoutFeaturesReportGenerator();
			slfrg.write(doc, outDir.getPath() + "/" + stem + "_spatialFeatures.dat");
		}

		if (extractUnclassified) {
			SpatiallyOrderedChunkTextWriter soctw = new SpatiallyOrderedChunkTextWriter();
			soctw.write(doc, outDir.getPath() + "/" + stem + "_unclassifiedFlowAwareText.dat");
		}

	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// File Processing functions
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Extracts the blocks within file to generate a LapdfDocument Object
	 * 
	 * @param file
	 *            - input file
	 * @return
	 * @throws PdfException
	 * @throws AccessException
	 * @throws EncryptionException
	 * @throws IOException
	 */
	public LapdfDocument blockifyFile(File f) throws Exception {

		LapdfDocument doc = null;

		doc = parser.parse(f);
		doc.setPdfFile(f);

		if (doc.hasjPedalDecodeFailed()) {
			return null;
		}
		return doc;

	}

	public LapdfDocument blockifyXml(String s) throws Exception {

		LapdfDocument doc = null;

		doc = parser.parseXml(s);

		if (doc.hasjPedalDecodeFailed()) {
			return null;
		}

		return doc;

	}

	public void classifyDocumentWithBaselineRules(LapdfDocument document)
			throws ClassificationException, IOException, URISyntaxException {

		File f = Converters.extractFileFromJarClasspath(".", "rules/general.drl");

		this.classifyDocument(document, f);

		if (this.isImgFlag())
			this.dumpWordOrderImageOutlinesToFiles(document, new File("."), "debug");

	}

	/**
	 * Classifies the chunks in a file based on the rule file
	 * 
	 * @param document
	 *            - an instantiated LapdfDocument
	 * @param ruleFile
	 *            - a rule file on disk
	 * @throws IOException
	 */
	public void classifyDocument(LapdfDocument document, File ruleFile) throws ClassificationException, IOException {

		for (int i = 1; i <= document.getTotalNumberOfPages(); i++) {

			PageBlock page = document.getPage(i);

			List<ChunkBlock> chunkList = page.getAllChunkBlocks(SpatialOrdering.MIXED_MODE);

			classfier.classify(chunkList);

		}

	}

	public String readBasicText(LapdfDocument document) throws IOException, FileNotFoundException {

		List<Set<String>> stack = new ArrayList<Set<String>>();

		Set<String> sections = new HashSet<String>();
		sections.add(ChunkBlock.TYPE_BODY);
		sections.add(ChunkBlock.TYPE_HEADING);
		stack.add(sections);

		sections = new HashSet<String>();
		sections.add(ChunkBlock.TYPE_FIGURE_LEGEND);
		stack.add(sections);

		return this.readClassifiedText(document, stack);

	}

	public String readCompleteText(LapdfDocument document) throws IOException, FileNotFoundException {

		List<Set<String>> stack = new ArrayList<Set<String>>();

		Set<String> sections1 = new HashSet<String>();
		sections1.add(ChunkBlock.TYPE_TITLE);
		sections1.add(ChunkBlock.TYPE_AUTHORS);
		sections1.add(ChunkBlock.TYPE_CITATION);
		stack.add(sections1);

		Set<String> sections2 = new HashSet<String>();
		sections2.add(ChunkBlock.TYPE_ABSTRACT);
		stack.add(sections2);

		Set<String> sections3 = new HashSet<String>();
		sections3.add(ChunkBlock.TYPE_BODY);
		sections3.add(ChunkBlock.TYPE_HEADING);
		sections3.add(ChunkBlock.SECTION_METHODS);
		sections3.add(ChunkBlock.SECTION_RESULTS);
		sections3.add(ChunkBlock.SECTION_DISCUSSION);
		sections3.add(ChunkBlock.SECTION_DISCUSSION);
		sections3.add(ChunkBlock.SECTION_CONCLUSIONS);
		sections3.add(ChunkBlock.SECTION_INTRODUCTION);
		stack.add(sections3);

		Set<String> sections4 = new HashSet<String>();
		sections4.add(ChunkBlock.SECTION_ACKNOWLEDGEMENTS);
		stack.add(sections4);

		Set<String> sections5 = new HashSet<String>();
		sections5.add(ChunkBlock.TYPE_FIGURE_LEGEND);
		sections5.add(ChunkBlock.TYPE_TABLE);
		stack.add(sections5);

		String text = this.readClassifiedText(document, stack);

		return text;

	}

	public String readClassifiedText(LapdfDocument document, List<Set<String>> stack)
			throws IOException, FileNotFoundException {

		StringBuilder text = new StringBuilder();

		Iterator<Set<String>> it = stack.iterator();
		while (it.hasNext()) {
			Set<String> sections = it.next();

			text.append(this.readClassifiedText(document, sections));

		}

		return text.toString();

	}

	public String readClassifiedText(LapdfDocument document, Set<String> sections)
			throws IOException, FileNotFoundException {

		StringBuilder sb = new StringBuilder();

		int n = document.getTotalNumberOfPages();
		for (int i = 1; i <= n; i++) {
			PageBlock page = document.getPage(i);

			List<ChunkBlock> chunksPerPage = page.getAllChunkBlocks(SpatialOrdering.MIXED_MODE);

			for (ChunkBlock chunkBlock : chunksPerPage) {
				if (sections.contains(chunkBlock.getChunkType())) {
					sb.append(chunkBlock.readChunkText() + "\n");
				}
			}
		}

		return sb.toString();

	}

	/**
	 * Write out the blocked LapdfDocument object to XML
	 * 
	 * @param doc
	 * @param out
	 */
	public void writeSpatialXmlToFile(LapdfDocument doc, File out) {

		logger.info("Writing spatial block XML to " + out.getPath());
		SpatialXMLWriter sxw = new SpatialXMLWriter();
		sxw.write(doc, out.getPath());

	}

	/**
	 * Write an LapdfDocument out to an OpenAccess-compatible XML format
	 * 
	 * @param doc
	 * @param out
	 * @throws IOException
	 */
	public void writeBlockStatisticsReport(LapdfDocument doc, File out) throws IOException {

		logger.info("Writing spatial features report to " + out.getPath());
		SpatialLayoutFeaturesReportGenerator slfrg = new SpatialLayoutFeaturesReportGenerator();
		slfrg.write(doc, out.getPath());

	}

	/**
	 * Render images of the pages of the PDF file
	 * 
	 * @param pdfFile
	 * @param outputDir
	 * @throws Exception
	 */
	public void renderPageImages(File pdfFile, File outputDir) throws Exception {

		this.imagifier.generateImages(pdfFile, outputDir);

	}

	/**
	 * Render images of the positions of words on each page of pdf annotated with
	 * order of being added to the block.
	 * 
	 * @param doc
	 * @param dir
	 * @param stem
	 * @param mode
	 * @throws IOException
	 */
	public void dumpWordOrderImageOutlinesToFiles(LapdfDocument doc, File dir, String stem) throws IOException {

		for (int i = 1; i <= doc.getTotalNumberOfPages(); i++) {
			PageBlock page = doc.getPage(i);
			File imgFile = new File(dir.getPath() + "/" + stem + "_" + page.getPageNumber() + ".png");
			PageImageOutlineRenderer.dumpWordOrderPageImageToFile(page, imgFile, stem + "_" + page.getPageNumber());
		}

	}

	/**
	 * Render images of the positions of chunks on each page annotated with chunk
	 * types.
	 * 
	 * @param doc
	 * @param dir
	 * @param stem
	 * @param lapdfMode
	 * @throws IOException
	 */
	public void dumpChunkTypeImageOutlinesToFiles(LapdfDocument doc, File dir, String stem) throws IOException {

		for (int i = 1; i <= doc.getTotalNumberOfPages(); i++) {
			PageBlock page = doc.getPage(i);
			File imgFile = new File(dir.getPath() + "/" + stem + "_" + page.getPageNumber() + ".png");
			PageImageOutlineRenderer.dumpChunkTypePageImageToFile(page, imgFile, stem + "_" + page.getPageNumber());
		}

	}

	public List<BufferedImage> buildWordOrderImageList(LapdfDocument doc, String stem) throws IOException {

		List<BufferedImage> imgList = new ArrayList<BufferedImage>();

		for (int i = 1; i <= doc.getTotalNumberOfPages(); i++) {
			PageBlock page = doc.getPage(i);
			BufferedImage img = PageImageOutlineRenderer.createPageImageForBlocksWordOrder(page,
					stem + "_" + page.getPageNumber());
			imgList.add(img);
		}

		return imgList;

	}

	/**
	 * Writing text based report of spatial features of the PDF file
	 * 
	 * @param doc
	 * @param out
	 * @throws IOException
	 */
	public void writeSpatialFeaturesReport(LapdfDocument doc, File out) throws IOException {

		logger.info("Writing block feature report of " + doc.getPdfFile().getPath() + " to " + out.getPath());
		SpatialLayoutFeaturesReportGenerator slfrg = new SpatialLayoutFeaturesReportGenerator();
		slfrg.write(doc, out.getPath());

	}

	public void writeTextToFile(LapdfDocument doc, Set<String> sections, File out) throws Exception {

		logger.info("Writing text of  " + doc.getPdfFile().getPath() + " to " + out.getPath());
		SectionsTextWriter stw = new SectionsTextWriter();
		stw.addToStack(sections);
		stw.write(doc, out.getPath());

	}

	public void writeTextToFile(LapdfDocument doc, List<Set<String>> stack, File out) throws Exception {

		logger.info("Writing text of  " + doc.getPdfFile().getPath() + " to " + out.getPath());
		SectionsTextWriter stw = new SectionsTextWriter();
		Iterator<Set<String>> it = stack.iterator();
		while (it.hasNext()) {
			Set<String> sections = it.next();
			stw.addToStack(sections);
		}
		stw.write(doc, out.getPath());

	}

	/**
	 * 
	 * @param doc
	 * @param outputFile
	 * @throws IOException
	 */
	public void dumpFeaturesToSpreadsheet(LapdfDocument doc, File outputFile) throws IOException {

		FileWriter fw = new FileWriter(outputFile);
		BufferedWriter bw = new BufferedWriter(fw);
		bw.write(this.dumpFeaturesToSpreadsheetString(doc));
		bw.close();

	}

	/**
	 * 
	 * @param doc
	 * @param outputFile
	 * @throws IOException
	 */
	public String dumpFeaturesToSpreadsheetString(LapdfDocument doc) throws IOException {

		/*
		 * The features of ChunkBlocks to include
		 * 
		 * features
		 * 
		 * 1. isMostPopularFontInDocument 2. isNextMostPopularFontInDocument 3.
		 * getHeightDifferenceBetweenChunkWordAndDocumentWord 4. isInTopHalf 5.
		 * getMostPopularFontSize 6. isAllCapitals 7. isMostPopularFontModifierBold 8.
		 * isMostPopularFontModifierItalic 9. isContainingFirstLineOfPage 10.
		 * isContainingLastLineOfPage 11. isOutlier 12. getChunkTextLength 13.
		 * readDensity 14. isAlignedWithColumnBoundaries 15. getPageNumber 16.
		 * isColumnCentered 17. isWithinBodyTextFrame
		 * 
		 * properties
		 * 
		 * 18. getMostPopularWordHeight 19. getMostPopularWordSpaceWidth 20.
		 * getMostPopularWordFont 21. getMostPopularWordStyle 22. readNumberOfLine 23.
		 * isHeaderOrFooter 24. readLeftRightMidline 25. readChunkText
		 */
		int nFeatures = 25;
		AbstractModelFactory modelFactory = new RTModelFactory();

		StringBuffer sb = new StringBuffer();

		if (doc.getPdfFile() == null) {
			sb.append("RuleSet,\n");
		} else {
			sb.append("RuleSet," + doc.getPdfFile().getName() + "\n");
		}

		sb.append("Import,\"edu.isi.bmkeg.lapdf.features.ChunkFeatures,edu.isi.bmkeg.lapdf.model.ChunkBlock\"\n");
		sb.append("Variables,\"ChunkBlock chunk, ChunkFeatures chunkFeature\"\n");
		sb.append("Sequential,true\n");
		sb.append(",,,\n");
		sb.append(",,RuleTable\n");

		// Row 8
		sb.append("NAME,DESCRIPTION");
		for (int i = 0; i < nFeatures; i++) {
			sb.append(",CONDITION");
		}
		sb.append(",ACTION\n");

		// Row 9
		sb.append(",Class Name or Operation");
		for (int i = 0; i < 17; i++) {
			sb.append(",ChunkFeatures");
		}
		for (int i = 0; i < 8; i++) {
			sb.append(",eval");
		}
		sb.append(",\n");

		// Row 10
		sb.append(",Attribute or Condition");
		FEATURES: {
			sb.append(",mostPopularFontInDocument"); // 1
			sb.append(",nextMostPopularFontInDocument"); // 2
			sb.append(",heightDifferenceBetweenChunkWordAndDocumentWord"); // 3
			sb.append(",inTopHalf"); // 4
			sb.append(",mostPopularFontSize"); // 5
			sb.append(",allCapitals"); // 6
			sb.append(",mostPopularFontModifierBold"); // 7
			sb.append(",mostPopularFontModifierItalic"); // 8
			sb.append(",containingFirstLineOfPage"); // 9
			sb.append(",containingLastLineOfPage"); // 10
			sb.append(",outlier"); // 11
			sb.append(",chunkTextLength"); // 12
			sb.append(",density"); // 13
			sb.append(",alignedWithColumnBoundaries"); // 14
			sb.append(",pageNumber"); // 15
			sb.append(",columnCentered"); // 16
			sb.append(",withinBodyTextFrame"); // 17
			sb.append(",chunk.getMostPopularWordHeight==$param"); // 18
			sb.append(",chunk.getMostPopularWordSpaceWidth==$param"); // 19
			sb.append(",chunk.getMostPopularWordFont==$param"); // 20
			sb.append(",chunk.getMostPopularWordStyle==$param"); // 21
			sb.append(",chunk.readNumberOfLine==$param"); // 22
			sb.append(",chunk.isHeaderOrFooter==$param"); // 23
			sb.append(",chunk.readLeftRightMidLine==$param"); // 24
			sb.append(",chunk.isMatchingRegularExpression($param)"); // 25
		}
		sb.append(",chunk.setType($param);\n");

		// Row 11
		sb.append("Name of Rule, Description of Rule (Textual documentation)");
		FEATURES: {
			sb.append(",The most popular font"); // 1
			sb.append(",Next most popular font"); // 2
			sb.append(",Height difference between chunk height and document height"); // 3
			sb.append(",Is in the top half"); // 4
			sb.append(",The most popular font size"); // 5
			sb.append(",Is all capitals"); // 6
			sb.append(",Is the most popular aligned font bold?"); // 7
			sb.append(",Is the most popular aligned font itallics?"); // 8
			sb.append(",Does this contain the first line of the page"); // 9
			sb.append(",Does this contain the last line of the page"); // 10
			sb.append(",Is an outlier"); // 11
			sb.append(",The chunk length"); // 12
			sb.append(",The density of words the block"); // 13
			sb.append(",Is this aligned with column boundaries"); // 14
			sb.append(",pageNumber==$param"); // 15
			sb.append(",is centered in the column"); // 16
			sb.append(",is within the frame"); // 17
			sb.append(",most popular font height"); // 18
			sb.append(",most popular word space"); // 19
			sb.append(",most popular word font"); // 20
			sb.append(",most popular word style"); // 21
			sb.append(",number of lines"); // 22
			sb.append(",is a header or footer"); // 23
			sb.append(",is left/right/midline"); // 24
			sb.append(",Define and run a regular expression on the text"); // 25
		}
		sb.append(",chunk.setType($param);\n");

		int n = doc.getTotalNumberOfPages();
		for (int i = 1; i <= n; i++) {
			PageBlock page = doc.getPage(i);

			List<ChunkBlock> blocks = page.getAllChunkBlocks(SpatialOrdering.MIXED_MODE);

			for (int j = 0; j < blocks.size(); j++) {
				ChunkBlock b = blocks.get(j);

				sb.append("p:" + i + "." + j + ","); // 1
				sb.append("," + b.isMostPopularFontInDocument()); // 1
				sb.append("," + b.isNextMostPopularFontInDocument()); // 2
				sb.append("," + b.getHeightDifferenceBetweenChunkWordAndDocumentWord()); // 3
				sb.append("," + b.isInTopHalf()); // 4
				sb.append("," + b.getMostPopularFontSize()); // 5
				sb.append("," + b.isAllCapitals()); // 6
				sb.append("," + b.isMostPopularFontModifierBold()); // 7
				sb.append("," + b.isMostPopularFontModifierItalic()); // 8
				sb.append("," + b.isContainingFirstLineOfPage()); // 9
				sb.append("," + b.isContainingLastLineOfPage()); // 10
				sb.append("," + b.isOutlier()); // 11
				sb.append("," + b.getChunkTextLength()); // 12
				sb.append("," + b.getDensity()); // 13
				sb.append("," + b.isAlignedWithColumnBoundaries()); // 14
				sb.append("," + b.getPageNumber()); // 15
				sb.append("," + b.isColumnCentered()); // 16
				sb.append("," + b.isWithinBodyTextFrame()); // 17
				sb.append("," + b.getMostPopularWordHeight()); // 18
				sb.append("," + b.getMostPopularWordSpaceWidth()); // 19
				sb.append("," + b.getMostPopularWordFont()); // 20
				sb.append("," + b.getMostPopularWordStyle()); // 21
				sb.append("," + b.readNumberOfLine()); // 22
				sb.append("," + b.isHeaderOrFooter()); // 23
				sb.append("," + b.readLeftRightMidLine()); // 24
				sb.append("," + b.readChunkText().replaceAll(",", ";")); // 25
				sb.append("\n");

			}

		}

		return sb.toString();

	}

	public void extractFiguresFromArticle(File pdf, File outDir, String stem) throws Exception {

		String figureRegex = "^(Figure|Fig\\.{0,1})\\s+(\\d+)";
		Pattern patt = Pattern.compile(figureRegex);

		LapdfDocument doc = this.blockifyFile(pdf);
		// this.classifyDocument(doc, ruleFile);

		SpatialEntity frame = doc.getBodyTextFrame();

		Map<String, BufferedImage> imageList = new HashMap<String, BufferedImage>();

		PDFDocument document = new PDFDocument();
		document.load(pdf);
		SimpleRenderer renderer = new SimpleRenderer();
		renderer.setResolution(72 * 5);
		List<Image> pageImages = renderer.render(document);
		List<Image> copyImages = new ArrayList<Image>();
		for (Image img : pageImages) {
			copyImages.add(deepCopy((BufferedImage) img));
		}

		Map<RTChunkBlock, RTChunkBlock> captionBlockMap = new HashMap<RTChunkBlock, RTChunkBlock>();
		for (ChunkBlock c : doc.readAllChunkBlocks()) {
			String txt = c.readChunkText();
			Matcher m = patt.matcher(txt);
			if (m.find()) {

				// Find neighboring blocks above, to the left, and to the right...
				if (c instanceof RTChunkBlock) {
					RTChunkBlock captionBlock = (RTChunkBlock) c;

					// distance in each direction
					// aggregated to a list:
					List<Integer> distances = new ArrayList<Integer>();
					int disNorth = captionBlock.getY1() - frame.getY1();
					ChunkBlock north = captionBlock.readNearestNeighborChunkBlock(ChunkBlock.NORTH, 30);
					north = captionBlock.computeWordEdgeForTestBlock(north);
					if (north != null)
						disNorth = captionBlock.getY1() - north.getY2();
					distances.add(disNorth);

					int disSouth = frame.getY2() - captionBlock.getY2();
					ChunkBlock south = captionBlock.readNearestNeighborChunkBlock(ChunkBlock.SOUTH, 30);
					south = captionBlock.computeWordEdgeForTestBlock(south);
					disSouth = south.getY1() - captionBlock.getY2();
					if (south != null) {
						List<SpatialEntity> wordsInBlock = (captionBlock.getPage()).intersectsByType(south,
								SpatialOrdering.MIXED_MODE, WordBlock.class);
						String text = "";
						for (SpatialEntity se : wordsInBlock) {
							text += ((WordBlock) se).getWord();
						}
						if (text.toLowerCase().startsWith("fig"))
							disSouth = 0;
						else
							disSouth = south.getY1() - captionBlock.getY2();
					}
					distances.add(disSouth);

					int disEast = frame.getX2() - captionBlock.getX2();
					ChunkBlock east = captionBlock.readNearestNeighborChunkBlock(ChunkBlock.EAST, 30);
					east = captionBlock.computeWordEdgeForTestBlock(east);
					if (east != null)
						disEast = east.getX1() - captionBlock.getX2();
					distances.add(disEast);

					int disWest = captionBlock.getX1() - frame.getX1();
					ChunkBlock west = captionBlock.readNearestNeighborChunkBlock(ChunkBlock.WEST, 30);
					west = captionBlock.computeWordEdgeForTestBlock(west);
					if (west != null)
						disWest = captionBlock.getX1() - west.getX2();
					distances.add(disWest);

					RTChunkBlock figureBlock = new RTChunkBlock(captionBlock.getX1() - disWest,
							captionBlock.getY1() - disNorth, captionBlock.getX2() + disEast,
							captionBlock.getY2() + disSouth, -1);
					int id = captionBlock.getPage().countChunks();
					figureBlock.setChunkType(ChunkBlock.TYPE_FIGURE);

					captionBlock.getPage().add(figureBlock, id);
					captionBlockMap.put(captionBlock, figureBlock);

					PageBlock pg = captionBlock.getPage();
					int p = pg.getPageNumber() - 1;
					BufferedImage copy = (BufferedImage) copyImages.get(p);
					double sf = copy.getHeight() / (captionBlock.getPage().getPageBoxHeight());
					if (north != null)
						outlineWordsOnImage(copy, pg, north, sf, Color.MAGENTA, 3.0f);
					if (south != null)
						outlineWordsOnImage(copy, pg, south, sf, Color.MAGENTA, 3.0f);
					if (east != null)
						outlineWordsOnImage(copy, pg, east, sf, Color.MAGENTA, 3.0f);
					if (west != null)
						outlineWordsOnImage(copy, pg, west, sf, Color.MAGENTA, 3.0f);

					logger.info("Completed Figure " + m.group(2));

				}

			}

		}

		//
		// Clean up figure blocks that overlap.
		//
		for (RTChunkBlock cb : captionBlockMap.keySet()) {
			RTChunkBlock fb = captionBlockMap.get(cb);
			List<SpatialEntity> overlappingBlocks = cb.getPage().intersectsByType(fb, null, ChunkBlock.class);
			for (SpatialEntity se : overlappingBlocks) {
				if (se instanceof RTChunkBlock && 
						((ChunkBlock) se).getChunkType().equals(ChunkBlock.TYPE_FIGURE) &&
						fb.getWidth() > se.getWidth() + 20 ) {
					SpatialEntity ir = fb.getIntersectingRectangle(se);
					if (ir.getX1() > fb.getX1())
						fb.resize(ir.getX2() + 1, fb.getY1(), fb.getX2() - ir.getX2() - 1, fb.getY2() - fb.getY1());
					else
						fb.resize(fb.getX1(), fb.getY1(), ir.getX1() - fb.getX1() - 1, fb.getY2() - fb.getY1());
					
				}
			}
		}

		for (RTChunkBlock captionBlock : captionBlockMap.keySet()) {

			String txt = captionBlock.readChunkText();
			Matcher m = patt.matcher(txt);
			if (!m.find()) {
				continue;
			}

			RTChunkBlock figureBlock = captionBlockMap.get(captionBlock);

			int p = captionBlock.getPage().getPageNumber() - 1;

			BufferedImage img = (BufferedImage) pageImages.get(p);
			BufferedImage copy = (BufferedImage) copyImages.get(p);

			double sf = img.getHeight() / (captionBlock.getPage().getPageBoxHeight());

			int x = (new Double(figureBlock.getX1() * sf)).intValue();
			int y = (new Double(figureBlock.getY1() * sf)).intValue();
			int w = (new Double(figureBlock.getWidth() * sf)).intValue();
			int h = (new Double(figureBlock.getHeight() * sf)).intValue();

			BufferedImage subImg = img.getSubimage(x, y, w, h);

			for (SpatialEntity wordBlock : ((PageBlock) captionBlock.getPage()).containsByType(captionBlock,
					SpatialOrdering.MIXED_MODE, WordBlock.class)) {

				int wb_x = (new Double(wordBlock.getX1() * sf)).intValue();
				int wb_y = (new Double(wordBlock.getY1() * sf)).intValue();
				int wb_w = (new Double(wordBlock.getWidth() * sf)).intValue();
				int wb_h = (new Double(wordBlock.getHeight() * sf)).intValue();
				Graphics2D graph = subImg.createGraphics();
				graph.setColor(Color.WHITE);
				graph.fill(new Rectangle(wb_x - x - 15, wb_y - y, wb_w + 30, wb_h));
				graph.dispose();

			}

			//
			// Run through all words in chunk to see if any are left over from image.
			//
			List<SpatialEntity> wordsInBlock = ((PageBlock) captionBlock.getPage()).containsByType(figureBlock,
					SpatialOrdering.MIXED_MODE, WordBlock.class);
			List<SpatialEntity> allWordsInCaption = ((PageBlock) captionBlock.getPage()).containsByType(captionBlock,
					SpatialOrdering.MIXED_MODE, WordBlock.class);
			wordsInBlock.removeAll(allWordsInCaption);
			if (wordsInBlock.size() > 0) {
				List<Map<String, String>> outputData = new ArrayList<Map<String, String>>();
				boolean greenLight = true;
				for (SpatialEntity se : wordsInBlock) {
					WordBlock wb = (WordBlock) se;
					/*
					 * String ww = wb.getWord(); if (ww.equals("Page") || ww.equals("Vol") ||
					 * ww.startsWith("http://")) { greenLight = false;
					 * drawSpatialEntityOnImage(copy, wb, sf, Color.CYAN, 4.0f); break; }
					 */
					drawSpatialEntityOnImage(copy, se, sf, Color.BLUE, 4.0f);

					Map<String, String> wbo = new HashMap<String, String>();
					int wb_x = (new Double((wb.getX1() - figureBlock.getX1()) * sf)).intValue();
					int wb_y = (new Double((wb.getY1() - figureBlock.getY1()) * sf)).intValue();
					int wb_w = (new Double(wb.getWidth() * sf)).intValue();
					int wb_h = (new Double(wb.getHeight() * sf)).intValue();
					wbo.put("word", wb.getWord());
					wbo.put("font", wb.getFont());
					wbo.put("p", "" + wb.getPage().getPageNumber());
					wbo.put("x", "" + wb_x);
					wbo.put("y", "" + wb_y);
					wbo.put("w", "" + wb_w);
					wbo.put("h", "" + wb_h);
					wbo.put("fontStyle", "" + wb.getFontStyle());
					outputData.add(wbo);
				}
				if (greenLight) {
					Gson gson = new GsonBuilder().setPrettyPrinting().create();
					String json = gson.toJson(outputData);
					File extraWordsFile = new File(outDir + "/" + stem + "_fig_" + m.group(2) + ".json");
					PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(extraWordsFile, true)));
					out.write(json);
					out.close();
				}
			}

			drawSpatialEntityOnImage(copy, captionBlock, sf, Color.MAGENTA, 4.0f);
			drawSpatialEntityOnImage(copy, doc.getBodyTextFrame(), sf, Color.PINK, 4.0f);
			drawSpatialEntityOnImage(copy, figureBlock, sf, Color.RED, 4.0f);

			File imgfile = new File(outDir + "/" + stem + "_fig_" + m.group(2) + ".png");
			ImageIO.write(subImg, "png", imgfile);

			File pagefile = new File(outDir + "/" + stem + "_fig_" + m.group(2) + "_page.png");
			ImageIO.write(copy, "png", pagefile);

		}

	}

	static void drawSpatialEntityOnImage(BufferedImage bi, SpatialEntity se, double sf, Color color, float stroke) {

		int x = (new Double(se.getX1() * sf)).intValue();
		int y = (new Double(se.getY1() * sf)).intValue();
		int w = (new Double(se.getWidth() * sf)).intValue();
		int h = (new Double(se.getHeight() * sf)).intValue();

		Graphics2D graph = bi.createGraphics();
		graph.setColor(color);
		graph.setStroke(new BasicStroke(stroke));
		graph.draw(new Rectangle(x, y, w, h));
		graph.dispose();

	}

	static void outlineWordsOnImage(BufferedImage bi, PageBlock pg, ChunkBlock cb, double sf, Color color,
			float stroke) {

		drawSpatialEntityOnImage(bi, cb, sf, color, stroke);
		List<SpatialEntity> wordsInBlock = pg.intersectsByType(cb, SpatialOrdering.MIXED_MODE, WordBlock.class);
		for (SpatialEntity se : wordsInBlock) {
			WordBlock wb = (WordBlock) se;
			drawSpatialEntityOnImage(bi, se, sf, color, stroke);
		}
	}

	static BufferedImage deepCopy(BufferedImage bi) {
		ColorModel cm = bi.getColorModel();
		boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
		WritableRaster raster = bi.copyData(null);
		return new BufferedImage(cm, raster, isAlphaPremultiplied, null);
	}

}

package edu.isi.bmkeg.lapdf.bin;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.isi.bmkeg.lapdf.controller.LapdfEngine;
import edu.isi.bmkeg.lapdf.xml.model.LapdftextXMLChunk;
import edu.isi.bmkeg.lapdf.xml.model.LapdftextXMLDocument;
import edu.isi.bmkeg.lapdf.xml.model.LapdftextXMLPage;
import edu.isi.bmkeg.lapdf.xml.model.LapdftextXMLWord;
import edu.isi.bmkeg.utils.Converters;
import edu.isi.bmkeg.utils.xml.XmlBindingTools;

public class BlocksToText {

	private static String USAGE = "usage: <input-dir-or-file> [<output-dir>] \n\n"
			+ "<input-dir-or-file> - the full path to the PDF file or directory to be extracted \n"
			+ "<output-dir> (optional or '-') - the full path to the output directory \n"
			+ "Running this command on a block file or directory will extract and render blocks of text annotated with section.\n";

	public static void main(String args[]) throws Exception {

		LapdfEngine engine = new LapdfEngine();

		if (args.length < 1) {
			System.err.println(USAGE);
			System.exit(1);
		}
		String[] triggerStrings = {"Project Title\\:", 
		                           "Limited to one sentence",
		                           "Limited to 250 words",
		                           "Reference"};
		                           
		
		String inputFileOrDirPath = args[0];
		String outputDirPath = "";
		String ruleFilePath = "";

		File inputFileOrDir = new File(inputFileOrDirPath);
		if (!inputFileOrDir.exists()) {
			System.err.println(USAGE);
			System.err.println("Input file / dir '" + inputFileOrDirPath
					+ "' does not exist.");
			System.err.println("Please include full path");
			System.exit(1);
		}

		// output folder is set.
		if (args.length > 1) {
			outputDirPath = args[1];
		} else {
			outputDirPath = "-";
		}

		if (outputDirPath.equals("-")) {
			if (inputFileOrDir.isDirectory()) {
				outputDirPath = inputFileOrDirPath;
			} else {
				outputDirPath = inputFileOrDir.getParent();
			}
		}

		File outDir = new File(outputDirPath);
		if (!outDir.exists()) {
			outDir.mkdir();
		}

		// output folder is set.
		File ruleFile = null;
		if (args.length > 2) {
			ruleFilePath = args[2];
		} else {
			ruleFilePath = "-";
		}

		if (ruleFilePath.equals("-")) {
			ruleFile = Converters
					.extractFileFromJarClasspath(".", "rules/general.drl");
		} else {
			ruleFile = new File(ruleFilePath);
		}

		if (!ruleFile.exists()) {
			System.err.println(USAGE);
			System.err.println(ruleFilePath + " does not exist.");
			System.err.println("Please include full path");
		}

		if (inputFileOrDir.isDirectory()) {

			int i = 0; 
			FileWriter tsv_writer = new FileWriter(outputDirPath+"/titles_abstracts.tsv");
			tsv_writer.write("file\ttitle\tgoal\tabstract\n");

			Pattern patt = Pattern.compile("_lapdf\\.xml$");
			Map<String, File> inputFiles = Converters.recursivelyListFiles(
					inputFileOrDir, patt);
			Iterator<String> it = inputFiles.keySet().iterator();
			while (it.hasNext()) {
				String key = it.next();
				File xml = inputFiles.get(key);
				String xmlStem = xml.getName();
				xmlStem = xmlStem.replaceAll("_lapdf\\.xml", "");
				tsv_writer.write(xmlStem +'\t');

				String outTxtPath = Converters.mimicDirectoryStructure(
						inputFileOrDir, outDir, xml).getPath();
				outTxtPath = outTxtPath.replaceAll("_lapdf\\.xml", "")
						+ "_lapdf.txt";
				File outTxtFile = new File(outTxtPath);

		      	FileReader reader = new FileReader(xml);
		      	LapdftextXMLDocument xmlDoc = XmlBindingTools.parseXML(reader, LapdftextXMLDocument.class);
		      	
		      	Map<String,LapdftextXMLChunk> chunkMap = new HashMap<String,LapdftextXMLChunk>();
		      	FileWriter writer = new FileWriter(outTxtFile);
		      	for(LapdftextXMLPage p : xmlDoc.getPages() ) {	
		      		for(LapdftextXMLChunk c : p.getChunks() ) {
			      		String cSig = "p"+String.format("%03d", p.getPageNumber())+"_y"+String.format("%05d", c.getY())+"_x"+String.format("%05d", c.getX());
			      		chunkMap.put(cSig, c);
		      		}
		      	}
		      	
		      	List<String> keyList = new ArrayList<String>(chunkMap.keySet());
		      	Collections.sort(keyList);
		      	boolean triggered = false;
		      	for(String k : keyList ) {
		      		LapdftextXMLChunk c = chunkMap.get(k);
		      		String cText = "";
		      		for(LapdftextXMLWord w : c.getWords() ) {
		      			writer.append(w.getT() + ' ');	
		      			cText += w.getT() + ' ';
		      		}
	      			writer.append('\n');
	      			Pattern p = Pattern.compile(triggerStrings[i]);
	      			Matcher m = p.matcher(cText);
	      			if(m.find()) {
	      				triggered = true;
	      			} else if (triggered) {
	      				i++;
	      				triggered = false;
	      				tsv_writer.write(cText + '\t');
	      			}
		      	}
		      	writer.close();
		      	i=0;
		      	triggered=false;
  				tsv_writer.write('\n');
				
			}
			tsv_writer.close();
			

		} else {

			String xmlStem = inputFileOrDir.getName();
			xmlStem = xmlStem.replaceAll("\\_lapdf.xml", "");

			String outPath = outDir + "/" + xmlStem + "_lapdf.txt";
			File outTxtFile = new File(outPath);

	      	FileReader reader = new FileReader(inputFileOrDir);
	      	LapdftextXMLDocument xmlDoc = XmlBindingTools.parseXML(reader, LapdftextXMLDocument.class);
	      	
	      	FileWriter writer = new FileWriter(inputFileOrDir);
	      	for(LapdftextXMLPage p : xmlDoc.getPages() ) {
	      		for(LapdftextXMLChunk c : p.getChunks() ) {
	      			//writer.append(c.getType() + '\t');
		      		for(LapdftextXMLWord w : c.getWords() ) {
		      			writer.append(w.getT() + ' ');	
		      		}
	      			writer.append('\n');	
	      		}
	      	}
	      	writer.close();
						
		}
		
	}
	
}

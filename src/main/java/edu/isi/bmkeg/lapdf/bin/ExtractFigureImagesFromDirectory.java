package edu.isi.bmkeg.lapdf.bin;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import edu.isi.bmkeg.lapdf.controller.LapdfEngine;

/**
 * This script runs through the digital library and extracts all fragments for a
 * given corpus
 * 
 * @author Gully
 * 
 */
public class ExtractFigureImagesFromDirectory {

	public static class Options {

		@Option(name = "-outDir", usage = "Output", required = true, metaVar = "OUTPUT")
		public File outdir;

		@Option(name = "-pdfDir", usage = "PDF", required = true, metaVar = "PDF")
		public File pdfDir;

	}

	private static Logger logger = Logger.getLogger(ExtractFigureImagesFromDirectory.class);

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {

		Options options = new Options();
		Pattern patt = Pattern.compile("^(Figure|Fig\\.{0,1})\\s+(\\d+)");

		CmdLineParser parser = new CmdLineParser(options);

		try {

			parser.parseArgument(args);

			LapdfEngine eng = new LapdfEngine();
			//URL u = parser.getClass().getClassLoader().getResource("rules/basicRules.xls");
			//eng.setRuleFile(new File(u.getPath()));
			Pattern pattern = Pattern.compile("\\.(.*)$");
			
			String[] fileTypes = {"pdf"};
			
			@SuppressWarnings("unchecked")
			Iterator<File> it = FileUtils.iterateFiles(options.pdfDir, fileTypes, true);
			List<File> files = new ArrayList<File>();
			while( it.hasNext() ) 
				files.add(it.next());
				
			ListIterator<File> li = files.listIterator(files.size());
			while(li.hasPrevious()) {
				File pdf = li.previous();
				
				String newPath = pdf.getPath().replaceAll(
						options.pdfDir.getPath(),
						options.outdir.getPath());
				
				newPath = newPath.substring(0, newPath.indexOf(".pdf"));
				String stem = pdf.getName().substring(0,pdf.getName().length()-4);
				File newDir = new File(newPath);
				
				if(!newDir.exists())
					newDir.mkdirs();
				
				try {					
					eng.extractFiguresFromArticle(pdf, newDir, stem);					
				} catch (Exception e) {
					e.printStackTrace();
				}
			
			}

		} catch (CmdLineException e) {

			System.err.println(e.getMessage());
			System.err.print("Arguments: ");
			parser.printSingleLineUsage(System.err);
			System.err.println("\n\n Options: \n");
			parser.printUsage(System.err);
			System.exit(-1);

		} catch (Exception e2) {

			e2.printStackTrace();

		}

	}

}

# Layout Aware PDF (LAPDF) Extraction 

## Installation Instructions 

This is a Maven project and should be installed by issuing the following commands:
```
$ git clone https://github.com/SciKnowEngine/lapdftext/
$ cd lapdftext
$ mvn clean install assembly:assembly
```
This will build the `jar` archive file: `target/lapdftext-1.8.0-SNAPSHOT-jar-with-dependencies.jar`

You can execute commands against this library to run extraction tasks from PDF files. 

## Command-line functionality 

Executing commands against the assembled jar file takes the form: 
```
java -cp path/to/lapdftext-1.8.0-SNAPSHOT-jar-with-dependencies.jar edu.isi.bmkeg.lapdf.bin.<COMMAND> options
```
where `COMMAND` could be 

* Blockify - constructs text blocks from PDF files and outputs them as XML-formatted files.
* BlockifyClassify - executes Blockify but also runs rule-based classification on blocks
* BlockStatistics - provides statistics about each block
* ExtractFigureImagesFromFile - extracts images of figures from PDF-based scientific articles. 

Details of each command is described in usage documentation available by running the code without options. 

package com.thed.plugin.util;

import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.IOCase;
import org.apache.commons.io.comparator.LastModifiedFileComparator;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.lang.StringUtils;

public class FileUtils {
	
	private static final String BASE_PATH = "c:\\automation\\qtp";
	private static final String RUNNER_SCRIPT = "option Explicit \n" +
			"Dim qtApp, testPath\n" +
			"Set qtApp = CreateObject(\"QuickTest.Application\") \n" +
			"qtApp.Launch\n" +
			"qtApp.Visible = True\n" +
			"testPath = {0} \n" +
			"qtApp.open testPath,True \n" +
			"Dim qtTest \n" +
			"Set qtTest = qtApp.Test \n" +
			"qtTest.Run \n" +
			//"'qtTest.Close \n" +	//Causes issues, hence commented
			"qtApp.Quit \n" +
			"set qtTest = Nothing \n" +
			"set qtApp = Nothing";
	
	private static Logger logger = Logger.getLogger(FileUtils.class.getName());

	public static String createRunner(String testPath){
		File baseFile = new File(BASE_PATH);
		if(!baseFile.exists()){
			baseFile.mkdirs();
		}
		
		File tmpRunnerFile = new File(baseFile, "qtpRunner.vbs");
		FileWriter writer = null;
		try {
			writer = new FileWriter(tmpRunnerFile, false);
			writer.write(MessageFormat.format(RUNNER_SCRIPT, testPath));
			writer.flush();
			return tmpRunnerFile.getAbsolutePath();
		} catch (IOException e) {
			logger.log(Level.SEVERE, "ERROR in CREATING RUNNER Script, TEST WILL ABORT NOW !!!! ", e);
		}finally{
			try {
				writer.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return null;
	}
	
	public static File findCurrentResultFolder(String testPath){
		testPath = StringUtils.removeStart(testPath, "\"");
		testPath = StringUtils.removeEnd(testPath, "\"");
		
		File dir = new File(testPath);
		FilenameFilter fileFilter = FileFilterUtils.makeDirectoryOnly(FileFilterUtils.prefixFileFilter("Res", IOCase.INSENSITIVE));
		
		logger.log(Level.INFO, "Test path is " + testPath);
		
		File[] files = dir.listFiles(fileFilter);

		/** The newest file comes first **/
		if(files != null && files.length > 0){
			Arrays.sort(files, LastModifiedFileComparator.LASTMODIFIED_REVERSE);
			logger.log(Level.INFO, "Returning RESULT FILE " + files[0].getName());
			return files[0];
		}
		return null;
	}
	
	public static void main(String [] args){
		System.out.println(FileUtils.findCurrentResultFolder("\"C:\\shared\\irwin-mitchel\""));
	}

}

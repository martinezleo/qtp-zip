/**
* ////////////////////////////////////////////////////////////////////////////////
* //
* //  D SOFTWARE INCORPORATED
* //  Copyright 2007-2013 D Software Incorporated
* //  All Rights Reserved.
* //
* //  NOTICE: D Software permits you to use, modify, and distribute this file
* //  in accordance with the terms of the license agreement accompanying it.
* //
* //  Unless required by applicable law or agreed to in writing, software
* //  distributed under the License is distributed on an "AS IS" BASIS,
* //  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* //
* ////////////////////////////////////////////////////////////////////////////////
*/

package com.thed.plugin;

import java.io.File;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.text.StrTokenizer;
import org.apache.cxf.helpers.FileUtils;

import com.thed.launcher.DefaultZBotScriptLauncher;
import com.thed.launcher.IZBotScriptLauncher;
import com.thed.plugin.runner.ParallelRunner;
import com.thed.util.CodecUtils;
import com.thed.util.ScriptUtil;
import com.thed.util.Utils;

public class QtpZephyrInterceptor extends DefaultZBotScriptLauncher{

	private String outputPath = null;
	private String comments = null;
	private Logger logger = Logger.getLogger(QtpZephyrInterceptor.class.getName());
	private Integer currentTcExecutionResult;
	private String type = "qtp";
	private List<Thread> threads;
	private WebServiceUtils util;
	
	/**
	 * This method grabs the username, password and Zephyr server URL from the properties file and use it to login this ZIP into Zephyr using it
	 */
	public QtpZephyrInterceptor() {
		super();
		threads = new ArrayList<Thread>();
		try{
			String encPwd = Utils.getZbotProperties().getString(Utils.ENC_PWD_KEY);
			String pwd = CodecUtils.decrypt(encPwd);
			util = new WebServiceUtils(Utils.getZbotProperties().getString("zephyrServerURL") + "/flex/services/soap/zephyrsoapservice-v1?wsdl", 
					Utils.getZbotProperties().getString(Utils.USERNAME_KEY), pwd);
		}catch(Exception ex){
			ex.printStackTrace(System.out);
		}
		
	}
	
	/**
	 * This method gets called once for each testcase and is the start of the automation for that testcase.
	 * In this custom ZIP, we are able to make to determine based on the provided script path whether the
	 * automation being ran is QTP or Selenium
	 * It updates the Zbot agent real-time status with the current testcase id and name
     * to <b>'STATUS: Script execution starting...'</b> before executing the testcase.
	 */
	@Override
	public void testcaseExecutionStart() { 
		currentTcExecutionResult = null;
		comments = "";
		if((currentTestcaseExecution.getScriptPath() != null)){
			String[] arguments = new StrTokenizer(currentTestcaseExecution.getScriptPath(), ' ', '"').getTokenArray();

			if(currentTestcaseExecution.getScriptPath().contains("cmdrv.exe") || currentTestcaseExecution.getScriptPath().contains("qtRunner")){
				type = "qtp";
				outputPath = arguments[arguments.length -1];
			}else{
				type = "selenium";
				if(arguments.length > 2){
					outputPath = arguments[arguments.length -1] + File.separator +  arguments[arguments.length-2] + "_result.txt";
				}else{
					outputPath = "";
				}
			}
			logger.info("Test type is determined " + type);
			logger.info("outputPath " + outputPath);
		}
	}
	
	/**
	* This method handles the parallel (all test cases in a batch launched in parallel without waiting for the completion of previous test case) and	
	* sequential (a test case waits for the completion of previous test case in teh batch) execution of testcases
	* and updates the current test case execution result to 'pass' or 'fail'.
	* <br/>
	* Here the zbot agent real-time status message is updated to <b>'STATUS: Script Executing'</b> and
	* <b>'STATUS: Waiting for execution completion'</b>, which can be seen in the Zephyr UI.
	*/

	@Override
	public void testcaseExecutionRun() { 
		try {
			status = "Script "+currentTestcaseExecution.getTcId() + ": "+ currentTestcaseExecution.getName() +" STATUS: Script Executing";
			logger.info("about to run script with path: " + currentTestcaseExecution.getScriptPath());
			
			if((currentTestcaseExecution.getScriptPath() != null)){
			    Process process = Runtime.getRuntime().exec(currentTestcaseExecution.getScriptPath() + " " + currentTestcaseExecution.getReleaseTestScheduleId() + " " + url);
			
			    /* logic to execute testcases sequentially. i.e. next testcase waits till first testcase is finished */
			    if (testcaseBatchExecution.isParallelExecution()) {
			    	ParallelRunner pRunner = new ParallelRunner(type, outputPath, currentTestcaseExecution, agent, url);
			    	pRunner.start();
			    	threads.add(pRunner);
					logger.info("started process (parallel): " + currentTestcaseExecution.getScriptPath());
					System.out.println("Launching in parallel...");
				    currentTestcaseExecution.setStatus("Script Executing");
			    } else {
					status = currentTestcaseExecution.getTcId() + ": "+ currentTestcaseExecution.getName() +" STATUS: Waiting for execution completion";
					currentTestcaseExecution.setStatus("Waiting for execution completion");
					@SuppressWarnings("unused")
					int status = process.waitFor() ;
				    currentTestcaseExecution.setStatus("Script successfully executed");
			    }
			}
		} catch (Exception e) {
			currentTestcaseExecution.setStatus(currentTestcaseExecution.getTcId() + ": "+ currentTestcaseExecution.getName() +" STATUS: Execution Error " + e.getMessage());
			status = currentTestcaseExecution.getTcId() + ": "+ currentTestcaseExecution.getName() +" STATUS: Execution Error " + e.getMessage();
			logger.log(Level.WARNING, "exception in script execution. " + e.getMessage());
		    e.printStackTrace();
		    
			currentTcExecutionResult = new Integer(2);;	/* 2 = fail */
			comments = "Error executing testcase " + currentTestcaseExecution.getTcId() + " on " + agent.getAgentHostAndIp(); 
		}
	}
	
	/**
	 * This method updates the Zbot agent real-time status to <b>'STATUS: Script Successfully Executed'</b> 
	 * and makes a Webservice call to update testcase execution result to the server <br/>
	 * @see {@link IZBotScriptLauncher#testcaseExecutionResult()}
	 */
	public void testcaseExecutionResult() {
		if (testcaseBatchExecution.isParallelExecution()){
			return;
		}
		if(currentTcExecutionResult == null){
			try{
				System.out.println(" *********************** Execution Result is null, lets calculate it");
				if(type.equals("qtp")){
					comments = outputPath;
					logger.info("**************** Output path is " + outputPath);
					currentTcExecutionResult = new Integer(getQTPTCResult(outputPath)); /* 1 = pass */
					File file = new File(outputPath + File.separator + "output.txt");
					if(util != null)
						util.uploadAttachment(file, "releaseTestSchedule", Long.parseLong(currentTestcaseExecution.getReleaseTestScheduleId()));
				}else{
					currentTcExecutionResult = new Integer(getSeleniumResult(outputPath)); /* 1 = pass */
				}
				comments += " \n Successfully executed on " + agent.getAgentHostAndIp();
			}catch(Exception ex){
				ex.printStackTrace(System.out);
				currentTcExecutionResult = new Integer(2);;	/* 2 = fail */
				comments += " \n Error in parsing response,  TC:" + currentTestcaseExecution.getTcId() + " on " + agent.getAgentHostAndIp(); 
			}
		}
		status = currentTestcaseExecution.getTcId() + ": "+ currentTestcaseExecution.getName() + " STATUS: Script Successfully Executed";

		if(currentTcExecutionResult != null){
			comments = StringUtils.left(comments, 254);
			ScriptUtil.updateTestcaseExecutionResult(url, currentTestcaseExecution, currentTcExecutionResult, comments);
		}
	}
	
	/**
	 * This method sends output text file path and a keyword
	 * @param outputPath
	 * @return
	 * @throws Exception
	 */
	private int getQTPTCResult(String outputPath) throws Exception{
		return getResult(outputPath + File.separator + "output.txt", "Error:");
	}
	
	/**
	 * This method sends output text file path and a keyword
	 * @param outputPath
	 * @return
	 * @throws Exception
	 */
	private int getSeleniumResult(String outputPath) throws Exception{
		return getResult(outputPath, "FAILURES!!!");
	}
	
	/**
	 * Returns FAIL is the output file is now found otherwise will search the file for the keyword given.
	 * If keyword is found in the file, returns FAIL. If not found, returns PASS.
	 * Keyword is automation tool specific error message
	 * @param outputFile
	 * @param keyWord
	 * @return
	 * @throws Exception
	 */
	private int getResult(String outputFile, String keyWord)throws Exception{
		File file = new File(outputFile);
		String path = outputFile.substring(outputFile.indexOf("C:\\automation\\") + 15).replace("\\", "/");
		String url =  "http://" + InetAddress.getLocalHost().getHostAddress() +"/" + path;
		if(!file.exists()){
			comments = "Can't determine testcase result, Output file is unavailable " + file.getAbsolutePath();
			System.out.println("OUTPUT NOT FOUND, RETURNING FALSE");
			return 2;
		}
		
		@SuppressWarnings("rawtypes")
		List lines = FileUtils.readLines(file);
		logger.info("total lines are" + (lines==null?"NULL":lines.size()));
		for (Object line : lines){
			logger.info("Line is " + line);
			System.out.println("line " + line);
			if(line != null && ((String)line).startsWith(keyWord)){
				comments = "Found Error, Output can be accessed at " + url;
				return 2;
			}
		}
		comments = "Success, Output can be accessed at " + url;
		return 1;
	}
	
	/**
	 * This method the ZBot agent real-time status to <b>'Batch execution completed'</b>. 
	 * The completion of batch is logged for inspection.
	 */
	public void batchEnd() {
		if (testcaseBatchExecution.isParallelExecution()){
			for(Thread t : threads){
				try {
					logger.info("Waiting for all the testcases to finish...");
					t.join();
					/**
					 * provide implementation
					 */
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		System.out.println("All Done ...");
		super.batchEnd();	
	}
}

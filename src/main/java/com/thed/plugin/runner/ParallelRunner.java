package com.thed.plugin.runner;

import java.io.File;
import java.net.InetAddress;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.cxf.helpers.FileUtils;

import com.thed.launcher.IZBotScriptLauncher;
import com.thed.model.Agent;
import com.thed.model.TestcaseExecution;
import com.thed.util.ScriptUtil;

public class ParallelRunner extends Thread {
	private String type;
	private String outputPath;
	private String comments = null;
	/* current testcase execution information model object */
	private final TestcaseExecution currentTestcaseExecution;
	private final Agent agent;
	private String url;
	
	private Integer currentTcExecutionResult;
	private Logger logger = Logger.getLogger(ParallelRunner.class.getName());
	
	/**
	 * @param type
	 * @param outputPath
	 * @param currentTestcaseExecution
	 * @param agent
	 * @param url
	 */
	public ParallelRunner(String type, String outputPath,
			TestcaseExecution currentTestcaseExecution, Agent agent, String url) {
		super();
		this.type = type;
		this.outputPath = outputPath;
		this.currentTestcaseExecution = currentTestcaseExecution;
		this.agent = agent;
		this.url = url;
	}

	public void run() {
		try {
			logger.info("Kicking off the process...");
			Process process = Runtime.getRuntime().exec(currentTestcaseExecution.getScriptPath());
			logger.info("testcase started the process... " + Thread.currentThread().getName());
			process.waitFor();
			logger.info("testcase Finished " + Thread.currentThread().getName());
			try {
				logger.info("waiting for files to get written by the testcase...");
				Thread.sleep(30000);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			testcaseExecutionResult();
		} catch (Exception e) {
			currentTestcaseExecution.setStatus(currentTestcaseExecution.getTcId() + ": "+ currentTestcaseExecution.getName() +" STATUS: Execution Error " + e.getMessage());
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
		if(currentTcExecutionResult == null){
			try{
				if(type.equals("qtp")){
					currentTcExecutionResult = new Integer(getQTPTCResult(outputPath)); /* 1 = pass */
				}else{
					System.out.println("Fetching selenium testcase results...");
					currentTcExecutionResult = new Integer(getSeleniumResult(outputPath)); /* 1 = pass */
					System.out.println("Result is " + currentTcExecutionResult + " for thread " + Thread.currentThread().getName());
				}
				comments += " \n Successfully executed on " + agent.getAgentHostAndIp();
			}catch(Exception ex){
				currentTcExecutionResult = new Integer(2);;	/* 2 = fail */
				comments += " \n Error in parsing response,  TC:" + currentTestcaseExecution.getTcId() + " on " + agent.getAgentHostAndIp(); 
			}
		}
		if(currentTcExecutionResult != null){
			ScriptUtil.updateTestcaseExecutionResult(url, currentTestcaseExecution, currentTcExecutionResult, comments);
		}
	}
	
	private int getQTPTCResult(String outputPath) throws Exception{
		return getResult(outputPath + File.separator + "output.txt", "Error");
	}
	
	/**
	 * To be implemented
	 * @param outputPath
	 * @return
	 * @throws Exception
	 */
	private int getSeleniumResult(String outputPath) throws Exception{
		return getResult(outputPath, "FAILURES!!!");
	}
	
	private int getResult(String outputFile, String keyWord)throws Exception{
		File file = new File(outputFile);
		String path = outputFile.substring(outputFile.indexOf("C:\\automation\\") + 15).replace("\\", "/");
		String url =  "http://" + InetAddress.getLocalHost().getHostAddress() +"/" + path;
		if(!file.exists()){
			comments = "Can't determine testcase result, Output file is unavailable " + file.getAbsolutePath();
			System.out.println("OUTPUT NOT FOUND, RETURNING FALSE");
			return 2;
		}
		if(!file.canWrite()){
			try {
				logger.info("waiting for files to get written by the testcase...");
				Thread.sleep(10000);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
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

}

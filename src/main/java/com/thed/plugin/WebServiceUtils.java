   /*  D SOFTWARE INCORPORATED
    *  Copyright 2007-2011 D Software Incorporated
    *  All Rights Reserved.
    *
    *  NOTICE: D Software permits you to use, modify, and distribute this file
    *  in accordance with the terms of the license agreement accompanying it.
    *
    *  Unless required by applicable law or agreed to in writing, software
    *  distributed under the License is distributed on an "AS IS" BASIS,
    *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    */

/*
 * This is a sample of what can be done by using Zephyr API to test Selenium test cases in the JAVA coding language.
 * 
 * Eclipse IDE for Java Developers- Version: Helios Service Release 2. Build id: 20110218-0911
 * Java- Java JDK 1.6.0_25
 * 
 * Author: Shailesh Mangal, Chief Architect, D Software Inc.
 * Co-author, Editor: Daniel Gannon, Technical Support Analyst, D Software Inc.
 */

// Change or delete package inclusion depending on working environment
package com.thed.plugin;

// Imported libraries
import java.awt.AWTException;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.xml.namespace.QName;

import com.thed.service.soap.client.*;

public class WebServiceUtils {
	
	// Setup a variable of QName initialized with Namespace URI and local
	private static final QName SERVICE_NAME = new QName("http://getzephyr.com/com/thed/services/soap/zephyrsoapservice", "ZephyrSoapService");
	
	// Setup a variable of ZephyrSoapService initialized with null
	private ZephyrSoapService _port = null;
	
	// Setup a variable of String to hold the session token
	private String token;
	
	/**
	 * Default constructor, brings in WSDL url, username, and password for Zephyr
	 * Calls login method, passes UN and PW, then gets session token
	 * @param url
	 * @param userName
	 * @param password
	 */
	public WebServiceUtils(String url, String userName, String password){
		System.out.println("Url is " + url + "" + userName + " pwd : " + password );
		URL wsdlURL = ZephyrSoapService_Service.WSDL_LOCATION;
		if (url != null) {
			File wsdlFile = new File(url);
			try {
				if (wsdlFile.exists()) {
					wsdlURL = wsdlFile.toURI().toURL();
				} else {
					wsdlURL = new URL(url);
				}
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}
		}
		ZephyrSoapService_Service ss = new ZephyrSoapService_Service(wsdlURL, SERVICE_NAME);
		_port = ss.getZephyrSoapServiceImplPort();
		try {
			System.out.println("Performing Login *************");
			token = login(userName, password);
			System.out.println(" Login Done ************* " + token);
		} catch (ZephyrServiceException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Login method, takes in UN and PW and returns session token on success
	 * @param _login_username
	 * @param _login_password
	 * @return
	 * @throws ZephyrServiceException
	 */
	private String login(String _login_username, String _login_password) throws ZephyrServiceException {
		System.out.println("Invoking login...");
		try {
			java.lang.String token = _port.login(_login_username, _login_password);
			System.out.println("login.result=" + token);
			return token;
		} catch (ZephyrServiceException e) {
			System.out.println("Exception encounterd during login, Details:");
			e.printStackTrace(System.out);
			throw e;
		}
	}
	
	/**
	 * Method for updating the TC test result after a completed test
	 * @param testExecutionId
	 * @param status
	 * @param notes
	 */
	public void updateTestResult(String testExecutionId, String status, String notes) {
		System.out.println("Invoking updateTestStatus...");
		java.util.List<RemoteTestResult> testResults = new ArrayList<RemoteTestResult>();
		RemoteTestResult testResult = new RemoteTestResult();
		testResult.setReleaseTestScheduleId(testExecutionId);
		testResult.setExecutionStatus(status);
		testResult.setExecutionNotes(notes);
		testResults.add(testResult);
		try {
			java.util.List<RemoteFieldValue> statusUpdateResponse = _port.updateTestStatus(testResults, token);
			System.out.println("updateTestStatus.result=" + statusUpdateResponse);
		} catch (ZephyrServiceException e) {
			System.out.println("Expected exception: ZephyrServiceException has occurred.");
			System.out.println(e.toString());
		}
	}
	
	/**
	 * Default method for taking a screenshot. Calls the method to take a screenshot and returns screenshot path
	 * @return
	 * @throws AWTException
	 * @throws IOException
	 */
	public static String takeAScreenShotOfTheApp() throws AWTException, IOException {
    	return takeAScreenShotOfTheApp(System.getProperty("java.io.tmpdir"), "Image-" + System.currentTimeMillis());
    }
    
    /**
     * Packages screen shot and saves(.png) to file system, returning the path of the saved screen shot
     * @param parentDir
     * @param fileName
     * @return
     * @throws AWTException
     * @throws IOException
     */
    public static String takeAScreenShotOfTheApp(String parentDir, String fileName) throws AWTException, IOException {
	    Dimension screenDim = Toolkit.getDefaultToolkit().getScreenSize();
	    Rectangle screenBounds = new Rectangle(0, 0, screenDim.width, screenDim.height);

	    Robot robot = new Robot();
	    BufferedImage image =  robot.createScreenCapture(screenBounds);

	    File screenshotFile = new File(parentDir + File.separator
	                                   + fileName + ".png");
	    ImageIO.write(image, "png", screenshotFile);
	    
	    return screenshotFile.getAbsolutePath();
	}
    
    /**
     * 
     * @param executionId
     * @param stepIndex
     */
	void captureScreen(long executionId, int stepIndex) {
		try {
			String fileName = takeAScreenShotOfTheApp(System.getProperty("java.io.tmpdir"), "StepId-" + stepIndex);
			uploadAttachment(new File(fileName), "releaseTestSchedule", executionId);
		} catch (AWTException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ZephyrServiceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * Method for uploading an attachment from a test
	 * @param file
	 * @param entityType
	 * @param entityId
	 * @throws IOException
	 * @throws ZephyrServiceException
	 */
	public void uploadAttachment(File file, String entityType, Long entityId) throws IOException, ZephyrServiceException{
		System.out.println("**********************************file " + file.exists() + "" + file.getAbsolutePath());
		System.out.println("**********************************type" + entityType + " 	" + entityId);
		List<RemoteAttachment> remoteFiles = new ArrayList<RemoteAttachment>();
		RemoteAttachment remoteFile = new RemoteAttachment();
		byte[] bytes = getBytes(file);
		System.out.println("**********************************File has " + bytes.length + " Bytes");
		remoteFile.setAttachment(bytes);
		remoteFile.setFileName(file.getName());
		remoteFile.setEntityId(entityId);
		remoteFile.setEntityName(entityType);
		remoteFiles.add(remoteFile);
		_port.addAttachments(remoteFiles, token);
		System.out.println("Attachment done*********************");
	}

	/**
	 * 
	 * @param file
	 * @return
	 * @throws IOException
	 */
	private byte[] getBytes(File file) throws IOException {
		InputStream is = new FileInputStream(file);
		long length = file.length();
		// Create the byte array to hold the data
	    byte[] bytes = new byte[(int)length];

	    // Read in the bytes
	    int offset = 0;
	    int numRead = 0;
	    while (offset < bytes.length
	           && (numRead=is.read(bytes, offset, bytes.length-offset)) >= 0) {
	        offset += numRead;
	    }

	    // Ensure all the bytes have been read in
	    if (offset < bytes.length) {
	        throw new IOException("Could not completely read file "+file.getName());
	    }

	    // Close the input stream and return bytes
	    is.close();
	    return bytes;
	}
	
	/**
	 * Method for getting executionId from a testcase ID
	 * @param tcID
	 * @return
	 * @throws NumberFormatException
	 * @throws ZephyrServiceException
	 */
	public Long getExecutionId(String tcID) throws NumberFormatException, ZephyrServiceException 
	{
		/* 
		 * Takes in value and assigned to tcID
		*/
		//
		Long testSchedule = null;
		//
		String ID = tcID;
		
		// Effectively unused
		List<RemoteCriteria> sc = new ArrayList<RemoteCriteria>();
		RemoteCriteria sc0 = new RemoteCriteria();
		sc0.setSearchName("");
		sc0.setSearchOperation(SearchOperation.IN);
		sc0.setSearchValue("");
		sc.add(sc0);
		
		List<RemoteReleaseTestSchedule> TSbc;
		TSbc = _port.getTestSchedulesByCriteria(sc, true, token);
		
		RemoteReleaseTestSchedule RTS;
		
		if(TSbc.isEmpty() == false)
		{
			for(int i = 0; i < TSbc.size(); i++)
			{
				RTS = TSbc.get(i);
				if(RTS.getRemoteTestcaseId().toString().equalsIgnoreCase(ID))
				{
					testSchedule = RTS.getTestScheduleId();
				}
			}
		}
		
		// Return result, either test case executionId or null
		return testSchedule;
	}
	
	/**
	 * Logout method which ends a session
	 * Token for session no longer becomes valid after this call
	 */
	public void logout() {
		System.out.println("Invoking logout...");
		try {
			_port.logout(token);
		} catch (ZephyrServiceException e) {
			System.out.println("Expected exception: ZephyrServiceException has occurred.");
			System.out.println(e.toString());
		}
	}
}
	

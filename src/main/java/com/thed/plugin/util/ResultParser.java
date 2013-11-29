package com.thed.plugin.util;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class ResultParser {
	public static String parseResults(String path) throws XPathExpressionException, ParserConfigurationException, SAXException, IOException, ParseException {
		String scriptPath = path + File.separator + "Results.xml";
		File scriptFile = new File(scriptPath);
		return parseResults(scriptFile);
	}

	public static String parseResults(File resultFile)
			throws ParserConfigurationException, SAXException, IOException,
			XPathExpressionException, ParseException {
		XPath xpath = XPathFactory.newInstance().newXPath();
		Document resultDoc = getDoc(resultFile);
		NodeList result = (NodeList) xpath.compile("//NodeArgs[@status='Failed']").evaluate(resultDoc, XPathConstants.NODESET);
		System.out.println("Result is " + result.getLength());
		return result.getLength() > 0 ? "2" : "1" ;
	}
	
	public static void main(String args[]) throws XPathExpressionException, ParserConfigurationException, SAXException, IOException, ParseException{
		System.out.println(parseResults("C:\\qtp\\ZephyrSearchPass\\Report\\Report"));
	}

	private static Document getDoc(File xmlFile)
			throws ParserConfigurationException, SAXException, IOException,
			XPathExpressionException, ParseException {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		return builder.parse(xmlFile);
	}
}
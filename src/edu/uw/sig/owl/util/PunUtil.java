/**
 * 
 */
package edu.uw.sig.owl.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import edu.uw.sig.owl.PunningGenerator;

/**
 * @author detwiler
 * @date Dec 9, 2014
 *
 */
public class PunUtil
{
	private Map<String,String> inFile2OutFile = new HashMap<String,String>();
	
	private boolean readConfig(String configFilePath)
	{
		try
		{
			BufferedReader reader = new BufferedReader(new FileReader(configFilePath));
			String line = null;
	        while ((line=reader.readLine()) != null) 
	        {
	        	String[] lineParts = line.split("\\s+");
	        	if(lineParts.length!=2)
	        	{
	        		System.err.println("bad line in config file: "+line);
	        	}
	        	
	        	String inFilePath = lineParts[0];
	        	String outFilePath = lineParts[1];
	        	
	        	/*
	        	if(inFilePath==null|outFilePath==null)
	        	{
	        		System.err.println("bad config file syntax");
	        		return false;
	        	}
	        	
	        	File inFile = new File(inFilePath);
	        	if(!inFile.canRead())
	        	{
	        		System.err.println("input file not found");
	        		return false;
	        	}
	        	
	        	File outFile = new File(outFilePath);
	        	outFile.mkdirs();
	        	outFile.createNewFile();
	        	if(!outFile.canWrite())
	        	{
	        		System.err.println("unable to create output file");
	        		return false;
	        	}
	        	*/
	        	
	        	
	        	inFile2OutFile.put(inFilePath,outFilePath);
	        }
		}
		catch (FileNotFoundException e)
		{
			e.printStackTrace();
			return false;
		}
		catch(IOException e)
		{
			e.printStackTrace();
			return false;
		}
		
		return true;
	}
	
	public boolean run(String configFilePath)
	{
		if(!readConfig(configFilePath))
		{
			System.err.println("error reading configuration file");
			return false;
		}
		
		for(String inputFilePath : inFile2OutFile.keySet())
		{
			String outputFilePath = inFile2OutFile.get(inputFilePath);
			PunningGenerator pungen = new PunningGenerator();
			pungen.run(inputFilePath, outputFilePath);
			pungen.savePunOnt();
		}
		
		return true;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		PunUtil util = new PunUtil();
		util.run(args[0]);

	}

}

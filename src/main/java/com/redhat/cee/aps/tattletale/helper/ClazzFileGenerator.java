package com.redhat.cee.aps.tattletale.helper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;

/**
 * @author lywang
 * Generate the .clz file which is used by Tattletale report tool
 * Update the "tattletale-helper.properties" and run this class 
 *
 */
public class ClazzFileGenerator {
	static private Properties configProps;
	static private Logger logger = Logger.getLogger(ClazzFileGenerator.class.getName());

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		
		if (!loadCustomConfig()) {
			logger.info("No custom \"tattletale-helper.properties\" found, using the one packaged inside tattletale-helper jar file ... ...");
			loadDefaultConfig();
		} 

		if (configProps.size() < 3) {
			logger.log(Level.SEVERE, "Configuration Incomplete!");
			return;
		}
		logger.info("Configuration Loaded: " + configProps.size() + " properties");

		String actionType = configProps.getProperty("action");
		String inputPath = configProps.getProperty("input.path");
		String outputFolder = configProps.getProperty("output.path");

		StringBuffer sbClzList = new StringBuffer();

		if (actionType.equals("EAP")) {
			String eapName = inputPath.substring(inputPath.indexOf("/jboss-eap-") + 1, inputPath.indexOf("/modules/"));
			logger.info("Processing EAP module: " + eapName);
			sbClzList = generateEAPModuleclazz(eapName, inputPath);
			logger.info("Writing report file: " + outputFolder + eapName + ".clz");
			writeClzFile(outputFolder + eapName + ".clz", sbClzList);

		} else if (actionType.equals("EE")) {
			String eeName = inputPath.substring(inputPath.indexOf("/javaee-api-"));
			logger.info("Processing EE API file: " + eeName);
			sbClzList = generateEEFileclazz(eeName, inputPath);
			logger.info("Writing report file: " + outputFolder + eeName + ".clz");
			writeClzFile(outputFolder + eeName + ".clz", sbClzList);
		}
		logger.info("=== Processing Finished ===");

	}

	/**
	 * Iterate the javaee-api-x.0.jar file and generate the class file list
	 * filePath: absolute path for javaee-api jar file, loaded from
	 * configuration file
	 * 
	 */
	private static StringBuffer generateEEFileclazz(String eeName, String filePath) {
		StringBuffer sbClzList = new StringBuffer();

		JarInputStream jarInputStream = null;

		try {
			File eeAPIFile = new File(filePath);
			jarInputStream = new JarInputStream(new FileInputStream(eeAPIFile));
			JarEntry jarEntry = jarInputStream.getNextJarEntry();
			String tmpClassName = "";
			while (null != jarEntry) {
				if (jarEntry.getName().endsWith(".class")) {
					tmpClassName = jarEntry.getName().replaceAll("/", "\\.");
					tmpClassName = tmpClassName.substring(0, tmpClassName.indexOf(".class"));
					sbClzList.append(tmpClassName + "\n");
				}
				jarEntry = jarInputStream.getNextJarEntry();
			}
		} catch (Exception e) {
			logger.log(Level.SEVERE, e.getMessage());
			e.printStackTrace();
		} finally {
			if (null != jarInputStream) {
				try {
					jarInputStream.close();
				} catch (IOException ioe) {
					logger.log(Level.SEVERE, ioe.getMessage());
					ioe.printStackTrace();
				}
			}
		}
		return sbClzList;
	}

	/**
	 * Iterate the EAP module folder and generate the class file list
	 * input.path: the EAP shipped module path "modules/system/layers/base/",
	 * absolute path is loaded from configuration file output.path: output
	 * folder for the clz file list
	 */
	private static void writeClzFile(String fileName, StringBuffer report) {
		try {
			FileUtils.writeStringToFile(new File(fileName), report.toString(), "UTF-8", false);
		} catch (IOException ioe) {
			logger.log(Level.SEVERE, ioe.getMessage());
			ioe.printStackTrace();
		}
	}

	/**
	 * Iterate the EAP module folder and generate the class file list
	 * inputFolder: absolute path for EAP shipped modules
	 * "${JBOSS_HOME}/modules/system/layers/base/", loaded from configuration
	 * file
	 * 
	 */
	private static StringBuffer generateEAPModuleclazz(String eapName, String inputFolder) {
		StringBuffer sbClzList = new StringBuffer();

		File currentJarFile = null;
		String currentJarPath = "";
		JarInputStream jarInputStream = null;

		try {
			Iterator<File> fileIterator = FileUtils.iterateFiles(new File(inputFolder), new String[] { "jar" }, true);
			while (fileIterator.hasNext()) {
				currentJarFile = fileIterator.next();

				currentJarPath = currentJarFile.getAbsolutePath();
				currentJarPath = currentJarPath.substring(currentJarPath.indexOf("/modules/"));
				sbClzList.append(eapName + currentJarPath + "=\n");

				jarInputStream = new JarInputStream(new FileInputStream(currentJarFile));
				JarEntry jarEntry = jarInputStream.getNextJarEntry();
				String tmpClassName = "";
				while (null != jarEntry) {
					if (jarEntry.getName().endsWith(".class")) {
						tmpClassName = jarEntry.getName().replaceAll("/", "\\.");
						tmpClassName = tmpClassName.substring(0, tmpClassName.indexOf(".class"));
						sbClzList.append(tmpClassName + "\n");
					}
					jarEntry = jarInputStream.getNextJarEntry();
				}
			}
		} catch (Exception e) {
			logger.log(Level.SEVERE, e.getMessage());
			e.printStackTrace();
		} finally {
			if (null != jarInputStream) {
				try {
					jarInputStream.close();
				} catch (IOException ioe) {
					logger.log(Level.SEVERE, ioe.getMessage());
					ioe.printStackTrace();
				}
			}
		}
		return sbClzList;
	}

	/**
	 * Load default configuration file "tattletale-helper.properties" packaged inside jar file 
	 * @return true: success; false: failure
	 * 
	 */
	private static boolean loadCustomConfig() {
		
		boolean customLoaded = false;
		String configFile = "tattletale-helper.properties";
		InputStream is = null;
		
		Properties properties = new Properties();
		String customPath = "";
		try {
			customPath = ClazzFileGenerator.class.getProtectionDomain().getCodeSource().getLocation().getPath();
			if (customPath.endsWith(".jar")) {
				customPath = customPath.substring(0, customPath.lastIndexOf("/")+1);
			}
			
			logger.info("Searching for custom configuration file in current path : " + customPath + configFile);
			is = new FileInputStream(new File(customPath + configFile));
			
			properties.load(is);
			configProps = properties;
			customLoaded = true;
			logger.info("Custom configuration loaded from: " + customPath +  "tattletale-helper.properties");
			
		} catch (FileNotFoundException nfEx) {
			return false;
			
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Unable to load " + configFile);
			e.printStackTrace();
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (IOException ioe) {
					logger.log(Level.SEVERE, ioe.getMessage());
					ioe.printStackTrace();
				}
			}
		}
		return customLoaded;
	}

	/**
	 * Load custom configuration file "tattletale-helper.properties" which sits in same folder with jar file 
	 * 
	 */
	private static void loadDefaultConfig() {
		Properties properties = new Properties();
		String configFile = "tattletale-helper.properties";

		InputStream is = null;
		try {
			is = ClazzFileGenerator.class.getClassLoader().getResourceAsStream(configFile);
			properties.load(is);
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Unable to load " + configFile);
			e.printStackTrace();
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (IOException ioe) {
					logger.log(Level.SEVERE, ioe.getMessage());
					ioe.printStackTrace();
				}
			}
		}
		configProps = properties;
	}
	
}

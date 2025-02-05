package edu.lu.uni.serval.config;

/**
 * Configuration of PAR.
 * 
 * @author kui.liu
 *
 */
public class Configuration {

	public static String knownBugPositions = "BugPositions.txt";
	public static String suspPositionsFilePath = "SuspiciousCodePositions/";
	public static String failedTestCasesFilePath = "FailedTestCases/";
	public static String faultLocalizationMetric = "Ochiai";
	public static String outputPath = "OUTPUT/";
	public static int maxnrpatches = 1000;

	public static final String TEMP_FILES_PATH = ".temp/";
	public static final long SHELL_RUN_TIMEOUT = 10800L;

}

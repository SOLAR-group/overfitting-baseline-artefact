package edu.lu.uni.serval.main;

import java.io.File;

import edu.lu.uni.serval.bug.fixer.AbstractFixer;
import edu.lu.uni.serval.bug.fixer.File_kParFixer;
import edu.lu.uni.serval.bug.fixer.Line_kParFixer;
import edu.lu.uni.serval.bug.fixer.Method_kParFixer;
import edu.lu.uni.serval.config.Configuration;

/**
 * Fix bugs with the known bug positions at three level of granularity: line,
 * method, file.
 * 
 * @author kui.liu
 *
 */
public class Main_Pos {

	private static Granularity granularity = Granularity.Line;

	enum Granularity {
		Line, Method, File
	}

	public static void main(String[] args) {
		if (args.length != 6) {
			System.out.println(
					"Arguments: <Failed_Test_Cases_File_Path> <Known_Bug_Positions_File> <Buggy_Project_Path> <defects4j_Path> <Project_Name> <Suspicious_Code_Positions_File_Path> <Line/Method/File>");
			System.exit(0);
		}
		// Configuration.failedTestCasesFilePath = args[0];
		// Configuration.knownBugPositions = args[1];
		// Configuration.suspPositionsFilePath = args[5];

		String buggyProjectsPath = args[0];// "../Defects4JData/"
		String defects4jPath = args[1]; // "../defects4j/"
		String projectName = args[2]; // "Chart_1"
		System.out.println(projectName);

		String granularityStr = args[3];
		if ("line".equalsIgnoreCase(granularityStr) || "l".equalsIgnoreCase(granularityStr)) {
			granularity = Granularity.Line;
			Configuration.outputPath += "Line/";
		} else if ("method".equalsIgnoreCase(granularityStr) || "m".equalsIgnoreCase(granularityStr)) {
			granularity = Granularity.Method;
			Configuration.outputPath += "Method/";
		} else if ("file".equalsIgnoreCase(granularityStr) || "f".equalsIgnoreCase(granularityStr)) {
			granularity = Granularity.File;
			Configuration.outputPath += "File/";
		} else {
			System.out.println("Last argument must be l, L, line, Line, m, M, method, Method, f, F, file, or File.");
			System.exit(0);
		}

		File location = new File(args[4]);
		Configuration.outputPath = location.getAbsolutePath();

		Configuration.outputPath += File.separator + "FL/";
		System.out.println(projectName);

		Boolean stopFirst = new Boolean(args[5]);

		if (stopFirst)
			Configuration.maxnrpatches = 1;

		System.out.println(projectName);

		fixBug(buggyProjectsPath, defects4jPath, projectName);
	}

	public static void fixBug(String buggyProjectsPath, String defects4jPath, String buggyProjectName) {
		String dataType = "kPAR";
		String[] elements = buggyProjectName.split("_");
		String projectName = elements[0];
		int bugId;
		try {
			bugId = Integer.valueOf(elements[1]);
		} catch (NumberFormatException e) {
			System.err.println("Please input correct buggy project ID, such as \"Chart_1\".");
			return;
		}

		AbstractFixer fixer = null;
		switch (granularity) {
		case Line:
			fixer = new Line_kParFixer(buggyProjectsPath, projectName, bugId, defects4jPath);
			break;
		case Method:
			fixer = new Method_kParFixer(buggyProjectsPath, projectName, bugId, defects4jPath);
			break;
		case File:
			fixer = new File_kParFixer(buggyProjectsPath, projectName, bugId, defects4jPath);
			break;
		default:
			break;
		}
		if (fixer == null)
			return;

		fixer.metric = Configuration.faultLocalizationMetric;
		fixer.dataType = dataType;
		if (Integer.MAX_VALUE == fixer.minErrorTest) {
			System.out.println("Failed to defects4j compile bug " + buggyProjectName);
			return;
		}

		fixer.fixProcess();

		int fixedStatus = fixer.fixedStatus;
		switch (fixedStatus) {
		case 0:
			System.out.println("Failed to fix bug " + buggyProjectName);
			break;
		case 1:
			System.out.println("Succeeded to fix bug " + buggyProjectName);
			break;
		case 2:
			System.out.println("Partial succeeded to fix bug " + buggyProjectName);
			break;
		}
	}

}

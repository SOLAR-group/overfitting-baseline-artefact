package edu.lu.uni.serval.bug.fixer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.lu.uni.serval.config.Configuration;
import edu.lu.uni.serval.dataprepare.DataPreparer;
import edu.lu.uni.serval.jdt.tree.ITree;
import edu.lu.uni.serval.patch.Patch;
import edu.lu.uni.serval.utils.FileHelper;
import edu.lu.uni.serval.utils.FileUtils;
import edu.lu.uni.serval.utils.PathUtils;
import edu.lu.uni.serval.utils.ShellUtils;
import edu.lu.uni.serval.utils.SuspiciousCodeParser;
import edu.lu.uni.serval.utils.SuspiciousPosition;
import edu.lu.uni.serval.utils.TestUtils;

/**
 * Abstract Fixer.
 * 
 * @author kui.liu
 *
 */
public abstract class AbstractFixer implements IFixer {

	private static Logger log = LoggerFactory.getLogger(AbstractFixer.class);

	public String metric = "null"; // Fault localization metric.
	protected String path = "";
	protected String buggyProject = ""; // The buggy project name.
	protected String defects4jPath; // The path of local installed defects4j.
	public int minErrorTest; // Number of failed test cases before fixing.
	protected int minErrorTestAfterFix = 0; // Number of failed test cases after fixing
	protected String fullBuggyProjectPath; // The full path of the local buggy project.
	public String outputPath = ""; // Output path for the generated patches.
	public File suspCodePosFile = null; // The file containing suspicious code positions localized by FL tools.
	protected DataPreparer dp; // The needed data of buggy program for compiling and testing.

	private String failedTestCaseClasses = ""; // Classes of the failed test cases before fixing.
	// All specific failed test cases after testing the buggy project with defects4j
	// command in Java code before fixing.
	protected List<String> failedTestStrList = new ArrayList<>();
	// All specific failed test cases after testing the buggy project with defects4j
	// command in terminal before fixing.
	protected List<String> failedTestCasesStrList = new ArrayList<>();
	// The failed test cases after running defects4j command in Java code but not in
	// terminal.
	private List<String> fakeFailedTestCasesList = new ArrayList<>();

	// 0: failed to fix the bug, 1: succeeded to fix the bug. 2: partially succeeded
	// to fix the bug.
	public int fixedStatus = 0;
	public String dataType = "";
	protected int patchId = 0;
//	private TimeLine timeLine;
	public String GPR = "GPR";
	// MM
	public Date initTime = new Date();
	int nrOfPlausiblePatchesGenerated = 0;

	public AbstractFixer(String path, String projectName, int bugId, String defects4jPath) {
		System.out.println(GPR + "[SSUP]-" + (new Date()).getTime());
		this.path = path;
		this.buggyProject = projectName + "_" + bugId;
		fullBuggyProjectPath = path + buggyProject;
		this.defects4jPath = defects4jPath;
//		int compileResult = TestUtils.compileProjectWithDefects4j(fullBuggyProjectPath, this.defects4jPath);
//      if (compileResult == 1) {
//      	log.debug(buggyProject + " ---Fixer: fix fail because of compile fail! ");
//      }
		minErrorTest = TestUtils.getFailTestNumInProject(path + buggyProject, defects4jPath, failedTestStrList);
		log.info(buggyProject + " Failed Tests: " + this.minErrorTest);

		// Read paths of the buggy project.
		this.dp = new DataPreparer(path);
		dp.prepareData(buggyProject);

		readPreviouslyFailedTestCases();
		System.out.println(GPR + "[ESUP]-" + (new Date()).getTime());
	}

	public AbstractFixer(String path, String metric, String projectName, int bugId, String defects4jPath) {
		this(path, projectName, bugId, defects4jPath);
		this.metric = metric;
	}

	private void readPreviouslyFailedTestCases() {
		String[] failedTestCases = FileHelper
				.readFile(Configuration.failedTestCasesFilePath + this.buggyProject + ".txt").split("\n");
		List<String> failedTestCasesList = new ArrayList<>();
		List<String> failed = new ArrayList<>();
		for (int index = 1, length = failedTestCases.length; index < length; index++) {
			// - org.jfree.data.general.junit.DatasetUtilitiesTests::testBug2849731_2
			String failedTestCase = failedTestCases[index].trim();
			failed.add(failedTestCase);
			failedTestCase = failedTestCase.substring(failedTestCase.indexOf("-") + 1).trim();
			failedTestCasesStrList.add(failedTestCase);
			int colonIndex = failedTestCase.indexOf("::");
			if (colonIndex > 0) {
				failedTestCase = failedTestCase.substring(0, colonIndex);
			}
			if (!failedTestCasesList.contains(failedTestCase)) {
				this.failedTestCaseClasses += failedTestCase + " ";
				failedTestCasesList.add(failedTestCase);
			}
		}

		List<String> tempFailed = new ArrayList<>();
		tempFailed.addAll(this.failedTestStrList);
		tempFailed.removeAll(failed);
		// FIXME: Using defects4j command in Java code may generate some new
		// failed-passing test cases.
		// We call them as fake failed-passing test cases.
		this.fakeFailedTestCasesList.addAll(tempFailed);
	}

	public List<SuspiciousPosition> readSuspiciousCodeFromFile(String metric, String path, String buggyProject,
			DataPreparer dp) {
		File suspiciousFile = null;
		if (this.suspCodePosFile == null) {
			suspiciousFile = new File(Configuration.suspPositionsFilePath);
		} else {
			suspiciousFile = this.suspCodePosFile;
		}
		suspiciousFile = new File(suspiciousFile.getPath() + "/" + this.buggyProject + "/" + this.metric + ".txt");
		if (!suspiciousFile.exists())
			suspiciousFile = new File(suspiciousFile.getPath() + "/" + this.buggyProject + "/All.txt");
		if (!suspiciousFile.exists())
			return null;
		List<SuspiciousPosition> suspiciousCodeList = new ArrayList<>();
		try {
			FileReader fileReader = new FileReader(suspiciousFile);
			BufferedReader reader = new BufferedReader(fileReader);
			String line = null;
			while ((line = reader.readLine()) != null) {
				String[] elements = line.split("@");
				SuspiciousPosition sp = new SuspiciousPosition();
				sp.classPath = elements[0];
				sp.lineNumber = Integer.valueOf(elements[1]);
				suspiciousCodeList.add(sp);
			}
			reader.close();
			fileReader.close();
		} catch (Exception e) {
			e.printStackTrace();
			log.debug("Reloading Localization Result...");
			return null;
		}
		if (suspiciousCodeList.isEmpty())
			return null;
		return suspiciousCodeList;
	}

	@Override
	public SuspCodeNode parseSuspiciousCode(SuspiciousPosition suspiciousCode) {
		String suspiciousClassName = suspiciousCode.classPath;
		int buggyLine = suspiciousCode.lineNumber;

		log.debug(suspiciousClassName + " ===" + buggyLine);
		String suspiciousJavaFile = suspiciousClassName.replace(".", "/") + ".java";

		suspiciousClassName = suspiciousJavaFile.substring(0, suspiciousJavaFile.length() - 5).replace("/", ".");

		String filePath = dp.srcPath + suspiciousJavaFile;
		SuspiciousCodeParser scp = new SuspiciousCodeParser();
		scp.parseSuspiciousCode(new File(filePath), buggyLine);

		ITree suspCodeAstNode = scp.getSuspiciousCodeAstNode();
		String suspCodeStr = scp.getSuspiciousCodeStr();
		if (suspCodeAstNode == null || suspCodeStr == null) {
			log.debug("Failed to identify the buggy statement in: " + suspiciousClassName + " --- " + buggyLine);
			return null;
		}
		log.info("Suspicious Code: \n" + suspCodeStr);

		int startPos = suspCodeAstNode.getPos();
		int endPos = startPos + suspCodeAstNode.getLength();

		File targetJavaFile = new File(FileUtils.getFileAddressOfJava(dp.srcPath, suspiciousClassName));
		File targetClassFile = new File(FileUtils.getFileAddressOfClass(dp.classPath, suspiciousClassName));
		File javaBackup = new File(
				FileUtils.tempJavaPath(suspiciousClassName, this.dataType + "/" + this.buggyProject));
		File classBackup = new File(
				FileUtils.tempClassPath(suspiciousClassName, this.dataType + "/" + this.buggyProject));
		try {
			if (javaBackup.exists())
				javaBackup.delete();
			if (classBackup.exists())
				classBackup.delete();
			Files.copy(targetJavaFile.toPath(), javaBackup.toPath());
			Files.copy(targetClassFile.toPath(), classBackup.toPath());
		} catch (IOException e) {
			e.printStackTrace();
		}

		SuspCodeNode scn = new SuspCodeNode(javaBackup, classBackup, targetJavaFile, targetClassFile, startPos, endPos,
				suspCodeAstNode, suspCodeStr, suspiciousJavaFile, buggyLine);
		return scn;
	}

	protected List<Patch> triedPatchCandidates = new ArrayList<>();

	protected void testGeneratedPatches(List<Patch> patchCandidates, SuspCodeNode scn) {
		// Testing generated patches.
		for (Patch patch : patchCandidates) {
			System.out.println(GPR + "[SPVS]-" + System.currentTimeMillis());
			patch.buggyFileName = scn.suspiciousJavaFile;

			System.out.println(GPR + "[SPVAP]-" + System.currentTimeMillis());
			addPatchCodeToFile(scn, patch);// Insert the patch.
			if (this.triedPatchCandidates.contains(patch)) {
				try {
					scn.targetJavaFile.delete();
					scn.targetClassFile.delete();
					Files.copy(scn.javaBackup.toPath(), scn.targetJavaFile.toPath());
					Files.copy(scn.classBackup.toPath(), scn.targetClassFile.toPath());
				} catch (IOException e) {
					e.printStackTrace();
				}
				continue;
			}
			patchId++;
			this.triedPatchCandidates.add(patch);

			String buggyCode = patch.getBuggyCodeStr();
			if ("===StringIndexOutOfBoundsException===".equals(buggyCode))
				continue;
			String patchCode = patch.getFixedCodeStr1();
			scn.targetClassFile.delete();

			log.debug("Compiling");
			try {// Compile patched file.
				ShellUtils
						.shellRun(Arrays.asList("javac -Xlint:unchecked -source 1.7 -target 1.7 -cp "
								+ PathUtils.buildCompileClassPath(Arrays.asList(PathUtils.getJunitPath()), dp.classPath,
										dp.testClassPath)
								+ " -d " + dp.classPath + " " + scn.targetJavaFile.getAbsolutePath()), buggyProject);
			} catch (IOException e) {
				log.debug(buggyProject + " ---Fixer: fix fail because of javac exception! ");
				System.out.println(GPR + "[EPVAP]-" + System.currentTimeMillis());
				continue;
			}
			if (!scn.targetClassFile.exists()) { // fail to compile
				log.debug(buggyProject + " ---Fixer: fix fail because of failed compiling! ");
				System.out.println(GPR + "[EPVAP]-" + System.currentTimeMillis());
				continue;
			}
			System.out.println(GPR + "[EPVAP]-" + System.currentTimeMillis());

			log.debug("Finish of compiling.");

			log.debug("Test previously failed test cases.");
			System.out.println(GPR + "[SPVATF]-" + System.currentTimeMillis());
			try {
				String results = ShellUtils.shellRun(
						Arrays.asList("java -cp " + PathUtils.buildTestClassPath(dp.classPath, dp.testClassPath)
								+ " org.junit.runner.JUnitCore " + this.failedTestCaseClasses),
						buggyProject);
				List<String> tempFailedTestCases = readTestResults(results);

				if (!tempFailedTestCases.isEmpty()) {
					if (this.failedTestCasesStrList.size() == 1) {
						System.out.println(GPR + "[EPVATF]-" + System.currentTimeMillis());
						continue;
					}

					// Might be partially fixed.
					tempFailedTestCases.removeAll(this.failedTestCasesStrList);
					if (!tempFailedTestCases.isEmpty()) {
						System.out.println(GPR + "[EPVATF]-" + System.currentTimeMillis());
						continue;
					} // Generate new bugs.
				}
			} catch (IOException e) {
				log.debug(buggyProject + " ---Fixer: fix fail because of faile passing previously failed test cases! ");
				{
					System.out.println(GPR + "[EPVATF]-" + System.currentTimeMillis());
					continue;
				}
			}

			System.out.println(GPR + "[EPVATF]-" + System.currentTimeMillis());
			System.out.println(GPR + "[SPVATR]-" + System.currentTimeMillis());
			List<String> failedTestsAfterFix = new ArrayList<>();
			int errorTestAfterFix = TestUtils.getFailTestNumInProject(fullBuggyProjectPath, this.defects4jPath,
					failedTestsAfterFix);
			failedTestsAfterFix.removeAll(this.fakeFailedTestCasesList);
			System.out.println(GPR + "[EPVATR]-" + System.currentTimeMillis());
			if (errorTestAfterFix < minErrorTest) {
				failedTestsAfterFix.removeAll(this.failedTestStrList);
				if (failedTestsAfterFix.size() > 0) { // Generate new bugs.
					log.debug(buggyProject + " ---Generated new bugs: " + failedTestsAfterFix.size());
					continue;
				}

				// Output the generated patch.
				if (errorTestAfterFix == 0) {
					fixedStatus = 1;
					log.info("Succeeded to fix the bug " + buggyProject + "====================");
					String patchStr = TestUtils.readPatch(this.fullBuggyProjectPath);
					long nowtime = System.currentTimeMillis();
					long passTime = (nowtime - initTime.getTime());
					System.out.println(GPR + "[PF]-" + nowtime);
					String fileName = Configuration.outputPath + File.separator + "FixedBugs/" + buggyProject
							+ "/Patch_" + passTime + "_" + patchId + "_ts_" + nowtime + ".txt";

					if (patchStr == null || !patchStr.startsWith("diff")) {

						FileHelper.outputToFile(fileName,
								"//**********************************************************\n//"
										+ scn.suspiciousJavaFile + " ------ " + scn.buggyLine
										+ "\n//**********************************************************\n"
										+ "===Buggy Code===\n" + buggyCode + "\n\n===Patch Code===\n" + patchCode,
								false);
					} else {
						FileHelper.outputToFile(fileName, patchStr, false);
					}

					/// MM added
					nrOfPlausiblePatchesGenerated++;

					if (nrOfPlausiblePatchesGenerated >= Configuration.maxnrpatches) {
						System.out.println("MM stops patch execution: nr of patches " + nrOfPlausiblePatchesGenerated);
						this.minErrorTest = 0;
						break;
					}

					// MM commented

					// this.minErrorTest = 0;
					// break;
				}
//MM commented
//				else {
//					if (minErrorTestAfterFix == 0 || errorTestAfterFix <= minErrorTestAfterFix) {
//						minErrorTestAfterFix = errorTestAfterFix;
//						if (fixedStatus != 1) fixedStatus = 2;
//						log.info("Partially Succeeded to fix the bug " + buggyProject + "====================");
//						String patchStr = TestUtils.readPatch(this.fullBuggyProjectPath);
//						if (patchStr == null || !patchStr.startsWith("diff")) {
//							FileHelper.outputToFile("OUTPUT/FixedBugs/" + buggyProject + "/Patch_" + patchId + ".txt",
//									"//**********************************************************\n//" + scn.suspiciousJavaFile + " ------ " + scn.buggyLine
//									+ "\n//**********************************************************\n"
//									+ "===Buggy Code===\n" + buggyCode + "\n\n===Patch Code===\n" + patchCode, false);
//						} else {
//							FileHelper.outputToFile("OUTPUT/FixedBugs/" + buggyProject + "/Patch_" + patchId + ".txt", patchStr, false);
//						}
//					}
//				}
			} else {
				log.debug("Failed Tests after fixing: " + errorTestAfterFix + " " + buggyProject);
			}
			System.out.println(GPR + "[EPVS]-" + (new Date()).getTime());
		}
		System.out.println(GPR + "[SPVESU]-" + System.currentTimeMillis());
		try {
			scn.targetJavaFile.delete();
			scn.targetClassFile.delete();
			Files.copy(scn.javaBackup.toPath(), scn.targetJavaFile.toPath());
			Files.copy(scn.classBackup.toPath(), scn.targetClassFile.toPath());
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		System.out.println(GPR + "[EPVESU]-" + System.currentTimeMillis());
	}

	private List<String> readTestResults(String results) {
		List<String> failedTeatCases = new ArrayList<>();
		String[] testResults = results.split("\n");
		for (String testResult : testResults) {
			if (testResult.isEmpty())
				continue;

			if (NumberUtils.isDigits(testResult.substring(0, 1))) {
				int index = testResult.indexOf(") ");
				if (index <= 0)
					continue;
				testResult = testResult.substring(index + 1, testResult.length() - 1).trim();
				int indexOfLeftParenthesis = testResult.indexOf("(");
				String testCase = testResult.substring(0, indexOfLeftParenthesis);
				String testClass = testResult.substring(indexOfLeftParenthesis + 1);
				failedTeatCases.add(testClass + "::" + testCase);
			}
		}
		return failedTeatCases;
	}

	private void addPatchCodeToFile(SuspCodeNode scn, Patch patch) {
		String javaCode = FileHelper.readFile(scn.javaBackup);

		String fixedCodeStr1 = patch.getFixedCodeStr1();
		String fixedCodeStr2 = patch.getFixedCodeStr2();
		int exactBuggyCodeStartPos = patch.getBuggyCodeStartPos();
		int exactBuggyCodeEndPos = patch.getBuggyCodeEndPos();
		String patchCode = fixedCodeStr1;
		boolean needBuggyCode = false;
		if (exactBuggyCodeEndPos > exactBuggyCodeStartPos) {
			if ("MOVE-BUGGY-STATEMENT".equals(fixedCodeStr2)) {
				// move statement position.
			} else if (exactBuggyCodeStartPos != -1 && exactBuggyCodeStartPos < scn.startPos) {
				// Remove the buggy method declaration.
			} else {
				needBuggyCode = true;
				if (exactBuggyCodeStartPos == 0) {
					// Insert the missing override method, the buggy node is TypeDeclaration.
					int pos = scn.suspCodeAstNode.getPos() + scn.suspCodeAstNode.getLength() - 1;
					for (int i = pos; i >= 0; i--) {
						if (javaCode.charAt(i) == '}') {
							exactBuggyCodeStartPos = i;
							exactBuggyCodeEndPos = i + 1;
							break;
						}
					}
				} else if (exactBuggyCodeStartPos == -1) {
					// Insert generated patch code before the buggy code.
					exactBuggyCodeStartPos = scn.startPos;
					exactBuggyCodeEndPos = scn.endPos;
				} else {
					// Insert a block-held statement to surround the buggy code
				}
			}
		} else if (exactBuggyCodeStartPos == -1 && exactBuggyCodeEndPos == -1) {
			// Replace the buggy code with the generated patch code.
			exactBuggyCodeStartPos = scn.startPos;
			exactBuggyCodeEndPos = scn.endPos;
		} else if (exactBuggyCodeStartPos == exactBuggyCodeEndPos) {
			// Remove buggy variable declaration statement.
			exactBuggyCodeStartPos = scn.startPos;
		}

		patch.setBuggyCodeStartPos(exactBuggyCodeStartPos);
		patch.setBuggyCodeEndPos(exactBuggyCodeEndPos);
		String buggyCode;
		try {
			buggyCode = javaCode.substring(exactBuggyCodeStartPos, exactBuggyCodeEndPos);
			if (needBuggyCode) {
				patchCode += buggyCode;
				if (fixedCodeStr2 != null) {
					patchCode += fixedCodeStr2;
				}
			}

			File newFile = new File(scn.targetJavaFile.getAbsolutePath() + ".temp");
			String patchedJavaFile = javaCode.substring(0, exactBuggyCodeStartPos) + patchCode
					+ javaCode.substring(exactBuggyCodeEndPos);
			FileHelper.outputToFile(newFile, patchedJavaFile, false);
			newFile.renameTo(scn.targetJavaFile);
		} catch (StringIndexOutOfBoundsException e) {
			log.debug(exactBuggyCodeStartPos + " ==> " + exactBuggyCodeEndPos + " : " + javaCode.length());
			e.printStackTrace();
			buggyCode = "===StringIndexOutOfBoundsException===";
		}

		patch.setBuggyCodeStr(buggyCode);
		patch.setFixedCodeStr1(patchCode);
	}

	class SuspCodeNode {

		public File javaBackup;
		public File classBackup;
		public File targetJavaFile;
		public File targetClassFile;
		public int startPos;
		public int endPos;
		public ITree suspCodeAstNode;
		public String suspCodeStr;
		public String suspiciousJavaFile;
		public int buggyLine;

		public SuspCodeNode(File javaBackup, File classBackup, File targetJavaFile, File targetClassFile, int startPos,
				int endPos, ITree suspCodeAstNode, String suspCodeStr, String suspiciousJavaFile, int buggyLine) {
			this.javaBackup = javaBackup;
			this.classBackup = classBackup;
			this.targetJavaFile = targetJavaFile;
			this.targetClassFile = targetClassFile;
			this.startPos = startPos;
			this.endPos = endPos;
			this.suspCodeAstNode = suspCodeAstNode;
			this.suspCodeStr = suspCodeStr;
			this.suspiciousJavaFile = suspiciousJavaFile;
			this.buggyLine = buggyLine;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == null)
				return false;
			if (obj instanceof SuspCodeNode) {
				SuspCodeNode suspN = (SuspCodeNode) obj;
				if (startPos != suspN.startPos)
					return false;
				if (endPos != suspN.endPos)
					return false;
				if (suspiciousJavaFile.equals(suspN.suspiciousJavaFile))
					return true;
			}
			return false;
		}
	}
}

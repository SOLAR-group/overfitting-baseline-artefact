package edu.lu.uni.serval.bug.fixer;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.lu.uni.serval.config.Configuration;
import edu.lu.uni.serval.fixminer.ProjectLiteral;
import edu.lu.uni.serval.fixminer.insertTemplate.InsertIfNonNullCheck;
import edu.lu.uni.serval.fixminer.insertTemplate.InsertIfNullCheck;
import edu.lu.uni.serval.fixminer.insertTemplate.InsertIfSizeCheck;
import edu.lu.uni.serval.fixminer.insertTemplate.StatementInserter;
import edu.lu.uni.serval.fixminer.insertTemplate.StatementMover;
import edu.lu.uni.serval.patch.Patch;
import edu.lu.uni.serval.templates.FixTemplate;
import edu.lu.uni.serval.utils.Checker;
import edu.lu.uni.serval.utils.FileHelper;
import edu.lu.uni.serval.utils.SuspiciousPosition;
import edu.uni.lu.serval.fixminer.fixtemplate.DeleteStatement;
import edu.uni.lu.serval.fixminer.fixtemplate.IfNullChecker;
import edu.uni.lu.serval.fixminer.fixtemplate.IfOperator;
import edu.uni.lu.serval.fixminer.fixtemplate.ModifyBooleanArg;
import edu.uni.lu.serval.fixminer.fixtemplate.ModifyBooleanValue;
import edu.uni.lu.serval.fixminer.fixtemplate.ModifyExpStmtAssignMethodName;
import edu.uni.lu.serval.fixminer.fixtemplate.ModifyExpStmtMethodName;
import edu.uni.lu.serval.fixminer.fixtemplate.ModifyIfStmtMethodName;
import edu.uni.lu.serval.fixminer.fixtemplate.ModifyIfVariable;
import edu.uni.lu.serval.fixminer.fixtemplate.ModifyMethodInvocation;
import edu.uni.lu.serval.fixminer.fixtemplate.ModifyMethodInvocation.ModifyType;
import edu.uni.lu.serval.fixminer.fixtemplate.ModifyMethodQualifiedArgument;
import edu.uni.lu.serval.fixminer.fixtemplate.ModifyMethodVarArgument;
import edu.uni.lu.serval.fixminer.fixtemplate.ModifyModifier;
import edu.uni.lu.serval.fixminer.fixtemplate.ModifyNumberValue;
import edu.uni.lu.serval.fixminer.fixtemplate.ModifyPrimitiveType;
import edu.uni.lu.serval.fixminer.fixtemplate.ModifyReturnStmtMethodName;
import edu.uni.lu.serval.fixminer.fixtemplate.ModifyReturnStmtMethodVarRef;
import edu.uni.lu.serval.fixminer.fixtemplate.ModifyStringValue;
import edu.uni.lu.serval.fixminer.fixtemplate.ModifyVarDecStmtMethodName;
import edu.uni.lu.serval.fixminer.fixtemplate.ModifyVarDecStmtMethodVarArgument;
import edu.uni.lu.serval.fixminer.fixtemplate.ModifyVarDecStmtMethodVarArgument2MethodInvocation;
import edu.uni.lu.serval.fixminer.fixtemplate.ModifyVarDecStmtMethodVarRef;
import edu.uni.lu.serval.fixminer.fixtemplate.ModifyVariable;

/**
 * Automated Program Repair Tool: FixMiner.
 * 
 * @author kui.liu
 *
 */
public class FixMinerFixer extends AbstractFixer {

	private static Logger log = LoggerFactory.getLogger(FixMinerFixer.class);

	public List<ProjectLiteral> literals;

	public FixMinerFixer(String path, String projectName, int bugId, String defects4jPath) {
		super(path, projectName, bugId, defects4jPath);
	}

	public FixMinerFixer(String path, String metric, String projectName, int bugId, String defects4jPath) {
		super(path, metric, projectName, bugId, defects4jPath);
	}

	@Override
	public void fixProcess() {
		// Read paths of the buggy project.
		if (!dp.validPaths)
			return;
		System.out.println(GPR + "[SFL]-" + System.currentTimeMillis());
		// Read suspicious positions.
		List<SuspiciousPosition> suspiciousCodeList = readSuspiciousCodeFromFile(metric, path, buggyProject, dp);
		System.out.println(GPR + "[EFL]-" + System.currentTimeMillis());

		if (suspiciousCodeList == null)
			return;

		log.info("=======Fixing Beginning======");
		List<SuspCodeNode> scns = new ArrayList<>();
		System.out.println(GPR + "[SFP]-" + (System.currentTimeMillis()));
		for (SuspiciousPosition suspiciousCode : suspiciousCodeList) {
			System.out.println(GPR + "[SSUN]-" + System.currentTimeMillis());
			SuspCodeNode scn = parseSuspiciousCode(suspiciousCode);
			if (scn == null)
				continue;

//			System.err.println(scn.suspCodeStr);
			// Match fix templates for this suspicious code with its context information.
			fixWithMatchedFixTemplates(scn);
			scns.add(scn);

			/// MM added
			nrOfPlausiblePatchesGenerated++;

			if (nrOfPlausiblePatchesGenerated >= Configuration.maxnrpatches) {
				System.out.println("MM stops patch execution: nr of patches " + nrOfPlausiblePatchesGenerated);
				this.minErrorTest = 0;
				break;
			}
			// MM commented
			// if (minErrorTest == 0)
			// break;
			System.out.println(GPR + "[ESUN]-" + System.currentTimeMillis());
		}

		if (minErrorTest != 0) {
			subFixTemplates(scns);
		}
		log.info("=======Finish off Fixing======");
		System.out.println(GPR + "[EFP]-" + (System.currentTimeMillis()));
		FileHelper.deleteDirectory(Configuration.TEMP_FILES_PATH + this.dataType + "/" + this.buggyProject);
	}

	private void fixWithMatchedFixTemplates(SuspCodeNode scn) {

		int suspiciousCodeType = scn.suspCodeAstNode.getType();

		// generate patches with fix templates.
		FixTemplate ft = null;
		if (Checker.isVariableDeclarationStatement(suspiciousCodeType)) {
			ft = new ModifyPrimitiveType();
			generatePatches(ft, scn);
			if (minErrorTest == 0)
				return;

			ft = new ModifyVarDecStmtMethodName();
			generatePatches(ft, scn);
			if (minErrorTest == 0)
				return;

			ft = new ModifyVarDecStmtMethodVarArgument();
			generatePatches(ft, scn);
			if (minErrorTest == 0)
				return;

			ft = new ModifyVarDecStmtMethodVarArgument2MethodInvocation();
			generatePatches(ft, scn);
			if (minErrorTest == 0)
				return;

			ft = new ModifyVarDecStmtMethodVarRef();
		} else if (Checker.isFieldDeclaration(suspiciousCodeType)) {
			ft = new ModifyModifier();
		} else if (Checker.isReturnStatement(suspiciousCodeType)) {
			ft = new ModifyMethodVarArgument();
			generatePatches(ft, scn);
			if (minErrorTest == 0)
				return;

			ft = new ModifyReturnStmtMethodName();
			generatePatches(ft, scn);
			if (minErrorTest == 0)
				return;

			ft = new ModifyReturnStmtMethodVarRef();
			generatePatches(ft, scn);
			if (minErrorTest == 0)
				return;

			ft = new ModifyBooleanArg();
			generatePatches(ft, scn);
			if (minErrorTest == 0)
				return;

			ft = new IfNullChecker();
		} else if (Checker.isExpressionStatement(suspiciousCodeType)) {
			ft = new ModifyBooleanArg();
			generatePatches(ft, scn);
			if (minErrorTest == 0)
				return;

			ft = new ModifyMethodVarArgument();
			generatePatches(ft, scn);
			if (minErrorTest == 0)
				return;

			ft = new ModifyMethodQualifiedArgument();
			generatePatches(ft, scn);
			if (minErrorTest == 0)
				return;

			ft = new ModifyExpStmtMethodName();
			generatePatches(ft, scn);
			if (minErrorTest == 0)
				return;

			ft = new ModifyExpStmtAssignMethodName();
		} else if (Checker.isIfStatement(suspiciousCodeType)) {
			ft = new IfOperator();
			generatePatches(ft, scn);
			if (minErrorTest == 0)
				return;

			ft = new ModifyIfStmtMethodName();
			generatePatches(ft, scn);
			if (minErrorTest == 0)
				return;

			ft = new ModifyMethodVarArgument();
			generatePatches(ft, scn);
			if (minErrorTest == 0)
				return;

			ft = new ModifyIfVariable();
			generatePatches(ft, scn);
			if (minErrorTest == 0)
				return;

			ft = new IfNullChecker();
		} else {
		}
		if (ft != null) {
			generatePatches(ft, scn);
		}
		if (minErrorTest == 0)
			return;
		ft = new InsertIfNonNullCheck();
		generatePatches(ft, scn);

		if (minErrorTest == 0)
			return;
		ft = new InsertIfNullCheck();
		generatePatches(ft, scn);

		if (minErrorTest == 0)
			return;
		ft = new InsertIfSizeCheck();
		generatePatches(ft, scn);

		if (minErrorTest == 0)
			return;
		ft = new StatementInserter();
		generatePatches(ft, scn);

		if (minErrorTest == 0)
			return;
		ft = new StatementMover();
		generatePatches(ft, scn);

		if (minErrorTest == 0)
			return;
		ft = new DeleteStatement();
		generatePatches(ft, scn);
		ft = null;
	}

	private void generatePatches(FixTemplate ft, SuspCodeNode scn) {
		System.out.println(GPR + "[SPS]-" + System.currentTimeMillis());
		ft.setSuspiciousCodeStr(scn.suspCodeStr);
		ft.setSuspiciousCodeTree(scn.suspCodeAstNode);
		ft.setLiterals(this.literals);
		if (scn.javaBackup == null)
			ft.setSourceCodePath(dp.srcPath);
		else
			ft.setSourceCodePath(dp.srcPath, scn.javaBackup);
		ft.generatePatches();
		List<Patch> patchCandidates = ft.getPatches();
		System.out.println(GPR + "[EPS]-" + System.currentTimeMillis());

		// Test generated patches.
		if (patchCandidates.isEmpty())
			return;
		System.out.println(GPR + "[SPVA]-" + System.currentTimeMillis());
		testGeneratedPatches(patchCandidates, scn);
		System.out.println(GPR + "[EPVA]-" + System.currentTimeMillis());
	}

	private void subFixTemplates(List<SuspCodeNode> scns) {
		for (SuspCodeNode scn : scns) {
			FixTemplate ft = null;
			ft = new ModifyMethodInvocation(scn.suspCodeAstNode, ModifyType.DEFAULT);
			generatePatches(ft, scn);
			if (minErrorTest == 0)
				break;

			if (Checker.isTypeDeclaration(scn.suspCodeAstNode.getType())) {
				ft = new ModifyModifier();
				generatePatches(ft, scn);
				if (minErrorTest == 0)
					break;
			} else if (Checker.isMethodDeclaration(scn.suspCodeAstNode.getType())) {
				ft = new ModifyModifier();
				generatePatches(ft, scn);
				if (minErrorTest == 0)
					break;
			}

			ft = new ModifyVariable(true);
			generatePatches(ft, scn);
			if (minErrorTest == 0)
				break;

			ft = new ModifyBooleanValue();
			generatePatches(ft, scn);
			if (minErrorTest == 0)
				break;

			ft = new ModifyNumberValue();
			generatePatches(ft, scn);
			if (minErrorTest == 0)
				break;

			ft = new ModifyStringValue();
			generatePatches(ft, scn);
			if (minErrorTest == 0)
				break;

//			ft = new ModifyMethodInvocation(scn.suspCodeAstNode, ModifyType.DEFAULT);
//			generatePatches(ft, scn);
//			if (minErrorTest == 0) break;

			ft = null;
		}
	}

}

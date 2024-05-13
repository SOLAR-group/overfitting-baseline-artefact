package fr.inria.astor.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.apache.log4j.Level;
import org.junit.jupiter.api.Test;

import fr.inria.astor.approaches.jgenprog.operators.InsertBeforeOp;
import fr.inria.astor.approaches.jgenprog.operators.RemoveOp;
import fr.inria.astor.approaches.jgenprog.operators.ReplaceOp;
import fr.inria.astor.core.entities.OperatorInstance;
import fr.inria.astor.core.entities.ProgramVariant;
import fr.inria.astor.core.entities.StatementOperatorInstance;
import fr.inria.astor.core.manipulation.MutationSupporter;
import fr.inria.astor.core.setup.ConfigurationProperties;
import fr.inria.astor.core.solutionsearch.AstorCoreEngine;
import fr.inria.main.CommandSummary;
import fr.inria.main.evolution.AstorMain;
import spoon.reflect.code.CtCodeElement;

public class UnrepairedTest extends D4JWorkflowTestSingle {

	@Test
	public void testSynthesisIssueMath78PatchNopol() throws Exception {
		org.apache.log4j.LogManager.getRootLogger().setLevel(Level.DEBUG);
		String bugid = "Math78";

		configureBuggyProject(bugid, "-Dmaven.compiler.source=7 -Dmaven.compiler.target=7");
		CommandSummary cs = new CommandSummary();
		createCommand(bugid, "jKali", 5, "gzoltar", cs); //

		cs.command.put("-maxtime", "0");

		assertEquals("0", cs.command.get("-maxtime"));

		AstorMain main1 = new AstorMain();

		System.out.println("command before " + cs.flat());
		main1.execute(cs.flat());

		ConfigurationProperties.setProperty("maxtime", "1000000");
		AstorCoreEngine engine = main1.getEngine();

		assertEquals(1, engine.getVariants().size());
		ProgramVariant programVariant = engine.getVariants().get(0);
		programVariant.getModificationPoints()
				.removeIf(e -> !(e.getCodeElement().toString().equals("delta = 0.5 * dx")));

		assertEquals(1, programVariant.getModificationPoints().size());

		engine.startSearch();
		engine.atEnd();

		System.out.println("---Starting second run---");
		// Patch of nopol
		CtCodeElement newIf = MutationSupporter.getFactory().Code()
				.createCodeSnippetStatement("if(y0 < 1){delta = 0.5 * dx;} ").partiallyEvaluate();

		ReplaceOp rop = new ReplaceOp();

		OperatorInstance opinstance = new StatementOperatorInstance(programVariant.getModificationPoints().get(0), rop,
				programVariant.getModificationPoints().get(0).getCodeElement(), newIf);

		boolean applied = opinstance.applyModification();

		assertTrue(applied);

		programVariant.putModificationInstance(0, opinstance);

		programVariant.setId(10);

		ConfigurationProperties.setProperty("saveall", "true");

		boolean passes = engine.processCreatedVariant(programVariant, 1);

		assertNotNull(programVariant.getValidationResult());

		assertTrue(passes);

		boolean undo = opinstance.undoModification();

		assertTrue(undo);

	}

	@Test
	public void testSynthesisIssueMath78PatchjKlai() throws Exception {
		org.apache.log4j.LogManager.getRootLogger().setLevel(Level.DEBUG);
		String bugid = "Math78";

		configureBuggyProject(bugid, "-Dmaven.compiler.source=7 -Dmaven.compiler.target=7");
		CommandSummary cs = new CommandSummary();
		createCommand(bugid, "jKali", 5, "gzoltar", cs); //

		cs.command.put("-maxtime", "0");

		assertEquals("0", cs.command.get("-maxtime"));

		AstorMain main1 = new AstorMain();

		System.out.println("command before " + cs.flat());
		main1.execute(cs.flat());

		ConfigurationProperties.setProperty("maxtime", "1000000");
		AstorCoreEngine engine = main1.getEngine();

		assertEquals(1, engine.getVariants().size());
		ProgramVariant programVariant = engine.getVariants().get(0);
		programVariant.getModificationPoints().removeIf(e -> !(e.getCodeElement().toString().equals("t0 = t")
				&& e.getCodeElement().getPosition().getLine() == 285));

		assertEquals(1, programVariant.getModificationPoints().size());

		engine.startSearch();
		engine.atEnd();

		System.out.println("---Starting second run---");

		RemoveOp rop = new RemoveOp();

		OperatorInstance opinstance = new StatementOperatorInstance(programVariant.getModificationPoints().get(0), rop,
				programVariant.getModificationPoints().get(0).getCodeElement(), null);

		boolean applied = opinstance.applyModification();

		assertTrue(applied);

		programVariant.putModificationInstance(0, opinstance);

		programVariant.setId(10);

		ConfigurationProperties.setProperty("saveall", "true");

		boolean passes = engine.processCreatedVariant(programVariant, 1);

		assertNotNull(programVariant.getValidationResult());

		assertTrue(passes);

		boolean undo = opinstance.undoModification();

		assertTrue(undo);

	}

	@Test
	public void testFaultLocalizationFlacocoIssueMath84Flacoco() throws Exception {
		org.apache.log4j.LogManager.getRootLogger().setLevel(Level.DEBUG);
		configureBuggyProject("Math84", "-Dmaven.compiler.source=7 -Dmaven.compiler.target=7");
		CommandSummary cs = new CommandSummary();
		createCommand("Math84", "jGenprog", 5, "fr.inria.astor.core.faultlocalization.flacoco.FlacocoFaultLocalization",
				cs); // ""

		cs.command.put("-flthreshold", "0.1");
		cs.command.put("-maxgen", "0");
		assertEquals("0", cs.command.get("-maxgen"));

		AstorMain main1 = new AstorMain();

		System.out.println("command before " + cs.flat());
		main1.execute(cs.flat());

		ConfigurationProperties.setProperty("maxtime", "1000000");
		AstorCoreEngine engine = main1.getEngine();

		assertEquals(1, engine.getVariants().size());
		ProgramVariant programVariant = engine.getVariants().get(0);
		programVariant.getModificationPoints().removeIf(e -> !(e.getCodeElement().getPosition().getLine() == 90
				&& e.getCodeElement().getPosition().getFile().getName().contains("MultiDirectional.java")));

		assertEquals(1, programVariant.getModificationPoints().size());

		assertTrue(programVariant.getModificationPoints().get(0).getCodeElement().toString()
				.startsWith("if (comparator.compare(contracted, best) < 0)"));

		System.out.println("---Starting second run---");
		// Patch of nopol
		CtCodeElement newIf = MutationSupporter.getFactory().Code().createCodeSnippetStatement("break")
				.partiallyEvaluate();

		ReplaceOp rop = new ReplaceOp();

		OperatorInstance opinstance = new StatementOperatorInstance(programVariant.getModificationPoints().get(0), rop,
				programVariant.getModificationPoints().get(0).getCodeElement(), newIf);

		boolean applied = opinstance.applyModification();

		assertTrue(applied);

		programVariant.putModificationInstance(0, opinstance);

		programVariant.setId(10);

		ConfigurationProperties.setProperty("saveall", "true");

		boolean passes = engine.processCreatedVariant(programVariant, 1);

		assertNotNull(programVariant.getValidationResult());

		assertTrue(passes);

		boolean undo = opinstance.undoModification();

		assertTrue(undo);
	}

	@Test
	public void testFaultLocalizationFlacocoIssueChart15Flacoco() throws Exception {
		org.apache.log4j.LogManager.getRootLogger().setLevel(Level.DEBUG);
		configureBuggyProject("Chart15", "-Dmaven.compiler.source=7 -Dmaven.compiler.target=7");
		CommandSummary cs = new CommandSummary();
		createCommand("Chart15", "jKali", 5, "fr.inria.astor.core.faultlocalization.flacoco.FlacocoFaultLocalization",
				cs); // ""

		cs.command.put("-flthreshold", "0.7");
		cs.command.put("-javacompliancelevel", "4");
		cs.command.put("-maxgen", "0");
		assertEquals("0", cs.command.get("-maxgen"));

		AstorMain main1 = new AstorMain();

		cs.command.put("-maxtime", "0");

		assertEquals("0", cs.command.get("-maxtime"));

		System.out.println("command before " + cs.flat());
		main1.execute(cs.flat());

		ConfigurationProperties.setProperty("maxtime", "1000000");
		AstorCoreEngine engine = main1.getEngine();

		assertEquals(1, engine.getVariants().size());
		ProgramVariant programVariant = engine.getVariants().get(0);
		programVariant.getModificationPoints().removeIf(e -> !(e.getCodeElement().getPosition().getLine() == 230
				&& e.getCodeElement().getPosition().getFile().getName().contains("PiePlot3D.java")));

		assertEquals(1, programVariant.getModificationPoints().size());

		// assertTrue(programVariant.getModificationPoints().get(0).getCodeElement().toString()
		// .startsWith("if (comparator.compare(contracted, best) < 0)"));

		System.out.println("---Starting second run---");
		// Patch of nopol
		CtCodeElement newIf = MutationSupporter.getFactory().Code().createCodeSnippetStatement("if(true){return;}")
				.partiallyEvaluate();

		InsertBeforeOp rop = new InsertBeforeOp();

		OperatorInstance opinstance = new StatementOperatorInstance(programVariant.getModificationPoints().get(0), rop,
				programVariant.getModificationPoints().get(0).getCodeElement(), newIf);

		boolean applied = opinstance.applyModification();

		assertTrue(applied);

		programVariant.putModificationInstance(0, opinstance);

		programVariant.setId(10);

		ConfigurationProperties.setProperty("saveall", "true");

		boolean passes = engine.processCreatedVariant(programVariant, 1);

		assertNotNull(programVariant.getValidationResult());

		assertTrue(passes);

		boolean undo = opinstance.undoModification();

		assertTrue(undo);
	}

	@Test
	public void testSynthesisIssueMath95PatchjKlai() throws Exception {
		org.apache.log4j.LogManager.getRootLogger().setLevel(Level.DEBUG);
		String bugid = "Math95";

		configureBuggyProject(bugid, "-Dmaven.compiler.source=7 -Dmaven.compiler.target=7");
		CommandSummary cs = new CommandSummary();
		createCommand(bugid, "jKali", 5, "gzoltar", cs); //

		cs.command.put("-maxtime", "0");

		assertEquals("0", cs.command.get("-maxtime"));

		AstorMain main1 = new AstorMain();

		System.out.println("command before " + cs.flat());
		main1.execute(cs.flat());

		AstorCoreEngine engine = main1.getEngine();

		assertEquals(1, engine.getVariants().size());
		ProgramVariant programVariant = engine.getVariants().get(0);
		programVariant.getModificationPoints()
				.removeIf(e -> !(e.getCodeElement().getPosition().getFile().getName().contains("FDistributionImpl.java")
						&& e.getCodeElement().getPosition().getLine() == 148));

		assertEquals(1, programVariant.getModificationPoints().size());
		System.out.println("MD selected: " + programVariant.getModificationPoints().get(0).getCodeElement().toString());
		ConfigurationProperties.setProperty("maxtime", "1000000");
		engine.startSearch();
		engine.atEnd();

		assertEquals(1, engine.getSolutions().size());
		System.out.println("---Starting second run---");

	}

	@Test
	public void testSynthesisIssueMath40PatchjKali() throws Exception {
		org.apache.log4j.LogManager.getRootLogger().setLevel(Level.DEBUG);
		String bugid = "Math40";

		configureBuggyProject(bugid, "-Dmaven.compiler.source=7 -Dmaven.compiler.target=7");
		CommandSummary cs = new CommandSummary();
		createCommand(bugid, "jKali", 5, "gzoltar", cs); //

		cs.command.put("-maxtime", "0");
		cs.command.put("-jvm4testexecution", (System.getenv("J7PATH") != null) ? System.getenv("J7PATH")
				: "/Library/Java/JavaVirtualMachines/jdk1.7.0_79.jdk/Contents/Home/bin/");
		assertEquals("0", cs.command.get("-maxtime"));

		AstorMain main1 = new AstorMain();

		System.out.println("command before " + cs.flat());
		main1.execute(cs.flat());

		AstorCoreEngine engine = main1.getEngine();

		assertEquals(1, engine.getVariants().size());
		ProgramVariant programVariant = engine.getVariants().get(0);
		programVariant.getModificationPoints().removeIf(e -> !(e.getCodeElement().getPosition().getFile().getName()
				.contains("BracketingNthOrderBrentSolver.java") && e.getCodeElement().getPosition().getLine() == 260));

		assertEquals(1, programVariant.getModificationPoints().size());
		System.out.println("MD selected: " + programVariant.getModificationPoints().get(0).getCodeElement().toString());
		ConfigurationProperties.setProperty("maxtime", "1000000");
		engine.startSearch();
		engine.atEnd();

		assertEquals(1, engine.getSolutions().size());
		System.out.println("---Starting second run---");

	}

	@Test
	public void testSynthesisIssueMath71Patch() throws Exception {
		org.apache.log4j.LogManager.getRootLogger().setLevel(Level.DEBUG);
		String bugid = "Math71";

		configureBuggyProject(bugid, "-Dmaven.compiler.source=7 -Dmaven.compiler.target=7");
		CommandSummary cs = new CommandSummary();
		createCommand(bugid, "jGenProg", 5, "gzoltar", cs); //

		cs.command.put("-maxtime", "0");

		assertEquals("0", cs.command.get("-maxtime"));

		AstorMain main1 = new AstorMain();

		System.out.println("command before " + cs.flat());
		main1.execute(cs.flat());

		AstorCoreEngine engine = main1.getEngine();

		assertEquals(1, engine.getVariants().size());
		ProgramVariant programVariant = engine.getVariants().get(0);
		programVariant.getModificationPoints()
				.removeIf(e -> !(e.getCodeElement().getPosition().getFile().getName().contains("FDistributionImpl.java")
						&& e.getCodeElement().getPosition().getLine() == 148));

		assertEquals(1, programVariant.getModificationPoints().size());
		System.out.println("MD selected: " + programVariant.getModificationPoints().get(0).getCodeElement().toString());
		ConfigurationProperties.setProperty("maxtime", "1000000");
		engine.startSearch();
		engine.atEnd();

		System.out.println("---Starting second run---");

	}

}

package fr.inria.astor.test;

import org.junit.jupiter.api.Test;

import spoon.Launcher;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtConstructor;
import spoon.reflect.reference.CtExecutableReference;

class TestSpoon {

	@Test
	void test() {
		CtClass s = Launcher
				.parseClass("class A { public A(){super(); }  public String sayHello() { return \"Hello World!\";}}");

		System.out.println(s);

		CtConstructor<?> m = (CtConstructor<?>) s.getConstructors().stream().findFirst().get();
		System.out.println(m.getBody().getStatement(0).getClass());

		CtInvocation target = m.getBody().getStatement(0);
		System.out.println((target instanceof CtInvocation && ((CtInvocation<?>) target).getExecutable().getSimpleName()
				.startsWith(CtExecutableReference.CONSTRUCTOR_NAME)));

	}

}

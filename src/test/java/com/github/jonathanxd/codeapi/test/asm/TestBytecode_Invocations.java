/*
 *      CodeAPI-BytecodeWriter - Framework to generate Java code and Bytecode code. <https://github.com/JonathanxD/CodeAPI-BytecodeWriter>
 *
 *         The MIT License (MIT)
 *
 *      Copyright (c) 2017 TheRealBuggy/JonathanxD (https://github.com/JonathanxD/ & https://github.com/TheRealBuggy/) <jonathan.scripter@programmer.net>
 *      Copyright (c) contributors
 *
 *
 *      Permission is hereby granted, free of charge, to any person obtaining a copy
 *      of this software and associated documentation files (the "Software"), to deal
 *      in the Software without restriction, including without limitation the rights
 *      to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *      copies of the Software, and to permit persons to whom the Software is
 *      furnished to do so, subject to the following conditions:
 *
 *      The above copyright notice and this permission notice shall be included in
 *      all copies or substantial portions of the Software.
 *
 *      THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *      IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *      FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *      AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *      LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *      OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *      THE SOFTWARE.
 */
package com.github.jonathanxd.codeapi.test.asm;

import com.github.jonathanxd.codeapi.CodeAPI;
import com.github.jonathanxd.codeapi.CodePart;
import com.github.jonathanxd.codeapi.MutableCodeSource;
import com.github.jonathanxd.codeapi.Types;
import com.github.jonathanxd.codeapi.base.ClassDeclaration;
import com.github.jonathanxd.codeapi.base.ConstructorDeclaration;
import com.github.jonathanxd.codeapi.base.FieldDeclaration;
import com.github.jonathanxd.codeapi.base.MethodDeclaration;
import com.github.jonathanxd.codeapi.base.MethodInvocation;
import com.github.jonathanxd.codeapi.base.TypeDeclaration;
import com.github.jonathanxd.codeapi.base.VariableDeclaration;
import com.github.jonathanxd.codeapi.builder.ClassDeclarationBuilder;
import com.github.jonathanxd.codeapi.builder.ConstructorDeclarationBuilder;
import com.github.jonathanxd.codeapi.builder.MethodDeclarationBuilder;
import com.github.jonathanxd.codeapi.bytecode.BytecodeClass;
import com.github.jonathanxd.codeapi.bytecode.BytecodeOptions;
import com.github.jonathanxd.codeapi.bytecode.VisitLineType;
import com.github.jonathanxd.codeapi.bytecode.gen.BytecodeGenerator;
import com.github.jonathanxd.codeapi.common.CodeArgument;
import com.github.jonathanxd.codeapi.common.CodeModifier;
import com.github.jonathanxd.codeapi.common.CodeParameter;
import com.github.jonathanxd.codeapi.common.InvokeDynamic;
import com.github.jonathanxd.codeapi.common.InvokeType;
import com.github.jonathanxd.codeapi.common.MethodTypeSpec;
import com.github.jonathanxd.codeapi.common.Scope;
import com.github.jonathanxd.codeapi.common.TypeSpec;
import com.github.jonathanxd.codeapi.factory.FieldFactory;
import com.github.jonathanxd.codeapi.factory.VariableFactory;
import com.github.jonathanxd.codeapi.fragment.SimpleMethodFragmentBuilder;
import com.github.jonathanxd.codeapi.helper.Predefined;
import com.github.jonathanxd.codeapi.literal.Literals;
import com.github.jonathanxd.codeapi.operator.Operators;
import com.github.jonathanxd.codeapi.test.Greeter;
import com.github.jonathanxd.codeapi.test.WorldGreeter;
import com.github.jonathanxd.codeapi.type.CodeType;

import org.junit.Test;

import java.io.PrintStream;
import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.MutableCallSite;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.function.Supplier;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

/**
 * Created by jonathan on 03/06/16.
 */
@SuppressWarnings("Duplicates")
public class TestBytecode_Invocations {
    public static final TestBytecode_Invocations INSTANCE = new TestBytecode_Invocations();
    public static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();
    public static final MethodTypeSpec BOOTSTRAP_SPEC = new MethodTypeSpec(
            CodeAPI.getJavaType(TestBytecode_Invocations.class),
            "myBootstrap",
            CodeAPI.typeSpec(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class, Object[].class)
    );

    public static final MethodHandle FALLBACK;

    static {
        try {
            FALLBACK = LOOKUP.findStatic(
                    TestBytecode_Invocations.class,
                    "fallback",
                    MethodType.methodType(Object.class, MyCallSite.class, Object[].class));
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public static CodePart invokePrintln(CodeArgument toPrint) {
        return CodeAPI.invoke(InvokeType.INVOKE_VIRTUAL, CodeAPI.getJavaType(PrintStream.class),
                CodeAPI.accessStaticField(CodeAPI.getJavaType(System.class), CodeAPI.getJavaType(PrintStream.class), "out"),
                "println",
                CodeAPI.voidTypeSpec(Types.OBJECT),
                Collections.singletonList(toPrint));
    }

    public static void bmp(String a, String b) {
        System.out.println("A = " + a + ", B = " + b);
    }

    public static CallSite myBootstrap(MethodHandles.Lookup caller, String name,
                                       MethodType type, Object... parameters) throws Throwable {

        MyCallSite myCallSite = new MyCallSite(caller, name, type);

        MethodHandle methodHandle = FALLBACK.bindTo(myCallSite).asCollector(Object[].class, type.parameterCount()).asType(type);

        myCallSite.setTarget(methodHandle);

        return myCallSite;
    }

    public static Object fallback(MyCallSite callSite, Object[] args) throws Throwable {
        MethodHandle virtual = LOOKUP.findVirtual(TestBytecode_Invocations.class, callSite.name, callSite.type()).bindTo(INSTANCE);

        System.out.println("Invoking '" + callSite.name + "' type: '" + callSite.getTarget().type() + "', with args: '" + Arrays.toString(args) + "' ");

        //return virtual.invokeWithArguments(args);
        throw new RuntimeException("Oops");
    }

    @Test
    public void testBytecode() {
        byte[] bytes = generateTestClass();

        ResultSaver.save(this.getClass(), bytes);

        BCLoader bcLoader = new BCLoader(this.getClass().getClassLoader());

        Class<?> define = bcLoader.define("fullName." + this.getClass().getSimpleName() + "_Generated", bytes);

        System.out.println("Class -> " + Modifier.toString(define.getModifiers()) + " " + define);

        Object o;
        try {
            o = define.newInstance();
            MethodHandles.Lookup lookup = MethodHandles.publicLookup();

            MethodHandle printIt = lookup.findVirtual(define, "printIt", MethodType.methodType(Void.TYPE, Object.class)).bindTo(o);

            try {
                System.out.println("NAO DEVE FALAR HELLO");
                printIt.invoke((Object) null);
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }

            MethodHandle check = lookup.findVirtual(define, "check", MethodType.methodType(Boolean.TYPE, Integer.TYPE)).bindTo(o);

            try {
                System.out.println("CHECK NINE");
                boolean invoke = (boolean) check.invoke(9);

                System.out.println("Invoke = " + invoke);
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }

        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }

        for (Field field : define.getDeclaredFields()) {
            try {
                System.out.println("Field -> " + field + " = " + field.get(o));
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public byte[] generateTestClass() {

        MutableCodeSource codeSource = new MutableCodeSource();
        MutableCodeSource clSource = new MutableCodeSource();

        ClassDeclaration codeClass = ClassDeclarationBuilder.builder()
                .withModifiers(CodeModifier.PUBLIC)
                .withQualifiedName("fullName." + this.getClass().getSimpleName() + "_Generated")
                .withSuperClass(Types.OBJECT)
                .withBody(clSource)
                .build();

        FieldDeclaration codeField = FieldFactory.field(
                EnumSet.of(CodeModifier.PUBLIC, CodeModifier.FINAL),
                Types.STRING,
                "FIELD",
                Literals.STRING("AVD")
        );

        FieldDeclaration codeField2 = FieldFactory.field(
                EnumSet.of(CodeModifier.PUBLIC, CodeModifier.FINAL),
                Types.INT,
                "n",
                Literals.INT(15)
        );

        clSource.add(codeField);
        clSource.add(codeField2);

        CodePart invokeTest = CodeAPI.invoke(InvokeType.INVOKE_VIRTUAL, CodeAPI.getJavaType(PrintStream.class),
                CodeAPI.accessStaticField(CodeAPI.getJavaType(System.class), CodeAPI.getJavaType(PrintStream.class), "out"),
                "println",
                CodeAPI.voidTypeSpec(Types.OBJECT),
                Collections.singletonList(new CodeArgument(Literals.STRING("Hello"))));

        CodePart invokeTest2 = CodeAPI.invoke(InvokeType.INVOKE_VIRTUAL, codeClass, CodeAPI.accessThis(),
                "printIt",
                CodeAPI.voidTypeSpec(Types.OBJECT),
                Collections.singletonList(new CodeArgument(Literals.STRING("Oi"))));

        ConstructorDeclaration codeConstructor = ConstructorDeclarationBuilder.builder()
                .withModifiers(CodeModifier.PUBLIC)
                .withBody(CodeAPI.source(invokeTest, invokeTest2))
                .build();

        clSource.add(codeConstructor);

        clSource.add(TestBytecode.makeCM());
        clSource.add(makeCM2(codeClass));

        codeSource.add(codeClass);

        BytecodeGenerator bytecodeGenerator = new BytecodeGenerator((cl) -> cl.getSimpleName() + ".cai");

        bytecodeGenerator.getOptions().set(BytecodeOptions.VISIT_LINES, VisitLineType.FOLLOW_CODE_SOURCE);

        BytecodeClass bytecodeClass = bytecodeGenerator.gen(codeSource)[0];

        byte[] bytes = bytecodeClass.getBytecode();

        return bytes;


    }

    public MethodDeclaration makeCM2(TypeDeclaration typeDeclaration) {
        MutableCodeSource methodSource = new MutableCodeSource();

        MethodDeclaration codeMethod = MethodDeclarationBuilder.builder()
                .withModifiers(CodeModifier.PUBLIC)
                .withName("check")
                .withReturnType(Types.BOOLEAN)
                .withParameters(new CodeParameter(Types.INT, "x"))
                .withBody(methodSource)
                .build();

        // Invoke BMP

        methodSource.add(
                CodeAPI.invoke(InvokeType.INVOKE_STATIC, TestBytecode_Invocations.class, CodeAPI.accessStatic(),
                        "bmp",
                        CodeAPI.voidTypeSpec(String.class, String.class),
                        Arrays.asList(new CodeArgument(Literals.STRING("xy")), new CodeArgument(Literals.STRING("yz")))
                )
        );

        // Invocations test
        methodSource.add(Predefined.invokePrintln(new CodeArgument(Literals.STRING("Invoke Interface ->"))));

        methodSource.add(
                VariableFactory.variable(CodeAPI.getJavaType(Greeter.class), "greeter", CodeAPI.invokeConstructor(CodeAPI.getJavaType(WorldGreeter.class)))
        );

        MethodInvocation greetingInvoke = CodeAPI.invoke(
                InvokeType.INVOKE_INTERFACE, Greeter.class, CodeAPI.accessLocalVariable(Greeter.class, "greeter"),
                "hello",
                CodeAPI.typeSpec(Types.STRING),
                emptyList());

        VariableDeclaration greetingVar = VariableFactory.variable(Types.STRING, "greetingVar", greetingInvoke);

        methodSource.add(greetingVar);

        methodSource.add(Predefined.invokePrintln(new CodeArgument(CodeAPI.accessLocalVariable(greetingVar))));

        methodSource.add(Predefined.invokePrintln(new CodeArgument(Literals.STRING("Invoke Interface <-"))));

        methodSource.add(Predefined.invokePrintln(new CodeArgument(Literals.STRING("Invoke Dynamic ->"))));

        CodeType supplierType = CodeAPI.getJavaType(Supplier.class);

        ////////////////////////////////////////////////////////////////////////////////////////////

        MethodInvocation dynamicSupplierGet = CodeAPI.Specific.invokeDynamicFragment(
                new InvokeDynamic.LambdaFragment(
                        new MethodTypeSpec(supplierType, "get", CodeAPI.typeSpec(Types.OBJECT)),
                        CodeAPI.typeSpec(Types.STRING),
                        SimpleMethodFragmentBuilder.builder()
                                .withDeclaringType(typeDeclaration)
                                .withScope(Scope.STATIC)
                                .withDescription(new TypeSpec(Types.STRING))
                                .withBody(
                                        CodeAPI.source(
                                                CodeAPI.returnValue(Types.STRING, Literals.STRING("BRB"))
                                        )
                                )
                                .build()
                )
        );
        //
        /*MethodInvocation dynamicSupplierGet = Helper.invokeDynamicFragment(InvokeDynamic.invokeDynamicLambdaFragment(
                new FullMethodSpec(supplierType, PredefinedTypes.OBJECT, "get"),
                new TypeSpec(PredefinedTypes.STRING),
                new MethodFragmentImpl(
                        typeDeclaration, Scope.STATIC, PredefinedTypes.STRING,
                        new CodeParameter[]{},
                        new CodeArgument[]{},
                        Helper.sourceOf(Helper.returnValue(PredefinedTypes.STRING, Literals.STRING("BRB")))
                )));*/

        VariableDeclaration supplierField = VariableFactory.variable(supplierType, "supplier2", dynamicSupplierGet);

        methodSource.add(supplierField);

        methodSource.add(Predefined.invokePrintln(
                CodeAPI.argument(
                        CodeAPI.cast(Types.OBJECT, Types.STRING,
                                CodeAPI.invokeInterface(supplierType, CodeAPI.accessLocalVariable(supplierField), "get",
                                        new TypeSpec(Types.OBJECT),
                                        emptyList())
                        )
                )
        ));

        ////////////////////////////////////////////////////////////////////////////////////////////

        MethodInvocation dynamicGet = CodeAPI.invokeDynamic(
                new InvokeDynamic.LambdaMethodReference(
                        new MethodTypeSpec(supplierType, "get", CodeAPI.typeSpec(Types.OBJECT)),
                        new TypeSpec(Types.STRING)),
                greetingInvoke);

        VariableDeclaration supplierVar = VariableFactory.variable(supplierType, "supplier", dynamicGet);

        methodSource.add(supplierVar);

        CodePart castedGet = CodeAPI.cast(Types.OBJECT, Types.STRING,
                CodeAPI.invokeInterface(
                        supplierType,
                        CodeAPI.accessLocalVariable(supplierVar),
                        "get",
                        new TypeSpec(Types.OBJECT),
                        emptyList()
                )
        );

        VariableDeclaration var2 = VariableFactory.variable(Types.STRING, "str", castedGet);

        methodSource.add(var2);

        methodSource.add(Predefined.invokePrintln(new CodeArgument(CodeAPI.accessLocalVariable(var2))));

        methodSource.add(Predefined.invokePrintln(new CodeArgument(Literals.STRING("Invoke Dynamic <-"))));

        methodSource.add(Predefined.invokePrintln(new CodeArgument(Literals.STRING("Invoke Dynamic Bootstrap ->"))));

        MethodInvocation methodInvocation = CodeAPI.Specific.invokeDynamic(
                new InvokeDynamic.Bootstrap(BOOTSTRAP_SPEC, InvokeType.INVOKE_STATIC, new Object[0]),
                CodeAPI.invoke(InvokeType.INVOKE_VIRTUAL, CodeAPI.getJavaType(TestBytecode_Invocations.class), CodeAPI.accessStatic(),
                        "helloWorld",
                        CodeAPI.typeSpec(Types.VOID, Types.STRING),
                        singletonList(new CodeArgument(Literals.STRING("World")))));

        methodSource.add(methodInvocation);

        methodSource.add(Predefined.invokePrintln(new CodeArgument(Literals.STRING("Invoke Dynamic Bootstrap <-"))));

        methodSource.add(CodeAPI.ifStatement(
                CodeAPI.ifExprs(
                        CodeAPI.check(CodeAPI.accessLocalVariable(Types.INT, "x"), Operators.EQUAL_TO, Literals.INT(9)),
                        Operators.OR,
                        CodeAPI.check(CodeAPI.accessLocalVariable(Types.INT, "x"), Operators.EQUAL_TO, Literals.INT(7))
                ),
                CodeAPI.source(
                        CodeAPI.returnValue(Types.INT, Literals.INT(0))
                )));

        methodSource.add(Predefined.invokePrintln(
                new CodeArgument(CodeAPI.accessLocalVariable(Types.INT, "x"))
        ));

        methodSource.add(CodeAPI.returnValue(Types.INT, Literals.INT(1)));

        return codeMethod;
    }

    public void helloWorld(String name) {
        System.out.println("Hello, " + name);
    }

    public static class MyCallSite extends MutableCallSite {

        final MethodHandles.Lookup callerLookup;
        final String name;

        MyCallSite(MethodHandles.Lookup callerLookup, String name, MethodType type) {
            super(type);
            this.callerLookup = callerLookup;
            this.name = name;
        }

        MyCallSite(MethodHandles.Lookup callerLookup, MethodHandle target, String name) {
            super(target);
            this.callerLookup = callerLookup;
            this.name = name;
        }


    }

    private static final class BCLoader extends ClassLoader {

        protected BCLoader(ClassLoader parent) {
            super(parent);
        }

        public Class<?> define(String name, byte[] bytes) {
            return super.defineClass(name, bytes, 0, bytes.length);
        }
    }


}
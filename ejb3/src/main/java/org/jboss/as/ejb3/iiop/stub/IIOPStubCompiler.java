/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.ejb3.iiop.stub;
// because it calls some ProxyAssembler
// methods that currently are package
// accessible

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.rmi.RemoteException;

import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.classfilewriter.ClassFile;
import org.jboss.classfilewriter.ClassMethod;
import org.jboss.classfilewriter.code.CodeAttribute;
import org.jboss.classfilewriter.util.Boxing;
import org.jboss.classfilewriter.util.DescriptorUtils;
import org.wildfly.iiop.openjdk.rmi.AttributeAnalysis;
import org.wildfly.iiop.openjdk.rmi.ExceptionAnalysis;
import org.wildfly.iiop.openjdk.rmi.InterfaceAnalysis;
import org.wildfly.iiop.openjdk.rmi.OperationAnalysis;
import org.wildfly.iiop.openjdk.rmi.RMIIIOPViolationException;
import org.wildfly.iiop.openjdk.rmi.marshal.CDRStream;
import org.wildfly.iiop.openjdk.rmi.marshal.strategy.StubStrategy;

/**
 * Utility class responsible for the dynamic generation of bytecodes of
 * IIOP stub classes.
 *
 * @author Stuart Douglas
 * @author <a href="mailto:reverbel@ime.usp.br">Francisco Reverbel</a>
 */
public class IIOPStubCompiler {

    public static final String ID_FIELD_NAME = "$ids";

    /**
     * Returns the name of the stub strategy field associated with the method
     * whose index is <code>methodIndex</code>.
     */
    private static String strategy(int methodIndex) {
        return "$s" + methodIndex;
    }

    /**
     * Returns the name of static initializer method associated with the method
     * whose index is <code>methodIndex</code>.
     */
    private static String init(int methodIndex) {
        return "$i" + methodIndex;
    }

    /**
     * Generates the code of a given method within a stub class.
     *
     * @param asm           the <code>ProxyAssembler</code> used to assemble
     *                      the method code
     * @param superclass    the superclass of the stub class within which the
     *                      method will be generated
     * @param m             a <code>Method</code> instance describing the
     *                      method declaration by an RMI/IDL interface
     * @param idlName       a string with the method name mapped to IDL
     * @param strategyField a string with the name of the strategy field that
     *                      will be associated with the generated method
     * @param initMethod    a string with the name of the static initialization
     *                      method that will be associated with the generated
     *                      method.
     */
    private static void generateMethodCode(ClassFile asm,
                                           Class<?> superclass,
                                           Method m,
                                           String idlName,
                                           String strategyField,
                                           String initMethod) {
        Class<?> returnType = m.getReturnType();
        Class<?>[] paramTypes = m.getParameterTypes();
        Class<?>[] exceptions = m.getExceptionTypes();

        // Generate a static field with the StubStrategy for the method
        asm.addField(Modifier.PRIVATE + Modifier.STATIC, strategyField, StubStrategy.class);

        // Generate the method code
        final CodeAttribute ca = asm.addMethod(m).getCodeAttribute();

        // The method code issues a call
        // super.invoke*(idlName, strategyField, args)
        ca.aload(0);
        ca.ldc(idlName);
        ca.getstatic(asm.getName(), strategyField, StubStrategy.class);

        // Push args
        if (paramTypes.length == 0) {
            ca.iconst(0);
            ca.anewarray(Object.class.getName());
            //asm.pushField(Util.class, "NOARGS");
        } else {
            ca.iconst(paramTypes.length);
            ca.anewarray(Object.class.getName());
            int index = 1;
            for (int j = 0; j < paramTypes.length; j++) {

                Class<?> type = paramTypes[j];

                ca.dup();
                ca.iconst(j);
                if (!type.isPrimitive()) {
                    // object or array
                    ca.aload(index);
                } else if (type.equals(double.class)) {
                    ca.dload(index);
                    Boxing.boxDouble(ca);
                    index++;
                } else if (type.equals(long.class)) {
                    ca.lload(index);
                    Boxing.boxLong(ca);
                    index++;
                } else if (type.equals(float.class)) {
                    ca.fload(index);
                    Boxing.boxFloat(ca);
                } else {
                    ca.iload(index);
                    Boxing.boxIfNessesary(ca, DescriptorUtils.makeDescriptor(type));
                }
                index++;
                ca.aastore();
            }
        }
        // Generate the call to an invoke* method ot the superclass
        String invoke = "invoke";
        String ret = "Ljava/lang/Object;";
        if (returnType.isPrimitive() && returnType != Void.TYPE) {
            String typeName = returnType.getName();
            invoke += (Character.toUpperCase(typeName.charAt(0))
                    + typeName.substring(1));
            ret = DescriptorUtils.makeDescriptor(returnType);
        }
        ca.invokevirtual(superclass.getName(), invoke, "(Ljava/lang/String;Lorg/wildfly/iiop/openjdk/rmi/marshal/strategy/StubStrategy;[Ljava/lang/Object;)" + ret);
        if (!returnType.isPrimitive() && returnType != Object.class) {
            ca.checkcast(returnType);
        }
        ca.returnInstruction();

        // Generate a static method that initializes the method's strategy field
        final CodeAttribute init = asm.addMethod(Modifier.PRIVATE + Modifier.STATIC,initMethod, "V").getCodeAttribute();
            int i;
            int len;

            // Push first argument for StubStrategy constructor:
            // array with abbreviated names of the param marshallers
            len = paramTypes.length;
            init.iconst(len);
            init.anewarray(String.class.getName());
            for (i = 0; i < len; i++) {
                init.dup();
                init.iconst(i);
                init.ldc(CDRStream.abbrevFor(paramTypes[i]));
                init.aastore();
            }

            // Push second argument for StubStrategy constructor:
            // array with exception repository ids
            len = exceptions.length;
            int n = 0;
            for (i = 0; i < len; i++) {
                if (!RemoteException.class.isAssignableFrom(exceptions[i])) {
                    n++;
                }
            }
            init.iconst(n);
            init.anewarray(String.class.getName());
            try {
                int j = 0;
                for (i = 0; i < len; i++) {
                    if (!RemoteException.class.isAssignableFrom(exceptions[i])) {
                        init.dup();
                        init.iconst(j);
                        init.ldc(
                                ExceptionAnalysis.getExceptionAnalysis(exceptions[i])
                                        .getExceptionRepositoryId());
                        init.aastore();
                        j++;
                    }
                }
            } catch (RMIIIOPViolationException e) {
                throw EjbLogger.ROOT_LOGGER.exceptionRepositoryNotFound(exceptions[i].getName(), e.getLocalizedMessage());
            }

            // Push third argument for StubStrategy constructor:
            // array with exception class names
            init.iconst(n);
            init.anewarray(String.class.getName());
            int j = 0;
            for (i = 0; i < len; i++) {
                if (!RemoteException.class.isAssignableFrom(exceptions[i])) {
                    init.dup();
                    init.iconst(j);
                    init.ldc(exceptions[i].getName());
                    init.aastore();
                    j++;
                }
            }

            // Push fourth argument for StubStrategy constructor:
            // abbreviated name of the return value marshaller
            init.ldc(CDRStream.abbrevFor(returnType));

            // Push fifth argument for StubStrategy constructor:
            // null (no ClassLoader specified)
            init.aconstNull();

            // Constructs the StubStrategy
            init.invokestatic(StubStrategy.class.getName(), "forMethod", "([Ljava/lang/String;[Ljava/lang/String;[Ljava/lang/String;Ljava/lang/String;Ljava/lang/ClassLoader;)Lorg/wildfly/iiop/openjdk/rmi/marshal/strategy/StubStrategy;");

            // Set the strategy field of this stub class
            init.putstatic(asm.getName(), strategyField, StubStrategy.class);

            init.returnInstruction();
    }

    /**
     * Generates the bytecodes of a stub class for a given interface.
     *
     * @param interfaceAnalysis an <code>InterfaceAnalysis</code> instance
     *                        describing the RMI/IIOP interface to be
     *                        implemented by the stub class
     * @param superclass      the superclass of the stub class
     * @param stubClassName   the name of the stub class
     * @return a byte array with the generated bytecodes.
     */
    private static ClassFile generateCode(InterfaceAnalysis interfaceAnalysis,
                                       Class<?> superclass, String stubClassName) {
        final ClassFile asm =
                new ClassFile(stubClassName,
                        superclass.getName(),
                        interfaceAnalysis.getCls().getName());

        int methodIndex = 0;

        AttributeAnalysis[] attrs = interfaceAnalysis.getAttributes();
        for (int i = 0; i < attrs.length; i++) {
            OperationAnalysis op = attrs[i].getAccessorAnalysis();
            generateMethodCode(asm, superclass, op.getMethod(), op.getIDLName(),
                    strategy(methodIndex), init(methodIndex));
            methodIndex++;
            op = attrs[i].getMutatorAnalysis();
            if (op != null) {
                generateMethodCode(asm, superclass,
                        op.getMethod(), op.getIDLName(),
                        strategy(methodIndex), init(methodIndex));
                methodIndex++;
            }
        }

        final OperationAnalysis[] ops = interfaceAnalysis.getOperations();
        for (int i = 0; i < ops.length; i++) {
            generateMethodCode(asm, superclass,
                    ops[i].getMethod(), ops[i].getIDLName(),
                    strategy(methodIndex), init(methodIndex));
            methodIndex++;
        }

        // Generate the constructor
        final ClassMethod ctor = asm.addMethod(Modifier.PUBLIC, "<init>", "V");
        ctor.getCodeAttribute().aload(0);
        ctor.getCodeAttribute().invokespecial(superclass.getName(), "<init>", "()V");
        ctor.getCodeAttribute().returnInstruction();

        // Generate the method _ids(), declared as abstract in ObjectImpl
        final String[] ids = interfaceAnalysis.getAllTypeIds();
        asm.addField(Modifier.PRIVATE + Modifier.STATIC, ID_FIELD_NAME, String[].class);
        final CodeAttribute idMethod = asm.addMethod(Modifier.PUBLIC + Modifier.FINAL, "_ids", "[Ljava/lang/String;").getCodeAttribute();
        idMethod.getstatic(stubClassName, ID_FIELD_NAME, "[Ljava/lang/String;");
        idMethod.returnInstruction();

        // Generate the static initializer
        final CodeAttribute clinit = asm.addMethod(Modifier.STATIC, "<clinit>", "V").getCodeAttribute();
        clinit.iconst(ids.length);
        clinit.anewarray(String.class.getName());
        for (int i = 0; i < ids.length; i++) {
            clinit.dup();
            clinit.iconst(i);
            clinit.ldc(ids[i]);
            clinit.aastore();
        }
        clinit.putstatic(stubClassName, ID_FIELD_NAME, "[Ljava/lang/String;");

        int n = methodIndex; // last methodIndex + 1
        for (methodIndex = 0; methodIndex < n; methodIndex++) {
            clinit.invokestatic(stubClassName, init(methodIndex), "()V");
        }
        clinit.returnInstruction();

        return asm;
    }

    /**
     * Generates the bytecodes of a stub class for a given interface.
     *
     * @param interfaceAnalysis an <code>InterfaceAnalysis</code> instance
     *                          describing the RMI/IIOP interface to be
     *                          implemented by the stub class
     * @param superclass        the superclass of the stub class
     * @param stubClassName     the name of the stub class
     * @return a byte array with the generated bytecodes.
     */
    private static ClassFile makeCode(InterfaceAnalysis interfaceAnalysis,
                                   Class<?> superclass, String stubClassName) {

        ClassFile code = generateCode(interfaceAnalysis, superclass, stubClassName);
        //try {
        //   String fname = stubClassName;
        //   fname = fname.substring(1 + fname.lastIndexOf('.')) + ".class";
        //   fname = "/tmp/" + fname;
        //   java.io.OutputStream cf = new java.io.FileOutputStream(fname);
        //   cf.write(code);
        //   cf.close();
        //   System.err.println("wrote " + fname);
        //}
        //catch(java.io.IOException ee) {
        //}
        return code;
    }

    // Public method ----------------------------------------------------------

    /**
     * Generates the bytecodes of a stub class for a given interface.
     *
     * @param intf          RMI/IIOP interface to be implemented by the
     *                      stub class
     * @param stubClassName the name of the stub class
     * @return a byte array with the generated bytecodes;
     */
    public static ClassFile compile(Class<?> intf, String stubClassName) {
        InterfaceAnalysis interfaceAnalysis = null;

        try {
            interfaceAnalysis = InterfaceAnalysis.getInterfaceAnalysis(intf);
        } catch (RMIIIOPViolationException e) {
            throw EjbLogger.ROOT_LOGGER.rmiIiopVoliation(e.getLocalizedMessage());
        }
        return makeCode(interfaceAnalysis, DynamicIIOPStub.class, stubClassName);
    }


    public Class<?> compileToClass(Class<?> intf, String stubClassName) {
        return compile(intf, stubClassName).define(intf.getClassLoader(), intf.getProtectionDomain());
    }

}

package org.jboss.as.ejb3.iiop.handle;

import java.io.ObjectInputStream;
import java.lang.reflect.Modifier;

import org.jboss.classfilewriter.ClassFile;
import org.jboss.classfilewriter.ClassMethod;
import org.jboss.classfilewriter.code.CodeAttribute;

/**
 * As ObjectInputStream is broken it looks for the class loader of the last non JDK object on the stack.
 * <p/>
 * As this would normally be from the ejb3 module which can't see deployment modules, we instead define a proxy
 * in the deployment class loader, that simply calls readObject.
 *
 * @author Stuart Douglas
 */
public abstract class SerializationHackProxy {

    public static final String NAME = "org.jboss.as.ejb3.SerializationProxyHackImplementation";

    public Object read(ObjectInputStream stream) {
        return null;
    }


    public static final SerializationHackProxy proxy(final ClassLoader loader) {
        Class<?> clazz;
        try {
            clazz = loader.loadClass(NAME);
        } catch (ClassNotFoundException e) {
            try {
                final ClassFile file = new ClassFile(NAME, SerializationHackProxy.class.getName());

                final ClassMethod method = file.addMethod(Modifier.PUBLIC, "read", "Ljava/lang/Object;", "Ljava/io/ObjectInputStream;");
                final CodeAttribute codeAttribute = method.getCodeAttribute();
                codeAttribute.aload(1);
                codeAttribute.invokevirtual("java/io/ObjectInputStream", "readObject", "()Ljava/lang/Object;");
                codeAttribute.returnInstruction();

                ClassMethod ctor = file.addMethod(Modifier.PUBLIC, "<init>", "V");
                ctor.getCodeAttribute().aload(0);
                ctor.getCodeAttribute().invokespecial(SerializationHackProxy.class.getName(), "<init>", "()V");
                ctor.getCodeAttribute().returnInstruction();

                clazz = file.define(loader);
            } catch (RuntimeException ex) {
                try {
                    clazz = loader.loadClass(NAME);
                } catch (ClassNotFoundException e1) {
                    throw ex;
                }
            }
        }
        try {
            return (SerializationHackProxy) clazz.newInstance();
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

}

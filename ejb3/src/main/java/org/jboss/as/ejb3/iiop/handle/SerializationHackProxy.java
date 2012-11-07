/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

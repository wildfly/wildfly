/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.iiop.stub;

import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.classfilewriter.ClassFile;
import com.sun.corba.se.impl.presentation.rmi.StubFactoryBase;
import com.sun.corba.se.impl.presentation.rmi.StubFactoryFactoryDynamicBase;
import com.sun.corba.se.spi.presentation.rmi.PresentationManager;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * @author Stuart Douglas
 */
public class DynamicStubFactoryFactory extends StubFactoryFactoryDynamicBase {

    @Override
    public PresentationManager.StubFactory makeDynamicStubFactory(final PresentationManager pm, final PresentationManager.ClassData classData, final ClassLoader classLoader) {
        final Class<?> myClass = classData.getMyClass();
        Class<?> theClass = makeStubClass(myClass);
        return new StubFactory(classData, theClass);
    }

    /**
     * Makes a dynamic stub class, if it does not already exist.
     * @param myClass The class to create a stub for
     * @return The dynamic stub class
     */
    public static Class<?> makeStubClass(final Class<?> myClass) {
        final String stubClassName = myClass + "_Stub";
        ClassLoader cl = WildFlySecurityManager.getCurrentContextClassLoaderPrivileged();
        if (cl == null) {
            cl = myClass.getClassLoader();
        }
        if (cl == null) {
            throw EjbLogger.ROOT_LOGGER.couldNotFindClassLoaderForStub(stubClassName);
        }
        Class<?> theClass;
        try {
            theClass = cl.loadClass(stubClassName);
        } catch (ClassNotFoundException e) {
            try {
                final ClassFile clazz = IIOPStubCompiler.compile(myClass, stubClassName);
                theClass = clazz.define(cl, myClass.getProtectionDomain());
            } catch (Throwable ex) {
                //there is a possibility that another thread may have defined the same class in the meantime
                try {
                    theClass = cl.loadClass(stubClassName);
                } catch (ClassNotFoundException e1) {
                    EjbLogger.ROOT_LOGGER.dynamicStubCreationFailed(stubClassName, ex);
                    throw ex;
                }
            }
        }
        return theClass;
    }

    private static final class StubFactory extends StubFactoryBase {

        private final Class<?> clazz;

        protected StubFactory(PresentationManager.ClassData classData, final Class<?> clazz) {
            super(classData);

            this.clazz = clazz;
        }

        @Override
        public org.omg.CORBA.Object makeStub() {
            try {
                return (org.omg.CORBA.Object) clazz.newInstance();
            } catch (InstantiationException e) {
                throw new RuntimeException(e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }
}

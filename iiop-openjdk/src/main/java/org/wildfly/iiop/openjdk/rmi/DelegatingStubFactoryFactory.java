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

package org.wildfly.iiop.openjdk.rmi;

import javax.rmi.CORBA.Tie;

import com.sun.corba.se.impl.presentation.rmi.StubFactoryFactoryBase;
import com.sun.corba.se.impl.presentation.rmi.StubFactoryFactoryProxyImpl;
import com.sun.corba.se.impl.presentation.rmi.StubFactoryFactoryStaticImpl;
import com.sun.corba.se.spi.presentation.rmi.PresentationManager;
import org.wildfly.security.manager.WildFlySecurityManager;

import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * Stub factory
 *
 * @author Stuart Douglas
 */
public class DelegatingStubFactoryFactory extends StubFactoryFactoryBase {

    private final PresentationManager.StubFactoryFactory staticFactory;
    private final PresentationManager.StubFactoryFactory dynamicFactory;

    private static volatile PresentationManager.StubFactoryFactory overriddenDynamicFactory;

    public DelegatingStubFactoryFactory() {
        staticFactory = new StubFactoryFactoryStaticImpl();
        dynamicFactory = new StubFactoryFactoryProxyImpl();
    }

    public PresentationManager.StubFactory createStubFactory(final String className, final boolean isIDLStub, final String remoteCodeBase, final Class expectedClass, final ClassLoader classLoader) {
        if(WildFlySecurityManager.isChecking()) {
            return AccessController.doPrivileged(new PrivilegedAction<PresentationManager.StubFactory>() {
                @Override
                public PresentationManager.StubFactory run() {
                    return getStubFactoryImpl(className, isIDLStub, remoteCodeBase, expectedClass, classLoader);
                }
            });
        } else {
            return getStubFactoryImpl(className, isIDLStub, remoteCodeBase, expectedClass, classLoader);
        }
    }

    private PresentationManager.StubFactory getStubFactoryImpl(String className, boolean isIDLStub, String remoteCodeBase, Class<?> expectedClass, ClassLoader classLoader) {
        try {
            PresentationManager.StubFactory stubFactory = staticFactory.createStubFactory(className, isIDLStub, remoteCodeBase, expectedClass, classLoader);
            if (stubFactory != null) {
                return stubFactory;
            }
        } catch (Exception e) {

        }
        if (overriddenDynamicFactory != null) {
            return overriddenDynamicFactory.createStubFactory(className, isIDLStub, remoteCodeBase, expectedClass, classLoader);

        } else {
            return dynamicFactory.createStubFactory(className, isIDLStub, remoteCodeBase, expectedClass, classLoader);
        }
    }

    public Tie getTie(final Class cls) {
        if (WildFlySecurityManager.isChecking()) {
            return AccessController.doPrivileged(new PrivilegedAction<Tie>() {
                @Override
                public Tie run() {
                    return getTieImpl(cls);
                }
            });
        } else {
            return getTieImpl(cls);
        }
    }

    private Tie getTieImpl(Class<?> cls) {
        try {
            Tie tie = staticFactory.getTie(cls);
            if (tie != null) {
                return tie;
            }
        } catch (Exception e) {

        }
        return dynamicFactory.getTie(cls);
    }

    public boolean createsDynamicStubs() {
        return true;
    }

    public static PresentationManager.StubFactoryFactory getOverriddenDynamicFactory() {
        return overriddenDynamicFactory;
    }

    public static void setOverriddenDynamicFactory(final PresentationManager.StubFactoryFactory overriddenDynamicFactory) {
        DelegatingStubFactoryFactory.overriddenDynamicFactory = overriddenDynamicFactory;
    }
}

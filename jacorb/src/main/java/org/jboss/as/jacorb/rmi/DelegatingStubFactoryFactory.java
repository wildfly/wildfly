package org.jboss.as.jacorb.rmi;

import org.jboss.com.sun.corba.se.impl.presentation.rmi.StubFactoryFactoryBase;
import org.jboss.com.sun.corba.se.impl.presentation.rmi.StubFactoryFactoryProxyImpl;
import org.jboss.com.sun.corba.se.impl.presentation.rmi.StubFactoryFactoryStaticImpl;
import org.jboss.com.sun.corba.se.spi.presentation.rmi.PresentationManager;

import javax.rmi.CORBA.Tie;

/**
 * Stub factory
 *
 * @author Stuart Douglas
 */
public class DelegatingStubFactoryFactory extends StubFactoryFactoryBase {

    private final StubFactoryFactoryBase staticFactory;
    private final StubFactoryFactoryBase dynamicFactory;

    public DelegatingStubFactoryFactory() {
        staticFactory = new StubFactoryFactoryStaticImpl();
        dynamicFactory = new StubFactoryFactoryProxyImpl();
    }

    public PresentationManager.StubFactory createStubFactory(final String className, final boolean isIDLStub, final String remoteCodeBase, final Class<?> expectedClass, final ClassLoader classLoader) {
        try {
            PresentationManager.StubFactory stubFactory = staticFactory.createStubFactory(className, isIDLStub, remoteCodeBase, expectedClass, classLoader);
            if (stubFactory != null) {
                return stubFactory;
            }
        } catch (Exception e) {

        }
        return dynamicFactory.createStubFactory(className, isIDLStub, remoteCodeBase, expectedClass, classLoader);
    }

    public Tie getTie(final Class<?> cls) {
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
}

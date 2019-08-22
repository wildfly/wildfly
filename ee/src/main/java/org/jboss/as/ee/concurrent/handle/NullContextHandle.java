package org.jboss.as.ee.concurrent.handle;

import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * A context handle without invocation context to set. For now it provides only the setup and reset of TCCL captured at handle creation.
 * @author Eduardo Martins
 */
public class NullContextHandle implements SetupContextHandle {

    private static final long serialVersionUID = 2928225776829357837L;
    private final SetupContextHandle tcclSetupHandle;

    public NullContextHandle() {
        tcclSetupHandle = new ClassLoaderContextHandleFactory.ClassLoaderSetupContextHandle(WildFlySecurityManager.getCurrentContextClassLoaderPrivileged());
    }

    @Override
    public ResetContextHandle setup() throws IllegalStateException {
        return tcclSetupHandle.setup();
    }

    @Override
    public String getFactoryName() {
        return "NULL";
    }

}

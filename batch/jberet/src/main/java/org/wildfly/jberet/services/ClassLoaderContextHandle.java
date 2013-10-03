package org.wildfly.jberet.services;

import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
class ClassLoaderContextHandle implements ContextHandle {
    private final ClassLoader classLoader;

    ClassLoaderContextHandle(final ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    @Override
    public Handle setup() {
        final ClassLoader current = WildFlySecurityManager.getCurrentContextClassLoaderPrivileged();
        WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(classLoader);
        return new Handle() {
            @Override
            public void tearDown() {
                WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(current);
            }
        };
    }
}

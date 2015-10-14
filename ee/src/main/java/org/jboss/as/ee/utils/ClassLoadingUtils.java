package org.jboss.as.ee.utils;

import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.modules.Module;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * @author Stuart Douglas
 */
public class ClassLoadingUtils {

    public static Class<?> loadClass(final String className, final DeploymentUnit du) throws ClassNotFoundException {
        return loadClass(className, du.getAttachment(Attachments.MODULE));
    }

    public static Class<?> loadClass(final String className, final Module module) throws ClassNotFoundException {
        final ClassLoader oldTccl = WildFlySecurityManager.getCurrentContextClassLoaderPrivileged();
        try {
            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(module.getClassLoader());
            return Class.forName(className, false, module.getClassLoader());
        } finally {
            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(oldTccl);
        }
    }

    private ClassLoadingUtils() {

    }
}

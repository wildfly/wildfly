package org.jboss.as.ee.component;

import java.security.AccessController;
import java.security.PrivilegedAction;

import org.jboss.as.naming.ConstructorManagedReferenceFactory;
import org.jboss.as.naming.ManagedReference;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * @author Stuart Douglas
 */
public class ReflectiveClassIntrospector implements EEClassIntrospector, Service<EEClassIntrospector> {

    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("ee", "reflectiveClassIntrospector");
    public static final RuntimePermission CHECK_MEMBER_ACCESS_PERMISSION = new RuntimePermission("accessDeclaredMembers");

    /**
     * Constructs a new instance.
     *
     * @throws SecurityException if the security manager is present and the runtime {@code accessDeclaredMembers}
     *                           access
     *                           is not granted
     */
    public ReflectiveClassIntrospector() {
        // If security manager is enabled check the permissions that would normally be checked in
        // Class.getDeclaredConstructor()
        if (WildFlySecurityManager.isChecking()) {
            SecurityManager s = System.getSecurityManager();
            s.checkPermission(CHECK_MEMBER_ACCESS_PERMISSION);
        }
    }

    @Override
    public ManagedReferenceFactory createFactory(final Class<?> clazz) {
        if (WildFlySecurityManager.isChecking()) {
            // Execute in a privileged block for executions, such as JSP's, that do not copy the security
            // context/protection domains onto class loaders. The permission check is done on the constructor.
            return AccessController.doPrivileged(new PrivilegedAction<ManagedReferenceFactory>() {
                @Override
                public ManagedReferenceFactory run() {
                    try {
                        return new ConstructorManagedReferenceFactory(clazz.getDeclaredConstructor());
                    } catch (NoSuchMethodException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
        } else {
            try {
                return new ConstructorManagedReferenceFactory(clazz.getDeclaredConstructor());
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public ManagedReference createInstance(Object instance) {
        return null;
    }

    @Override
    public void start(StartContext startContext) throws StartException {
    }

    @Override
    public void stop(StopContext stopContext) {
    }

    @Override
    public EEClassIntrospector getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }
}

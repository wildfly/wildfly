package org.wildfly.jberet.services;

import java.security.AccessController;
import java.security.PrivilegedAction;

import org.jboss.security.SecurityContext;
import org.jboss.security.SecurityContextAssociation;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
class SecurityContextHandle implements ContextHandle {
    private final SecurityContext securityContext;

    SecurityContextHandle() {
        this.securityContext = getSecurityContext();
    }

    @Override
    public Handle setup() {
        final SecurityContext current = getSecurityContext();
        setSecurityContext(securityContext);
        return new Handle() {
            @Override
            public void tearDown() {
                setSecurityContext(current);
            }
        };
    }

    private static SecurityContext getSecurityContext() {
        if (WildFlySecurityManager.isChecking()) {
            return AccessController.doPrivileged(new PrivilegedAction<SecurityContext>() {
                @Override
                public SecurityContext run() {
                    return SecurityContextAssociation.getSecurityContext();
                }
            });
        }
        return SecurityContextAssociation.getSecurityContext();
    }

    private static void setSecurityContext(final SecurityContext securityContext) {
        if (WildFlySecurityManager.isChecking()) {
            AccessController.doPrivileged(new PrivilegedAction<Void>() {
                @Override
                public Void run() {
                    SecurityContextAssociation.setSecurityContext(securityContext);
                    return null;
                }
            });
        } else {
            SecurityContextAssociation.setSecurityContext(securityContext);
        }
    }
}

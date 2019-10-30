package org.wildfly.iiop.openjdk.csiv2;

import org.jboss.security.RunAs;
import org.jboss.security.SecurityContextAssociation;
import org.wildfly.security.manager.WildFlySecurityManager;

import java.security.AccessController;
import java.security.Principal;
import java.security.PrivilegedAction;

/**
 * @author Stuart Douglas
 */
class SecurityActions {

    private static final PrivilegedAction<Principal> GET_PRINCIPLE_ACTION = new PrivilegedAction<Principal>() {
        @Override
        public Principal run() {
            return SecurityContextAssociation.getPrincipal();
        }
    };

    private static final PrivilegedAction<Object> GET_CREDENTIAL_ACTION = new PrivilegedAction<Object>() {
        @Override
        public Object run() {
            return SecurityContextAssociation.getCredential();
        }
    };

    private static final PrivilegedAction<RunAs> PEEK_RUN_AS_IDENTITY_ACTION = new PrivilegedAction<RunAs>() {
        @Override
        public RunAs run() {
            return SecurityContextAssociation.peekRunAsIdentity();
        }
    };

    static Principal getPrincipal() {
        if(WildFlySecurityManager.isChecking()) {
            return AccessController.doPrivileged(GET_PRINCIPLE_ACTION);
        } else {
            return SecurityContextAssociation.getPrincipal();
        }
    }

    static Object getCredential() {
        if(WildFlySecurityManager.isChecking()) {
            return AccessController.doPrivileged(GET_CREDENTIAL_ACTION);
        } else {
            return SecurityContextAssociation.getCredential();
        }
    }

    static RunAs peekRunAsIdentity() {
        if(WildFlySecurityManager.isChecking()) {
            return AccessController.doPrivileged(PEEK_RUN_AS_IDENTITY_ACTION);
        } else {
            return SecurityContextAssociation.peekRunAsIdentity();
        }
    }
}

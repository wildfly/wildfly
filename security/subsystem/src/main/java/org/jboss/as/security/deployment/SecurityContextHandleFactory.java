/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.jboss.as.security.deployment;

import org.jboss.as.ee.concurrent.handle.ContextHandleFactory;
import org.jboss.as.ee.concurrent.handle.ResetContextHandle;
import org.jboss.as.ee.concurrent.handle.SetupContextHandle;
import org.jboss.security.SecurityContext;
import org.jboss.security.SecurityContextAssociation;
import org.wildfly.security.manager.WildFlySecurityManager;

import javax.enterprise.concurrent.ContextService;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Map;

/**
 * The context handle factory responsible for saving and setting the security context.
 *
 * @author Eduardo Martins
 */
public class SecurityContextHandleFactory implements ContextHandleFactory {

    public static final String NAME = "SECURITY";

    public static final SecurityContextHandleFactory INSTANCE = new SecurityContextHandleFactory();

    private SecurityContextHandleFactory() {
    }

    @Override
    public SetupContextHandle saveContext(ContextService contextService, Map<String, String> contextObjectProperties) {
        return new SecuritySetupContextHandle();
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public int getChainPriority() {
        return 300;
    }

    @Override
    public void writeSetupContextHandle(SetupContextHandle contextHandle, ObjectOutputStream out) throws IOException {
        out.writeObject(contextHandle);
    }

    @Override
    public SetupContextHandle readSetupContextHandle(ObjectInputStream in) throws IOException, ClassNotFoundException {
        return (SetupContextHandle) in.readObject();
    }

    private static class SecuritySetupContextHandle implements SetupContextHandle {

        private final SecurityContext securityContext;

        private SecuritySetupContextHandle() {
            if (WildFlySecurityManager.isChecking()) {
                this.securityContext = AccessController.doPrivileged(new PrivilegedAction<SecurityContext>() {
                    @Override
                    public SecurityContext run() {
                        return saveSecurityContext();
                    }
                });
            } else {
                this.securityContext = saveSecurityContext();
            }
        }

        private SecurityContext saveSecurityContext() {
            return SecurityContextAssociation.getSecurityContext();
        }

        @Override
        public String getFactoryName() {
            return NAME;
        }

        @Override
        public ResetContextHandle setup() throws IllegalStateException {
            final SecurityContext previous;
            if (WildFlySecurityManager.isChecking()) {
                previous = AccessController.doPrivileged(new PrivilegedAction<SecurityContext>() {
                    @Override
                    public SecurityContext run() {
                        return setupSecurityContext();
                    }
                });
            } else {
                previous = setupSecurityContext();
            }
            return new SecurityResetContextHandle(previous);
        }

        private SecurityContext setupSecurityContext() {
            final SecurityContext previous = SecurityContextAssociation.getSecurityContext();
            SecurityContextAssociation.setSecurityContext(securityContext);
            return previous;
        }
    }

    private static class SecurityResetContextHandle implements ResetContextHandle {

        private final SecurityContext previous;

        private SecurityResetContextHandle(SecurityContext previous) {
            this.previous = previous;
        }

        @Override
        public String getFactoryName() {
            return NAME;
        }

        @Override
        public void reset() {
            if (WildFlySecurityManager.isChecking()) {
                AccessController.doPrivileged(new PrivilegedAction<Void>() {
                    @Override
                    public Void run() {
                        resetSecurityContext();
                        return null;
                    }
                });
            } else {
                resetSecurityContext();
            }
        }

        private void resetSecurityContext() {
            SecurityContextAssociation.setSecurityContext(previous);
        }
    }
}

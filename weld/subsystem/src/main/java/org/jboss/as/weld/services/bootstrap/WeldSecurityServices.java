/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.weld.services.bootstrap;

import java.security.AccessController;
import java.security.Principal;
import java.security.PrivilegedAction;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jboss.as.core.security.ServerSecurityManager;
import org.jboss.as.weld.ServiceNames;
import org.jboss.as.weld.logging.WeldLogger;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.security.SecurityContext;
import org.jboss.security.SecurityContextAssociation;
import org.jboss.weld.security.spi.SecurityServices;
import org.wildfly.security.auth.server.SecurityDomain;
import org.wildfly.security.auth.server.SecurityIdentity;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public class WeldSecurityServices implements Service, SecurityServices {

    public static final ServiceName SERVICE_NAME = ServiceNames.WELD_SECURITY_SERVICES_SERVICE_NAME;
    private final Consumer<SecurityServices> securityServicesConsumer;
    // This is a Supplier<ServerSecurityManager>. I use ? even though with type erasure
    // that doesn't matter, just to make it harder for someone to modify this class and
    // accidentally introduce any unnecessary loading of ServerSecurityManager
    private final Supplier<?> securityManagerSupplier;

    public WeldSecurityServices(final Consumer<SecurityServices> securityServicesConsumer, final Supplier<?> securityManagerSupplier) {
        this.securityServicesConsumer = securityServicesConsumer;
        this.securityManagerSupplier = securityManagerSupplier;
    }

    @Override
    public void start(final StartContext context) throws StartException {
        securityServicesConsumer.accept(this);
    }

    @Override
    public void stop(final StopContext context) {
        securityServicesConsumer.accept(null);
    }

    @Override
    public Principal getPrincipal() {
        SecurityDomain elytronDomain = getCurrentSecurityDomain();
        if(elytronDomain != null) {
            return elytronDomain.getCurrentSecurityIdentity().getPrincipal();
        }

        // Use 'Object' initially to avoid loading ServerSecurityManager (which may not be present)
        // until we know for sure we need it.
        final Object securityManager = securityManagerSupplier != null ? securityManagerSupplier.get() : null;
        if (securityManager == null)
            throw WeldLogger.ROOT_LOGGER.securityNotEnabled();
        if (WildFlySecurityManager.isChecking()) {
            return AccessController.doPrivileged((PrivilegedAction<Principal>) ((ServerSecurityManager) securityManager)::getCallerPrincipal);
        } else {
            return ((ServerSecurityManager)securityManager).getCallerPrincipal();
        }
    }

    @Override
    public void cleanup() {
    }

    @Override
    public org.jboss.weld.security.spi.SecurityContext getSecurityContext() {
        if (securityManagerSupplier == null) {
            return SecurityServices.super.getSecurityContext();
        }

        SecurityContext ctx;
        if (WildFlySecurityManager.isChecking()) {
            ctx = AccessController.doPrivileged((PrivilegedAction<SecurityContext>) () -> SecurityContextAssociation.getSecurityContext());
        } else {
            ctx = SecurityContextAssociation.getSecurityContext();
        }
        return new WeldSecurityContext(ctx);
    }

    @Override
    public Consumer<Runnable> getSecurityContextAssociator(){
        SecurityDomain elytronDomain = getCurrentSecurityDomain();
        if(elytronDomain != null) {
            // store the identity from the original thread and use it in callback which will be invoked in a different thread
            SecurityIdentity storedSecurityIdentity = elytronDomain.getCurrentSecurityIdentity();
            return (action) -> storedSecurityIdentity.runAs(action);
        } else {
            return SecurityServices.super.getSecurityContextAssociator();
        }
    }

    private SecurityDomain getCurrentSecurityDomain() {
        if (WildFlySecurityManager.isChecking()) {
            return AccessController.doPrivileged((PrivilegedAction<SecurityDomain>) () -> SecurityDomain.getCurrent());
        } else {
            return SecurityDomain.getCurrent();
        }
    }

    static class WeldSecurityContext implements org.jboss.weld.security.spi.SecurityContext, PrivilegedAction<Void> {

        private final SecurityContext ctx;

        WeldSecurityContext(SecurityContext ctx) {
            this.ctx = ctx;
        }

        @Override
        public void associate() {
            if (WildFlySecurityManager.isChecking()) {
                AccessController.doPrivileged((PrivilegedAction<Void>) () -> this.run());
            } else {
                run();
            }
        }

        @Override
        public void dissociate() {
            if (WildFlySecurityManager.isChecking()) {
                AccessController.doPrivileged((PrivilegedAction<Void>)() -> {
                    SecurityContextAssociation.clearSecurityContext();
                    return null;
                });
            } else {
                SecurityContextAssociation.clearSecurityContext();
            }
        }

        @Override
        public void close() {
        }

        @Override
        public Void run() {
            SecurityContextAssociation.setSecurityContext(ctx);
            return null;
        }
    }
}

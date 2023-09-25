/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.weld.services.bootstrap;

import java.security.AccessController;
import java.security.Principal;
import java.security.PrivilegedAction;
import java.util.function.Consumer;

import org.jboss.as.weld.ServiceNames;
import org.jboss.as.weld.logging.WeldLogger;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
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

    public WeldSecurityServices(final Consumer<SecurityServices> securityServicesConsumer) {
        this.securityServicesConsumer = securityServicesConsumer;
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

        throw WeldLogger.ROOT_LOGGER.securityNotEnabled();
    }

    @Override
    public void cleanup() {
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

}

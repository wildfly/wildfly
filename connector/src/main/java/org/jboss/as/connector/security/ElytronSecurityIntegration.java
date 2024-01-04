/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.connector.security;

import java.security.AccessController;

import javax.security.auth.callback.CallbackHandler;

import org.jboss.as.connector.logging.ConnectorLogger;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.server.CurrentServiceContainer;
import org.jboss.jca.core.spi.security.Callback;
import org.jboss.jca.core.spi.security.SecurityContext;
import org.jboss.jca.core.spi.security.SecurityIntegration;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceName;
import org.wildfly.security.auth.server.SecurityDomain;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * An Elytron based {@link SecurityIntegration} implementation.
 *
 * @author Flavia Rainone
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
public class ElytronSecurityIntegration implements SecurityIntegration {

    static final String SECURITY_IDENTITY_ROLE = "ejb";

    private static final String SECURITY_DOMAIN_CAPABILITY =  "org.wildfly.security.security-domain";

    private static final RuntimeCapability<Void> SECURITY_DOMAIN_RUNTIME_CAPABILITY = RuntimeCapability
            .Builder.of(SECURITY_DOMAIN_CAPABILITY, true, SecurityDomain.class)
            .build();

    private final ThreadLocal<SecurityContext> securityContext = new ThreadLocal<>();

    @Override
    public SecurityContext createSecurityContext(String sd) throws Exception {
        return new ElytronSecurityContext();
    }

    @Override
    public SecurityContext getSecurityContext() {
        return this.securityContext.get();
    }

    @Override
    public void setSecurityContext(SecurityContext context) {
        this.securityContext.set(context);
    }

    @Override
    public CallbackHandler createCallbackHandler() {
        // we need a Callback to retrieve the Elytron security domain that will be used by the CallbackHandler.
        throw ConnectorLogger.ROOT_LOGGER.unsupportedCreateCallbackHandlerMethod();
    }

    @Override
    public CallbackHandler createCallbackHandler(final Callback callback) {
        assert callback != null;
        // TODO switch to use the elytron security domain once the callback has that info available.
        final String securityDomainName = callback.getDomain();
        // get domain reference from the service container and create the callback handler using the domain.
        if (securityDomainName != null) {
            final ServiceContainer container = this.currentServiceContainer();
            final ServiceName securityDomainServiceName = SECURITY_DOMAIN_RUNTIME_CAPABILITY.getCapabilityServiceName(securityDomainName);
            final SecurityDomain securityDomain = (SecurityDomain) container.getRequiredService(securityDomainServiceName).getValue();
            return new ElytronCallbackHandler(securityDomain, callback);
        }
        // TODO use subsystem logger for the exception.
        throw ConnectorLogger.ROOT_LOGGER.invalidCallbackSecurityDomain();
    }

    /**
     * Get a reference to the current {@link ServiceContainer}.
     *
     * @return a reference to the current {@link ServiceContainer}.
     */
    private ServiceContainer currentServiceContainer() {
        if(WildFlySecurityManager.isChecking()) {
            return AccessController.doPrivileged(CurrentServiceContainer.GET_ACTION);
        }
        return CurrentServiceContainer.getServiceContainer();
    }
}

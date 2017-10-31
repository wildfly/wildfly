/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.security.service;

import java.util.concurrent.ConcurrentMap;

import javax.security.auth.login.Configuration;

import org.jboss.as.security.SecurityExtension;
import org.jboss.as.security.logging.SecurityLogger;
import org.jboss.as.security.plugins.AuthenticationCacheFactory;
import org.jboss.as.security.plugins.DefaultAuthenticationCacheFactory;
import org.jboss.as.security.plugins.JNDIBasedSecurityManagement;
import org.jboss.as.security.plugins.SecurityDomainContext;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.security.ISecurityManagement;
import org.jboss.security.JSSESecurityDomain;
import org.jboss.security.config.ApplicationPolicy;
import org.jboss.security.config.ApplicationPolicyRegistration;

/**
 * Service to install security domains.
 *
 * @author <a href="mailto:mmoyses@redhat.com">Marcus Moyses</a>
 */
public class SecurityDomainService implements Service<SecurityDomainContext> {

    public static final ServiceName SERVICE_NAME = SecurityExtension.JBOSS_SECURITY.append("security-domain");

    private final InjectedValue<ISecurityManagement> securityManagementValue = new InjectedValue<ISecurityManagement>();

    private final InjectedValue<Configuration> configurationValue = new InjectedValue<Configuration>();

    private final InjectedValue<ConcurrentMap> cacheValue = new InjectedValue<>();

    private final String name;

    private final ApplicationPolicy applicationPolicy;

    private final JSSESecurityDomain jsseSecurityDomain;

    private volatile SecurityDomainContext securityDomainContext;

    private final String cacheType;

    public SecurityDomainService(String name, ApplicationPolicy applicationPolicy, JSSESecurityDomain jsseSecurityDomain,
            String cacheType) {
        this.name = name;
        this.applicationPolicy = applicationPolicy;
        this.jsseSecurityDomain = jsseSecurityDomain;
        this.cacheType = cacheType;
    }

    /** {@inheritDoc} */
    @Override
    public void start(StartContext context) throws StartException {
        SecurityLogger.ROOT_LOGGER.debugf("Starting SecurityDomainService(%s)", name);
        if (applicationPolicy != null) {
            final ApplicationPolicyRegistration applicationPolicyRegistration = (ApplicationPolicyRegistration) configurationValue
                    .getValue();
            applicationPolicyRegistration.addApplicationPolicy(applicationPolicy.getName(), applicationPolicy);
        }
        final JNDIBasedSecurityManagement securityManagement = (JNDIBasedSecurityManagement) securityManagementValue.getValue();
        AuthenticationCacheFactory cacheFactory = null;
        if ("infinispan".equals(cacheType)) {
            cacheFactory = () -> this.cacheValue.getValue();
        } else if ("default".equals(cacheType)) {
            cacheFactory = new DefaultAuthenticationCacheFactory();
        }
        SecurityDomainContext sdc;
        try {
            sdc = securityManagement.createSecurityDomainContext(name, cacheFactory, jsseSecurityDomain);
        } catch (Exception e) {
            throw SecurityLogger.ROOT_LOGGER.unableToStartException("SecurityDomainService", e);
        }
        if (jsseSecurityDomain != null) {
            try {
                jsseSecurityDomain.reloadKeyAndTrustStore();
            } catch (Exception e) {
                throw SecurityLogger.ROOT_LOGGER.unableToStartException("SecurityDomainService", e);
            }
        }
        securityManagement.getSecurityManagerMap().put(name, sdc);
        this.securityDomainContext = sdc;
    }

    /** {@inheritDoc} */
    @Override
    public void stop(StopContext context) {
        SecurityLogger.ROOT_LOGGER.debugf("Stopping security domain service %s", name);
        final JNDIBasedSecurityManagement securityManagement = (JNDIBasedSecurityManagement) securityManagementValue.getValue();
        securityManagement.removeSecurityDomain(name);
        // TODO clear auth cache?
        final ApplicationPolicyRegistration applicationPolicyRegistration = (ApplicationPolicyRegistration) configurationValue
                .getValue();
        applicationPolicyRegistration.removeApplicationPolicy(name);
    }

    /** {@inheritDoc} */
    @Override
    public SecurityDomainContext getValue() throws IllegalStateException, IllegalArgumentException {
        return securityDomainContext;
    }

    /**
     * Target {@code Injector}
     *
     * @return target
     */
    public Injector<ISecurityManagement> getSecurityManagementInjector() {
        return securityManagementValue;
    }

    /**
     * Target {@code Injector}
     *
     * @return target
     */
    public Injector<Configuration> getConfigurationInjector() {
        return configurationValue;
    }

    /**
     * Target {@code Injector}
     *
     * @return target
     */
    public Injector<ConcurrentMap> getCacheInjector() {
        return cacheValue;
    }
}

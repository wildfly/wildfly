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

package org.jboss.as.web.security;

import java.util.Set;

import javax.security.jacc.PolicyContext;

import org.apache.catalina.Realm;
import org.jboss.as.security.plugins.SecurityDomainContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.security.SecurityConstants;

/**
 * Service to install the default {@code Realm} implementation.
 *
 * @author <a href="mailto:mmoyses@redhat.com">Marcus Moyses</a>
 */
public class JBossWebRealmService implements Service<Realm> {

    private volatile Realm realm;

    private final InjectedValue<SecurityDomainContext> securityDomainContextValue = new InjectedValue<SecurityDomainContext>();

    private final DeploymentUnit deploymentUnit;

    public JBossWebRealmService(DeploymentUnit deploymentUnit) {
        this.deploymentUnit = deploymentUnit;
    }

    /** {@inheritDoc} */
    @Override
    public void start(StartContext context) throws StartException {
        JBossWebRealm jbossWebRealm = new JBossWebRealm();
        SecurityDomainContext sdc = securityDomainContextValue.getValue();
        jbossWebRealm.setAuthenticationManager(sdc.getAuthenticationManager());
        jbossWebRealm.setAuthorizationManager(sdc.getAuthorizationManager());
        jbossWebRealm.setMappingManager(sdc.getMappingManager());
        jbossWebRealm.setAuditManager(sdc.getAuditManager());
        jbossWebRealm.setDeploymentUnit(deploymentUnit);
        this.realm = jbossWebRealm;
    }

    /** {@inheritDoc} */
    @SuppressWarnings("rawtypes")
    @Override
    public void stop(StopContext context) {
        realm = null;
    }

    /** {@inheritDoc} */
    @Override
    public Realm getValue() throws IllegalStateException, IllegalArgumentException {
        return realm;
    }

    /**
     * Target {@code Injector}
     *
     * @return target
     */
    public Injector<SecurityDomainContext> getSecurityDomainContextInjector() {
        return securityDomainContextValue;
    }

}

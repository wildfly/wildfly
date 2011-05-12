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
package org.jboss.as.webservices.service;

import javax.management.ObjectName;

import org.jboss.as.security.plugins.SecurityDomainContext;
import org.jboss.as.security.service.SecurityDomainService;
import org.jboss.as.webservices.security.SecurityDomainContextAdaptor;
import org.jboss.as.webservices.util.WSServices;
import org.jboss.logging.Logger;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceBuilder.DependencyType;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.security.SecurityConstants;
import org.jboss.security.SecurityUtil;
import org.jboss.wsf.spi.deployment.Endpoint;

/**
 * WS endpoint service; this is meant for setting the lazy deployment time info into the Endpoint (stuff coming from
 * dependencies upon other AS services that are started during the deployment)
 *
 * @author alessio.soldano@jboss.com
 * @since 13-May-2011
 */
public final class EndpointService implements Service<Endpoint> {

    private static final Logger log = Logger.getLogger(EndpointService.class);
    private final Endpoint endpoint;
    private ServiceName name;
    private final InjectedValue<SecurityDomainContext> securityDomainContextValue = new InjectedValue<SecurityDomainContext>();

    private EndpointService(final Endpoint endpoint) {
        this.endpoint = endpoint;
        final ObjectName on = endpoint.getName();
        this.name = WSServices.ENDPOINT_SERVICE.append(on.getKeyProperty(Endpoint.SEPID_PROPERTY_CONTEXT)).append(
                on.getKeyProperty(Endpoint.SEPID_PROPERTY_ENDPOINT));
    }

    @Override
    public Endpoint getValue() {
        return endpoint;
    }

    public ServiceName getName() {
        return name;
    }

    @Override
    public void start(final StartContext context) throws StartException {
        log.infof("Starting %s", name);
        endpoint.setSecurityDomainContext(new SecurityDomainContextAdaptor(securityDomainContextValue.getValue()));
    }

    @Override
    public void stop(final StopContext context) {
        log.infof("Stopping %s", name);
        endpoint.setSecurityDomainContext(null);
    }

    /**
     * Target {@code Injector}
     *
     * @return target
     */
    public Injector<SecurityDomainContext> getSecurityDomainContextInjector() {
        return securityDomainContextValue;
    }

    public static void install(final ServiceTarget serviceTarget, final Endpoint endpoint) {
        final EndpointService service = new EndpointService(endpoint);
        final ServiceBuilder<Endpoint> builder = serviceTarget.addService(service.getName(), service);
        builder.addDependency(DependencyType.REQUIRED,
                SecurityDomainService.SERVICE_NAME.append(getDeploymentSecurityDomainName(endpoint)),
                SecurityDomainContext.class, service.getSecurityDomainContextInjector());
        builder.setInitialMode(Mode.ACTIVE);
        builder.install();
    }

    private static String getDeploymentSecurityDomainName(Endpoint ep) {
        JBossWebMetaData metadata = ep.getService().getDeployment().getAttachment(JBossWebMetaData.class);
        String metaDataSecurityDomain = metadata != null ? metadata.getSecurityDomain() : null;
        return metaDataSecurityDomain == null ? SecurityConstants.DEFAULT_APPLICATION_POLICY : SecurityUtil
                .unprefixSecurityDomain(metaDataSecurityDomain.trim());
    }
}

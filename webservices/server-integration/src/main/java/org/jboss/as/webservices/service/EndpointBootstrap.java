/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

import static org.jboss.as.webservices.WSLogger.ROOT_LOGGER;

import java.util.List;

import javax.management.JMException;
import javax.management.MBeanServer;

import org.jboss.as.security.plugins.SecurityDomainContext;
import org.jboss.as.webservices.security.SecurityDomainContextAdaptor;
import org.jboss.as.webservices.util.WebAppController;
import org.jboss.msc.value.Value;
import org.jboss.ws.api.monitoring.RecordProcessor;
import org.jboss.ws.common.ObjectNameFactory;
import org.jboss.ws.common.monitoring.ManagedRecordProcessor;
import org.jboss.wsf.spi.deployment.Deployment;
import org.jboss.wsf.spi.deployment.Endpoint;
import org.jboss.wsf.spi.management.EndpointRegistry;
import org.jboss.wsf.spi.metadata.webservices.WebservicesMetaData;

/**
 * Bootstrap JBossWS Endpoint
 *
 * @author alessio.soldano@jboss.com
 */
public class EndpointBootstrap {

    private final Value<SecurityDomainContext> securityDomainContextValue;
    private final Value<WebAppController> pclWebAppControllerValue;
    private final Value<EndpointRegistry> endpointRegistryValue;
    private final Value<MBeanServer> mBeanServerValue;
    private final Endpoint endpoint;

    public EndpointBootstrap(final Endpoint endpoint, final Value<SecurityDomainContext> securityDomainContextValue,
            final Value<WebAppController> pclWebAppControllerValue, final Value<EndpointRegistry> endpointRegistryValue,
            final Value<MBeanServer> mBeanServerValue) {
        this.endpoint = endpoint;
        this.securityDomainContextValue = securityDomainContextValue;
        this.pclWebAppControllerValue = pclWebAppControllerValue;
        this.endpointRegistryValue = endpointRegistryValue;
        this.mBeanServerValue = mBeanServerValue;
    }

    public Endpoint getEndpoint() {
        return endpoint;
    }

    public void start() {
        if (securityDomainContextValue != null) {
            endpoint.setSecurityDomainContext(new SecurityDomainContextAdaptor(securityDomainContextValue.getValue()));
        }
        if (pclWebAppControllerValue != null) {
            if (hasWebservicesMD(endpoint)) { //basically JAX-RPC deployments require the PortComponentLinkServlet to be available
                pclWebAppControllerValue.getValue().incrementUsers();
            }
        }
        if (mBeanServerValue != null) {
            final List<RecordProcessor> processors = endpoint.getRecordProcessors();
            for (final RecordProcessor processor : processors) {
               registerRecordProcessor(processor, endpoint);
            }
        }
        if (endpointRegistryValue != null) {
            endpointRegistryValue.getValue().register(endpoint);
        }
    }

    public void stop() {
        if (securityDomainContextValue != null) {
            endpoint.setSecurityDomainContext(null);
        }
        if (pclWebAppControllerValue != null && hasWebservicesMD(endpoint)) {
            pclWebAppControllerValue.getValue().decrementUsers();
        }
        if (endpointRegistryValue != null) {
            endpointRegistryValue.getValue().unregister(endpoint);
        }
        if (mBeanServerValue != null) {
            final List<RecordProcessor> processors = endpoint.getRecordProcessors();
            for (final RecordProcessor processor : processors) {
               unregisterRecordProcessor(processor, endpoint);
            }
        }
    }

    private void registerRecordProcessor(final RecordProcessor processor, final Endpoint ep) {
        MBeanServer mbeanServer = mBeanServerValue.getValue();
        if (mbeanServer != null) {
            try {
                mbeanServer.registerMBean(processor, ObjectNameFactory.create(ep.getName() + ",recordProcessor=" + processor.getName()));
            }
            catch (final JMException ex) {
                ROOT_LOGGER.trace("Cannot register endpoint with JMX server, trying with the default ManagedRecordProcessor: " + ex.getMessage());
                try {
                    mbeanServer.registerMBean(new ManagedRecordProcessor(processor), ObjectNameFactory.create(ep.getName() + ",recordProcessor=" + processor.getName()));
                }
                catch (final JMException e) {
                    ROOT_LOGGER.cannotRegisterRecordProcessor();
                }
            }
        } else {
            ROOT_LOGGER.mBeanServerNotAvailable(processor);
        }
    }

    private void unregisterRecordProcessor(final RecordProcessor processor, final Endpoint ep) {
        MBeanServer mbeanServer = mBeanServerValue.getValue();
        if (mbeanServer != null) {
            try {
                mbeanServer.unregisterMBean(ObjectNameFactory.create(ep.getName() + ",recordProcessor=" + processor.getName()));
            } catch (final JMException e) {
                ROOT_LOGGER.cannotUnregisterRecordProcessor();
            }
        } else {
            ROOT_LOGGER.mBeanServerNotAvailable(processor);
        }
    }

    private boolean hasWebservicesMD(final Endpoint endpoint) {
        final Deployment dep = endpoint.getService().getDeployment();
        return dep.getAttachment(WebservicesMetaData.class) != null;
    }
}

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
package org.jboss.as.webservices.service;

import java.security.AccessController;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.management.JMException;
import javax.management.MBeanServer;

import org.jboss.as.controller.ServiceNameFactory;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.ejb3.security.service.EJBViewMethodSecurityAttributesService;
import org.jboss.as.ejb3.subsystem.ApplicationSecurityDomainService;
import org.jboss.as.ejb3.subsystem.ApplicationSecurityDomainService.ApplicationSecurityDomain;
import org.jboss.as.security.plugins.SecurityDomainContext;
import org.jboss.as.security.service.SecurityDomainService;
import org.jboss.as.server.CurrentServiceContainer;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.webservices.logging.WSLogger;
import org.jboss.as.webservices.metadata.model.EJBEndpoint;
import org.jboss.as.webservices.security.EJBMethodSecurityAttributesAdaptor;
import org.jboss.as.webservices.security.ElytronSecurityDomainContextImpl;
import org.jboss.as.webservices.security.SecurityDomainContextImpl;
import org.jboss.as.webservices.util.ASHelper;
import org.jboss.as.webservices.util.ServiceContainerEndpointRegistry;
import org.jboss.as.webservices.util.WSAttachmentKeys;
import org.jboss.as.webservices.util.WSServices;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;
import org.jboss.security.SecurityConstants;
import org.jboss.security.SecurityUtil;
import org.jboss.ws.api.monitoring.RecordProcessor;
import org.jboss.ws.common.ObjectNameFactory;
import org.jboss.ws.common.management.AbstractServerConfig;
import org.jboss.ws.common.management.ManagedEndpoint;
import org.jboss.ws.common.monitoring.ManagedRecordProcessor;
import org.jboss.wsf.spi.SPIProvider;
import org.jboss.wsf.spi.deployment.Deployment;
import org.jboss.wsf.spi.deployment.Endpoint;
import org.jboss.wsf.spi.deployment.EndpointType;
import org.jboss.wsf.spi.management.EndpointMetricsFactory;
import org.jboss.wsf.spi.security.EJBMethodSecurityAttributeProvider;
import org.wildfly.extension.undertow.Capabilities;
import org.wildfly.extension.undertow.Constants;
import org.wildfly.extension.undertow.deployment.UndertowAttachments;
import org.wildfly.security.auth.server.SecurityDomain;

/**
 * WS endpoint service; this is meant for setting the lazy deployment time info into the Endpoint (stuff coming from
 * dependencies upon other AS services that are started during the deployment)
 *
 * @author alessio.soldano@jboss.com
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 * @author <a href="mailto:ema@redhat.com">Jim Ma</a>
 */
public final class EndpointService implements Service {

    private static final String WEB_APPLICATION_SECURITY_DOMAIN = "org.wildfly.undertow.application-security-domain";
    private static final String EJB_APPLICATION_SECURITY_DOMAIN = "org.wildfly.ejb3.application-security-domain";
    private static final RuntimeCapability<Void> EJB_APPLICATION_SECURITY_DOMAIN_RUNTIME_CAPABILITY = RuntimeCapability
            .Builder.of(EJB_APPLICATION_SECURITY_DOMAIN, true, ApplicationSecurityDomain.class)
            .build();
    private static final String SECURITY_DOMAIN_NAME = "securityDomainName";
    private static final String ELYTRON_SECURITY_DOMAIN = "elytronSecurityDomain";
    private final Endpoint endpoint;
    private final ServiceName name;
    private final ServiceName aliasName;
    private final Consumer<Endpoint> endpointConsumer;
    private final Supplier<SecurityDomainContext> securityDomainContext;
    private final Supplier<AbstractServerConfig> serverConfigService;
    private final Supplier<ApplicationSecurityDomainService.ApplicationSecurityDomain> ejbApplicationSecurityDomain;
    private final Supplier<EJBViewMethodSecurityAttributesService> ejbMethodSecurityAttributeService;
    private final Supplier<SecurityDomain> elytronSecurityDomain;

    private EndpointService(final Endpoint endpoint, final ServiceName name, final ServiceName aliasName, final Consumer<Endpoint> endpointConsumer,
                            final Supplier<SecurityDomainContext> securityDomainContext,
                            Supplier<AbstractServerConfig> serverConfigService,
                            Supplier<ApplicationSecurityDomainService.ApplicationSecurityDomain> ejbApplicationSecurityDomain,
                            Supplier<EJBViewMethodSecurityAttributesService> ejbMethodSecurityAttributeService,
                            final Supplier<SecurityDomain> elytronSecurityDomain
    ) {
        this.endpoint = endpoint;
        this.name = name;
        this.aliasName = aliasName;
        this.endpointConsumer = endpointConsumer;
        this.securityDomainContext = securityDomainContext;
        this.serverConfigService = serverConfigService;
        this.ejbApplicationSecurityDomain = ejbApplicationSecurityDomain;
        this.ejbMethodSecurityAttributeService = ejbMethodSecurityAttributeService;
        this.elytronSecurityDomain = elytronSecurityDomain;
    }

    public static ServiceName getServiceName(final DeploymentUnit unit, final String endpointName) {
        if (unit.getParent() != null) {
            return WSServices.ENDPOINT_SERVICE.append(unit.getParent().getName()).append(unit.getName()).append(endpointName);
        } else {
            return WSServices.ENDPOINT_SERVICE.append(unit.getName()).append(endpointName);
        }
    }

    @Override
    public void start(final StartContext context) {
        WSLogger.ROOT_LOGGER.starting(name);
        if (endpoint.getProperty(ELYTRON_SECURITY_DOMAIN) != null && Boolean.parseBoolean(endpoint.getProperty(ELYTRON_SECURITY_DOMAIN).toString())) {
            if (EndpointType.JAXWS_EJB3.equals(endpoint.getType())) {
                endpoint.setSecurityDomainContext(new ElytronSecurityDomainContextImpl(this.ejbApplicationSecurityDomain.get().getSecurityDomain()));
            } else {
                endpoint.setSecurityDomainContext(new ElytronSecurityDomainContextImpl(this.elytronSecurityDomain.get()));
            }
        } else {
            endpoint.setSecurityDomainContext(new SecurityDomainContextImpl(securityDomainContext.get()));
        }
        if (EndpointType.JAXWS_EJB3.equals(endpoint.getType())) {
            final EJBViewMethodSecurityAttributesService ejbMethodSecurityAttributeService = this.ejbMethodSecurityAttributeService.get();
            endpoint.addAttachment(EJBMethodSecurityAttributeProvider.class, new EJBMethodSecurityAttributesAdaptor(ejbMethodSecurityAttributeService));
        }
        final List<RecordProcessor> processors = endpoint.getRecordProcessors();
        for (final RecordProcessor processor : processors) {
            registerRecordProcessor(processor, endpoint);
        }
        final EndpointMetricsFactory endpointMetricsFactory = SPIProvider.getInstance().getSPI(EndpointMetricsFactory.class);
        endpoint.setEndpointMetrics(endpointMetricsFactory.newEndpointMetrics());
        registerEndpoint(endpoint);
        endpoint.getLifecycleHandler().start(endpoint);
        ServiceContainerEndpointRegistry.register(aliasName, endpoint);
        endpointConsumer.accept(endpoint);
    }

    @Override
    public void stop(final StopContext context) {
        WSLogger.ROOT_LOGGER.stopping(name);
        ServiceContainerEndpointRegistry.unregister(aliasName, endpoint);
        endpoint.getLifecycleHandler().stop(endpoint);
        endpoint.setSecurityDomainContext(null);
        unregisterEndpoint(endpoint);
        final List<RecordProcessor> processors = endpoint.getRecordProcessors();
        for (final RecordProcessor processor : processors) {
            unregisterRecordProcessor(processor, endpoint);
        }
    }

    private void registerEndpoint(final Endpoint endpoint) {
        MBeanServer mbeanServer = serverConfigService.get().getMbeanServer();
        if (mbeanServer != null) {
            try {
                ManagedEndpoint jmxEndpoint = new ManagedEndpoint(endpoint, mbeanServer);
                mbeanServer.registerMBean(jmxEndpoint, endpoint.getName());
            } catch (final JMException ex) {
                WSLogger.ROOT_LOGGER.trace("Cannot register endpoint in JMX server", ex);
                WSLogger.ROOT_LOGGER.cannotRegisterEndpoint(endpoint.getShortName());
            }
        } else {
            WSLogger.ROOT_LOGGER.mBeanServerNotAvailable(endpoint.getShortName());
        }
    }

    private void unregisterEndpoint(final Endpoint endpoint) {
        MBeanServer mbeanServer = serverConfigService.get().getMbeanServer();
        if (mbeanServer != null) {
            try {
                mbeanServer.unregisterMBean(endpoint.getName());
            } catch (final JMException ex) {
                WSLogger.ROOT_LOGGER.trace("Cannot unregister endpoint from JMX server", ex);
                WSLogger.ROOT_LOGGER.cannotUnregisterEndpoint(endpoint.getShortName());
            }
        } else {
            WSLogger.ROOT_LOGGER.mBeanServerNotAvailable(endpoint.getShortName());
        }
    }

    private void registerRecordProcessor(final RecordProcessor processor, final Endpoint ep) {
        MBeanServer mbeanServer = serverConfigService.get().getMbeanServer();
        if (mbeanServer != null) {
            try {
                mbeanServer.registerMBean(processor,
                        ObjectNameFactory.create(ep.getName() + ",recordProcessor=" + processor.getName()));
            } catch (final JMException ex) {
                WSLogger.ROOT_LOGGER.trace("Cannot register endpoint in JMX server, trying with the default ManagedRecordProcessor", ex);
                try {
                    mbeanServer.registerMBean(new ManagedRecordProcessor(processor),
                            ObjectNameFactory.create(ep.getName() + ",recordProcessor=" + processor.getName()));
                } catch (final JMException e) {
                    WSLogger.ROOT_LOGGER.cannotRegisterRecordProcessor();
                }
            }
        } else {
            WSLogger.ROOT_LOGGER.mBeanServerNotAvailable(processor);
        }
    }

    private void unregisterRecordProcessor(final RecordProcessor processor, final Endpoint ep) {
        MBeanServer mbeanServer = serverConfigService.get().getMbeanServer();
        if (mbeanServer != null) {
            try {
                mbeanServer.unregisterMBean(ObjectNameFactory.create(ep.getName() + ",recordProcessor=" + processor.getName()));
            } catch (final JMException e) {
                WSLogger.ROOT_LOGGER.cannotUnregisterRecordProcessor();
            }
        } else {
            WSLogger.ROOT_LOGGER.mBeanServerNotAvailable(processor);
        }
    }

    public static void install(final ServiceTarget serviceTarget, final Endpoint endpoint, final DeploymentUnit unit) {
        final ServiceName serviceName = getServiceName(unit, endpoint.getShortName());
        final String propContext = endpoint.getName().getKeyProperty(Endpoint.SEPID_PROPERTY_CONTEXT);
        final String propEndpoint = endpoint.getName().getKeyProperty(Endpoint.SEPID_PROPERTY_ENDPOINT);
        final StringBuilder context = new StringBuilder(Endpoint.SEPID_PROPERTY_CONTEXT).append("=").append(propContext);
        final ServiceBuilder<?> builder = serviceTarget.addService(serviceName);
        Supplier<SecurityDomainContext> securityDomainContext = null;
        Supplier<ApplicationSecurityDomainService.ApplicationSecurityDomain> ejbApplicationSecurityDomain = null;
        Supplier<EJBViewMethodSecurityAttributesService> ejbMethodSecurityAttributeService = null;
        Supplier<SecurityDomain> elytronSecurityDomain = null;
        final ServiceName alias = WSServices.ENDPOINT_SERVICE.append(context.toString()).append(propEndpoint);
        final Consumer<Endpoint> endpointConsumer = builder.provides(serviceName, alias);
        //builder.addAliases(alias);
        final String domainName = getDeploymentSecurityDomainName(endpoint, unit);
        endpoint.setProperty(SECURITY_DOMAIN_NAME, domainName);
        if (isElytronSecurityDomain(unit, endpoint, domainName)) {
            if (EndpointType.JAXWS_EJB3.equals(endpoint.getType())) {
                ServiceName ejbSecurityDomainServiceName = EJB_APPLICATION_SECURITY_DOMAIN_RUNTIME_CAPABILITY
                        .getCapabilityServiceName(domainName, ApplicationSecurityDomainService.ApplicationSecurityDomain.class);
                ejbApplicationSecurityDomain = builder.requires(ejbSecurityDomainServiceName);
            } else {
                ServiceName securityDomainName = unit.getAttachment(Attachments.CAPABILITY_SERVICE_SUPPORT)
                        .getCapabilityServiceName(
                                Capabilities.CAPABILITY_APPLICATION_SECURITY_DOMAIN,
                                domainName).append(Constants.SECURITY_DOMAIN);
                elytronSecurityDomain = builder.requires(securityDomainName);
            }
            endpoint.setProperty(ELYTRON_SECURITY_DOMAIN, true);
        } else {
            // This is still picketbox jaas securityDomainContext
            securityDomainContext = builder.requires(SecurityDomainService.SERVICE_NAME.append(domainName));
        }
        final Supplier<AbstractServerConfig> serverConfigService = builder.requires(WSServices.CONFIG_SERVICE);
        if (EndpointType.JAXWS_EJB3.equals(endpoint.getType())) {
            ejbMethodSecurityAttributeService = builder.requires(getEJBViewMethodSecurityAttributesServiceName(unit, endpoint));
        }
        builder.setInstance(new EndpointService(endpoint, serviceName, alias, endpointConsumer, securityDomainContext,
                serverConfigService, ejbApplicationSecurityDomain, ejbMethodSecurityAttributeService, elytronSecurityDomain));
        builder.install();
        //add a dependency on the endpoint service to web deployments, so that the
        //endpoint servlet is not started before the endpoint is actually available
        unit.addToAttachmentList(Attachments.WEB_DEPENDENCIES, serviceName);
    }

    public static void uninstall(final Endpoint endpoint, final DeploymentUnit unit) {
        final ServiceName serviceName = getServiceName(unit, endpoint.getShortName());
        final ServiceController<?> endpointService = currentServiceContainer().getService(serviceName);
        if (endpointService != null) {
            endpointService.setMode(Mode.REMOVE);
        }
    }

    private static String getDeploymentSecurityDomainName(final Endpoint ep, final DeploymentUnit unit) {
        JBossWebMetaData metadata = ep.getService().getDeployment().getAttachment(JBossWebMetaData.class);
        String metaDataSecurityDomain = metadata != null ? metadata.getSecurityDomain() : null;
        if (metaDataSecurityDomain == null) {
            if (unit.hasAttachment(UndertowAttachments.DEFAULT_SECURITY_DOMAIN)) {
                metaDataSecurityDomain = unit.getAttachment(UndertowAttachments.DEFAULT_SECURITY_DOMAIN);
            } else {
                metaDataSecurityDomain = SecurityConstants.DEFAULT_APPLICATION_POLICY;
            }
        } else {
            metaDataSecurityDomain = SecurityUtil.unprefixSecurityDomain(metaDataSecurityDomain.trim());
        }
        return metaDataSecurityDomain;
    }

    private static ServiceName getEJBViewMethodSecurityAttributesServiceName(final DeploymentUnit unit, final Endpoint endpoint) {
        for (EJBEndpoint ep : ASHelper.getJaxwsEjbs(unit)) {
            if (ep.getClassName().equals(endpoint.getTargetBeanName())) {
                return ep.getEJBViewMethodSecurityAttributesService();
            }
        }
        return null;
    }

    static List<ServiceName> getServiceNamesFromDeploymentUnit(final DeploymentUnit unit) {
        final List<ServiceName> endpointServiceNames = new ArrayList<>();
        Deployment deployment = unit.getAttachment(WSAttachmentKeys.DEPLOYMENT_KEY);
        for (Endpoint ep : deployment.getService().getEndpoints()) {
            endpointServiceNames.add(EndpointService.getServiceName(unit, ep.getShortName()));
        }
        return endpointServiceNames;
    }

    private static ServiceContainer currentServiceContainer() {
        if(System.getSecurityManager() == null) {
            return CurrentServiceContainer.getServiceContainer();
        }
        return AccessController.doPrivileged(CurrentServiceContainer.GET_ACTION);
    }

    private static boolean isElytronSecurityDomain(DeploymentUnit unit, Endpoint endpoint, String domainName) {
        final ServiceName serviceName;
        if (EndpointType.JAXWS_EJB3.equals(endpoint.getType())) {
            serviceName = EJB_APPLICATION_SECURITY_DOMAIN_RUNTIME_CAPABILITY.getCapabilityServiceName(domainName,
                    ApplicationSecurityDomainService.ApplicationSecurityDomain.class);
        } else {
            serviceName = ServiceNameFactory.parseServiceName(WEB_APPLICATION_SECURITY_DOMAIN).append(domainName)
                    .append(Constants.SECURITY_DOMAIN);
        }
        return currentServiceContainer().getService(serviceName) != null;
    }

}

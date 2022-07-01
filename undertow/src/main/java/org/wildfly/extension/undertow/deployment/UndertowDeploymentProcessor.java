/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.undertow.deployment;

import static org.jboss.as.ee.component.Attachments.STARTUP_COUNTDOWN;
import static org.jboss.as.server.security.SecurityMetaData.ATTACHMENT_KEY;
import static org.jboss.as.server.security.VirtualDomainMarkerUtility.isVirtualDomainRequired;
import static org.jboss.as.web.common.VirtualHttpServerMechanismFactoryMarkerUtility.isVirtualMechanismFactoryRequired;
import static org.wildfly.extension.undertow.Capabilities.REF_LEGACY_SECURITY;
import static org.wildfly.extension.undertow.logging.UndertowLogger.ROOT_LOGGER;

import java.net.MalformedURLException;
import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import javax.security.jacc.PolicyConfiguration;

import org.apache.jasper.Constants;
import org.apache.jasper.deploy.FunctionInfo;
import org.apache.jasper.deploy.TagAttributeInfo;
import org.apache.jasper.deploy.TagFileInfo;
import org.apache.jasper.deploy.TagInfo;
import org.apache.jasper.deploy.TagLibraryInfo;
import org.apache.jasper.deploy.TagLibraryValidatorInfo;
import org.apache.jasper.deploy.TagVariableInfo;
import org.jboss.annotation.javaee.Icon;
import org.jboss.as.clustering.controller.CapabilityServiceConfigurator;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.as.ee.component.ComponentRegistry;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ee.component.deployers.StartupCountdown;
import org.jboss.as.ee.security.JaccService;
import org.jboss.as.server.ServerEnvironment;
import org.jboss.as.server.ServerEnvironmentService;
import org.jboss.as.server.Services;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentResourceSupport;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.ExplodedDeploymentMarker;
import org.jboss.as.server.deployment.SetupAction;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.as.server.security.SecurityMetaData;
import org.jboss.as.server.suspend.SuspendController;
import org.jboss.as.web.common.AdvancedSecurityMetaData;
import org.jboss.as.web.common.CachingWebInjectionContainer;
import org.jboss.as.web.common.ExpressionFactoryWrapper;
import org.jboss.as.web.common.ServletContextAttribute;
import org.jboss.as.web.common.SimpleWebInjectionContainer;
import org.jboss.as.web.common.WarMetaData;
import org.jboss.as.web.common.WebComponentDescription;
import org.jboss.as.web.common.WebInjectionContainer;
import org.jboss.as.web.session.SessionIdentifierCodec;
import org.jboss.as.web.session.SharedSessionManagerConfig;
import org.jboss.dmr.ModelNode;
import org.jboss.metadata.javaee.spec.DescriptionGroupMetaData;
import org.jboss.metadata.javaee.spec.ParamValueMetaData;
import org.jboss.metadata.web.jboss.JBossServletMetaData;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.metadata.web.spec.AttributeMetaData;
import org.jboss.metadata.web.spec.FunctionMetaData;
import org.jboss.metadata.web.spec.ListenerMetaData;
import org.jboss.metadata.web.spec.SessionConfigMetaData;
import org.jboss.metadata.web.spec.TagFileMetaData;
import org.jboss.metadata.web.spec.TagMetaData;
import org.jboss.metadata.web.spec.TldMetaData;
import org.jboss.metadata.web.spec.VariableMetaData;
import org.jboss.modules.Module;
import org.jboss.msc.service.DuplicateServiceException;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.vfs.VirtualFile;
import org.wildfly.clustering.web.container.SessionManagementProvider;
import org.wildfly.clustering.web.container.SessionManagerFactoryConfiguration;
import org.wildfly.extension.io.IOServices;
import org.wildfly.extension.requestcontroller.ControlPoint;
import org.wildfly.extension.requestcontroller.ControlPointService;
import org.wildfly.extension.requestcontroller.RequestControllerActivationMarker;
import org.wildfly.extension.undertow.Capabilities;
import org.wildfly.extension.undertow.DeploymentDefinition;
import org.wildfly.extension.undertow.Host;
import org.wildfly.extension.undertow.HostSingleSignOnDefinition;
import org.wildfly.extension.undertow.ServletContainerService;
import org.wildfly.extension.undertow.UndertowExtension;
import org.wildfly.extension.undertow.UndertowService;
import org.wildfly.extension.undertow.logging.UndertowLogger;
import org.wildfly.extension.undertow.security.jacc.WarJACCDeployer;
import org.wildfly.extension.undertow.session.NonDistributableSessionManagementProvider;
import org.wildfly.extension.undertow.session.SessionManagementProviderFactory;
import org.wildfly.security.auth.server.SecurityDomain;
import org.wildfly.security.http.HttpServerAuthenticationMechanismFactory;

import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.SessionManagerFactory;
import io.undertow.servlet.core.InMemorySessionManagerFactory;

/**
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public class UndertowDeploymentProcessor implements DeploymentUnitProcessor, Function<SessionManagerFactoryConfiguration, SessionManagerFactory> {

    public static final String OLD_URI_PREFIX = "http://java.sun.com";
    public static final String NEW_URI_PREFIX = "http://xmlns.jcp.org";

    private static final String LEGACY_JACC_CAPABILITY_NAME = "org.wildfly.legacy-security.jacc";
    private static final String ELYTRON_JACC_CAPABILITY_NAME = "org.wildfly.security.jacc-policy";

    private final String defaultServer;
    private final String defaultHost;
    private final String defaultContainer;
    private final Predicate<String> mappedSecurityDomain;
    /**
        default module mappings, where we have key as name of default deployment,
        for value we have Map.Entry which has key as server-name where deployment is bound to,
        value is host name where deployment is bound to.
     */
    private final DefaultDeploymentMappingProvider defaultModuleMappingProvider;
    private final SessionManagementProviderFactory sessionManagementProviderFactory;
    private final SessionManagementProvider nonDistributableSessionManagementProvider;

    public UndertowDeploymentProcessor(String defaultHost, final String defaultContainer, String defaultServer, Predicate<String> mappedSecurityDomain) {
        this.defaultHost = defaultHost;
        this.defaultModuleMappingProvider = DefaultDeploymentMappingProvider.instance();
        if (defaultHost == null) {
            throw UndertowLogger.ROOT_LOGGER.nullDefaultHost();
        }
        this.defaultContainer = defaultContainer;
        this.defaultServer = defaultServer;
        this.mappedSecurityDomain = mappedSecurityDomain;
        Iterator<SessionManagementProviderFactory> factories = ServiceLoader.load(SessionManagementProviderFactory.class, SessionManagementProviderFactory.class.getClassLoader()).iterator();
        this.sessionManagementProviderFactory = factories.hasNext() ? factories.next() : null;
        this.nonDistributableSessionManagementProvider = new NonDistributableSessionManagementProvider(this);
    }

    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();

        //install the control point for the top level deployment no matter what
        if (RequestControllerActivationMarker.isRequestControllerEnabled(deploymentUnit) && deploymentUnit.getParent() == null) {
            ControlPointService.install(phaseContext.getServiceTarget(), deploymentUnit.getName(),
                    UndertowExtension.SUBSYSTEM_NAME);
        }
        final WarMetaData warMetaData = deploymentUnit.getAttachment(WarMetaData.ATTACHMENT_KEY);
        if (warMetaData == null) {
            return;
        }
        String deploymentName;
        if (deploymentUnit.getParent() == null) {
            deploymentName = deploymentUnit.getName();
        } else {
            deploymentName = deploymentUnit.getParent().getName() + "." + deploymentUnit.getName();
        }

        final Map.Entry<String,String> serverHost = defaultModuleMappingProvider.getMapping(deploymentName);
        String defaultHostForDeployment;
        String defaultServerForDeployment;
        if (serverHost != null) {
            defaultServerForDeployment = serverHost.getKey();
            defaultHostForDeployment = serverHost.getValue();
        } else {
            defaultServerForDeployment = this.defaultServer;
            defaultHostForDeployment = this.defaultHost;
        }

        String serverInstanceName = warMetaData.getMergedJBossWebMetaData().getServerInstanceName() == null ? defaultServerForDeployment : warMetaData.getMergedJBossWebMetaData().getServerInstanceName();
        String hostName = hostNameOfDeployment(warMetaData, defaultHostForDeployment);
        // flag if the app is the default web module for the server/host
        boolean isDefaultWebModule = serverHost != null && serverInstanceName.equals(defaultServerForDeployment) && hostName.equals(defaultHostForDeployment);

        processDeployment(warMetaData, deploymentUnit, phaseContext.getServiceTarget(), deploymentName, hostName, serverInstanceName, isDefaultWebModule);
    }


    private String hostNameOfDeployment(final WarMetaData metaData, String defaultHost) {
        Collection<String> hostNames = null;
        if (metaData.getMergedJBossWebMetaData() != null) {
            hostNames = metaData.getMergedJBossWebMetaData().getVirtualHosts();
        }
        if (hostNames == null || hostNames.isEmpty()) {
            hostNames = Collections.singleton(defaultHost);
        }
        String hostName = hostNames.iterator().next();
        if (hostName == null) {
            throw UndertowLogger.ROOT_LOGGER.nullHostName();
        }
        return hostName;
    }

    private void processDeployment(final WarMetaData warMetaData, final DeploymentUnit deploymentUnit, final ServiceTarget serviceTarget,
                                   final String deploymentName, final String hostName, final String serverInstanceName, final boolean isDefaultWebModule)
            throws DeploymentUnitProcessingException {
        ResourceRoot deploymentResourceRoot = deploymentUnit.getAttachment(Attachments.DEPLOYMENT_ROOT);
        final VirtualFile deploymentRoot = deploymentResourceRoot.getRoot();
        final Module module = deploymentUnit.getAttachment(Attachments.MODULE);
        if (module == null) {
            throw new DeploymentUnitProcessingException(UndertowLogger.ROOT_LOGGER.failedToResolveModule(deploymentUnit));
        }
        final JBossWebMetaData metaData = warMetaData.getMergedJBossWebMetaData();
        final List<SetupAction> setupActions = deploymentUnit.getAttachmentList(org.jboss.as.ee.component.Attachments.WEB_SETUP_ACTIONS);
        CapabilityServiceSupport capabilitySupport = deploymentUnit.getAttachment(Attachments.CAPABILITY_SERVICE_SUPPORT);

        ScisMetaData scisMetaData = deploymentUnit.getAttachment(ScisMetaData.ATTACHMENT_KEY);

        final Set<ServiceName> dependentComponents = new HashSet<>();
        // see AS7-2077
        // basically we want to ignore components that have failed for whatever reason
        // if they are important they will be picked up when the web deployment actually starts
        final List<ServiceName> components = deploymentUnit.getAttachmentList(WebComponentDescription.WEB_COMPONENTS);
        final Set<ServiceName> failed = deploymentUnit.getAttachment(org.jboss.as.ee.component.Attachments.FAILED_COMPONENTS);
        for (final ServiceName component : components) {
            if (!failed.contains(component)) {
                dependentComponents.add(component);
            }
        }
        String servletContainerName = metaData.getServletContainerName();
        if(servletContainerName == null) {
            servletContainerName = defaultContainer;
        }

        final boolean componentRegistryExists = deploymentUnit.getAttachment(org.jboss.as.ee.component.Attachments.COMPONENT_REGISTRY) != null;
        final ComponentRegistry componentRegistry = componentRegistryExists ? deploymentUnit.getAttachment(org.jboss.as.ee.component.Attachments.COMPONENT_REGISTRY) : new ComponentRegistry(null);
        final ClassLoader loader = module.getClassLoader();
        final WebInjectionContainer injectionContainer = (metaData.getDistributable() == null) ? new CachingWebInjectionContainer(loader, componentRegistry) : new SimpleWebInjectionContainer(loader, componentRegistry);

        String jaccContextId = metaData.getJaccContextID();

        if (jaccContextId == null) {
            jaccContextId = deploymentUnit.getName();
        }
        if (deploymentUnit.getParent() != null) {
            jaccContextId = deploymentUnit.getParent().getName() + "!" + jaccContextId;
        }

        String pathName = pathNameOfDeployment(deploymentUnit, metaData, isDefaultWebModule);

        final Set<ServiceName> additionalDependencies = new HashSet<>();
        for (final SetupAction setupAction : setupActions) {
            Set<ServiceName> dependencies = setupAction.dependencies();
            if (dependencies != null) {
                additionalDependencies.addAll(dependencies);
            }
        }
        SharedSessionManagerConfig sharedSessionManagerConfig = deploymentUnit.getParent() != null ? deploymentUnit.getParent().getAttachment(SharedSessionManagerConfig.ATTACHMENT_KEY) : null;

        if(!deploymentResourceRoot.isUsePhysicalCodeSource()) {
            try {
                deploymentUnit.addToAttachmentList(ServletContextAttribute.ATTACHMENT_KEY, new ServletContextAttribute(Constants.CODE_SOURCE_ATTRIBUTE_NAME, deploymentRoot.toURL()));
            } catch (MalformedURLException e) {
                throw new DeploymentUnitProcessingException(e);
            }
        }

        deploymentUnit.addToAttachmentList(ServletContextAttribute.ATTACHMENT_KEY, new ServletContextAttribute(Constants.PERMISSION_COLLECTION_ATTRIBUTE_NAME, deploymentUnit.getAttachment(Attachments.MODULE_PERMISSIONS)));

        additionalDependencies.addAll(warMetaData.getAdditionalDependencies());
        try {
            String capability = HostSingleSignOnDefinition.HOST_SSO_CAPABILITY.fromBaseCapability(serverInstanceName, hostName).getName();
            capabilitySupport.getCapabilityRuntimeAPI(capability, Object.class);
            additionalDependencies.add(capabilitySupport.getCapabilityServiceName(capability));
        } catch (CapabilityServiceSupport.NoSuchCapabilityException e) {
            //ignore
        }

        final ServiceName hostServiceName = UndertowService.virtualHostName(serverInstanceName, hostName);
        final ServiceName legacyDeploymentServiceName = UndertowService.deploymentServiceName(serverInstanceName, hostName, pathName);
        final ServiceName deploymentServiceName = UndertowService.deploymentServiceName(deploymentUnit.getServiceName());

        StartupCountdown countDown = deploymentUnit.getAttachment(STARTUP_COUNTDOWN);
        if (countDown != null) {
            deploymentUnit.addToAttachmentList(UndertowAttachments.UNDERTOW_INITIAL_HANDLER_CHAIN_WRAPPERS,
                    handler->new ComponentStartupCountdownHandler(handler, countDown));
        }

        String securityDomain = deploymentUnit.getAttachment(UndertowAttachments.RESOLVED_SECURITY_DOMAIN);
        TldsMetaData tldsMetaData = deploymentUnit.getAttachment(TldsMetaData.ATTACHMENT_KEY);
        final ServiceName deploymentInfoServiceName = deploymentServiceName.append(UndertowDeploymentInfoService.SERVICE_NAME);
        final ServiceName legacyDeploymentInfoServiceName = legacyDeploymentServiceName.append(UndertowDeploymentInfoService.SERVICE_NAME);
        final ServiceBuilder<?> udisBuilder = serviceTarget.addService(deploymentInfoServiceName);
        final Consumer<DeploymentInfo> diConsumer = udisBuilder.provides(deploymentInfoServiceName, legacyDeploymentInfoServiceName);
        final Supplier<UndertowService> usSupplier = udisBuilder.requires(UndertowService.UNDERTOW);
        final Supplier<SessionManagerFactory> smfSupplier;
        final Supplier<SessionIdentifierCodec> sicSupplier;
        final Supplier<ServletContainerService> scsSupplier = udisBuilder.requires(UndertowService.SERVLET_CONTAINER.append(servletContainerName));
        final Supplier<ComponentRegistry> crSupplier = componentRegistryExists ? udisBuilder.requires(ComponentRegistry.serviceName(deploymentUnit)) : new Supplier<ComponentRegistry>() {
            @Override
            public ComponentRegistry get() {
                return componentRegistry;
            }
        };
        final Supplier<Host> hostSupplier = udisBuilder.requires(hostServiceName);
        Supplier<ControlPoint> cpSupplier = null;
        final Supplier<SuspendController> scSupplier = udisBuilder.requires(capabilitySupport.getCapabilityServiceName(Capabilities.REF_SUSPEND_CONTROLLER));
        final Supplier<ServerEnvironment> serverEnvSupplier = udisBuilder.requires(ServerEnvironmentService.SERVICE_NAME);
        Supplier<SecurityDomain> sdSupplier = null;
        Supplier<HttpServerAuthenticationMechanismFactory> mechanismFactorySupplier = null;
        Supplier<BiFunction> bfSupplier = null;

        for (final ServiceName additionalDependency : additionalDependencies) {
            udisBuilder.requires(additionalDependency);
        }

        final SecurityMetaData securityMetaData = deploymentUnit.getAttachment(ATTACHMENT_KEY);
        if (isVirtualDomainRequired(deploymentUnit) || isVirtualMechanismFactoryRequired(deploymentUnit)) {
            sdSupplier = udisBuilder.requires(securityMetaData.getSecurityDomain());
        } else if(securityDomain != null) {
            if (mappedSecurityDomain.test(securityDomain)) {
                bfSupplier = udisBuilder.requires(
                        deploymentUnit.getAttachment(Attachments.CAPABILITY_SERVICE_SUPPORT)
                                .getCapabilityServiceName(Capabilities.CAPABILITY_APPLICATION_SECURITY_DOMAIN, securityDomain));
            } else {
                throw ROOT_LOGGER.deploymentConfiguredForLegacySecurity();
            }
        }
        if (isVirtualMechanismFactoryRequired(deploymentUnit)) {
            if (securityMetaData instanceof AdvancedSecurityMetaData) {
                mechanismFactorySupplier = udisBuilder.requires(((AdvancedSecurityMetaData) securityMetaData).getHttpServerAuthenticationMechanismFactory());
            }
        }

        if (RequestControllerActivationMarker.isRequestControllerEnabled(deploymentUnit)){
            String topLevelName;
            if(deploymentUnit.getParent() == null) {
                topLevelName = deploymentUnit.getName();
            } else {
                topLevelName = deploymentUnit.getParent().getName();
            }
            cpSupplier = udisBuilder.requires(ControlPointService.serviceName(topLevelName, UndertowExtension.SUBSYSTEM_NAME));
        }
        if (sharedSessionManagerConfig != null) {
            final ServiceName parentSN = deploymentUnit.getParent().getServiceName();
            smfSupplier = udisBuilder.requires(parentSN.append(SharedSessionManagerConfig.SHARED_SESSION_MANAGER_SERVICE_NAME));
            sicSupplier = udisBuilder.requires(parentSN.append(SharedSessionManagerConfig.SHARED_SESSION_IDENTIFIER_CODEC_SERVICE_NAME));
        } else {
            ServletContainerService servletContainer = deploymentUnit.getAttachment(UndertowAttachments.SERVLET_CONTAINER_SERVICE);
            Integer maxActiveSessions = (metaData.getMaxActiveSessions() != null) ? metaData.getMaxActiveSessions() : (servletContainer != null) ? servletContainer.getMaxSessions() : null;
            SessionConfigMetaData sessionConfig = metaData.getSessionConfig();
            int defaultSessionTimeout = ((sessionConfig != null) && sessionConfig.getSessionTimeoutSet()) ? sessionConfig.getSessionTimeout() : (servletContainer != null) ? servletContainer.getDefaultSessionTimeout() : Integer.valueOf(30);
            ServiceName factoryServiceName = deploymentServiceName.append("session");
            ServiceName codecServiceName = deploymentServiceName.append("codec");

            SessionManagementProvider provider = this.getDistributableWebDeploymentProvider(deploymentUnit, metaData);
            SessionManagerFactoryConfiguration configuration = new SessionManagerFactoryConfiguration() {
                @Override
                public String getServerName() {
                    return serverInstanceName;
                }

                @Override
                public String getDeploymentName() {
                    return deploymentName;
                }

                @Override
                public DeploymentUnit getDeploymentUnit() {
                    return deploymentUnit;
                }

                @Override
                public Integer getMaxActiveSessions() {
                    return maxActiveSessions;
                }

                @Override
                public Duration getDefaultSessionTimeout() {
                    return Duration.ofMinutes(defaultSessionTimeout);
                }
            };
            CapabilityServiceConfigurator factoryConfigurator = provider.getSessionManagerFactoryServiceConfigurator(factoryServiceName, configuration);
            CapabilityServiceConfigurator codecConfigurator = provider.getSessionIdentifierCodecServiceConfigurator(codecServiceName, configuration);

            smfSupplier = udisBuilder.requires(factoryConfigurator.getServiceName());
            sicSupplier = udisBuilder.requires(codecConfigurator.getServiceName());

            CapabilityServiceSupport support = deploymentUnit.getAttachment(Attachments.CAPABILITY_SERVICE_SUPPORT);
            factoryConfigurator.configure(support).build(serviceTarget).install();
            codecConfigurator.configure(support).build(serviceTarget).install();
        }
        UndertowDeploymentInfoService undertowDeploymentInfoService = UndertowDeploymentInfoService.builder()
                .setAttributes(deploymentUnit.getAttachmentList(ServletContextAttribute.ATTACHMENT_KEY))
                .setContextPath(pathName)
                .setDeploymentName(deploymentName) //todo: is this deployment name concept really applicable?
                .setDeploymentRoot(deploymentRoot)
                .setMergedMetaData(warMetaData.getMergedJBossWebMetaData())
                .setModule(module)
                .setScisMetaData(scisMetaData)
                .setJaccContextId(jaccContextId)
                .setSecurityDomain(securityDomain)
                .setTldInfo(createTldsInfo(tldsMetaData, tldsMetaData == null ? null : tldsMetaData.getSharedTlds(deploymentUnit)))
                .setSetupActions(setupActions)
                .setSharedSessionManagerConfig(sharedSessionManagerConfig)
                .setOverlays(warMetaData.getOverlays())
                .setExpressionFactoryWrappers(deploymentUnit.getAttachmentList(ExpressionFactoryWrapper.ATTACHMENT_KEY))
                .setPredicatedHandlers(deploymentUnit.getAttachment(UndertowHandlersDeploymentProcessor.PREDICATED_HANDLERS))
                .setInitialHandlerChainWrappers(deploymentUnit.getAttachmentList(UndertowAttachments.UNDERTOW_INITIAL_HANDLER_CHAIN_WRAPPERS))
                .setInnerHandlerChainWrappers(deploymentUnit.getAttachmentList(UndertowAttachments.UNDERTOW_INNER_HANDLER_CHAIN_WRAPPERS))
                .setOuterHandlerChainWrappers(deploymentUnit.getAttachmentList(UndertowAttachments.UNDERTOW_OUTER_HANDLER_CHAIN_WRAPPERS))
                .setThreadSetupActions(deploymentUnit.getAttachmentList(UndertowAttachments.UNDERTOW_THREAD_SETUP_ACTIONS))
                .setServletExtensions(deploymentUnit.getAttachmentList(UndertowAttachments.UNDERTOW_SERVLET_EXTENSIONS))
                .setExplodedDeployment(ExplodedDeploymentMarker.isExplodedDeployment(deploymentUnit))
                .setWebSocketDeploymentInfo(deploymentUnit.getAttachment(UndertowAttachments.WEB_SOCKET_DEPLOYMENT_INFO))
                .setTempDir(warMetaData.getTempDir())
                .setExternalResources(deploymentUnit.getAttachmentList(UndertowAttachments.EXTERNAL_RESOURCES))
                .setAllowSuspendedRequests(deploymentUnit.getAttachmentList(UndertowAttachments.ALLOW_REQUEST_WHEN_SUSPENDED))
                .createUndertowDeploymentInfoService(diConsumer, usSupplier, smfSupplier, sicSupplier,
                        scsSupplier, crSupplier, hostSupplier, cpSupplier, scSupplier, serverEnvSupplier, sdSupplier, mechanismFactorySupplier, bfSupplier);
        udisBuilder.setInstance(undertowDeploymentInfoService);

        final Set<String> seenExecutors = new HashSet<String>();
        if (metaData.getExecutorName() != null) {
            final Supplier<Executor> executor = udisBuilder.requires(IOServices.WORKER.append(metaData.getExecutorName()));
            undertowDeploymentInfoService.addInjectedExecutor(metaData.getExecutorName(), executor);
            seenExecutors.add(metaData.getExecutorName());
        }
        if (metaData.getServlets() != null) {
            for (JBossServletMetaData servlet : metaData.getServlets()) {
                if (servlet.getExecutorName() != null && !seenExecutors.contains(servlet.getExecutorName())) {
                    final Supplier<Executor> executor = udisBuilder.requires(IOServices.WORKER.append(servlet.getExecutorName()));
                    undertowDeploymentInfoService.addInjectedExecutor(servlet.getExecutorName(), executor);
                    seenExecutors.add(servlet.getExecutorName());
                }
            }
        }

        try {
            udisBuilder.install();
        } catch (DuplicateServiceException e) {
            throw UndertowLogger.ROOT_LOGGER.duplicateHostContextDeployments(deploymentInfoServiceName, e.getMessage());
        }

        final ServiceBuilder<?> udsBuilder = serviceTarget.addService(deploymentServiceName);
        final Consumer<UndertowDeploymentService> sConsumer = udsBuilder.provides(deploymentServiceName, legacyDeploymentServiceName);
        final Supplier<ServletContainerService> cSupplier = udsBuilder.requires(UndertowService.SERVLET_CONTAINER.append(defaultContainer));
        final Supplier<ExecutorService> seSupplier = Services.requireServerExecutor(udsBuilder);
        final Supplier<Host> hSupplier = udsBuilder.requires(hostServiceName);
        final Supplier<DeploymentInfo> diSupplier = udsBuilder.requires(deploymentInfoServiceName);
        for (final ServiceName webDependency : deploymentUnit.getAttachmentList(Attachments.WEB_DEPENDENCIES)) {
            udsBuilder.requires(webDependency);
        }
        for (final ServiceName dependentComponent : dependentComponents) {
            udsBuilder.requires(dependentComponent);
        }
        udsBuilder.setInstance(new UndertowDeploymentService(sConsumer, cSupplier, seSupplier, hSupplier, diSupplier, injectionContainer, true));
        udsBuilder.install();

        deploymentUnit.addToAttachmentList(Attachments.DEPLOYMENT_COMPLETE_SERVICES, deploymentServiceName);

        // adding Jakarta Authorization service
        final boolean elytronJacc = capabilitySupport.hasCapability(ELYTRON_JACC_CAPABILITY_NAME);
        final boolean legacyJacc = !elytronJacc && legacySecurityInstalled(deploymentUnit);
        if(legacyJacc || elytronJacc) {
            WarJACCDeployer deployer = new WarJACCDeployer();
            JaccService<WarMetaData> jaccService = deployer.deploy(deploymentUnit, jaccContextId);
            if (jaccService != null) {
                final ServiceName jaccServiceName = deploymentUnit.getServiceName().append(JaccService.SERVICE_NAME);
                ServiceBuilder<?> jaccBuilder = serviceTarget.addService(jaccServiceName, jaccService);
                if (deploymentUnit.getParent() != null) {
                    // add dependency to parent policy
                    final DeploymentUnit parentDU = deploymentUnit.getParent();
                    jaccBuilder.addDependency(parentDU.getServiceName().append(JaccService.SERVICE_NAME), PolicyConfiguration.class,
                            jaccService.getParentPolicyInjector());
                }
                jaccBuilder.requires(capabilitySupport.getCapabilityServiceName(elytronJacc ? ELYTRON_JACC_CAPABILITY_NAME : LEGACY_JACC_CAPABILITY_NAME));
                // add dependency to web deployment service
                jaccBuilder.requires(deploymentServiceName);
                jaccBuilder.setInitialMode(Mode.PASSIVE).install();
            }
        }

        // Process the web related mgmt information
        final DeploymentResourceSupport deploymentResourceSupport = deploymentUnit.getAttachment(Attachments.DEPLOYMENT_RESOURCE_SUPPORT);
        final ModelNode node = deploymentResourceSupport.getDeploymentSubsystemModel(UndertowExtension.SUBSYSTEM_NAME);
        node.get(DeploymentDefinition.CONTEXT_ROOT.getName()).set("".equals(pathName) ? "/" : pathName);
        node.get(DeploymentDefinition.VIRTUAL_HOST.getName()).set(hostName);
        node.get(DeploymentDefinition.SERVER.getName()).set(serverInstanceName);
        processManagement(deploymentUnit, metaData);
    }

    private static boolean legacySecurityInstalled(final DeploymentUnit deploymentUnit) {
        final CapabilityServiceSupport capabilities = deploymentUnit.getAttachment(Attachments.CAPABILITY_SERVICE_SUPPORT);

        return capabilities.hasCapability(REF_LEGACY_SECURITY);
    }

    @Override
    public SessionManagerFactory apply(SessionManagerFactoryConfiguration configuration) {
        Integer maxActiveSessions = configuration.getMaxActiveSessions();
        return (maxActiveSessions != null) ? new InMemorySessionManagerFactory(maxActiveSessions.intValue()) : new InMemorySessionManagerFactory();
    }

    private SessionManagementProvider getDistributableWebDeploymentProvider(DeploymentUnit unit, JBossWebMetaData metaData) {
        if (metaData.getDistributable() != null) {
            if (this.sessionManagementProviderFactory != null) {
                return this.sessionManagementProviderFactory.createSessionManagementProvider(unit, metaData.getReplicationConfig());
            }
            // Fallback to local session manager if server does not support clustering
            UndertowLogger.ROOT_LOGGER.clusteringNotSupported();
        }
        return this.nonDistributableSessionManagementProvider;
    }

    static String pathNameOfDeployment(final DeploymentUnit deploymentUnit, final JBossWebMetaData metaData, final boolean isDefaultWebModule) {
        String pathName;
        if (isDefaultWebModule) {
            pathName = "/";
        } else if (metaData.getContextRoot() == null) {
            final EEModuleDescription description = deploymentUnit.getAttachment(org.jboss.as.ee.component.Attachments.EE_MODULE_DESCRIPTION);
            if (description != null) {
                // if there is an EEModuleDescription we need to take into account that the module name may have been overridden
                pathName = "/" + description.getModuleName();
            } else {
                pathName = "/" + deploymentUnit.getName().substring(0, deploymentUnit.getName().length() - 4);
            }
        } else {
            pathName = metaData.getContextRoot();
            if (pathName.length() > 0 && pathName.charAt(0) != '/') {
                pathName = "/" + pathName;
            }
        }
        return pathName;
    }

    //todo move to UndertowDeploymentService and use all registered servlets from Deployment instead of just one found by metadata
    void processManagement(final DeploymentUnit unit, JBossWebMetaData metaData) {
        final DeploymentResourceSupport deploymentResourceSupport = unit.getAttachment(Attachments.DEPLOYMENT_RESOURCE_SUPPORT);
        for (final JBossServletMetaData servlet : metaData.getServlets()) {
            try {
                final String name = servlet.getName();
                final ModelNode node = deploymentResourceSupport.getDeploymentSubModel(UndertowExtension.SUBSYSTEM_NAME, PathElement.pathElement("servlet", name));
                node.get("servlet-class").set(servlet.getServletClass());
                node.get("servlet-name").set(servlet.getServletName());
            } catch (Exception e) {
                // Should a failure in creating the mgmt view also make to the deployment to fail?
                continue;
            }
        }

    }

    @Override
    public void undeploy(final DeploymentUnit deploymentUnit) {
        deploymentUnit.removeAttachment(ServletContextAttribute.ATTACHMENT_KEY);
    }

    private static HashMap<String, TagLibraryInfo> createTldsInfo(final TldsMetaData tldsMetaData, List<TldMetaData> sharedTlds) {

        final HashMap<String, TagLibraryInfo> ret = new HashMap<>();
        if (tldsMetaData != null) {
            if (tldsMetaData.getTlds() != null) {
                for (Map.Entry<String, TldMetaData> tld : tldsMetaData.getTlds().entrySet()) {
                    createTldInfo(tld.getKey(), tld.getValue(), ret);
                }
            }
            if (sharedTlds != null) {
                for (TldMetaData metaData : sharedTlds) {

                    createTldInfo(null, metaData, ret);
                }
            }
        }

        //we also register them under the new namespaces
        for (String k : new HashSet<>(ret.keySet())) {
            if (k != null && k.startsWith(OLD_URI_PREFIX)) {
                String newUri = k.replace(OLD_URI_PREFIX, NEW_URI_PREFIX);
                ret.put(newUri, ret.get(k));
            }
        }

        return ret;
    }



    private static TagLibraryInfo createTldInfo(final String location, final TldMetaData tldMetaData, final HashMap<String, TagLibraryInfo> ret) {
        String relativeLocation = location;
        String jarPath = null;
        if (relativeLocation != null && relativeLocation.startsWith("/WEB-INF/lib/")) {
            int pos = relativeLocation.indexOf('/', "/WEB-INF/lib/".length());
            if (pos > 0) {
                jarPath = relativeLocation.substring(pos);
                if (jarPath.startsWith("/")) {
                    jarPath = jarPath.substring(1);
                }
                relativeLocation = relativeLocation.substring(0, pos);
            }
        }

        TagLibraryInfo tagLibraryInfo = new TagLibraryInfo();
        if(tldMetaData.getListeners() != null) {
            for (ListenerMetaData l : tldMetaData.getListeners()) {
                tagLibraryInfo.addListener(l.getListenerClass());
            }
        }
        tagLibraryInfo.setTlibversion(tldMetaData.getTlibVersion());
        if (tldMetaData.getJspVersion() == null) {
            tagLibraryInfo.setJspversion(tldMetaData.getVersion());
        } else {
            tagLibraryInfo.setJspversion(tldMetaData.getJspVersion());
        }
        tagLibraryInfo.setShortname(tldMetaData.getShortName());
        tagLibraryInfo.setUri(tldMetaData.getUri());
        if (tldMetaData.getDescriptionGroup() != null) {
            tagLibraryInfo.setInfo(tldMetaData.getDescriptionGroup().getDescription());
        }
        // Validator
        if (tldMetaData.getValidator() != null) {
            TagLibraryValidatorInfo tagLibraryValidatorInfo = new TagLibraryValidatorInfo();
            tagLibraryValidatorInfo.setValidatorClass(tldMetaData.getValidator().getValidatorClass());
            if (tldMetaData.getValidator().getInitParams() != null) {
                for (ParamValueMetaData paramValueMetaData : tldMetaData.getValidator().getInitParams()) {
                    tagLibraryValidatorInfo.addInitParam(paramValueMetaData.getParamName(), paramValueMetaData.getParamValue());
                }
            }
            tagLibraryInfo.setValidator(tagLibraryValidatorInfo);
        }
        // Tag
        if (tldMetaData.getTags() != null) {
            for (TagMetaData tagMetaData : tldMetaData.getTags()) {
                TagInfo tagInfo = new TagInfo();
                tagInfo.setTagName(tagMetaData.getName());
                tagInfo.setTagClassName(tagMetaData.getTagClass());
                tagInfo.setTagExtraInfo(tagMetaData.getTeiClass());
                if (tagMetaData.getBodyContent() != null) {
                    tagInfo.setBodyContent(tagMetaData.getBodyContent().toString());
                }
                tagInfo.setDynamicAttributes(tagMetaData.getDynamicAttributes());
                // Description group
                if (tagMetaData.getDescriptionGroup() != null) {
                    DescriptionGroupMetaData descriptionGroup = tagMetaData.getDescriptionGroup();
                    if (descriptionGroup.getIcons() != null && descriptionGroup.getIcons().value() != null
                            && (descriptionGroup.getIcons().value().length > 0)) {
                        Icon icon = descriptionGroup.getIcons().value()[0];
                        tagInfo.setLargeIcon(icon.largeIcon());
                        tagInfo.setSmallIcon(icon.smallIcon());
                    }
                    tagInfo.setInfoString(descriptionGroup.getDescription());
                    tagInfo.setDisplayName(descriptionGroup.getDisplayName());
                }
                // Variable
                if (tagMetaData.getVariables() != null) {
                    for (VariableMetaData variableMetaData : tagMetaData.getVariables()) {
                        TagVariableInfo tagVariableInfo = new TagVariableInfo();
                        tagVariableInfo.setNameGiven(variableMetaData.getNameGiven());
                        tagVariableInfo.setNameFromAttribute(variableMetaData.getNameFromAttribute());
                        tagVariableInfo.setClassName(variableMetaData.getVariableClass());
                        tagVariableInfo.setDeclare(variableMetaData.getDeclare());
                        if (variableMetaData.getScope() != null) {
                            tagVariableInfo.setScope(variableMetaData.getScope().toString());
                        }
                        tagInfo.addTagVariableInfo(tagVariableInfo);
                    }
                }
                // Attribute
                if (tagMetaData.getAttributes() != null) {
                    for (AttributeMetaData attributeMetaData : tagMetaData.getAttributes()) {
                        TagAttributeInfo tagAttributeInfo = new TagAttributeInfo();
                        tagAttributeInfo.setName(attributeMetaData.getName());
                        tagAttributeInfo.setType(attributeMetaData.getType());
                        tagAttributeInfo.setReqTime(attributeMetaData.getRtexprvalue());
                        tagAttributeInfo.setRequired(attributeMetaData.getRequired());
                        tagAttributeInfo.setFragment(attributeMetaData.getFragment());
                        if (attributeMetaData.getDeferredValue() != null) {
                            tagAttributeInfo.setDeferredValue("true");
                            tagAttributeInfo.setExpectedTypeName(attributeMetaData.getDeferredValue().getType());
                        } else {
                            tagAttributeInfo.setDeferredValue("false");
                        }
                        if (attributeMetaData.getDeferredMethod() != null) {
                            tagAttributeInfo.setDeferredMethod("true");
                            tagAttributeInfo.setMethodSignature(attributeMetaData.getDeferredMethod().getMethodSignature());
                        } else {
                            tagAttributeInfo.setDeferredMethod("false");
                        }
                        tagInfo.addTagAttributeInfo(tagAttributeInfo);
                    }
                }
                tagLibraryInfo.addTagInfo(tagInfo);
            }
        }
        // Tag files
        if (tldMetaData.getTagFiles() != null) {
            for (TagFileMetaData tagFileMetaData : tldMetaData.getTagFiles()) {
                TagFileInfo tagFileInfo = new TagFileInfo();
                tagFileInfo.setName(tagFileMetaData.getName());
                tagFileInfo.setPath(tagFileMetaData.getPath());
                tagLibraryInfo.addTagFileInfo(tagFileInfo);
            }
        }
        // Function
        if (tldMetaData.getFunctions() != null) {
            for (FunctionMetaData functionMetaData : tldMetaData.getFunctions()) {
                FunctionInfo functionInfo = new FunctionInfo();
                functionInfo.setName(functionMetaData.getName());
                functionInfo.setFunctionClass(functionMetaData.getFunctionClass());
                functionInfo.setFunctionSignature(functionMetaData.getFunctionSignature());
                tagLibraryInfo.addFunctionInfo(functionInfo);
            }
        }

        if (jarPath == null && relativeLocation == null) {
            if (!ret.containsKey(tagLibraryInfo.getUri())) {
                ret.put(tagLibraryInfo.getUri(), tagLibraryInfo);
            }
        } else if (jarPath == null) {
            tagLibraryInfo.setLocation("");
            tagLibraryInfo.setPath(relativeLocation);
            if (!ret.containsKey(tagLibraryInfo.getUri())) {
                ret.put(tagLibraryInfo.getUri(), tagLibraryInfo);
            }
            ret.put(relativeLocation, tagLibraryInfo);
        } else {
            tagLibraryInfo.setLocation(relativeLocation);
            tagLibraryInfo.setPath(jarPath);
            if (!ret.containsKey(tagLibraryInfo.getUri())) {
                ret.put(tagLibraryInfo.getUri(), tagLibraryInfo);
            }
            if (jarPath.equals("META-INF/taglib.tld")) {
                ret.put(relativeLocation, tagLibraryInfo);
            }
        }
        return tagLibraryInfo;
    }

}

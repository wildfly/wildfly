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

import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.function.BiFunction;
import java.util.function.Predicate;

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
import org.jboss.as.clustering.controller.SimpleCapabilityServiceConfigurator;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.as.ee.component.ComponentRegistry;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.security.deployment.AbstractSecurityDeployer;
import org.jboss.as.security.deployment.SecurityAttachments;
import org.jboss.as.security.plugins.SecurityDomainContext;
import org.jboss.as.security.service.JaccService;
import org.jboss.as.security.service.SecurityDomainService;
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
import org.jboss.as.server.suspend.SuspendController;
import org.jboss.as.web.common.ExpressionFactoryWrapper;
import org.jboss.as.web.common.ServletContextAttribute;
import org.jboss.as.web.common.WarMetaData;
import org.jboss.as.web.common.WebComponentDescription;
import org.jboss.as.web.common.WebInjectionContainer;
import org.jboss.as.web.session.SessionIdentifierCodec;
import org.jboss.dmr.ModelNode;
import org.jboss.metadata.ear.jboss.JBossAppMetaData;
import org.jboss.metadata.ear.spec.EarMetaData;
import org.jboss.metadata.javaee.spec.DescriptionGroupMetaData;
import org.jboss.metadata.javaee.spec.ParamValueMetaData;
import org.jboss.metadata.web.jboss.JBossServletMetaData;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.metadata.web.spec.AttributeMetaData;
import org.jboss.metadata.web.spec.FunctionMetaData;
import org.jboss.metadata.web.spec.ListenerMetaData;
import org.jboss.metadata.web.spec.TagFileMetaData;
import org.jboss.metadata.web.spec.TagMetaData;
import org.jboss.metadata.web.spec.TldMetaData;
import org.jboss.metadata.web.spec.VariableMetaData;
import org.jboss.modules.Module;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.value.ImmediateValue;
import org.jboss.msc.value.InjectedValue;
import org.jboss.security.SecurityUtil;
import org.jboss.vfs.VirtualFile;
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
import org.wildfly.extension.undertow.session.DistributableSessionIdentifierCodecServiceConfiguratorProvider;
import org.wildfly.extension.undertow.session.DistributableSessionManagerConfiguration;
import org.wildfly.extension.undertow.session.DistributableSessionManagerFactoryServiceConfiguratorProvider;
import org.wildfly.extension.undertow.session.SharedSessionManagerConfig;
import org.wildfly.extension.undertow.session.SimpleDistributableSessionManagerConfiguration;
import org.wildfly.extension.undertow.session.SimpleSessionIdentifierCodecServiceConfigurator;

import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.SessionManagerFactory;
import io.undertow.servlet.core.InMemorySessionManagerFactory;

public class UndertowDeploymentProcessor implements DeploymentUnitProcessor {

    public static final String OLD_URI_PREFIX = "http://java.sun.com";
    public static final String NEW_URI_PREFIX = "http://xmlns.jcp.org";


    private final String defaultServer;
    private final String defaultHost;
    private final String defaultContainer;
    private final String defaultSecurityDomain;
    private final Predicate<String> knownSecurityDomain;
    /**
        default module mappings, where we have key as name of default deployment,
        for value we have Map.Entry which has key as server-name where deployment is bound to,
        value is host name where deployment is bound to.
     */
    private final DefaultDeploymentMappingProvider defaultModuleMappingProvider;

    public UndertowDeploymentProcessor(String defaultHost, final String defaultContainer, String defaultServer, String defaultSecurityDomain, Predicate<String> knownSecurityDomain) {
        this.defaultHost = defaultHost;
        this.defaultSecurityDomain = defaultSecurityDomain;
        this.defaultModuleMappingProvider = DefaultDeploymentMappingProvider.instance();
        if (defaultHost == null) {
            throw UndertowLogger.ROOT_LOGGER.nullDefaultHost();
        }
        this.defaultContainer = defaultContainer;
        this.defaultServer = defaultServer;
        this.knownSecurityDomain = knownSecurityDomain;
    }

    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();

        //install the control point for the top level deployment no matter what
        if(RequestControllerActivationMarker.isRequestControllerEnabled(deploymentUnit)) {
            if(deploymentUnit.getParent() == null) {
                ControlPointService.install(phaseContext.getServiceTarget(), deploymentUnit.getName(), UndertowExtension.SUBSYSTEM_NAME);
            }
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

        final Map.Entry<String,String> severHost = defaultModuleMappingProvider.getMapping(deploymentName);
        String defaultHostForDeployment;
        String defaultServerForDeployment;
        if (severHost != null) {
            defaultServerForDeployment = severHost.getKey();
            defaultHostForDeployment = severHost.getValue();
        } else {
            defaultServerForDeployment = this.defaultServer;
            defaultHostForDeployment = this.defaultHost;
        }

        String serverInstanceName = warMetaData.getMergedJBossWebMetaData().getServerInstanceName() == null ? defaultServerForDeployment : warMetaData.getMergedJBossWebMetaData().getServerInstanceName();
        String hostName = hostNameOfDeployment(warMetaData, defaultHostForDeployment);
        processDeployment(warMetaData, deploymentUnit, phaseContext.getServiceTarget(), deploymentName, hostName, serverInstanceName);
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
                                   final String deploymentName, final String hostName, final String serverInstanceName)
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

        boolean componentRegistryExists = true;
        ComponentRegistry componentRegistry = deploymentUnit.getAttachment(org.jboss.as.ee.component.Attachments.COMPONENT_REGISTRY);
        if (componentRegistry == null) {
            componentRegistryExists = false;
            //we do this to avoid lots of other null checks
            //this will only happen if the EE subsystem is not installed
            componentRegistry = new ComponentRegistry(null);
        }

        final WebInjectionContainer injectionContainer = new WebInjectionContainer(module.getClassLoader(), componentRegistry);

        String jaccContextId = metaData.getJaccContextID();

        if (jaccContextId == null) {
            jaccContextId = deploymentUnit.getName();
        }
        if (deploymentUnit.getParent() != null) {
            jaccContextId = deploymentUnit.getParent().getName() + "!" + jaccContextId;
        }

        final String pathName = pathNameOfDeployment(deploymentUnit, metaData);

        boolean securityEnabled = deploymentUnit.hasAttachment(SecurityAttachments.SECURITY_ENABLED);

        String tempSecurityDomain = metaData.getSecurityDomain();
        if (tempSecurityDomain == null) {
            tempSecurityDomain = getJBossAppSecurityDomain(deploymentUnit);
        }
        tempSecurityDomain = tempSecurityDomain == null ? defaultSecurityDomain : SecurityUtil.unprefixSecurityDomain(tempSecurityDomain);
        boolean known = tempSecurityDomain != null && knownSecurityDomain.test(tempSecurityDomain);

        final String securityDomain = (securityEnabled || known) ? tempSecurityDomain : null;

        final Set<ServiceName> additionalDependencies = new HashSet<>();
        for (final SetupAction setupAction : setupActions) {
            Set<ServiceName> dependencies = setupAction.dependencies();
            if (dependencies != null) {
                additionalDependencies.addAll(dependencies);
            }
        }
        SharedSessionManagerConfig sharedSessionManagerConfig = deploymentUnit.getParent() != null ? deploymentUnit.getParent().getAttachment(UndertowAttachments.SHARED_SESSION_MANAGER_CONFIG) : null;

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


        TldsMetaData tldsMetaData = deploymentUnit.getAttachment(TldsMetaData.ATTACHMENT_KEY);
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
                .createUndertowDeploymentInfoService();

        final ServiceName deploymentInfoServiceName = deploymentServiceName.append(UndertowDeploymentInfoService.SERVICE_NAME);
        final ServiceName legacyDeploymentInfoServiceName = legacyDeploymentServiceName.append(UndertowDeploymentInfoService.SERVICE_NAME);
        ServiceBuilder<DeploymentInfo> infoBuilder = serviceTarget.addService(deploymentInfoServiceName, undertowDeploymentInfoService)
                .addAliases(legacyDeploymentInfoServiceName)
                .addDependency(UndertowService.SERVLET_CONTAINER.append(servletContainerName), ServletContainerService.class, undertowDeploymentInfoService.getContainer())
                .addDependency(UndertowService.UNDERTOW, UndertowService.class, undertowDeploymentInfoService.getUndertowService())
                .addDependency(hostServiceName, Host.class, undertowDeploymentInfoService.getHost())
                .addDependency(ServerEnvironmentService.SERVICE_NAME, ServerEnvironment.class, undertowDeploymentInfoService.getServerEnvironmentInjectedValue())
                .addDependency(SuspendController.SERVICE_NAME, SuspendController.class, undertowDeploymentInfoService.getSuspendControllerInjectedValue())
                .addDependencies(additionalDependencies);
        if(securityDomain != null) {
            if (known) {
                infoBuilder.addDependency(
                        deploymentUnit.getAttachment(Attachments.CAPABILITY_SERVICE_SUPPORT)
                                .getCapabilityServiceName(
                                        Capabilities.CAPABILITY_APPLICATION_SECURITY_DOMAIN,
                                        securityDomain),
                        BiFunction.class, undertowDeploymentInfoService.getSecurityFunctionInjector());
            } else {
                infoBuilder.addDependency(SecurityDomainService.SERVICE_NAME.append(securityDomain), SecurityDomainContext.class, undertowDeploymentInfoService.getSecurityDomainContextValue());
            }
        }

        if(RequestControllerActivationMarker.isRequestControllerEnabled(deploymentUnit)){
            String topLevelName;
            if(deploymentUnit.getParent() == null) {
                topLevelName = deploymentUnit.getName();
            } else {
                topLevelName = deploymentUnit.getParent().getName();
            }
            infoBuilder.addDependency(ControlPointService.serviceName(topLevelName, UndertowExtension.SUBSYSTEM_NAME), ControlPoint.class, undertowDeploymentInfoService.getControlPointInjectedValue());
        }
        final Set<String> seenExecutors = new HashSet<String>();
        if (metaData.getExecutorName() != null) {
            final InjectedValue<Executor> executor = new InjectedValue<Executor>();
            infoBuilder.addDependency(IOServices.WORKER.append(metaData.getExecutorName()), Executor.class, executor);
            undertowDeploymentInfoService.addInjectedExecutor(metaData.getExecutorName(), executor);
            seenExecutors.add(metaData.getExecutorName());
        }
        if (metaData.getServlets() != null) {
            for (JBossServletMetaData servlet : metaData.getServlets()) {
                if (servlet.getExecutorName() != null && !seenExecutors.contains(servlet.getExecutorName())) {
                    final InjectedValue<Executor> executor = new InjectedValue<Executor>();
                    infoBuilder.addDependency(IOServices.WORKER.append(servlet.getExecutorName()), Executor.class, executor);
                    undertowDeploymentInfoService.addInjectedExecutor(servlet.getExecutorName(), executor);
                    seenExecutors.add(servlet.getExecutorName());
                }
            }
        }

        if (componentRegistryExists) {
            infoBuilder.addDependency(ComponentRegistry.serviceName(deploymentUnit), ComponentRegistry.class, undertowDeploymentInfoService.getComponentRegistryInjectedValue());
        } else {
            undertowDeploymentInfoService.getComponentRegistryInjectedValue().setValue(new ImmediateValue<>(componentRegistry));
        }

        if (sharedSessionManagerConfig != null) {
            infoBuilder.addDependency(deploymentUnit.getParent().getServiceName().append(SharedSessionManagerConfig.SHARED_SESSION_MANAGER_SERVICE_NAME), SessionManagerFactory.class, undertowDeploymentInfoService.getSessionManagerFactoryInjector());
            infoBuilder.addDependency(deploymentUnit.getParent().getServiceName().append(SharedSessionManagerConfig.SHARED_SESSION_IDENTIFIER_CODEC_SERVICE_NAME), SessionIdentifierCodec.class, undertowDeploymentInfoService.getSessionIdentifierCodecInjector());
        } else {
            CapabilityServiceSupport support = deploymentUnit.getAttachment(Attachments.CAPABILITY_SERVICE_SUPPORT);

            CapabilityServiceConfigurator factoryConfigurator = getSessionManagerFactoryServiceConfigurator(deploymentServiceName, serverInstanceName, deploymentName, module, metaData, deploymentUnit.getAttachment(UndertowAttachments.SERVLET_CONTAINER_SERVICE));
            infoBuilder.addDependency(factoryConfigurator.getServiceName(), SessionManagerFactory.class, undertowDeploymentInfoService.getSessionManagerFactoryInjector());

            CapabilityServiceConfigurator codecConfigurator = getSessionIdentifierCodecServiceConfigurator(deploymentServiceName, serverInstanceName, deploymentName, metaData);
            infoBuilder.addDependency(codecConfigurator.getServiceName(), SessionIdentifierCodec.class, undertowDeploymentInfoService.getSessionIdentifierCodecInjector());

            for (CapabilityServiceConfigurator configurator : Arrays.asList(factoryConfigurator, codecConfigurator)) {
                configurator.configure(support).build(serviceTarget).install();
            }
        }

        infoBuilder.install();

        final UndertowDeploymentService service = new UndertowDeploymentService(injectionContainer, true);
        final ServiceBuilder<UndertowDeploymentService> builder = serviceTarget.addService(deploymentServiceName, service)
                .addAliases(legacyDeploymentServiceName)
                .addDependencies(dependentComponents)
                .addDependency(UndertowService.SERVLET_CONTAINER.append(defaultContainer), ServletContainerService.class, service.getContainer())
                .addDependency(hostServiceName, Host.class, service.getHost())
                .addDependencies(deploymentUnit.getAttachmentList(Attachments.WEB_DEPENDENCIES))
                .addDependency(deploymentInfoServiceName, DeploymentInfo.class, service.getDeploymentInfoInjectedValue());
        // inject the server executor which can be used by the WebDeploymentService for blocking tasks in start/stop
        // of that service
        Services.addServerExecutorDependency(builder, service.getServerExecutorInjector());
        builder.install();

        deploymentUnit.addToAttachmentList(Attachments.DEPLOYMENT_COMPLETE_SERVICES, deploymentServiceName);


        // adding JACC service
        if(securityEnabled) {
            AbstractSecurityDeployer<WarMetaData> deployer = new WarJACCDeployer();
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
                // add dependency to web deployment service
                jaccBuilder.addDependency(deploymentServiceName);
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

    private static CapabilityServiceConfigurator getSessionManagerFactoryServiceConfigurator(ServiceName deploymentServiceName, String serverName, String deploymentName, Module module, JBossWebMetaData metaData, ServletContainerService servletContainerService) {

        Integer maxActiveSessions = metaData.getMaxActiveSessions();
        if(maxActiveSessions == null && servletContainerService != null) {
            maxActiveSessions = servletContainerService.getMaxSessions();
        }
        ServiceName name = deploymentServiceName.append("session");
        if (metaData.getDistributable() != null) {
            if (DistributableSessionManagerFactoryServiceConfiguratorProvider.INSTANCE.isPresent()) {
                DistributableSessionManagerConfiguration config = new SimpleDistributableSessionManagerConfiguration(maxActiveSessions, metaData.getReplicationConfig(), serverName, deploymentName, module);
                return DistributableSessionManagerFactoryServiceConfiguratorProvider.INSTANCE.get().getServiceConfigurator(name, config);
            }
            // Fallback to local session manager if server does not support clustering
            UndertowLogger.ROOT_LOGGER.clusteringNotSupported();
        }
        return new SimpleCapabilityServiceConfigurator<>(name, (maxActiveSessions != null) ? new InMemorySessionManagerFactory(maxActiveSessions) : new InMemorySessionManagerFactory());
    }

    private static CapabilityServiceConfigurator getSessionIdentifierCodecServiceConfigurator(ServiceName deploymentServiceName, String serverName, String deploymentName, JBossWebMetaData metaData) {
        ServiceName name = deploymentServiceName.append("codec");
        if (metaData.getDistributable() != null) {
            if (DistributableSessionIdentifierCodecServiceConfiguratorProvider.INSTANCE.isPresent()) {
                return DistributableSessionIdentifierCodecServiceConfiguratorProvider.INSTANCE.get().getDeploymentServiceConfigurator(name, serverName, deploymentName);
            }
            // Fallback to simple codec if server does not support clustering
        }
        return new SimpleSessionIdentifierCodecServiceConfigurator(name, serverName);
    }

    static String pathNameOfDeployment(final DeploymentUnit deploymentUnit, final JBossWebMetaData metaData) {
        String pathName;
        if (metaData.getContextRoot() == null) {
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

    /**
     * Try to obtain the security domain configured in jboss-app.xml at the ear level if available
     */
    private String getJBossAppSecurityDomain(final DeploymentUnit deploymentUnit) {
        String securityDomain = null;
        DeploymentUnit parent = deploymentUnit.getParent();
        if (parent != null) {
            final EarMetaData jbossAppMetaData = parent.getAttachment(org.jboss.as.ee.structure.Attachments.EAR_METADATA);
            if (jbossAppMetaData instanceof JBossAppMetaData) {
                securityDomain = ((JBossAppMetaData) jbossAppMetaData).getSecurityDomain();
            }
        }
        return securityDomain != null ? securityDomain.trim() : null;
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
            if (k != null) {
                if (k.startsWith(OLD_URI_PREFIX)) {
                    String newUri = k.replace(OLD_URI_PREFIX, NEW_URI_PREFIX);
                    ret.put(newUri, ret.get(k));
                }
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

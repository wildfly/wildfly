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

package org.wildfly.extension.undertow.deployment;

import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.SessionManagerFactory;
import io.undertow.servlet.core.InMemorySessionManagerFactory;

import org.apache.jasper.Constants;
import org.jboss.as.controller.PathElement;
import org.jboss.as.ee.component.ComponentRegistry;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.security.deployment.AbstractSecurityDeployer;
import org.jboss.as.security.deployment.SecurityAttachments;
import org.jboss.as.security.plugins.SecurityDomainContext;
import org.jboss.as.security.service.JaccService;
import org.jboss.as.security.service.SecurityDomainService;
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
import org.jboss.as.web.common.ExpressionFactoryWrapper;
import org.jboss.as.web.common.ServletContextAttribute;
import org.jboss.as.web.common.WarMetaData;
import org.jboss.as.web.common.WebComponentDescription;
import org.jboss.as.web.common.WebInjectionContainer;
import org.jboss.as.web.host.ContextActivator;
import org.jboss.as.web.session.SessionIdentifierCodec;
import org.jboss.dmr.ModelNode;
import org.jboss.metadata.ear.jboss.JBossAppMetaData;
import org.jboss.metadata.ear.spec.EarMetaData;
import org.jboss.metadata.web.jboss.JBossServletMetaData;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.metadata.web.spec.TldMetaData;
import org.jboss.modules.Module;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.ValueService;
import org.jboss.msc.value.ImmediateValue;
import org.jboss.msc.value.InjectedValue;
import org.jboss.security.SecurityConstants;
import org.jboss.security.SecurityUtil;
import org.jboss.vfs.VirtualFile;
import org.wildfly.extension.io.IOServices;
import org.wildfly.extension.requestcontroller.ControlPoint;
import org.wildfly.extension.requestcontroller.ControlPointService;
import org.wildfly.extension.requestcontroller.RequestControllerActivationMarker;
import org.wildfly.extension.undertow.DeploymentDefinition;
import org.wildfly.extension.undertow.Host;
import org.wildfly.extension.undertow.ServletContainerService;
import org.wildfly.extension.undertow.UndertowExtension;
import org.wildfly.extension.undertow.logging.UndertowLogger;
import org.wildfly.extension.undertow.UndertowService;
import org.wildfly.extension.undertow.security.jacc.WarJACCDeployer;
import org.wildfly.extension.undertow.session.DistributableSessionIdentifierCodecBuilder;
import org.wildfly.extension.undertow.session.DistributableSessionIdentifierCodecBuilderValue;
import org.wildfly.extension.undertow.session.DistributableSessionManagerFactoryBuilder;
import org.wildfly.extension.undertow.session.DistributableSessionManagerFactoryBuilderValue;
import org.wildfly.extension.undertow.session.SharedSessionManagerConfig;
import org.wildfly.extension.undertow.session.SimpleDistributableSessionManagerConfiguration;
import org.wildfly.extension.undertow.session.SimpleSessionIdentifierCodecService;

import javax.security.jacc.PolicyConfiguration;

import java.net.MalformedURLException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;

public class UndertowDeploymentProcessor implements DeploymentUnitProcessor {

    private final String defaultServer;
    private final String defaultHost;
    private final String defaultContainer;

    public UndertowDeploymentProcessor(String defaultHost, final String defaultContainer, String defaultServer) {
        this.defaultHost = defaultHost;
        if (defaultHost == null) {
            throw UndertowLogger.ROOT_LOGGER.nullDefaultHost();
        }
        this.defaultContainer = defaultContainer;
        this.defaultServer = defaultServer;
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
        String hostName = hostNameOfDeployment(warMetaData, defaultHost);
        processDeployment(warMetaData, deploymentUnit, phaseContext.getServiceTarget(), hostName);
    }


    static String hostNameOfDeployment(final WarMetaData metaData, final String defaultHost) {
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

    @Override
    public void undeploy(final DeploymentUnit context) {
        //AbstractSecurityDeployer<?> deployer = new WarJACCDeployer();
        //deployer.undeploy(context);
    }

    private void processDeployment(final WarMetaData warMetaData, final DeploymentUnit deploymentUnit, final ServiceTarget serviceTarget, String hostName)
            throws DeploymentUnitProcessingException {
        ResourceRoot deploymentResourceRoot = deploymentUnit.getAttachment(Attachments.DEPLOYMENT_ROOT);
        final VirtualFile deploymentRoot = deploymentResourceRoot.getRoot();
        final Module module = deploymentUnit.getAttachment(Attachments.MODULE);
        if (module == null) {
            throw new DeploymentUnitProcessingException(UndertowLogger.ROOT_LOGGER.failedToResolveModule(deploymentUnit));
        }
        final JBossWebMetaData metaData = warMetaData.getMergedJBossWebMetaData();
        final List<SetupAction> setupActions = deploymentUnit.getAttachmentList(org.jboss.as.ee.component.Attachments.WEB_SETUP_ACTIONS);
        metaData.resolveRunAs();

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

        String deploymentName;
        if (deploymentUnit.getParent() == null) {
            deploymentName = deploymentUnit.getName();
        } else {
            deploymentName = deploymentUnit.getParent().getName() + "." + deploymentUnit.getName();
        }

        final String pathName = pathNameOfDeployment(deploymentUnit, metaData);

        boolean securityEnabled = deploymentUnit.hasAttachment(SecurityAttachments.SECURITY_ENABLED);

        String metaDataSecurityDomain = metaData.getSecurityDomain();
        if (metaDataSecurityDomain == null) {
            metaDataSecurityDomain = getJBossAppSecurityDomain(deploymentUnit);
        }
        if (metaDataSecurityDomain != null) {
            metaDataSecurityDomain = metaDataSecurityDomain.trim();
        }

        final String securityDomain;
        if(securityEnabled) {
            securityDomain = metaDataSecurityDomain == null ? SecurityConstants.DEFAULT_APPLICATION_POLICY : SecurityUtil
                    .unprefixSecurityDomain(metaDataSecurityDomain);
        } else {
            securityDomain = null;
        }

        String serverInstanceName = metaData.getServerInstanceName() == null ? defaultServer : metaData.getServerInstanceName();
        final ServiceName deploymentServiceName = UndertowService.deploymentServiceName(serverInstanceName, hostName, pathName);

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

        final ServiceName hostServiceName = UndertowService.virtualHostName(serverInstanceName, hostName);
        TldsMetaData tldsMetaData = deploymentUnit.getAttachment(TldsMetaData.ATTACHMENT_KEY);
        UndertowDeploymentInfoService undertowDeploymentInfoService = UndertowDeploymentInfoService.builder()
                .setAttributes(deploymentUnit.getAttachmentList(ServletContextAttribute.ATTACHMENT_KEY))
                .setTopLevelDeploymentName(deploymentUnit.getParent() == null ? deploymentUnit.getName() : deploymentUnit.getParent().getName())
                .setContextPath(pathName)
                .setDeploymentName(deploymentName) //todo: is this deployment name concept really applicable?
                .setDeploymentRoot(deploymentRoot)
                .setMergedMetaData(warMetaData.getMergedJBossWebMetaData())
                .setModule(module)
                .setScisMetaData(scisMetaData)
                .setJaccContextId(jaccContextId)
                .setSecurityDomain(securityDomain)
                .setSharedTlds(tldsMetaData == null ? Collections.<TldMetaData>emptyList() : tldsMetaData.getSharedTlds(deploymentUnit))
                .setTldsMetaData(tldsMetaData)
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
                .createUndertowDeploymentInfoService();

        final ServiceName deploymentInfoServiceName = deploymentServiceName.append(UndertowDeploymentInfoService.SERVICE_NAME);
        ServiceBuilder<DeploymentInfo> infoBuilder = serviceTarget.addService(deploymentInfoServiceName, undertowDeploymentInfoService)
                .addDependency(UndertowService.SERVLET_CONTAINER.append(defaultContainer), ServletContainerService.class, undertowDeploymentInfoService.getContainer())
                .addDependency(UndertowService.UNDERTOW, UndertowService.class, undertowDeploymentInfoService.getUndertowService())
                .addDependencies(deploymentUnit.getAttachmentList(Attachments.WEB_DEPENDENCIES))
                .addDependency(hostServiceName, Host.class, undertowDeploymentInfoService.getHost())
                .addDependencies(additionalDependencies);
        if(securityDomain != null) {
            infoBuilder.addDependency(SecurityDomainService.SERVICE_NAME.append(securityDomain), SecurityDomainContext.class, undertowDeploymentInfoService.getSecurityDomainContextValue());
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
            ServiceName sessionManagerFactoryServiceName = installSessionManagerFactory(serviceTarget, deploymentServiceName, deploymentName, module, metaData);
            infoBuilder.addDependency(sessionManagerFactoryServiceName, SessionManagerFactory.class, undertowDeploymentInfoService.getSessionManagerFactoryInjector());

            ServiceName sessionIdentifierCodecServiceName = installSessionIdentifierCodec(serviceTarget, deploymentServiceName, deploymentName, metaData);
            infoBuilder.addDependency(sessionIdentifierCodecServiceName, SessionIdentifierCodec.class, undertowDeploymentInfoService.getSessionIdentifierCodecInjector());
        }

        infoBuilder.install();

        final boolean isWebappBundle = deploymentUnit.hasAttachment(Attachments.OSGI_MANIFEST);

        final UndertowDeploymentService service = new UndertowDeploymentService(injectionContainer, !isWebappBundle);
        final ServiceBuilder<UndertowDeploymentService> builder = serviceTarget.addService(deploymentServiceName, service)
                .addDependencies(dependentComponents)
                .addDependency(UndertowService.SERVLET_CONTAINER.append(defaultContainer), ServletContainerService.class, service.getContainer())
                .addDependency(hostServiceName, Host.class, service.getHost())
                .addDependencies(deploymentUnit.getAttachmentList(Attachments.WEB_DEPENDENCIES))
                .addDependency(deploymentInfoServiceName, DeploymentInfo.class, service.getDeploymentInfoInjectedValue());
        // inject the server executor which can be used by the WebDeploymentService for blocking tasks in start/stop
        // of that service
        Services.addServerExecutorDependency(builder, service.getServerExecutorInjector(), false);

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


        // OSGi web applications are activated in {@link WebContextActivationProcessor} according to bundle lifecycle changes
        if (isWebappBundle) {
            UndertowDeploymentService.ContextActivatorImpl activator = new UndertowDeploymentService.ContextActivatorImpl(builder.install());
            deploymentUnit.putAttachment(ContextActivator.ATTACHMENT_KEY, activator);
            deploymentUnit.addToAttachmentList(Attachments.BUNDLE_ACTIVE_DEPENDENCIES, deploymentServiceName);
        } else {
            builder.install();
        }

        // Process the web related mgmt information
        final DeploymentResourceSupport deploymentResourceSupport = deploymentUnit.getAttachment(Attachments.DEPLOYMENT_RESOURCE_SUPPORT);
        final ModelNode node = deploymentResourceSupport.getDeploymentSubsystemModel(UndertowExtension.SUBSYSTEM_NAME);
        node.get(DeploymentDefinition.CONTEXT_ROOT.getName()).set("".equals(pathName) ? "/" : pathName);
        node.get(DeploymentDefinition.VIRTUAL_HOST.getName()).set(hostName);
        node.get(DeploymentDefinition.SERVER.getName()).set(serverInstanceName);
        processManagement(deploymentUnit, metaData);
    }

    private static ServiceName installSessionManagerFactory(ServiceTarget target, ServiceName deploymentServiceName, String deploymentName, Module module, JBossWebMetaData metaData) {
        ServiceName name = deploymentServiceName.append("session");
        if (metaData.getDistributable() != null) {
            DistributableSessionManagerFactoryBuilder sessionManagerFactoryBuilder = new DistributableSessionManagerFactoryBuilderValue().getValue();
            if (sessionManagerFactoryBuilder != null) {
                sessionManagerFactoryBuilder.build(target, name, new SimpleDistributableSessionManagerConfiguration(metaData, deploymentName, module))
                        .setInitialMode(Mode.ON_DEMAND)
                        .install()
                ;
                return name;
            }
            // Fallback to local session manager if server does not support clustering
            UndertowLogger.ROOT_LOGGER.clusteringNotSupported();
        }
        Integer maxActiveSessions = metaData.getMaxActiveSessions();
        InMemorySessionManagerFactory factory = (maxActiveSessions != null) ? new InMemorySessionManagerFactory(maxActiveSessions.intValue()) : new InMemorySessionManagerFactory();
        target.addService(name,  new ValueService<>(new ImmediateValue<>(factory))).setInitialMode(Mode.ON_DEMAND).install();
        return name;
    }

    private static ServiceName installSessionIdentifierCodec(ServiceTarget target, ServiceName deploymentServiceName, String deploymentName, JBossWebMetaData metaData) {
        ServiceName name = deploymentServiceName.append("codec");
        if (metaData.getDistributable() != null) {
            DistributableSessionIdentifierCodecBuilder sessionIdentifierCodecBuilder = new DistributableSessionIdentifierCodecBuilderValue().getValue();
            if (sessionIdentifierCodecBuilder != null) {
                sessionIdentifierCodecBuilder.build(target, name, deploymentName).setInitialMode(Mode.ON_DEMAND).install();
                return name;
            }
            // Fallback to simple codec if server does not support clustering
        }
        SimpleSessionIdentifierCodecService.build(target, name).setInitialMode(Mode.ON_DEMAND).install();
        return name;
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
        return securityDomain;
    }

}

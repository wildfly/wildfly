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

import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.services.path.PathManager;
import org.jboss.as.controller.services.path.PathManagerService;
import org.jboss.as.ee.component.ComponentRegistry;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.security.deployment.AbstractSecurityDeployer;
import org.jboss.as.security.plugins.SecurityDomainContext;
import org.jboss.as.security.service.JaccService;
import org.jboss.as.security.service.SecurityDomainService;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.ExplodedDeploymentMarker;
import org.jboss.as.server.deployment.SetupAction;
import org.jboss.as.web.common.ExpressionFactoryWrapper;
import org.jboss.as.web.common.ServletContextAttribute;
import org.jboss.as.web.common.WarMetaData;
import org.jboss.as.web.common.WebComponentDescription;
import org.jboss.as.web.common.WebInjectionContainer;
import org.jboss.as.web.host.ContextActivator;
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
import org.jboss.msc.value.ImmediateValue;
import org.jboss.msc.value.InjectedValue;
import org.jboss.security.SecurityConstants;
import org.jboss.security.SecurityUtil;
import org.jboss.vfs.VirtualFile;
import org.wildfly.extension.io.IOServices;
import org.wildfly.extension.undertow.DeploymentDefinition;
import org.wildfly.extension.undertow.Host;
import org.wildfly.extension.undertow.ServletContainerService;
import org.wildfly.extension.undertow.UndertowExtension;
import org.wildfly.extension.undertow.UndertowLogger;
import org.wildfly.extension.undertow.UndertowService;
import org.wildfly.extension.undertow.security.jacc.WarJACCDeployer;
import org.wildfly.extension.undertow.session.DistributableSessionManagerFactoryBuilder;
import org.wildfly.extension.undertow.session.DistributableSessionManagerFactoryBuilderValue;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;

import static org.wildfly.extension.undertow.UndertowMessages.MESSAGES;

import javax.security.jacc.PolicyConfiguration;

public class UndertowDeploymentProcessor implements DeploymentUnitProcessor {

    private final String defaultServer;
    private final String defaultHost;
    private final String defaultContainer;

    public UndertowDeploymentProcessor(String defaultHost, final String defaultContainer, String defaultServer) {
        this.defaultHost = defaultHost;
        if (defaultHost == null) {
            throw MESSAGES.nullDefaultHost();
        }
        this.defaultContainer = defaultContainer;
        this.defaultServer = defaultServer;
    }

    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
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
            throw MESSAGES.nullHostName();
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
        final VirtualFile deploymentRoot = deploymentUnit.getAttachment(Attachments.DEPLOYMENT_ROOT).getRoot();
        final Module module = deploymentUnit.getAttachment(Attachments.MODULE);
        if (module == null) {
            throw new DeploymentUnitProcessingException(MESSAGES.failedToResolveModule(deploymentUnit));
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

        String securityContextId = deploymentUnit.getName();
        if (deploymentUnit.getParent() != null) {
            securityContextId = deploymentUnit.getParent().getName() + "!" + securityContextId;
        }

        final String pathName = pathNameOfDeployment(deploymentUnit, metaData);


        String metaDataSecurityDomain = metaData.getSecurityDomain();
        if (metaDataSecurityDomain == null) {
            metaDataSecurityDomain = getJBossAppSecurityDomain(deploymentUnit);
        }
        if (metaDataSecurityDomain != null) {
            metaDataSecurityDomain = metaDataSecurityDomain.trim();
        }

        String securityDomain = metaDataSecurityDomain == null ? SecurityConstants.DEFAULT_APPLICATION_POLICY : SecurityUtil
                .unprefixSecurityDomain(metaDataSecurityDomain);

        final ServiceName deploymentServiceName = UndertowService.deploymentServiceName(hostName,pathName);

        final Set<ServiceName> additionalDependencies = new HashSet<>();
        for(final SetupAction setupAction : setupActions) {
            Set<ServiceName> dependencies = setupAction.dependencies();
            if(dependencies != null) {
                additionalDependencies.addAll(dependencies);
            }
        }
        final ServiceName hostServiceName = UndertowService.virtualHostName(defaultServer, hostName);
        TldsMetaData tldsMetaData = deploymentUnit.getAttachment(TldsMetaData.ATTACHMENT_KEY);
        UndertowDeploymentInfoService undertowDeploymentInfoService = UndertowDeploymentInfoService.builder()
                        .setAttributes(deploymentUnit.getAttachment(ServletContextAttribute.ATTACHMENT_KEY))
                .setContextPath(pathName)
                .setDeploymentName(deploymentUnit.getName())
                .setDeploymentRoot(deploymentRoot)
                .setMergedMetaData(warMetaData.getMergedJBossWebMetaData())
                .setModule(module)
                .setScisMetaData(scisMetaData)
                .setSecurityContextId(securityContextId)
                .setSecurityDomain(securityDomain)
                .setSharedTlds(tldsMetaData == null ? Collections.<TldMetaData>emptyList() : tldsMetaData.getSharedTlds(deploymentUnit))
                .setTldsMetaData(tldsMetaData)
                .setSetupActions(setupActions)
                .setOverlays(warMetaData.getOverlays())
                .setExpressionFactoryWrappers(deploymentUnit.getAttachmentList(ExpressionFactoryWrapper.ATTACHMENT_KEY))
                .setPredicatedHandlers(deploymentUnit.getAttachment(UndertowHandlersDeploymentProcessor.PREDICATED_HANDLERS))
                .setExplodedDeployment(ExplodedDeploymentMarker.isExplodedDeployment(deploymentUnit))
                .createUndertowDeploymentInfoService();

        final ServiceName deploymentInfoServiceName = deploymentServiceName.append(UndertowDeploymentInfoService.SERVICE_NAME);
        ServiceBuilder<DeploymentInfo> infoBuilder = serviceTarget.addService(deploymentInfoServiceName, undertowDeploymentInfoService)
                .addDependency(UndertowService.SERVLET_CONTAINER.append(defaultContainer), ServletContainerService.class, undertowDeploymentInfoService.getContainer())
                .addDependency(SecurityDomainService.SERVICE_NAME.append(securityDomain), SecurityDomainContext.class, undertowDeploymentInfoService.getSecurityDomainContextValue())
                .addDependency(UndertowService.UNDERTOW, UndertowService.class, undertowDeploymentInfoService.getUndertowService())
                .addDependencies(deploymentUnit.getAttachmentList(Attachments.WEB_DEPENDENCIES))
                .addDependency(PathManagerService.SERVICE_NAME, PathManager.class, undertowDeploymentInfoService.getPathManagerInjector())
                .addDependency(hostServiceName, Host.class, undertowDeploymentInfoService.getHost())
                .addDependencies(additionalDependencies);

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

        if(componentRegistryExists) {
            infoBuilder.addDependency(ComponentRegistry.serviceName(deploymentUnit), ComponentRegistry.class, undertowDeploymentInfoService.getComponentRegistryInjectedValue());
        } else {
            undertowDeploymentInfoService.getComponentRegistryInjectedValue().setValue(new ImmediateValue<>(componentRegistry));
        }

        if (metaData.getDistributable() != null) {
            DistributableSessionManagerFactoryBuilder builder = new DistributableSessionManagerFactoryBuilderValue().getValue();
            if (builder != null) {
                ServiceName factoryName = deploymentServiceName.append("session");
                builder.buildDeploymentDependency(serviceTarget, factoryName, deploymentServiceName, module, metaData)
                    .setInitialMode(Mode.ON_DEMAND)
                    .install()
                ;
                infoBuilder.addDependency(factoryName, SessionManagerFactory.class, undertowDeploymentInfoService.getSessionManagerFactoryInjector());
            } else {
                UndertowLogger.ROOT_LOGGER.clusteringNotSupported();
            }
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

        deploymentUnit.addToAttachmentList(Attachments.DEPLOYMENT_COMPLETE_SERVICES, deploymentServiceName);


        // adding JACC service
        AbstractSecurityDeployer<WarMetaData> deployer = new WarJACCDeployer();
        JaccService<WarMetaData> jaccService = deployer.deploy(deploymentUnit);
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


        // OSGi web applications are activated in {@link WebContextActivationProcessor} according to bundle lifecycle changes
        if (isWebappBundle) {
            UndertowDeploymentService.ContextActivatorImpl activator = new UndertowDeploymentService.ContextActivatorImpl(builder.install());
            deploymentUnit.putAttachment(ContextActivator.ATTACHMENT_KEY, activator);
            deploymentUnit.addToAttachmentList(Attachments.BUNDLE_ACTIVE_DEPENDENCIES, deploymentServiceName);
        } else {
            builder.install();
        }

        // Process the web related mgmt information
        final ModelNode node = deploymentUnit.getDeploymentSubsystemModel(UndertowExtension.SUBSYSTEM_NAME);
        node.get(DeploymentDefinition.CONTEXT_ROOT.getName()).set("".equals(pathName) ? "/" : pathName);
        node.get(DeploymentDefinition.VIRTUAL_HOST.getName()).set(hostName);
        processManagement(deploymentUnit, metaData);
    }

    static String pathNameOfDeployment(final DeploymentUnit deploymentUnit, final JBossWebMetaData metaData) {
        String pathName;
        if (metaData.getContextRoot() == null) {
            final EEModuleDescription description = deploymentUnit.getAttachment(org.jboss.as.ee.component.Attachments.EE_MODULE_DESCRIPTION);
            if (description != null) {
                // if there is a EEModuleDescription we need to take into account that the module name may have been overridden
                pathName = "/" + description.getModuleName();
            } else {
                pathName = "/" + deploymentUnit.getName().substring(0, deploymentUnit.getName().length() - 4);
            }
        } else {
            pathName = metaData.getContextRoot();
            if ("/".equals(pathName)) {
                pathName = "";
            } else if (pathName.length() > 0 && pathName.charAt(0) != '/') {
                pathName = "/" + pathName;
            }
        }
        return pathName;
    }

    void processManagement(final DeploymentUnit unit, JBossWebMetaData metaData) {
        for (final JBossServletMetaData servlet : metaData.getServlets()) {
            try {
                final String name = servlet.getName();
                final ModelNode node = unit.createDeploymentSubModel(UndertowExtension.SUBSYSTEM_NAME, PathElement.pathElement("servlet", name));
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

/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.web.deployment;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.security.jacc.PolicyConfiguration;

import org.apache.catalina.ContainerListener;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Loader;
import org.apache.catalina.Realm;
import org.apache.catalina.Valve;
import org.apache.catalina.core.StandardContext;
import org.apache.tomcat.util.IntrospectionUtils;
import org.jboss.as.clustering.web.DistributedCacheManagerFactory;
import org.jboss.as.clustering.web.DistributedCacheManagerFactoryService;
import org.jboss.as.controller.PathElement;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.naming.deployment.JndiNamingDependencyProcessor;
import org.jboss.as.security.deployment.AbstractSecurityDeployer;
import org.jboss.as.security.plugins.SecurityDomainContext;
import org.jboss.as.security.service.JaccService;
import org.jboss.as.security.service.SecurityDomainService;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.SetupAction;
import org.jboss.as.web.VirtualHost;
import org.jboss.as.web.WebDeploymentDefinition;
import org.jboss.as.web.WebSubsystemServices;
import org.jboss.as.web.deployment.component.ComponentInstantiator;
import org.jboss.as.web.ext.WebContextFactory;
import org.jboss.as.web.security.JBossWebRealmService;
import org.jboss.as.web.security.SecurityContextAssociationValve;
import org.jboss.as.web.security.WarJaccService;
import org.jboss.dmr.ModelNode;
import org.jboss.metadata.ear.jboss.JBossAppMetaData;
import org.jboss.metadata.ear.spec.EarMetaData;
import org.jboss.metadata.javaee.spec.ParamValueMetaData;
import org.jboss.metadata.web.jboss.ContainerListenerMetaData;
import org.jboss.metadata.web.jboss.JBossServletMetaData;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.metadata.web.jboss.ValveMetaData;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceBuilder.DependencyType;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistryException;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.security.SecurityConstants;
import org.jboss.security.SecurityUtil;
import org.jboss.vfs.VirtualFile;

import static org.jboss.as.web.WebMessages.MESSAGES;

/**
 * {@code DeploymentUnitProcessor} creating the actual deployment services.
 *
 * @author Emanuel Muckenhuber
 * @author Anil.Saldhana@redhat.com
 */
public class WarDeploymentProcessor implements DeploymentUnitProcessor {

    private final String defaultHost;

    public WarDeploymentProcessor(String defaultHost) {
        if (defaultHost == null) {
            throw MESSAGES.nullDefaultHost();
        }
        this.defaultHost = defaultHost;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final WarMetaData metaData = deploymentUnit.getAttachment(WarMetaData.ATTACHMENT_KEY);
        if (metaData == null) {
            return;
        }
        String hostName = hostNameOfDeployment(metaData, defaultHost);
        processDeployment(hostName, metaData, deploymentUnit, phaseContext.getServiceTarget());
    }

    public static String hostNameOfDeployment(final WarMetaData metaData, final String defaultHost) {
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
        AbstractSecurityDeployer<?> deployer = new WarSecurityDeployer();
        deployer.undeploy(context);
    }

    protected void processDeployment(final String hostName, final WarMetaData warMetaData, final DeploymentUnit deploymentUnit,
                                     final ServiceTarget serviceTarget) throws DeploymentUnitProcessingException {
        final VirtualFile deploymentRoot = deploymentUnit.getAttachment(Attachments.DEPLOYMENT_ROOT).getRoot();
        final Module module = deploymentUnit.getAttachment(Attachments.MODULE);
        if (module == null) {
            throw new DeploymentUnitProcessingException(MESSAGES.failedToResolveModule(deploymentRoot));
        }
        final ClassLoader classLoader = module.getClassLoader();
        final JBossWebMetaData metaData = warMetaData.getMergedJBossWebMetaData();
        final List<SetupAction> setupActions = deploymentUnit
                .getAttachmentList(org.jboss.as.ee.component.Attachments.WEB_SETUP_ACTIONS);

        // Resolve the context factory
        WebContextFactory contextFactory = deploymentUnit.getAttachment(WebContextFactory.ATTACHMENT);
        if (contextFactory == null) {
            contextFactory = WebContextFactory.DEFAULT;
        }

        // Create the context
        final StandardContext webContext = contextFactory.createContext(deploymentUnit);
        final JBossContextConfig config = new JBossContextConfig(deploymentUnit);

        // Add SecurityAssociationValve right at the beginning
        webContext.addValve(new SecurityContextAssociationValve(deploymentUnit));

        // Set the deployment root
        try {
            webContext.setDocBase(deploymentRoot.getPhysicalFile().getAbsolutePath());
        } catch (IOException e) {
            throw new DeploymentUnitProcessingException(e);
        }
        webContext.addLifecycleListener(config);

        final String pathName = pathNameOfDeployment(deploymentUnit, metaData);
        webContext.setPath(pathName);
        webContext.setIgnoreAnnotations(true);
        webContext.setCrossContext(!metaData.isDisableCrossContext());

        // Hook for post processing the web context (e.g. for SIP)
        contextFactory.postProcessContext(deploymentUnit, webContext);

        final WebInjectionContainer injectionContainer = new WebInjectionContainer(module.getClassLoader());

        //see AS7-2077
        //basically we want to ignore components that have failed for whatever reason
        //if they are important they will be picked up when the web deployment actually starts
        final Map<String, ComponentInstantiator> components = deploymentUnit.getAttachment(WebAttachments.WEB_COMPONENT_INSTANTIATORS);
        if (components != null) {
            final Set<ServiceName> failed = deploymentUnit.getAttachment(org.jboss.as.ee.component.Attachments.FAILED_COMPONENTS);
            for (Map.Entry<String, ComponentInstantiator> entry : components.entrySet()) {
                boolean skip = false;
                for (final ServiceName serviceName : entry.getValue().getServiceNames()) {
                    if (failed.contains(serviceName)) {
                        skip = true;
                        break;
                    }
                }
                if (!skip) {
                    injectionContainer.addInstantiator(entry.getKey(), entry.getValue());
                }
            }
        }

        final Loader loader = new WebCtxLoader(classLoader);
        webContext.setLoader(loader);

        // Valves
        List<ValveMetaData> valves = metaData.getValves();
        if (valves == null) {
            metaData.setValves(valves = new ArrayList<ValveMetaData>());
        }
        for (ValveMetaData valve : valves) {
            Valve valveInstance = (Valve) getInstance(module, valve.getModule(), valve.getValveClass(), valve.getParams());
            webContext.getPipeline().addValve(valveInstance);
        }

        // Container listeners
        List<ContainerListenerMetaData> listeners = metaData.getContainerListeners();
        if (listeners != null) {
            for (ContainerListenerMetaData listener : listeners) {
                switch (listener.getListenerType()) {
                    case CONTAINER:
                        ContainerListener containerListener = (ContainerListener) getInstance(module, listener.getModule(), listener.getListenerClass(), listener.getParams());
                        webContext.addContainerListener(containerListener);
                        break;
                    case LIFECYCLE:
                        LifecycleListener lifecycleListener = (LifecycleListener) getInstance(module, listener.getModule(), listener.getListenerClass(), listener.getParams());
                        if (webContext instanceof Lifecycle) {
                            ((Lifecycle) webContext).addLifecycleListener(lifecycleListener);
                        }
                        break;
                   case SERVLET_INSTANCE:
                        webContext.addInstanceListener(listener.getListenerClass());
                        break;
                    case SERVLET_CONTAINER:
                        webContext.addWrapperListener(listener.getListenerClass());
                        break;
                    case SERVLET_LIFECYCLE:
                        webContext.addWrapperLifecycle(listener.getListenerClass());
                        break;
                }
            }
        }

        // Set the session cookies flag according to metadata
        switch (metaData.getSessionCookies()) {
            case JBossWebMetaData.SESSION_COOKIES_ENABLED:
                webContext.setCookies(true);
                break;
            case JBossWebMetaData.SESSION_COOKIES_DISABLED:
                webContext.setCookies(false);
                break;
        }

        String metaDataSecurityDomain = metaData.getSecurityDomain();
        if(metaDataSecurityDomain == null) {
            metaDataSecurityDomain = getJBossAppSecurityDomain(deploymentUnit);
        }
        if (metaDataSecurityDomain != null) {
            metaDataSecurityDomain = metaDataSecurityDomain.trim();
        }

        String securityDomain = metaDataSecurityDomain == null ? SecurityConstants.DEFAULT_APPLICATION_POLICY : SecurityUtil
                .unprefixSecurityDomain(metaDataSecurityDomain);

        // Setup an deployer configured ServletContext attributes
        final List<ServletContextAttribute> attributes = deploymentUnit.getAttachment(ServletContextAttribute.ATTACHMENT_KEY);

        try {
            final ServiceName deploymentServiceName = WebSubsystemServices.deploymentServiceName(hostName, pathName);
            final ServiceName realmServiceName = deploymentServiceName.append("realm");

            final JBossWebRealmService realmService = new JBossWebRealmService(deploymentUnit);
            ServiceBuilder<?> builder = serviceTarget.addService(realmServiceName, realmService);
            builder.addDependency(DependencyType.REQUIRED, SecurityDomainService.SERVICE_NAME.append(securityDomain),
                    SecurityDomainContext.class, realmService.getSecurityDomainContextInjector()).setInitialMode(Mode.ACTIVE)
                    .install();

            final WebDeploymentService webDeploymentService = new WebDeploymentService(webContext, injectionContainer, setupActions, attributes);
            builder = serviceTarget
                    .addService(deploymentServiceName, webDeploymentService)
                    .addDependency(WebSubsystemServices.JBOSS_WEB_HOST.append(hostName), VirtualHost.class,
                            new WebContextInjector(webContext)).addDependencies(injectionContainer.getServiceNames())
                    .addDependency(realmServiceName, Realm.class, webDeploymentService.getRealm())
                    .addDependencies(deploymentUnit.getAttachmentList(Attachments.WEB_DEPENDENCIES))
                    .addDependency(JndiNamingDependencyProcessor.serviceName(deploymentUnit));

            //add any dependencies required by the setup action
            for(final SetupAction action : setupActions) {
                builder.addDependencies(action.dependencies());
            }

            if (metaData.getDistributable() != null) {
                DistributedCacheManagerFactoryService factoryService = new DistributedCacheManagerFactoryService();
                DistributedCacheManagerFactory factory = factoryService.getValue();
                if (factory != null) {
                    ServiceName factoryServiceName = deploymentServiceName.append("session");
                    builder.addDependency(DependencyType.OPTIONAL, factoryServiceName, DistributedCacheManagerFactory.class, config.getDistributedCacheManagerFactoryInjector());

                    ServiceBuilder<DistributedCacheManagerFactory> factoryBuilder = serviceTarget.addService(factoryServiceName, factoryService);
                    boolean enabled = factory.addDeploymentDependencies(deploymentServiceName, deploymentUnit.getServiceRegistry(), serviceTarget, factoryBuilder, metaData);
                    factoryBuilder.setInitialMode(enabled ? ServiceController.Mode.ON_DEMAND : ServiceController.Mode.NEVER).install();
                }
            }

            builder.install();

            // adding JACC service
            AbstractSecurityDeployer<?> deployer = new WarSecurityDeployer();
            JaccService<?> service = deployer.deploy(deploymentUnit);
            if (service != null) {
                ((WarJaccService) service).setContext(webContext);
                final ServiceName jaccServiceName = deploymentUnit.getServiceName().append(JaccService.SERVICE_NAME);
                builder = serviceTarget.addService(jaccServiceName, service);
                if (deploymentUnit.getParent() != null) {
                    // add dependency to parent policy
                    final DeploymentUnit parentDU = deploymentUnit.getParent();
                    builder.addDependency(parentDU.getServiceName().append(JaccService.SERVICE_NAME), PolicyConfiguration.class,
                            service.getParentPolicyInjector());
                }
                // add dependency to web deployment service
                builder.addDependency(deploymentServiceName);
                builder.setInitialMode(Mode.ACTIVE).install();
            }
        } catch (ServiceRegistryException e) {
            throw new DeploymentUnitProcessingException(MESSAGES.failedToAddWebDeployment(), e);
        }

        // Process the web related mgmt information
        final ModelNode node = deploymentUnit.getDeploymentSubsystemModel("web");
        node.get(WebDeploymentDefinition.CONTEXT_ROOT.getName()).set("".equals(pathName) ? "/" : pathName);
        node.get(WebDeploymentDefinition.VIRTUAL_HOST.getName()).set(hostName);
        processManagement(deploymentUnit, metaData);
    }

    public static String pathNameOfDeployment(final DeploymentUnit deploymentUnit, final JBossWebMetaData metaData) {
        String pathName;
        if (metaData.getContextRoot() == null) {


            final EEModuleDescription description = deploymentUnit.getAttachment(org.jboss.as.ee.component.Attachments.EE_MODULE_DESCRIPTION);
            if(description != null) {
                //if there is a EEModuleDescription we need to take into account that the module name
                //may have been overridden
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
                final ModelNode node = unit.createDeploymentSubModel("web", PathElement.pathElement("servlet", name));
                node.get("servlet-class").set(servlet.getServletClass());
                node.get("servlet-name").set(servlet.getServletName());
            } catch (Exception e) {
                // Should a failure in creating the mgmt view also make to the deployment to fail?
                continue;
            }
        }

    }

    protected Object getInstance(Module module, String moduleName, String className, List<ParamValueMetaData> params)
        throws DeploymentUnitProcessingException {
        try {
            ClassLoader moduleClassLoader = null;
            if (moduleName == null) {
                moduleClassLoader = module.getClassLoader();
            } else {
                moduleClassLoader = module.getModule(ModuleIdentifier.create(moduleName)).getClassLoader();
            }
            Object instance = moduleClassLoader.loadClass(className).newInstance();
            if (params != null) {
                for (ParamValueMetaData param : params) {
                    IntrospectionUtils.setProperty(instance, param.getParamName(), param.getParamValue());
                }
            }
            return instance;
        } catch (Throwable t) {
            throw new DeploymentUnitProcessingException(MESSAGES.failToCreateContainerComponentInstance(className), t);
        }
    }

    /**
     * Try to obtain the security domain configured in jboss-app.xml at the ear level if available
     *
     * @param deploymentUnit
     * @return
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


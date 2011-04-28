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
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletContext;

import org.apache.catalina.Host;
import org.apache.catalina.Loader;
import org.apache.catalina.Realm;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.startup.ContextConfig;
import org.jboss.as.ee.naming.NamespaceSelectorService;
import org.jboss.as.naming.context.NamespaceContextSelector;
import org.jboss.as.security.plugins.SecurityDomainContext;
import org.jboss.as.security.service.SecurityDomainService;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.web.NamingListener;
import org.jboss.as.web.WebSubsystemServices;
import org.jboss.as.web.deployment.component.ComponentInstantiator;
import org.jboss.as.web.security.JBossWebRealmService;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.modules.Module;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceBuilder.DependencyType;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistryException;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.security.SecurityConstants;
import org.jboss.security.SecurityUtil;
import org.jboss.vfs.VirtualFile;

/**
 * @author Emanuel Muckenhuber
 * @author Anil.Saldhana@redhat.com
 */
public class WarDeploymentProcessor implements DeploymentUnitProcessor {

    private final String defaultHost;

    public WarDeploymentProcessor(String defaultHost) {
        if (defaultHost == null) {
            throw new IllegalArgumentException("null default host");
        }
        this.defaultHost = defaultHost;
    }

    /** {@inheritDoc} */
    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final WarMetaData metaData = deploymentUnit.getAttachment(WarMetaData.ATTACHMENT_KEY);
        if (metaData == null) {
            return;
        }
        Collection<String> hostNames = null;
        if (metaData.getMergedJBossWebMetaData() != null) {
            hostNames = metaData.getMergedJBossWebMetaData().getVirtualHosts();
        }
        if (hostNames == null || hostNames.isEmpty()) {
            hostNames = Collections.singleton(defaultHost);
        }
        String hostName = hostNames.iterator().next();
        if (hostName == null) {
            throw new IllegalStateException("null host name");
        }
        processDeployment(hostName, metaData, deploymentUnit, phaseContext.getServiceTarget());
    }

    @Override
    public void undeploy(final DeploymentUnit context) {
    }

    protected void processDeployment(final String hostName, final WarMetaData warMetaData, final DeploymentUnit deploymentUnit,
            final ServiceTarget serviceTarget) throws DeploymentUnitProcessingException {
        final VirtualFile deploymentRoot = deploymentUnit.getAttachment(Attachments.DEPLOYMENT_ROOT).getRoot();
        final Module module = deploymentUnit.getAttachment(Attachments.MODULE);
        if (module == null) {
            throw new DeploymentUnitProcessingException("failed to resolve module for deployment " + deploymentRoot);
        }
        final ClassLoader classLoader = module.getClassLoader();
        final JBossWebMetaData metaData = warMetaData.getMergedJBossWebMetaData();

        // Create the context
        final StandardContext webContext = new StandardContext();
        final ContextConfig config = new JBossContextConfig(deploymentUnit);

        webContext.addInstanceListener(NamingListener.class.getName());

        // Set the deployment root
        try {
            webContext.setDocBase(deploymentRoot.getPhysicalFile().getAbsolutePath());
        } catch (IOException e) {
            throw new DeploymentUnitProcessingException(e);
        }
        webContext.addLifecycleListener(config);

        // Set the path name
        final String deploymentName = deploymentUnit.getName();
        String pathName = null;
        if (metaData.getContextRoot() == null) {
            pathName = "/" + deploymentUnit.getName().substring(0, deploymentUnit.getName().length() - 4);
        } else {
            pathName = metaData.getContextRoot();
            if ("/".equals(pathName)) {
                pathName = "";
            } else if (pathName.length() > 0 && pathName.charAt(0) != '/') {
                pathName = "/" + pathName;
            }
        }
        webContext.setPath(pathName);
        webContext.setIgnoreAnnotations(true);
        webContext.setCrossContext(!metaData.isDisableCrossContext());

        final WebInjectionContainer injectionContainer = new WebInjectionContainer(module.getClassLoader());

        final Map<String, ComponentInstantiator> components = deploymentUnit
                .getAttachment(WebAttachments.WEB_COMPONENT_INSTANTIATORS);
        if (components != null) {
            for (Map.Entry<String, ComponentInstantiator> entry : components.entrySet()) {
                injectionContainer.addInstantiator(entry.getKey(), entry.getValue());
            }
        }
        webContext.setInstanceManager(injectionContainer);

        final Loader loader = new WebCtxLoader(classLoader);
        webContext.setLoader(loader);

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
        if (metaDataSecurityDomain != null) {
            metaDataSecurityDomain = metaDataSecurityDomain.trim();
        }

        String securityDomain = metaDataSecurityDomain == null ? SecurityConstants.DEFAULT_APPLICATION_POLICY : SecurityUtil
                .unprefixSecurityDomain(metaDataSecurityDomain);
        Map<String, Set<String>> principalVersusRolesMap = metaData.getSecurityRoles().getPrincipalVersusRolesMap();

        // Setup an deployer configured ServletContext attributes
        final List<ServletContextAttribute> attributes = deploymentUnit.getAttachment(ServletContextAttribute.ATTACHMENT_KEY);
        if (attributes != null) {
            final ServletContext context = webContext.getServletContext();
            for (ServletContextAttribute attribute : attributes) {
                context.setAttribute(attribute.getName(), attribute.getValue());
            }
        }

        try {
            JBossWebRealmService realmService = new JBossWebRealmService(principalVersusRolesMap);
            ServiceBuilder<?> builder = serviceTarget.addService(WebSubsystemServices.JBOSS_WEB_REALM.append(securityDomain),
                    realmService);
            builder.addDependency(DependencyType.REQUIRED, SecurityDomainService.SERVICE_NAME.append(securityDomain),
                    SecurityDomainContext.class, realmService.getSecurityDomainContextInjector()).setInitialMode(Mode.ACTIVE)
                    .install();
            ServiceName namespaceSelectorServiceName = deploymentUnit.getServiceName().append(NamespaceSelectorService.NAME);
            WebDeploymentService webDeploymentService = new WebDeploymentService(webContext);
            builder = serviceTarget.addService(WebSubsystemServices.JBOSS_WEB.append(deploymentName), webDeploymentService);
            builder.addDependency(WebSubsystemServices.JBOSS_WEB_HOST.append(hostName), Host.class,
                    new WebContextInjector(webContext)).addDependencies(injectionContainer.getServiceNames());
            builder.addDependency(WebSubsystemServices.JBOSS_WEB_REALM.append(securityDomain), Realm.class,
                    webDeploymentService.getRealm());
            builder.addDependency(namespaceSelectorServiceName, NamespaceContextSelector.class,
                    webDeploymentService.getNamespaceSelector()).setInitialMode(Mode.ACTIVE);

            builder.addDependencies(deploymentUnit.getAttachmentList(Attachments.WEB_DEPENDENCIES));

            builder.install();

        } catch (ServiceRegistryException e) {
            throw new DeploymentUnitProcessingException("Failed to add JBoss web deployment service", e);
        }
    }

}

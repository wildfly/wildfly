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

import org.apache.catalina.Host;
import org.apache.catalina.Loader;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.startup.ContextConfig;
import org.apache.tomcat.InstanceManager;
import org.jboss.as.deployment.DeploymentPhases;
import org.jboss.as.deployment.attachment.VirtualFileAttachment;
import org.jboss.as.deployment.module.ModuleDeploymentProcessor;
import org.jboss.as.deployment.unit.DeploymentUnitContext;
import org.jboss.as.deployment.unit.DeploymentUnitProcessingException;
import org.jboss.as.deployment.unit.DeploymentUnitProcessor;
import org.jboss.as.web.WebSubsystemElement;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.modules.Module;
import org.jboss.msc.service.BatchBuilder;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.vfs.VirtualFile;

/**
 * @author Emanuel Muckenhuber
 */
public class WarDeploymentProcessor implements DeploymentUnitProcessor {

    /** The deployment processor priority. */
    public static final long PRIORITY = DeploymentPhases.INSTALL_SERVICES.plus(300L);
    private final String defaultHost;

    public WarDeploymentProcessor(String defaultHost) {
        if(defaultHost == null) {
            throw new IllegalArgumentException("null default host");
        }
        this.defaultHost = defaultHost;
    }

    /** {@inheritDoc} */
    public void processDeployment(final DeploymentUnitContext context) throws DeploymentUnitProcessingException {
        final WarMetaData metaData = context.getAttachment(WarMetaData.ATTACHMENT_KEY);
        if(metaData == null) {
            return;
        }
        Collection<String> hostNames = metaData.getMergedJBossWebMetaData().getVirtualHosts();
        if(hostNames == null || hostNames.isEmpty()) {
            hostNames = Collections.singleton(defaultHost);
        }
        for(final String hostName : hostNames) {
            if(hostName == null) {
                throw new IllegalStateException("null host name");
            }
            processDeployment(hostName, metaData, context);
        }
    }

    protected void processDeployment(final String hostName, final WarMetaData warMetaData, final DeploymentUnitContext context) throws DeploymentUnitProcessingException {
        final VirtualFile deploymentRoot = VirtualFileAttachment.getVirtualFileAttachment(context);
        final Module module = context.getAttachment(ModuleDeploymentProcessor.MODULE_ATTACHMENT_KEY);
        if (module == null) {
            throw new DeploymentUnitProcessingException("failed to resolve module for deployment " + deploymentRoot);
        }
        final ClassLoader classLoader = module.getClassLoader();

        // Create the context
        final StandardContext webContext = new StandardContext();
        final ContextConfig config = new JBossContextConfig(context);
        // Set the deployment root
        try {
            webContext.setDocBase(deploymentRoot.getPhysicalFile().getAbsolutePath());
        } catch (IOException e) {
            throw new DeploymentUnitProcessingException(e);
        }
        webContext.addLifecycleListener(config);

        //
        final String deploymentName = context.getName();
        String pathName = deploymentRoot.getName();
        if (pathName.equals("ROOT.war")) {
            pathName = "";
        } else {
            pathName = "/" + pathName.substring(0, pathName.length() - 4);
        }
        webContext.setPath(pathName);
        webContext.setIgnoreAnnotations(true);

        //
        final Loader loader = new WebCtxLoader(classLoader);
        final InstanceManager manager = new WebInjectionContainer(classLoader);
        webContext.setInstanceManager(manager);
        webContext.setLoader(loader);

        // Set the session cookies flag according to metadata
        final JBossWebMetaData metaData = warMetaData.getMergedJBossWebMetaData();
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

        // Add the context service
        final BatchBuilder builder = context.getBatchBuilder();
        builder.addService(WebSubsystemElement.JBOSS_WEB.append(deploymentName), new WebDeploymentService(webContext))
            .addDependency(WebSubsystemElement.JBOSS_WEB_HOST.append(hostName), Host.class, new WebContextInjector(webContext))
            .setInitialMode(Mode.IMMEDIATE);
    }

}

/*
 * JBoss, Home of Professional Open Source
 * Copyright 2018 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.wildfly.extension.classchange;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.fakereplace.api.ClassChangeAware;
import org.fakereplace.core.Fakereplace;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.ExplodedDeploymentMarker;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.msc.service.LifecycleEvent;
import org.jboss.msc.service.LifecycleListener;
import org.jboss.msc.service.ServiceController;
import org.jboss.vfs.VirtualFile;
import org.wildfly.extension.classchange.logging.ClassChangeMessages;

public class ClassChangeDeploymentUnitProcessor implements DeploymentUnitProcessor {

    private static final String META_INF = "META-INF/class-change.properties";
    private static final String WEB_INF = "WEB-INF/class-change.properties";
    private static final String[] LOCATIONS = {WEB_INF, META_INF};

    private static final String WEB_RESOURCES_DIR = "web.resources.dir";
    private static final String SRCS_DIR = "srcs.dir";
    private static final String CLASSES_DIR = "classes.dir";
    private static final String REMOTE_PASSWORD = "remote.password";

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        AbstractClassChangeSupport classChangeSupport = null;
        if (deploymentUnit.getParent() != null || deploymentUnit.getName().endsWith(".ear")) {
            //we don't support ear deployments, we probably will at some point in the future but at this stage it will likely just cause problems
            return;
        } else {
            boolean explodedDeployment = ExplodedDeploymentMarker.isExplodedDeployment(deploymentUnit);
            if (explodedDeployment) {
                classChangeSupport = new ExplodedDeploymentClassChangeSupport(deploymentUnit);
            } else {
                ArchiveClassChangeSupport archiveClassChangeSupport = new ArchiveClassChangeSupport(deploymentUnit);
                //archive support needs to hook into the deployment process, so it can updated the physical archive
                //on undeploy, otherwise changes are lost on reload/restart
                ServiceController<?> controller = deploymentUnit.getServiceRegistry().getRequiredService(deploymentUnit.getServiceName());
                controller.addListener(new LifecycleListener() {
                    @Override
                    public void handleEvent(ServiceController<?> controller, LifecycleEvent event) {
                        if (event == LifecycleEvent.DOWN) {
                            archiveClassChangeSupport.deploymentUnmounted();
                            controller.removeListener(this);
                        }
                    }
                });

                classChangeSupport = archiveClassChangeSupport;
            }
            deploymentUnit.putAttachment(ClassChangeAttachments.DEPLOYMENT_CLASS_CHANGE_SUPPORT, classChangeSupport);
            Fakereplace.addClassChangeAware(classChangeSupport);
        }
        if (classChangeSupport != null) {
            //check for a properties file
            final ResourceRoot root = deploymentUnit.getAttachment(Attachments.DEPLOYMENT_ROOT);
            for (String location : LOCATIONS) {
                VirtualFile file = root.getRoot().getChild(location);
                if (file.isFile()) {
                    Properties p = new Properties();
                    try (InputStream inStream = file.openStream()) {
                        p.load(inStream);
                        deploymentUnit.putAttachment(ClassChangeAttachments.PROPERTIES, p);
                        if (!p.containsKey(REMOTE_PASSWORD)) {
                            //we only use the local roots if the remote password has not been set
                            if (p.containsKey(CLASSES_DIR)) {
                                classChangeSupport.setExternalClassFileLocation(p.getProperty(CLASSES_DIR));
                            }
                            if (p.containsKey(SRCS_DIR)) {
                                classChangeSupport.setExternalSourceFileLocation(p.getProperty(SRCS_DIR));
                            }
                            if (p.containsKey(WEB_RESOURCES_DIR)) {
                                classChangeSupport.setExternalWebResourceLocation(p.getProperty(WEB_RESOURCES_DIR));
                            }
                        }
                    } catch (IOException e) {
                        throw ClassChangeMessages.ROOT_LOGGER.failedToRead(file, e);
                    }
                }
            }
        }
    }

    @Override
    public void undeploy(DeploymentUnit context) {
        DeploymentClassChangeSupport support = context.getAttachment(ClassChangeAttachments.DEPLOYMENT_CLASS_CHANGE_SUPPORT);
        if (support instanceof ClassChangeAware) {
            Fakereplace.removeClassChangeAware((ClassChangeAware) support);
        }
    }

}

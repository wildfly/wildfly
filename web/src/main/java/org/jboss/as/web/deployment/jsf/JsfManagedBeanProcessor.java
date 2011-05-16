/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.web.deployment.jsf;

import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ee.structure.DeploymentType;
import org.jboss.as.ee.structure.DeploymentTypeMarker;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.DeploymentUtils;
import org.jboss.as.server.deployment.annotation.CompositeIndex;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.as.web.deployment.WebAttachments;
import org.jboss.as.web.deployment.component.WebComponentDescription;
import org.jboss.as.web.deployment.component.WebComponentInstantiator;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.logging.Logger;
import org.jboss.metadata.parser.util.NoopXmlResolver;
import org.jboss.vfs.VirtualFile;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Sets up JSF managed beans as components using information in the annotations and
 *
 * @author Stuart Douglas
 */
public class JsfManagedBeanProcessor implements DeploymentUnitProcessor {

    public static final DotName MANAGED_BEAN_ANNOTATION = DotName.createSimple("javax.faces.bean.ManagedBean");

    private static final String[] FACES_CONFIG = {"WEB-INF/faces-config.xml", "META-INF/faces-config.xml"};


    private static final Logger log = Logger.getLogger("org.jboss.as.web.jsf");

    private static final String MANAGED_BEAN = "managed-bean";
    private static final String MANAGED_BEAN_CLASS = "managed-bean-class";

    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final CompositeIndex index = deploymentUnit.getAttachment(Attachments.COMPOSITE_ANNOTATION_INDEX);
        final EEModuleDescription moduleDescription = deploymentUnit.getAttachment(org.jboss.as.ee.component.Attachments.EE_MODULE_DESCRIPTION);
        if (index == null) {
            return;
        }
        if (!DeploymentTypeMarker.isType(DeploymentType.WAR, deploymentUnit)) {
            return;
        }
        final Set<String> managedBeanClasses = new HashSet<String>();
        handleAnnotations(index, managedBeanClasses);
        processXmlManagedBeans(deploymentUnit, managedBeanClasses);
        for (String managedBean : managedBeanClasses) {
            installManagedBeanComponent(managedBean, moduleDescription, deploymentUnit);
        }

    }

    /**
     * Parse the faces config files looking for managed bean classes. The parser is quite
     * simplistic as the only information we need is the managed-bean-class element
     *
     */
    private void processXmlManagedBeans(final DeploymentUnit deploymentUnit, final Set<String> managedBeanClasses) {
        final List<ResourceRoot> resourceRoots = DeploymentUtils.allResourceRoots(deploymentUnit);
        for (final ResourceRoot resourceRoot : resourceRoots) {
            for (final String file : FACES_CONFIG) {
                final VirtualFile root = resourceRoot.getRoot();
                final VirtualFile facesConfig = root.getChild(file);
                if (facesConfig.exists()) {
                    InputStream is = null;
                    try {
                        is = facesConfig.openStream();
                        final XMLInputFactory inputFactory = XMLInputFactory.newInstance();
                        inputFactory.setXMLResolver(NoopXmlResolver.create());
                        XMLStreamReader parser = inputFactory.createXMLStreamReader(is);
                        int indent = 0;
                        boolean managedBean = false;
                        boolean managedBeanClass = false;
                        while (true) {
                            int event = parser.next();
                            if (event == XMLStreamConstants.END_DOCUMENT) {
                                parser.close();
                                break;
                            }
                            if (event == XMLStreamConstants.START_ELEMENT) {
                                indent++;
                                if (indent == 2) {
                                    if (parser.getLocalName().equals(MANAGED_BEAN)) {
                                        managedBean = true;
                                    }
                                } else if (indent == 3 && managedBean) {
                                    if (parser.getLocalName().equals(MANAGED_BEAN_CLASS)) {
                                        managedBeanClass = true;
                                    }
                                }

                            } else if (event == XMLStreamConstants.END_ELEMENT) {
                                indent--;
                                managedBeanClass = false;
                                if (indent == 1) {
                                    managedBean = false;
                                }
                            } else if (managedBeanClass && event == XMLStreamConstants.CHARACTERS) {
                                managedBeanClasses.add(parser.getText());
                            }
                        }
                    } catch (Exception e) {
                        log.error("Failed to parse " + facesConfig + " injection into manage beans defined in this file will not be available");
                    } finally {
                        try {
                            if (is != null) {
                                is.close();
                            }
                        } catch (IOException e) {
                            // Ignore
                        }
                    }
                }

            }
        }
    }

    private void handleAnnotations(final CompositeIndex index, final Set<String> managedBeanClasses) throws DeploymentUnitProcessingException {
        final List<AnnotationInstance> annotations = index.getAnnotations(MANAGED_BEAN_ANNOTATION);
        if (annotations != null) {
            for (final AnnotationInstance annotation : annotations) {

                final AnnotationTarget target = annotation.target();
                if (target instanceof ClassInfo) {
                    final String className = ((ClassInfo) target).name().toString();
                    managedBeanClasses.add(className);
                } else {
                    throw new DeploymentUnitProcessingException("@ManagedBean can only be placed on a class");
                }
            }
        }
    }

    private void installManagedBeanComponent(String className, final EEModuleDescription moduleDescription, final DeploymentUnit deploymentUnit) {
        final ComponentDescription componentDescription = new WebComponentDescription(MANAGED_BEAN.toString() + "." + className, className, moduleDescription, deploymentUnit.getServiceName());
        moduleDescription.addComponent(componentDescription);
        deploymentUnit.getAttachment(WebAttachments.WEB_COMPONENT_INSTANTIATORS).put(componentDescription.getComponentClassName(), new WebComponentInstantiator(deploymentUnit, componentDescription));
    }

    @Override
    public void undeploy(final DeploymentUnit context) {

    }
}

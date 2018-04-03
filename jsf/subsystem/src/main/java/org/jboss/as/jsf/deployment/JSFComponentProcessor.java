/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012, Red Hat Inc., and individual contributors as indicated
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
package org.jboss.as.jsf.deployment;

import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.EEApplicationClasses;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ee.structure.DeploymentType;
import org.jboss.as.ee.structure.DeploymentTypeMarker;
import org.jboss.as.jsf.logging.JSFLogger;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.DeploymentUtils;
import org.jboss.as.server.deployment.annotation.CompositeIndex;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.as.web.common.WarMetaData;
import org.jboss.as.web.common.WebComponentDescription;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.metadata.javaee.spec.ParamValueMetaData;
import org.jboss.metadata.parser.util.NoopXMLResolver;
import org.jboss.metadata.web.spec.WebMetaData;
import org.jboss.modules.Module;
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
public class JSFComponentProcessor implements DeploymentUnitProcessor {

    public static final DotName MANAGED_BEAN_ANNOTATION = DotName.createSimple("javax.faces.bean.ManagedBean");

    private static final String WEB_INF_FACES_CONFIG = "WEB-INF/faces-config.xml";

    private static final String MANAGED_BEAN = "managed-bean";
    private static final String MANAGED_BEAN_CLASS = "managed-bean-class";
    private static final String LIFECYCLE = "lifecycle";
    private static final String PHASE_LISTENER = "phase-listener";

    private static final String CONFIG_FILES = "javax.faces.CONFIG_FILES";

    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final CompositeIndex index = deploymentUnit.getAttachment(Attachments.COMPOSITE_ANNOTATION_INDEX);
        final EEModuleDescription moduleDescription = deploymentUnit.getAttachment(org.jboss.as.ee.component.Attachments.EE_MODULE_DESCRIPTION);
        final EEApplicationClasses applicationClassesDescription = deploymentUnit.getAttachment(org.jboss.as.ee.component.Attachments.EE_APPLICATION_CLASSES_DESCRIPTION);
        final Module module = deploymentUnit.getAttachment(Attachments.MODULE);
        if(JsfVersionMarker.isJsfDisabled(deploymentUnit)) {
            return;
        }
        if (index == null) {
            return;
        }
        if (module == null) {
            return;
        }
        if (!DeploymentTypeMarker.isType(DeploymentType.WAR, deploymentUnit)) {
            return;
        }
        final Set<String> managedBeanClasses = new HashSet<String>();
        handleAnnotations(index, managedBeanClasses);
        processXmlManagedBeans(deploymentUnit, managedBeanClasses);
        processPhaseListeners(deploymentUnit, managedBeanClasses);
        for (String managedBean : managedBeanClasses) {
            //try and load the class, and skip the class if it cannot be loaded
            //this is not ideal, but we are not allowed to let the deployment
            //fail due to missing managed beans
            try {
                final Class<?> componentClass = module.getClassLoader().loadClass(managedBean);
                componentClass.getConstructor();
            } catch (ClassNotFoundException e) {
                JSFLogger.ROOT_LOGGER.managedBeanLoadFail(managedBean);
                continue;
            } catch (NoSuchMethodException e) {
                JSFLogger.ROOT_LOGGER.managedBeanNoDefaultConstructor(managedBean);
                continue;
            }
            installManagedBeanComponent(managedBean, moduleDescription, deploymentUnit, applicationClassesDescription);
        }

    }

    /**
     * Parse the faces config files looking for managed bean classes. The parser is quite
     * simplistic as the only information we need is the managed-bean-class element
     */
    private void processXmlManagedBeans(final DeploymentUnit deploymentUnit, final Set<String> managedBeanClasses) {
        for (final VirtualFile facesConfig : getConfigurationFiles(deploymentUnit)) {
            InputStream is = null;
            try {
                is = facesConfig.openStream();
                final XMLInputFactory inputFactory = XMLInputFactory.newInstance();
                inputFactory.setXMLResolver(NoopXMLResolver.create());
                XMLStreamReader parser = inputFactory.createXMLStreamReader(is);
                StringBuilder className = null;
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
                                className = new StringBuilder();
                            }
                        }

                    } else if (event == XMLStreamConstants.END_ELEMENT) {
                        indent--;
                        managedBeanClass = false;
                        if (indent == 1) {
                            managedBean = false;
                        }
                        if (className != null) {
                            managedBeanClasses.add(className.toString().trim());
                            className = null;
                        }
                    } else if (managedBeanClass && event == XMLStreamConstants.CHARACTERS) {
                        className.append(parser.getText());
                    }
                }
            } catch (Exception e) {
                JSFLogger.ROOT_LOGGER.managedBeansConfigParseFailed(facesConfig);
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

    /**
     * WFLY-6617
     * According to  JSF 2.2 spec, it should be possible to inject beans using @EJB annotation into
     * PhaseListeners.
     */
    private void processPhaseListeners(final DeploymentUnit deploymentUnit, final Set<String> managedBeanClasses) {
        for (final VirtualFile facesConfig : getConfigurationFiles(deploymentUnit)) {
            InputStream is = null;
            try {
                is = facesConfig.openStream();
                final XMLInputFactory inputFactory = XMLInputFactory.newInstance();
                inputFactory.setXMLResolver(NoopXMLResolver.create());
                XMLStreamReader parser = inputFactory.createXMLStreamReader(is);
                StringBuilder phaseListenerName = null;
                int indent = 0;
                boolean lifecycle = false;
                boolean phaseListener = false;
                while (true) {
                    int event = parser.next();
                    if (event == XMLStreamConstants.END_DOCUMENT) {
                        parser.close();
                        break;
                    }
                    if (event == XMLStreamConstants.START_ELEMENT) {
                        indent++;
                        if (indent == 2) {
                            if(parser.getLocalName().equals(LIFECYCLE)){
                                lifecycle = true;
                            }
                        } else if (indent == 3 && lifecycle) {
                            if(parser.getLocalName().equals(PHASE_LISTENER)){
                                phaseListener = true;
                                phaseListenerName = new StringBuilder();
                            }
                        }
                    } else if (event == XMLStreamConstants.END_ELEMENT) {
                        indent--;
                        phaseListener = false;
                        if (indent == 1) {
                            lifecycle = false;
                        }
                        if(phaseListenerName != null){
                            managedBeanClasses.add(phaseListenerName.toString().trim());
                            phaseListenerName = null;
                        }
                    } else if (phaseListener && event == XMLStreamConstants.CHARACTERS) {
                        phaseListenerName.append(parser.getText());
                    }
                }
            } catch (Exception e) {
                JSFLogger.ROOT_LOGGER.phaseListenersConfigParseFailed(facesConfig);
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

    public Set<VirtualFile> getConfigurationFiles(DeploymentUnit deploymentUnit) {
        final Set<VirtualFile> ret = new HashSet<VirtualFile>();
        final List<ResourceRoot> resourceRoots = DeploymentUtils.allResourceRoots(deploymentUnit);
        for (final ResourceRoot resourceRoot : resourceRoots) {
            final VirtualFile webInfFacesConfig = resourceRoot.getRoot().getChild(WEB_INF_FACES_CONFIG);
            if (webInfFacesConfig.exists()) {
                ret.add(webInfFacesConfig);
            }
            //look for files that end in .faces-config.xml
            final VirtualFile metaInf = resourceRoot.getRoot().getChild("META-INF");
            if (metaInf.exists() && metaInf.isDirectory()) {
                for (final VirtualFile file : metaInf.getChildren()) {
                    if (file.getName().equals("faces-config.xml") || file.getName().endsWith(".faces-config.xml")) {
                        ret.add(file);
                    }
                }
            }
        }
        String configFiles = null;
        //now look for files in the javax.faces.CONFIG_FILES context param
        final WarMetaData warMetaData = deploymentUnit.getAttachment(WarMetaData.ATTACHMENT_KEY);
        if (warMetaData != null) {
            final WebMetaData webMetaData = warMetaData.getWebMetaData();
            if (webMetaData != null) {
                final List<ParamValueMetaData> contextParams = webMetaData.getContextParams();
                if (contextParams != null) {
                    for (final ParamValueMetaData param : contextParams) {
                        if (param.getParamName().equals(CONFIG_FILES)) {
                            configFiles = param.getParamValue();
                            break;
                        }
                    }
                }
            }
        }
        if (configFiles != null) {
            final String[] files = configFiles.split(",");
            final ResourceRoot deploymentRoot = deploymentUnit.getAttachment(Attachments.DEPLOYMENT_ROOT);
            if (deploymentRoot != null) {
                for (final String file : files) {
                    final VirtualFile configFile = deploymentRoot.getRoot().getChild(file);
                    if (configFile.exists()) {
                        ret.add(configFile);
                    }
                }
            }
        }
        return ret;
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
                    throw new DeploymentUnitProcessingException(JSFLogger.ROOT_LOGGER.invalidManagedBeanAnnotation(target));
                }
            }
        }
    }

    private void installManagedBeanComponent(String className, final EEModuleDescription moduleDescription, final DeploymentUnit deploymentUnit, final EEApplicationClasses applicationClassesDescription) {
        final ComponentDescription componentDescription = new WebComponentDescription(MANAGED_BEAN.toString() + "." + className, className, moduleDescription, deploymentUnit.getServiceName(), applicationClassesDescription);
        moduleDescription.addComponent(componentDescription);
        deploymentUnit.addToAttachmentList(WebComponentDescription.WEB_COMPONENTS, componentDescription.getStartServiceName());
    }

    @Override
    public void undeploy(final DeploymentUnit context) {

    }
}

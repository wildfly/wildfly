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
import java.io.InputStream;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import static org.jboss.as.weld.Capabilities.WELD_CAPABILITY_NAME;
import org.jboss.as.weld.WeldCapability;

/**
 * Sets up Jakarta Server Faces managed beans as components using information in the annotations and
 *
 * @author Stuart Douglas
 */
public class JSFComponentProcessor implements DeploymentUnitProcessor {

    public static final DotName MANAGED_BEAN_ANNOTATION = DotName.createSimple("javax.faces.bean.ManagedBean");

    private static final String WEB_INF_FACES_CONFIG = "WEB-INF/faces-config.xml";

    private static final String CONFIG_FILES = "javax.faces.CONFIG_FILES";

    /**
     * Jakarta Server Faces tags that should be checked in the configuration file. All the
     * tags that are needed for injection are taken into account.
     * If more artifacts should be included just add it to the enum and to
     * the <em>facesConfigElement</em> tree in order to be parsed.
     */
    private enum JsfTag {
        FACES_CONFIG,
        FACTORY,
        APPLICATION_FACTORY,
        VISIT_CONTEXT_FACTORY,
        EXCEPTION_HANDLER_FACTORY,
        EXTERNAL_CONTEXT_FACTORY,
        FACES_CONTEXT_FACTORY,
        PARTIAL_VIEW_CONTEXT_FACTORY,
        LIFECYCLE_FACTORY,
        RENDER_KIT_FACTORY,
        VIEW_DECLARATION_LANGUAGE_FACTORY,
        FACELET_CACHE_FACTORY,
        TAG_HANDLER_DELEGATE_FACTORY,
        APPLICATION,
        EL_RESOLVER,
        RESOURCE_HANDLER,
        STATE_MANAGER,
        ACTION_LISTENER,
        NAVIGATION_HANDLER,
        VIEW_HANDLER,
        SYSTEM_EVENT_LISTENER,
        SYSTEM_EVENT_LISTENER_CLASS,
        LIFECYCLE,
        PHASE_LISTENER,
        MANAGED_BEAN,
        MANAGED_BEAN_CLASS;

        private String tagName;

        JsfTag() {
            tagName = this.name().toLowerCase().replaceAll("_", "-");
        }

        public String getTagName() {
            return tagName;
        }
    };

    /**
     * Helper tree class to save the XML tree structure of elements
     * to take into consideration for injection.
     */
    private static class JsfTree {
        private final JsfTag tag;
        private final Map<String, JsfTree> children;

        public JsfTree(JsfTag tag, JsfTree... children) {
            this.tag = tag;
            this.children = new HashMap<>();
            for (JsfTree c : children) {
                this.children.put(c.getTag().getTagName(), c);
            }
        }

        public JsfTag getTag() {
            return tag;
        }

        public JsfTree getChild(String name) {
            return children.get(name);
        }

        public boolean isLeaf() {
            return children.isEmpty();
        }
    }

    /**
     * Helper class to queue tags read from the Jakarta Server Faces XML configuration file. The
     * idea is saving the element which can be a known tree element or another
     * one that is not interested for injection.
     */
    private static class JsfElement {
        private final JsfTree tree;
        private final String tag;

        public JsfElement(JsfTree tree) {
            this.tree = tree;
            this.tag = null;
        }

        public JsfElement(String tag) {
            this.tree = null;
            this.tag = tag;
        }

        public String getTag() {
            return tag;
        }

        public JsfTree getTree() {
            return tree;
        }

        public boolean isTree() {
            return tree != null;
        }
    }

    /**
     * The element tree to parse the XML artifacts for injection.
     */
    private static final JsfTree facesConfigElement;

    static {
        // tree of Jakarta Server Faces artifact tags with order
        facesConfigElement = new JsfTree(JsfTag.FACES_CONFIG,
                new JsfTree(JsfTag.FACTORY,
                        new JsfTree(JsfTag.APPLICATION_FACTORY),
                        new JsfTree(JsfTag.VISIT_CONTEXT_FACTORY),
                        new JsfTree(JsfTag.EXCEPTION_HANDLER_FACTORY),
                        new JsfTree(JsfTag.EXTERNAL_CONTEXT_FACTORY),
                        new JsfTree(JsfTag.FACES_CONTEXT_FACTORY),
                        new JsfTree(JsfTag.PARTIAL_VIEW_CONTEXT_FACTORY),
                        new JsfTree(JsfTag.LIFECYCLE_FACTORY),
                        new JsfTree(JsfTag.RENDER_KIT_FACTORY),
                        new JsfTree(JsfTag.VIEW_DECLARATION_LANGUAGE_FACTORY),
                        new JsfTree(JsfTag.FACELET_CACHE_FACTORY),
                        new JsfTree(JsfTag.TAG_HANDLER_DELEGATE_FACTORY)),
                new JsfTree(JsfTag.APPLICATION,
                        new JsfTree(JsfTag.EL_RESOLVER),
                        new JsfTree(JsfTag.RESOURCE_HANDLER),
                        new JsfTree(JsfTag.STATE_MANAGER),
                        new JsfTree(JsfTag.ACTION_LISTENER),
                        new JsfTree(JsfTag.NAVIGATION_HANDLER),
                        new JsfTree(JsfTag.VIEW_HANDLER),
                        new JsfTree(JsfTag.SYSTEM_EVENT_LISTENER,
                                new JsfTree(JsfTag.SYSTEM_EVENT_LISTENER_CLASS))),
                new JsfTree(JsfTag.LIFECYCLE,
                        new JsfTree(JsfTag.PHASE_LISTENER)),
                new JsfTree(JsfTag.MANAGED_BEAN,
                        new JsfTree(JsfTag.MANAGED_BEAN_CLASS)));
    }

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
        for (String managedBean : managedBeanClasses) {
            installManagedBeanComponent(managedBean, moduleDescription, module, deploymentUnit, applicationClassesDescription);
        }
        // process all the other elements elegible for injection in the Jakarta Server Faces spec
        processJSFArtifactsForInjection(deploymentUnit, managedBeanClasses);
    }

    /**
     * According to Jakarta Server Faces specification there is a table of eligible components
     * for Jakarta Contexts and Dependency Injection (TABLE 5-3 Jakarta Server Faces Artifacts Eligible for Injection in chapter
     * 5.4.1 Jakarta Server Faces Managed Classes and Jakarta EE Annotations). This method parses
     * the faces-config configuration files and registers the classes.
     * The parser is quite simplistic. The tags are saved into a queue and
     * using the facesConfigElement tree it is known when a tag is one of the
     * classes to use for injection.
     */
    private void processJSFArtifactsForInjection(final DeploymentUnit deploymentUnit, final Set<String> managedBeanClasses) {
        final CapabilityServiceSupport support = deploymentUnit.getAttachment(Attachments.CAPABILITY_SERVICE_SUPPORT);
        boolean isCDI = support.hasCapability(WELD_CAPABILITY_NAME) &&
                support.getOptionalCapabilityRuntimeAPI(WELD_CAPABILITY_NAME, WeldCapability.class).get().isPartOfWeldDeployment(deploymentUnit);
        final EEApplicationClasses applicationClassesDescription = deploymentUnit.getAttachment(org.jboss.as.ee.component.Attachments.EE_APPLICATION_CLASSES_DESCRIPTION);
        final EEModuleDescription moduleDescription = deploymentUnit.getAttachment(org.jboss.as.ee.component.Attachments.EE_MODULE_DESCRIPTION);
        final Module module = deploymentUnit.getAttachment(Attachments.MODULE);
        JsfElement current = null;
        Deque<JsfElement> queue = new LinkedList<>();
        for (final VirtualFile facesConfig : getConfigurationFiles(deploymentUnit)) {
            try (InputStream is = facesConfig.openStream()) {
                final XMLInputFactory inputFactory = XMLInputFactory.newInstance();
                inputFactory.setXMLResolver(NoopXMLResolver.create());
                XMLStreamReader parser = inputFactory.createXMLStreamReader(is);
                boolean finished = false;
                while (!finished) {
                    int event = parser.next();
                    switch (event) {
                        case XMLStreamConstants.END_DOCUMENT:
                            finished = true;
                            parser.close();
                            break;
                        case XMLStreamConstants.START_ELEMENT:
                            String tagName = parser.getLocalName();
                            if (current == null) {
                                // first element => should be faces-context
                                if (tagName.equals(JsfTag.FACES_CONFIG.getTagName())) {
                                    current = new JsfElement(facesConfigElement);
                                } else {
                                    current = new JsfElement(tagName);
                                }
                            } else {
                                JsfTree child = current.isTree()? current.getTree().getChild(tagName) : null;
                                if (child != null && child.isLeaf()) {
                                    // leaf component => read the class and register the component
                                    String className = parser.getElementText().trim();
                                    if (!managedBeanClasses.contains(className)) {
                                        managedBeanClasses.add(className);
                                        if (child.getTag() == JsfTag.MANAGED_BEAN_CLASS) {
                                            installManagedBeanComponent(className, moduleDescription, module, deploymentUnit, applicationClassesDescription);
                                        } else {
                                            installJsfArtifactComponent(child.getTag().getTagName(), className, isCDI, moduleDescription, module, deploymentUnit, applicationClassesDescription);
                                        }
                                    }
                                } else if (child != null) {
                                    // non-leaf known element => advance into it
                                    queue.push(current);
                                    current = new JsfElement(child);
                                } else {
                                    // unknown element => just put it in the queue
                                    queue.push(current);
                                    current = new JsfElement(tagName);
                                }
                            }
                            break;
                        case XMLStreamConstants.END_ELEMENT:
                            // end of current element, just get the previous element from the queue
                            current = queue.isEmpty()? null : queue.pop();
                            break;
                    }
                }
            } catch (Exception e) {
                JSFLogger.ROOT_LOGGER.managedBeansConfigParseFailed(facesConfig);
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
                    if (!file.isEmpty()) {
                        final VirtualFile configFile = deploymentRoot.getRoot().getChild(file);
                        if (configFile.exists()) {
                            ret.add(configFile);
                        }
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

    private void installManagedBeanComponent(String className, final EEModuleDescription moduleDescription,
            final Module module, final DeploymentUnit deploymentUnit, final EEApplicationClasses applicationClassesDescription) {
        //try and load the class, and skip the class if it cannot be loaded
        //this is not ideal, but we are not allowed to let the deployment
        //fail due to missing managed beans
        try {
            final Class<?> componentClass = module.getClassLoader().loadClass(className);
            componentClass.getConstructor();
        } catch (ClassNotFoundException e) {
            JSFLogger.ROOT_LOGGER.managedBeanLoadFail(className);
            return;
        } catch (NoSuchMethodException e) {
            JSFLogger.ROOT_LOGGER.managedBeanNoDefaultConstructor(className);
            return;
        }
        install(JsfTag.MANAGED_BEAN.getTagName(), className, moduleDescription, module, deploymentUnit, applicationClassesDescription);
    }

    private void installJsfArtifactComponent(String type, String className, boolean isCDI, final EEModuleDescription moduleDescription,
            final Module module, final DeploymentUnit deploymentUnit, final EEApplicationClasses applicationClassesDescription) {
        try {
            final Class<?> componentClass = module.getClassLoader().loadClass(className);
            if (!isCDI) {
                // if not Jakarta Contexts and Dependency Injection the default constructor is compulsory to inject the artifact
                componentClass.getConstructor();
            }
        } catch (ClassNotFoundException e) {
            JSFLogger.ROOT_LOGGER.managedBeanLoadFail(className);
            return;
        } catch (NoSuchMethodException e) {
            JSFLogger.ROOT_LOGGER.jsfArtifactNoDefaultConstructor(type, className);
            return;
        }
        install(type, className, moduleDescription, module, deploymentUnit, applicationClassesDescription);
    }

    private void install(String type, String className, final EEModuleDescription moduleDescription, final Module module,
            final DeploymentUnit deploymentUnit, final EEApplicationClasses applicationClassesDescription) {
        final ComponentDescription componentDescription = new WebComponentDescription(type + "." + className, className, moduleDescription, deploymentUnit.getServiceName(), applicationClassesDescription);
        moduleDescription.addComponent(componentDescription);
        deploymentUnit.addToAttachmentList(WebComponentDescription.WEB_COMPONENTS, componentDescription.getStartServiceName());
    }

    @Override
    public void undeploy(final DeploymentUnit context) {

    }
}

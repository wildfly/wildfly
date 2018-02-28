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
package org.jboss.as.weld.deployment.processors;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.function.BiConsumer;

import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ee.weld.WeldDeploymentMarker;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.weld.deployment.BeanDeploymentArchiveImpl;
import org.jboss.as.weld.deployment.BeanDeploymentArchiveImpl.BeanArchiveType;
import org.jboss.as.weld.deployment.ExplicitBeanArchiveMetadata;
import org.jboss.as.weld.deployment.ExplicitBeanArchiveMetadataContainer;
import org.jboss.as.weld.deployment.PropertyReplacingBeansXmlParser;
import org.jboss.as.weld.deployment.WeldAttachments;
import org.jboss.as.weld.deployment.processors.UrlScanner.ClassFile;
import org.jboss.as.weld.discovery.AnnotationType;
import org.jboss.as.weld.logging.WeldLogger;
import org.jboss.as.weld.spi.ComponentSupport;
import org.jboss.as.weld.spi.ModuleServicesProvider;
import org.jboss.as.weld.util.Reflections;
import org.jboss.as.weld.util.ServiceLoaders;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.jboss.jandex.IndexReader;
import org.jboss.jandex.Indexer;
import org.jboss.modules.DependencySpec;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleDependencySpec;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.ModuleLoader;
import org.jboss.weld.bootstrap.api.Service;
import org.jboss.weld.bootstrap.spi.BeanDiscoveryMode;
import org.jboss.weld.bootstrap.spi.BeansXml;
import org.jboss.weld.xml.BeansXmlParser;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * Deployment processor that builds bean archives from external deployments.
 * <p/>
 * This is only run at the top level, as multiple sub deployments can reference the same
 * beans.xml information, so we have to iterate through all bean deployment archives in this processor, to prevent
 * beans.xml from being potentially parsed twice.
 * <p/>
 *
 * @author Stuart Douglas
 * @author Jozef Hartinger
 */
public class ExternalBeanArchiveProcessor implements DeploymentUnitProcessor {

    private static final String META_INF_BEANS_XML = "META-INF/beans.xml";

    private static final String META_INF_JANDEX_IDX = "META-INF/jandex.idx";

    private final String ALL_KNOWN_CLASSES = "ALL_KNOWN_CLASSES";
    private final String BEAN_CLASSES = "BEAN_CLASSES";

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();

        if (!WeldDeploymentMarker.isPartOfWeldDeployment(deploymentUnit)) {
            return;
        }

        if (deploymentUnit.getParent() != null) {
            return;
        }

        final Set<String> componentClassNames = new HashSet<>();
        final ServiceLoader<ComponentSupport> supportServices = ServiceLoader.load(ComponentSupport.class,
                WildFlySecurityManager.getClassLoaderPrivileged(ExternalBeanArchiveProcessor.class));

        final String beanArchiveIdPrefix = deploymentUnit.getName() + ".external.";

        // This set is used for external bean archives with annotated discovery mode
        final Set<AnnotationType> beanDefiningAnnotations = new HashSet<>(deploymentUnit.getAttachment(WeldAttachments.BEAN_DEFINING_ANNOTATIONS));

        final List<DeploymentUnit> deploymentUnits = new ArrayList<DeploymentUnit>();
        deploymentUnits.add(deploymentUnit);
        deploymentUnits.addAll(deploymentUnit.getAttachmentList(Attachments.SUB_DEPLOYMENTS));

        PropertyReplacingBeansXmlParser parser = new PropertyReplacingBeansXmlParser(deploymentUnit);

        final HashSet<URL> existing = new HashSet<URL>();

        for (DeploymentUnit deployment : deploymentUnits) {
            try {
                final ExplicitBeanArchiveMetadataContainer weldDeploymentMetadata = deployment.getAttachment(ExplicitBeanArchiveMetadataContainer.ATTACHMENT_KEY);
                if (weldDeploymentMetadata != null) {
                    for (ExplicitBeanArchiveMetadata md : weldDeploymentMetadata.getBeanArchiveMetadata().values()) {
                        existing.add(md.getBeansXmlFile().toURL());
                        if (md.getAdditionalBeansXmlFile() != null) {
                            existing.add(md.getAdditionalBeansXmlFile().toURL());
                        }
                    }
                }
            } catch (MalformedURLException e) {
                throw new DeploymentUnitProcessingException(e);
            }

            EEModuleDescription moduleDesc = deployment.getAttachment(org.jboss.as.ee.component.Attachments.EE_MODULE_DESCRIPTION);
            if(moduleDesc != null) {
                for(ComponentDescription component : moduleDesc.getComponentDescriptions()) {
                    for (ComponentSupport support : supportServices) {
                        if (!support.isDiscoveredExternalType(component)) {
                            componentClassNames.add(component.getComponentClassName());
                            break;
                        }
                    }
                }
            }
        }

        final ServiceLoader<ModuleServicesProvider> moduleServicesProviders = ServiceLoader.load(ModuleServicesProvider.class,
                WildFlySecurityManager.getClassLoaderPrivileged(WeldDeploymentProcessor.class));

        for (DeploymentUnit deployment : deploymentUnits) {
            final Module module = deployment.getAttachment(Attachments.MODULE);
            if (module == null) {
                return;
            }
            for (DependencySpec dep : module.getDependencies()) {
                final Module dependency = loadModuleDependency(dep);
                if (dependency == null) {
                    continue;
                }
                Map<URL, URL> resourcesMap = findExportedResources(dependency);
                if (!resourcesMap.isEmpty()) {
                    List<BeanDeploymentArchiveImpl> moduleBdas = new ArrayList<>();
                    for (Entry<URL,URL> entry : resourcesMap.entrySet()) {
                        URL beansXmlUrl = entry.getKey();
                        if (existing.contains(beansXmlUrl)) {
                            continue;
                        }
                        /*
                         * Workaround for http://java.net/jira/browse/JAVASERVERFACES-2837
                         */
                        if (beansXmlUrl.toString().contains("jsf-impl-2.2")) {
                            continue;
                        }
                        /*
                         * Workaround for resteasy-cdi bundling beans.xml
                         */
                        if (beansXmlUrl.toString().contains("resteasy-cdi")) {
                            continue;
                        }

                        WeldLogger.DEPLOYMENT_LOGGER.debugf("Found external beans.xml: %s", beansXmlUrl.toString());
                        final BeansXml beansXml = parseBeansXml(beansXmlUrl, parser, deploymentUnit);

                        if (BeanDiscoveryMode.NONE.equals(beansXml.getBeanDiscoveryMode())) {
                            // Scanning suppressed per spec
                            continue;
                        }

                        Map<String, List<String>> allAndBeanClasses = discover(beansXml.getBeanDiscoveryMode(), beansXmlUrl, entry.getValue(),
                                beanDefiningAnnotations);
                        Collection<String> discoveredBeanClasses = allAndBeanClasses.get(BEAN_CLASSES);
                        Collection<String> allKnownClasses = allAndBeanClasses.get(ALL_KNOWN_CLASSES);
                        if (discoveredBeanClasses == null) {
                            // URL scanner probably does not understand the protocol
                            continue;
                        }
                        discoveredBeanClasses.removeAll(componentClassNames);

                        final BeanDeploymentArchiveImpl bda = new BeanDeploymentArchiveImpl(new HashSet<String>(discoveredBeanClasses), new HashSet<String>(allKnownClasses), beansXml, dependency, beanArchiveIdPrefix + beansXmlUrl.toExternalForm(), BeanArchiveType.EXTERNAL);
                        WeldLogger.DEPLOYMENT_LOGGER.beanArchiveDiscovered(bda);

                        // Add module services to external bean deployment archive
                        for (Entry<Class<? extends Service>, Service> moduleService : ServiceLoaders
                                .loadModuleServices(moduleServicesProviders, deploymentUnit, deployment, module, null).entrySet()) {
                            bda.getServices().add(moduleService.getKey(), Reflections.cast(moduleService.getValue()));
                        }

                        deploymentUnit.addToAttachmentList(WeldAttachments.ADDITIONAL_BEAN_DEPLOYMENT_MODULES, bda);
                        moduleBdas.add(bda);

                        // make sure that if this beans.xml is seen by some other module, it is not processed twice
                        existing.add(beansXmlUrl);
                    }
                    //BDA's from inside the same module have visibility on each other
                    for(BeanDeploymentArchiveImpl i : moduleBdas) {
                        for(BeanDeploymentArchiveImpl j : moduleBdas) {
                            if(i != j) {
                                i.addBeanDeploymentArchive(j);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     *
     * @param beanDiscoveryMode
     * @param beansXmlUrl
     * @param indexUrl
     * @param beanDefiningAnnotations
     * @return the set of discovered bean classes or null if unable to handle the provided beans.xml url
     */
    private Map<String, List<String>> discover(BeanDiscoveryMode beanDiscoveryMode, URL beansXmlUrl, URL indexUrl, Set<AnnotationType> beanDefiningAnnotations) {
        List<String> discoveredBeanClasses = new ArrayList<String>();
        List<String> allKnownClasses = new ArrayList<String>();
        BiConsumer<String, ClassFile> consumer;

        if (BeanDiscoveryMode.ANNOTATED.equals(beanDiscoveryMode)) {
            // We must only consider types with bean defining annotations
            Index index = tryLoadIndex(indexUrl);
            if (index != null) {
                // Use the provided index to find ClassInfo
                consumer = (name, classFile) -> {
                    ClassInfo classInfo = index.getClassByName(DotName.createSimple(name));
                    allKnownClasses.add(name);
                    if (classInfo != null && hasBeanDefiningAnnotation(classInfo, beanDefiningAnnotations)) {
                        discoveredBeanClasses.add(name);
                    }
                };
            } else {
                // Build ClassInfo on the fly
                Indexer indexer = new Indexer();
                consumer = (name, classFile) -> {
                    try (InputStream in = classFile.openStream()) {
                        ClassInfo classInfo = indexer.index(in);
                        allKnownClasses.add(name);
                        if (classInfo != null && hasBeanDefiningAnnotation(classInfo, beanDefiningAnnotations)) {
                            discoveredBeanClasses.add(name);
                        }
                    } catch (IOException e) {
                        WeldLogger.DEPLOYMENT_LOGGER.cannotIndexClassName(name, beansXmlUrl);
                    }
                };
            }
        } else {
            // Bean discovery mode ALL
            consumer = (name, classFile) -> {
                allKnownClasses.add(name);
                discoveredBeanClasses.add(name);
            };
        }
        Map<String, List<String>> result = new HashMap<>();
        result.put(ALL_KNOWN_CLASSES, allKnownClasses);
        result.put(BEAN_CLASSES, discoveredBeanClasses);
        UrlScanner scanner = new UrlScanner(beansXmlUrl, consumer);
        return scanner.scan() ? result : null;
    }

    private Index tryLoadIndex(URL indexUrl) {
        if (indexUrl == null) {
            return null;
        }
        try (InputStream in = indexUrl.openStream()) {
            return new IndexReader(in).read();
        } catch (Exception e) {
            WeldLogger.DEPLOYMENT_LOGGER.cannotLoadAnnotationIndexOfExternalBeanArchive(indexUrl);
            return null;
        }
    }

    /**
     * The entry key is bean.xml URL, value is (optional) jandex index URL.
     *
     * @param dependencyModule
     * @return the map of exported resources
     */
    private Map<URL, URL> findExportedResources(Module dependencyModule) {
        Set<URL> beanXmls = findExportedResource(dependencyModule, META_INF_BEANS_XML);
        if (beanXmls.isEmpty()) {
            return Collections.emptyMap();
        }
        Set<URL> indexes = findExportedResource(dependencyModule, META_INF_JANDEX_IDX);
        Map<URL, URL> ret = new HashMap<>();
        for (URL beansXml : beanXmls) {
            String urlBase = beansXml.toString().substring(0, beansXml.toString().length() - META_INF_BEANS_XML.length());
            URL idx = null;
            for (URL index : indexes) {
                if (index.toString().startsWith(urlBase)) {
                    idx = index;
                    break;
                }
            }
            ret.put(beansXml, idx);
        }
        return ret;
    }

    private Set<URL> findExportedResource(Module dependencyModule, String name) {
        Enumeration<URL> exported = dependencyModule.getExportedResources(name);
        return new HashSet<>(Collections.list(exported));
    }

    private boolean hasBeanDefiningAnnotation(ClassInfo classInfo, Set<AnnotationType> beanDefiningAnnotations) {
        Map<DotName, List<AnnotationInstance>> annotationsMap = classInfo.annotations();
        for (AnnotationType beanDefiningAnnotation : beanDefiningAnnotations) {
            List<AnnotationInstance> annotations = annotationsMap.get(beanDefiningAnnotation.getName());
            if (annotations != null) {
                for (AnnotationInstance annotationInstance : annotations) {
                    if (annotationInstance.target().equals(classInfo)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private Module loadModuleDependency(DependencySpec dep) {
        if (dep instanceof ModuleDependencySpec) {
            ModuleDependencySpec dependency = (ModuleDependencySpec) dep;
            final ModuleLoader loader = dependency.getModuleLoader();
            if (loader != null) {
                try {
                    return dependency.getModuleLoader().loadModule(dependency.getIdentifier());
                } catch (ModuleLoadException e) {
                    return null;
                }
            }
        }
        return null;
    }

    @Override
    public void undeploy(DeploymentUnit context) {
    }

    private BeansXml parseBeansXml(URL beansXmlFile, BeansXmlParser parser, final DeploymentUnit deploymentUnit) throws DeploymentUnitProcessingException {
        return parser.parse(beansXmlFile);
    }
}

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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ee.weld.WeldDeploymentMarker;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.weld.logging.WeldLogger;
import org.jboss.as.weld.deployment.BeanDeploymentArchiveImpl;
import org.jboss.as.weld.deployment.BeanDeploymentArchiveImpl.BeanArchiveType;
import org.jboss.as.weld.deployment.ExplicitBeanArchiveMetadata;
import org.jboss.as.weld.deployment.ExplicitBeanArchiveMetadataContainer;
import org.jboss.as.weld.deployment.PropertyReplacingBeansXmlParser;
import org.jboss.as.weld.deployment.UrlScanner;
import org.jboss.as.weld.deployment.WeldAttachments;
import org.jboss.as.weld.services.bootstrap.WeldJaxwsInjectionServices;
import org.jboss.as.weld.services.bootstrap.WeldJpaInjectionServices;
import org.jboss.modules.DependencySpec;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleDependencySpec;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.ModuleLoader;
import org.jboss.modules.Resource;
import org.jboss.weld.bootstrap.spi.BeansXml;
import org.jboss.weld.injection.spi.JaxwsInjectionServices;
import org.jboss.weld.injection.spi.JpaInjectionServices;
import org.jboss.weld.xml.BeansXmlParser;

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

        final String beanArchiveIdPrefix = deploymentUnit.getName() + ".external.";

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
                    componentClassNames.add(component.getComponentClassName());
                }
            }
        }

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
                Set<URL> urls = findExportedLocalBeansXml(dependency);
                if (urls != null) {
                    List<BeanDeploymentArchiveImpl> moduleBdas = new ArrayList<>();
                    for (URL url : urls) {
                        if (existing.contains(url)) {
                            continue;
                        }
                        /*
                         * Workaround for http://java.net/jira/browse/JAVASERVERFACES-2837
                         */
                        if (url.toString().contains("jsf-impl-2.2")) {
                            continue;
                        }
                        /*
                         * Workaround for resteasy-cdi bundling beans.xml
                         */
                        if (url.toString().contains("resteasy-cdi")) {
                            continue;
                        }

                        WeldLogger.DEPLOYMENT_LOGGER.debugf("Found external beans.xml: %s", url.toString());
                        final BeansXml beansXml = parseBeansXml(url, parser, deploymentUnit);

                        final UrlScanner urlScanner = new UrlScanner();

                        final List<String> discoveredClasses = new ArrayList<String>();
                        if (!urlScanner.handleBeansXml(url, discoveredClasses)) {
                            continue;
                        }
                        discoveredClasses.removeAll(componentClassNames);

                        final BeanDeploymentArchiveImpl bda = new BeanDeploymentArchiveImpl(new HashSet<String>(discoveredClasses), beansXml, dependency, beanArchiveIdPrefix + url.toExternalForm(), BeanArchiveType.EXTERNAL);
                        WeldLogger.DEPLOYMENT_LOGGER.beanArchiveDiscovered(bda);

                        final JpaInjectionServices jpaInjectionServices = new WeldJpaInjectionServices(deploymentUnit);
                        final JaxwsInjectionServices jaxwsInjectionServices = new WeldJaxwsInjectionServices(deploymentUnit);
                        bda.getServices().add(JpaInjectionServices.class, jpaInjectionServices);
                        bda.getServices().add(JaxwsInjectionServices.class, jaxwsInjectionServices);
                        deploymentUnit.addToAttachmentList(WeldAttachments.ADDITIONAL_BEAN_DEPLOYMENT_MODULES, bda);
                        moduleBdas.add(bda);

                        // make sure that if this beans.xml is seen by some other module, it is not processed twice
                        existing.add(url);
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

    private Set<URL> findExportedLocalBeansXml(Module dependencyModule) {
        HashSet<URL> ret = new HashSet<>();
        Enumeration<URL> exported = dependencyModule.getExportedResources(META_INF_BEANS_XML);
        if (exported.hasMoreElements()) {
            Set<URL> exportedSet = new HashSet<>(Collections.list(exported));

            Collection<Resource> locals = dependencyModule.getClassLoader().loadResourceLocal(META_INF_BEANS_XML);
            if (!locals.isEmpty()) {
                for(Resource local: locals) {
                    URL url = local.getURL();
                    if (exportedSet.contains(url)) {
                        ret.add(url);
                    }
                }
                return ret;
            }
        }
        return null;
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

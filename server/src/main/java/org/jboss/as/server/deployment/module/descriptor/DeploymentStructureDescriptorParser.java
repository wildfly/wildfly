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
package org.jboss.as.server.deployment.module.descriptor;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.DeploymentUtils;
import org.jboss.as.server.deployment.module.AdditionalModuleSpecification;
import org.jboss.as.server.deployment.module.ModuleSpecification;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.as.server.moduleservice.ServiceModuleLoader;
import org.jboss.logging.Logger;
import org.jboss.modules.ModuleLoader;
import org.jboss.staxmapper.XMLMapper;
import org.jboss.vfs.VirtualFile;

/**
 * Parses <code>jboss-deployment-structure.xml</code>, and merges the result with the deployment.
 * <p/>
 * <code>jboss-deployment-structure.xml</code> is only parsed for top level deployments. It allows configuration of the following for
 * deployments and sub deployments:
 * <ul>
 * <li>Additional dependencies</li>
 * <li>Additional resource roots</li>
 * <li>{@link java.lang.instrument.ClassFileTransformer}s that will be applied at classloading</li>
 * <li>Child first behaviour</li>
 * </ul>
 * <p/>
 * It also allows for the use to add additional modules, using a syntax similar to that used in module xml files.
 *
 * @author Stuart Douglas
 * @author Marius Bogoevici
 */
public class DeploymentStructureDescriptorParser implements DeploymentUnitProcessor {

    private static final Logger log = Logger
            .getLogger("org.jboss.as.server.deployment.module.deployment-structure-descriptor-processor");

    public static final String[] DEPLOYMENT_STRUCTURE_DESCRIPTOR_LOCATIONS = {"META-INF/jboss-deployment-structure.xml",
            "WEB-INF/jboss-deployment-structure.xml"};


    private static final QName ROOT_1_0 = new QName(JBossDeploymentStructureParser10.NAMESPACE_1_0, "jboss-deployment-structure");
    private static final QName ROOT_1_1 = new QName(JBossDeploymentStructureParser11.NAMESPACE_1_1, "jboss-deployment-structure");
    private static final QName ROOT_NO_NAMESPACE = new QName("jboss-deployment-structure");


    private static final XMLInputFactory INPUT_FACTORY = XMLInputFactory.newInstance();

    private final XMLMapper mapper;

    public DeploymentStructureDescriptorParser() {
        mapper = XMLMapper.Factory.create();
        mapper.registerRootElement(ROOT_1_0, JBossDeploymentStructureParser10.INSTANCE);
        mapper.registerRootElement(ROOT_1_1, JBossDeploymentStructureParser11.INSTANCE);
        mapper.registerRootElement(ROOT_NO_NAMESPACE, JBossDeploymentStructureParser11.INSTANCE);
    }

    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final ResourceRoot resourceRoot = deploymentUnit.getAttachment(Attachments.DEPLOYMENT_ROOT);
        final ServiceModuleLoader moduleLoader = deploymentUnit.getAttachment(Attachments.SERVICE_MODULE_LOADER);

        VirtualFile deploymentFile = null;
        for (final String loc : DEPLOYMENT_STRUCTURE_DESCRIPTOR_LOCATIONS) {
            final VirtualFile file = resourceRoot.getRoot().getChild(loc);
            if (file.exists()) {
                deploymentFile = file;
                break;
            }
        }
        if (deploymentFile == null) {
            return;
        }
        if (deploymentUnit.getParent() != null) {
            log.warnf("%s in subdeployment ignored. jboss-deployment-structure.xml is only parsed for top level deployments.",
                    deploymentFile.getPathName());
            return;
        }

        try {
            final ParseResult result = parse(deploymentFile.getPhysicalFile(), deploymentUnit, moduleLoader);

            final ModuleSpecification moduleSpec = deploymentUnit.getAttachment(Attachments.MODULE_SPECIFICATION);
            if (result.getEarSubDeploymentsIsolated() != null) {
                // set the ear subdeployment isolation value overridden via the jboss-deployment-structure.xml
                moduleSpec.setSubDeploymentModulesIsolated(result.getEarSubDeploymentsIsolated());
            }
            // handle the the root deployment
            final ModuleStructureSpec rootDeploymentSpecification = result.getRootDeploymentSpecification();
            if (rootDeploymentSpecification != null) {
                final Map<VirtualFile, ResourceRoot> resourceRoots = resourceRoots(deploymentUnit);
                moduleSpec.addUserDependencies(rootDeploymentSpecification.getModuleDependencies());
                moduleSpec.addExclusions(rootDeploymentSpecification.getExclusions());
                moduleSpec.addAliases(rootDeploymentSpecification.getAliases());
                moduleSpec.addModuleSystemDependencies(rootDeploymentSpecification.getSytemDependencies());
                for (final ResourceRoot additionalResourceRoot : rootDeploymentSpecification.getResourceRoots()) {

                    final ResourceRoot existingRoot = resourceRoots.get(additionalResourceRoot.getRoot());
                    if (existingRoot != null) {
                        //we already have to the resource root
                        //so now we want to merge it
                        existingRoot.merge(additionalResourceRoot);
                    } else {
                        deploymentUnit.addToAttachmentList(Attachments.RESOURCE_ROOTS, additionalResourceRoot);
                    }
                }
                for (final String classFileTransformer : rootDeploymentSpecification.getClassFileTransformers()) {
                    moduleSpec.addClassFileTransformer(classFileTransformer);
                }
            }
            // handle sub deployments
            final List<DeploymentUnit> subDeployments = deploymentUnit.getAttachmentList(Attachments.SUB_DEPLOYMENTS);
            final Map<String, DeploymentUnit> subDeploymentMap = new HashMap<String, DeploymentUnit>();
            for (final DeploymentUnit subDeployment : subDeployments) {
                final ResourceRoot subDeploymentRoot = subDeployment.getAttachment(Attachments.DEPLOYMENT_ROOT);
                final String path = subDeploymentRoot.getRoot().getPathNameRelativeTo(resourceRoot.getRoot());
                subDeploymentMap.put(path, subDeployment);
            }

            for (final Entry<String, ModuleStructureSpec> subDeploymentResult : result.getSubDeploymentSpecifications().entrySet()) {
                final String path = subDeploymentResult.getKey();
                final ModuleStructureSpec spec = subDeploymentResult.getValue();
                if (!subDeploymentMap.containsKey(path)) {
                    throw subDeploymentNotFound(path, subDeploymentMap.keySet());
                }
                final DeploymentUnit subDeployment = subDeploymentMap.get(path);
                final ModuleSpecification subModuleSpec = subDeployment.getAttachment(Attachments.MODULE_SPECIFICATION);

                final Map<VirtualFile, ResourceRoot> resourceRoots = resourceRoots(subDeployment);
                subModuleSpec.addUserDependencies(spec.getModuleDependencies());
                subModuleSpec.addExclusions(spec.getExclusions());
                subModuleSpec.addAliases(spec.getAliases());
                subModuleSpec.addModuleSystemDependencies(spec.getSytemDependencies());
                for (final ResourceRoot additionalResourceRoot : spec.getResourceRoots()) {
                    final ResourceRoot existingRoot = resourceRoots.get(additionalResourceRoot.getRoot());
                    if (existingRoot != null) {
                        //we already have to the resource root
                        //so now we want to merge it
                        existingRoot.merge(additionalResourceRoot);
                    } else {
                        subDeployment.addToAttachmentList(Attachments.RESOURCE_ROOTS, additionalResourceRoot);
                    }
                }
                for (final String classFileTransformer : spec.getClassFileTransformers()) {
                    subModuleSpec.addClassFileTransformer(classFileTransformer);
                }
                subModuleSpec.setLocalLast(spec.isLocalLast());
            }

            // handle additional modules
            for (final ModuleStructureSpec additionalModule : result.getAdditionalModules()) {
                final AdditionalModuleSpecification additional = new AdditionalModuleSpecification(additionalModule
                        .getModuleIdentifier(),
                        additionalModule.getResourceRoots());
                additional.addAliases(additionalModule.getAliases());
                additional.addSystemDependencies(additionalModule.getModuleDependencies());
                deploymentUnit.addToAttachmentList(Attachments.ADDITIONAL_MODULES, additional);
            }

        } catch (IOException e) {
            throw new DeploymentUnitProcessingException(e);
        }
    }

    private Map<VirtualFile, ResourceRoot> resourceRoots(final DeploymentUnit deploymentUnit) {
        final Map<VirtualFile, ResourceRoot> resourceRoots = new HashMap<VirtualFile, ResourceRoot>();
        for (final ResourceRoot root : DeploymentUtils.allResourceRoots(deploymentUnit)) {
            resourceRoots.put(root.getRoot(), root);
        }
        return resourceRoots;
    }

    private DeploymentUnitProcessingException subDeploymentNotFound(final String path, final Collection<String> subDeployments) {
        final StringBuilder builder = new StringBuilder();
        builder.append("Sub deployment ");
        builder.append(path);
        builder.append(" in jboss-structure.xml was not found. Available sub deployments: ");
        for (final String dep : subDeployments) {
            builder.append(dep);
            builder.append(", ");
        }
        return new DeploymentUnitProcessingException(builder.toString());
    }

    @Override
    public void undeploy(final DeploymentUnit context) {

    }

    private ParseResult parse(final File file, final DeploymentUnit deploymentUnit, final ModuleLoader moduleLoader) throws DeploymentUnitProcessingException {
        final FileInputStream fis;
        try {
            fis = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            throw new DeploymentUnitProcessingException("No jboss-deployment-structure.xml file found at " + file);
        }
        try {
            return parse(fis, file, deploymentUnit, moduleLoader);
        } finally {
            safeClose(fis);
        }
    }

    private void setIfSupported(final XMLInputFactory inputFactory, final String property, final Object value) {
        if (inputFactory.isPropertySupported(property)) {
            inputFactory.setProperty(property, value);
        }
    }

    private ParseResult parse(final InputStream source, final File file, final DeploymentUnit deploymentUnit, final ModuleLoader moduleLoader)
            throws DeploymentUnitProcessingException {
        try {

            final XMLInputFactory inputFactory = INPUT_FACTORY;
            setIfSupported(inputFactory, XMLInputFactory.IS_VALIDATING, Boolean.FALSE);
            setIfSupported(inputFactory, XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
            final XMLStreamReader streamReader = inputFactory.createXMLStreamReader(source);
            try {
                final ParseResult result = new ParseResult(moduleLoader, deploymentUnit);
                mapper.parseDocument(result, streamReader);
                return result;
            } finally {
                safeClose(streamReader);
            }
        } catch (XMLStreamException e) {
            throw new DeploymentUnitProcessingException("Error loading jboss-structure.xml from " + file.getPath(), e);
        }
    }

    private static void safeClose(final Closeable closeable) {
        if (closeable != null)
            try {
                closeable.close();
            } catch (IOException e) {
                // ignore
            }
    }

    private static void safeClose(final XMLStreamReader streamReader) {
        if (streamReader != null)
            try {
                streamReader.close();
            } catch (XMLStreamException e) {
                // ignore
            }
    }


}

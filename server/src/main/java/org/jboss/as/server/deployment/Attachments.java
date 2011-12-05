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

package org.jboss.as.server.deployment;

import java.util.Set;
import java.util.jar.Manifest;

import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.server.deployment.annotation.AnnotationIndexProcessor;
import org.jboss.as.server.deployment.annotation.CompositeIndex;
import org.jboss.as.server.deployment.module.AdditionalModuleSpecification;
import org.jboss.as.server.deployment.module.ExtensionInfo;
import org.jboss.as.server.deployment.module.ExtensionListEntry;
import org.jboss.as.server.deployment.module.ModuleDependency;
import org.jboss.as.server.deployment.module.ModuleSpecification;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.as.server.deployment.reflect.DeploymentClassIndex;
import org.jboss.as.server.deployment.reflect.DeploymentReflectionIndex;
import org.jboss.as.server.deployment.reflect.ProxyMetadataSource;
import org.jboss.as.server.deployment.repository.api.ServerDeploymentRepository;
import org.jboss.as.server.moduleservice.ExternalModuleService;
import org.jboss.as.server.moduleservice.ServiceModuleLoader;
import org.jboss.jandex.Index;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.msc.service.ServiceName;
import org.jboss.vfs.VirtualFile;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class Attachments {

    //
    // GENERAL
    //
    /**
     * A list of service dependencies that must be satisfied before the next deployment phase can begin executing.
     */
    public static final AttachmentKey<AttachmentList<ServiceName>> NEXT_PHASE_DEPS = AttachmentKey.createList(ServiceName.class);
    /**
     * A list of service dependencies that must be satisfied before the next deployment phase can begin executing.
     */
    public static final AttachmentKey<AttachmentList<AttachableDependency>> NEXT_PHASE_ATTACHABLE_DEPS = AttachmentKey.createList(AttachableDependency.class);

    /**
     * The deployments runtime name
     *
     * @deprecated use {@link org.jboss.as.server.deployment.DeploymentUnit#getName()}
     */
    @Deprecated
    public static final AttachmentKey<String> RUNTIME_NAME = AttachmentKey.create(String.class);

    /**
     * The name that uniquely identifies the deployment to the management layer across the domain.
     */
    public static final AttachmentKey<String> MANAGEMENT_NAME = AttachmentKey.create(String.class);

    /**
     * The deployment contents
     */
    public static final AttachmentKey<VirtualFile> DEPLOYMENT_CONTENTS = AttachmentKey.create(VirtualFile.class);

    /**
     * The deployment hash
     */
    //public static final AttachmentKey<byte[]> DEPLOYMENT_HASH = AttachmentKey.create(byte[].class);

    /**
     * The special status listener attachment.
     */
    public static final AttachmentKey<AbstractDeploymentUnitService.DeploymentServiceListener> STATUS_LISTENER = AttachmentKey.create(AbstractDeploymentUnitService.DeploymentServiceListener.class);

    /**
     * This should be added as a listener to all non child services
     */
    public static final AttachmentKey<ServiceVerificationHandler> SERVICE_VERIFICATION_HANDLER = AttachmentKey.create(ServiceVerificationHandler.class);

    //
    // STRUCTURE
    //

    /**
     * The primary deployment root.
     */
    public static final AttachmentKey<ResourceRoot> DEPLOYMENT_ROOT = AttachmentKey.create(ResourceRoot.class);
    /**
     * Information used to build up the deployments Module
     */
    public static final AttachmentKey<ModuleSpecification> MODULE_SPECIFICATION = AttachmentKey.create(ModuleSpecification.class);
    /**
     * The additional resource roots of the deployment unit.
     */
    public static final AttachmentKey<AttachmentList<ResourceRoot>> RESOURCE_ROOTS = AttachmentKey.createList(ResourceRoot.class);
    /**
     * The MANIFEST.MF of the deployment unit.
     */
    public static final AttachmentKey<Manifest> MANIFEST = AttachmentKey.create(Manifest.class);

    /**
     *  A flag indicating whether the presence of a bundle manifest attributes should be ignored and a bundle not created
     */
    public static final AttachmentKey<Boolean> IGNORE_OSGI = AttachmentKey.create(Boolean.class);
    /**
     * Available when the deployment contains a valid OSGi manifest
     */
    public static final AttachmentKey<Manifest> OSGI_MANIFEST = AttachmentKey.create(Manifest.class);

    /**
     * Module identifiers for Class-Path information
     */
    public static final AttachmentKey<AttachmentList<ModuleIdentifier>> CLASS_PATH_ENTRIES = AttachmentKey.createList(ModuleIdentifier.class);

    /**
     * Resource roots for additional modules referenced via Class-Path
     */
    public static final AttachmentKey<AttachmentList<ResourceRoot>> CLASS_PATH_RESOURCE_ROOTS = AttachmentKey.createList(ResourceRoot.class);

    /**
     * The list of extensions given in the manifest and structure configurations.
     */
    public static final AttachmentKey<AttachmentList<ExtensionListEntry>> EXTENSION_LIST_ENTRIES = AttachmentKey.createList(ExtensionListEntry.class);
    /**
     * Information about extensions in a jar library deployment.
     */
    public static final AttachmentKey<ExtensionInfo> EXTENSION_INFORMATION = AttachmentKey.create(ExtensionInfo.class);

    /**
     * The server deployment repository
     */
    public static final AttachmentKey<ServerDeploymentRepository> SERVER_DEPLOYMENT_REPOSITORY = AttachmentKey.create(ServerDeploymentRepository.class);

    /**
     * An annotation index for a (@link ResourceRoot). This is attached to the {@link ResourceRoot}s of the deployment that contain
     * the annotations
     */
    public static final AttachmentKey<Index> ANNOTATION_INDEX = AttachmentKey.create(Index.class);

    /**
     * The composite annotation index for this deployment.
     */
    public static final AttachmentKey<CompositeIndex> COMPOSITE_ANNOTATION_INDEX = AttachmentKey.create(CompositeIndex.class);

    /**
     * Flag to indicate whether to compute the composite annotation index for this deployment.  Absence of this flag will
     * be cause the composite index to be attached.
     */
    public static final AttachmentKey<Boolean> COMPUTE_COMPOSITE_ANNOTATION_INDEX = AttachmentKey.create(Boolean.class);

    /**
     * An attachment that indicates if a {@link ResourceRoot} should be indexed by the {@link AnnotationIndexProcessor}. If this
     * is not present then the resource root is indexed by default.
     */
    public static final AttachmentKey<Boolean> INDEX_RESOURCE_ROOT = AttachmentKey.create(Boolean.class);

     /**
     * A list of paths within a root to ignore when indexing.
     */
    public static final AttachmentKey<AttachmentList<String>> INDEX_IGNORE_PATHS = AttachmentKey.createList(String.class);

    /**
     * Flag to determine whether to process the child annotation indexes as part of the parent deployment.
     * Ex.  An EAR deployment should not processes nested JAR index when checking for deployable annotations.
     * It should rely on the child actually being deployed.  WARs and RARs on the other hand should process all the
     * children as though the are all one index.
     */
    public static final AttachmentKey<Boolean> PROCESS_CHILD_ANNOTATION_INDEX = AttachmentKey.create(Boolean.class);

    /**
     * Sub deployment services
     */
    public static final AttachmentKey<AttachmentList<DeploymentUnit>> SUB_DEPLOYMENTS = AttachmentKey.createList(DeploymentUnit.class);
    /**
     * Additional modules attached to the top level deployment
     */
    public static final AttachmentKey<AttachmentList<AdditionalModuleSpecification>> ADDITIONAL_MODULES = AttachmentKey.createList(AdditionalModuleSpecification.class);

    public static final AttachmentKey<AttachmentList<ModuleIdentifier>> ADDITIONAL_ANNOTATION_INDEXES = AttachmentKey.createList(ModuleIdentifier.class);

    //
    // VALIDATE
    //

    //
    // PARSE
    //

    //
    // DEPENDENCIES
    //
    public static final AttachmentKey<AttachmentList<ModuleDependency>> MANIFEST_DEPENDENCIES = AttachmentKey.createList(ModuleDependency.class);

    //
    // CONFIGURE
    //
    /**
     * The module idetifier.
     */

    public static final AttachmentKey<ModuleIdentifier> MODULE_IDENTIFIER = AttachmentKey.create(ModuleIdentifier.class);

    //
    // MODULARIZE
    //

    /**
     * The module of this deployment unit.
     */
    public static final AttachmentKey<Module> MODULE = AttachmentKey.create(Module.class);

    /**
     * Information about a modules dependencies used to setup transitive deps
     */
    public static final AttachmentKey<AttachmentList<ModuleSpecification>> MODULE_DEPENDENCY_INFORMATION = AttachmentKey.createList(ModuleSpecification.class);

    /**
     * The module loader for the deployment
     */
    public static final AttachmentKey<ServiceModuleLoader> SERVICE_MODULE_LOADER  = AttachmentKey.create(ServiceModuleLoader.class);

    /**
     * The extenal module service
     */
    public static final AttachmentKey<ExternalModuleService> EXTERNAL_MODULE_SERVICE  = AttachmentKey.create(ExternalModuleService.class);

    /**
     * An index of {@link java.util.ServiceLoader}-type services in this deployment unit
     */
    public static final AttachmentKey<ServicesAttachment> SERVICES = AttachmentKey.create(ServicesAttachment.class);

    /**
     * Sub deployments that are visible from this deployments module loader, in the order they are accessible.
     *
     * This list includes the current deployment, which under normal circumstances will be the first item in the list
     */
    public static final AttachmentKey<AttachmentList<DeploymentUnit>> ACCESSIBLE_SUB_DEPLOYMENTS = AttachmentKey.createList(DeploymentUnit.class);

    //
    // POST_MODULE
    //

    //
    // INSTALL
    //

    /**
     * A list of services that a web deployment should have as dependencies.
     */
    public static final AttachmentKey<AttachmentList<ServiceName>> WEB_DEPENDENCIES = AttachmentKey.createList(ServiceName.class);

    /**
     * JNDI dependencies, only attached to the top level deployment
     */
    public static final AttachmentKey<Set<ServiceName>> JNDI_DEPENDENCIES = AttachmentKey.create(Set.class);

    /**
     * The reflection index for the deployment.
     */
    public static final AttachmentKey<DeploymentReflectionIndex> REFLECTION_INDEX = AttachmentKey.create(DeploymentReflectionIndex.class);

    /**
     * The class index for the deployment.
     */
    public static final AttachmentKey<DeploymentClassIndex> CLASS_INDEX = AttachmentKey.create(DeploymentClassIndex.class);
    /**
     * The reflection index used to generate jboss-invoation proxies
     */
    public static final AttachmentKey<ProxyMetadataSource> PROXY_REFLECTION_INDEX = AttachmentKey.create(ProxyMetadataSource.class);

    /**
     * Setup actions that must be run before running an arquillian test
     */
    public static final AttachmentKey<AttachmentList<SetupAction>> SETUP_ACTIONS = AttachmentKey.createList(SetupAction.class);

    //
    // CLEANUP
    //

    private Attachments() {
    }


}

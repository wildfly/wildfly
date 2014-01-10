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

import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.jar.Manifest;

import org.jboss.as.controller.ServiceVerificationHandler;
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
import org.jboss.as.server.moduleservice.ExternalModuleService;
import org.jboss.as.server.moduleservice.ServiceModuleLoader;
import org.jboss.as.server.services.security.AbstractVaultReader;
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
     * A set of subsystem names that should not be processed. Any subsystem whos name is in this list will not have
     * its deployment unit processors run.
     */
    public static final AttachmentKey<Set<String>> EXCLUDED_SUBSYSTEMS = AttachmentKey.create(Set.class);

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
     * This should be added as a listener to all non child services
     */
    public static final AttachmentKey<ServiceVerificationHandler> SERVICE_VERIFICATION_HANDLER = AttachmentKey.create(ServiceVerificationHandler.class);


    public static final AttachmentKey<Boolean> ALLOW_PHASE_RESTART = AttachmentKey.create(Boolean.class);
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
     * Available when the deployment contains a valid OSGi manifest
     */
    public static final AttachmentKey<Manifest> OSGI_MANIFEST = AttachmentKey.create(Manifest.class);

    /**
     * Module identifiers for Class-Path information
     */
    public static final AttachmentKey<AttachmentList<ModuleIdentifier>> CLASS_PATH_ENTRIES = AttachmentKey.createList(ModuleIdentifier.class);

    /**
     * Resource roots for additional modules referenced via Class-Path.
     *
     * These are attached to the resource root that actually defined the class path entry, and are used to transitively resolve
     * the annotation index for class path items.
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
    public static final AttachmentKey<DeploymentMountProvider> SERVER_DEPLOYMENT_REPOSITORY = AttachmentKey.create(DeploymentMountProvider.class);

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
     * An attachment that indicates if a {@link ResourceRoot} should be indexed by the {@link org.jboss.as.server.deployment.annotation.AnnotationIndexProcessor}. If this
     * is not present then the resource root is indexed by default.
     */
    public static final AttachmentKey<Boolean> INDEX_RESOURCE_ROOT = AttachmentKey.create(Boolean.class);

     /**
     * A list of paths within a root to ignore when indexing.
     */
    public static final AttachmentKey<AttachmentList<String>> INDEX_IGNORE_PATHS = AttachmentKey.createList(String.class);

    /**
     * Sub deployment services
     */
    public static final AttachmentKey<AttachmentList<DeploymentUnit>> SUB_DEPLOYMENTS = AttachmentKey.createList(DeploymentUnit.class);
    /**
     * Additional modules attached to the top level deployment
     */
    public static final AttachmentKey<AttachmentList<AdditionalModuleSpecification>> ADDITIONAL_MODULES = AttachmentKey.createList(AdditionalModuleSpecification.class);

    public static final AttachmentKey<AttachmentList<ModuleIdentifier>> ADDITIONAL_ANNOTATION_INDEXES = AttachmentKey.createList(ModuleIdentifier.class);

    public static final AttachmentKey<Map<ModuleIdentifier, CompositeIndex>> ADDITIONAL_ANNOTATION_INDEXES_BY_MODULE = AttachmentKey.create(Map.class);

    public static final AttachmentKey<AttachmentList<VirtualFile>> DEPLOYMENT_OVERRIDE_LOCATIONS = AttachmentKey.createList(VirtualFile.class);

    //
    // VALIDATE
    //

    //
    // PARSE
    //

    public static final AttachmentKey<AbstractVaultReader> VAULT_READER_ATTACHMENT_KEY = AttachmentKey.create(AbstractVaultReader.class);

    //
    // REGISTER
    //

    public static final AttachmentKey<BundleState> BUNDLE_STATE_KEY = AttachmentKey.create(BundleState.class);

    //
    // DEPENDENCIES
    //
    public static final AttachmentKey<AttachmentList<ModuleDependency>> MANIFEST_DEPENDENCIES = AttachmentKey.createList(ModuleDependency.class);

    //
    // CONFIGURE
    //
    /**
     * The module identifier.
     */
    public static final AttachmentKey<ModuleIdentifier> MODULE_IDENTIFIER = AttachmentKey.create(ModuleIdentifier.class);

    /**
     * Flags for deferred module phase handling.
     */
    public static final AttachmentKey<AttachmentList<String>> DEFERRED_MODULES = AttachmentKey.createList(String.class);
    public static final AttachmentKey<AtomicInteger> DEFERRED_ACTIVATION_COUNT = AttachmentKey.create(AtomicInteger.class);

    //
    // MODULARIZE
    //

    /**
     * The module of this deployment unit.
     */
    public static final AttachmentKey<Module> MODULE = AttachmentKey.create(Module.class);

    /**
     * The module loader for the deployment
     */
    public static final AttachmentKey<ServiceModuleLoader> SERVICE_MODULE_LOADER  = AttachmentKey.create(ServiceModuleLoader.class);

    /**
     * The external module service
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
    public static final AttachmentKey<AttachmentList<ServiceName>> JNDI_DEPENDENCIES = AttachmentKey.createList(ServiceName.class);

    /**
     * The reflection index for the deployment.
     */
    public static final AttachmentKey<DeploymentReflectionIndex> REFLECTION_INDEX = AttachmentKey.create(DeploymentReflectionIndex.class);

    /**
     * The class index for the deployment.
     */
    public static final AttachmentKey<DeploymentClassIndex> CLASS_INDEX = AttachmentKey.create(DeploymentClassIndex.class);
    /**
     * The reflection index used to generate jboss-invocation proxies
     */
    public static final AttachmentKey<ProxyMetadataSource> PROXY_REFLECTION_INDEX = AttachmentKey.create(ProxyMetadataSource.class);

    /**
     * Setup actions that must be run before running an arquillian test
     */
    public static final AttachmentKey<AttachmentList<SetupAction>> SETUP_ACTIONS = AttachmentKey.createList(SetupAction.class);

    /**
     * Attachment key for services that the {@link BundleActivateService} depends on.
     */
    public static final AttachmentKey<AttachmentList<ServiceName>> BUNDLE_ACTIVE_DEPENDENCIES = AttachmentKey.createList(ServiceName.class);

    /**
     * List of services that need to be up before we consider this deployment 'done'. This is used to manage initialize-in-order,
     * and inter deployment dependencies.
     *
     * It would better if this could be handled by MSC without needing to add all these into a list manually
     */
    public static final AttachmentKey<AttachmentList<ServiceName>> DEPLOYMENT_COMPLETE_SERVICES = AttachmentKey.createList(ServiceName.class);

    //
    // CLEANUP
    //

    private Attachments() {
    }

    /**
     * The state of an OSGi bundle deployment
     */
    public static enum BundleState {
        INSTALLED,
        RESOLVED,
        ACTIVE,
        UNINSTALLED
    }
}

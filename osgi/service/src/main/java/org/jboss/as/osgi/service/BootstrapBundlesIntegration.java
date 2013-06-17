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

package org.jboss.as.osgi.service;

import static org.jboss.as.osgi.OSGiLogger.LOGGER;
import static org.jboss.as.osgi.OSGiMessages.MESSAGES;
import static org.jboss.osgi.framework.spi.IntegrationConstants.STORAGE_STATE_KEY;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.jboss.as.osgi.OSGiConstants;
import org.jboss.as.osgi.parser.SubsystemState;
import org.jboss.as.osgi.parser.SubsystemState.OSGiCapability;
import org.jboss.as.server.ServerEnvironment;
import org.jboss.as.server.ServerEnvironmentService;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleClassLoader;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.ModuleLoader;
import org.jboss.modules.Resource;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.value.InjectedValue;
import org.jboss.osgi.deployment.deployer.Deployment;
import org.jboss.osgi.deployment.deployer.DeploymentFactory;
import org.jboss.osgi.framework.Services;
import org.jboss.osgi.framework.spi.AbstractBundleRevisionAdaptor;
import org.jboss.osgi.framework.spi.BootstrapBundlesActivate;
import org.jboss.osgi.framework.spi.BootstrapBundlesInstall;
import org.jboss.osgi.framework.spi.BootstrapBundlesResolve;
import org.jboss.osgi.framework.spi.BundleManager;
import org.jboss.osgi.framework.spi.IntegrationConstants;
import org.jboss.osgi.framework.spi.IntegrationServices;
import org.jboss.osgi.framework.spi.StorageManager;
import org.jboss.osgi.framework.spi.StorageState;
import org.jboss.osgi.metadata.OSGiManifestBuilder;
import org.jboss.osgi.metadata.OSGiMetaData;
import org.jboss.osgi.metadata.OSGiMetaDataBuilder;
import org.jboss.osgi.repository.XRepository;
import org.jboss.osgi.resolver.MavenCoordinates;
import org.jboss.osgi.resolver.XBundle;
import org.jboss.osgi.resolver.XBundleRevision;
import org.jboss.osgi.resolver.XBundleRevisionBuilderFactory;
import org.jboss.osgi.resolver.XCapability;
import org.jboss.osgi.resolver.XEnvironment;
import org.jboss.osgi.resolver.XRequirementBuilder;
import org.jboss.osgi.resolver.XResource;
import org.jboss.osgi.resolver.XResourceBuilder;
import org.jboss.osgi.spi.BundleInfo;
import org.jboss.osgi.vfs.AbstractVFS;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.startlevel.BundleStartLevel;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.service.repository.ContentNamespace;

/**
 * An {@link org.jboss.osgi.framework.spi.IntegrationService} to install the configured capability bundles.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 11-Sep-2010
 */
class BootstrapBundlesIntegration extends BootstrapBundlesInstall<Void> {

    private final InjectedValue<BundleManager> injectedBundleManager = new InjectedValue<BundleManager>();
    private final InjectedValue<StorageManager> injectedStorageManager = new InjectedValue<StorageManager>();
    private final InjectedValue<ServerEnvironment> injectedServerEnvironment = new InjectedValue<ServerEnvironment>();
    private final InjectedValue<BundleContext> injectedSystemContext = new InjectedValue<BundleContext>();
    private final InjectedValue<SubsystemState> injectedSubsystemState = new InjectedValue<SubsystemState>();
    private final InjectedValue<XEnvironment> injectedEnvironment = new InjectedValue<XEnvironment>();
    private final InjectedValue<XRepository> injectedRepository = new InjectedValue<XRepository>();
    private List<OSGiCapability> modulecaps;
    private List<File> bundlesPath;

    BootstrapBundlesIntegration() {
        super(IntegrationServices.BOOTSTRAP_BUNDLES);
    }

    @Override
    protected void addServiceDependencies(ServiceBuilder<Void> builder) {
        super.addServiceDependencies(builder);
        builder.addDependency(OSGiConstants.SUBSYSTEM_STATE_SERVICE_NAME, SubsystemState.class, injectedSubsystemState);
        builder.addDependency(ServerEnvironmentService.SERVICE_NAME, ServerEnvironment.class, injectedServerEnvironment);
        builder.addDependency(RepositoryService.SERVICE_NAME, XRepository.class, injectedRepository);
        builder.addDependency(Services.BUNDLE_MANAGER, BundleManager.class, injectedBundleManager);
        builder.addDependency(Services.FRAMEWORK_CREATE, BundleContext.class, injectedSystemContext);
        builder.addDependency(Services.ENVIRONMENT, XEnvironment.class, injectedEnvironment);
        builder.addDependency(IntegrationServices.STORAGE_MANAGER_PLUGIN, StorageManager.class, injectedStorageManager);
    }

    @Override
    public synchronized void start(StartContext context) throws StartException {
        List<Deployment> deployments = new ArrayList<Deployment>();
        ServerEnvironment serverEnvironment = injectedServerEnvironment.getValue();
        bundlesPath = LayeredBundlePathFactory.resolveLayeredBundlePath(serverEnvironment);

        modulecaps = new ArrayList<OSGiCapability>();

        List<OSGiCapability> configcaps = new ArrayList<OSGiCapability>();
        for (String capspec : SystemPackagesIntegration.DEFAULT_CAPABILITIES) {
            configcaps.add(new OSGiCapability(capspec, null));
        }
        configcaps.addAll(injectedSubsystemState.getValue().getCapabilities());
        Iterator<OSGiCapability> iterator = configcaps.iterator();
        while (iterator.hasNext()) {
            OSGiCapability configcap = iterator.next();
            String identifier = configcap.getIdentifier();
            if (isValidModuleIdentifier(identifier)) {
                try {
                    Module module = installInitialModuleCapability(configcap);
                    if (module != null) {
                        modulecaps.add(configcap);
                        iterator.remove();
                    }
                } catch (Exception ex) {
                    throw MESSAGES.startFailedToProcessInitialCapability(ex, identifier);
                }
            }
        }

        for (OSGiCapability configcap : configcaps) {
            String identifier = configcap.getIdentifier();
            try {
                Deployment dep = getInitialBundleDeployment(configcap);
                deployments.add(dep);
            } catch (Exception ex) {
                throw MESSAGES.startFailedToProcessInitialCapability(ex, identifier);
            }
        }

        // Install the bundles from the given locations
        installBootstrapBundles(context.getChildTarget(), deployments);
    }

    @Override
    protected ServiceController<Void> installResolveService(ServiceTarget serviceTarget, Set<XBundleRevision> installedRevisions) {
        return new BootstrapResolveIntegration(getServiceName().getParent(), installedRevisions).install(serviceTarget, getServiceListener());
    }

    private Module installInitialModuleCapability(OSGiCapability configcap) throws Exception {
        String identifier = configcap.getIdentifier();
        ModuleIdentifier moduleId = ModuleIdentifier.fromString(identifier);

        // Find the module in the bundles hierarchy
        File bundleFile = ModuleIdentityRepository.getRepositoryEntry(bundlesPath, moduleId);
        if (bundleFile != null)
            return null;

        LOGGER.tracef("Installing initial module capability: %s", identifier);

        // Attempt to load the module from the modules hierarchy
        final Module module;
        try {
            ModuleLoader moduleLoader = Module.getBootModuleLoader();
            module = moduleLoader.loadModule(moduleId);
        } catch (ModuleLoadException ex) {
            throw MESSAGES.cannotResolveInitialCapability(ex, identifier);
        }

        final OSGiMetaData metadata = getModuleMetadata(module);
        final BundleContext syscontext = injectedSystemContext.getValue();
        XBundleRevisionBuilderFactory factory = new XBundleRevisionBuilderFactory() {
            @Override
            public XBundleRevision createResource() {
                return new AbstractBundleRevisionAdaptor(syscontext, module);
            }
        };
        XBundleRevision brev;
        XResourceBuilder<XBundleRevision> builder = XBundleRevisionBuilderFactory.create(factory);
        if (metadata != null) {
            builder.loadFrom(metadata);
            brev = builder.getResource();
            brev.putAttachment(IntegrationConstants.OSGI_METADATA_KEY, metadata);
        } else {
            builder.loadFrom(module);
            brev = builder.getResource();
        }
        injectedEnvironment.getValue().installResources(brev);

        // Set the start level of the adapted bundle
        Integer startlevel = configcap.getStartLevel();
        if (startlevel != null && startlevel > 0) {
            XBundle bundle = brev.getBundle();
            BundleStartLevel bundleStartLevel = bundle.adapt(BundleStartLevel.class);
            bundleStartLevel.setStartLevel(startlevel);
        }

        return module;
    }

    private Deployment getInitialBundleDeployment(OSGiCapability configcap) throws Exception {
        String identifier = configcap.getIdentifier();
        Integer level = configcap.getStartLevel();

        Deployment deployment = null;

        // Try the identifier as ModuleIdentifier
        if (isValidModuleIdentifier(identifier)) {
            ModuleIdentifier moduleId = ModuleIdentifier.fromString(identifier);

            // Attempt to install the bundle from the bundles hierarchy
            File bundleFile = ModuleIdentityRepository.getRepositoryEntry(bundlesPath, moduleId);
            if (bundleFile != null) {
                LOGGER.tracef("Installing initial bundle capability: %s", identifier);
                URL bundleURL = bundleFile.toURI().toURL();
                deployment = getDeploymentFromURL(bundleURL, identifier, level);
            }
        }

        // Try the identifier as MavenCoordinates
        else if (isValidMavenIdentifier(identifier)) {
            LOGGER.tracef("Installing initial maven capability: %s", identifier);
            XRepository repository = injectedRepository.getValue();
            MavenCoordinates mavenId = MavenCoordinates.parse(identifier);
            Requirement req = XRequirementBuilder.create(mavenId).getRequirement();
            Collection<Capability> caps = repository.findProviders(req);
            if (caps.isEmpty() == false) {
                XResource resource = (XResource) caps.iterator().next().getResource();
                XCapability ccap = (XCapability) resource.getCapabilities(ContentNamespace.CONTENT_NAMESPACE).get(0);
                URL bundleURL = new URL((String) ccap.getAttribute(ContentNamespace.CAPABILITY_URL_ATTRIBUTE));
                deployment = getDeploymentFromURL(bundleURL, identifier, level);
            }
        }

        if (deployment == null)
            throw MESSAGES.cannotResolveInitialCapability(null, identifier);

        return deployment;
    }

    private boolean isValidModuleIdentifier(String identifier) {
        try {
            ModuleIdentifier.fromString(identifier);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private boolean isValidMavenIdentifier(String identifier) {
        try {
            MavenCoordinates.parse(identifier);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private Deployment getDeploymentFromURL(URL bundleURL, String location, Integer level) throws Exception {
        BundleInfo info = BundleInfo.createBundleInfo(AbstractVFS.toVirtualFile(bundleURL), location);
        Deployment dep = DeploymentFactory.createDeployment(info);
        int startlevel = level != null ? level.intValue() : 0;
        if (startlevel > 0) {
            dep.setStartLevel(level.intValue());
            dep.setAutoStart(true);
        }
        StorageManager storageManager = injectedStorageManager.getValue();
        Long bundleId = injectedEnvironment.getValue().nextResourceIdentifier(null, dep.getSymbolicName());
        StorageState storageState = storageManager.createStorageState(bundleId, location, startlevel, null);
        dep.putAttachment(STORAGE_STATE_KEY, storageState);
        return dep;
    }

    private OSGiMetaData getModuleMetadata(Module module) throws Exception {

        Manifest manifest = null;
        ModuleIdentifier identifier = module.getIdentifier();

        // Iterate over the local resources in the META-INF dir
        ModuleClassLoader moduleClassLoader = module.getClassLoader();
        Iterator<Resource> itres = moduleClassLoader.iterateResources("/META-INF", false);
        while (itres.hasNext()) {
            Resource res = itres.next();
            if (res.getName().equals(JarFile.MANIFEST_NAME)) {
                if (manifest != null) {
                    LOGGER.debugf("Cannot process multiple manifest entries for: %s", identifier);
                    manifest = null;
                    break;
                }
                InputStream input = res.openStream();
                try {
                    manifest = new Manifest(input);
                } finally {
                    input.close();
                }
            }
        }

        if (OSGiManifestBuilder.isValidBundleManifest(manifest))
            return OSGiMetaDataBuilder.load(manifest);

        // Check for a jbosgi-xservice.properties in the root of a module directory under $JBOSS_HOME/modules/system/layers/base
        FileSystem fileSystem = FileSystems.getDefault();
        File homeDir = injectedServerEnvironment.getValue().getHomeDir();
        Path modulesPath = fileSystem.getPath(homeDir.getAbsolutePath(), "modules", "system", "layers", "base");

        String identifierPath = identifier.getName().replace('.', File.separatorChar) + File.separator + identifier.getSlot();
        File entryFile = fileSystem.getPath(modulesPath.toString(), identifierPath, "jbosgi-xservice.properties").toFile();
        if (entryFile.exists()) {
            FileInputStream input = new FileInputStream(entryFile);
            try {
                Properties props = new Properties();
                props.load(input);
                return OSGiMetaDataBuilder.load(props);
            } finally {
                input.close();
            }
        }

        return null;
    }

    class BootstrapResolveIntegration extends BootstrapBundlesResolve<Void> {

        BootstrapResolveIntegration(ServiceName baseName, Set<XBundleRevision> installedRevisions) {
            super(baseName, installedRevisions);
        }

        @Override
        protected ServiceController<Void> installActivateService(ServiceTarget serviceTarget, Set<XBundle> resolvedBundles) {
            return new BootstrapActivateIntegration(getServiceName().getParent(), resolvedBundles).install(serviceTarget, getServiceListener());
        }
    }

    class BootstrapActivateIntegration extends BootstrapBundlesActivate<Void> {

        BootstrapActivateIntegration(ServiceName baseName, Set<XBundle> resolvedBundles) {
            super(baseName, resolvedBundles);
        }

        @Override
        public void start(StartContext context) throws StartException {

            // Start the module capabilities that have a start level assigned
            BundleManager bundleManager = injectedBundleManager.getValue();
            for (OSGiCapability modcap : modulecaps) {
                if (modcap.getStartLevel() != null) {
                    String identifier = modcap.getIdentifier();
                    XBundle bundle = bundleManager.getBundleByLocation(identifier);
                    try {
                        bundle.start(Bundle.START_ACTIVATION_POLICY);
                    } catch (BundleException ex) {
                        LOGGER.errorCannotStartBundle(ex, bundle);
                    }
                }
            }

            // Start the bundle capabilities
            super.start(context);
        }
    }
}

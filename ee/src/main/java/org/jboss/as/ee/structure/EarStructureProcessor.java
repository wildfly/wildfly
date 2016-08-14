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

package org.jboss.as.ee.structure;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.jboss.as.ee.logging.EeLogger;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.MountedDeploymentOverlay;
import org.jboss.as.server.deployment.SubDeploymentMarker;
import org.jboss.as.server.deployment.SubExplodedDeploymentMarker;
import org.jboss.as.server.deployment.module.ModuleRootMarker;
import org.jboss.as.server.deployment.module.MountHandle;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.as.server.deployment.module.TempFileProviderService;
import org.jboss.metadata.ear.spec.EarMetaData;
import org.jboss.metadata.ear.spec.ModuleMetaData;
import org.jboss.metadata.ear.spec.ModuleMetaData.ModuleType;
import org.jboss.vfs.VFS;
import org.jboss.vfs.VFSUtils;
import org.jboss.vfs.VirtualFile;
import org.jboss.vfs.VisitorAttributes;
import org.jboss.vfs.util.SuffixMatchFilter;

/**
 * Deployment processor responsible for detecting EAR deployments and putting setting up the basic structure.
 *
 * @author John Bailey
 * @author Stuart Douglas
 */
public class EarStructureProcessor implements DeploymentUnitProcessor {

    private static final String JAR_EXTENSION = ".jar";
    private static final String WAR_EXTENSION = ".war";
    private static final String SAR_EXTENSION = ".sar";
    private static final String RAR_EXTENSION = ".rar";
    private static final List<String> CHILD_ARCHIVE_EXTENSIONS = new ArrayList<String>();

    static {
        CHILD_ARCHIVE_EXTENSIONS.add(JAR_EXTENSION);
        CHILD_ARCHIVE_EXTENSIONS.add(WAR_EXTENSION);
        CHILD_ARCHIVE_EXTENSIONS.add(SAR_EXTENSION);
        CHILD_ARCHIVE_EXTENSIONS.add(RAR_EXTENSION);
    }

    private static final SuffixMatchFilter CHILD_ARCHIVE_FILTER = new SuffixMatchFilter(CHILD_ARCHIVE_EXTENSIONS, new VisitorAttributes() {

        public boolean isLeavesOnly() {
            return false;
        }
    });

    private static final String DEFAULT_LIB_DIR = "lib";


    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        if (!DeploymentTypeMarker.isType(DeploymentType.EAR, deploymentUnit)) {
            return;
        }

        final ResourceRoot deploymentRoot = phaseContext.getDeploymentUnit().getAttachment(Attachments.DEPLOYMENT_ROOT);
        final VirtualFile virtualFile = deploymentRoot.getRoot();

        //  Make sure we don't index or add this as a module root
        deploymentRoot.putAttachment(Attachments.INDEX_RESOURCE_ROOT, false);
        ModuleRootMarker.mark(deploymentRoot, false);

        String libDirName = DEFAULT_LIB_DIR;
        //its possible that the ear metadata could come for jboss-app.xml
        final boolean appXmlPresent = deploymentRoot.getRoot().getChild("META-INF/application.xml").exists();
        final EarMetaData earMetaData = deploymentUnit.getAttachment(org.jboss.as.ee.structure.Attachments.EAR_METADATA);
        if (earMetaData != null) {
            final String xmlLibDirName = earMetaData.getLibraryDirectory();
            if (xmlLibDirName != null) {
                if (xmlLibDirName.length() == 1 && xmlLibDirName.charAt(0) == '/') {
                    throw EeLogger.ROOT_LOGGER.rootAsLibraryDirectory();
                }
                libDirName = xmlLibDirName;
            }
        }

        // Process all the children
        Map<String, MountedDeploymentOverlay> overlays = deploymentUnit.getAttachment(Attachments.DEPLOYMENT_OVERLAY_LOCATIONS);
        try {
            final VirtualFile libDir;
            // process the lib directory
            if (!libDirName.isEmpty()) {
                libDir = virtualFile.getChild(libDirName);
                if (libDir.exists()) {
                    List<VirtualFile> libArchives = libDir.getChildren(CHILD_ARCHIVE_FILTER);
                    for (final VirtualFile child : libArchives) {
                        String relativeName = child.getPathNameRelativeTo(deploymentRoot.getRoot());
                        MountedDeploymentOverlay overlay = overlays.get(relativeName);
                        final MountHandle mountHandle;
                        if(overlay != null) {
                            overlay.remountAsZip(false);
                            mountHandle = new MountHandle(null);
                        } else {
                            final Closeable closable = child.isFile() ? mount(child, false) : null;
                            mountHandle = new MountHandle(closable);
                        }
                        final ResourceRoot childResource = new ResourceRoot(child, mountHandle);
                        if (child.getName().toLowerCase(Locale.ENGLISH).endsWith(JAR_EXTENSION)) {
                            ModuleRootMarker.mark(childResource);
                            deploymentUnit.addToAttachmentList(Attachments.RESOURCE_ROOTS, childResource);
                        }
                    }
                }
            } else {
                libDir = null;
            }
            // scan the ear looking for wars and jars
            final List<VirtualFile> childArchives = new ArrayList<VirtualFile>(virtualFile.getChildren(new SuffixMatchFilter(
                    CHILD_ARCHIVE_EXTENSIONS, new VisitorAttributes() {
                @Override
                public boolean isLeavesOnly() {
                    return false;
                }

                @Override
                public boolean isRecurse(VirtualFile file) {
                    // don't recurse into /lib
                    if (file.equals(libDir)) {
                        return false;
                    }
                    for (String suffix : CHILD_ARCHIVE_EXTENSIONS) {
                        if (file.getName().endsWith(suffix)) {
                            // don't recurse into sub deployments
                            return false;
                        }
                    }
                    return true;
                }
            })));

            // if there is no application.xml then look in the ear root for modules
            if (!appXmlPresent) {
                for (final VirtualFile child : childArchives) {
                    final boolean isWarFile = child.getName().toLowerCase(Locale.ENGLISH).endsWith(WAR_EXTENSION);
                    final boolean isRarFile = child.getName().toLowerCase(Locale.ENGLISH).endsWith(RAR_EXTENSION);
                    this.createResourceRoot(deploymentUnit, child, isWarFile || isRarFile, isWarFile);
                }
            } else {
                final Set<VirtualFile> subDeploymentFiles = new HashSet<VirtualFile>();
                // otherwise read from application.xml
                for (final ModuleMetaData module : earMetaData.getModules()) {

                    if(module.getFileName().endsWith(".xml")) {
                        throw EeLogger.ROOT_LOGGER.unsupportedModuleType(module.getFileName());
                    }

                    final VirtualFile moduleFile = virtualFile.getChild(module.getFileName());
                    if (!moduleFile.exists()) {
                        throw EeLogger.ROOT_LOGGER.cannotProcessEarModule(virtualFile, module.getFileName());
                    }

                    if (libDir != null) {
                        VirtualFile moduleParentFile = moduleFile.getParent();
                        if (moduleParentFile != null) {
                            if (libDir.equals(moduleParentFile)) {
                                throw EeLogger.ROOT_LOGGER.earModuleChildOfLibraryDirectory(libDirName, module.getFileName());
                            }
                        }
                    }

                    // maintain this in a collection of subdeployment virtual files, to be used later
                    subDeploymentFiles.add(moduleFile);

                    final boolean webArchive = module.getType() == ModuleType.Web;
                    final ResourceRoot childResource = this.createResourceRoot(deploymentUnit, moduleFile, true, webArchive);
                    childResource.putAttachment(org.jboss.as.ee.structure.Attachments.MODULE_META_DATA, module);

                    if (!webArchive) {
                        ModuleRootMarker.mark(childResource);
                    }

                    final String alternativeDD = module.getAlternativeDD();
                    if (alternativeDD != null && alternativeDD.trim().length() > 0) {
                        final VirtualFile alternateDeploymentDescriptor = deploymentRoot.getRoot().getChild(alternativeDD);
                        if (!alternateDeploymentDescriptor.exists()) {
                            throw EeLogger.ROOT_LOGGER.alternateDeploymentDescriptor(alternateDeploymentDescriptor, moduleFile);
                        }
                        switch (module.getType()) {
                            case Client:
                                childResource.putAttachment(org.jboss.as.ee.structure.Attachments.ALTERNATE_CLIENT_DEPLOYMENT_DESCRIPTOR, alternateDeploymentDescriptor);
                                break;
                            case Connector:
                                childResource.putAttachment(org.jboss.as.ee.structure.Attachments.ALTERNATE_CONNECTOR_DEPLOYMENT_DESCRIPTOR, alternateDeploymentDescriptor);
                                break;
                            case Ejb:
                                childResource.putAttachment(org.jboss.as.ee.structure.Attachments.ALTERNATE_EJB_DEPLOYMENT_DESCRIPTOR, alternateDeploymentDescriptor);
                                break;
                            case Web:
                                childResource.putAttachment(org.jboss.as.ee.structure.Attachments.ALTERNATE_WEB_DEPLOYMENT_DESCRIPTOR, alternateDeploymentDescriptor);
                                break;
                            case Service:
                                throw EeLogger.ROOT_LOGGER.unsupportedModuleType(module.getFileName());

                        }
                    }
                }
                // now check the rest of the archive for any other jar/sar files
                for (final VirtualFile child : childArchives) {
                    if (subDeploymentFiles.contains(child)) {
                        continue;
                    }
                    final String fileName = child.getName().toLowerCase(Locale.ENGLISH);
                    if (fileName.endsWith(SAR_EXTENSION) || fileName.endsWith(JAR_EXTENSION)) {
                        this.createResourceRoot(deploymentUnit, child, false, false);
                    }
                }
            }

        } catch (IOException e) {
            throw EeLogger.ROOT_LOGGER.failedToProcessChild(e, virtualFile);
        }
    }

    private static Closeable mount(VirtualFile moduleFile, boolean explode) throws IOException {
        return explode ? VFS.mountZipExpanded(moduleFile, moduleFile, TempFileProviderService.provider())
                : VFS.mountZip(moduleFile, moduleFile, TempFileProviderService.provider());
    }

    /**
     * Creates a {@link ResourceRoot} for the passed {@link VirtualFile file} and adds it to the list of {@link ResourceRoot}s
     * in the {@link DeploymentUnit deploymentUnit}
     *
     * @param deploymentUnit      The deployment unit
     * @param file                The file for which the resource root will be created
     * @param markAsSubDeployment If this is true, then the {@link ResourceRoot} that is created will be marked as a subdeployment
     *                            through a call to {@link SubDeploymentMarker#mark(org.jboss.as.server.deployment.module.ResourceRoot)}
     * @param explodeDuringMount  If this is true then the {@link VirtualFile file} will be exploded during mount,
     *                            while creating the {@link ResourceRoot}
     * @return Returns the created {@link ResourceRoot}
     * @throws IOException
     */
    private ResourceRoot createResourceRoot(final DeploymentUnit deploymentUnit, final VirtualFile file, final boolean markAsSubDeployment, final boolean explodeDuringMount) throws IOException {
        final boolean war = file.getName().toLowerCase(Locale.ENGLISH).endsWith(WAR_EXTENSION);
        final Closeable closable = file.isFile() ? mount(file, explodeDuringMount) : exportExplodedWar(war, file, deploymentUnit);
        final MountHandle mountHandle = new MountHandle(closable);
        final ResourceRoot resourceRoot = new ResourceRoot(file, mountHandle);
        deploymentUnit.addToAttachmentList(Attachments.RESOURCE_ROOTS, resourceRoot);
        if (markAsSubDeployment) {
            SubDeploymentMarker.mark(resourceRoot);
        }
        if (war) {
            resourceRoot.putAttachment(Attachments.INDEX_RESOURCE_ROOT, false);
            SubExplodedDeploymentMarker.mark(resourceRoot);
        }
        return resourceRoot;
    }

    private Closeable exportExplodedWar(final boolean war, final VirtualFile file, final DeploymentUnit deploymentUnit) throws IOException {
        if (isExplodedWarInArchiveEar(war, file, deploymentUnit)) {
            File warContent = file.getPhysicalFile();
            VFSUtils.recursiveCopy(file, warContent.getParentFile());
            return VFS.mountReal(warContent, file);
        }
        return null;
    }

    private boolean isExplodedWarInArchiveEar(final boolean war, final VirtualFile file, final DeploymentUnit deploymentUnit) {
        return war && !file.isFile() && deploymentUnit.hasAttachment(Attachments.DEPLOYMENT_CONTENTS) && deploymentUnit.getAttachment(Attachments.DEPLOYMENT_CONTENTS).isFile();
    }


    public void undeploy(DeploymentUnit context) {
        final List<ResourceRoot> children = context.removeAttachment(Attachments.RESOURCE_ROOTS);
        if (children != null) {
            for (ResourceRoot childRoot : children) {
                VFSUtils.safeClose(childRoot.getMountHandle());
            }
        }
    }
}

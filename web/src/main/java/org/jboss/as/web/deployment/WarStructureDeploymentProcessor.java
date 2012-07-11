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

package org.jboss.as.web.deployment;

import static org.jboss.as.web.WebMessages.MESSAGES;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jboss.as.ee.structure.DeploymentType;
import org.jboss.as.ee.structure.DeploymentTypeMarker;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.PrivateSubDeploymentMarker;
import org.jboss.as.server.deployment.module.ModuleRootMarker;
import org.jboss.as.server.deployment.module.ModuleSpecification;
import org.jboss.as.server.deployment.module.MountHandle;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.as.server.deployment.module.TempFileProviderService;
import org.jboss.as.web.SharedTldsMetaDataBuilder;
import org.jboss.metadata.web.spec.WebMetaData;
import org.jboss.vfs.VFS;
import org.jboss.vfs.VirtualFile;
import org.jboss.vfs.VirtualFileFilter;
import org.jboss.vfs.VisitorAttributes;
import org.jboss.vfs.util.SuffixMatchFilter;

/**
 * Create and mount classpath entries in the .war deployment.
 *
 * @author Emanuel Muckenhuber
 * @author Thomas.Diesler@jboss.com
 */
public class WarStructureDeploymentProcessor implements DeploymentUnitProcessor {

    public static final String WEB_INF_LIB = "WEB-INF/lib";
    public static final String WEB_INF_CLASSES = "WEB-INF/classes";

    public static final VirtualFileFilter DEFAULT_WEB_INF_LIB_FILTER = new SuffixMatchFilter(".jar", VisitorAttributes.DEFAULT);

    private final WebMetaData sharedWebMetaData;
    private final SharedTldsMetaDataBuilder sharedTldsMetaData;

    public WarStructureDeploymentProcessor(final WebMetaData sharedWebMetaData, final SharedTldsMetaDataBuilder sharedTldsMetaData) {
        this.sharedWebMetaData = sharedWebMetaData;
        this.sharedTldsMetaData = sharedTldsMetaData;
    }

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        if (!DeploymentTypeMarker.isType(DeploymentType.WAR, deploymentUnit)) {
            return; // Skip non web deployments
        }

        final ResourceRoot deploymentResourceRoot = deploymentUnit.getAttachment(Attachments.DEPLOYMENT_ROOT);

        final VirtualFile deploymentRoot = deploymentResourceRoot.getRoot();
        if (deploymentRoot == null) {
            return;
        }

        // set the child first behaviour
        final ModuleSpecification moduleSpecification = deploymentUnit.getAttachment(Attachments.MODULE_SPECIFICATION);
        if (moduleSpecification == null) {
            return;
        }
        moduleSpecification.setPrivateModule(true);

        // other sub deployments should not have access to classes in the war module
        PrivateSubDeploymentMarker.mark(deploymentUnit);

        // OSGi WebApp deployments (WAB) may use the deployment root
        if (deploymentUnit.hasAttachment(Attachments.OSGI_MANIFEST)) {
            deploymentUnit.addToAttachmentList(Attachments.RESOURCE_ROOTS, deploymentResourceRoot);
        } else {
            // we do not want to index the resource root, only WEB-INF/classes and WEB-INF/lib
            deploymentResourceRoot.putAttachment(Attachments.INDEX_RESOURCE_ROOT, false);
            // Make sure the root does not end up in the module
            ModuleRootMarker.mark(deploymentResourceRoot, false);
        }

        // TODO: This needs to be ported to add additional resource roots the standard way
        final MountHandle mountHandle = deploymentResourceRoot.getMountHandle();
        try {
            // add standard resource roots, this should eventually replace ClassPathEntry
            final List<ResourceRoot> resourceRoots = createResourceRoots(deploymentRoot, mountHandle);
            for (ResourceRoot root : resourceRoots) {
                deploymentUnit.addToAttachmentList(Attachments.RESOURCE_ROOTS, root);
            }
        } catch (Exception e) {
            throw new DeploymentUnitProcessingException(e);
        }
        // Add the war metadata
        final WarMetaData warMetaData = new WarMetaData();
        warMetaData.setSharedWebMetaData(sharedWebMetaData);
        deploymentUnit.putAttachment(WarMetaData.ATTACHMENT_KEY, warMetaData);
        // Add the shared TLDs metadata
        final TldsMetaData tldsMetaData = new TldsMetaData();
        tldsMetaData.setSharedTlds(sharedTldsMetaData);
        deploymentUnit.putAttachment(TldsMetaData.ATTACHMENT_KEY, tldsMetaData);
    }

    @Override
    public void undeploy(final DeploymentUnit context) {
    }

    /**
     * Create the resource roots for a .war deployment
     *
     * @param deploymentRoot the deployment root
     * @param mountHandle    the root mount handle
     * @return the resource roots
     * @throws IOException for any error
     */
    private List<ResourceRoot> createResourceRoots(final VirtualFile deploymentRoot, MountHandle mountHandle) throws IOException, DeploymentUnitProcessingException {
        final List<ResourceRoot> entries = new ArrayList<ResourceRoot>();
        // WEB-INF classes
        final VirtualFile webinfClasses = deploymentRoot.getChild(WEB_INF_CLASSES);
        if (webinfClasses.exists()) {
            final ResourceRoot webInfClassesRoot = new ResourceRoot(webinfClasses.getName(), webinfClasses, null);
            ModuleRootMarker.mark(webInfClassesRoot);
            entries.add(webInfClassesRoot);
        }
        // WEB-INF lib
        final VirtualFile webinfLib = deploymentRoot.getChild(WEB_INF_LIB);
        if (webinfLib.exists()) {
            final List<VirtualFile> archives = webinfLib.getChildren(DEFAULT_WEB_INF_LIB_FILTER);
            for (final VirtualFile archive : archives) {
                try {
                    final Closeable closable = VFS.mountZip(archive, archive, TempFileProviderService.provider());
                    final ResourceRoot webInfArchiveRoot = new ResourceRoot(archive.getName(), archive, new MountHandle(closable));
                    ModuleRootMarker.mark(webInfArchiveRoot);
                    entries.add(webInfArchiveRoot);
                } catch (IOException e) {
                    throw new DeploymentUnitProcessingException(MESSAGES.failToProcessWebInfLib(archive), e);
                }
            }
        }
        return entries;
    }
}

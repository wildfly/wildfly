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

import static org.jboss.as.deployment.attachment.VirtualFileAttachment.getVirtualFileAttachment;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jboss.as.deployment.DeploymentPhases;
import org.jboss.as.deployment.module.MountHandle;
import org.jboss.as.deployment.module.TempFileProviderService;
import org.jboss.as.deployment.unit.DeploymentUnitContext;
import org.jboss.as.deployment.unit.DeploymentUnitProcessingException;
import org.jboss.as.deployment.unit.DeploymentUnitProcessor;
import org.jboss.as.web.deployment.helpers.DeploymentStructure;
import org.jboss.as.web.deployment.helpers.DeploymentStructure.ClassPathEntry;
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
 */
public class WarStructureDeploymentProcessor implements DeploymentUnitProcessor {

    public static final long PRIORITY = DeploymentPhases.VALIDATE.plus(300L);

    public static final String WEB_INF_LIB = "WEB-INF/lib";
    public static final String WEB_INF_CLASSES = "WEB-INF/classes";

    public static final VirtualFileFilter DEFAULT_WEB_INF_LIB_FILTER = new SuffixMatchFilter(".jar", VisitorAttributes.DEFAULT);

    private final WebMetaData sharedWebMetaData;

    public WarStructureDeploymentProcessor(final WebMetaData sharedWebMetaData) {
        this.sharedWebMetaData = sharedWebMetaData;
    }

    /** {@inheritDoc} */
    public void processDeployment(DeploymentUnitContext context) throws DeploymentUnitProcessingException {
        final VirtualFile deploymentRoot = getVirtualFileAttachment(context);
        if(deploymentRoot == null) {
            return;
        }
        final MountHandle mountHandle = context.getAttachment(MountHandle.ATTACHMENT_KEY);
        try {
            final ClassPathEntry[] entries = createResourceRoots(deploymentRoot, mountHandle);
            final DeploymentStructure structure = new DeploymentStructure(entries);
            context.putAttachment(DeploymentStructure.ATTACHMENT_KEY, structure);
        } catch(IOException e) {
            throw new DeploymentUnitProcessingException(e);
        }
        // add the war metadata
        final WarMetaData warMetaData = new WarMetaData();
        warMetaData.setSharedWebMetaData(sharedWebMetaData);
        context.putAttachment(WarMetaData.ATTACHMENT_KEY, warMetaData);
    }

    /**
     * Create the resource roots for a .war deployment
     *
     * @param deploymentRoot the deployment root
     * @param mountHandle the root mount handle
     * @return the resource roots
     * @throws IOException for any error
     */
    ClassPathEntry[] createResourceRoots(final VirtualFile deploymentRoot, MountHandle mountHandle) throws IOException {
        final List<ClassPathEntry> entries = new ArrayList<ClassPathEntry>();
        // deployment root
        entries.add(new ClassPathEntry("", deploymentRoot, mountHandle));
        // WEB-INF classes
        entries.add(new ClassPathEntry(deploymentRoot.getChild(WEB_INF_CLASSES), null));
        // WEB-INF lib
        createWebInfLibResources(deploymentRoot, entries);
        return entries.toArray(new ClassPathEntry[entries.size()]);
    }

    /**
     * Create the ResourceRoots for .jars in the WEB-INF/lib folder.
     *
     * @param deploymentRoot the deployment root
     * @param resourcesRoots the resource root map
     * @throws IOException for any error
     */
    void createWebInfLibResources(final VirtualFile deploymentRoot, List<ClassPathEntry> entries) throws IOException {
        final VirtualFile webinfLib = deploymentRoot.getChild(WEB_INF_LIB);
        if(webinfLib.exists()) {
            final List<VirtualFile> archives = webinfLib.getChildren(DEFAULT_WEB_INF_LIB_FILTER);
            for(final VirtualFile archive : archives) {
                final Closeable closable = VFS.mountZip(archive, archive, TempFileProviderService.provider());
                entries.add(new ClassPathEntry(archive, new MountHandle(closable)));
            }
        }
    }

}

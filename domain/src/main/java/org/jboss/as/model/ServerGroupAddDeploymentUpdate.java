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

package org.jboss.as.model;

import org.jboss.as.deployment.chain.DeploymentChain;
import org.jboss.as.deployment.chain.DeploymentChainProvider;
import org.jboss.as.deployment.item.DeploymentItem;
import org.jboss.as.deployment.item.DeploymentItemRegistry;
import org.jboss.as.deployment.module.TempFileProviderService;
import org.jboss.as.deployment.unit.DeploymentUnitContextImpl;
import org.jboss.as.deployment.unit.DeploymentUnitProcessingException;
import org.jboss.vfs.VFS;
import org.jboss.vfs.VFSUtils;
import org.jboss.vfs.VirtualFile;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;

import static org.jboss.as.deployment.attachment.VirtualFileAttachment.attachVirtualFile;

/**
 * Update used when adding a deployment unit to a server group.
 *
 * @author John E. Bailey
 */
public class ServerGroupAddDeploymentUpdate extends AbstractModelUpdate<ServerGroupElement> {

    private final DeploymentUnitKey key;

    public ServerGroupAddDeploymentUpdate(final String fileName, final byte[] sha1Hash) {
        this.key = new DeploymentUnitKey(fileName, sha1Hash);
    }

    @Override
    protected Class<ServerGroupElement> getModelElementType() {
        return ServerGroupElement.class;
    }

    @Override
    protected AbstractModelUpdate<ServerGroupElement> applyUpdate(ServerGroupElement serverGroup) {
        final DeploymentUnitKey key = this.key;

        final VirtualFile deploymentRoot = VFS.getChild(getFullyQualifiedDeploymentPath(key.getName()));
        if (!deploymentRoot.exists())
            throw new RuntimeException("Deployment root does not exist." + deploymentRoot);

        Closeable handle = null;
        try {
            // Mount virtual file
            try {
                if(deploymentRoot.isFile())
                    handle = VFS.mountZip(deploymentRoot, deploymentRoot, TempFileProviderService.provider());
            } catch (IOException e) {
                throw new RuntimeException("Failed to mount deployment archive", e);
            }

            // Create the deployment unit context
            final String deploymentName = key.getName() + ":" + key.getSha1HashAsHexString();
            final DeploymentUnitContextImpl deploymentUnitContext = new DeploymentUnitContextImpl(deploymentName);
            attachVirtualFile(deploymentUnitContext, deploymentRoot);

            // Execute the deployment chain
            final DeploymentChainProvider deploymentChainProvider = DeploymentChainProvider.INSTANCE;
            final DeploymentChain deploymentChain = deploymentChainProvider.determineDeploymentChain(deploymentRoot);
            if(deploymentChain == null)
                throw new RuntimeException("Failed determine the deployment chain for deployment root: " + deploymentRoot);
            try {
                deploymentChain.processDeployment(deploymentUnitContext);
            } catch (DeploymentUnitProcessingException e) {
                throw new RuntimeException("Failed to process deployment chain.", e);
            }
            
            // Serialize deployment items
            final List<DeploymentItem> deploymentItems = deploymentUnitContext.getDeploymentItems();
            DeploymentItemRegistry.registerDeploymentItems(key, deploymentItems);
        } finally {
            VFSUtils.safeClose(handle);
        }
        return null; // TODO: return a deployment removal update
    }

    private String getFullyQualifiedDeploymentPath(final String fileName) {
        return fileName;  // TODO: Need some way to get fully qualified path for this (should be a system prop)
    }
}

/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.capedwarf.deployment;

import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.as.server.deployment.AttachmentList;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.vfs.TempDir;
import org.jboss.vfs.VFS;
import org.jboss.vfs.VFSUtils;
import org.jboss.vfs.VirtualFile;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Random;

/**
 * Handle CapeDwarf JSF usage.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class CapedwarfJsfProcessor extends CapedwarfDeploymentUnitProcessor {

    static final String FACES_CONFIG = "faces-config.xml";
    static final Random rng = new Random();
    static final AttachmentKey<AttachmentList<Closeable>> ASSEMBLY_HANDLE = AttachmentKey.createList(Closeable.class);

    final TempDir tempDir;
    volatile File configFile;

    public CapedwarfJsfProcessor(TempDir tempDir) {
        this.tempDir = tempDir;
    }

    protected File getTempConfigFile() throws IOException {
        if (configFile == null) {
            synchronized (this) {
                if (configFile == null) {
                    final InputStream stream = new ByteArrayInputStream("<faces-config />".getBytes());
                    configFile = tempDir.createFile(Long.toHexString(rng.nextLong()) + "_" + FACES_CONFIG, stream);
                }
            }
        }
        return configFile;
    }

    protected void doDeploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit unit = phaseContext.getDeploymentUnit();
        final ResourceRoot deploymentRoot = unit.getAttachment(Attachments.DEPLOYMENT_ROOT);
        final VirtualFile parent = deploymentRoot.getRoot().getChild("WEB-INF");
        if (parent.getChild(FACES_CONFIG).exists() == false) {
            try {
                final ModifiedFileSystem mfs = ModifiedFileSystem.get(unit, parent.getPhysicalFile());
                mfs.addFile(FACES_CONFIG, getTempConfigFile());
                final Closeable closeable = VFS.mount(parent, mfs);
                unit.addToAttachmentList(ASSEMBLY_HANDLE, closeable);
            } catch (IOException e) {
                throw new DeploymentUnitProcessingException("Cannot mount CapeDwarf JFS usage.", e);
            }
        }
    }

    @Override
    public void undeploy(DeploymentUnit context) {
        final AttachmentList<Closeable> closeables = context.getAttachment(ASSEMBLY_HANDLE);
        VFSUtils.safeClose(closeables);
        ModifiedFileSystem.remove(context);
    }
}

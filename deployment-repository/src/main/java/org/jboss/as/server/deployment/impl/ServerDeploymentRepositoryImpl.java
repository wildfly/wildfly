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

package org.jboss.as.server.deployment.impl;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;

import java.util.concurrent.Executors;
import org.jboss.as.server.deployment.api.ServerDeploymentRepository;
import org.jboss.logging.Logger;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.vfs.TempFileProvider;
import org.jboss.vfs.VFS;
import org.jboss.vfs.VirtualFile;

/**
 * Default implementation of {@link ServerDeploymentRepository}.
 *
 * @author Brian Stansberry
 */
public class ServerDeploymentRepositoryImpl extends DeploymentRepositoryImpl implements ServerDeploymentRepository, Service<ServerDeploymentRepository> {

    private static final Logger log = Logger.getLogger("org.jboss.as.server.deployment");

    private final File systemDeployDir;
    private TempFileProvider tempFileProvider;

    /**
     * Creates a new ServerDeploymentRepositoryImpl.
     */
    public ServerDeploymentRepositoryImpl(final File repoRoot, final File systemDeployDir) {
        super(repoRoot);
        this.systemDeployDir = systemDeployDir;
    }

    public Closeable mountDeploymentContent(String name, String runtimeName, byte[] deploymentHash, VirtualFile mountPoint) throws IOException {
        // Internal deployments have no hash, and are unique by name
        if (deploymentHash == null) {
            File file = new File(systemDeployDir, name);
            return VFS.mountZip(file, mountPoint, tempFileProvider);
        }

        // TODO recognize exploded content stored in a hot-deploy dir
        String sha1 = bytesToHexString(deploymentHash);
        String partA = sha1.substring(0,2);
        String partB = sha1.substring(2);
        File base = new File(getRepoRoot(), partA);
        File hashDir = new File(base, partB);
        File content = new File(hashDir, CONTENT);
        // FIXME
        if(name.endsWith("war")) {
            return VFS.mountZipExpanded(content, mountPoint, tempFileProvider);
        } else {
            return VFS.mountZip(content, mountPoint, tempFileProvider);
        }
    }

    @Override
    public void start(StartContext context) throws StartException {
        try {
            tempFileProvider = TempFileProvider.create("temp", Executors.newScheduledThreadPool(2));
        } catch (IOException e) {
            throw new StartException("Failed to create temp file provider");
        }

        log.debugf("%s started", ServerDeploymentRepository.class.getSimpleName());
    }


    @Override
    public void stop(StopContext context) {
        log.debugf("%s stopped", ServerDeploymentRepository.class.getSimpleName());
    }


    @Override
    public ServerDeploymentRepository getValue() throws IllegalStateException {
        return this;
    }

}

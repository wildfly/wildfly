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

package org.jboss.as.server.deployment.repository.impl;

import java.io.Closeable;
import java.io.IOException;
import java.security.AccessController;
import java.util.concurrent.Executors;

import org.jboss.as.server.deployment.repository.api.MountType;
import org.jboss.as.server.deployment.repository.api.ServerDeploymentRepository;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.threads.JBossThreadFactory;
import org.jboss.vfs.TempFileProvider;
import org.jboss.vfs.VFS;
import org.jboss.vfs.VirtualFile;

import static org.jboss.as.server.deployment.repository.impl.DeploymentRepositoryLogger.ROOT_LOGGER;
import static org.jboss.as.server.deployment.repository.impl.DeploymentRepositoryMessages.MESSAGES;

/**
 * Default implementation of {@link ServerDeploymentRepository}.
 *
 * @author Brian Stansberry
 */
public class ServerDeploymentRepositoryImpl implements ServerDeploymentRepository, Service<ServerDeploymentRepository> {
    private volatile TempFileProvider tempFileProvider;
    private final ContentRepositoryImpl contentRepository;


    public static void addService(final ServiceTarget serviceTarget, final ContentRepositoryImpl contentRepository) {
        serviceTarget.addService(ServerDeploymentRepository.SERVICE_NAME,
                new ServerDeploymentRepositoryImpl(contentRepository))
                .install();
    }

    /**
     * Creates a new ServerDeploymentRepositoryImpl.
     */
    public ServerDeploymentRepositoryImpl(final ContentRepositoryImpl contentRepository) {
        this.contentRepository = contentRepository;
    }

    @Override
    public Closeable mountDeploymentContent(final VirtualFile contents, VirtualFile mountPoint, MountType type) throws IOException {
        // according to the javadoc contents can not be null
        if (contents == null)
            throw MESSAGES.nullVar("contents");
        switch (type) {
            case ZIP:
                return VFS.mountZip(contents, mountPoint, tempFileProvider);
            case EXPANDED:
                return VFS.mountZipExpanded(contents, mountPoint, tempFileProvider);
            case REAL:
                return VFS.mountReal(contents.getPhysicalFile(), mountPoint);
            default:
                throw DeploymentRepositoryMessages.MESSAGES.unknownMountType(type);
        }
    }

    @Override
    public void start(StartContext context) throws StartException {
        try {
            final JBossThreadFactory threadFactory = new JBossThreadFactory(new ThreadGroup("ServerDeploymentRepository-temp-threads"), Boolean.FALSE, null, "%G - %t", null, null, AccessController.getContext());
            tempFileProvider = TempFileProvider.create("temp", Executors.newScheduledThreadPool(2, threadFactory));
        } catch (IOException e) {
            throw MESSAGES.failedCreatingTempProvider();
        }
        ROOT_LOGGER.debugf("%s started", ServerDeploymentRepository.class.getSimpleName());
    }

    @Override
    public void stop(StopContext context) {
        ROOT_LOGGER.debugf("%s stopped", ServerDeploymentRepository.class.getSimpleName());
    }


    @Override
    public ServerDeploymentRepository getValue() throws IllegalStateException {
        return this;
    }

}

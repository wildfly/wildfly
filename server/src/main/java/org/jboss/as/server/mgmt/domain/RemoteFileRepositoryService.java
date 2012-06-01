/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.server.mgmt.domain;

import org.jboss.as.controller.HashUtil;
import org.jboss.as.repository.ContentRepository;
import org.jboss.as.repository.DeploymentFileRepository;
import org.jboss.as.repository.LocalDeploymentFileRepository;
import static org.jboss.as.server.ServerMessages.MESSAGES;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.vfs.VirtualFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author Emanuel Muckenhuber
 */
public class RemoteFileRepositoryService implements CompositeContentRepository, Service<CompositeContentRepository> {

    private final InjectedValue<HostControllerClient> clientInjectedValue = new InjectedValue<HostControllerClient>();

    private final File localDeploymentFolder;
    private final DeploymentFileRepository localRepository;
    private final ContentRepository contentRepository;
    private volatile RemoteFileRepositoryExecutor remoteFileRepositoryExecutor;

    public static void addService(final ServiceTarget target, final File localDeploymentContentsFolder) {
        final RemoteFileRepositoryService service = new RemoteFileRepositoryService(localDeploymentContentsFolder);
        target.addService(ContentRepository.SERVICE_NAME, service)
                .addDependency(HostControllerConnectionService.SERVICE_NAME, HostControllerClient.class, service.clientInjectedValue)
                .setInitialMode(ServiceController.Mode.ACTIVE)
                .install();
    }

    RemoteFileRepositoryService(final File localDeploymentFolder) {
        this.localDeploymentFolder = localDeploymentFolder;
        this.contentRepository = ContentRepository.Factory.create(localDeploymentFolder);
        this.localRepository = new LocalDeploymentFileRepository(localDeploymentFolder);
    }

    @Override
    public void start(final StartContext context) throws StartException {
        final HostControllerClient client = clientInjectedValue.getValue();
        this.remoteFileRepositoryExecutor = client.getRemoteFileRepository();
    }

    @Override
    public void stop(StopContext context) {
        remoteFileRepositoryExecutor = null;
    }

    @Override
    public CompositeContentRepository getValue() throws IllegalStateException, IllegalArgumentException {
        final RemoteFileRepositoryExecutor executor = this.remoteFileRepositoryExecutor;
        if (executor == null) {
            throw MESSAGES.couldNotFindHcFileRepositoryConnection();
        }
        return this;
    }

    @Override
    public byte[] addContent(InputStream stream) throws IOException {
        return contentRepository.addContent(stream);
    }

    @Override
    public VirtualFile getContent(byte[] hash) {
        return contentRepository.getContent(hash);
    }

    @Override
    public boolean syncContent(byte[] hash) {
        if(! contentRepository.hasContent(hash)) {
            getDeploymentFiles(hash); // Make sure it's in sync
        }
        return contentRepository.hasContent(hash);
    }

    @Override
    public boolean hasContent(byte[] hash) {
        return contentRepository.hasContent(hash);
    }

    @Override
    public void removeContent(byte[] hash) {
        contentRepository.removeContent(hash);
    }

    @Override
    public final File[] getDeploymentFiles(byte[] deploymentHash) {
        final File root = getDeploymentRoot(deploymentHash);
        return root.listFiles();
    }

    @Override
    public File getDeploymentRoot(byte[] deploymentHash) {
        String hex = deploymentHash == null ? "" : HashUtil.bytesToHexString(deploymentHash);
        final File file = localRepository.getDeploymentRoot(deploymentHash);
        if(! file.exists()) {
            return getFile(hex, DomainServerProtocol.PARAM_ROOT_ID_DEPLOYMENT);
        }
        return file;
    }

    private File getFile(final String relativePath, final byte repoId) {
        final RemoteFileRepositoryExecutor executor = this.remoteFileRepositoryExecutor;
        if (executor == null) {
            throw MESSAGES.couldNotFindHcFileRepositoryConnection();
        }
        return remoteFileRepositoryExecutor.getFile(relativePath, repoId, localDeploymentFolder);
    }

    @Override
    public void deleteDeployment(byte[] deploymentHash) {
        localRepository.deleteDeployment(deploymentHash);
    }

}

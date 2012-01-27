/*
* JBoss, Home of Professional Open Source.
* Copyright 2011, Red Hat Middleware LLC, and individual contributors
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

import static org.jboss.as.server.ServerMessages.MESSAGES;

import java.io.File;

import org.jboss.as.controller.HashUtil;
import org.jboss.as.repository.DeploymentFileRepository;
import org.jboss.as.repository.LocalDeploymentFileRepository;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

public class RemoteFileRepository implements DeploymentFileRepository, Service<RemoteFileRepository>{

    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("host", "server", "remote", "repository");
    final File localDeploymentFolder;
    final DeploymentFileRepository localRepository;
    private volatile RemoteFileRepositoryExecutor remoteFileRepositoryExecutor;

    private RemoteFileRepository(final File localDeploymentFolder) {
        this.localDeploymentFolder = localDeploymentFolder;
        // this.remoteFileRepositoryExecutor = remoteFileRepositoryExecutor;
        localRepository = new LocalDeploymentFileRepository(localDeploymentFolder);
    }

    public static RemoteFileRepository addService(ServiceTarget serviceTarget, File localDeploymentFolder) {
        RemoteFileRepository repo = new RemoteFileRepository(localDeploymentFolder);
        serviceTarget.addService(SERVICE_NAME, repo).install();
        return repo;
    }

    @Override
    public RemoteFileRepository getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    @Override
    public void start(StartContext context) throws StartException {
    }

    @Override
    public void stop(StopContext context) {
    }

    @Override
    public final File[] getDeploymentFiles(byte[] deploymentHash) {
        String hex = deploymentHash == null ? "" : HashUtil.bytesToHexString(deploymentHash);
        return getFile(hex, DomainServerProtocol.PARAM_ROOT_ID_DEPLOYMENT).listFiles();
    }

    @Override
    public File getDeploymentRoot(byte[] deploymentHash) {
        String hex = deploymentHash == null ? "" : HashUtil.bytesToHexString(deploymentHash);
        return getFile(hex, DomainServerProtocol.PARAM_ROOT_ID_DEPLOYMENT);
    }

    private File getFile(final String relativePath, final byte repoId) {
        //The executor might not have been set up yet if we start with deployments
        RemoteFileRepositoryExecutor remoteFileRepositoryExecutor = this.remoteFileRepositoryExecutor;
        if (remoteFileRepositoryExecutor == null) {
            synchronized (this) {
                remoteFileRepositoryExecutor = this.remoteFileRepositoryExecutor;
                try {
                    while (remoteFileRepositoryExecutor == null){
                        wait();
                        remoteFileRepositoryExecutor = this.remoteFileRepositoryExecutor;
                    }
                } catch (InterruptedException e){
                    Thread.currentThread().interrupt();
                }
            }
        }
        if (remoteFileRepositoryExecutor == null) {
            //TODO i18n
            throw MESSAGES.couldNotFindHcFileRepositoryConnection();
        }
        return remoteFileRepositoryExecutor.getFile(relativePath, repoId, localDeploymentFolder);
    }

    void setRemoteFileRepositoryExecutor(RemoteFileRepositoryExecutor remoteFileRepositoryExecutor) {
        synchronized (this) {
            this.remoteFileRepositoryExecutor = remoteFileRepositoryExecutor;
            this.notifyAll();
        }
    }

    @Override
    public void deleteDeployment(byte[] deploymentHash) {
        localRepository.deleteDeployment(deploymentHash);
    }

    interface RemoteFileRepositoryExecutor {
        File getFile(final String relativePath, final byte repoId, final File localDeploymentFolder);
    }
}

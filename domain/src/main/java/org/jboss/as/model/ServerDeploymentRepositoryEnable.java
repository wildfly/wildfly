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

import org.jboss.as.deployment.filesystem.FileSystemDeploymentService;
import org.jboss.msc.service.ServiceController;

/**
 * Update enabling a {@code DeploymentRepositoryElement}.
 *
 * @author Emanuel Muckenhuber
 */
public class ServerDeploymentRepositoryEnable extends AbstractServerModelUpdate<Void> {

    private static final long serialVersionUID = 5959855923764647668L;
    private final String path;

    public ServerDeploymentRepositoryEnable(String path) {
        super(false, true);
        this.path = path;
    }

    /** {@inheritDoc} */
    protected void applyUpdate(ServerModel element) throws UpdateFailedException {
        final DeploymentRepositoryElement repository = element.getDeploymentRepository(path);
        if(repository == null) {
            throw new UpdateFailedException("non existent deployment repository " + path);
        }
        repository.setEnabled(true);
    }

    /** {@inheritDoc} */
    public AbstractServerModelUpdate<?> getCompensatingUpdate(ServerModel original) {
        return new ServerDeploymentRepositoryDisable(path);
    }

    /** {@inheritDoc} */
    public <P> void applyUpdate(UpdateContext context, UpdateResultHandler<? super Void,P> resultHandler, P param) {
        final ServiceController<?> controller = context.getServiceContainer().getService(FileSystemDeploymentService.getServiceName(path));
        if(controller == null) {
            resultHandler.handleFailure(notConfigured(), param);
        } else {
            try {
                final FileSystemDeploymentService service = (FileSystemDeploymentService) controller.getValue();
                service.setScanEnabled(true);
                resultHandler.handleSuccess(null, param);
            } catch (Throwable t) {
                resultHandler.handleFailure(t, param);
            }
        }
    }

    private UpdateFailedException notConfigured() {
        return new UpdateFailedException("No deployment repository named " + path + " is configured");
    }

}

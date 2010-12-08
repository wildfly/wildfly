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

import org.jboss.as.deployment.scanner.DeploymentScannerService;
import org.jboss.msc.service.ServiceController;

/**
 * Update removing a {@code DeploymentRepositoryElement} form the {@code ServerModel}.
 *
 * @author Emanuel Muckenhuber
 */
public class ServerDeploymentRepositoryRemove extends AbstractServerModelUpdate<Void> {

    private static final long serialVersionUID = -8749135039011134115L;
    private final String path;

    public ServerDeploymentRepositoryRemove(String path) {
        super(false, true);
        this.path = path;
    }

    /** {@inheritDoc} */
    protected void applyUpdate(ServerModel element) throws UpdateFailedException {
        if(! element.removeDeploymentRepository(path)) {
            throw new UpdateFailedException(String.format("deployment repository (%s) does not exist", path));
        }
    }

    /** {@inheritDoc} */
    public AbstractServerModelUpdate<?> getCompensatingUpdate(ServerModel original) {
        final DeploymentRepositoryElement repository = original.getDeploymentRepository(path);
        if(repository == null) {
            return null;
        }
        ServerDeploymentRepositoryAdd action = new ServerDeploymentRepositoryAdd(path, repository.getScanInterval(), repository.isScanEnabled());
        action.setName(repository.getName());
        action.setRelativeTo(repository.getRelativeTo());
        return action;
    }

    /** {@inheritDoc} */
    public <P> void applyUpdate(UpdateContext context, UpdateResultHandler<? super Void,P> resultHandler, P param) {
        final ServiceController<?> controller = context.getServiceRegistry().getService(DeploymentScannerService.getServiceName(path));
        if(controller == null) {
            resultHandler.handleSuccess(null, param);
            return;
        }
        controller.addListener(new UpdateResultHandler.ServiceRemoveListener<P>(resultHandler, param));
    }

}

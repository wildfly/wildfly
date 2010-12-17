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

package org.jboss.as.server.deployment.scanner;

import org.jboss.as.model.AbstractSubsystemUpdate;
import org.jboss.as.model.UpdateContext;
import org.jboss.as.model.UpdateFailedException;
import org.jboss.as.model.UpdateResultHandler;
import org.jboss.msc.service.ServiceController;

/**
 * Update removing a {@code DeploymentRepositoryElement} form the {@code ServerModel}.
 *
 * @author Emanuel Muckenhuber
 */
public class DeploymentScannerRemove extends AbstractDeploymentScannerSubsystemUpdate {

    private static final long serialVersionUID = -8749135039011134115L;
    private final String path;

    public DeploymentScannerRemove(String path) {
        this.path = path;
    }

    /**
     * {@inheritDoc}
     */
    protected void applyUpdate(DeploymentScannerSubsystemElement element) throws UpdateFailedException {
        final DeploymentScannerElement scannerElement = element.removeScanner(path);
        if (scannerElement == null) {
            throw new IllegalStateException("No deployment scanner for path " + path);
        }
    }

    /**
     * {@inheritDoc}
     */
    public AbstractSubsystemUpdate<DeploymentScannerSubsystemElement, ?> getCompensatingUpdate(DeploymentScannerSubsystemElement element) {
        final DeploymentScannerElement original = element.getScanner(path);
        if (original == null) {
            throw new IllegalStateException("No deployment scanner for path " + path);
        }
        return new DeploymentScannerAdd(original.getName(), path, original.getRelativeTo(), original.getScanInterval(), original.isScanEnabled());
    }

    /**
     * {@inheritDoc}
     */
    public <P> void applyUpdate(UpdateContext context, UpdateResultHandler<? super Void, P> resultHandler, P param) {
        final ServiceController<?> controller = context.getServiceRegistry().getService(DeploymentScannerService.getServiceName(path));
        if (controller == null) {
            resultHandler.handleSuccess(null, param);
            return;
        }
        controller.addListener(new UpdateResultHandler.ServiceRemoveListener<P>(resultHandler, param));
    }

}

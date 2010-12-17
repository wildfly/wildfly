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
import org.jboss.as.server.deployment.scanner.api.DeploymentScanner;
import org.jboss.msc.service.ServiceController;

/**
 * Update enabling a {@code DeploymentRepositoryElement}.
 *
 * @author Emanuel Muckenhuber
 */
public class DeploymentScannerEnable extends AbstractDeploymentScannerSubsystemUpdate {

    private static final long serialVersionUID = 5959855923764647668L;
    private final String name;
    private final String path;

    public DeploymentScannerEnable(final String name, String path) {
        this.name = name;
        this.path = path;
    }

    /** {@inheritDoc} */
    protected void applyUpdate(DeploymentScannerSubsystemElement element) throws UpdateFailedException {
        final DeploymentScannerElement scannerElement = element.getScanner(path);
        if (scannerElement == null) {
            throw new IllegalStateException("No deployment scanner for path " + path);
        }
        scannerElement.setEnabled(true);
    }

    /** {@inheritDoc} */
    public AbstractSubsystemUpdate<DeploymentScannerSubsystemElement, ?> getCompensatingUpdate(DeploymentScannerSubsystemElement original) {
        return new DeploymentScannerDisable(name, path);
    }

    /** {@inheritDoc} */
    public <P> void applyUpdate(UpdateContext context, UpdateResultHandler<? super Void,P> resultHandler, P param) {
        final ServiceController<?> controller = context.getServiceRegistry().getService(DeploymentScannerService.getServiceName(name));
        if(controller == null) {
            resultHandler.handleFailure(notConfigured(), param);
        } else {
            try {
                final DeploymentScanner scanner = (DeploymentScanner) controller.getValue();
                scanner.startScanner();
                resultHandler.handleSuccess(null, param);
            } catch (Throwable t) {
                resultHandler.handleFailure(t, param);
            }
        }
    }

    private UpdateFailedException notConfigured() {
        return new UpdateFailedException("No deployment repository named " + name + " is configured");
    }

}

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

import java.util.concurrent.TimeUnit;
import org.jboss.as.model.AbstractSubsystemUpdate;
import org.jboss.as.model.UpdateContext;
import org.jboss.as.model.UpdateFailedException;
import org.jboss.as.model.UpdateResultHandler;
import org.jboss.msc.service.ServiceTarget;

/**
 * Update adding a new {@code DeploymentScanner}.
 *
 * @author John E. Bailey
 */
public class DeploymentScannerAdd extends AbstractDeploymentScannerSubsystemUpdate {

    private static final long serialVersionUID = -1611269698053636197L;
    private static final String DEFAULT_NAME = "default";

    private final String name;
    private final String path;

    private final String relativeTo;
    private final int interval;
    private final boolean enabled;

    public DeploymentScannerAdd(final String name, String path, final String relativeTo, int interval, boolean enabled) {
        this.name = name;
        this.path = path;
        this.relativeTo = relativeTo;
        this.interval = interval;
        this.enabled = enabled;
    }

    /** {@inheritDoc} */
    @Override
    public <P> void applyUpdate(UpdateContext updateContext, UpdateResultHandler<? super Void,P> resultHandler, P param) {
        final ServiceTarget target = updateContext.getServiceTarget().subTarget();
        target.addListener(new UpdateResultHandler.ServiceStartListener<P>(resultHandler, param));
        DeploymentScannerService.addService(target, repositoryName(), relativeTo, path, interval, TimeUnit.MILLISECONDS, enabled);
    }

    public AbstractSubsystemUpdate<DeploymentScannerSubsystemElement, ?> getCompensatingUpdate(DeploymentScannerSubsystemElement original) {
        return new DeploymentScannerRemove(path);
    }

    protected void applyUpdate(DeploymentScannerSubsystemElement element) throws UpdateFailedException {
        final DeploymentScannerElement scannerElement = new DeploymentScannerElement();
        scannerElement.setEnabled(enabled);
        scannerElement.setInterval(interval);
        scannerElement.setName(name);
        scannerElement.setPath(path);
        scannerElement.setRelativeTo(relativeTo);
        element.addScanner(scannerElement);
    }

    private String repositoryName() {
        return name != null ? name : DEFAULT_NAME;
    }

}

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

import java.util.concurrent.TimeUnit;

import org.jboss.as.deployment.scanner.DeploymentScannerService;
import org.jboss.msc.service.BatchBuilder;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceTarget;

/**
 * Update adding a new {@code DeploymentRepositoryElement} to a {@code ServerModel}.
 *
 * @author Emanuel Muckenhuber
 */
public class ServerDeploymentRepositoryAdd extends AbstractServerModelUpdate<Void> {

    private static final long serialVersionUID = -1611269698053636197L;
    private static final String DEFAULT_NAME = "default";

    private final String path;
    private String name;
    private String relativeTo;
    private int interval = 0;
    private boolean enabled = true;

    public ServerDeploymentRepositoryAdd(String path, int interval, boolean enabled) {
        super(false, true);
        this.path = path;
        this.interval = interval;
        this.enabled = enabled;
    }

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getRelativePath() {
        return relativeTo;
    }
    public void setRelativeTo(String relativePath) {
        this.relativeTo = relativePath;
    }

    /** {@inheritDoc} */
    @Override
    protected void applyUpdate(ServerModel element) throws UpdateFailedException {
        final String repositoryName = repositoryName();
        if(! element.addDeploymentRepository(repositoryName)) {
            throw new UpdateFailedException("duplicate deployment repository " + repositoryName);
        }
        final DeploymentRepositoryElement repository = element.getDeploymentRepository(repositoryName);
        repository.setInterval(interval);
        repository.setEnabled(enabled);
        repository.setPath(path);
        repository.setRelativeTo(relativeTo);
    }

    /** {@inheritDoc} */
    @Override
    public AbstractServerModelUpdate<?> getCompensatingUpdate(ServerModel original) {
        return new ServerDeploymentRepositoryRemove(repositoryName());
    }

    /** {@inheritDoc} */
    @Override
    public <P> void applyUpdate(UpdateContext updateContext, UpdateResultHandler<? super Void,P> resultHandler, P param) {
        final ServiceTarget target = updateContext.getBatchBuilder().subTarget();
        target.addListener(new UpdateResultHandler.ServiceStartListener<P>(resultHandler, param));
        DeploymentScannerService.addService(target, repositoryName(), relativeTo, path, interval, TimeUnit.MILLISECONDS, enabled);
    }

    private String repositoryName() {
        return name != null ? name : DEFAULT_NAME;
    }

}

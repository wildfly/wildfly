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

import java.io.File;

import org.jboss.as.deployment.filesystem.FileSystemDeploymentService;
import org.jboss.msc.service.BatchBuilder;
import org.jboss.msc.service.BatchServiceBuilder;
import org.jboss.msc.service.ServiceRegistryException;

/**
 * Update adding a new {@code DeploymentRepositoryElement} to a {@code ServerModel}.
 *
 * @author Emanuel Muckenhuber
 */
public class ServerDeploymentRepositoryAdd extends AbstractServerModelUpdate<Void> {

    private static final long serialVersionUID = -1611269698053636197L;
    private final String path;
    private int interval = 0;
    private boolean enabled = true;

    public ServerDeploymentRepositoryAdd(String path, int interval, boolean enabled) {
        this.path = path;
        this.interval = interval;
        this.enabled = enabled;
    }

    /** {@inheritDoc} */
    protected void applyUpdate(ServerModel element) throws UpdateFailedException {
        if(! element.addDeploymentRepository(path)) {
            throw new UpdateFailedException("duplicate deployment repository " + path);
        }
        final DeploymentRepositoryElement repository = element.getDeploymentRepository(path);
        repository.setInterval(interval);
        repository.setEnabled(enabled);
    }

    /** {@inheritDoc} */
    public AbstractServerModelUpdate<?> getCompensatingUpdate(ServerModel original) {
        return new ServerDeploymentRepositoryRemove(path);
    }

    /** {@inheritDoc} */
    public <P> void applyUpdate(UpdateContext updateContext, UpdateResultHandler<? super Void,P> resultHandler, P param) {
        final BatchBuilder batch = updateContext.getBatchBuilder();
        // FIXME
        final String absolutePath = getAbsolutePath(path);
        final BatchServiceBuilder<?> builder = FileSystemDeploymentService.addService(batch, absolutePath, interval, enabled);
        builder.addListener(new UpdateResultHandler.ServiceStartListener<P>(resultHandler, param));
    }

    private String getAbsolutePath(String path) {
        if (File.separatorChar == '/') {
            if (path.startsWith(File.separator)) {
                return path;
            }
        }
        else if (path.indexOf(":\\") == 1) {
            return path;
        }
        // TODO. Yuck. Better would be to use ServerEnvironment
        String jbossHome = System.getProperty("jboss.home.dir");
        return jbossHome.endsWith(File.separator) ? jbossHome + path : jbossHome + File.separatorChar + path;
    }

}

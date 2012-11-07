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

package org.jboss.as.server.deploymentoverlay.service;

import org.jboss.as.repository.ContentRepository;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.vfs.VirtualFile;

/**
 * @author Stuart Douglas
 */
public class ContentService implements Service<ContentService> {

    public static final ServiceName SERVICE_NAME = DeploymentOverlayService.SERVICE_NAME.append("contentService");

    private final InjectedValue<ContentRepository> contentRepositoryInjectedValue = new InjectedValue<ContentRepository>();
    private final InjectedValue<DeploymentOverlayService> deploymentOverlayServiceInjectedValue = new InjectedValue<DeploymentOverlayService>();

    private final String path;
    private final byte[] contentHash;

    public ContentService(final String path, final byte[] contentHash) {
        this.path = path;
        this.contentHash = contentHash;
    }


    @Override
    public void start(final StartContext context) throws StartException {
        deploymentOverlayServiceInjectedValue.getValue().addContentService(this);
    }

    @Override
    public void stop(final StopContext context) {
        deploymentOverlayServiceInjectedValue.getValue().removeContentService(this);
    }

    @Override
    public ContentService getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    public VirtualFile getContentHash() {
        return contentRepositoryInjectedValue.getValue().getContent(contentHash);
    }

    public String getPath() {
        return path;
    }

    public InjectedValue<ContentRepository> getContentRepositoryInjectedValue() {
        return contentRepositoryInjectedValue;
    }

    public InjectedValue<DeploymentOverlayService> getDeploymentOverlayServiceInjectedValue() {
        return deploymentOverlayServiceInjectedValue;
    }
}

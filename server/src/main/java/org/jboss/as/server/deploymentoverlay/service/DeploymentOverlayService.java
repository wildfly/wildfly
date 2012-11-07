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

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * @author Stuart Douglas
 */
public class DeploymentOverlayService implements Service<DeploymentOverlayService> {

    public static final ServiceName SERVICE_NAME = DeploymentOverlayIndexService.SERVICE_NAME.append("deploymentOverlayService");

    private final Set<ContentService> contentServices = new CopyOnWriteArraySet<ContentService>();
    private final String name;

    public DeploymentOverlayService(final String name) {
        this.name = name;
    }

    @Override
    public void start(final StartContext context) throws StartException {
    }

    @Override
    public void stop(final StopContext context) {
    }

    public void addContentService(ContentService service) {
        contentServices.add(service);
    }

    public void removeContentService(ContentService service) {
        contentServices.remove(service);
    }

    @Override
    public DeploymentOverlayService getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    public Set<ContentService> getContentServices() {
        return contentServices;
    }

    public String getName() {
        return name;
    }
}

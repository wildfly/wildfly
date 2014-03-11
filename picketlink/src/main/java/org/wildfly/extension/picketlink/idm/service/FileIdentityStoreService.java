/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.picketlink.idm.service;

import org.jboss.as.controller.services.path.PathManager;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.picketlink.idm.config.FileStoreConfigurationBuilder;

/**
 * @author Pedro Igor
 */
public class FileIdentityStoreService implements Service<FileIdentityStoreService> {

    private final String relativeTo;
    private final String workingDir;
    private final FileStoreConfigurationBuilder fileStoreConfigurationBuilder;
    private InjectedValue<PathManager> pathManager = new InjectedValue<PathManager>();

    public FileIdentityStoreService(FileStoreConfigurationBuilder fileStoreConfigurationBuilder, String workingDir, String relativeTo) {
        this.fileStoreConfigurationBuilder = fileStoreConfigurationBuilder;
        this.workingDir = workingDir;
        this.relativeTo = relativeTo;
    }

    @Override
    public void start(StartContext startContext) throws StartException {
        String resolvedPath = getPathManager().getValue().resolveRelativePathEntry(this.workingDir, this.relativeTo);
        this.fileStoreConfigurationBuilder.workingDirectory(resolvedPath);
    }

    @Override
    public void stop(StopContext stopContext) {
    }

    @Override
    public FileIdentityStoreService getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    public InjectedValue<PathManager> getPathManager() {
        return this.pathManager;
    }
}

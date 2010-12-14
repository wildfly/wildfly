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

package org.jboss.as.service;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import org.jboss.as.deployment.ServerDeploymentRepository;
import org.jboss.as.server.deployment.module.TempFileProviderService;
import org.jboss.msc.service.BatchBuilder;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.vfs.VFS;
import org.jboss.vfs.VirtualFile;

/**
 * Mock implementation of ServerDeploymentRepository.
 *
 * @author Brian Stansberry
 */
public class MockServerDeploymentRepository implements ServerDeploymentRepository, Service<ServerDeploymentRepository> {

    private Set<Closeable> handles = new HashSet<Closeable>();
    private final File root;

    public static void addService(BatchBuilder batchBuilder, File root) {
        MockServerDeploymentRepository service = new MockServerDeploymentRepository(root);
        batchBuilder.addService(SERVICE_NAME, service).install();
    }
    public MockServerDeploymentRepository(File root) {
        this.root = root;
    }

    @Override
    public byte[] addDeploymentContent(String name, String runtimeName, InputStream stream) throws IOException {
        return null;
    }

    @Override
    public Closeable mountDeploymentContent(String name, String runtimeName, byte[] deploymentHash, VirtualFile mountPoint) throws IOException {
        Closeable handle = null;
        File content = new File(root, name);
        if (content.isFile()) {
            handle = VFS.mountZip(content, mountPoint, TempFileProviderService.provider());
        }
        else {
            handle = VFS.mountReal(content, mountPoint);
        }
        handles.add(handle);
        return handle;
    }

    @Override
    public void start(StartContext context) throws StartException {
    }

    @Override
    public void stop(StopContext context) {
        for (Closeable handle : handles) {
            try {
                handle.close();
            } catch (IOException e) {
                e.printStackTrace();
            };
        }
    }

    @Override
    public ServerDeploymentRepository getValue() throws IllegalStateException {
        return this;
    }

}

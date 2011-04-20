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

package org.jboss.as.server.deployment.impl;

import org.jboss.as.server.deployment.api.ServerDeploymentRepository;
import org.jboss.logging.Logger;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.threads.JBossThreadFactory;
import org.jboss.vfs.TempFileProvider;
import org.jboss.vfs.VFS;
import org.jboss.vfs.VFSUtils;
import org.jboss.vfs.VirtualFile;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.AccessController;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.util.concurrent.Executors;

import static org.jboss.as.server.deployment.impl.ContentRepositoryImpl.safeClose;

/**
 * Default implementation of {@link ServerDeploymentRepository}.
 *
 * @author Brian Stansberry
 */
public class ServerDeploymentRepositoryImpl implements ServerDeploymentRepository, Service<ServerDeploymentRepository> {

    private static final Logger log = Logger.getLogger("org.jboss.as.server.deployment");
    private static final String EXTERNAL = "external";
    private final File systemDeployDir;
    private TempFileProvider tempFileProvider;
    private final ContentRepositoryImpl contentRepository;


    public static void addService(final ServiceTarget serviceTarget, final File repoRoot, final File systemDeployDir, final ContentRepositoryImpl contentRepository) {
        serviceTarget.addService(ServerDeploymentRepository.SERVICE_NAME,
                new ServerDeploymentRepositoryImpl(repoRoot, systemDeployDir, contentRepository))
                .install();
    }

    /**
     * Creates a new ServerDeploymentRepositoryImpl.
     */
    public ServerDeploymentRepositoryImpl(final File repoRoot, final File systemDeployDir, final ContentRepositoryImpl contentRepository) {
        this.systemDeployDir = systemDeployDir;
        this.contentRepository = contentRepository;
    }

    /** {@inheritDoc} */
    @Override
    public byte[] addExternalFileReference(File file) throws IOException {
        byte[] sha1Bytes = null;
        final String fileName = file.getAbsolutePath();
        final MessageDigest messageDigest = contentRepository.messageDigest;
        synchronized(messageDigest) {
            messageDigest.reset();
            if(! file.exists()) {
                throw new FileNotFoundException(fileName);
            }
            final OutputStream os = new OutputStream() {
                public void write(int b) throws IOException {
                    //
                }
            };
            final DigestOutputStream dos = new DigestOutputStream(os, messageDigest);
            calculateHash(file, dos);
            sha1Bytes = messageDigest.digest();
        }
        final File content = getExternalFileReference(sha1Bytes, true);
        final OutputStream os = new FileOutputStream(content);
        try {
            os.write(fileName.getBytes());
            os.flush();
            os.close();
        } finally {
            safeClose(os);
        }
        return sha1Bytes;
    }

    void calculateHash(final File file, final OutputStream os) throws IOException {
        os.write(file.getAbsolutePath().getBytes());
        if(file.isDirectory()) {
            for(File f : file.listFiles()) {
                calculateHash(f, os);
            }
        } else {
            final InputStream is = new FileInputStream(file);
            try {
                VFSUtils.copyStreamAndClose(is, os);
            } finally {
                safeClose(is);
            }
        }
    }

    public boolean hasDeploymentContent(byte[] hash) {
        if(getExternalFileReference(hash, false).exists()) {
            return true;
        }
        return contentRepository.hasContent(hash);
    }

    private File getExternalFileReference(byte[] deploymentHash, boolean validate) {
        final File hashDir = contentRepository.getDeploymentHashDir(deploymentHash, validate);
        return new File(hashDir, EXTERNAL);
    }

    public Closeable mountDeploymentContent(String name, String runtimeName, final VirtualFile contents, VirtualFile mountPoint) throws IOException {
        return mountDeploymentContent(name, runtimeName, contents, mountPoint, false);
    }

    @Override
    public Closeable mountDeploymentContent(String name, String runtimeName, final VirtualFile contents, VirtualFile mountPoint, boolean mountExpanded) throws IOException {
        // according to the javadoc contents can not be null
        if (contents == null)
            throw new IllegalArgumentException("contents is null");
        if(mountExpanded) {
            return VFS.mountZipExpanded(contents, mountPoint, tempFileProvider);
        } else {
            return VFS.mountZip(contents, mountPoint, tempFileProvider);
        }
    }

    @Override
    public void start(StartContext context) throws StartException {
        try {
            final JBossThreadFactory threadFactory = new JBossThreadFactory(new ThreadGroup("ServerDeploymentRepository-temp-threads"), Boolean.FALSE, null, "%G - %t", null, null, AccessController.getContext());
            tempFileProvider = TempFileProvider.create("temp", Executors.newScheduledThreadPool(2, threadFactory));
        } catch (IOException e) {
            throw new StartException("Failed to create temp file provider");
        }
        log.debugf("%s started", ServerDeploymentRepository.class.getSimpleName());
    }

    @Override
    public void stop(StopContext context) {
        log.debugf("%s stopped", ServerDeploymentRepository.class.getSimpleName());
    }


    @Override
    public ServerDeploymentRepository getValue() throws IllegalStateException {
        return this;
    }

}

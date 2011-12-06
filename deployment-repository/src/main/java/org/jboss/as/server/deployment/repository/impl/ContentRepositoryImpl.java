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

package org.jboss.as.server.deployment.repository.impl;

import static org.jboss.as.server.deployment.repository.impl.DeploymentRepositoryLogger.ROOT_LOGGER;
import static org.jboss.as.server.deployment.repository.impl.DeploymentRepositoryMessages.MESSAGES;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.jboss.as.server.deployment.repository.api.ContentRepository;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.vfs.VFS;
import org.jboss.vfs.VirtualFile;

/**
 * Default implementation of {@link org.jboss.as.server.deployment.repository.api.ContentRepository}.
 * @author John Bailey
 */
public class ContentRepositoryImpl implements ContentRepository, Service<ContentRepository> {

    protected static final String CONTENT = "content";
    private final File repoRoot;
    protected final MessageDigest messageDigest;

    // TODO: return void
    public static ContentRepositoryImpl addService(final ServiceTarget serviceTarget, final File repoRoot) {
        ContentRepositoryImpl contentRepository = new ContentRepositoryImpl(repoRoot);
        serviceTarget.addService(ContentRepository.SERVICE_NAME, contentRepository).install();
        return contentRepository;
    }

    protected ContentRepositoryImpl(final File repoRoot) {
        if (repoRoot == null)
            throw MESSAGES.nullVar("repoRoot");
        if (repoRoot.exists()) {
            if (!repoRoot.isDirectory()) {
                throw MESSAGES.notADirectory(repoRoot.getAbsolutePath());
            }
            else if (!repoRoot.canWrite()) {
                throw MESSAGES.directoryNotWritable(repoRoot.getAbsolutePath());
            }
        }
        else if (!repoRoot.mkdirs()) {
            throw MESSAGES.cannotCreateDirectory(repoRoot.getAbsolutePath());
        }
        this.repoRoot = repoRoot;

        try {
            this.messageDigest = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            throw MESSAGES.cannotObtainSha1(e, MessageDigest.class.getSimpleName());
        }
    }

    @Override
    public byte[] addContent(InputStream stream) throws IOException {
        byte[] sha1Bytes = null;
        File tmp = File.createTempFile(CONTENT, "tmp", repoRoot);
        FileOutputStream fos = new FileOutputStream(tmp);
        synchronized (messageDigest) {
            messageDigest.reset();
            try {
                DigestOutputStream dos = new DigestOutputStream(fos, messageDigest);
                BufferedInputStream bis = new BufferedInputStream(stream);
                byte[] bytes = new byte[8192];
                int read;
                while ((read = bis.read(bytes)) > -1) {
                    dos.write(bytes, 0, read);
                }
            }
            finally {
                safeClose(fos);
            }
            sha1Bytes = messageDigest.digest();
        }
        final File realFile = getDeploymentContentFile(sha1Bytes, true);
        if(hasContent(sha1Bytes)) {
            // we've already got this content
            if (!tmp.delete()) {
                tmp.deleteOnExit();
            }
            ROOT_LOGGER.debugf("Content was already present in repository at location %s", realFile.getAbsolutePath());
        } else {
            moveTempToPermanent(tmp, realFile);
            ROOT_LOGGER.contentAdded(realFile.getAbsolutePath());
        }

        return sha1Bytes;
    }

    @Override
    public VirtualFile getContent(byte[] hash) {
        if (hash == null)
            throw MESSAGES.nullVar("hash");
        return VFS.getChild(getDeploymentContentFile(hash, true).toURI());
    }

    @Override
    public boolean hasContent(byte[] hash) {
        return getDeploymentContentFile(hash).exists();
    }

    protected File getRepoRoot() {
        return repoRoot;
    }

    protected File getDeploymentContentFile(byte[] deploymentHash) {
        return getDeploymentContentFile(deploymentHash, false);
    }

    private File getDeploymentContentFile(byte[] deploymentHash, boolean validate) {
        final File hashDir = getDeploymentHashDir(deploymentHash, validate);
        return new File(hashDir, CONTENT);
    }

    protected File getDeploymentHashDir(final byte[] deploymentHash, final boolean validate) {
        final String sha1 = HashUtil.bytesToHexString(deploymentHash);
        final String partA = sha1.substring(0,2);
        final String partB = sha1.substring(2);
        final File base = new File(getRepoRoot(), partA);
        if (validate) {
            validateDir(base);
        }
        final File hashDir = new File(base, partB);
        if (validate && !hashDir.exists() && !hashDir.mkdirs()) {
            throw MESSAGES.cannotCreateDirectory(hashDir.getAbsolutePath());
        }
        return hashDir;
    }

    protected void validateDir(File dir) {
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                throw MESSAGES.cannotCreateDirectory(dir.getAbsolutePath());
            }
        } else if (!dir.isDirectory()) {
            throw MESSAGES.notADirectory(dir.getAbsolutePath());
        } else if (!dir.canWrite()) {
            throw MESSAGES.directoryNotWritable(dir.getAbsolutePath());
        }
    }

    private void moveTempToPermanent(File tmpFile, File permanentFile) throws IOException {

        if (!tmpFile.renameTo(permanentFile)) {
            FileOutputStream fos = null;
            BufferedOutputStream bos = null;
            FileInputStream fis = null;
            try {
                fos = new FileOutputStream(permanentFile);
                bos = new BufferedOutputStream(fos);
                fis = new FileInputStream(tmpFile);
                BufferedInputStream bis = new BufferedInputStream(fis);
                byte[] bytes = new byte[8192];
                int read;
                while ((read = bis.read(bytes)) > -1) {
                    bos.write(bytes, 0, read);
                }
            } finally {
                safeClose(bos);
                safeClose(fos);
                safeClose(fis);
                if (!tmpFile.delete()) {
                    tmpFile.deleteOnExit();
                }
            }
        }
    }

    @Override
    public void removeContent(byte[] hash) {
        File file = getDeploymentContentFile(hash, true);
        if(!file.delete()) {
            file.deleteOnExit();
        }
        File parent = file.getParentFile();
        if (!parent.delete()) {
            parent.deleteOnExit();
        }
        parent = parent.getParentFile();
        if (parent.list().length == 0) {
            if (!parent.delete()) {
                parent.deleteOnExit();
            }
        }
        ROOT_LOGGER.contentRemoved(file.getAbsolutePath());
    }

    protected static void safeClose(final Closeable closeable) {
        if(closeable != null) {
            try {
                closeable.close();
            } catch(Exception ignore) {
                //
            }
        }
    }

    @Override
    public void start(StartContext context) throws StartException {
        ROOT_LOGGER.debugf("%s started", ContentRepository.class.getSimpleName());
    }

    @Override
    public void stop(StopContext context) {
        ROOT_LOGGER.debugf("%s stopped", ContentRepository.class.getSimpleName());
    }

    @Override
    public ContentRepository getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }
}

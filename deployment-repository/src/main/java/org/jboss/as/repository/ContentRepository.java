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

package org.jboss.as.repository;

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

import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.vfs.VFS;
import org.jboss.vfs.VirtualFile;

/**
 * Repository for deployment content and other managed content.
 *
 * @author John Bailey
 */
public interface ContentRepository {

    /**
     * Standard ServiceName under which a service controller for an instance of
     * @code Service<ContentRepository> would be registered.
     */
    ServiceName SERVICE_NAME = ServiceName.JBOSS.append("content-repository");

    /**
     * Add the given content to the repository.
     *
     * @param stream stream from which the content can be read. Cannot be <code>null</code>
     * @return the hash of the content that will be used as an internal identifier
     *         for the content. Will not be <code>null</code>
     * @throws IOException if there is a problem reading the stream
     */
    byte[] addContent(InputStream stream) throws IOException;

    /**
     * Get the content as a virtual file.
     *
     * @param hash the hash. Cannot be {@code null}
     *
     * @return the content as a virtual file
     */
    VirtualFile getContent(byte[] hash);

    /**
     * Gets whether content with the given hash is stored in the repository.
     *
     * @param hash the hash. Cannot be {@code null}
     *
     * @return {@code true} if the repository has content with the given hash
     */
    boolean hasContent(byte[] hash);

    /**
     * Remove the given content from the repository.
     *
     * @param hash the hash. Cannot be {@code null}
     */
    void removeContent(byte[] hash);

    static class Factory {

        public static void addService(final ServiceTarget serviceTarget, final File repoRoot) {
            ContentRepositoryImpl contentRepository = new ContentRepositoryImpl(repoRoot);
            serviceTarget.addService(SERVICE_NAME, contentRepository).install();
        }

        public static ContentRepository create(final File repoRoot) {
            return new ContentRepositoryImpl(repoRoot);
        }

        /**
         * Default implementation of {@link ContentRepository}.
         * @author John Bailey
         */
        private static class ContentRepositoryImpl implements ContentRepository, Service<ContentRepository> {

            protected static final String CONTENT = "content";
            private final File repoRoot;
            protected final MessageDigest messageDigest;

            protected ContentRepositoryImpl(final File repoRoot) {
                if (repoRoot == null)
                    throw DeploymentRepositoryMessages.MESSAGES.nullVar("repoRoot");
                if (repoRoot.exists()) {
                    if (!repoRoot.isDirectory()) {
                        throw DeploymentRepositoryMessages.MESSAGES.notADirectory(repoRoot.getAbsolutePath());
                    }
                    else if (!repoRoot.canWrite()) {
                        throw DeploymentRepositoryMessages.MESSAGES.directoryNotWritable(repoRoot.getAbsolutePath());
                    }
                }
                else if (!repoRoot.mkdirs()) {
                    throw DeploymentRepositoryMessages.MESSAGES.cannotCreateDirectory(repoRoot.getAbsolutePath());
                }
                this.repoRoot = repoRoot;

                try {
                    this.messageDigest = MessageDigest.getInstance("SHA-1");
                } catch (NoSuchAlgorithmException e) {
                    throw DeploymentRepositoryMessages.MESSAGES.cannotObtainSha1(e, MessageDigest.class.getSimpleName());
                }
            }

            @Override
            public byte[] addContent(InputStream stream) throws IOException {
                byte[] sha1Bytes;
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
                        fos.flush();
                        fos.getFD().sync();
                        fos.close();
                        fos = null;
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
                    DeploymentRepositoryLogger.ROOT_LOGGER.debugf("Content was already present in repository at location %s", realFile.getAbsolutePath());
                } else {
                    moveTempToPermanent(tmp, realFile);
                    DeploymentRepositoryLogger.ROOT_LOGGER.contentAdded(realFile.getAbsolutePath());
                }

                return sha1Bytes;
            }

            @Override
            public VirtualFile getContent(byte[] hash) {
                if (hash == null)
                    throw DeploymentRepositoryMessages.MESSAGES.nullVar("hash");
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
                    throw DeploymentRepositoryMessages.MESSAGES.cannotCreateDirectory(hashDir.getAbsolutePath());
                }
                return hashDir;
            }

            protected void validateDir(File dir) {
                if (!dir.exists()) {
                    if (!dir.mkdirs()) {
                        throw DeploymentRepositoryMessages.MESSAGES.cannotCreateDirectory(dir.getAbsolutePath());
                    }
                } else if (!dir.isDirectory()) {
                    throw DeploymentRepositoryMessages.MESSAGES.notADirectory(dir.getAbsolutePath());
                } else if (!dir.canWrite()) {
                    throw DeploymentRepositoryMessages.MESSAGES.directoryNotWritable(dir.getAbsolutePath());
                }
            }

            private void moveTempToPermanent(File tmpFile, File permanentFile) throws IOException {

                if (!tmpFile.renameTo(permanentFile)) {
                    // AS7-3574. Try to avoid writing the permanent file bit by bit in we crash in the middle.
                    // Copy tmpFile to another tmpfile in the same dir as the permanent file (and thus same filesystem)
                    // and see then if we can rename it.
                    File localTmp = new File(permanentFile.getParent(), "tmp");
                    try {
                        copyFile(tmpFile, localTmp);
                        if (!localTmp.renameTo(permanentFile)) {
                            // No luck; need to copy
                            copyFile(localTmp, permanentFile);
                        }
                    } catch (IOException e) {
                        if (permanentFile.exists()) {
                            permanentFile.delete();
                        }
                        throw e;
                    } catch (RuntimeException e) {
                        if (permanentFile.exists()) {
                            permanentFile.delete();
                        }
                        throw e;

                    } finally {
                        if (!tmpFile.delete()) {
                            tmpFile.deleteOnExit();
                        }
                        if (localTmp.exists() && !localTmp.delete()) {
                            localTmp.deleteOnExit();
                        }
                    }
                }
            }

            private void copyFile(File src, File dest) throws IOException {
                FileOutputStream fos = null;
                FileInputStream fis = null;
                try {
                    fos = new FileOutputStream(dest);
                    fis = new FileInputStream(src);
                    byte[] bytes = new byte[8192];
                    int read;
                    while ((read = fis.read(bytes)) > -1) {
                        fos.write(bytes, 0, read);
                    }
                    fos.flush();
                    fos.getFD().sync();
                    fos.close();
                    fos = null;
                } finally {
                    safeClose(fos);
                    safeClose(fis);
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
                DeploymentRepositoryLogger.ROOT_LOGGER.contentRemoved(file.getAbsolutePath());
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
                DeploymentRepositoryLogger.ROOT_LOGGER.debugf("%s started", ContentRepository.class.getSimpleName());
            }

            @Override
            public void stop(StopContext context) {
                DeploymentRepositoryLogger.ROOT_LOGGER.debugf("%s stopped", ContentRepository.class.getSimpleName());
            }

            @Override
            public ContentRepository getValue() throws IllegalStateException, IllegalArgumentException {
                return this;
            }
        }

    }
}

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

package org.jboss.as.domain.controller;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.jboss.as.deployment.ServerDeploymentRepository;
import org.jboss.logging.Logger;

/**
 * Default implementation of {@link ServerDeploymentRepository}.
 *
 * @author Brian Stansberry
 */
public class DomainDeploymentRepository {

    private static final Logger log = Logger.getLogger("org.jboss.as.domain.controller");

    private static final String CONTENT = "content";
    private File repoRoot;
    private MessageDigest messageDigest;

    /**
     * Creates a new DomainDeploymentRepository.
     */
    public DomainDeploymentRepository(File deployDir) {

        if (deployDir == null)
            throw new IllegalArgumentException("deployDir is null");
        if (deployDir.exists()) {
            if (!deployDir.isDirectory()) {
                throw new IllegalStateException("Deployment repository root " + deployDir.getAbsolutePath() + " is not a directory");
            }
            else if (!deployDir.canWrite()) {
                throw new IllegalStateException("Deployment repository root " + deployDir.getAbsolutePath() + " is not a writable");
            }
        }
        else if (!deployDir.mkdirs()) {
            throw new IllegalStateException("Failed to create a directory at " + deployDir.getAbsolutePath());
        }

        this.repoRoot = deployDir;

        try {
            this.messageDigest = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Cannot obtain SHA-1 " + MessageDigest.class.getSimpleName(), e);
        }
    }


    public byte[] addDeploymentContent(String name, String runtimeName, InputStream stream)
            throws IOException {

        log.debugf("Adding content with name %s", name);

        byte[] sha1Bytes = null;
        File tmp = File.createTempFile(name, "tmp", repoRoot);
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
                try { fos.close(); } catch (Exception ignored) {}
            }
            sha1Bytes = messageDigest.digest();
        }
        String sha1 = bytesToHexString(sha1Bytes);
        String partA = sha1.substring(0,2);
        String partB = sha1.substring(2);
        File base = new File(repoRoot, partA);
        validateDir(base);
        File realDir = new File(base, partB);
        if (!realDir.exists() && !realDir.mkdirs()) {
            throw new IllegalStateException("Cannot create directory " + realDir.getAbsolutePath());
        }
        File realFile = new File(realDir, CONTENT);
        if (realFile.exists()) {
            // we've already got this content
            if (!tmp.delete()) {
                tmp.deleteOnExit();
            }
            log.debugf("Content with name %s was already present in repository at location %s" , name, realFile.getAbsolutePath());
        }
        else {
            moveTempToPermanent(tmp, realFile);
            log.infof("Content with name %s added at location %s" , name, realFile.getAbsolutePath());
        }

        return sha1Bytes;
    }

    private void validateDir(File dir) {
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                throw new IllegalStateException("Cannot create directory " + dir.getAbsolutePath());
            }
        }
        else if (!dir.isDirectory()) {
            throw new IllegalStateException(dir.getAbsolutePath() + " is not a directory");
        }
        else if (!dir.canWrite()) {
            throw new IllegalStateException("Cannot write to directory " + dir.getAbsolutePath());
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
            }
            finally {
                try {
                    if (bos != null) {
                        bos.close();
                    }
                 } catch (Exception ignored) {}
                 try {
                     if (fos != null) {
                         fos.close();
                     }
                  } catch (Exception ignored) {}
                  try {
                      if (fis != null) {
                          fis.close();
                      }
                  } catch (Exception ignored) {}

                  if (!tmpFile.delete()) {
                      tmpFile.deleteOnExit();
                  }
            }
        }
    }

    // TODO move this sha1 translation stuff to a general utility class
    private static char[] table = {
            '0', '1', '2', '3', '4', '5', '6', '7',
            '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
    };

    /**
     * Convert a byte array into a hex string.
     *
     * @param bytes the bytes
     * @return the string
     */
    protected static String bytesToHexString(final byte[] bytes) {
        final StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            builder.append(table[b >> 4 & 0x0f]).append(table[b & 0x0f]);
        }
        return builder.toString();
    }

}

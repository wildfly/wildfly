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

package org.jboss.as.patching.generator;

import static org.jboss.as.patching.generator.PatchGenerator.processingError;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Set;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import org.jboss.as.patching.HashUtils;

/**
 * File based content item implementation.
 *
 * @author Emanuel Muckenhuber
 */
class DistributionItemFileImpl extends DistributionContentItem {

    private final File file;
    private final Set<DistributionContentItem> children;
    private byte[] cachedMetadataHash = null;

    protected DistributionItemFileImpl(File file, DistributionContentItem parent) {
        this(file, parent, file.getName());
    }

    protected DistributionItemFileImpl(File file, DistributionContentItem parent, String name) {
        super(parent, name);
        this.file = file;
        if (file.isDirectory()) {
            children = new TreeSet<DistributionContentItem>();
        } else {
            children = NO_CHILDREN;
        }
    }

    @Override
    public String getName() {
        return file.getName();
    }

    @Override
    public byte[] getMetadataHash() {
        try {
            if (cachedMetadataHash == null) {
                cachedMetadataHash = HashUtils.hashFile(file);
            }
            return cachedMetadataHash;
        } catch (IOException e) {
            throw processingError(e, "failed to generate hash");
        }
    }

    @Override
    public byte[] getComparisonHash() {
        final String name = file.getName();
        try {
            if (name.endsWith(".jar")) {
                return internalJarComparison(file);
            } else {
                return getMetadataHash();
            }
        } catch (Exception e) {
            throw processingError(e, "failed to generate hash");
        }
    }

    @Override
    public boolean isLeaf() {
        return file.isFile();
    }

    @Override
    public Set<DistributionContentItem> getChildren() {
        return children;
    }

    /**
     * Internally compute the hash used for jar comparison.
     * <p/>
     * https://github.com/beachhouse/jboss-beach-jar-digest
     * <p/>
     * TODO we also need a way to do something similar for modules, since a hash for jar is not enough. We probably
     * need to include all .jars, resources and parts of module.xml (not <resources /> which are by name changes).
     *
     * @param file the jar file
     * @return the hash
     * @throws NoSuchAlgorithmException
     * @throws IOException
     */
    protected static byte[] internalJarComparison(final File file) throws NoSuchAlgorithmException, IOException {
        // TODO: make the algorithm choice configurable
        final MessageDigest jarDigest = MessageDigest.getInstance("SHA1");
        final MessageDigest digest = MessageDigest.getInstance("SHA1");
        final JarInputStream in = new JarInputStream(new BufferedInputStream(new FileInputStream(file)));
        try {
            JarEntry entry;
            while ((entry = in.getNextJarEntry()) != null) {
                // do not hash directories
                if (entry.isDirectory()) {
                    continue;
                }
                final String name = entry.getName();
                // do not hash information added by jarsigner
                if (name.startsWith("META-INF/")) {
                    if (name.endsWith(".SF") || name.endsWith(".DSA"))
                        continue;
                }
                if (name.equals("META-INF/INDEX.LIST")) {
                    continue;
                }
                // do not hash timestamped maven artifacts
                // TODO: make this optional, enabled by default
                if (name.startsWith("META-INF/maven/") && name.endsWith("/pom.properties")) {
                    continue;
                }
                digest.reset();
                final byte[] buf = new byte[4096];
                int l;
                while ((l = in.read(buf)) > 0) {
                    digest.update(buf, 0, l);
                }
                final byte[] d = digest.digest();
                jarDigest.update(d);
            }
        } finally {
            in.close();
        }
        return jarDigest.digest();
    }

}

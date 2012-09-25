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

package org.jboss.as.patching.runner;

import org.jboss.as.boot.DirectoryStructure;
import org.jboss.as.patching.api.Patch;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

/**
 * @author Emanuel Muckenhuber
 */
public class PatchingRecord {

    final ContentItem item;
    byte[] backupHash;
    byte[] sourceHash;

    public PatchingRecord(ContentItem item) {
        this.item = item;
    }

    protected void prepare(final File source, final File target) throws IOException {
        // Backup the original in the history directory
        if(source.exists()) {
            final FileInputStream is = new FileInputStream(source);
            try {
                backupHash = copy(is, target);
                is.close();
            } finally {
                PatchUtils.safeClose(is);
            }
        }
    }

    protected void execute(PatchContentLoader loader, final File target) throws IOException {
        final InputStream is = loader.openContentStream(item);
        try {
            sourceHash = copy(is, target);
        } finally {
            PatchUtils.safeClose(is);
        }
    }

    protected void undo(final Patch patch, final PatchingContext context) throws IOException {
        final String patchId = patch.getPatchId();
        final DirectoryStructure structure = context.getStructure();
        final File jbossHome = structure.getInstalledImage().getJbossHome();
        final File historyDir = structure.getHistoryDir(patch.getPatchId());
        final File source = getTargetFile(jbossHome, item);
        final File backup = getTargetFile(historyDir, item);
        if(backup.exists()) {
            if(source.exists()) {
                // Check if the source is still the same we copied
                byte[] hash = PatchUtils.calculateHash(source);
                if(! Arrays.equals(sourceHash, hash)) {
                    throw new IllegalStateException();
                }
            }
            // Copy
            PatchUtils.copyFile(backup, source);
        }
    }

    static File getTargetFile(final File root, final ContentItem item)  {
        File file = root;
        for(final String path : item.getPath()) {
            file = new File(file, path);
        }
        return new File(file, item.getName());
    }

    static byte[] copy(final InputStream is, final File target) throws IOException {
        final OutputStream os = new FileOutputStream(target);
        try {
            byte[] nh = PatchUtils.copyAndGetHash(is, os);
            os.close();
            return nh;
        } finally {
            PatchUtils.safeClose(os);
        }
    }

}

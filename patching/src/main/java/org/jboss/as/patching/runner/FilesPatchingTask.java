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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author Emanuel Muckenhuber
 */
public class FilesPatchingTask implements PatchingTask {

    private final Collection<ContentItem> contentItems;
    public FilesPatchingTask(Collection<ContentItem> contentItems) {
        this.contentItems = contentItems;
    }

    @Override
    public void execute(final Patch patch, final PatchingContext context) throws IOException {
        //
        final DirectoryStructure structure = context.getStructure();
        final File jbossHome = structure.getInstalledImage().getJbossHome();
        final File historyDir = structure.getHistoryDir(patch.getPatchId());
        final List<PatchingRecord> records = new ArrayList<PatchingRecord>();
        // Backup the content
        for(final ContentItem item : contentItems) {

            // Get the target
            final File target = PatchingRecord.getTargetFile(jbossHome, item);
            final File backup = PatchingRecord.getTargetFile(historyDir, item);
            final PatchingRecord record = new PatchingRecord(item);
            // Backup
            record.prepare(target, backup);
            records.add(record);
        }
        // Replace the
        final PatchContentLoader loader = context.getLoader();
        for(final PatchingRecord record : records) {
            context.record(record);
            final File target = PatchingRecord.getTargetFile(jbossHome, record.item);
            record.execute(loader, target);
        }
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

    static FilesPatchingTask createTask(final File file) {
        final Collection<ContentItem> contentItems = PatchContents.getContents(file);
        return new FilesPatchingTask(contentItems);
    }

}

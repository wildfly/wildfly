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

import static org.jboss.as.patching.runner.PatchUtils.copyAndGetHash;
import static org.jboss.as.patching.runner.PatchUtils.safeClose;

import org.jboss.as.patching.PatchMessages;
import org.jboss.as.patching.metadata.ContentItem;
import org.jboss.as.patching.metadata.ContentModification;
import org.jboss.as.patching.metadata.MiscContentItem;
import org.jboss.as.patching.metadata.ModificationType;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.DigestInputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Task for removing a file. In case the removed file is a directory, this task will create multiple rollback operation
 * for each file in this folder.
 *
 * @author Emanuel Muckenhuber
 */
class FileRemoveTask implements PatchingTask {

    private final MiscContentItem item;
    private final File target;
    private final File backup;
    private final ContentModification modification;

    private final List<ContentModification> rollback = new ArrayList<ContentModification>();

    FileRemoveTask(MiscContentItem item, File target, File backup, ContentModification modification) {
        this.item = item;
        this.target = target;
        this.backup = backup;
        this.modification = modification;
    }

    @Override
    public ContentItem getContentItem() {
        return item;
    }

    @Override
    public boolean prepare(PatchingContext context) throws IOException {
        // See if the hash matches the metadata
        final byte[] expected = modification.getTargetHash();
        final byte[] actual = PatchUtils.calculateHash(target);
        if (Arrays.equals(expected, actual)) {
            backup(target, Collections.<String>emptyList(), rollback);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void execute(PatchingContext context) throws IOException {
        // delete the file or directory recursively
        boolean ok = PatchUtils.recursiveDelete(target);
        for(ContentModification mod : rollback) {
            // Add the rollback (add actions)
            context.recordRollbackAction(mod);
        }
        if(! ok) {
            throw PatchMessages.MESSAGES.failedToDelete(target.getAbsolutePath());
        }
    }

    void backup(final File root, final List<String> path, final List<ContentModification> rollback) throws IOException {
        if(!root.exists()) {
            // Perhaps an error condition?
        } else if(root.isDirectory()) {
            final File[] files = root.listFiles();
            if(files.length == 0) {
                // Create empty directory
                rollback.add(createRollbackItem(root.getName(), path, NO_CONTENT, true));
            } else {
                for (File file : files) {
                    final List<String> newPath = new ArrayList<String>(path);
                    newPath.add(file.getName());
                    backup(file, newPath, rollback);
                }
            }
        } else {
            // Copy and record the backup action
            final byte[] hash = copy(root, backup);
            rollback.add(createRollbackItem(root.getName(), path, hash, false));
        }
    }

    static ContentModification createRollbackItem(String name, List<String> path,  byte[] backupHash, boolean directory) {
        final MiscContentItem backupItem = new MiscContentItem(name, path, backupHash, directory);
        return new ContentModification(backupItem, NO_CONTENT, ModificationType.ADD);
    }

    static byte[] copy(File source, File target) throws IOException {
        final FileInputStream is = new FileInputStream(source);
        try {
            return copy(is, target);
        } finally {
            safeClose(is);
        }
    }

    static byte[] copy(final InputStream is, final File target) throws IOException {
        if(! target.getParentFile().exists()) {
            target.getParentFile().mkdirs(); // Hmm
        }
        final OutputStream os = new FileOutputStream(target);
        try {
            return copyAndGetHash(is, os);
        } finally {
            safeClose(os);
        }
    }

}

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
import org.jboss.as.patching.PatchInfo;
import org.jboss.as.patching.PatchLogger;
import org.jboss.as.patching.PatchMessages;
import org.jboss.as.patching.metadata.MiscContentItem;
import org.jboss.as.patching.metadata.MiscContentModification;
import org.jboss.as.patching.metadata.Patch;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * @author Emanuel Muckenhuber
 */
public class PatchingTaskRunner {

    private final PatchInfo patchInfo;
    private final DirectoryStructure structure;
    public PatchingTaskRunner(final PatchInfo patchInfo, final DirectoryStructure structure) {
        this.patchInfo = patchInfo;
        this.structure = structure;
    }

    public PatchingResult execute(final Patch patch, final InputStream content) throws PatchingException {
        // Check if we can apply this patch
        final String patchId = patch.getPatchId();
        final List<String> appliesTo = patch.getAppliesTo();
        if(! appliesTo.contains(patchInfo.getVersion())) {
            throw PatchMessages.MESSAGES.doesNotApply(appliesTo, patchInfo.getVersion());
        }
        // Create the work directory
        final File workDir = new File(structure.getHistoryDir(patchId), "work");
        if(workDir.exists()) {
            // Remove cached contents
            recursiveDelete(workDir);
        }
        workDir.mkdirs();
        final PatchContentLoader loader = new PatchContentLoader(workDir);
        final PatchingContext context = new PatchingContext(patch, patchInfo, structure, loader);
        final File cachedContent = new File(workDir, "content");
        try {
            FileOutputStream os = null;
            try {
                // Cache the content first
                os = new FileOutputStream(cachedContent);
                PatchUtils.copyStream(content, os);
                os.close();
            } catch (IOException e) {
                throw new PatchingException(e);
            } finally {
                PatchUtils.safeClose(os);
            }
            try {
                // Unpack to the work dir
                unpack(cachedContent, workDir);
            } catch (IOException e) {
                throw new PatchingException(e);
            }
            // Create the overlay directory
            final File modules = new File(workDir, PatchContents.MODULES);
            if(modules.exists()) {
                // patches/patch-id/...
                final File patchDir = structure.getModulePatchDirectory(patchId);
                if(! modules.renameTo(patchDir)) {
                    throw new PatchingException("...");
                }
            }
            // Create the modification tasks
            final List<ContentTask> tasks = new ArrayList<ContentTask>();
            final List<MiscContentItem> problems = new ArrayList<MiscContentItem>();
            for(final MiscContentModification modification : patch.getModifications()) {
                final ContentTask task = ContentTask.Factory.create(modification, context);
                try {
                    // backup
                    if(! task.prepare(context)) {
                        // In case the file was modified we report a problem
                        final MiscContentItem item = modification.getItem();
                        // Unless it was ignored (or excluded)
                        if(context.isIgnored(item)) {
                            problems.add(item);
                        }
                    }
                    tasks.add(task);
                } catch (IOException e) {
                    throw new PatchingException(e);
                }
            }
            // If there were problems report them
            if(! problems.isEmpty()) {
                // TODO
                throw new PatchingException("...");
            }
            //
            final List<MiscContentModification> rollbackTasks = new ArrayList<MiscContentModification>();
            try {
                // Execute the tasks
                for(final ContentTask task : tasks) {
                    // Unless it's excluded by the user
                    if(context.isExcluded(task.getContentItem())) {
                        continue;
                    }
                    // Record the rollback task
                    final MiscContentModification rollback = task.execute(context);
                    rollbackTasks.add(rollback);
                }
            } catch (Exception e) {
                // Rollback
                e.printStackTrace();
            }
            // Finish..
            return context.finish(patch);
        } finally {
            cachedContent.delete();
            if(! recursiveDelete(workDir)) {
                PatchLogger.ROOT_LOGGER.debugf("failed to remove work directory (%s)", workDir);
            }
        }
    }

    /**
     * Attempt to recursively delete a file or directory.
     *
     * @param root the real file to delete
     * @return {@code true} if the file was deleted
     */
    static boolean recursiveDelete(File root) {
        boolean ok = true;
        if (root.isDirectory()) {
            final File[] files = root.listFiles();
            for (File file : files) {
                ok &= recursiveDelete(file);
            }
            return ok && (root.delete() || !root.exists());
        } else {
            ok &= root.delete() || !root.exists();
        }
        return ok;
    }

    /**
     * unpack...
     *
     * @param zip the zip
     * @param patchDir the patch dir
     * @throws IOException
     */
    static void unpack(final File zip, final File patchDir) throws IOException {
        final ZipFile zipFile = new ZipFile(zip);
        try {
            unpack(zipFile, patchDir);
        } finally {
            if(zip != null) try {
                zipFile.close();
            } catch (IOException ignore) {
                //
            }
        }
    }

    /**
     * unpack...
     *
     * @param zip the zip
     * @param patchDir the patch dir
     * @throws IOException
     */
    static void unpack(final ZipFile zip, final File patchDir) throws IOException {
        final Enumeration<? extends ZipEntry> entries = zip.entries();
        while(entries.hasMoreElements()) {
            final ZipEntry entry = entries.nextElement();
            final String name = entry.getName();
            final File current = new File(patchDir, name);
            if(entry.isDirectory()) {
                continue;
            } else {
                if(! current.getParentFile().exists()) {
                    current.getParentFile().mkdirs();
                }
                final InputStream eis = zip.getInputStream(entry);
                try {
                    final FileOutputStream eos = new FileOutputStream(current);
                    try {
                        PatchUtils.copyStream(eis, eos);
                        eis.close();
                        eos.close();
                    } finally {
                        PatchUtils.safeClose(eos);
                    }
                } finally {
                    PatchUtils.safeClose(eis);
                }
            }
        }
    }

}

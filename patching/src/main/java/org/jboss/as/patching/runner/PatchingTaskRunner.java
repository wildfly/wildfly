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
import org.jboss.as.patching.metadata.ContentItem;
import org.jboss.as.patching.metadata.ContentModification;
import org.jboss.as.patching.metadata.Patch;
import org.jboss.as.patching.metadata.PatchXml;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.FileInputStream;
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

    private final File tempDir = null;

    public PatchingTaskRunner(final PatchInfo patchInfo, final DirectoryStructure structure) {
        this.patchInfo = patchInfo;
        this.structure = structure;
    }

    /**
     * Execute directly based of a stream.
     *
     * @param content the patch content
     * @return the patching result
     * @throws PatchingException
     */
    public PatchingResult executeDirect(final InputStream content) throws PatchingException {

        File workDir = null;
        try {
            workDir = File.createTempFile("patch", "work", tempDir);
            workDir.mkdir();

            // Save the content
            final File cachedContent = new File(workDir, "content");
            FileOutputStream os = null;
            try {
                // Cache the content first
                os = new FileOutputStream(cachedContent);
                PatchUtils.copyStream(content, os);
                os.close();
            } finally {
                PatchUtils.safeClose(os);
            }

            // Unpack to the work dir
            unpack(cachedContent, workDir);

            // Parse the xml
            final File patchXml = new File(workDir, PatchXml.PATCH_XML);
            final InputStream patchIS = new FileInputStream(patchXml);
            final Patch patch;
            try {
                patch = PatchXml.parse(patchIS);
                patchIS.close();
            } finally {
                PatchUtils.safeClose(patchIS);
            }

            // Execute the patch itself
            final PatchContentLoader loader = new PatchContentLoader(workDir);
            final PatchingContext context = new PatchingContext(patch, patchInfo, structure, loader);
            return execute(patch, context);

        } catch (IOException e) {
            throw new PatchingException(e);
        } catch (XMLStreamException e) {
            throw new PatchingException(e);
        } finally {
            if(workDir != null && ! recursiveDelete(workDir)) {
                PatchLogger.ROOT_LOGGER.debugf("failed to remove work directory (%s)", workDir);
            }
        }
    }

    /**
     * Execute a patch.
     *
     * @param patch the patch
     * @param context the context
     * @return the result of the patching action
     * @throws PatchingException
     */
    public PatchingResult execute(final Patch patch, final PatchingContext context) throws PatchingException {
        // Check if we can apply this patch
        final List<String> appliesTo = patch.getAppliesTo();
        if(! appliesTo.contains(patchInfo.getVersion())) {
            throw PatchMessages.MESSAGES.doesNotApply(appliesTo, patchInfo.getVersion());
        }
        try {
            // Create the modification tasks
            final List<PatchingTask> tasks = new ArrayList<PatchingTask>();
            final List<ContentItem> problems = new ArrayList<ContentItem>();
            for(final ContentModification modification : patch.getModifications()) {
                final PatchingTask task = PatchingTask.Factory.create(modification, context);
                try {
                    // backup
                    if(! task.prepare(context)) {
                        // In case the file was modified we report a problem
                        final ContentItem item = modification.getItem();
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
            try {
                // Execute the tasks
                for(final PatchingTask task : tasks) {
                    // Unless it's excluded by the user
                    if(context.isExcluded(task.getContentItem())) {
                        continue;
                    }
                    // Record the rollback task
                    task.execute(context);
                }
            } catch (Exception e) {
                throw new PatchingException(e);
            }
            // Finish..
            return context.finish(patch);
        } finally {

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

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
import org.jboss.as.patching.LocalPatchInfo;
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
 * The main patching task runner.
 *
 * @author Emanuel Muckenhuber
 */
public class PatchingTaskRunner {

    private final PatchInfo patchInfo;
    private final DirectoryStructure structure;

    private static final String DIRECTORY_SUFFIX = "jboss-as-patch-";
    private static final File TEMP_DIR = new File(System.getProperty("java.io.tmpdir"));

    public PatchingTaskRunner(final PatchInfo patchInfo, final DirectoryStructure structure) {
        this.patchInfo = patchInfo;
        this.structure = structure;
    }

    public PatchingResult executeDirect(final InputStream content) throws PatchingException {
        File workDir = null;
        try {
            // Create a working dir
            workDir = createTempDir();

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

            // Execute
            return execute(workDir, ContentVerificationPolicy.STRICT);
        } catch (IOException e) {
            throw new PatchingException(e);
        } finally {
            if(workDir != null && ! recursiveDelete(workDir)) {
                PatchLogger.ROOT_LOGGER.debugf("failed to remove work directory (%s)", workDir);
            }
        }
    }

    /**
     * Execute the patch.
     *
     * @param workDir the workDir
     * @param policy the content verification policy
     * @return the patching result
     * @throws PatchingException
     */
    private PatchingResult execute(final File workDir, final ContentVerificationPolicy policy) throws PatchingException {
        try {
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

            // Check if we can apply this patch
            final List<String> appliesTo = patch.getAppliesTo();
            if(! appliesTo.contains(patchInfo.getVersion())) {
                throw PatchMessages.MESSAGES.doesNotApply(appliesTo, patchInfo.getVersion());
            }

            // Execute the patch itself
            final PatchingContext context = PatchingContext.create(patch, patchInfo, structure, policy, workDir);
            try {
                return executeTasks(patch, context);
            } catch (Exception e) {
                // Undo patch
                context.undo();
                throw rethrowException(e);
            }

        } catch (IOException e) {
            throw new PatchingException(e);
        } catch (XMLStreamException e) {
            throw new PatchingException(e);
        }
    }

    /**
     * This will create and execute all the patching tasks based on the patch metadata.
     *
     * @param patch the patch
     * @param context the context
     * @return the result of the patching action
     * @throws PatchingException
     */
    private PatchingResult executeTasks(final Patch patch, final PatchingContext context) throws PatchingException {
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
            throw rethrowException(e);
        }
        // Finish..
        return context.finish(patch);
    }

    /**
     * Rollback an active patch.
     *
     * @param patchId the patch id to rollback
     * @throws PatchingException
     */
    public void rollback(final String patchId) throws PatchingException {
        // Check if the patch is currently active
        if(! patchInfo.getCumulativeID().equals(patchId)) {
            if(! patchInfo.getPatchIDs().contains(patchId)) {
                PatchLogger.ROOT_LOGGER.cannotRollbackPatch(patchId);
                return;
            }
        }
        final File historyDir = structure.getHistoryDir(patchId);
        if(! historyDir.exists()) {
            PatchLogger.ROOT_LOGGER.cannotRollbackPatch(patchId);
            return;
        }
        final File patchXml = new File(historyDir, PatchXml.PATCH_XML);
        if(! patchXml.exists()) {
            PatchLogger.ROOT_LOGGER.cannotRollbackPatch(patchId);
            return;
        }
        File workDir = createTempDir();
        try {
            final InputStream is = new FileInputStream(patchXml);
            try {
                // Parse the rollback patch.xml
                final Patch patch = PatchXml.parse(is);
                // Check the consistency of the CP history
                final File previousCP = new File(historyDir, DirectoryStructure.CUMULATIVE);
                final String cumulative = PatchUtils.readRef(previousCP);
                if(! cumulative.equals(patch.getPatchId())) {
                    // TODO perhaps just ignore or warn?
                    throw new PatchingException("inconsistent cumulative version expected: %s, was: %s", patch.getPatchId(), cumulative);
                }
                // Check the consistency of the patches history
                final File cumulativeReferences = structure.getCumulativeRefs(cumulative);
                final File referencesHistory = new File(historyDir, DirectoryStructure.REFERENCES);
                final List<String> cumulativePatches = PatchUtils.readRefs(cumulativeReferences);
                final List<String> historyPatches = PatchUtils.readRefs(referencesHistory);
                if(! cumulativePatches.equals(historyPatches)) {
                    // TODO perhaps just ignore or warn?
                    throw new PatchingException("inconsistent patches for '%s' expected: %s, was: %s", cumulative, historyDir, cumulativePatches);
                }

                final PatchingContext context = PatchingContext.createForRollback(patch, patchInfo, structure, workDir);
                // Rollback
                executeTasks(patch, context);

            } finally {
                PatchUtils.safeClose(is);
            }
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
            zipFile.close();
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

    static File createTempDir() throws PatchingException {
        File workDir = null;
        int count = 0;
        while (workDir == null || workDir.exists()) {
            count++;
            workDir = new File(TEMP_DIR, DIRECTORY_SUFFIX + count);
        }
        if (!workDir.mkdirs()) {
            throw new PatchingException("Cannot create tmp dir for patch creation at " + workDir);
        }
        return workDir;
    }

    static PatchingException rethrowException(final Exception e) {
        if(e instanceof PatchingException) {
            return (PatchingException) e;
        } else {
            return new PatchingException(e);
        }
    }

}

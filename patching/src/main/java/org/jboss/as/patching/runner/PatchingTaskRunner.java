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

import static org.jboss.as.patching.IoUtils.safeClose;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLStreamException;

import org.jboss.as.patching.DirectoryStructure;
import org.jboss.as.patching.IoUtils;
import org.jboss.as.patching.PatchInfo;
import org.jboss.as.patching.PatchLogger;
import org.jboss.as.patching.PatchMessages;
import org.jboss.as.patching.ZipUtils;
import org.jboss.as.patching.metadata.ContentItem;
import org.jboss.as.patching.metadata.Patch;
import org.jboss.as.patching.metadata.Patch.PatchType;
import org.jboss.as.patching.metadata.PatchXml;

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
        return executeDirect(content, ContentVerificationPolicy.STRICT);
    }

    public PatchingResult executeDirect(final InputStream content, final ContentVerificationPolicy policy) throws PatchingException {
        File workDir = null;
        try {
            // Create a working dir
            workDir = createTempDir();

            // Save the content
            final File cachedContent = new File(workDir, "content");
            IoUtils.copy(content, cachedContent);
            // Unpack to the work dir
            ZipUtils.unzip(cachedContent, workDir);

            // Execute
            return execute(workDir, policy);
        } catch (IOException e) {
            throw new PatchingException(e);
        } finally {
            if(workDir != null && ! IoUtils.recursiveDelete(workDir)) {
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
                safeClose(patchIS);
            }

            // Check if we can apply this patch
            final String patchId = patch.getPatchId();
            final List<String> appliesTo = patch.getAppliesTo();
            if(! appliesTo.contains(patchInfo.getVersion())) {
                throw PatchMessages.MESSAGES.doesNotApply(appliesTo, patchInfo.getVersion());
            }
            if(patchInfo.getCumulativeID().equals(patchId)) {
                throw PatchMessages.MESSAGES.alreadyApplied(patchId);
            }
            if(patchInfo.getPatchIDs().contains(patchId)) {
                throw PatchMessages.MESSAGES.alreadyApplied(patchId);
            }

            // Execute the patch itself
            final PatchingContext context = PatchingContext.create(patch, patchInfo, structure, policy, workDir);
            final PatchingContext.TaskFinishCallback task = new PatchingApplyCallback(patch, patchId, structure);
            try {
                return applyPatch(patch, context, task);
            } catch (Exception e) {
                try {
                    // Undo patch
                    context.undo();
                } finally {
                    task.rollbackCallback();
                }
                throw rethrowException(e);
            }

        } catch (IOException e) {
            throw new PatchingException(e);
        } catch (XMLStreamException e) {
            throw new PatchingException(e);
        }
    }

    /**
     * Apply the patch.
     *
     * @param patch the patch
     * @param context the patching context
     * @return the patching result
     * @throws PatchingException
     */
    private PatchingResult applyPatch(final Patch patch, final PatchingContext context, final PatchingContext.TaskFinishCallback finishTask) throws PatchingException {
        // Rollback one-off patches
        final List<String> rollbacks;
        final Patch.PatchType type = patch.getPatchType();
        if(type == Patch.PatchType.CUMULATIVE) {
            rollbacks = patchInfo.getPatchIDs();
        } else {
            rollbacks = Collections.emptyList();
        }
        // Rollback one-off patches (if there are any to roll back)
        final Map<Location, PatchingTasks.ContentTaskDefinition> definitions = new LinkedHashMap<Location, PatchingTasks.ContentTaskDefinition>();
        for(final String oneOff : rollbacks) {
            try {
                // Rollback one off patches
                context.recordRollback(oneOff, definitions);
            } catch (Exception e) {
                throw new PatchingException(e);
            }
        }
        try {
            // Apply the current patch
            context.applyPatch(patch, definitions);
        } catch (Exception e) {
            throw new PatchingException(e);
        }
        // Process the resolved tasks
        return executeTasks(finishTask, definitions, context);
    }


    /**
     * This will create and execute all the patching tasks based on the patch metadata.
     *
     * @param finishTask the finish Task
     * @param context the context
     * @return the result of the patching action
     * @throws PatchingException
     */
    private PatchingResult executeTasks(final PatchingContext.TaskFinishCallback finishTask, final Map<Location, PatchingTasks.ContentTaskDefinition> definitions, final PatchingContext context) throws PatchingException {
        // Create the modification tasks
        final List<PatchingTask> tasks = new ArrayList<PatchingTask>();
        final List<ContentItem> problems = new ArrayList<ContentItem>();
        // Process the consolidated modifications
        for(final PatchingTasks.ContentTaskDefinition definition : definitions.values()) {
            final PatchingTask task = context.createTask(definition);
            try {
                // backup and validate content
                if(! task.prepare(context) || definition.hasConflicts()) {
                    // Unless it a content item was manually ignored (or excluded)
                    final ContentItem item = task.getContentItem();
                    if(! context.isIgnored(item)) {
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
            throw new PatchingException(problems);
        }
        try {
            // Execute the tasks
            for(final PatchingTask task : tasks) {
                // Unless it's excluded by the user
                final ContentItem item = task.getContentItem();
                if(item != null && context.isExcluded(item)) {
                    continue;
                }
                // Run the task
                task.execute(context);
            }
        } catch (Exception e) {
            throw rethrowException(e);
        }
        // Finish ...
        return context.finish(finishTask);
    }

    /**
     * Rollback an active patch.
     *
     * @param patchId the patch id to rollback
     * @param contentPolicy the content verification policy
     * @param rollbackTo rollback all one off patches until the given patch-id
     * @param restoreConfiguration whether to restore the configuration or not
     * @return the result
     * @throws PatchingException
     */
    public PatchingResult rollback(final String patchId, final ContentVerificationPolicy contentPolicy, boolean rollbackTo, boolean restoreConfiguration) throws PatchingException {
        // Check if the patch is currently active
        final int index = patchInfo.getPatchIDs().indexOf(patchId);
        if(index == -1 ) {
            if(!patchInfo.getCumulativeID().equals(patchId)) {
                throw PatchMessages.MESSAGES.cannotRollbackPatch(patchId);
            }
        }

        //
        final List<String> patches = new ArrayList<String>();
        final List<String> oneOffs = patchInfo.getPatchIDs();
        if(index == -1) {
            // Means we rollback a CP and all it's one-off patches
            patches.addAll(oneOffs);
            patches.add(patchId);
        } else if (index == 0) {
            patches.add(patchId);
        } else {
            if (rollbackTo) {
                // rollback one-offs up to the given patchId
                for(int i = 0; i <= index; i++) {
                    patches.add(oneOffs.get(i));
                }
            } else {
                // TODO perhaps we can allow this as well?
                throw PatchMessages.MESSAGES.cannotRollbackPatch(patchId);
            }
        }

        final File historyDir = structure.getHistoryDir(patchId);
        if(! historyDir.exists()) {
            throw PatchMessages.MESSAGES.cannotRollbackPatch(patchId);
        }
        final File patchXml = new File(historyDir, PatchingContext.ROLLBACK_XML);
        if(! patchXml.exists()) {
            throw PatchMessages.MESSAGES.cannotRollbackPatch(patchId);
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

                // Check the consistency of the patches history for cumulative patch
                if (PatchType.CUMULATIVE == patch.getPatchType()) {
                    final File cumulativeReferences = structure.getCumulativeRefs(cumulative);
                    final File referencesHistory = new File(historyDir, DirectoryStructure.REFERENCES);
                    final List<String> cumulativePatches = PatchUtils.readRefs(cumulativeReferences);
                    final List<String> historyPatches = PatchUtils.readRefs(referencesHistory);
                    if(! cumulativePatches.equals(historyPatches)) {
                        // TODO perhaps just ignore or warn?
                        throw new PatchingException("inconsistent patches for '%s' expected: %s, was: %s", cumulative, historyDir, cumulativePatches);
                    }
                }

                // Process potentially multiple rollbacks
                final PatchingContext context = PatchingContext.createForRollback(patch, patchInfo, structure, contentPolicy, workDir);
                final Map<Location, PatchingTasks.ContentTaskDefinition> definitions = new LinkedHashMap<Location, PatchingTasks.ContentTaskDefinition>();
                for(final String rollback : patches) {
                    try {
                        // Rollback one off patches
                        context.recordRollback(rollback, definitions);
                    } catch (Exception e) {
                        throw new PatchingException(e);
                    }
                }
                // Rollback
                final PatchingContext.TaskFinishCallback task = new PatchingRollbackCallback(patchId, patch, patches, cumulative, restoreConfiguration, structure);
                try {
                    return executeTasks(task, definitions, context);
                } catch (Exception e) {
                    task.rollbackCallback();
                    throw rethrowException(e);
                }
            } finally {
                safeClose(is);
            }
        } catch (IOException e) {
            throw new PatchingException(e);
        } catch (XMLStreamException e) {
            throw new PatchingException(e);
        } finally {
            if(workDir != null && ! IoUtils.recursiveDelete(workDir)) {
                PatchLogger.ROOT_LOGGER.debugf("failed to remove work directory (%s)", workDir);
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
            throw new PatchingException(PatchMessages.MESSAGES.cannotCreateDirectory(workDir.getAbsolutePath()));
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

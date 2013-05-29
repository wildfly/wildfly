package org.jboss.as.patching.runner;

import static org.jboss.as.patching.runner.PatchingTasks.ContentTaskDefinition;
import static org.jboss.as.patching.runner.PatchingTasks.apply;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jboss.as.patching.Constants;
import org.jboss.as.patching.PatchMessages;
import org.jboss.as.patching.installation.InstallationManager;
import org.jboss.as.patching.installation.InstalledImage;
import org.jboss.as.patching.installation.PatchableTarget;
import org.jboss.as.patching.metadata.ContentItem;
import org.jboss.as.patching.metadata.IdentityPatch;
import org.jboss.as.patching.metadata.Patch;
import org.jboss.as.patching.metadata.PatchXml;

/**
 * @author Emanuel Muckenhuber
 */
class IdentityPatchRunner implements InstallationManager.ModificationCompletion {

    private static final String DIRECTORY_SUFFIX = "jboss-as-patch-";
    private static final File TEMP_DIR = new File(System.getProperty("java.io.tmpdir"));

    private final InstalledImage installedImage;

    IdentityPatchRunner(InstalledImage installedImage) {
        this.installedImage = installedImage;
    }

    /**
     * Apply a patch.
     *
     * @param patch           the patch metadata
     * @param contentProvider the patch content provider
     * @param contentPolicy   the content verification policy
     * @param modification    the installation modification
     * @throws PatchingException for any error
     */
    public PatchingResult applyPatch(final IdentityPatch patch, final PatchContentProvider contentProvider, final ContentVerificationPolicy contentPolicy, final InstallationManager.InstallationModification modification) throws PatchingException {
        try {
            // Check if we can apply this patch
            final String patchId = patch.getPatchId();
            final List<String> appliesTo = patch.getAppliesTo();
            if (!appliesTo.contains(modification.getVersion())) {
                throw PatchMessages.MESSAGES.doesNotApply(appliesTo, modification.getVersion());
            }
            if (modification.getCumulativeID().equals(patchId)) {
                throw PatchMessages.MESSAGES.alreadyApplied(patchId);
            }
            if (modification.getPatchIDs().contains(patchId)) {
                throw PatchMessages.MESSAGES.alreadyApplied(patchId);
            }
            // Apply the patch
            final File backup = installedImage.getPatchHistoryDir(patchId);
            final IdentityPatchContext context = new IdentityPatchContext(backup, contentProvider, contentPolicy, modification, installedImage);
            try {
                return applyPatch(patchId, patch, context);
            } catch (Exception e) {
                throw rethrowException(e);
            } finally {
                context.cleanup();
            }
        } finally {
            contentProvider.cleanup();
        }
    }

    /**
     * Apply a patch.
     *
     * @param patchId the patch id
     * @param patch   the patch metadata
     * @param context the patch context
     * @throws PatchingException
     * @throws IOException
     * @throws XMLStreamException
     */
    private PatchingResult applyPatch(final String patchId, final IdentityPatch patch, final IdentityPatchContext context) throws PatchingException, IOException, XMLStreamException {

        final List<String> rollbacks;
        final Patch.PatchType patchType = patch.getPatchType();
        if (patchType == Patch.PatchType.CUMULATIVE) {
            rollbacks = context.getModification().getPatchIDs();
        } else {
            rollbacks = Collections.emptyList();
        }
        // Record the rollbacks first
        for (final String rollback : rollbacks) {
            rollback(rollback, context);
        }
        // Then apply the current patch
        for (final IdentityPatch.PatchElement element : patch.getPatchElements()) {
            // Apply the content modifications
            final IdentityPatchContext.PatchEntry target = context.resolveForElement(element);
            final String elementPatchId = element.getPatchId();
            checkApplied(elementPatchId, target);
            apply(elementPatchId, element.getModifications(), target.getModifications());
            target.apply(elementPatchId, patchType);
        }
        // Apply the patch to the identity
        final IdentityPatchContext.PatchEntry identity = context.getIdentityEntry();
        apply(patchId, patch.getModifications(), identity.getModifications());
        identity.apply(patchId, patchType);
        if (patchType == Patch.PatchType.CUMULATIVE) {
            identity.setResultingVersion(patch.getResultingVersion());
        }

        // Execute the tasks
        final IdentityApplyCallback callback = new IdentityApplyCallback(patch, identity.getDirectoryStructure());
        try {
            return executeTasks(context, callback);
        } catch (Exception e) {
            callback.rollback();
            throw rethrowException(e);
        }
    }

    public PatchingResult rollbackPatch(final String patchId, final ContentVerificationPolicy contentPolicy, final boolean rollbackTo, final boolean restoreConfiguration, InstallationManager.InstallationModification modification) throws PatchingException {
        // Check if the patch is currently active
        final int index = modification.getPatchIDs().indexOf(patchId);
        if (index == -1) {
            if (!modification.getCumulativeID().equals(patchId)) {
                throw PatchMessages.MESSAGES.cannotRollbackPatch(patchId);
            }
        }
        // Figure out what to do
        final List<String> patches = new ArrayList<String>();
        final List<String> oneOffs = modification.getPatchIDs();
        if (index == -1) {
            // Means we rollback a CP and all it's one-off patches
            patches.addAll(oneOffs);
            patches.add(patchId);
        } else if (index == 0) {
            patches.add(patchId);
        } else {
            if (rollbackTo) {
                // rollback one-offs up to the given patchId
                for (int i = 0; i <= index; i++) {
                    patches.add(oneOffs.get(i));
                }
            } else {
                // TODO perhaps we can allow this as well?
                throw PatchMessages.MESSAGES.cannotRollbackPatch(patchId);
            }
        }
        final File historyDir = installedImage.getPatchHistoryDir(patchId);
        if (!historyDir.exists()) {
            throw PatchMessages.MESSAGES.cannotRollbackPatch(patchId);
        }
        final File patchXml = new File(historyDir, Constants.ROLLBACK_XML);
        if (!patchXml.exists()) {
            throw PatchMessages.MESSAGES.cannotRollbackPatch(patchId);
        }
        final File workDir = createTempDir();
        final PatchContentProvider provider = null;
        final IdentityPatchContext context = new IdentityPatchContext(workDir, provider, contentPolicy, modification, installedImage);
        try {
            // Rollback patches
            for (final String rollback : patches) {
                rollback(rollback, context);
            }
            // Execute the tasks
            final IdentityPatchContext.PatchEntry identity = context.getIdentityEntry();

            final IdentityRollbackCallback callback = new IdentityRollbackCallback(patchId, patches, restoreConfiguration, identity.getDirectoryStructure());
            try {
                return executeTasks(context, callback);
            } catch (Exception e) {
                throw rethrowException(e);
            }

        } finally {
            context.cleanup();
        }
    }

    @Override
    public void completed() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void canceled() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    /**
     * Rollback a patch.
     *
     * @param patchID the patch id
     * @param context the patch context
     * @throws PatchingException
     */
    private void rollback(final String patchID, final IdentityPatchContext context) throws PatchingException {
        try {
            // Load the patch history
            final IdentityPatch originalPatch = loadPatchInformation(patchID, installedImage);
            final IdentityPatch rollbackPatch = loadRollbackInformation(patchID, installedImage);
            final Patch.PatchType patchType = rollbackPatch.getPatchType();

            // Process originals by type first
            final LinkedHashMap<String, IdentityPatch.PatchElement> originalLayers = new LinkedHashMap<String, IdentityPatch.PatchElement>();
            final LinkedHashMap<String, IdentityPatch.PatchElement> originalAddOns = new LinkedHashMap<String, IdentityPatch.PatchElement>();
            for (final IdentityPatch.PatchElement patchElement : originalPatch.getPatchElements()) {
                final String layerName = patchElement.getLayerName();
                final IdentityPatch.LayerType layerType = patchElement.getLayerType();
                final Map<String, IdentityPatch.PatchElement> originals;
                switch (layerType) {
                    case Layer:
                        originals = originalLayers;
                        break;
                    case AddOn:
                        originals = originalAddOns;
                        break;
                    default:
                        throw new IllegalStateException();
                }
                if (!originals.containsKey(layerName)) {
                    originals.put(layerName, patchElement);
                } else {
                    throw new PatchingException("duplicate layer " + layerName);
                }
            }
            // Process the rollback xml
            for (final IdentityPatch.PatchElement patchElement : rollbackPatch.getPatchElements()) {
                final String layerName = patchElement.getLayerName();
                final IdentityPatch.LayerType layerType = patchElement.getLayerType();
                final LinkedHashMap<String, IdentityPatch.PatchElement> originals;
                switch (layerType) {
                    case Layer:
                        originals = originalLayers;
                        break;
                    case AddOn:
                        originals = originalAddOns;
                        break;
                    default:
                        throw new IllegalStateException();
                }
                final IdentityPatch.PatchElement original = originals.remove(layerName);
                if (original == null) {
                    throw new PatchingException("did not exist in original " + layerName);
                }
                final IdentityPatchContext.PatchEntry entry = context.resolveForElement(patchElement);
                final Map<Location, ContentTaskDefinition> modifications = entry.getModifications();
                // Create the rollback
                PatchingTasks.rollback(patchID, original.getModifications(), patchElement.getModifications(), modifications, ContentItemFilter.ALL_BUT_MISC);
                entry.rollback(patchID);
            }
            if (!originalLayers.isEmpty() || !originalAddOns.isEmpty()) {
                throw new PatchingException("rollback did not contain all layers");
            }

            final IdentityPatchContext.PatchEntry identity = context.getIdentityEntry();
            PatchingTasks.rollback(patchID, originalPatch.getModifications(), rollbackPatch.getModifications(), identity.getModifications(), ContentItemFilter.MISC_ONLY);
            identity.rollback(patchID);

            if (patchType == Patch.PatchType.CUMULATIVE) {
                assert identity.getModifiedState().getPatchIDs().isEmpty();

                // TODO does this make sense !?
                identity.rollback(patchID);
                final String rollbackPatchId = rollbackPatch.getPatchId();
                identity.apply(rollbackPatchId, Patch.PatchType.CUMULATIVE);

                // TODO does this make sense !?
                identity.setResultingVersion(rollbackPatch.getResultingVersion());

            }

        } catch (Exception e) {
            throw rethrowException(e);
        }
    }

    /**
     * Execute all recorded tasks.
     *
     * @param context  the patch context
     * @param callback the finalization callback
     * @throws Exception
     */
    static PatchingResult executeTasks(final IdentityPatchContext context, final IdentityPatchContext.FinalizeCallback callback) throws Exception {
        final List<PreparedTask> tasks = new ArrayList<PreparedTask>();
        final List<ContentItem> conflicts = new ArrayList<ContentItem>();
        // Identity
        prepareTasks(context.getIdentityEntry(), context, tasks, conflicts);
        // Layers
        for (final IdentityPatchContext.PatchEntry layer : context.getLayers()) {
            prepareTasks(layer, context, tasks, conflicts);
        }
        // AddOns
        for (final IdentityPatchContext.PatchEntry addOn : context.getAddOns()) {
            prepareTasks(addOn, context, tasks, conflicts);
        }
        // If there were problems report them
        if (!conflicts.isEmpty()) {
            throw new PatchingException(conflicts);
        }
        // Execute the tasks
        for (final PreparedTask task : tasks) {
            // Unless it's excluded by the user
            final ContentItem item = task.getContentItem();
            if (item != null && context.isExcluded(item)) {
                continue;
            }
            // Run the task
            task.execute();
        }
        return context.finalize(callback);
    }

    /**
     * Prepare all tasks.
     *
     * @param entry     the patch entry
     * @param context   the patch context
     * @param tasks     a list for prepared tasks
     * @param conflicts a list for conflicting content items
     * @throws PatchingException
     */
    static void prepareTasks(final IdentityPatchContext.PatchEntry entry, final IdentityPatchContext context, final List<PreparedTask> tasks, final List<ContentItem> conflicts) throws PatchingException {
        for (final PatchingTasks.ContentTaskDefinition definition : entry.getModifications().values()) {
            final PatchingTask task = createTask(definition, context, entry);
            try {
                // backup and validate content
                if (!task.prepare(entry) || definition.hasConflicts()) {
                    // Unless it a content item was manually ignored (or excluded)
                    final ContentItem item = task.getContentItem();
                    if (!context.isIgnored(item)) {
                        conflicts.add(item);
                    }
                }
                tasks.add(new PreparedTask(task, entry));
            } catch (IOException e) {
                throw new PatchingException(e);
            }
        }
    }

    /**
     * Create the patching task based on the definition.
     *
     * @param definition the task description
     * @param provider   the content provider
     * @param context    the task context
     * @return the created task
     */
    static PatchingTask createTask(final PatchingTasks.ContentTaskDefinition definition, final PatchContentProvider provider, final PatchingTaskContext context) {
        final PatchContentLoader contentLoader = provider.getLoader(definition.getTarget().getPatchId());
        final PatchingTaskDescription description = PatchingTaskDescription.create(definition, contentLoader);
        return PatchingTask.Factory.create(description, context);
    }

    static class PreparedTask {

        private final PatchingTask task;
        private final IdentityPatchContext.PatchEntry entry;

        PreparedTask(PatchingTask task, IdentityPatchContext.PatchEntry entry) {
            this.task = task;
            this.entry = entry;
        }

        ContentItem getContentItem() {
            return task.getContentItem();
        }

        protected void execute() throws IOException {
            task.execute(entry);
        }

    }

    static IdentityPatch loadPatchInformation(final String patchId, final InstalledImage installedImage) throws IOException, XMLStreamException {
        final File patchDir = installedImage.getPatchHistoryDir(patchId);
        final File patchXml = new File(patchDir, PatchXml.PATCH_XML);
        return IdentityPatch.Wrapper.wrap(PatchXml.parse(patchXml));
    }

    static IdentityPatch loadRollbackInformation(final String patchId, final InstalledImage installedImage) throws IOException, XMLStreamException {
        final File historyDir = installedImage.getPatchHistoryDir(patchId);
        final File patchXml = new File(historyDir, Constants.ROLLBACK_XML);
        return IdentityPatch.Wrapper.wrap(PatchXml.parse(patchXml));
    }

    static void checkApplied(final String patchId, final PatchableTarget.TargetInfo info) throws PatchingException {
        if (info.getCumulativeID().equals(patchId)) {
            throw PatchMessages.MESSAGES.alreadyApplied(patchId);
        }
        if (info.getPatchIDs().contains(patchId)) {
            throw PatchMessages.MESSAGES.alreadyApplied(patchId);
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
        if (e instanceof PatchingException) {
            return (PatchingException) e;
        } else {
            return new PatchingException(e);
        }
    }

}

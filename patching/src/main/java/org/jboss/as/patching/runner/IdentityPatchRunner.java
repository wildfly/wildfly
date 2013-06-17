package org.jboss.as.patching.runner;

import static org.jboss.as.patching.runner.PatchingTasks.ContentTaskDefinition;
import static org.jboss.as.patching.runner.PatchingTasks.apply;
import static org.jboss.as.patching.runner.PatchingTasks.rollback;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jboss.as.patching.Constants;
import org.jboss.as.patching.PatchMessages;
import org.jboss.as.patching.installation.InstallationManager;
import org.jboss.as.patching.installation.InstalledIdentity;
import org.jboss.as.patching.installation.InstalledImage;
import org.jboss.as.patching.installation.PatchableTarget;
import org.jboss.as.patching.metadata.ContentItem;
import org.jboss.as.patching.metadata.LayerType;
import org.jboss.as.patching.metadata.Patch;
import org.jboss.as.patching.metadata.PatchElement;
import org.jboss.as.patching.metadata.PatchElementProvider;
import org.jboss.as.patching.metadata.PatchXml;
import org.jboss.as.patching.metadata.RollbackPatch;

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
    public PatchingResult applyPatch(final Patch patch, final PatchContentProvider contentProvider, final ContentVerificationPolicy contentPolicy, final InstallationManager.InstallationModification modification) throws PatchingException {
        try {
            // Check if we can apply this patch
            final String patchId = patch.getPatchId();
            final Collection<String> appliesTo = patch.getAppliesTo();
            if (!appliesTo.contains(modification.getVersion())) {
                throw PatchMessages.MESSAGES.doesNotApply(appliesTo, modification.getVersion());
            }
            // Cannot apply the same patch twice
            if (modification.isApplied(patchId)) {
                throw PatchMessages.MESSAGES.alreadyApplied(patchId);
            }
            // See if the prerequisites are met
            for (final String required : patch.getIdentity().getRequires()) {
                if (!modification.isApplied(required)) {
                    throw PatchMessages.MESSAGES.requiresPatch(required);
                }
            }
            // Check for incompatibilities
            for (final String incompatible : patch.getIncompatibleWith()) {
                if (modification.isApplied(incompatible)) {
                    throw PatchMessages.MESSAGES.incompatibePatch(incompatible);
                }
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
    private PatchingResult applyPatch(final String patchId, final Patch patch, final IdentityPatchContext context) throws PatchingException, IOException, XMLStreamException {

        final List<String> invalidation;
        final Patch.PatchType patchType = patch.getPatchType();
        final InstallationManager.InstallationModification modification = context.getModification();
        if (patchType == Patch.PatchType.ONE_OFF) {
            // We don't need to invalidate anything
            invalidation = Collections.emptyList();
            // Check whether the patch applies to the current cumulative patch id
            if (patch.getAppliesTo().contains(modification.getCumulativeID())) {
                throw PatchMessages.MESSAGES.doesNotApply(patch.getAppliesTo(), modification.getCumulativeID());
            }
        } else {
            // Invalidate all installed patches (one-off, cumulative) - we never need to invalidate the release base
            invalidation = new ArrayList<String>(modification.getPatchIDs());
            if (!Constants.BASE.equals(modification.getCumulativeID())) {
                invalidation.add(modification.getCumulativeID());
            }
        }
        // Record the rollbacks first
        for (final String rollback : invalidation) {
            rollback(rollback, context);
        }
        // Then apply the current patch
        for (final PatchElement element : patch.getElements()) {
            // Apply the content modifications
            final IdentityPatchContext.PatchEntry target = context.resolveForElement(element);
            final Patch.PatchType elementPatchType = element.getPatchType();
            final String elementPatchId = element.getId();
            // See if we can skip this element
            if (target.isApplied(elementPatchId)) {
                // TODO if it is already applied, we can just skip the entry (maybe based ont the type of the patch)
                // This needs some further testing, maybe we need to compare our history with the patch if they are consistent
                throw PatchMessages.MESSAGES.alreadyApplied(elementPatchId);
            }
            // Check requirements
            final PatchElementProvider provider = element.getProvider();
            for (final String required : provider.getRequires()) {
                if (!target.isApplied(required)) {
                    throw PatchMessages.MESSAGES.requiresPatch(required);
                }
            }
            // Check for incompatibilities
            for (final String incompatible : element.getIncompatibleWith()) {
                if (target.isApplied(incompatible)) {
                    throw PatchMessages.MESSAGES.incompatibePatch(incompatible);
                }
            }
            if (elementPatchType == Patch.PatchType.ONE_OFF) {
                // TODO check whether the element applies to the cumulative ID
            }
            apply(elementPatchId, element.getModifications(), target.getModifications());
            target.apply(elementPatchId, elementPatchType);
        }
        // Apply the patch to the identity
        final IdentityPatchContext.PatchEntry identity = context.getIdentityEntry();
        apply(patchId, patch.getModifications(), identity.getModifications());
        identity.apply(patchId, patchType);

        // We need the resulting version for rollback
        if (patchType == Patch.PatchType.UPGRADE) {
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
        if (Constants.BASE.equals(patchId)) {
            throw PatchMessages.MESSAGES.cannotRollbackPatch(patchId);
        }

        // Figure out what to do
        final List<String> patches = new ArrayList<String>();
        final List<String> oneOffs = modification.getPatchIDs();
        final int index = oneOffs.indexOf(patchId);

        if (index == -1) {
            if (patchId.equals(modification.getReleasePatchID())) {
                // Rollback all active
                patches.addAll(oneOffs);
                patches.add(modification.getCumulativeID());
                patches.add(modification.getReleasePatchID());
            } else if (patchId.equals(modification.getCumulativeID())) {
                // Rollback all active
                patches.addAll(oneOffs);
                patches.add(modification.getCumulativeID());
            } else {
                throw PatchMessages.MESSAGES.cannotRollbackPatch(patchId);
            }
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
                if (!Constants.BASE.equals(rollback)) {
                    rollback(rollback, context);
                }
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
        // nothing here
    }

    @Override
    public void canceled() {
        // nothing here
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
            final Patch originalPatch = loadPatchInformation(patchID, installedImage);
            final RollbackPatch rollbackPatch = loadRollbackInformation(patchID, installedImage);
            final InstalledIdentity history = rollbackPatch.getIdentityState();
            final Patch.PatchType patchType = rollbackPatch.getPatchType();

            // Process originals by type first
            final LinkedHashMap<String, PatchElement> originalLayers = new LinkedHashMap<String, PatchElement>();
            final LinkedHashMap<String, PatchElement> originalAddOns = new LinkedHashMap<String, PatchElement>();
            for (final PatchElement patchElement : originalPatch.getElements()) {
                final PatchElementProvider provider = patchElement.getProvider();
                final String layerName = provider.getName();
                final LayerType layerType = provider.getLayerType();
                final Map<String, PatchElement> originals;
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
            for (final PatchElement patchElement : rollbackPatch.getElements()) {
                final String elementPatchId = patchElement.getId();
                final PatchElementProvider provider = patchElement.getProvider();
                final String layerName = provider.getName();
                final LayerType layerType = provider.getLayerType();
                final LinkedHashMap<String, PatchElement> originals;
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
                final PatchElement original = originals.remove(layerName);
                if (original == null) {
                    throw new PatchingException("did not exist in original " + layerName);
                }
                final IdentityPatchContext.PatchEntry entry = context.resolveForElement(patchElement);
                final Map<Location, ContentTaskDefinition> modifications = entry.getModifications();
                // Create the rollback
                PatchingTasks.rollback(elementPatchId, original.getModifications(), patchElement.getModifications(), modifications, ContentItemFilter.MISC_ONLY);
                entry.rollback(elementPatchId);

                // We need to restore the previous state
                final Patch.PatchType elementPatchType = patchElement.getPatchType();
                if (elementPatchType != Patch.PatchType.ONE_OFF) {
                    final PatchableTarget.TargetInfo info;
                    if (layerType == LayerType.AddOn) {
                        info = history.getAddOn(layerName).loadTargetInfo();
                    } else {
                        info = history.getLayer(layerName).loadTargetInfo();
                    }
                    restoreFromHistory(entry, elementPatchId, elementPatchType, info);
                }

            }
            if (!originalLayers.isEmpty() || !originalAddOns.isEmpty()) {
                throw new PatchingException("rollback did not contain all layers");
            }

            // Rollback the patch
            final IdentityPatchContext.PatchEntry identity = context.getIdentityEntry();
            PatchingTasks.rollback(patchID, originalPatch.getModifications(), rollbackPatch.getModifications(), identity.getModifications(), ContentItemFilter.MISC_ONLY);
            identity.rollback(patchID);

            // Restore previous state
            final PatchableTarget.TargetInfo identityHistory = history.getIdentity().loadTargetInfo();
            restoreFromHistory(identity, rollbackPatch.getPatchId(), patchType, identityHistory);
            if (patchType == Patch.PatchType.UPGRADE) {
                identity.setResultingVersion(rollbackPatch.getResultingVersion());
            }

        } catch (Exception e) {
            throw rethrowException(e);
        }
    }

    static void restoreFromHistory(final InstallationManager.MutablePatchingTarget target, final String rollbackPatchId,
                                   final Patch.PatchType patchType, final PatchableTarget.TargetInfo history) throws PatchingException {
        if (patchType == Patch.PatchType.UPGRADE) {
            assert history.getReleasePatchID().equals(rollbackPatchId);
            target.apply(rollbackPatchId, patchType);
            // Restore previous CP state
            target.apply(history.getCumulativeID(), Patch.PatchType.CUMULATIVE);
        } else if (patchType == Patch.PatchType.CUMULATIVE) {
            assert history.getCumulativeID().equals(rollbackPatchId);
            target.apply(rollbackPatchId, patchType);
        }
        if (patchType != Patch.PatchType.ONE_OFF) {
            // Restore one off state
            final List<String> oneOffs = new ArrayList<String>(history.getPatchIDs());
            Collections.reverse(oneOffs);
            for (final String oneOff : oneOffs) {
                target.apply(oneOff, Patch.PatchType.ONE_OFF);
            }
        }
        checkState(history, history); // The rollback should restore the old state
    }

    static void checkState(final PatchableTarget.TargetInfo o, final PatchableTarget.TargetInfo n) {
        assert n.getPatchIDs().equals(o.getPatchIDs());
        assert n.getCumulativeID().equals(o.getCumulativeID());
        assert n.getReleasePatchID().equals(o.getReleasePatchID());
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

    static Patch loadPatchInformation(final String patchId, final InstalledImage installedImage) throws IOException, XMLStreamException {
        final File patchDir = installedImage.getPatchHistoryDir(patchId);
        final File patchXml = new File(patchDir, PatchXml.PATCH_XML);
        return PatchXml.parse(patchXml);
    }

    static RollbackPatch loadRollbackInformation(final String patchId, final InstalledImage installedImage) throws IOException, XMLStreamException {
        final File historyDir = installedImage.getPatchHistoryDir(patchId);
        final File patchXml = new File(historyDir, Constants.ROLLBACK_XML);
        return (RollbackPatch) PatchXml.parse(patchXml);
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

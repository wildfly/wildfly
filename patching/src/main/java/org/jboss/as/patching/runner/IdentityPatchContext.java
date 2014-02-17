package org.jboss.as.patching.runner;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

import org.jboss.as.patching.Constants;
import org.jboss.as.patching.DirectoryStructure;
import org.jboss.as.patching.IoUtils;
import org.jboss.as.patching.PatchInfo;
import org.jboss.as.patching.PatchLogger;
import org.jboss.as.patching.PatchMessages;
import org.jboss.as.patching.PatchingException;
import org.jboss.as.patching.installation.InstallationManager;
import org.jboss.as.patching.installation.InstalledIdentity;
import org.jboss.as.patching.installation.InstalledImage;
import org.jboss.as.patching.installation.PatchableTarget;
import org.jboss.as.patching.metadata.ContentItem;
import org.jboss.as.patching.metadata.ContentModification;
import org.jboss.as.patching.metadata.ContentType;
import org.jboss.as.patching.metadata.LayerType;
import org.jboss.as.patching.metadata.MiscContentItem;
import org.jboss.as.patching.metadata.ModuleItem;
import org.jboss.as.patching.metadata.Patch;
import org.jboss.as.patching.metadata.PatchElement;
import org.jboss.as.patching.metadata.PatchElementProvider;
import org.jboss.as.patching.metadata.PatchImpl;
import org.jboss.as.patching.metadata.PatchXml;
import org.jboss.as.patching.metadata.RollbackPatch;
import org.jboss.as.patching.metadata.impl.IdentityImpl;
import org.jboss.as.patching.metadata.impl.PatchElementImpl;
import org.jboss.as.patching.tool.ContentVerificationPolicy;
import org.jboss.as.patching.tool.PatchingHistory;
import org.jboss.as.patching.tool.PatchingResult;

/**
 * @author Emanuel Muckenhuber
 */
class IdentityPatchContext implements PatchContentProvider {


    private final File miscBackup;
    private final File configBackup;
    private final File miscTargetRoot;

    private final PatchEntry identityEntry;
    private final InstalledImage installedImage;
    private final PatchContentProvider contentProvider;
    private final ContentVerificationPolicy contentPolicy;
    private final InstallationManager.InstallationModification modification;
    private final Map<String, PatchContentLoader> contentLoaders = new HashMap<String, PatchContentLoader>();
    private final PatchingHistory history;

    // TODO initialize layers in the correct order
    private final Map<String, PatchEntry> layers = new LinkedHashMap<String, PatchEntry>();
    private final Map<String, PatchEntry> addOns = new LinkedHashMap<String, PatchEntry>();

    private PatchingTaskContext.Mode mode;
    private volatile State state = State.NEW;
    private boolean checkForGarbageOnRestart; // flag to trigger a cleanup on restart
    private static final AtomicReferenceFieldUpdater<IdentityPatchContext, State> stateUpdater = AtomicReferenceFieldUpdater.newUpdater(IdentityPatchContext.class, State.class, "state");
    // The modules we need to invalidate
    private final List<File> moduleInvalidations = new ArrayList<File>();

    static enum State {

        NEW,
        PREPARED,
        COMPLETED,
        INVALIDATE,
        ROLLBACK_ONLY,
        ;

    }

    IdentityPatchContext(final File backup, final PatchContentProvider contentProvider, final ContentVerificationPolicy contentPolicy,
                         final InstallationManager.InstallationModification modification, final PatchingTaskContext.Mode mode,
                         final InstalledImage installedImage) {

        this.miscTargetRoot = installedImage.getJbossHome();

        this.mode = mode;
        this.contentProvider = contentProvider;
        this.contentPolicy = contentPolicy;
        this.modification = modification;
        this.installedImage = installedImage;
        this.history = PatchingHistory.Factory.getHistory(modification.getUnmodifiedInstallationState());

        if (backup != null) {
            this.miscBackup = new File(backup, PatchContentLoader.MISC);
            this.configBackup = new File(backup, Constants.CONFIGURATION);
        } else {
            this.miscBackup = null;     // This will trigger a failure when the root is actually needed
            this.configBackup = null;
        }
        this.identityEntry = new IdentityEntry(modification, null);
    }

    /**
     * Get the patch entry for the identity.
     *
     * @return the identity entry
     */
    PatchEntry getIdentityEntry() {
        return identityEntry;
    }

    /**
     * Get a patch entry for either a layer or add-on.
     *
     * @param name  the layer name
     * @param addOn whether the target is an add-on
     * @return the patch entry, {@code null} if it there is no such layer
     */
    PatchEntry getEntry(final String name, boolean addOn) {
        return addOn ? addOns.get(name) : layers.get(name);
    }

    /**
     * Get all entries.
     *
     * @return the entries for all layers
     */
    Collection<PatchEntry> getLayers() {
        return layers.values();
    }

    /**
     * Get all add-ons.
     *
     * @return the entries for all add-ons
     */
    Collection<PatchEntry> getAddOns() {
        return addOns.values();
    }

    /**
     * Get the current modification.
     *
     * @return the modification
     */
    InstallationManager.InstallationModification getModification() {
        return modification;
    }

    /**
     * Get the patch history.
     *
     * @return the history
     */
    PatchingHistory getHistory() {
        return history;
    }

    /**
     * Get the current mode.
     *
     * @return the mode
     */
    PatchingTaskContext.Mode getMode() {
        return mode;
    }

    /**
     * In case we cannot delete a directory create a marker to recheck whether we can garbage collect some not
     * referenced directories and files.
     *
     * @param file the directory
     */
    protected void failedToCleanupDir(final File file) {
        checkForGarbageOnRestart = true;
        PatchLogger.ROOT_LOGGER.cannotDeleteFile(file.getAbsolutePath());
    }

    @Override
    public PatchContentLoader getLoader(final String patchId) {
        final PatchContentLoader loader = contentLoaders.get(patchId);
        if (loader != null) {
            return loader;
        }
        return contentProvider.getLoader(patchId);
    }

    @Override
    public void cleanup() {
        // If cleanup gets called before finalizePatch, something went wrong
        if (state != State.PREPARED) {
            undoChanges();
        }
    }

    /**
     * Get the target entry for a given patch element.
     *
     * @param element the patch element
     * @return the patch entry
     * @throws PatchingException
     */
    protected PatchEntry resolveForElement(final PatchElement element) throws PatchingException {
        assert state == State.NEW;
        final PatchElementProvider provider = element.getProvider();
        final String layerName = provider.getName();
        final LayerType layerType = provider.getLayerType();

        final Map<String, PatchEntry> map;
        if (layerType == LayerType.Layer) {
            map = layers;
        } else {
            map = addOns;
        }
        PatchEntry entry = map.get(layerName);
        if (entry == null) {
            final InstallationManager.MutablePatchingTarget target = modification.resolve(layerName, layerType);
            if (target == null) {
                throw PatchMessages.MESSAGES.noSuchLayer(layerName);
            }
            entry = new PatchEntry(target, element);
            map.put(layerName, entry);
        }
        // Maintain the most recent element
        entry.updateElement(element);
        return entry;
    }

    /**
     * Finalize the patch.
     *
     * @param callback the finalize callback
     * @return the result
     * @throws Exception
     */
    protected PatchingResult finalize(final FinalizeCallback callback) throws Exception {
        assert state == State.NEW;
        final Patch original = callback.getPatch();
        final Patch.PatchType patchType = original.getIdentity().getPatchType();
        final String patchId;
        if (patchType == Patch.PatchType.CUMULATIVE) {
            patchId = modification.getCumulativePatchID();
        } else {
            patchId = original.getPatchId();
        }
        try {
            // The processed patch, based on the recorded changes
            final Patch processedPatch = createProcessedPatch(original);
            // The rollback containing all the recorded rollback actions
            final RollbackPatch rollbackPatch = createRollbackPatch(patchId, patchType);
            callback.finishPatch(processedPatch, rollbackPatch, this);
        } catch (Exception e) {
            if (undoChanges()) {
                callback.operationCancelled(this);
            }
            throw e;
        }
        state = State.PREPARED;
        return new PatchingResult() {
            @Override
            public String getPatchId() {
                return original.getPatchId();
            }

            @Override
            public PatchInfo getPatchInfo() {
                return new PatchInfo() {
                    @Override
                    public String getVersion() {
                        return identityEntry.getResultingVersion();
                    }

                    @Override
                    public String getCumulativePatchID() {
                        return identityEntry.delegate.getModifiedState().getCumulativePatchID();
                    }

                    @Override
                    public List<String> getPatchIDs() {
                        return identityEntry.delegate.getModifiedState().getPatchIDs();
                    }

                };
            }

            @Override
            public void commit() {
                if (state == State.PREPARED) {
                    complete(modification, callback);
                } else {
                    undoChanges();
                    throw new IllegalStateException();
                }
            }

            @Override
            public void rollback() {
                if (undoChanges()) {
                    try {
                        callback.operationCancelled(IdentityPatchContext.this);
                    } finally {
                        modification.cancel();
                    }
                }
            }
        };
    }

    /**
     * Cancel the current patch and undo the changes.
     *
     * @param callback the finalize callback
     */
    protected void cancel(final FinalizeCallback callback) {
        try {
            undoChanges();
        } finally {
            callback.operationCancelled(this);
        }
    }

    /**
     * Complete the current operation and persist the current state to the disk. This will also trigger the invalidation
     * of outdated modules.
     *
     * @param modification the current modification
     * @param callback     the completion callback
     */
    private void complete(final InstallationManager.InstallationModification modification, final FinalizeCallback callback) {
        final List<File> processed = new ArrayList<File>();
        try {
            try {
                // Update the state to invalidate and process module resources
                if (stateUpdater.compareAndSet(this, State.PREPARED, State.INVALIDATE)
                        && mode == PatchingTaskContext.Mode.APPLY) {
                    // Only invalidate modules when applying patches; on rollback files are immediately restored
                    for (final File invalidation : moduleInvalidations) {
                        processed.add(invalidation);
                        PatchModuleInvalidationUtils.processFile(invalidation, mode);
                    }
                }
                modification.complete();
                callback.completed(this);
                state = State.COMPLETED;
            } catch (Exception e) {
                this.moduleInvalidations.clear();
                this.moduleInvalidations.addAll(processed);
                throw new RuntimeException(e);
            }
        } finally {
            if (state != State.COMPLETED) {
                try {
                    modification.cancel();
                } finally {
                    try {
                        undoChanges();
                    } finally {
                        callback.operationCancelled(this);
                    }
                }
            } else  {
                try {
                    if (checkForGarbageOnRestart) {
                        final File cleanupMarker = new File(installedImage.getInstallationMetadata(), "cleanup-patching-dirs");
                        cleanupMarker.createNewFile();
                    }
                } catch (IOException e) {
                    PatchLogger.ROOT_LOGGER.infof(e, "failed to create cleanup marker");
                }
            }
        }
    }

    /**
     * Internally undo recorded changes we did so far.
     *
     * @return whether the state required undo actions
     */
    boolean undoChanges() {
        final State state = stateUpdater.getAndSet(this, State.ROLLBACK_ONLY);
        if (state == State.COMPLETED || state == State.ROLLBACK_ONLY) {
            // Was actually completed already
            return false;
        }
        PatchingTaskContext.Mode currentMode = this.mode;
        mode = PatchingTaskContext.Mode.UNDO;
        final PatchContentLoader loader = PatchContentLoader.create(miscBackup, null, null);
        // Undo changes for the identity
        undoChanges(identityEntry, loader);
        // TODO maybe check if we need to do something for the layers too !?
        if (state == State.INVALIDATE || currentMode == PatchingTaskContext.Mode.ROLLBACK) {
            // For apply the state needs to be invalidate
            // For rollback the files are invalidated as part of the tasks
            final PatchingTaskContext.Mode mode = currentMode == PatchingTaskContext.Mode.APPLY ? PatchingTaskContext.Mode.ROLLBACK : PatchingTaskContext.Mode.APPLY;
            for (final File file : moduleInvalidations) {
                try {
                    PatchModuleInvalidationUtils.processFile(file, mode);
                } catch (Exception e) {
                    PatchLogger.ROOT_LOGGER.debugf(e, "failed to restore state for %s", file);
                }
            }
        }
        return true;
    }

    /**
     * Undo changes for a single patch entry.
     *
     * @param entry  the patch entry
     * @param loader the content loader
     */
    static void undoChanges(final PatchEntry entry, final PatchContentLoader loader) {
        final List<ContentModification> modifications = new ArrayList<ContentModification>(entry.rollbackActions);
        for (final ContentModification modification : modifications) {
            final ContentItem item = modification.getItem();
            if (item.getContentType() != ContentType.MISC) {
                // Skip modules and bundles they should be removed as part of the {@link FinalizeCallback}
                continue;
            }
            final PatchingTaskDescription description = new PatchingTaskDescription(entry.applyPatchId, modification, loader, false, false);
            try {
                final PatchingTask task = PatchingTask.Factory.create(description, entry);
                task.execute(entry);
            } catch (Exception e) {
                PatchLogger.ROOT_LOGGER.warnf(e, "failed to undo change (%s)", modification);
            }
        }
    }

    /**
     * Add a rollback loader for a give patch.
     *
     * @param patchId the patch id.
     * @param target  the patchable target
     * @throws XMLStreamException
     * @throws IOException
     */
    private void recordRollbackLoader(final String patchId, PatchableTarget.TargetInfo target) {
        // setup the content loader paths
        final DirectoryStructure structure = target.getDirectoryStructure();
        final InstalledImage image = structure.getInstalledImage();
        final File historyDir = image.getPatchHistoryDir(patchId);
        final File miscRoot = new File(historyDir, PatchContentLoader.MISC);
        final File modulesRoot = structure.getModulePatchDirectory(patchId);
        final File bundlesRoot = structure.getBundlesPatchDirectory(patchId);
        final PatchContentLoader loader = PatchContentLoader.create(miscRoot, bundlesRoot, modulesRoot);
        //
        recordContentLoader(patchId, loader);
    }

    /**
     * Record a content loader for a given patch id.
     *
     * @param patchID       the patch id
     * @param contentLoader the content loader
     */
    protected void recordContentLoader(final String patchID, final PatchContentLoader contentLoader) {
        if (contentLoaders.containsKey(patchID)) {
            throw new IllegalStateException("Content loader already registered for patch " + patchID); // internal wrong usage, no i18n
        }
        contentLoaders.put(patchID, contentLoader);
    }

    /**
     * Whether a content verification can be ignored or not.
     *
     * @param item the content item to verify
     * @return
     */
    public boolean isIgnored(final ContentItem item) {
        return contentPolicy.ignoreContentValidation(item);
    }

    /**
     * Whether a content task execution can be excluded.
     *
     * @param item the content item
     * @return
     */
    public boolean isExcluded(final ContentItem item) {
        return contentPolicy.preserveExisting(item);
    }

    /**
     * Get the target file for misc items.
     *
     * @param item the misc item
     * @return the target location
     */
    public File getTargetFile(final MiscContentItem item) {
        final State state = this.state;
        if (state == State.NEW || state == State.ROLLBACK_ONLY) {
            return getTargetFile(miscTargetRoot, item);
        } else {
            throw new IllegalStateException(); // internal wrong usage, no i18n
        }
    }

    /**
     * Create a patch representing what we actually processed. This may contain some fixed content hashes for removed
     * modules.
     *
     * @param original the original
     * @return the processed patch
     */
    protected Patch createProcessedPatch(final Patch original) {

        // Process elements
        final List<PatchElement> elements = new ArrayList<PatchElement>();
        // Process layers
        for (final PatchEntry entry : getLayers()) {
            final PatchElement element = createPatchElement(entry, entry.element.getId(), entry.modifications);
            elements.add(element);
        }
        // Process add-ons
        for (final PatchEntry entry : getAddOns()) {
            final PatchElement element = createPatchElement(entry, entry.element.getId(), entry.modifications);
            elements.add(element);
        }

        // Swap the patch element modifications, keep the identity ones since we don't need to fix the misc modifications
        return new PatchImpl(original.getPatchId(), original.getDescription(), original.getIdentity(), elements, original.getModifications());
    }

    /**
     * Create a rollback patch based on the recorded actions.
     *
     * @param patchId   the new patch id, depending on release or one-off
     * @param patchType the current patch identity
     * @return the rollback patch
     */
    protected RollbackPatch createRollbackPatch(final String patchId, final Patch.PatchType patchType) {
        // Process elements
        final List<PatchElement> elements = new ArrayList<PatchElement>();
        // Process layers
        for (final PatchEntry entry : getLayers()) {
            final PatchElement element = createRollbackElement(entry);
            elements.add(element);
        }
        // Process add-ons
        for (final PatchEntry entry : getAddOns()) {
            final PatchElement element = createRollbackElement(entry);
            elements.add(element);
        }

        final InstalledIdentity installedIdentity = modification.getUnmodifiedInstallationState();
        final String name = installedIdentity.getIdentity().getName();
        final IdentityImpl identity = new IdentityImpl(name, modification.getVersion());
        if (patchType == Patch.PatchType.CUMULATIVE) {
            identity.setPatchType(Patch.PatchType.CUMULATIVE);
            identity.setResultingVersion(installedIdentity.getIdentity().getVersion());
        } else if (patchType == Patch.PatchType.ONE_OFF) {
            identity.setPatchType(Patch.PatchType.ONE_OFF);
        }
        final List<ContentModification> modifications = identityEntry.rollbackActions;
        final Patch delegate = new PatchImpl(patchId, "rollback patch", identity, elements, modifications);
        return new PatchImpl.RollbackPatchImpl(delegate, installedIdentity);
    }

    /**
     * Get a misc file.
     *
     * @param root the root
     * @param item the misc content item
     * @return the misc file
     */
    static File getTargetFile(final File root, final MiscContentItem item) {
        return PatchContentLoader.getMiscPath(root, item);
    }

    class IdentityEntry extends PatchEntry {

        IdentityEntry(InstallationManager.MutablePatchingTarget delegate, PatchElement element) {
            super(delegate, element);
        }

        @Override
        protected String getResultingVersion() {
            return modification.getVersion();
        }

        @Override
        public void setResultingVersion(String resultingVersion) {
            modification.setResultingVersion(resultingVersion);
        }
    }

    /**
     * Modification information for a patchable target.
     */
    class PatchEntry implements InstallationManager.MutablePatchingTarget, PatchingTaskContext {

        private String applyPatchId;
        private PatchElement element;
        private final InstallationManager.MutablePatchingTarget delegate;
        private final List<ContentModification> modifications = new ArrayList<ContentModification>();
        private final List<ContentModification> rollbackActions = new ArrayList<ContentModification>();
        private final Map<Location, PatchingTasks.ContentTaskDefinition> definitions = new LinkedHashMap<Location, PatchingTasks.ContentTaskDefinition>();
        private final Set<String> rollbacks = new HashSet<String>();

        PatchEntry(final InstallationManager.MutablePatchingTarget delegate, final PatchElement element) {
            assert delegate != null;
            this.delegate = delegate;
            this.element = element;
        }

        protected void updateElement(final PatchElement element) {
            this.element = element;
        }

        protected String getResultingVersion() {
            throw new IllegalStateException();
        }

        public void setResultingVersion(String resultingVersion) {
            throw new IllegalStateException();
        }

        @Override
        public boolean isApplied(String patchId) {
            return delegate.isApplied(patchId);
        }

        @Override
        public void rollback(String patchId) {
            rollbacks.add(patchId);
            // Rollback
            delegate.rollback(patchId);
            // Record rollback loader
            recordRollbackLoader(patchId, delegate);
        }

        @Override
        public void apply(String patchId, Patch.PatchType patchType) {
            delegate.apply(patchId, patchType);
            applyPatchId = patchId;
        }

        @Override
        public String getCumulativePatchID() {
            return delegate.getCumulativePatchID();
        }

        @Override
        public List<String> getPatchIDs() {
            return delegate.getPatchIDs();
        }

        @Override
        public Properties getProperties() {
            return delegate.getProperties();
        }

        @Override
        public DirectoryStructure getDirectoryStructure() {
            return delegate.getDirectoryStructure();
        }

        public Map<Location, PatchingTasks.ContentTaskDefinition> getDefinitions() {
            return definitions;
        }

        @Override
        public File getBackupFile(MiscContentItem item) {
            if (state == State.NEW) {
                return IdentityPatchContext.getTargetFile(miscBackup, item);
            } else if (state == State.ROLLBACK_ONLY) {
                // No backup when we undo the changes
                return null;
            } else {
                throw new IllegalStateException(); // internal wrong usage, no i18n
            }
        }

        @Override
        public boolean isExcluded(ContentItem contentItem) {
            return contentPolicy.preserveExisting(contentItem);
        }

        @Override
        public void recordChange(final ContentModification change, final ContentModification rollbackAction) {
            if (state == State.ROLLBACK_ONLY) {
                // don't record undo tasks
                return;
            }
            // Only misc remove is null, but we replace it with the
            if (change != null) {
                modifications.add(change);
            }
            if (rollbackAction != null) {
                rollbackActions.add(rollbackAction);
            }
        }

        @Override
        public Mode getCurrentMode() {
            return mode;
        }

        @Override
        public PatchableTarget.TargetInfo getModifiedState() {
            return delegate.getModifiedState();
        }

        @Override
        public File[] getTargetBundlePath() {
            // We need the updated state for invalidating one-off patches
            // When applying the overlay directory should not exist yet
            final PatchableTarget.TargetInfo updated = mode == Mode.APPLY ? delegate : delegate.getModifiedState();
            return PatchUtils.getBundlePath(delegate.getDirectoryStructure(), updated);
        }

        @Override
        public File[] getTargetModulePath() {
            // We need the updated state for invalidating one-off patches
            // When applying the overlay directory should not exist yet
            final PatchableTarget.TargetInfo updated = mode == Mode.APPLY ? delegate : delegate.getModifiedState();
            return PatchUtils.getModulePath(delegate.getDirectoryStructure(), updated);
        }

        @Override
        public File getTargetFile(ContentItem item) {
            if (item.getContentType() == ContentType.MISC) {
                return IdentityPatchContext.this.getTargetFile((MiscContentItem) item);
            }
            if (applyPatchId == null || state == State.ROLLBACK_ONLY) {
                throw new IllegalStateException("cannot process rollback tasks for modules/bundles"); // internal wrong usage, no i18n
            }
            final File root;
            final DirectoryStructure structure = delegate.getDirectoryStructure();
            if (item.getContentType() == ContentType.BUNDLE) {
                root = structure.getBundlesPatchDirectory(applyPatchId);
            } else {
                root = structure.getModulePatchDirectory(applyPatchId);
            }
            return PatchContentLoader.getModulePath(root, (ModuleItem) item);
        }

        @Override
        public void invalidateRoot(final File moduleRoot) throws IOException {
            final File[] files = moduleRoot.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.endsWith(".jar");
                }
            });
            if (files != null && files.length > 0) {
                for (final File file : files) {
                    moduleInvalidations.add(file);
                    if (mode == Mode.ROLLBACK) {
                        // For rollback we need to restore the file before calculating the hash
                        PatchModuleInvalidationUtils.processFile(file, mode);
                    }
                }
            }
        }

        /**
         * Cleanup the history directories for all recorded rolled back patches.
         */
        protected void cleanupRollbackPatchHistory() {
            final DirectoryStructure structure = getDirectoryStructure();
            for (final String rollback : rollbacks) {
                if (!IoUtils.recursiveDelete(structure.getBundlesPatchDirectory(rollback))) {
                    failedToCleanupDir(structure.getBundlesPatchDirectory(rollback));
                }
                if (!IoUtils.recursiveDelete(structure.getModulePatchDirectory(rollback))) {
                    failedToCleanupDir(structure.getModulePatchDirectory(rollback));
                }
            }
        }
    }

    /**
     * Patch finalization callback.
     */
    interface FinalizeCallback {

        /**
         * Get the original patch.
         *
         * @return the patch
         */
        Patch getPatch();

        /**
         * Finish step after the content modification were executed.
         *
         * @param processedPatch the processed patch
         * @param rollbackPatch  the rollback patch
         * @param context        the patch context
         * @throws Exception
         */
        void finishPatch(Patch processedPatch, RollbackPatch rollbackPatch, IdentityPatchContext context) throws Exception;

        /**
         * Completed.
         *
         * @param context the context
         */
        void completed(IdentityPatchContext context);

        /**
         * Cancelled.
         *
         * @param context the context
         */
        void operationCancelled(IdentityPatchContext context);
    }

    /**
     * Create a patch element for the rollback patch.
     *
     * @param entry the entry
     * @return the new patch element
     */
    protected static PatchElement createRollbackElement(final PatchEntry entry) {
        final PatchElement patchElement = entry.element;
        final String patchId;
        final Patch.PatchType patchType = patchElement.getProvider().getPatchType();
        if (patchType == Patch.PatchType.CUMULATIVE) {
            patchId = entry.getCumulativePatchID();
        } else {
            patchId = patchElement.getId();
        }
        return createPatchElement(entry, patchId, entry.rollbackActions);
    }

    /**
     * Copy a patch element
     *
     * @param entry         the patch entry
     * @param patchId       the patch id for the element
     * @param modifications the element modifications
     * @return the new patch element
     */
    protected static PatchElement createPatchElement(final PatchEntry entry, String patchId, final List<ContentModification> modifications) {
        final PatchElement patchElement = entry.element;
        final PatchElementImpl element = new PatchElementImpl(patchId);
        element.setProvider(patchElement.getProvider());
        // Add all the rollback actions
        element.getModifications().addAll(modifications);
        return element;
    }

    /**
     * Backup the current configuration as part of the patch history.
     *
     * @throws IOException for any error
     */
    void backupConfiguration() throws IOException {

        final String configuration = Constants.CONFIGURATION;

        final File a = new File(installedImage.getAppClientDir(), configuration);
        final File d = new File(installedImage.getDomainDir(), configuration);
        final File s = new File(installedImage.getStandaloneDir(), configuration);

        if (a.exists()) {
            final File ab = new File(configBackup, Constants.APP_CLIENT);
            backupDirectory(a, ab);
        }
        if (d.exists()) {
            final File db = new File(configBackup, Constants.DOMAIN);
            backupDirectory(d, db);
        }
        if (s.exists()) {
            final File sb = new File(configBackup, Constants.STANDALONE);
            backupDirectory(s, sb);
        }

    }

    static final FileFilter CONFIG_FILTER = new FileFilter() {

        @Override
        public boolean accept(File pathName) {
            return pathName.isFile() && pathName.getName().endsWith(".xml");
        }
    };

    /**
     * Backup all xml files in a given directory.
     *
     * @param source the source directory
     * @param target the target directory
     * @throws IOException for any error
     */
    static void backupDirectory(final File source, final File target) throws IOException {
        if (!target.exists()) {
            if (!target.mkdirs()) {
                throw PatchMessages.MESSAGES.cannotCreateDirectory(target.getAbsolutePath());
            }
        }
        final File[] files = source.listFiles(CONFIG_FILTER);
        for (final File file : files) {
            final File t = new File(target, file.getName());
            IoUtils.copyFile(file, t);
        }
    }


    /**
     * Restore the configuration. Depending on reset-configuration this is going to replace the original files with the
     * backup, otherwise it will create a restored-configuration folder the configuration directories.
     * <p/>
     * TODO log a warning if the restored configuration files are different from the current one?
     * or should we check that before rolling back the patch to give the user a chance to save the changes
     *
     * @param rollingBackPatchID the patch id
     * @param resetConfiguration whether to override the configuration files or not
     * @throws IOException for any error
     */
    void restoreConfiguration(final String rollingBackPatchID, final boolean resetConfiguration) throws IOException {

        final File backupConfigurationDir = new File(installedImage.getPatchHistoryDir(rollingBackPatchID), Constants.CONFIGURATION);
        final File ba = new File(backupConfigurationDir, Constants.APP_CLIENT);
        final File bd = new File(backupConfigurationDir, Constants.DOMAIN);
        final File bs = new File(backupConfigurationDir, Constants.STANDALONE);

        final String configuration;
        if (resetConfiguration) {
            configuration = Constants.CONFIGURATION;
        } else {
            configuration = Constants.CONFIGURATION + File.separator + Constants.RESTORED_CONFIGURATION;
        }

        if (ba.exists()) {
            final File a = new File(installedImage.getAppClientDir(), configuration);
            backupDirectory(ba, a);
        }
        if (bd.exists()) {
            final File d = new File(installedImage.getDomainDir(), configuration);
            backupDirectory(bd, d);
        }
        if (bs.exists()) {
            final File s = new File(installedImage.getStandaloneDir(), configuration);
            backupDirectory(bs, s);
        }
    }

    /**
     * Write the patch.xml
     *
     * @param rollbackPatch the patch
     * @param file          the target file
     * @throws IOException
     */
    static void writePatch(final Patch rollbackPatch, final File file) throws IOException {
        final File parent = file.getParentFile();
        if (!parent.isDirectory()) {
            if (!parent.mkdirs() && !parent.exists()) {
                throw PatchMessages.MESSAGES.cannotCreateDirectory(file.getAbsolutePath());
            }
        }
        try {
            final OutputStream os = new FileOutputStream(file);
            try {
                PatchXml.marshal(os, rollbackPatch);
            } finally {
                IoUtils.safeClose(os);
            }
        } catch (XMLStreamException e) {
            throw new IOException(e);
        }
    }

}

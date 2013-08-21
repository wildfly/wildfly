package org.jboss.as.patching.runner;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

import org.jboss.as.patching.Constants;
import org.jboss.as.patching.DirectoryStructure;
import org.jboss.as.patching.IoUtils;
import org.jboss.as.patching.PatchInfo;
import org.jboss.as.patching.PatchLogger;
import org.jboss.as.patching.PatchMessages;
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

/**
 * @author Emanuel Muckenhuber
 */
class IdentityPatchContext implements PatchContentProvider {

    private final File miscTargetRoot;

    private final File configBackup;
    private final File miscBackup;

    private final PatchEntry identityEntry;
    private final InstalledImage installedImage;
    private final PatchContentProvider contentProvider;
    private final ContentVerificationPolicy contentPolicy;
    private final InstallationManager.InstallationModification modification;

    private final Map<String, PatchContentLoader> contentLoaders = new HashMap<String, PatchContentLoader>();

    // TODO initialize layers in the correct order
    private final Map<String, PatchEntry> layers = new LinkedHashMap<String, PatchEntry>();
    private final Map<String, PatchEntry> addOns = new LinkedHashMap<String, PatchEntry>();

    private volatile State state = State.NEW;
    private static final AtomicReferenceFieldUpdater<IdentityPatchContext, State> stateUpdater = AtomicReferenceFieldUpdater.newUpdater(IdentityPatchContext.class, State.class, "state");

    static enum State {

        NEW,
        FINISHED,
        COMPLETED,
        ROLLBACK_ONLY,;

    }

    IdentityPatchContext(final File backup, final PatchContentProvider contentProvider, final ContentVerificationPolicy contentPolicy,
                         final InstallationManager.InstallationModification modification, final InstalledImage installedImage) {

        this.miscTargetRoot = installedImage.getJbossHome();

        this.contentProvider = contentProvider;
        this.contentPolicy = contentPolicy;
        this.modification = modification;
        this.installedImage = installedImage;

        if (backup != null) {
            this.miscBackup = new File(backup, PatchContentLoader.MISC);
            this.configBackup = new File(backup, Constants.CONFIGURATION);
        } else {
            this.miscBackup = null;
            this.configBackup = null;
        }
        this.identityEntry = new PatchEntry(modification, null);
    }

    /**
     * Get the patch entry for the identity.
     *
     * @return the identity entry
     */
    PatchEntry getIdentityEntry() {
        return identityEntry;
    }

    Collection<PatchEntry> getLayers() {
        return layers.values();
    }

    Collection<PatchEntry> getAddOns() {
        return addOns.values();
    }

    InstallationManager.InstallationModification getModification() {
        return modification;
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
        // If cleanup gets called before we return, something went wrong
        if (state != State.FINISHED) {
            undoChanges();
        }
    }

    @Override
    public File getPatchContentRootDir() {
        return contentProvider.getPatchContentRootDir();
    }

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
            entry = new PatchEntry(target, element);
            map.put(layerName, entry);
        }
        if (entry == null) {
            throw new PatchingException("failed to resolve target for " + element);
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
        if (patchType == Patch.PatchType.UPGRADE) {
            patchId = modification.getReleasePatchID();
        } else if (patchType == Patch.PatchType.CUMULATIVE) {
            patchId = modification.getCumulativeID();
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
                callback.operationCancelled();
            }
            throw e;
        }
        state = State.FINISHED;
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
                    public String getReleasePatchID() {
                        return identityEntry.delegate.getModifiedState().getReleasePatchID();
                    }

                    @Override
                    public String getCumulativePatchID() {
                        return identityEntry.delegate.getModifiedState().getCumulativeID();
                    }

                    @Override
                    public List<String> getPatchIDs() {
                        return identityEntry.delegate.getModifiedState().getPatchIDs();
                    }

                };
            }

            @Override
            public void commit() {
                if (state == State.FINISHED) {
                    try {
                        callback.completed();
                        modification.complete();
                        state = State.COMPLETED;
                    } catch (Exception e) {
                        undoChanges(); // Hmm, is this right here?
                        throw new RuntimeException(e);
                    }
                } else {
                    undoChanges();
                    throw new IllegalStateException();
                }
            }

            @Override
            public void rollback() {
                if (undoChanges()) {
                    try {
                        callback.operationCancelled();
                    } finally {
                        modification.cancel();
                    }
                }
            }
        };
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
        final PatchContentLoader loader = PatchContentLoader.create(miscBackup, null, null);
        // Undo changes for the identity
        undoChanges(identityEntry, loader);
        // TODO maybe check if we need to do something for the layers too !?
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
    protected void recordRollbackLoader(final String patchId, PatchableTarget.TargetInfo target) {
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

    protected void recordContentLoader(final String patchID, final PatchContentLoader contentLoader) {
        if (contentLoaders.containsKey(patchID)) {
            throw new IllegalStateException();
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
            throw new IllegalStateException();
        }

    }

    /**
     * Create a patch representing what we actually processed. This may contain additional items in case we had
     * to port forward modules which were not changed in the actual patch.
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

        // Swap the patch element modifications, keep the identity ones since we don't need to track misc items
        return new PatchImpl(original.getPatchId(), original.getDescription(), original.getIdentity(), elements, original.getModifications());
    }

    /**
     * Create a rollback patch based on the recorded actions.
     *
     * @param patchId          the new patch id, depending on release or one-off
     * @param patchType        the current patch identity
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
        if (patchType == Patch.PatchType.UPGRADE) {
            identity.setPatchType(Patch.PatchType.UPGRADE);
            identity.setResultingVersion(installedIdentity.getIdentity().getVersion());
        } else if (patchType == Patch.PatchType.ONE_OFF) {
            identity.setPatchType(Patch.PatchType.ONE_OFF);
            identity.setCumulativePatchId(modification.getCumulativeID());
        }
        final List<ContentModification> modifications = identityEntry.rollbackActions;
        final Patch delegate = new PatchImpl(patchId, "rollback patch", identity, elements, modifications);
        return new PatchImpl.RollbackPatchImpl(delegate, installedIdentity);
    }

    static File getTargetFile(final File root, final MiscContentItem item) {
        return PatchContentLoader.getMiscPath(root, item);
    }

    class PatchEntry implements InstallationManager.MutablePatchingTarget, PatchingTaskContext {

        private String applyPatchId;
        private String resultingVersion;
        private PatchElement element;
        private final InstallationManager.MutablePatchingTarget delegate;
        private final List<ContentModification> modifications = new ArrayList<ContentModification>();
        private final List<ContentModification> rollbackActions = new ArrayList<ContentModification>();
        private final Map<Location, PatchingTasks.ContentTaskDefinition> definitions = new LinkedHashMap<Location, PatchingTasks.ContentTaskDefinition>();

        PatchEntry(final InstallationManager.MutablePatchingTarget delegate, final PatchElement element) {
            assert delegate != null;
            this.delegate = delegate;
            this.element = element;
            this.resultingVersion = modification.getVersion();
        }

        protected void updateElement(final PatchElement element) {
            this.element = element;
        }

        protected String getResultingVersion() {
            return resultingVersion;
        }

        public void setResultingVersion(String resultingVersion) {
            this.resultingVersion = resultingVersion;
        }

        @Override
        public boolean isApplied(String patchId) {
            return delegate.isApplied(patchId);
        }

        @Override
        public void rollback(String patchId) {
            // Rollback
            delegate.rollback(patchId);
            // Record rollback loader
            recordRollbackLoader(patchId, delegate);
        }

        @Override
        public void apply(String patchId, Patch.PatchType patchType) {
//            if (applyPatchId != null) {
//                throw new IllegalStateException("can only apply a single patch to a layer");
//            }
            delegate.apply(patchId, patchType);
            applyPatchId = patchId;
        }

        @Override
        public String getReleasePatchID() {
            return delegate.getReleasePatchID();
        }

        @Override
        public String getCumulativeID() {
            return delegate.getCumulativeID();
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
                return IdentityPatchContext.this.getTargetFile(miscBackup, item);
            } else if (state == State.ROLLBACK_ONLY) {
                return null;
            } else {
                throw new IllegalStateException();
            }
        }

        @Override
        public boolean isExcluded(ContentItem contentItem) {
            return contentPolicy.preserveExisting(contentItem);
        }

        @Override
        public void recordChange(final ContentModification change, final ContentModification rollbackAction) {
            // Only misc remove is null, but we replace it with the
            if (change != null) {
                modifications.add(change);
            }
            if (rollbackAction != null) {
                rollbackActions.add(rollbackAction);
            }
        }

        @Override
        public PatchableTarget.TargetInfo getModifiedState() {
            return delegate.getModifiedState();
        }

        @Override
        public File[] getTargetBundlePath() {
            // We need the updated state for invalidating one-off patches
            // When applying the overlay directory should not exist yet (in theory)
            final PatchableTarget.TargetInfo updated = delegate.getModifiedState();
            return PatchUtils.getBundlePath(delegate.getDirectoryStructure(), updated);
        }

        @Override
        public File[] getTargetModulePath() {
            // We need the updated state for invalidating one-off patches
            // When applying the overlay directory should not exist yet (in theory)
            final PatchableTarget.TargetInfo updated = delegate.getModifiedState();
            return PatchUtils.getModulePath(delegate.getDirectoryStructure(), updated);
        }

        @Override
        public File getTargetFile(ContentItem item) {
            if (item.getContentType() == ContentType.MISC) {
                return IdentityPatchContext.this.getTargetFile((MiscContentItem) item);
            }
            if (applyPatchId == null || state == State.ROLLBACK_ONLY) {
                throw new IllegalStateException("cannot process rollback tasks for modules/bundles");
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

        void completed();

        void operationCancelled();
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
        if (patchType == Patch.PatchType.UPGRADE) {
            patchId = entry.getReleasePatchID();
        } else if (patchType == Patch.PatchType.CUMULATIVE) {
            patchId = entry.getCumulativeID();
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

    // TODO log a warning if the restored configuration files are different from the current one?
    // or should we check that before rolling back the patch to give the user a chance to save the changes
    void restoreConfiguration(final String rollingBackPatchID) throws IOException {
        final String configuration = Constants.CONFIGURATION;

        File backupConfigurationDir = new File(installedImage.getPatchHistoryDir(rollingBackPatchID), configuration);
        final File ba = new File(backupConfigurationDir, Constants.APP_CLIENT);
        final File bd = new File(backupConfigurationDir, Constants.DOMAIN);
        final File bs = new File(backupConfigurationDir, Constants.STANDALONE);

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

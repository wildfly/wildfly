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
import org.jboss.as.patching.metadata.ContentItem;
import org.jboss.as.patching.metadata.ContentModification;
import org.jboss.as.patching.metadata.ContentType;
import org.jboss.as.patching.metadata.MiscContentItem;
import org.jboss.as.patching.metadata.ModuleItem;
import org.jboss.as.patching.metadata.Patch;
import org.jboss.as.patching.metadata.PatchXml;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Emanuel Muckenhuber
 */
class PatchingContext {

    private final Patch patch;
    private final PatchInfo info;
    private final PatchContentLoader loader;
    private final DirectoryStructure structure;

    private final File target;
    private final File backup;
    private final File miscBackup;

    private ContentVerificationPolicy verificationPolicy;
    private final List<ContentModification> rollbackActions = new ArrayList<ContentModification>();

    private boolean rollbackOnly;

    /**
     * Create a patching context.
     *
     * @param patch the patch
     * @param info the current patch info
     * @param structure the directory structure
     * @param policy the content verification policy
     * @param workDir the current work dir
     * @return the patching context
     */
    static PatchingContext create(final Patch patch, final PatchInfo info, final DirectoryStructure structure, final ContentVerificationPolicy policy, final File workDir) {
        //
        final File backup = structure.getHistoryDir(patch.getPatchId());
        final PatchContentLoader loader = PatchContentLoader.create(workDir);
        return new PatchingContext(patch, info, structure, backup, policy, loader);
    }

    /**
     * Create a patching context for rollback actions.
     *
     * @param patch the patch
     * @param current the current patch info
     * @param structure the directory structure
     * @param workDir the current work dir
     * @return the patching context for rollback
     */
    static PatchingContext createForRollback(final Patch patch, final PatchInfo current, final DirectoryStructure structure, final File workDir) {
        // backup is just a temp dir
        final File backup = workDir;
        // setup the content loader paths
        final String patchId = patch.getPatchId();
        final File historyDir = structure.getHistoryDir(patchId);
        final File miscRoot = new File(historyDir, PatchContentLoader.MISC);
        final File modulesRoot = structure.getModulePatchDirectory(patchId);
        final File bundlesRoot = structure.getBundlesPatchDirectory(patchId);
        final PatchContentLoader loader = new PatchContentLoader(miscRoot, bundlesRoot, modulesRoot);
        //
        return new PatchingContext(patch, current, structure, backup, ContentVerificationPolicy.OVERRIDE_ALL, loader);
    }

    private PatchingContext(final Patch patch, final PatchInfo info, final DirectoryStructure structure,
                            final File backup, final ContentVerificationPolicy policy, final PatchContentLoader loader) {
        this.patch = patch;
        this.info = info;
        this.loader = loader;
        this.backup = backup;
        this.structure = structure;
        this.verificationPolicy = policy;
        this.miscBackup = new File(backup, PatchContentLoader.MISC);
        this.target = structure.getInstalledImage().getJbossHome();
    }

    /**
     * Get the current patch info.
     *
     * @return the patch info
     */
    public PatchInfo getPatchInfo() {
        return info;
    }

    /**
     * Get the patch content loader.
     *
     * @return the patch content loader
     */
    public PatchContentLoader getLoader() {
        return loader;
    }

    /**
     * Get the target directory for the module or bundle items.
     *
     * @return the module item
     */
    public File getModulePatchDirectory(final ModuleItem item) {
        final File root;
        final String patchId = patch.getPatchId();
        if(item.getContentType() == ContentType.BUNDLE) {
            root = structure.getBundlesPatchDirectory(patchId);
        } else {
            root = structure.getModulePatchDirectory(patchId);
        }
        return PatchContentLoader.getModulePath(root, item);
    }

    /**
     * Get the target file for misc items.
     *
     * @param item the misc item
     * @return the target location
     */
    public File getTargetFile(final MiscContentItem item) {
        return getTargetFile(target, item);
    }

    /**
     * Get the backup location for misc items.
     *
     * @param item the misc item
     * @return the backup location
     */
    public File getBackupFile(final MiscContentItem item) {
        return getTargetFile(miscBackup, item);
    }

    /**
     * Whether a content verification can be ignored or not.
     *
     * @param item the content item to verify
     * @return
     */
    public boolean isIgnored(final ContentItem item) {
        return verificationPolicy.ignoreContentValidation(item);
    }

    /**
     * Whether a content task execution can be excluded.
     *
     * @param item the content item
     * @return
     */
    public boolean isExcluded(final ContentItem item) {
        return verificationPolicy.preserveExisting(item);
    }

    /**
     * Record a content action for rollback.
     *
     * @param modification the modification
     */
    protected void recordRollbackAction(final ContentModification modification) {
        rollbackActions.add(modification);
    }

    /**
     * Finish the patch.
     *
     * @param patch the patch
     * @return the patching result
     * @throws PatchingException
     */
    PatchingResult finish(Patch patch) throws PatchingException {
        assert ! rollbackOnly;
        // Create the new info
        final String patchId = patch.getPatchId();
        final PatchInfo newInfo;
        if(Patch.PatchType.ONE_OFF == patch.getPatchType()) {
            final List<String> patches = new ArrayList<String>(info.getPatchIDs());
            patches.add(0, patchId);
            final String resultingVersion = info.getVersion();
            newInfo = new LocalPatchInfo(resultingVersion, info.getCumulativeID(), patches, info.getEnvironment());
        } else {
            final String resultingVersion = patch.getResultingVersion();
            newInfo = new LocalPatchInfo(resultingVersion, patchId, Collections.<String>emptyList(), info.getEnvironment());
        }
        // Backup the current active patch Info
        final File cumulativeBackup = new File(backup, DirectoryStructure.CUMULATIVE);
        final File referencesBackup = new File(backup, DirectoryStructure.REFERENCES);
        try {
            PatchUtils.writeRef(cumulativeBackup, info.getCumulativeID());
            PatchUtils.writeRefs(referencesBackup, info.getPatchIDs());
        } catch (IOException e) {
            throw  new PatchingException(e);
        }

        // Persist the patch rollback information
        final Patch newPatch = new RollbackPatch();
        final File patchXml = new File(backup, PatchXml.PATCH_XML);
        try {
            final OutputStream os = new FileOutputStream(patchXml);
            try {
                PatchXml.marshal(os, newPatch);
            } finally {
                PatchUtils.safeClose(os);
            }
        } catch (XMLStreamException e) {
            throw new PatchingException(e);
        } catch (IOException e) {
            throw new PatchingException(e);
        }
        try {
            // Persist
            persist(newInfo);
            // Rollback to the previous info
            return new PatchingResult() {
                @Override
                public PatchInfo getPatchInfo() {
                    return newInfo;
                }

                @Override
                public void rollback() {
                    try {
                        // Persist the original info
                        persist(info);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            };
        } catch (Exception e) {
            try {
                // Try to persist the current information
                persist(info);
            } catch (Exception ex) {
                PatchLogger.ROOT_LOGGER.debugf(ex, "failed to persist current version");
            }
            throw new PatchingException(e);
        }
    }

    /**
     * Undo the changes we've done so far.
     */
    void undo() {
        //
        rollbackOnly = true;
        // Use the misc backup location for the content items
        final PatchContentLoader loader = new PatchContentLoader(miscBackup, null, null);
        final File backup = null; // We skip the prepare step, so there should be no backup
        final PatchingContext undoContext = new PatchingContext(patch, info, structure, backup, ContentVerificationPolicy.OVERRIDE_ALL, loader);

        for(final ContentModification modification : rollbackActions) {
            final PatchingTask task = PatchingTask.Factory.create(modification, undoContext);
            try {
                // Just execute the task directly and copy the files back
                task.execute(undoContext);
            } catch (Exception e) {
                PatchLogger.ROOT_LOGGER.warnf(e, "failed to undo change (%s)", modification);
            }
        }
        try {
            persist(info);
        } catch (Exception e) {
            PatchLogger.ROOT_LOGGER.warnf(e, "failed to persist info (%s)", info);
        }
    }

    /**
     * Persist the changes.
     *
     * @param newPatchInfo the patch
     * @return the new patch info
     */
    PatchInfo persist(final PatchInfo newPatchInfo) throws IOException {
        final DirectoryStructure environment = newPatchInfo.getEnvironment();
        final String cumulativeID = newPatchInfo.getCumulativeID();
        PatchUtils.writeRef(environment.getCumulativeLink(), newPatchInfo.getCumulativeID());
        PatchUtils.writeRefs(environment.getCumulativeRefs(cumulativeID), newPatchInfo.getPatchIDs());
        return newPatchInfo;
    }

    static File getTargetFile(final File root, final MiscContentItem item)  {
        return PatchContentLoader.getMiscPath(root,  item);
    }

    class RollbackPatch implements Patch {

        @Override
        public String getPatchId() {
            return patch.getPatchId();
        }

        @Override
        public String getDescription() {
            return patch.getDescription();
        }

        @Override
        public PatchType getPatchType() {
            return patch.getPatchType();
        }

        @Override
        public String getResultingVersion() {
            return info.getVersion();
        }

        @Override
        public List<String> getAppliesTo() {
            if(getPatchType() == PatchType.CUMULATIVE) {
                return Collections.singletonList(patch.getResultingVersion());
            } else {
                return Collections.singletonList(info.getVersion());
            }
        }

        @Override
        public List<ContentModification> getModifications() {
            return rollbackActions;
        }
    }

}

/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.jboss.as.patching.tool;

import static org.jboss.as.patching.Constants.BASE;
import static org.jboss.as.patching.metadata.Patch.PatchType.CUMULATIVE;
import static org.jboss.as.patching.metadata.Patch.PatchType.ONE_OFF;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import org.jboss.as.patching.Constants;
import org.jboss.as.patching.PatchingException;
import org.jboss.as.patching.installation.InstalledIdentity;
import org.jboss.as.patching.installation.PatchableTarget;
import org.jboss.as.patching.installation.PatchableTarget.TargetInfo;
import org.jboss.as.patching.management.PatchManagementMessages;
import org.jboss.as.patching.metadata.Patch;
import org.jboss.as.patching.metadata.PatchBuilder;
import org.jboss.as.patching.metadata.PatchElement;
import org.jboss.as.patching.metadata.PatchXml;
import org.jboss.as.patching.metadata.RollbackPatch;
import org.jboss.as.patching.metadata.Patch.PatchType;
import org.jboss.as.patching.runner.PatchUtils;
import org.jboss.dmr.ModelNode;

/**
 * Provides a read-only view to the patching history of the installation.
 *
 * @author Alexey Loubyansky
 */
public interface PatchingHistory {

    /**
     * Information about a patch.
     *
     * @author Alexey Loubyansky
     */
    public interface Entry {
        String getPatchId();
        Patch.PatchType getType();
        String getAppliedAt();

        /**
         * Patch element ids by layer names they are applied to.
         *
         * @return  map of patch element ids by layer names they are applied to
         */
        Map<String,String> getLayerPatches();
        Map<String,String> getAddOnPatches();

    }

    public interface Iterator extends java.util.Iterator<Entry> {
        /**
         * Whether there is still a cumulative patch.
         *
         * @return  true if there is still a cumulative patch, otherwise - false
         */
        boolean hasNextCP();

        /**
         * Skips all the one-off patches (if any) and moves straight to
         * the next cumulative patch. If there is no cumulative patch left,
         * NoSuchElementException will be thrown.
         *
         * @return  next cumulative patch or throw NoSuchElementException
         *          if no more cumulative patches left
         */
        Entry nextCP();
    }

    /**
     * Returns the history as a list of ModelNode's
     * Entry node has the following attributes:
     * - patch-id - the id of the patch;
     * - type - the type of the patch (cumulative or one-off);
     * - applied-at - a timestamp the patch was applied at.
     *
     * @return  returns a list of entries representing basic info
     *          about the patches applied or an empty list if
     *          there is no patching information
     * @throws PatchingException  in case there was an error loading the history
     */
    ModelNode getHistory() throws PatchingException;

    /**
     * Same as getHistory() but for the specified target,
     * i.e. specific point.
     *
     * @param info  the point from which to load the history
     * @return  returns a list of entries representing basic info
     *          about the patches applied or an empty list if
     *          there is no patching information
     * @throws PatchingException  in case there was an error loading the history
     */
    ModelNode getHistory(PatchableTarget.TargetInfo info) throws PatchingException;

    /**
     * Returns an iterator over the history.
     *
     * @return  iterator over the patching history
     *
     * @throws PatchingException  in case there was an error loading the history
     */
    Iterator iterator() throws PatchingException;

    /**
     * Same as iterator() but starting from a specific point.
     *
     * @param info  the point to start from
     * @return  iterator over the patching history
     * @throws PatchingException  in case there was an error loading the history
     */
    Iterator iterator(final PatchableTarget.TargetInfo info) throws PatchingException;

    public class Factory {

        private Factory() {}

        public static ModelNode getHistory(InstalledIdentity installedImage, PatchableTarget.TargetInfo info) throws PatchingException {
            final ModelNode result = new ModelNode();
            result.setEmptyList();
            fillHistoryIn(installedImage, info, result);
            return result;
        }

        public static Iterator iterator(final InstalledIdentity mgr, final PatchableTarget.TargetInfo info) {
            if(info == null) {
                throw new IllegalArgumentException("target info is null");
            }
            return new IteratorImpl(info, mgr);
        }

        public static PatchingHistory getHistory(final InstalledIdentity mgr) {
            if(mgr == null) {
                throw new IllegalStateException("installedImage is null");
            }

            return new PatchingHistory() {

                @Override
                public ModelNode getHistory() throws PatchingException {
                    try {
                        return getHistory(mgr.getIdentity().loadTargetInfo());
                    } catch (IOException e) {
                        throw new PatchingException(PatchManagementMessages.MESSAGES.failedToLoadIdentity(), e);
                    }
                }

                @Override
                public ModelNode getHistory(TargetInfo info) throws PatchingException {
                    return Factory.getHistory(mgr, info);
                }

                @Override
                public Iterator iterator() throws PatchingException {
                    try {
                        return iterator(mgr.getIdentity().loadTargetInfo());
                    } catch (IOException e) {
                        throw new PatchingException(PatchManagementMessages.MESSAGES.failedToLoadIdentity(), e);
                    }
                }

                @Override
                public Iterator iterator(TargetInfo info) throws PatchingException {
                    return Factory.iterator(mgr, info);
                }
            };
        }

        private static class IteratorState {
            protected PatchableTarget.TargetInfo currentInfo;
            protected int patchIndex;
            protected Patch.PatchType type = ONE_OFF;

            IteratorState(PatchableTarget.TargetInfo info) {
                this(info, -1);
            }

            IteratorState(PatchableTarget.TargetInfo info, int patchIndex) {
                if(info == null) {
                    throw new IllegalArgumentException("Target info is null");
                }
                this.currentInfo = info;
                this.patchIndex = patchIndex;
            }

            IteratorState(InstalledIdentity mgr) throws PatchingException {
                if(mgr == null) {
                    throw new IllegalArgumentException("Installation manager is null.");
                }
                try {
                    this.currentInfo = mgr.getIdentity().loadTargetInfo();
                } catch (IOException e) {
                    throw new PatchingException(PatchManagementMessages.MESSAGES.failedToLoadIdentity());
                }
                patchIndex = -1;
            }
        }

        private static final class IteratorImpl extends IteratorState implements Iterator {
            private final InstalledIdentity mgr;

            private IteratorImpl(InstalledIdentity mgr) throws PatchingException {
                super(mgr);
                this.mgr = mgr;
            }

            private IteratorImpl(TargetInfo info, InstalledIdentity mgr) {
                super(info);
                this.mgr = mgr;
            }

            @Override
            public boolean hasNext() {
                return hasNext(mgr, this);
            }

            private static boolean hasNext(InstalledIdentity mgr, IteratorState state) {
                // current info hasn't been initialized yet
                if(state.patchIndex < 0) {
                    if(BASE.equals(state.currentInfo.getCumulativePatchID())) {
                        if(state.currentInfo.getPatchIDs().isEmpty()) {
                            return false; // unpatched
                        }
                    }
                    return true;
                }

                // check whether there are still one-offs left in the current info
                // one-offs + 1 means the cumulative patch has been returned as well
                final int size = state.currentInfo.getPatchIDs().size();
                if(state.patchIndex < size) {
                    return existsOnDisk(mgr, state.currentInfo.getPatchIDs().get(state.patchIndex));
                }

                // see whether there is the next CP
                final String releaseID = state.currentInfo.getCumulativePatchID();
                if(BASE.equals(releaseID)) {
                    return false;
                }

                // it's not the base yet and the cumulative has not been returned yet
                if(state.patchIndex == size) {
                    return existsOnDisk(mgr, state.currentInfo.getCumulativePatchID());
                }

                // if it's not BASE then it's a specific patch, so it actually
                // means that there should more to iterate. But we rely on
                // the presence of the patch directory and its rollback.xml.

                File patchHistoryDir = mgr.getInstalledImage().getPatchHistoryDir(releaseID);
                if(patchHistoryDir.exists()) {
                    final File rollbackXml = new File(patchHistoryDir, "rollback.xml");
                    if(rollbackXml.exists()) {
                        try {
                            final PatchBuilder patchBuilder = (PatchBuilder)PatchXml.parse(rollbackXml);
                            final RollbackPatch patch = (RollbackPatch) patchBuilder.build();
                            PatchableTarget.TargetInfo nextInfo = patch.getIdentityState().getIdentity().loadTargetInfo();
                            if(BASE.equals(nextInfo.getCumulativePatchID())) {
                                if(nextInfo.getPatchIDs().isEmpty()) {
                                    return false;
                                }
                            } else if(!existsOnDisk(mgr, nextInfo.getCumulativePatchID())) {
                                return false;
                            }
                        } catch(Exception e) {
                            throw new IllegalStateException(PatchManagementMessages.MESSAGES.failedToLoadIdentity(), e);
                        }
                        return true;
                    }
                }
                return false;
            }

            @Override
            public Entry next() {
                return next(mgr, this);
            }

            private static Entry next(final InstalledIdentity mgr, IteratorState state) {

                String patchId = nextPatchIdForCurrentInfo(state);
                if(patchId == null) { // current info is exhausted, try moving to the next CP
                    if(state.patchIndex < 0) {
                        state.patchIndex = 0;
                    } else {
                        final String releaseID = state.currentInfo.getCumulativePatchID();
                        if(BASE.equals(releaseID)) {
                            throw new NoSuchElementException(PatchManagementMessages.MESSAGES.noMorePatches());
                        }

                        final File patchHistoryDir = mgr.getInstalledImage().getPatchHistoryDir(releaseID);
                        if(patchHistoryDir.exists()) {
                            final File rollbackXml = new File(patchHistoryDir, "rollback.xml");
                            if(rollbackXml.exists()) {
                                try {
                                    final PatchBuilder patchBuilder = (PatchBuilder)PatchXml.parse(rollbackXml);
                                    final RollbackPatch patch = (RollbackPatch) patchBuilder.build();
                                    state.currentInfo = patch.getIdentityState().getIdentity().loadTargetInfo();
                                    state.patchIndex = 0;
                                    state.type = ONE_OFF;
                                } catch(Exception e) {
                                    throw new IllegalStateException(PatchManagementMessages.MESSAGES.failedToLoadIdentity(), e);
                                }
                            } else {
                                throw new NoSuchElementException(PatchManagementMessages.MESSAGES.patchIsMissingFile(rollbackXml.getAbsolutePath()));
                            }
                        } else {
                            throw new NoSuchElementException(PatchManagementMessages.MESSAGES.noPatchHistory(patchHistoryDir.getAbsolutePath()));
                        }
                    }

                    patchId = nextPatchIdForCurrentInfo(state);
                    if(patchId == null) {
                        throw new NoSuchElementException(PatchManagementMessages.MESSAGES.noMorePatches());
                    }
                    assertExistsOnDisk(mgr, patchId);
                }

                final String entryPatchId = patchId;
                final Patch.PatchType entryType = state.type;

                return new Entry() {
                    String appliedAt;
                    Map<String,String> layerPatches;
                    Map<String,String> addOnPatches;

                    @Override
                    public String getPatchId() {
                        return entryPatchId;
                    }

                    @Override
                    public PatchType getType() {
                        return entryType;
                    }

                    @Override
                    public String getAppliedAt() {
                        if(appliedAt == null) {
                            final File patchHistoryDir = mgr.getInstalledImage().getPatchHistoryDir(entryPatchId);
                            if(patchHistoryDir.exists()) {
                                try {
                                    appliedAt = getAppliedAt(patchHistoryDir);
                                } catch (PatchingException e) {
                                }
                            }
                        }
                        return appliedAt;
                    }

                    @Override
                    public Map<String, String> getLayerPatches() {
                        if(layerPatches == null) {
                            loadLayerPatches(false);
                        }
                        return layerPatches;
                    }

                    @Override
                    public Map<String, String> getAddOnPatches() {
                        if(addOnPatches == null) {
                            loadLayerPatches(true);
                        }
                        return addOnPatches;
                    }

                    private void loadLayerPatches(boolean addons) {
                        final File patchDir = mgr.getInstalledImage().getPatchHistoryDir(entryPatchId);
                        if(patchDir.exists()) {
                            final File patchXml = new File(patchDir, "patch.xml");
                            if(patchXml.exists()) {
                                try {
                                    Patch patch = ((PatchBuilder)PatchXml.parse(patchXml)).build();
                                    layerPatches = new HashMap<String, String>();
                                    for(PatchElement e : patch.getElements()) {
                                        if (e.getProvider().isAddOn() == addons) {
                                            layerPatches.put(e.getProvider().getName(), e.getId());
                                        }
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace(); // TODO
                                }
                            } else {
                                layerPatches = Collections.emptyMap();
                            }
                        } else {
                            layerPatches = Collections.emptyMap();
                        }
                    }

                    private String getAppliedAt(File patchDir) throws PatchingException {
                        File timestampFile = new File(patchDir, Constants.TIMESTAMP);
                        try {
                            return timestampFile.exists() ? PatchUtils.readRef(timestampFile) : null;
                        } catch (IOException e) {
                            throw new PatchingException(PatchManagementMessages.MESSAGES.fileIsNotReadable(timestampFile.getAbsolutePath()));
                        }
                    }
                };
            }

            /**
             * @throws UnsupportedOperationException if the {@code remove}
             *         operation is not supported by this iterator.
             */
            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }

            private static boolean existsOnDisk(InstalledIdentity mgr, String id) {
                try {
                    assertExistsOnDisk(mgr, id);
                    return true;
                } catch(NoSuchElementException e) {
                    return false;
                }
            }

            private static void assertExistsOnDisk(InstalledIdentity mgr, String id) throws NoSuchElementException {
                final File historyDir = mgr.getInstalledImage().getPatchHistoryDir(id);
                if(!historyDir.exists()) {
                    throw new NoSuchElementException(PatchManagementMessages.MESSAGES.noPatchHistory(historyDir.getAbsolutePath()));
                }
                // TODO parsed xml can be cached
                final File rollbackXml = new File(historyDir, "rollback.xml");
                if(!rollbackXml.exists()) {
                    throw new NoSuchElementException(PatchManagementMessages.MESSAGES.patchIsMissingFile(rollbackXml.getAbsolutePath()));
                }
                try {
                    PatchXml.parse(rollbackXml);
                } catch (Exception e) {
                    throw new NoSuchElementException(PatchManagementMessages.MESSAGES.fileIsNotReadable(rollbackXml.getAbsolutePath() + ": " + e.getLocalizedMessage()));
                }
                final File patchXml = new File(historyDir, "patch.xml");
                if(!patchXml.exists()) {
                    throw new NoSuchElementException(PatchManagementMessages.MESSAGES.patchIsMissingFile(patchXml.getAbsolutePath()));
                }
                try {
                    PatchXml.parse(patchXml);
                } catch (Exception e) {
                    throw new NoSuchElementException(PatchManagementMessages.MESSAGES.fileIsNotReadable(patchXml.getAbsolutePath() + ": " + e.getLocalizedMessage()));
                }
            }

            /**
             * Returns the next patch id, be it a one-off or the CP
             * <b>for the current info</b>. If the current info has been
             * exhausted, the method returns null.
             */
            private static String nextPatchIdForCurrentInfo(IteratorState state) {
                if(state.patchIndex < 0) {
                    return null;
                }
                final int size = state.currentInfo.getPatchIDs().size();
                if(state.patchIndex < size) {
                    return state.currentInfo.getPatchIDs().get(state.patchIndex++);
                } else if(state.patchIndex == size) {
                    ++state.patchIndex;
                    state.type = CUMULATIVE;
                    final String cp = state.currentInfo.getCumulativePatchID();
                    return BASE.equals(cp) ? null : cp;
                }
                return null;
            }

            @Override
            public boolean hasNextCP() {
                final IteratorState state = new IteratorState(currentInfo, patchIndex);
                return nextCP(mgr, state) != null;
            }

            @Override
            public Entry nextCP() {
                final IteratorState state = new IteratorState(currentInfo, patchIndex);
                final Entry entry = nextCP(mgr, state);
                if(entry == null) {
                    throw new NoSuchElementException(PatchManagementMessages.MESSAGES.noMorePatches());
                }
                currentInfo = state.currentInfo;
                patchIndex = state.patchIndex;
                type = state.type;
                return entry;
            }

            private static Entry nextCP(InstalledIdentity mgr, IteratorState state) {
                while(hasNext(mgr, state)) {
                    final Entry entry = next(mgr, state);
                    if(state.type == Patch.PatchType.CUMULATIVE) {
                        return entry;
                    }
                }
                return null;
            }
        }

        /**
         * Goes back in rollback history adding the patch id and it's application timestamp
         * to the resulting list.
         */
        private static void fillHistoryIn(InstalledIdentity installedImage, PatchableTarget.TargetInfo info, ModelNode result) throws PatchingException {
            final Iterator i = iterator(installedImage, info);
            while(i.hasNext()) {
                final Entry next = i.next();
                fillHistoryIn(result, next.getType(), next.getPatchId(), next.getAppliedAt());
            }
        }

        private static void fillHistoryIn(ModelNode result, PatchType type, String patchID, String appliedAt) throws PatchingException {
            ModelNode history = new ModelNode();
            history.get(Constants.PATCH_ID).set(patchID);
            history.get(Constants.TYPE).set(type.getName());
            final ModelNode appliedAtNode = history.get(Constants.APPLIED_AT);
            if(appliedAt != null) {
                appliedAtNode.set(appliedAt);
            }
            result.add(history);
        }
    }
}

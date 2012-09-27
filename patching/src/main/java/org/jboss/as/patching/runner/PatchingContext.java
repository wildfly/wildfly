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
import org.jboss.as.patching.metadata.ContentItem;
import org.jboss.as.patching.metadata.MiscContentItem;
import org.jboss.as.patching.metadata.MiscContentModification;
import org.jboss.as.patching.metadata.Patch;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Emanuel Muckenhuber
 */
class PatchingContext {

    private final PatchInfo info;
    private final PatchContentLoader loader;
    private final List<MiscContentModification> rollbackActions = new ArrayList<MiscContentModification>();

    private final File root;
    private final File backup;

    private boolean rollbackOnly;

    PatchingContext(final Patch patch, final PatchInfo info, final DirectoryStructure structure, final PatchContentLoader loader) {
        this.info = info;
        this.loader = loader;
        this.root = structure.getInstalledImage().getJbossHome();
        this.backup = structure.getHistoryDir(patch.getPatchId());
    }

    public PatchContentLoader getLoader() {
        return loader;
    }

    public File getTargetFile(final MiscContentItem item) {
        return getTargetFile(root, item);
    }

    public File getBackupFile(final MiscContentItem item) {
        return getTargetFile(backup, item);
    }

    public boolean isIgnored(final ContentItem item) {
        // TODO
        return false;
    }

    public boolean isExcluded(final ContentItem item) {
        // TODO
        return false;
    }

    public void addRollbackAction(final MiscContentModification modification) {
        rollbackActions.add(modification);
    }

    PatchingResult finish(Patch patch) throws PatchingException {
        assert ! rollbackOnly;
        // Create the new info
        final String patchId = patch.getPatchId();
        final PatchInfo newInfo;
        if(Patch.PatchType.ONE_OFF == patch.getPatchType()) {
            final List<String> patches = new ArrayList<String>(info.getPatchIDs());
            patches.add(0, patchId);
            newInfo = new LocalPatchInfo("undefined", info.getCumulativeID(), patches, info.getEnvironment());
        } else {
            newInfo = new LocalPatchInfo("undefined", patchId, Collections.<String>emptyList(), info.getEnvironment());
        }
        try {
            // Persist
            persist(newInfo);
            //
            return new PatchingResult() {
                @Override
                public PatchInfo getPatchInfo() {
                    return newInfo;
                }

                @Override
                public void rollback() {
                    try {
                        persist(info);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            };
        } catch (Exception e) {
            throw new PatchingException(e);
        }
    }

    void rollbackOnly() {
        rollbackOnly = true;
    }

    PatchInfo undo(final Patch patch) {
        // TODO rollback
        return info;
    }

    /**
     * Persist the changes.
     *
     * @param patch the patch
     * @return the new patch info
     */
    PatchInfo persist(PatchInfo patch) throws IOException {
        // TODO persist records...

        final DirectoryStructure environment = info.getEnvironment();
        final String cumulativeID = info.getCumulativeID();
        PatchUtils.writeRef(environment.getCumulativeLink(), info.getCumulativeID());
        PatchUtils.writeRefs(environment.getCumulativeRefs(cumulativeID), info.getPatchIDs());
        return patch;
    }

    static File getTargetFile(final File root, final MiscContentItem item)  {
        File file = root;
        for(final String path : item.getPath()) {
            file = new File(file, path);
        }
        return new File(file, item.getName());
    }

}

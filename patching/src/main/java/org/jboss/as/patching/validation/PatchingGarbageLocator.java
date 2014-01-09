/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

package org.jboss.as.patching.validation;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jboss.as.patching.Constants;
import org.jboss.as.patching.PatchLogger;
import org.jboss.as.patching.PatchingException;
import org.jboss.as.patching.installation.InstalledIdentity;
import org.jboss.as.patching.installation.Layer;
import org.jboss.as.patching.installation.PatchableTarget;

/**
 * Locate unreferenced directories and files and clean them up. This walks through the history checking for the entries
 * which are still able to get rolled back. Everything else is considered as garbage, except the current active state.
 *
 * @author Alexey Loubyansky
 */
public class PatchingGarbageLocator {

    private static final PatchLogger log = PatchLogger.ROOT_LOGGER;
    static final FilenameFilter ALL = new FilenameFilter() {
        @Override
        public boolean accept(File dir, String name) {
            return true;
        }
    };

    /**
     * Get the garbage locator.
     *
     * @param installedIdentity the installed identity
     * @return the garbage locator
     */
    public static PatchingGarbageLocator getIninitialized(InstalledIdentity installedIdentity) {
        return new PatchingGarbageLocator(installedIdentity);
    }

    private final InstalledIdentity installedIdentity;

    private Set<String> validHistory;
    private Set<File> referencedOverlayDirectories;

    protected PatchingGarbageLocator(final InstalledIdentity installedIdentity) {
        this.installedIdentity = installedIdentity;
    }

    private void walk() throws PatchingException {
        validHistory = new HashSet<String>();
        referencedOverlayDirectories = new HashSet<File>();
        // Get the active history
        final Set<String> activeHistory = new HashSet<String>();
        try {
            final PatchableTarget.TargetInfo info = installedIdentity.getIdentity().loadTargetInfo();
            activeHistory.addAll(info.getPatchIDs());
            activeHistory.add(info.getCumulativePatchID());
        } catch(IOException e) {
            throw new RuntimeException(e);
        }
        final PatchHistoryIterator.Builder builder = PatchHistoryIterator.Builder.create(installedIdentity);
        builder.addStateHandler(PatchingArtifacts.MODULE_OVERLAY, new PatchingArtifactStateHandler<PatchingFileArtifact.DirectoryArtifactState>() {
            @Override
            public void handleValidatedState(PatchingFileArtifact.DirectoryArtifactState state) {
                referencedOverlayDirectories.add(state.getFile());
            }
        });
        builder.addStateHandler(PatchingArtifacts.BUNDLE_OVERLAY, new PatchingArtifactStateHandler<PatchingFileArtifact.DirectoryArtifactState>() {
            @Override
            public void handleValidatedState(PatchingFileArtifact.DirectoryArtifactState state) {
                referencedOverlayDirectories.add(state.getFile());
            }
        });
        final PatchHistoryValidations.HistoryProcessor processor = new PatchHistoryValidations.HistoryProcessor() {
            boolean failed = false;

            @Override
            protected boolean includeCurrent() {
                return true;
            }

            @Override
            protected boolean canProceed() {
                return !activeHistory.isEmpty() || !failed;
            }

            @Override
            protected <P extends PatchingArtifact.ArtifactState, S extends PatchingArtifact.ArtifactState> boolean handleError(PatchingArtifact<P, S> artifact, S state) {
                // If (specific) parts of the history are is missing we can rollback to this patch, but no further
                failed = true;
                if (artifact == PatchingArtifacts.PATCH_XML
                        || artifact == PatchingArtifacts.ROLLBACK_XML
                        || artifact == PatchingArtifacts.MISC_BACKUP) {
                    return true;
                } else {
                    return false;
                }
            }

            @Override
            protected void processedPatch(String patch) {
                activeHistory.remove(patch);
                validHistory.add(patch);
            }
        };
        // Process
        processor.process(builder.iterator());
    }

    public void reset() {
        validHistory = null;
        referencedOverlayDirectories = null;
    }

    /**
     * Get the inactive history directories.
     *
     * @return the inactive history
     */
    public List<File> getInactiveHistory() throws PatchingException {
        if (validHistory == null) {
            walk();
        }
        final File[] inactiveDirs = installedIdentity.getInstalledImage().getPatchesDir().listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.isDirectory() && !validHistory.contains(pathname.getName());
            }
        });
        return inactiveDirs == null ? Collections.<File>emptyList() : Arrays.asList(inactiveDirs);
    }

    /**
     * Get the inactive overlay directories.
     *
     * @return the inactive overlay directories
     */
    public List<File> getInactiveOverlays() throws PatchingException {
        if (referencedOverlayDirectories == null) {
            walk();
        }
        List<File> inactiveDirs = null;
        for (Layer layer : installedIdentity.getLayers()) {
            final File overlaysDir = new File(layer.getDirectoryStructure().getModuleRoot(), Constants.OVERLAYS);
            final File[] inactiveLayerDirs = overlaysDir.listFiles(new FileFilter() {
                @Override
                public boolean accept(File pathname) {
                    return pathname.isDirectory() && !referencedOverlayDirectories.contains(pathname);
                }
            });
            if (inactiveLayerDirs != null && inactiveLayerDirs.length > 0) {
                if (inactiveDirs == null) {
                    inactiveDirs = new ArrayList<File>();
                }
                inactiveDirs.addAll(Arrays.asList(inactiveLayerDirs));
            }
        }
        return inactiveDirs == null ? Collections.<File>emptyList() : inactiveDirs;
    }

    /**
     * Delete inactive contents.
     */
    public void deleteInactiveContent() throws PatchingException {
        List<File> dirs = getInactiveHistory();
        if (!dirs.isEmpty()) {
            for (File dir : dirs) {
                deleteDir(dir, ALL);
            }
        }
        dirs = getInactiveOverlays();
        if (!dirs.isEmpty()) {
            for (File dir : dirs) {
                deleteDir(dir, ALL);
            }
        }
    }

    protected static boolean deleteDir(File dir, FilenameFilter filter) {
        boolean success = true;
        final File[] files = dir.listFiles(filter);
        if (files != null) {
            for (int i = 0; i < files.length; i++) {
                File f = files[i];
                if (f.isDirectory()) {
                    // delete the directory and all of its contents.
                    if (!deleteDir(f, filter)) {
                        success = false;
                        log.debugf("Failed to delete dir: %s", f.getAbsolutePath());
                    }
                }
                // delete each file in the directory
                else if (!f.delete()) {
                    success = false;
                    log.debugf("Failed to delete file: %s", f.getAbsolutePath());
                }
            }
        }

        // finally delete the directory
        if (!dir.delete()) {
            success = false;
            log.debugf("Failed to delete dir: %s", dir.getAbsolutePath());
        }
        return success;
    }

}

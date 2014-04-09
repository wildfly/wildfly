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

package org.jboss.as.patching.validation;


import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jboss.as.patching.Constants;
import org.jboss.as.patching.PatchLogger;
import org.jboss.as.patching.installation.InstallationManager;
import org.jboss.as.patching.installation.Layer;


/**
 * @author Alexey Loubyansky
 *
 */
public class PatchingGarbageLocator implements PatchStateHandler {

    private static final PatchLogger log = PatchLogger.ROOT_LOGGER;
    private static final String OVERLAYS = Constants.OVERLAYS;

    static final FilenameFilter ALL = new FilenameFilter() {
        @Override
        public boolean accept(File dir, String name) {
            return true;
        }
    };

    public static PatchingGarbageLocator getIninitialized(InstallationManager im) {
        final PatchingGarbageLocator garbageLocator = new PatchingGarbageLocator(im);
        final Context ctx = getContext(im, false);
        PatchingHistory.State history = PatchingHistory.getInstance().getState(ctx);
        history.handlePatches(ctx, garbageLocator);
        return garbageLocator;
    }

    private final InstallationManager manager;

    private Set<String> activeHistory = new HashSet<String>();
    private Set<File> activeOverlays = new HashSet<File>();

    public PatchingGarbageLocator(InstallationManager manager) {
        if(manager == null) {
            throw new IllegalArgumentException("Installation manager is null");
        }
        this.manager = manager;
    }

    @Override
    public void handle(PatchArtifact.State patch) {
        activeHistory.add(patch.getHistoryDir().getDirectory().getName());
        PatchElementArtifact.State patchElements = patch.getHistoryDir().getPatchXml().getPatchElements();
        patchElements.resetIndex();
        while(patchElements.hasNext()) {
            final File bundlesDir = patchElements.getState().getLayer().getBundlesDir();
            if(bundlesDir != null) {
                activeOverlays.add(bundlesDir);
            }
            final File modulesDir = patchElements.getState().getLayer().getModulesDir();
            if(modulesDir != null) {
                activeOverlays.add(modulesDir);
            }
            patchElements.next();
        }
    }

    public List<File> getInactiveHistory() {
        final File[] inactiveDirs = manager.getInstalledImage().getPatchesDir().listFiles(new FileFilter(){
            @Override
            public boolean accept(File pathname) {
                return pathname.isDirectory() && !activeHistory.contains(pathname.getName());
            }});
        return inactiveDirs == null ? Collections.<File>emptyList() : Arrays.asList(inactiveDirs);
    }

    public List<File> getInactiveOverlays() {
        List<File> inactiveDirs = null;
        for(Layer layer : manager.getLayers()) {
            final File overlaysDir = new File(layer.getDirectoryStructure().getModuleRoot(), OVERLAYS);
            final File[] inactiveLayerDirs = overlaysDir.listFiles(new FileFilter(){
                @Override
                public boolean accept(File pathname) {
                    return pathname.isDirectory() && !activeOverlays.contains(pathname);
                }});
            if(inactiveLayerDirs != null && inactiveLayerDirs.length > 0) {
                if(inactiveDirs == null) {
                    inactiveDirs = new ArrayList<File>();
                }
                inactiveDirs.addAll(Arrays.asList(inactiveLayerDirs));
            }
        }
        return inactiveDirs == null ? Collections.<File>emptyList() : inactiveDirs;
    }

    public void deleteInactiveContent() {
        List<File> dirs = getInactiveHistory();
        if(!dirs.isEmpty()) {
            for(File dir : dirs) {
                deleteDir(dir, ALL);
            }
        }
        dirs = getInactiveOverlays();
        if(!dirs.isEmpty()) {
            for(File dir : dirs) {
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
                        if(log.isDebugEnabled()) {
                            log.debug("Failed to delete dir: " + f.getAbsolutePath());
                        }
                    }
                }
                // delete each file in the directory
                else if (!f.delete()) {
                    success = false;
                    if(log.isDebugEnabled()) {
                        log.debug("Failed to delete file: " + f.getAbsolutePath());
                    }
                }
            }
        }

        // finally delete the directory
        if (!dir.delete()) {
            success = false;
            if(log.isDebugEnabled()) {
                log.debug("Failed to delete dir: " + dir.getAbsolutePath());
            }
        }
        return success;
    }

    private static Context getContext(final InstallationManager manager, final boolean failOnError) {
        return new Context() {

            @Override
            public InstallationManager getInstallationManager() {
                return manager;
            }

            @Override
            public ErrorHandler getErrorHandler() {
                return new ErrorHandler(){
                    @Override
                    public void error(String msg) {
                        // Don't fail, artifacts in error can be removed
                        PatchLogger.ROOT_LOGGER.debugf(msg);
                    }

                    @Override
                    public void error(String msg, Throwable t) {
                        // Don't fail, artifacts in error can be removed
                        PatchLogger.ROOT_LOGGER.debugf(t, msg);
                    }};
            }};
    }
}

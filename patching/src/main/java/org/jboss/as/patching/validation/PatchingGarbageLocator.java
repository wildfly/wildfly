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
import org.jboss.as.patching.validation.PatchingHistoryTree.Builder;

/**
 * @author Alexey Loubyansky
 *
 */
public class PatchingGarbageLocator {

    private static final PatchLogger log = PatchLogger.ROOT_LOGGER;
    private static final String OVERLAYS = Constants.OVERLAYS;

    static final FilenameFilter ALL = new FilenameFilter() {
        @Override
        public boolean accept(File dir, String name) {
            return true;
        }
    };

    private final InstallationManager manager;

    private Set<String> activeHistory;
    private Set<File> activeOverlays;

    final PatchingHistoryTree activeDirsHandler = Builder.getInstance()
            .addHandler(PatchHistoryDir.getInstance(), new ArtifactStateHandler<PatchHistoryDir.State>(){
                @Override
                public void handle(Context ctx, PatchHistoryDir.State state) {
                    activeHistory.add(state.getDirectory().getName());
                }})
            .addHandler(PatchElementProviderArtifact.getInstance(), new ArtifactStateHandler<PatchElementProviderArtifact.State>() {
                @Override
                public void handle(Context ctx, PatchElementProviderArtifact.State state) {
                    if(state.getModulesDir() != null) {
                        activeOverlays.add(state.getModulesDir());
                    }
                    if(state.getBundlesDir() != null) {
                        activeOverlays.add(state.getBundlesDir());
                    }
                }})
            .build();

    public PatchingGarbageLocator(InstallationManager manager) {
        if(manager == null) {
            throw new IllegalArgumentException("Installation manager is null");
        }
        this.manager = manager;
    }

    private void walk() {
        activeHistory = new HashSet<String>();
        activeOverlays = new HashSet<File>();
        activeDirsHandler.handleAll(getContext(manager, false));
    }

    public void reset() {
        activeHistory = null;
        activeOverlays = null;
    }

    public List<File> getInactiveHistory() {
        if(activeHistory == null) {
            walk();
        }
        final File[] inactiveDirs = manager.getInstalledImage().getPatchesDir().listFiles(new FileFilter(){
            @Override
            public boolean accept(File pathname) {
                return pathname.isDirectory() && !activeHistory.contains(pathname.getName());
            }});
        return inactiveDirs == null ? Collections.<File>emptyList() : Arrays.asList(inactiveDirs);
    }

    public List<File> getInactiveOverlays() {
        if(activeOverlays == null) {
            walk();
        }
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
                        if(failOnError) {
                            throw new IllegalStateException(msg);
                        }
                    }

                    @Override
                    public void error(String msg, Throwable t) {
                        if(failOnError) {
                            throw new IllegalStateException(msg, t);
                        }
                    }};
            }};
    }
}

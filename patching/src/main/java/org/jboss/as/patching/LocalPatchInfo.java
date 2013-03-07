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

package org.jboss.as.patching;

import org.jboss.as.patching.runner.PatchUtils;
import org.jboss.as.version.ProductConfig;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * @author Emanuel Muckenhuber
 */
public class LocalPatchInfo implements PatchInfo {

    private static final String MODULE_PATH = "module.path";
    private static final String BUNDLES_DIR = "jboss.bundles.dir";

    private final String version;
    private final String cumulativeId;
    private final List<String> patches;
    private final DirectoryStructure environment;

    public LocalPatchInfo(final String version, final String cumulativeId, final List<String> patches, final DirectoryStructure environment) {
        this.version = version;
        this.cumulativeId = cumulativeId;
        this.patches = patches;
        this.environment = environment;
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public String getCumulativeID() {
        return cumulativeId;
    }

    @Override
    public List<String> getPatchIDs() {
        return patches;
    }

    /**
     * Load the local patch information.
     *
     * @param config the product config
     * @param environment the directory structure
     * @return the local patch info
     * @throws IOException
     */
    public static LocalPatchInfo load(final ProductConfig config, final DirectoryStructure environment) throws IOException {
        return load(config.resolveVersion(), environment);
    }

    /**
     * Load the information from the disk.
     *
     * @param version the current version
     * @param environment the patch environment
     * @return the patches
     * @throws IOException
     */
    static LocalPatchInfo load(final String version, final DirectoryStructure environment) throws IOException {
        final String ref = PatchUtils.readRef(environment.getCumulativeLink());
        final List<String> patches = PatchUtils.readRefs(environment.getCumulativeRefs(ref));
        return new LocalPatchInfo(version, ref, patches, environment);
    }

    /**
     * Load the patch history for a given patch info.
     *
     * @param current the current patch info
     * @param structure the directory structure
     * @return the history
     * @throws IOException
     */
    static PatchInfo loadHistory(final PatchInfo current, final DirectoryStructure structure) throws IOException {
        final String currentId = current.getCumulativeID();
        final File history = structure.getHistoryDir(currentId);
        final File cumulative = new File(history, DirectoryStructure.CUMULATIVE);
        if(cumulative.exists()) {
            // Return the immediate persisted history
            // this does not necessarily mean it is consistent with the .metadata/references/cumulative-id
            final String ref = PatchUtils.readRef(cumulative);
            final File refsFile = new File(history, DirectoryStructure.REFERENCES);
            final List<String> refs = PatchUtils.readRefs(refsFile);
            // TODO perhaps we can use the backed up patch.xml to get the version?
            return new LocalPatchInfo("unknown", ref, refs, structure);
        }
        return null;
    }

    @Override
    public File[] getPatchingPath() {
        final List<File> path = getPatchingPathInternal();
        return path.toArray(new File[path.size()]);
    }

    @Override
    public File[] getModulePath() {
        final List<File> path = getPatchingPathInternal();
        final String modulePath = System.getProperty(MODULE_PATH, System.getenv("JAVA_MODULEPATH"));
        if(modulePath != null) {
            final String[] paths = modulePath.split(Pattern.quote(File.pathSeparator));
            for(final String s : paths) {
                final File file = new File(s);
                path.add(file);
            }
        }
        return path.toArray(new File[path.size()]);
    }

    List<File> getPatchingPathInternal() {
        final List<File> path = new ArrayList<File>();
        for(final String patch : patches) {
            path.add(environment.getModulePatchDirectory(patch));
        }
        if(!BASE.equals(cumulativeId)) {
            path.add(environment.getModulePatchDirectory(cumulativeId));
        }
        return path;
    }

    @Override
    public File[] getBundlePath() {
        final List<File> path = new ArrayList<File>();
        for(final String patch : patches) {
            path.add(environment.getBundlesPatchDirectory(patch));
        }
        if(!BASE.equals(cumulativeId)) {
            path.add(environment.getBundlesPatchDirectory(cumulativeId));
        }
        final String bundleDir = System.getProperty(BUNDLES_DIR);
        if(bundleDir == null) {
            path.add(environment.getInstalledImage().getBundlesDir());
        } else {
            path.add(new File(bundleDir));
        }
        return path.toArray(new File[path.size()]);
    }

}

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

import static org.jboss.as.patching.Constants.BASE;
import static org.jboss.as.patching.Constants.BUNDLES;
import static org.jboss.as.patching.Constants.LAYERS;
import static org.jboss.as.patching.Constants.MODULES;
import static org.jboss.as.patching.IoUtils.mkdir;
import static org.jboss.as.patching.runner.TestUtils.randomString;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jboss.as.patching.DirectoryStructure;
import org.jboss.as.patching.IoUtils;
import org.jboss.as.patching.PatchingException;
import org.jboss.as.patching.installation.InstallationManager;
import org.jboss.as.patching.installation.InstallationManagerImpl;
import org.jboss.as.patching.installation.InstalledIdentity;
import org.jboss.as.patching.tool.ContentVerificationPolicy;
import org.jboss.as.patching.tool.PatchTool;
import org.jboss.as.patching.tool.PatchingResult;
import org.jboss.as.version.ProductConfig;
import org.junit.After;
import org.junit.Before;


/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2012, Red Hat Inc
 */
public abstract class AbstractTaskTestCase {

    protected File tempDir;
    protected DirectoryStructure env;
    protected ProductConfig productConfig;

    @Before
    public void setup() throws Exception {
        tempDir = mkdir(new File(System.getProperty("java.io.tmpdir")), "patching-" + randomString());
        File jbossHome = mkdir(tempDir, "jboss-installation");
        mkdir(jbossHome, MODULES, "system", LAYERS, BASE);
        mkdir(jbossHome, BUNDLES, "system", LAYERS, BASE);
        env = TestUtils.createLegacyTestStructure(jbossHome);
        productConfig = new ProductConfig("product", "version", "consoleSlot");
    }

    @After
    public void tearDown() {
        if (!IoUtils.recursiveDelete(tempDir)) {
            tempDir.deleteOnExit();
        }
    }

    public InstalledIdentity loadInstalledIdentity() throws IOException {
        List<File> moduleRoots = new ArrayList<File>();
        moduleRoots.add(env.getInstalledImage().getModulesDir());
        List<File> bundleRoots = new ArrayList<File>();
        bundleRoots.add(env.getInstalledImage().getBundlesDir());
        InstalledIdentity installedIdentity = InstalledIdentity.load(env.getInstalledImage(), productConfig, moduleRoots, bundleRoots);
        return installedIdentity;
    }

    protected PatchTool newPatchTool() throws IOException {
        final InstalledIdentity installedIdentity = loadInstalledIdentity();
        final InstallationManager manager = new InstallationManagerImpl(installedIdentity);
        return PatchTool.Factory.create(manager);
    }

    protected PatchingResult executePatch(final File file) throws IOException, PatchingException {
        final PatchTool tool = newPatchTool();
        final PatchingResult result = tool.applyPatch(file, ContentVerificationPolicy.STRICT);
        result.commit();
        return result;
    }

    protected PatchingResult rollback(String patchId) throws IOException, PatchingException {
        return rollback(patchId, false);
    }

    protected PatchingResult rollback(String patchId, final boolean rollbackTo) throws IOException, PatchingException {
        return rollback(patchId, rollbackTo, ContentVerificationPolicy.STRICT);
    }

    protected PatchingResult rollback(String patchId, boolean rollbackTo, ContentVerificationPolicy policy) throws IOException, PatchingException {
        final PatchTool tool = newPatchTool();
        final PatchingResult result = tool.rollback(patchId, policy, rollbackTo, true);
        result.commit();
        return result;
    }
}

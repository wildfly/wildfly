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

package org.jboss.as.patching.cli;

import static org.jboss.as.patching.Constants.BASE;
import static org.jboss.as.patching.Constants.LAYERS;
import static org.jboss.as.patching.Constants.SYSTEM;
import static org.jboss.as.patching.IoUtils.mkdir;
import static org.jboss.as.patching.IoUtils.newFile;
import static org.jboss.as.patching.runner.TestUtils.createBundle0;
import static org.jboss.as.patching.runner.TestUtils.createInstalledImage;
import static org.jboss.as.patching.runner.TestUtils.createModule0;
import static org.jboss.as.patching.runner.TestUtils.createPatchXMLFile;
import static org.jboss.as.patching.runner.TestUtils.createZippedPatchFile;
import static org.jboss.as.patching.runner.TestUtils.dump;
import static org.jboss.as.patching.runner.TestUtils.randomString;
import static org.jboss.as.patching.runner.TestUtils.touch;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;

import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandContextFactory;
import org.jboss.as.cli.CommandLineException;
import org.jboss.as.patching.metadata.ContentModification;
import org.jboss.as.patching.metadata.Patch;
import org.jboss.as.patching.metadata.PatchBuilder;
import org.jboss.as.patching.runner.AbstractTaskTestCase;
import org.jboss.as.patching.runner.ContentModificationUtils;
import org.junit.Test;


/**
 * @author Alexey Loubyansky
 *
 */
public class ContentConflictsUnitTestCase extends AbstractTaskTestCase {

    /**
     * Tests content conflicts reporting in the CLI during patch application.
     *
     * The test creates 2 misc files, 2 modules and 2 bundles and a patch
     * which updates all of them. Before the patch is applied, one file,
     * one module and one bundle are modified on the disk. The patch is
     * expected to fail and the failure description should contain the
     * info about the conflicting content items.
     *
     * @throws Exception
     */
    @Test
    public void testApply() throws Exception {

        final File binDir = createInstalledImage(env, "consoleSlot", productConfig.getProductName(), productConfig.getProductVersion());

        // build a one-off patch for the base installation
        // with 1 updated file
        String patchID = randomString();
        File patchDir = mkdir(tempDir, patchID);

        // create a module for the conflict
        File baseModuleDir = newFile(env.getInstalledImage().getModulesDir(), SYSTEM, LAYERS, BASE);
        String moduleConflictName = "module-conflict";
        File moduleConflictDir = createModule0(baseModuleDir, moduleConflictName);
        // create the patch with the updated module
        ContentModification moduleConflictModified = ContentModificationUtils.modifyModule(patchDir, patchID, moduleConflictDir, "new resource in the module");

        // create a module to be updated w/o a conflict
        String moduleNoConflictName = "module-no-conflict";
        File moduleNoConflictDir = createModule0(baseModuleDir, moduleNoConflictName);
        // create the patch with the updated module
        ContentModification moduleNoConflictModified = ContentModificationUtils.modifyModule(patchDir, patchID, moduleNoConflictDir, "new resource in the module");

        // create a file for the conflict
        String fileConflictName = "file-conflict.txt";
        File conflictFile = touch(binDir, fileConflictName);
        dump(conflictFile, "original script to run standalone AS7");
        // patch the file
        ContentModification fileConflictModified = ContentModificationUtils.modifyMisc(patchDir, patchID, "updated script", conflictFile, "bin", fileConflictName);

        // create a file for the conflict
        String fileNoConflictName = "file-no-conflict.txt";
        File noConflictFile = touch(binDir, fileNoConflictName);
        dump(noConflictFile, "original script to run standalone AS7");
        // patch the file
        ContentModification fileNoConflictModified = ContentModificationUtils.modifyMisc(patchDir, patchID, "updated script", noConflictFile, "bin", fileNoConflictName);

        // create a bundle for the conflict
        File baseBundleDir = newFile(env.getInstalledImage().getBundlesDir(), SYSTEM, LAYERS, BASE);
        String bundleConflictName = "bundle-conflict";
        File bundleConflictDir = createBundle0(baseBundleDir, bundleConflictName, "bundle content");
        // patch the bundle
        ContentModification bundleConflictModified = ContentModificationUtils.modifyBundle(patchDir, patchID, bundleConflictDir, "updated bundle content");

        // create a bundle to be updated w/o a conflict
        String bundleNoConflictName = "bundle-no-conflict";
        File bundleNoConflictDir = createBundle0(baseBundleDir, bundleNoConflictName, "bundle content");
        // patch the bundle
        ContentModification bundleNoConflictModified = ContentModificationUtils.modifyBundle(patchDir, patchID, bundleNoConflictDir, "updated bundle content");

        //TestUtils.tree(env.getInstalledImage().getJbossHome());

        Patch patch = PatchBuilder.create()
                .setPatchId(patchID)
                .setDescription(randomString())
                .oneOffPatchIdentity(productConfig.getProductName(), productConfig.getProductVersion())
                .getParent()
                .addContentModification(fileConflictModified)
                .addContentModification(fileNoConflictModified)
                .oneOffPatchElement(patchID, "base", false)
                .addContentModification(moduleConflictModified)
                .addContentModification(moduleNoConflictModified)
                .addContentModification(bundleConflictModified)
                .addContentModification(bundleNoConflictModified)
                .getParent()
                .build();

        // create the patch
        createPatchXMLFile(patchDir, patch, false);

        File zippedPatch = createZippedPatchFile(patchDir, patch.getPatchId());

        // create a conflict for the file
        dump(conflictFile, "conflicting change");
        // create a conflict for the module
        createModule0(baseModuleDir, moduleConflictName, "oops");
        // create a conflict for bundle
        createBundle0(baseBundleDir, bundleConflictName, "oops");

        // apply the patch using the cli
        CommandContext ctx = CommandContextFactory.getInstance().newCommandContext();
        try {
            ctx.handle("patch apply " + zippedPatch.getAbsolutePath() + " --distribution=" + env.getInstalledImage().getJbossHome());
            fail("Conflicts expected.");
        } catch(CommandLineException e) {
            //e.printStackTrace();
            final int relativeIndex = env.getInstalledImage().getJbossHome().getAbsolutePath().length() + 1;
            assertConflicts(e, bundleConflictName + ":main", moduleConflictName + ":main", conflictFile.getAbsolutePath().substring(relativeIndex));
        } finally {
            ctx.terminateSession();
        }
    }

    /**
     * Tests content conflicts reporting in the CLI during patch rollback.
     *
     * The test creates 2 misc files, 2 modules and 2 bundles and a patch
     * which updates all of them. The patch is applied. Then one file,
     * one module and one bundle are modified on the disk. The patch is
     * rolled back then which is expected to fail and the failure description
     * should contain the info about the conflicting content items.
     *
     * @throws Exception
     */
    @Test
    public void testRollback() throws Exception {

        final File binDir = createInstalledImage(env, "consoleSlot", productConfig.getProductName(), productConfig.getProductVersion());

        // build a one-off patch for the base installation
        // with 1 updated file
        String patchID = randomString();
        String patchElementId = randomString();
        File patchDir = mkdir(tempDir, patchID);

        // create a module for the conflict
        File baseModuleDir = newFile(env.getInstalledImage().getModulesDir(), SYSTEM, LAYERS, BASE);
        String moduleConflictName = "module-conflict";
        File moduleConflictDir = createModule0(baseModuleDir, moduleConflictName);
        // create the patch with the updated module
        ContentModification moduleConflictModified = ContentModificationUtils.modifyModule(patchDir, patchElementId, moduleConflictDir, "new resource in the module");

        // create a module to be updated w/o a conflict
        String moduleNoConflictName = "module-no-conflict";
        File moduleNoConflictDir = createModule0(baseModuleDir, moduleNoConflictName);
        // create the patch with the updated module
        ContentModification moduleNoConflictModified = ContentModificationUtils.modifyModule(patchDir, patchElementId, moduleNoConflictDir, "new resource in the module");

        // create a file for the conflict
        String fileConflictName = "file-conflict.txt";
        File conflictFile = touch(binDir, fileConflictName);
        dump(conflictFile, "original script to run standalone AS7");
        // patch the file
        ContentModification fileConflictModified = ContentModificationUtils.modifyMisc(patchDir, patchID, "updated script", conflictFile, "bin", fileConflictName);

        // create a file for the conflict
        String fileNoConflictName = "file-no-conflict.txt";
        File noConflictFile = touch(binDir, fileNoConflictName);
        dump(noConflictFile, "original script to run standalone AS7");
        // patch the file
        ContentModification fileNoConflictModified = ContentModificationUtils.modifyMisc(patchDir, patchID, "updated script", noConflictFile, "bin", fileNoConflictName);

        // create a bundle for the conflict
        File baseBundleDir = newFile(env.getInstalledImage().getBundlesDir(), SYSTEM, LAYERS, BASE);
        String bundleConflictName = "bundle-conflict";
        File bundleConflictDir = createBundle0(baseBundleDir, bundleConflictName, "bundle content");
        // patch the bundle
        ContentModification bundleConflictModified = ContentModificationUtils.modifyBundle(patchDir, patchElementId, bundleConflictDir, "updated bundle content");

        // create a bundle to be updated w/o a conflict
        String bundleNoConflictName = "bundle-no-conflict";
        File bundleNoConflictDir = createBundle0(baseBundleDir, bundleNoConflictName, "bundle content");
        // patch the bundle
        ContentModification bundleNoConflictModified = ContentModificationUtils.modifyBundle(patchDir, patchElementId, bundleNoConflictDir, "updated bundle content");

        //TestUtils.tree(env.getInstalledImage().getJbossHome());

        Patch patch = PatchBuilder.create()
                .setPatchId(patchID)
                .setDescription(randomString())
                .oneOffPatchIdentity(productConfig.getProductName(), productConfig.getProductVersion())
                .getParent()
                .addContentModification(fileConflictModified)
                .addContentModification(fileNoConflictModified)
                .oneOffPatchElement(patchElementId, "base", false)
                .addContentModification(moduleConflictModified)
                .addContentModification(moduleNoConflictModified)
                .addContentModification(bundleConflictModified)
                .addContentModification(bundleNoConflictModified)
                .getParent()
                .build();

        // create the patch
        createPatchXMLFile(patchDir, patch, false);

        File zippedPatch = createZippedPatchFile(patchDir, patch.getPatchId());

        // apply the patch using the cli
        CommandContext ctx = CommandContextFactory.getInstance().newCommandContext();
        try {
            ctx.handle("patch apply " + zippedPatch.getAbsolutePath() + " --distribution=" + env.getInstalledImage().getJbossHome());
        } catch(CommandLineException e) {
            ctx.terminateSession();
            fail("Failed to apply the patch: " + e);
        }

        // create a conflict for the file
        dump(conflictFile, "conflicting change");
        // create a conflict for the module
        createModule0(baseModuleDir, moduleConflictName, "oops");
        // create a conflict for bundle
        createBundle0(baseBundleDir, bundleConflictName, "oops");

        try {
            ctx.handle("patch rollback --patch-id=" + patchID + " --distribution=" + env.getInstalledImage().getJbossHome() + " --reset-configuration=false");
            fail("Conflicts expected");
        } catch(CommandLineException e) {
            final int relativeIndex = env.getInstalledImage().getJbossHome().getAbsolutePath().length() + 1;
            // TODO modules and bundles are not checked at the moment
            assertConflicts(e, bundleConflictName + ":main", moduleConflictName + ":main", conflictFile.getAbsolutePath().substring(relativeIndex));
            //assertConflicts(e, conflictFile.getAbsolutePath().substring(relativeIndex));
        } finally {
            ctx.terminateSession();
        }

    }

    protected void assertConflicts(CommandLineException e, String... conflicts) {
        final StringBuilder buf = new StringBuilder();
        buf.append("Conflicts detected: ");
        int i = 0;
        while(i < conflicts.length) {
            buf.append(conflicts[i++].replace('\\', '/')); // fix paths on windows
            if(i < conflicts.length) {
                buf.append(", ");
            }
        }
        assertEquals(e.getMessage().split(System.getProperty("line.separator"))[0], buf.toString());
    }
}

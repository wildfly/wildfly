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

import static org.jboss.as.patching.IoUtils.mkdir;
import static org.jboss.as.patching.runner.TestUtils.randomString;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import org.jboss.as.patching.DirectoryStructure;
import org.jboss.as.patching.IoUtils;
import org.jboss.as.patching.PatchInfo;
import org.jboss.as.patching.installation.InstalledIdentity;
import org.jboss.as.patching.installation.InstalledImage;
import org.junit.After;
import org.junit.Before;


/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2012, Red Hat Inc
 */
public abstract class AbstractTaskTestCase {

    protected File tempDir;
    protected DirectoryStructure env;
    private String storedModulesPath;

    @Before
    public void setup() throws Exception {
        tempDir = mkdir(new File(System.getProperty("java.io.tmpdir")), randomString());
        File jbossHome = mkdir(tempDir, "jboss-installation");
        env = DirectoryStructure.createLegacy(jbossHome);
        // make sur we put the installation modules dir in the module.path
        // FIXME is there a way set the module path without changing this sys prop?
        storedModulesPath = System.getProperty("module.path");
        System.setProperty("module.path", env.getInstalledImage().getModulesDir().getAbsolutePath());
    }

    @After
    public void tearDown() {
        IoUtils.recursiveDelete(tempDir);
        // reset the module.path sys prop
        if (storedModulesPath != null) {
            System.setProperty("module.path", storedModulesPath);
        }
    }

    protected PatchingResult executePatch(final File file, InstalledIdentity installedIdentity, InstalledImage installedImage) throws FileNotFoundException, PatchingException {
        LegacyPatchRunner runner = new LegacyPatchRunner(installedImage, installedIdentity);
        final PatchingResult result = runner.executeDirect(new FileInputStream(file), ContentVerificationPolicy.STRICT);
        result.commit();
        return result;
    }

    @Deprecated
    protected PatchingResult executePatch(final PatchInfo info, final File file) throws FileNotFoundException, PatchingException {
        final PatchingRunnerWrapper runner = PatchingRunnerWrapper.Factory.create(info, env);
        final PatchingResult result = runner.executeDirect(new FileInputStream(file), ContentVerificationPolicy.STRICT);
        result.commit();
        return result;
    }

    protected PatchingResult rollback( String patchId, InstalledIdentity installedIdentity, InstalledImage installedImage) throws FileNotFoundException, PatchingException {
        LegacyPatchRunner runner = new LegacyPatchRunner(installedImage, installedIdentity);
        final PatchingResult result = runner.rollback(patchId, ContentVerificationPolicy.STRICT, false, true);
        result.commit();
        return result;
    }

    protected PatchingResult rollback(final PatchInfo info, final String patchId) throws PatchingException {
        return rollback(info, patchId, false);
    }

    protected PatchingResult rollback(final PatchInfo info, final String patchId, final boolean rollbackTo) throws PatchingException {
        final PatchingRunnerWrapper runner = PatchingRunnerWrapper.Factory.create(info, env);
        final PatchingResult result = runner.rollback(patchId, ContentVerificationPolicy.STRICT, rollbackTo, true);
        result.commit();
        return result;
    }

}

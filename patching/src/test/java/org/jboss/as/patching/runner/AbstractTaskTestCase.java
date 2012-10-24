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

import org.jboss.as.patching.PatchInfo;
import static org.jboss.as.patching.runner.PatchUtils.recursiveDelete;
import static org.jboss.as.patching.runner.TestUtils.mkdir;
import static org.jboss.as.patching.runner.TestUtils.randomString;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import org.jboss.as.boot.DirectoryStructure;
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
        env = DirectoryStructure.createDefault(jbossHome);
        // make sur we put the installation modules dir in the module.path
        // FIXME is there a way set the module path without changing this sys prop?
        storedModulesPath = System.getProperty("module.path");
        System.setProperty("module.path", env.getInstalledImage().getModulesDir().getAbsolutePath());
    }

    @After
    public void tearDown() {
        recursiveDelete(tempDir);
        // reset the module.path sys prop
        if (storedModulesPath != null) {
            System.setProperty("module.path", storedModulesPath);
        }
    }

    PatchingResult executePatch(final PatchInfo info, final File file) throws FileNotFoundException, PatchingException {
        final PatchingTaskRunner runner = new PatchingTaskRunner(info, env);
        final PatchingResult result = runner.executeDirect(new FileInputStream(file), ContentVerificationPolicy.STRICT);
        result.commit();
        return result;
    }

    PatchingResult rollback(final PatchInfo info, final String patchId) throws PatchingException {
        final PatchingTaskRunner runner = new PatchingTaskRunner(info, env);
        final PatchingResult result = runner.rollback(patchId, false);
        result.commit();
        return result;
    }

}

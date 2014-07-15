package org.jboss.as.test.patching;

import static org.jboss.as.patching.IoUtils.mkdir;
import static org.jboss.as.test.patching.PatchingTestUtil.BASE_MODULE_DIRECTORY;
import static org.jboss.as.test.patching.PatchingTestUtil.DO_CLEANUP;
import static org.jboss.as.test.patching.PatchingTestUtil.MODULES_PATH;
import static org.jboss.as.test.patching.PatchingTestUtil.assertPatchElements;
import static org.jboss.as.test.patching.PatchingTestUtil.randomString;

import java.io.File;
import java.io.IOException;

import javax.inject.Inject;

import org.jboss.as.patching.IoUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.wildfly.core.testrunner.ServerController;

/**
 * @author Martin Simka
 */
public class AbstractPatchingTestCase {

    protected File tempDir;

    @Inject
    protected ServerController controller;

    @Before
    public void prepareForAll() throws IOException {
        tempDir = mkdir(new File(System.getProperty("java.io.tmpdir")), randomString());
    }

    @After
    public void cleanupForAll() throws Exception {
        if (controller.isStarted()) { controller.stop(); }

        // clean up created temporary files and directories
        if (DO_CLEANUP) {
            if (IoUtils.recursiveDelete(tempDir)) {
                tempDir.deleteOnExit();
            }
        }

        rollbackAllPatches();
    }

    protected void rollbackAllPatches() throws Exception {
        // rollback all installed patches
        final boolean success = CliUtilsForPatching.rollbackAll();
        boolean ok = false;
        try {
            if (!success) {
                Assert.fail("failed to rollback all patches " + CliUtilsForPatching.info(false));
            }
            assertPatchElements(new File(MODULES_PATH), null);
            ok = true;
        } finally {
            if (!ok) {
                // Reset installation state
                final File home = new File(PatchingTestUtil.AS_DISTRIBUTION);
                PatchingTestUtil.resetInstallationState(home, BASE_MODULE_DIRECTORY);
            }
        }
    }
}

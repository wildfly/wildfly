package org.jboss.as.test.patching;

import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.patching.IoUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;

import java.io.File;
import java.io.IOException;

import static org.jboss.as.patching.IoUtils.mkdir;
import static org.jboss.as.test.patching.PatchingTestUtil.BASE_MODULE_DIRECTORY;
import static org.jboss.as.test.patching.PatchingTestUtil.CONTAINER;
import static org.jboss.as.test.patching.PatchingTestUtil.DO_CLEANUP;
import static org.jboss.as.test.patching.PatchingTestUtil.MODULES_PATH;
import static org.jboss.as.test.patching.PatchingTestUtil.assertPatchElements;
import static org.jboss.as.test.patching.PatchingTestUtil.randomString;

/**
 * @author Martin Simka
 */
public class AbstractPatchingTestCase {

    protected File tempDir;

    @ArquillianResource
    protected ContainerController controller;

    @Before
    public void prepareForAll() throws IOException {
        tempDir = mkdir(new File(System.getProperty("java.io.tmpdir")), randomString());
    }

    @After
    public void cleanupForAll() throws Exception {
        if (controller.isStarted(CONTAINER))
            controller.stop(CONTAINER);

        // clean up created temporary files and directories
        if(DO_CLEANUP) {
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

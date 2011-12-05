/**
 *
 */
package org.jboss.as.server.deployment.scanner;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CANCELLED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.COMPOSITE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEPLOY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FULL_REPLACE_DEPLOYMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_CHILDREN_NAMES_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ROLLED_BACK;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STEPS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.UNDEPLOY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.Operation;
import org.jboss.as.controller.client.OperationMessageHandler;
import org.jboss.as.server.deployment.repository.api.ContentRepository;
import org.jboss.as.server.deployment.repository.api.ServerDeploymentRepository;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.threads.AsyncFuture;
import org.jboss.vfs.VirtualFile;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Unit tests of {@link FileSystemDeploymentService}.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class FileSystemDeploymentServiceUnitTestCase {

    private static Logger logger = Logger.getLogger(FileSystemDeploymentServiceUnitTestCase.class);

    private static long count = System.currentTimeMillis();

    private static final Random random = new Random(System.currentTimeMillis());

    private static final DiscardTaskExecutor executor = new DiscardTaskExecutor();

    private static AutoDeployTestSupport testSupport;
    private File tmpDir;

    @BeforeClass
    public static void createTestSupport() throws Exception {
        testSupport = new AutoDeployTestSupport(FileSystemDeploymentServiceUnitTestCase.class.getSimpleName());
    }

    @AfterClass
    public static void cleanup() throws Exception {
        if (testSupport != null) {
            testSupport.cleanupFiles();
        }
    }

    @Before
    public void setup() throws Exception {
        executor.clear();

        File root = testSupport.getTempDir();
        for (int i = 0; i < 200; i++) {
            tmpDir = new File(root, String.valueOf(count++));
            if (!tmpDir.exists() && tmpDir.mkdirs()) {
                break;
            }
        }

        if (!tmpDir.exists()) {
            throw new RuntimeException("cannot create tmpDir");
        }
    }

    @After
    public void tearDown() throws Exception {
        testSupport.cleanupChannels();
    }

    @Test
    public void testIgnoreNoMarker() throws Exception {
        File f1 = createFile("foo.war");
        TesteeSet ts = createTestee();
//        ts.controller.addGetDeploymentNamesResponse();
        ts.testee.scan();
        assertTrue(ts.repo.content.isEmpty());
        assertTrue(f1.exists());
    }

    @Test
    public void testBasicDeploy() throws Exception {
        File war = createFile("foo.war");
        File dodeploy = createFile("foo.war" + FileSystemDeploymentService.DO_DEPLOY);
        File deployed = new File(tmpDir, "foo.war" + FileSystemDeploymentService.DEPLOYED);
        TesteeSet ts = createTestee();
//        ts.controller.addGetDeploymentNamesResponse();
        ts.controller.addCompositeSuccessResponse(1);
        ts.testee.scan();
        // Since AS7-431 the content is no longer managed
        //assertEquals(1, ts.repo.content.size());
        assertTrue(war.exists());
        assertFalse(dodeploy.exists());
        assertTrue(deployed.exists());
    }

    @Test
    public void testNestedDeploy() throws Exception {
        TesteeSet ts = createTestee();
        File nestedDir = new File(tmpDir, "nested");
        File war = createFile(nestedDir, "foo.war");
        File dodeploy = createFile(nestedDir, "foo.war" + FileSystemDeploymentService.DO_DEPLOY);
        File deployed = new File(nestedDir, "foo.war" + FileSystemDeploymentService.DEPLOYED);
//        ts.controller.addGetDeploymentNamesResponse();
        ts.controller.addCompositeSuccessResponse(1);
        ts.testee.scan();
        // Since AS7-431 the content is no longer managed
        //assertEquals(1, ts.repo.content.size());
        assertTrue(war.exists());
        assertFalse(dodeploy.exists());
        assertTrue(deployed.exists());
    }

    @Test
    public void testTwoFileDeploy() throws Exception {
        File war1 = createFile("foo.war");
        File dodeploy1 = createFile("foo.war" + FileSystemDeploymentService.DO_DEPLOY);
        File deployed1 = new File(tmpDir, "foo.war" + FileSystemDeploymentService.DEPLOYED);
        File war2 = createFile("bar.war");
        File dodeploy2 = createFile("bar.war" + FileSystemDeploymentService.DO_DEPLOY);
        File deployed2 = new File(tmpDir, "bar.war" + FileSystemDeploymentService.DEPLOYED);
        TesteeSet ts = createTestee();
//        ts.controller.addGetDeploymentNamesResponse();
        ts.controller.addCompositeSuccessResponse(2);
        ts.testee.scan();
        // Since AS7-431 the content is no longer managed
        //assertEquals(2, ts.repo.content.size());
        assertTrue(war1.exists());
        assertFalse(dodeploy1.exists());
        assertTrue(deployed1.exists());
        assertTrue(war2.exists());
        assertFalse(dodeploy2.exists());
        assertTrue(deployed2.exists());
    }

    @Test
    public void testBasicFailure() throws Exception {
        File war = createFile("foo.war");
        File dodeploy = createFile("foo.war" + FileSystemDeploymentService.DO_DEPLOY);
        File deployed = new File(tmpDir, "foo.war" + FileSystemDeploymentService.DEPLOYED);
        File failed = new File(tmpDir, "foo.war" + FileSystemDeploymentService.FAILED_DEPLOY);
        TesteeSet ts = createTestee();
//        ts.controller.addGetDeploymentNamesResponse();
        ts.controller.addCompositeFailureResponse(1, 1);
        ts.testee.scan();
        // Since AS7-431 the content is no longer managed
        //assertEquals(1, ts.repo.content.size());
        assertTrue(war.exists());
        assertFalse(dodeploy.exists());
        assertFalse(deployed.exists());
        assertTrue(failed.exists());
    }

    @Test
    public void testTwoFileFailure() throws Exception {
        File war1 = createFile("foo.war");
        File dodeploy1 = createFile("foo.war" + FileSystemDeploymentService.DO_DEPLOY);
        File deployed1 = new File(tmpDir, "foo.war" + FileSystemDeploymentService.DEPLOYED);
        File failed1 = new File(tmpDir, "foo.war" + FileSystemDeploymentService.FAILED_DEPLOY);
        File war2 = createFile("bar.war");
        File dodeploy2 = createFile("bar.war" + FileSystemDeploymentService.DO_DEPLOY);
        File deployed2 = new File(tmpDir, "bar.war" + FileSystemDeploymentService.DEPLOYED);
        File failed2 = new File(tmpDir, "bar.war" + FileSystemDeploymentService.FAILED_DEPLOY);
        TesteeSet ts = createTestee();
//        ts.controller.addGetDeploymentNamesResponse();
        ts.controller.addCompositeFailureResponse(2, 2);
        ts.testee.scan();
        // Since AS7-431 the content is no longer managed
        //assertEquals(2, ts.repo.content.size());
        assertTrue(war1.exists());
        assertFalse(dodeploy1.exists());
        assertFalse(deployed1.exists());
        assertTrue(failed1.exists());
        assertTrue(war2.exists());
        assertFalse(dodeploy2.exists());
        assertFalse(deployed2.exists());
        assertTrue(failed2.exists());
    }

    @Test
    public void testCancellationDueToFailure() throws Exception {
        File war1 = createFile("bar.war");
        File dodeploy1 = createFile("bar.war" + FileSystemDeploymentService.DO_DEPLOY);
        File deployed1 = new File(tmpDir, "bar.war" + FileSystemDeploymentService.DEPLOYED);
        File failed1 = new File(tmpDir, "bar.war" + FileSystemDeploymentService.FAILED_DEPLOY);
        File war2 = createFile("foo.war");
        File dodeploy2 = createFile("foo.war" + FileSystemDeploymentService.DO_DEPLOY);
        File deployed2 = new File(tmpDir, "foo.war" + FileSystemDeploymentService.DEPLOYED);
        File failed2 = new File(tmpDir, "foo.war" + FileSystemDeploymentService.FAILED_DEPLOY);
        TesteeSet ts = createTestee();
//        ts.controller.addGetDeploymentNamesResponse();
        ts.controller.addCompositeFailureResponse(2, 1);
        // Retry fails as well
        ts.controller.addCompositeFailureResponse(1, 1);
        ts.testee.scan();
        // Since AS7-431 the content is no longer managed
        //assertEquals(2, ts.repo.content.size());
        assertTrue(war1.exists());
        assertFalse(dodeploy1.exists());
        assertFalse(deployed1.exists());
        assertTrue(failed1.exists());
        assertTrue(war2.exists());
        assertFalse(dodeploy2.exists());
        assertFalse(deployed2.exists());
        assertTrue(failed2.exists());
    }

    @Test
    public void testSuccessfulRetry() throws Exception {
        File war1 = createFile("bar.war");
        File dodeploy1 = createFile("bar.war" + FileSystemDeploymentService.DO_DEPLOY);
        File deployed1 = new File(tmpDir, "bar.war" + FileSystemDeploymentService.DEPLOYED);
        File failed1 = new File(tmpDir, "bar.war" + FileSystemDeploymentService.FAILED_DEPLOY);
        File war2 = createFile("foo.war");
        File dodeploy2 = createFile("foo.war" + FileSystemDeploymentService.DO_DEPLOY);
        File deployed2 = new File(tmpDir, "foo.war" + FileSystemDeploymentService.DEPLOYED);
        File failed2 = new File(tmpDir, "foo.war" + FileSystemDeploymentService.FAILED_DEPLOY);
        TesteeSet ts = createTestee();
//        ts.controller.addGetDeploymentNamesResponse();
        ts.controller.addCompositeFailureResponse(2, 1);
        // Retry succeeds
        ts.controller.addCompositeSuccessResponse(1);
        ts.testee.scan();
        // Since AS7-431 the content is no longer managed
        //assertEquals(2, ts.repo.content.size());
        assertTrue(war1.exists());
        assertTrue(war2.exists());
        assertFalse(dodeploy1.exists());
        assertFalse(dodeploy2.exists());
        assertFalse(deployed1.exists() && deployed2.exists());
        assertTrue(failed1.exists() || failed2.exists());
    }

    @Test
    public void testCleanSpuriousMarkers() throws Exception {
        File f1 = createFile("spurious" + FileSystemDeploymentService.DEPLOYED);
        File f2 = createFile(new File(tmpDir, "nested"), "nested" + FileSystemDeploymentService.DEPLOYED);
        File f3 = createFile("ok" + FileSystemDeploymentService.DEPLOYED);
        File f4 = createFile(new File(tmpDir, "nested"), "nested-ok" + FileSystemDeploymentService.DEPLOYED);
        File f5 = createFile("spurious" + FileSystemDeploymentService.FAILED_DEPLOY);
        File f6 = createFile(new File(tmpDir, "nested"), "nested" + FileSystemDeploymentService.FAILED_DEPLOY);
        File f7 = createFile("spurious" + FileSystemDeploymentService.DEPLOYING);
        File f8 = createFile(new File(tmpDir, "nested"), "nested" + FileSystemDeploymentService.DEPLOYING);
        File f9 = createFile("spurious" + FileSystemDeploymentService.UNDEPLOYING);
        File f10 = createFile(new File(tmpDir, "nested"), "nested" + FileSystemDeploymentService.UNDEPLOYING);
        File f11 = createFile("spurious" + FileSystemDeploymentService.PENDING);
        File f12 = createFile(new File(tmpDir, "nested"), "nested" + FileSystemDeploymentService.PENDING);

        File ok = createFile("ok");
        File nestedOK = createFile(new File(tmpDir, "nested"), "nested-ok");

        // We expect 2 requests for children names due to subdir "nested"
        MockServerController sc = new MockServerController(new MockDeploymentRepository(), "ok", "nested-ok");
//        sc.responses.add(sc.responses.get(0));
        TesteeSet ts = createTestee(sc);

        // These should get cleaned in start
        Assert.assertFalse(f1.exists());
        Assert.assertFalse(f2.exists());
        Assert.assertTrue(f3.exists());
        Assert.assertTrue(f4.exists());
        Assert.assertTrue(ok.exists());
        Assert.assertTrue(nestedOK.exists());

        ts.testee.scan();

        // failed deployments should not be cleaned on initial scan - will retry
        Assert.assertTrue(f5.exists());
        Assert.assertTrue(f6.exists());

        // The others should get cleaned in a scan
        Assert.assertFalse(f7.exists());
        Assert.assertFalse(f8.exists());
        Assert.assertFalse(f9.exists());
        Assert.assertFalse(f10.exists());
        Assert.assertFalse(f11.exists());
        Assert.assertFalse(f12.exists());

        Assert.assertTrue(ok.exists());
        Assert.assertTrue(nestedOK.exists());

        f1 = createFile("spurious" + FileSystemDeploymentService.DEPLOYED);
        f2 = createFile(new File(tmpDir, "nested"), "nested" + FileSystemDeploymentService.DEPLOYED);

        ts.testee.scan();

        // Failed deployments should be cleaned on subsequent scans.
        Assert.assertFalse(f5.exists());
        Assert.assertFalse(f6.exists());

        Assert.assertFalse(f1.exists());
        Assert.assertFalse(f2.exists());

        Assert.assertTrue(ok.exists());
        Assert.assertTrue(nestedOK.exists());
    }

    @Test
    public void testOverridePreexisting() throws Exception {
        File war = createFile("foo.war");
        File dodeploy = createFile("foo.war" + FileSystemDeploymentService.DO_DEPLOY);
        File deployed = new File(tmpDir, "foo.war" + FileSystemDeploymentService.DEPLOYED);
        TesteeSet ts = createTestee("foo.war");
//        ts.controller.addGetDeploymentNamesResponse();
        ts.controller.addCompositeSuccessResponse(1);
        ts.testee.scan();
        // Since AS7-431 the content is no longer managed
        //assertEquals(1, ts.repo.content.size());
        assertTrue(war.exists());
        assertFalse(dodeploy.exists());
        assertTrue(deployed.exists());
    }

    @Test
    public void testRedeploy() throws Exception {
        File war = createFile("foo.war");
        File dodeploy = createFile("foo.war" + FileSystemDeploymentService.DO_DEPLOY);
        File deployed = new File(tmpDir, "foo.war" + FileSystemDeploymentService.DEPLOYED);
        TesteeSet ts = createTestee("foo.war");
//        ts.controller.addGetDeploymentNamesResponse();
        ts.controller.addCompositeSuccessResponse(1);
        ts.testee.scan();
        // Since AS7-431 the content is no longer managed
        //assertEquals(1, ts.repo.content.size());
        assertTrue(war.exists());
        assertFalse(dodeploy.exists());
        assertTrue(deployed.exists());
        assertEquals(1, ts.controller.added.size());
        assertEquals(1, ts.controller.deployed.size());
        byte[] bytes = ts.controller.deployed.get("foo.war");
        // Since AS7-431 the content is no longer managed
        //assertTrue(Arrays.equals(bytes, ts.repo.content.iterator().next()));

        dodeploy = createFile("foo.war" + FileSystemDeploymentService.DO_DEPLOY);
//        ts.controller.addGetDeploymentNamesResponse();
        ts.controller.addCompositeSuccessResponse(1);
        ts.testee.scan();
        // Since AS7-431 the content is no longer managed
        //assertEquals(2, ts.repo.content.size());
        assertTrue(war.exists());
        assertFalse(dodeploy.exists());
        assertTrue(deployed.exists());
        assertEquals(1, ts.controller.added.size());
        assertEquals(1, ts.controller.deployed.size());
        byte[] newbytes = ts.controller.deployed.get("foo.war");
        assertFalse(Arrays.equals(newbytes, bytes));
        // Since AS7-431 the content is no longer managed
        /*
        boolean installed = false;
        for (byte[] content : ts.repo.content) {
            if (Arrays.equals(newbytes, content)) {
                installed = true;
                break;
            }
        }
        assertTrue(installed);
        */
    }

    @Test
    public void testTwoFileRedeploy() throws Exception {
        File war1 = createFile("foo.war");
        File dodeploy1 = createFile("foo.war" + FileSystemDeploymentService.DO_DEPLOY);
        File deployed1 = new File(tmpDir, "foo.war" + FileSystemDeploymentService.DEPLOYED);
        File war2 = createFile("bar.war");
        File dodeploy2 = createFile("bar.war" + FileSystemDeploymentService.DO_DEPLOY);
        File deployed2 = new File(tmpDir, "bar.war" + FileSystemDeploymentService.DEPLOYED);
        TesteeSet ts = createTestee();
//        ts.controller.addGetDeploymentNamesResponse();
        ts.controller.addCompositeSuccessResponse(2);
        ts.testee.scan();
        // Since AS7-431 the content is no longer managed
        //assertEquals(2, ts.repo.content.size());
        assertTrue(war1.exists());
        assertFalse(dodeploy1.exists());
        assertTrue(deployed1.exists());
        assertTrue(war2.exists());
        assertFalse(dodeploy2.exists());
        assertTrue(deployed2.exists());

        dodeploy1 = createFile("foo.war" + FileSystemDeploymentService.DO_DEPLOY);
        dodeploy2 = createFile("bar.war" + FileSystemDeploymentService.DO_DEPLOY);
//        ts.controller.addGetDeploymentNamesResponse();
        ts.controller.addCompositeSuccessResponse(2);
        ts.testee.scan();
        // Since AS7-431 the content is no longer managed
        //assertEquals(4, ts.repo.content.size());
        assertTrue(war1.exists());
        assertFalse(dodeploy1.exists());
        assertTrue(deployed1.exists());
        assertTrue(war2.exists());
        assertFalse(dodeploy2.exists());
        assertTrue(deployed2.exists());
    }

    @Test
    public void testFailedRedeploy() throws Exception {
        File war = createFile("foo.war");
        File dodeploy = createFile("foo.war" + FileSystemDeploymentService.DO_DEPLOY);
        File deployed = new File(tmpDir, "foo.war" + FileSystemDeploymentService.DEPLOYED);
        File failed = new File(tmpDir, "foo.war" + FileSystemDeploymentService.FAILED_DEPLOY);
        TesteeSet ts = createTestee("foo.war");
//        ts.controller.addGetDeploymentNamesResponse();
        ts.controller.addCompositeSuccessResponse(1);
        ts.testee.scan();
        // Since AS7-431 the content is no longer managed
        //assertEquals(1, ts.repo.content.size());
        assertTrue(war.exists());
        assertFalse(dodeploy.exists());
        assertTrue(deployed.exists());
        assertFalse(failed.exists());

        dodeploy = createFile("foo.war" + FileSystemDeploymentService.DO_DEPLOY);
//        ts.controller.addGetDeploymentNamesResponse();
        ts.controller.addCompositeFailureResponse(1, 1);
        ts.testee.scan();
        // Since AS7-431 the content is no longer managed
        //assertEquals(2, ts.repo.content.size());
        assertTrue(war.exists());
        assertFalse(dodeploy.exists());
        assertFalse(deployed.exists());
        assertTrue(failed.exists());
    }

    @Test
    public void testTwoFileFailedRedeploy() throws Exception {
        File war1 = createFile("bar.war");
        File dodeploy1 = createFile("bar.war" + FileSystemDeploymentService.DO_DEPLOY);
        File deployed1 = new File(tmpDir, "bar.war" + FileSystemDeploymentService.DEPLOYED);
        File failed1 = new File(tmpDir, "bar.war" + FileSystemDeploymentService.FAILED_DEPLOY);
        File war2 = createFile("foo.war");
        File dodeploy2 = createFile("foo.war" + FileSystemDeploymentService.DO_DEPLOY);
        File deployed2 = new File(tmpDir, "foo.war" + FileSystemDeploymentService.DEPLOYED);
        File failed2 = new File(tmpDir, "foo.war" + FileSystemDeploymentService.FAILED_DEPLOY);
        TesteeSet ts = createTestee();
//        ts.controller.addGetDeploymentNamesResponse();
        ts.controller.addCompositeSuccessResponse(2);
        ts.testee.scan();
        // Since AS7-431 the content is no longer managed
        //assertEquals(2, ts.repo.content.size());
        assertTrue(war1.exists());
        assertFalse(dodeploy1.exists());
        assertTrue(deployed1.exists());
        assertTrue(war2.exists());
        assertFalse(dodeploy2.exists());
        assertTrue(deployed2.exists());

        dodeploy1 = createFile("foo.war" + FileSystemDeploymentService.DO_DEPLOY);
        dodeploy2 = createFile("bar.war" + FileSystemDeploymentService.DO_DEPLOY);
//        ts.controller.addGetDeploymentNamesResponse();
        ts.controller.addCompositeFailureResponse(2, 1);
        // Retry fails as well
        ts.controller.addCompositeFailureResponse(1, 1);
        ts.testee.scan();
        // Since AS7-431 the content is no longer managed
        //assertEquals(4, ts.repo.content.size());
        assertTrue(war1.exists());
        assertFalse(dodeploy1.exists());
        assertFalse(deployed1.exists());
        assertTrue(failed1.exists());
        assertTrue(war2.exists());
        assertFalse(dodeploy2.exists());
        assertFalse(deployed2.exists());
        assertTrue(failed2.exists());
    }

    @Test
    public void testFailedRedeployNoMarker() throws Exception {
        File war = createFile("foo.war");
        File dodeploy = createFile("foo.war" + FileSystemDeploymentService.DO_DEPLOY);
        File deployed = new File(tmpDir, "foo.war" + FileSystemDeploymentService.DEPLOYED);
        File failed = new File(tmpDir, "foo.war" + FileSystemDeploymentService.FAILED_DEPLOY);
        TesteeSet ts = createTestee();
        ts.testee.setAutoDeployZippedContent(true);
        ts.controller.addCompositeFailureResponse(1, 1);
        ts.testee.scan();
        assertTrue(war.exists());
        assertFalse(dodeploy.exists());
        assertFalse(deployed.exists());
        assertTrue(failed.exists());

        // New deployment won't work unless content timestamp is newer than failed marker, so...
        failed.setLastModified(0);

        testSupport.createZip(war, 0, false, false, true, false);
        ts.controller.addCompositeSuccessResponse(1);
        ts.testee.scan();
        assertTrue(war.exists());
        assertFalse(dodeploy.exists());
        assertTrue(deployed.exists());
        assertFalse(failed.exists());

    }

    @Test
    public void testSuccessfulRetryRedeploy() throws Exception {
        File war1 = createFile("bar.war");
        File dodeploy1 = createFile("bar.war" + FileSystemDeploymentService.DO_DEPLOY);
        File deployed1 = new File(tmpDir, "bar.war" + FileSystemDeploymentService.DEPLOYED);
        File failed1 = new File(tmpDir, "bar.war" + FileSystemDeploymentService.FAILED_DEPLOY);
        File war2 = createFile("foo.war");
        File dodeploy2 = createFile("foo.war" + FileSystemDeploymentService.DO_DEPLOY);
        File deployed2 = new File(tmpDir, "foo.war" + FileSystemDeploymentService.DEPLOYED);
        File failed2 = new File(tmpDir, "foo.war" + FileSystemDeploymentService.FAILED_DEPLOY);
        TesteeSet ts = createTestee();
//        ts.controller.addGetDeploymentNamesResponse();
        ts.controller.addCompositeSuccessResponse(2);
        ts.testee.scan();
        // Since AS7-431 the content is no longer managed
        //assertEquals(2, ts.repo.content.size());
        assertTrue(war1.exists());
        assertFalse(dodeploy1.exists());
        assertTrue(deployed1.exists());
        assertTrue(war2.exists());
        assertFalse(dodeploy2.exists());
        assertTrue(deployed2.exists());

        dodeploy1 = createFile("foo.war" + FileSystemDeploymentService.DO_DEPLOY);
        dodeploy2 = createFile("bar.war" + FileSystemDeploymentService.DO_DEPLOY);
//        ts.controller.addGetDeploymentNamesResponse();
        ts.controller.addCompositeFailureResponse(2, 1);
        // Retry succeeds
        ts.controller.addCompositeSuccessResponse(1);
        ts.testee.scan();
        // Since AS7-431 the content is no longer managed
        //assertEquals(4, ts.repo.content.size());
        assertTrue(war1.exists());
        assertTrue(war2.exists());
        assertFalse(dodeploy1.exists());
        assertFalse(dodeploy2.exists());
        assertFalse(deployed1.exists() && deployed2.exists());
        assertTrue(failed1.exists() || failed2.exists());
    }

    @Test
    public void testUndeployByMarkerDeletion() throws Exception {
        File war = createFile("foo.war");
        File dodeploy = createFile("foo.war" + FileSystemDeploymentService.DO_DEPLOY);
        File deployed = new File(tmpDir, "foo.war" + FileSystemDeploymentService.DEPLOYED);
        TesteeSet ts = createTestee();
//        ts.controller.addGetDeploymentNamesResponse();
        ts.controller.addCompositeSuccessResponse(1);
        ts.testee.scan();
        // Since AS7-431 the content is no longer managed
        //assertEquals(1, ts.repo.content.size());
        assertTrue(war.exists());
        assertFalse(dodeploy.exists());
        assertTrue(deployed.exists());
        assertEquals(1, ts.controller.added.size());
        assertEquals(1, ts.controller.deployed.size());

        assertTrue(deployed.delete());
//        ts.controller.addGetDeploymentNamesResponse();
        ts.controller.addCompositeSuccessResponse(1);
        ts.testee.scan();
        // Since AS7-431 the content is no longer managed
        //assertEquals(1, ts.repo.content.size());
        assertTrue(war.exists());
        assertFalse(dodeploy.exists());
        assertFalse(deployed.exists());
        assertEquals(0, ts.controller.added.size());
        assertEquals(0, ts.controller.deployed.size());
    }

    @Test
    public void testUndeployByContentDeletionZipped() throws Exception {

        // First, zipped content

        File war = createFile("foo.war");
        File dodeploy = createFile("foo.war" + FileSystemDeploymentService.DO_DEPLOY);
        File deployed = new File(tmpDir, "foo.war" + FileSystemDeploymentService.DEPLOYED);
        TesteeSet ts = createTestee();
        ts.testee.setAutoDeployZippedContent(true);
        ts.controller.addCompositeSuccessResponse(1);
        ts.testee.scan();
        assertTrue(war.exists());
        assertFalse(dodeploy.exists());
        assertTrue(deployed.exists());
        assertEquals(1, ts.controller.added.size());
        assertEquals(1, ts.controller.deployed.size());

        assertTrue(war.delete());
        ts.controller.addCompositeSuccessResponse(1);
        ts.testee.scan();
        assertFalse(war.exists());
        assertFalse(dodeploy.exists());
        assertFalse(deployed.exists());
        assertEquals(0, ts.controller.added.size());
        assertEquals(0, ts.controller.deployed.size());

        // Next, zipped content with auto-deploy disabled

        war = createFile("foo.war");
        dodeploy = createFile("foo.war" + FileSystemDeploymentService.DO_DEPLOY);
        deployed = new File(tmpDir, "foo.war" + FileSystemDeploymentService.DEPLOYED);
        ts = createTestee();
        ts.testee.setAutoDeployZippedContent(false);
        ts.controller.addCompositeSuccessResponse(1);
        ts.testee.scan();
        assertTrue(war.exists());
        assertFalse(dodeploy.exists());
        assertTrue(deployed.exists());
        assertEquals(1, ts.controller.added.size());
        assertEquals(1, ts.controller.deployed.size());

        assertTrue(war.delete());
        ts.testee.scan();
        assertFalse(war.exists());
        assertFalse(dodeploy.exists());
        assertTrue(deployed.exists());
        assertEquals(1, ts.controller.added.size());
        assertEquals(1, ts.controller.deployed.size());
    }

    @Test
    public void testUndeployByContentDeletionExploded() throws Exception {

        // First, auto-deploy enabled

        File war = new File(tmpDir, "foo.war");
        war.mkdirs();
        File dodeploy = createFile("foo.war" + FileSystemDeploymentService.DO_DEPLOY);
        File deployed = new File(tmpDir, "foo.war" + FileSystemDeploymentService.DEPLOYED);
        TesteeSet ts = createTestee();
        ts.testee.setAutoDeployExplodedContent(true);
        ts.controller.addCompositeSuccessResponse(1);
        ts.testee.scan();
        assertTrue(war.exists());
        assertFalse(dodeploy.exists());
        assertTrue(deployed.exists());
        assertEquals(1, ts.controller.added.size());
        assertEquals(1, ts.controller.deployed.size());

        assertTrue(war.delete());
        ts.controller.addCompositeSuccessResponse(1);
        ts.testee.scan();
        assertFalse(war.exists());
        assertFalse(dodeploy.exists());
        assertFalse(deployed.exists());
        assertEquals(0, ts.controller.added.size());
        assertEquals(0, ts.controller.deployed.size());

        // Next, with auto-deploy disabled

        war = new File(tmpDir, "foo.war");
        war.mkdirs();
        dodeploy = createFile("foo.war" + FileSystemDeploymentService.DO_DEPLOY);
        deployed = new File(tmpDir, "foo.war" + FileSystemDeploymentService.DEPLOYED);
        ts = createTestee();
        ts.testee.setAutoDeployExplodedContent(false);
        ts.controller.addCompositeSuccessResponse(1);
        ts.testee.scan();
        assertTrue(war.exists());
        assertFalse(dodeploy.exists());
        assertTrue(deployed.exists());
        assertEquals(1, ts.controller.added.size());
        assertEquals(1, ts.controller.deployed.size());

        assertTrue(war.delete());
        ts.testee.scan();
        assertFalse(war.exists());
        assertFalse(dodeploy.exists());
        assertTrue(deployed.exists());
        assertEquals(1, ts.controller.added.size());
        assertEquals(1, ts.controller.deployed.size());

        // Now, exploded content

        war = new File(tmpDir, "foo.war");
        war.mkdirs();
        dodeploy = createFile("foo.war" + FileSystemDeploymentService.DO_DEPLOY);
        deployed = new File(tmpDir, "foo.war" + FileSystemDeploymentService.DEPLOYED);
        ts = createTestee();
        ts.testee.setAutoDeployExplodedContent(true);
        ts.controller.addCompositeSuccessResponse(1);
        ts.testee.scan();
        assertTrue(war.exists());
        assertFalse(dodeploy.exists());
        assertTrue(deployed.exists());
        assertEquals(1, ts.controller.added.size());
        assertEquals(1, ts.controller.deployed.size());

        assertTrue(war.delete());
        ts.controller.addCompositeSuccessResponse(1);
        ts.testee.scan();
        assertFalse(war.exists());
        assertFalse(dodeploy.exists());
        assertFalse(deployed.exists());
        assertEquals(0, ts.controller.added.size());
        assertEquals(0, ts.controller.deployed.size());
    }

    @Test
    public void testRedeployUndeploy() throws Exception {
        File war = createFile("foo.war");
        File dodeploy = createFile("foo.war" + FileSystemDeploymentService.DO_DEPLOY);
        File deployed = new File(tmpDir, "foo.war" + FileSystemDeploymentService.DEPLOYED);
        TesteeSet ts = createTestee();
//        ts.controller.addGetDeploymentNamesResponse();
        ts.controller.addCompositeSuccessResponse(1);
        ts.testee.scan();
        // Since AS7-431 the content is no longer managed
        //assertEquals(1, ts.repo.content.size());
        assertTrue(war.exists());
        assertFalse(dodeploy.exists());
        assertTrue(deployed.exists());
        assertEquals(1, ts.controller.added.size());
        assertEquals(1, ts.controller.deployed.size());
        byte[] bytes = ts.controller.deployed.get("foo.war");
        // Since AS7-431 the content is no longer managed
        //assertTrue(Arrays.equals(bytes, ts.repo.content.iterator().next()));

        dodeploy = createFile("foo.war" + FileSystemDeploymentService.DO_DEPLOY);
//        ts.controller.addGetDeploymentNamesResponse();
        ts.controller.addCompositeSuccessResponse(1);
        ts.testee.scan();
        // Since AS7-431 the content is no longer managed
        //assertEquals(2, ts.repo.content.size());
        assertTrue(war.exists());
        assertFalse(dodeploy.exists());
        assertTrue(deployed.exists());
        assertEquals(1, ts.controller.added.size());
        assertEquals(1, ts.controller.deployed.size());
        byte[] newbytes = ts.controller.deployed.get("foo.war");
        assertFalse(Arrays.equals(newbytes, bytes));

        assertTrue(deployed.delete());
//        ts.controller.addGetDeploymentNamesResponse();
        ts.controller.addCompositeSuccessResponse(1);
        ts.testee.scan();
        // Since AS7-431 the content is no longer managed
        //assertEquals(2, ts.repo.content.size());
        assertTrue(war.exists());
        assertFalse(dodeploy.exists());
        assertFalse(deployed.exists());
        assertEquals(0, ts.controller.added.size());
        assertEquals(0, ts.controller.deployed.size());
    }

    @Test
    public void testRedeployUndeployedNoMarker() throws Exception {
        File war = createFile("foo.war");
        File dodeploy = createFile("foo.war" + FileSystemDeploymentService.DO_DEPLOY);
        File deployed = new File(tmpDir, "foo.war" + FileSystemDeploymentService.DEPLOYED);
        File undeployed = new File(tmpDir, "foo.war" + FileSystemDeploymentService.UNDEPLOYED);
        TesteeSet ts = createTestee();
        ts.testee.setAutoDeployZippedContent(true);
        ts.controller.addCompositeSuccessResponse(1);
        ts.testee.scan();
        assertTrue(war.exists());
        assertFalse(dodeploy.exists());
        assertTrue(deployed.exists());
        assertEquals(1, ts.controller.added.size());
        assertEquals(1, ts.controller.deployed.size());
        byte[] bytes = ts.controller.deployed.get("foo.war");

        assertTrue(deployed.delete());
        ts.controller.addCompositeSuccessResponse(1);
        ts.testee.scan();
        assertTrue(war.exists());
        assertFalse(dodeploy.exists());
        assertFalse(deployed.exists());
        assertTrue(undeployed.exists());
        assertEquals(0, ts.controller.added.size());
        assertEquals(0, ts.controller.deployed.size());

        // new content will be ignored unless it has a timestamp newer than .undeployed
        // so, rather than sleeping...
        undeployed.setLastModified(0);

        testSupport.createZip(war, 0, false, false, false, false);
        ts.controller.addCompositeSuccessResponse(1);
        ts.testee.scan();
        assertTrue(war.exists());
        assertFalse(dodeploy.exists());
        assertTrue(deployed.exists());
        assertFalse(undeployed.exists());
        assertEquals(1, ts.controller.added.size());
        assertEquals(1, ts.controller.deployed.size());
        byte[] newbytes = ts.controller.deployed.get("foo.war");
        assertFalse(Arrays.equals(newbytes, bytes));
    }

    @Test
    public void testDirectory() throws Exception {
        final File war = createDirectory("foo.war", "index.html");
        File dodeploy = createFile("foo.war" + FileSystemDeploymentService.DO_DEPLOY);
        File deployed = new File(tmpDir, "foo.war" + FileSystemDeploymentService.DEPLOYED);
        TesteeSet ts = createTestee("foo.war");
//        ts.controller.addGetDeploymentNamesResponse();
        ts.controller.addCompositeSuccessResponse(1);
        ts.testee.scan();
        // Since AS7-431 the content is no longer managed
        //assertEquals(1, ts.repo.content.size());
        assertTrue(war.exists());
        assertFalse(dodeploy.exists());
        assertTrue(deployed.exists());

        assertTrue(deployed.delete());
//        ts.controller.addGetDeploymentNamesResponse();
        ts.controller.addCompositeSuccessResponse(1);
        ts.testee.scan();
        // Since AS7-431 the content is no longer managed
        //assertEquals(1, ts.repo.content.size());
        assertTrue(war.exists());
        assertFalse(dodeploy.exists());
        assertFalse(deployed.exists());

    }

    // FIXME remove this marker used to make it easy to find these tests in the IDE

    /**
     * Tests that autodeploy does not happen by default.
     */
    @Test
    public void testNoDefaultAutoDeploy() throws Exception {

        File zip = new File(tmpDir, "foo.war");
        File zipdeployed = new File(tmpDir, "foo.war" + FileSystemDeploymentService.DEPLOYED);
        File zippending = new File(tmpDir, "foo.war" + FileSystemDeploymentService.PENDING);
        testSupport.createZip(zip, 0, false, false, true, false);
        File exploded = new File(tmpDir, "exploded.ear");
        exploded.mkdirs();
        File explodeddeployed = new File(tmpDir, "exploded.war" + FileSystemDeploymentService.DEPLOYED);
        File explodedpending = new File(tmpDir, "exploded.war" + FileSystemDeploymentService.PENDING);

        TesteeSet ts = createTestee();

        ts.testee.scan();

        assertFalse(zipdeployed.exists());
        assertFalse(zippending.exists());
        assertFalse(explodeddeployed.exists());
        assertFalse(explodedpending.exists());

        assertTrue(zip.exists());
        assertTrue(exploded.exists());
    }

    /**
     * Tests that an incomplete zipped deployment does not
     * auto-deploy.
     */
    @Test
    public void testIncompleteZipped() throws Exception {

        File incomplete = new File(tmpDir, "foo.war");
        File deployed = new File(tmpDir, "foo.war" + FileSystemDeploymentService.DEPLOYED);
        File pending = new File(tmpDir, "foo.war" + FileSystemDeploymentService.PENDING);
        testSupport.createZip(incomplete, 0, false, true, true, false);
        TesteeSet ts = createTestee();
        ts.testee.setAutoDeployZippedContent(true);

        ts.testee.scan();

        assertFalse(deployed.exists());
        assertTrue(pending.exists());

        incomplete.delete();
        testSupport.createZip(incomplete, 0, false, false, false, false);

        ts.controller.addCompositeSuccessResponse(1);
        ts.testee.scan();

        assertTrue(deployed.exists());
        assertFalse(pending.exists());
    }

    /**
     * Tests that an exploded deployment with an incomplete child
     * does not auto-deploy, but does auto-deploy when the child
     * is complete.
     */
    @Test
    public void testIncompleteExploded() throws Exception {
        File deployment = new File(tmpDir, "foo.ear");
        deployment.mkdirs();
        File deployed = new File(tmpDir, "foo.ear" + FileSystemDeploymentService.DEPLOYED);
        File pending = new File(tmpDir, "foo.ear" + FileSystemDeploymentService.PENDING);
        File incomplete = new File(deployment, "bar.war");
        testSupport.createZip(incomplete, 0, false, true, true, false);

        TesteeSet ts = createTestee();
        ts.testee.setAutoDeployExplodedContent(true);

        ts.testee.scan();

        assertFalse(deployed.exists());
        assertTrue(pending.exists());

        incomplete.delete();
        testSupport.createZip(incomplete, 0, false, false, true, false);

        ts.controller.addCompositeSuccessResponse(1);
        ts.testee.scan();

        assertTrue(deployed.exists());
        assertFalse(pending.exists());

    }

    /**
     * Test that an incomplete deployment prevents changing other items
     */
    @Test
    public void testIncompleteBlocksComplete() throws Exception {

        File incomplete = new File(tmpDir, "foo.war");
        File deployed = new File(tmpDir, "foo.war" + FileSystemDeploymentService.DEPLOYED);
        File pending = new File(tmpDir, "foo.war" + FileSystemDeploymentService.PENDING);
        testSupport.createZip(incomplete, 0, false, true, true, false);
        File complete = new File(tmpDir, "complete.jar");
        File completeDeployed = new File(tmpDir, "complete.jar" + FileSystemDeploymentService.DEPLOYED);
        File completePending = new File(tmpDir, "complete.jar" + FileSystemDeploymentService.PENDING);
        testSupport.createZip(complete, 0, false, false, true, false);

        TesteeSet ts = createTestee();
        ts.testee.setAutoDeployZippedContent(true);

        ts.testee.scan();

        assertFalse(deployed.exists());
        assertFalse(completeDeployed.exists());
        assertTrue(pending.exists());
        assertTrue(completePending.exists());

        incomplete.delete();
        testSupport.createZip(incomplete, 0, false, false, false, false);

        ts.controller.addCompositeSuccessResponse(2);
        ts.testee.scan();

        assertTrue(deployed.exists());
        assertTrue(completeDeployed.exists());
        assertFalse(pending.exists());
        assertFalse(completePending.exists());

    }

    /**
     * Tests that an incomplete deployment that makes no progress gets a .failed marker
     */
    @Test
    public void testMaxNoProgress() throws Exception {

        File incomplete = new File(tmpDir, "foo.war");
        File deployed = new File(tmpDir, "foo.war" + FileSystemDeploymentService.DEPLOYED);
        File pending = new File(tmpDir, "foo.war" + FileSystemDeploymentService.PENDING);
        File failed = new File(tmpDir, "foo.war" + FileSystemDeploymentService.FAILED_DEPLOY);
        testSupport.createZip(incomplete, 0, false, true, true, false);
        TesteeSet ts = createTestee();
        ts.testee.setAutoDeployZippedContent(true);
        ts.testee.setMaxNoProgress(-1); // trigger immediate failure

        ts.testee.scan();

        assertFalse(deployed.exists());
        assertTrue(failed.exists());
        assertFalse(pending.exists());

    }

    /**
     * Tests that non-archive/non-marker files are ignored
     */
    @Test
    public void testIgnoreNonArchive() throws Exception {

        File txt = createFile("foo.txt");
        File deployed = new File(tmpDir, "foo.txt" + FileSystemDeploymentService.DEPLOYED);
        File pending = new File(tmpDir, "foo.txt" + FileSystemDeploymentService.PENDING);
        TesteeSet ts = createTestee();
        ts.testee.setAutoDeployExplodedContent(true);
        ts.testee.setAutoDeployZippedContent(true);
        ts.testee.scan();

        assertTrue(txt.exists());
        assertFalse(pending.exists());
        assertFalse(deployed.exists());
    }

    /**
     * Tests that non-archive files nested in an exploded deployment are ignored
     */
    @Test
    public void testIgnoreNonArchiveInExplodedDeployment() throws Exception {
        File deployment = new File(tmpDir, "foo.ear");
        deployment.mkdirs();
        File deployed = new File(tmpDir, "foo.ear" + FileSystemDeploymentService.DEPLOYED);
        File pending = new File(tmpDir, "foo.ear" + FileSystemDeploymentService.PENDING);
        File nonarchive = createFile(deployment, "index.html");

        TesteeSet ts = createTestee();
        ts.testee.setAutoDeployExplodedContent(true);

        ts.controller.addCompositeSuccessResponse(1);
        ts.testee.scan();

        assertTrue(deployed.exists());
        assertFalse(pending.exists());
        assertTrue(nonarchive.exists());
    }

    /**
     * Tests that the .skipdeploy marker prevents auto-deploy
     */
    @Test
    public void testSkipDeploy() throws Exception {
        File deployment = new File(tmpDir, "foo.ear");
        deployment.mkdirs();
        File deployed = new File(tmpDir, "foo.ear" + FileSystemDeploymentService.DEPLOYED);
        File pending = new File(tmpDir, "foo.ear" + FileSystemDeploymentService.PENDING);
        File skip = createFile(tmpDir, "foo.ear" + FileSystemDeploymentService.SKIP_DEPLOY);

        TesteeSet ts = createTestee();
        ts.testee.setAutoDeployExplodedContent(true);

        ts.testee.scan();

        assertFalse(deployed.exists());
        assertFalse(pending.exists());

        skip.delete();

        ts.controller.addCompositeSuccessResponse(1);
        ts.testee.scan();

        assertTrue(deployed.exists());
        assertFalse(pending.exists());

    }

    /**
     * Tests that an incompletely copied file with a nested jar
     * *and* extraneous leading bytes fails cleanly.
     */
    @Test
    public void testNonScannableZipped() throws Exception {

        File incomplete = new File(tmpDir, "foo.war");
        File deployed = new File(tmpDir, "foo.war" + FileSystemDeploymentService.DEPLOYED);
        File pending = new File(tmpDir, "foo.war" + FileSystemDeploymentService.PENDING);
        File failed = new File(tmpDir, "foo.war" + FileSystemDeploymentService.FAILED_DEPLOY);
        testSupport.createZip(incomplete, 1, false, true, true, false);
        TesteeSet ts = createTestee();
        ts.testee.setAutoDeployZippedContent(true);

        ts.testee.scan();

        assertFalse(deployed.exists());
        assertTrue(failed.exists());
        assertFalse(pending.exists());
    }

    /**
     * Tests that a ZIP64 format autodeployed file fails cleanly.
     * If ZIP 64 support is added, this test should be revised.
     */
    @Test
    public void testZip64Zipped() throws Exception {

        File zip64 = new File(tmpDir, "foo.war");
        File deployed = new File(tmpDir, "foo.war" + FileSystemDeploymentService.DEPLOYED);
        File pending = new File(tmpDir, "foo.war" + FileSystemDeploymentService.PENDING);
        File failed = new File(tmpDir, "foo.war" + FileSystemDeploymentService.FAILED_DEPLOY);
        testSupport.createZip(zip64, 0, false, false, true, true);
        TesteeSet ts = createTestee();
        ts.testee.setAutoDeployZippedContent(true);

        ts.testee.scan();

        assertFalse(deployed.exists());
        assertTrue(failed.exists());
        assertFalse(pending.exists());
    }

    /**
     * Test for JBAS-9226 -- deleting a .deployed marker does not result in a redeploy
     *
     * @throws Exception
     */
    @Test
    public void testIgnoreUndeployed() throws Exception {

        File war = new File(tmpDir, "foo.war");
        File deployed = new File(tmpDir, "foo.war" + FileSystemDeploymentService.DEPLOYED);
        File undeployed = new File(tmpDir, "foo.war" + FileSystemDeploymentService.UNDEPLOYED);
        testSupport.createZip(war, 0, false, false, false, false);
        TesteeSet ts = createTestee();
        ts.testee.setAutoDeployZippedContent(true);

        ts.controller.addCompositeSuccessResponse(1);
        ts.testee.scan();

        assertTrue(deployed.exists());

        // Undeploy
        deployed.delete();
        ts.controller.addCompositeSuccessResponse(1);
        ts.testee.scan();

        assertTrue(undeployed.exists());
        assertFalse(deployed.exists());

        // Confirm it doesn't come back
        ts.controller.addCompositeSuccessResponse(1);
        ts.testee.scan();

        assertTrue(undeployed.exists());
        assertFalse(deployed.exists());
    }

    @Test
    public void testDeploymentTimeout() throws Exception {
        File deployment = new File(tmpDir, "foo.war");
        File dodeploy = createFile("foo.war" + FileSystemDeploymentService.DO_DEPLOY);
        File deployed = new File(tmpDir, "foo.war" + FileSystemDeploymentService.DEPLOYED);
        File failed = new File(tmpDir, "foo.war" + FileSystemDeploymentService.FAILED_DEPLOY);
        testSupport.createZip(deployment, 0, false, false, true, true);

        TesteeSet ts = createTestee(new DiscardTaskExecutor() {
            @Override
            public <T> Future<T> submit(Callable<T> tCallable) {
                return new TimeOutFuture<T>(5, tCallable);
            }
        });

        ts.testee.setDeploymentTimeout(5);

        ts.testee.scan();

        assertFalse(deployed.exists());
        assertFalse(dodeploy.exists());
        assertTrue(failed.exists());
    }

    @Test
    public void testArchiveRedeployedAfterReboot() throws Exception {

        File war = createFile("foo.war");
        File dodeploy = createFile("foo.war" + FileSystemDeploymentService.DO_DEPLOY);
        File deployed = new File(tmpDir, "foo.war" + FileSystemDeploymentService.DEPLOYED);
        TesteeSet ts = createTestee();
        ts.controller.addCompositeSuccessResponse(1);
        ts.testee.scan();
        assertTrue(war.exists());
        assertFalse(dodeploy.exists());
        assertTrue(deployed.exists());

        // Create a new testee to simulate a reboot
        ts = createTestee();
        ts.controller.addCompositeSuccessResponse(1);
        ts.testee.scan();
        assertTrue(war.exists());
        assertFalse(dodeploy.exists());
        assertTrue(deployed.exists());
    }

    @Test
    public void testExplodedRedeployedAfterReboot() throws Exception {
        final File war = createDirectory("foo.war", "index.html");
        File dodeploy = createFile("foo.war" + FileSystemDeploymentService.DO_DEPLOY);
        File deployed = new File(tmpDir, "foo.war" + FileSystemDeploymentService.DEPLOYED);
        TesteeSet ts = createTestee();
        ts.controller.addCompositeSuccessResponse(1);
        ts.testee.scan();
        assertTrue(war.exists());
        assertFalse(dodeploy.exists());
        assertTrue(deployed.exists());

        // Create a new testee to simulate a reboot
        ts = createTestee();
        ts.controller.addCompositeSuccessResponse(1);
        ts.testee.scan();
        assertTrue(war.exists());
        assertFalse(dodeploy.exists());
        assertTrue(deployed.exists());
    }

    /**
     * Tests that a deployment which had failed earlier, is redeployed (i.e. picked for deployment) when the deployment
     * file is updated (i.e. timestamp changes).
     *
     * @throws Exception
     */
    @Test
    public void testFailedArchiveRedeployedAfterDeploymentUpdate() throws Exception {
        final String warName = "helloworld.war";
        // create our deployment file
        File war = new File(tmpDir, warName);
        testSupport.createZip(war, 0, false, false, false, false);
        // trigger deployment
        TesteeSet ts = createTestee();
        ts.controller.addCompositeFailureResponse(1, 1);
        // set the auto-deploy of zipped content on the scanner
        ts.testee.setAutoDeployZippedContent(true);
        ts.testee.scan();

        // make sure the deployment exists after the scan
        assertTrue(war.getAbsolutePath() + " is missing after deployment", war.exists());

        // failed marker file
        File failed = new File(tmpDir, warName + FileSystemDeploymentService.FAILED_DEPLOY);
        // make sure the failed marker exists
        assertTrue(failed.getAbsolutePath() + " marker file not found after deployment", failed.exists());

        // Now update the timestamp of the deployment war and retrigger a deployment scan expecting it to process
        // the deployment war and create a deployed (a.k.a successful) deployment marker. This simulates the case where the
        // original deployment file is "fixed" and the deployment scanner should pick it up even in the presence of the
        // (previous) failed marker
        long newLastModifiedTime = failed.lastModified() + 1000;
        logger.info("Updating last modified time of war " + war + " from " + war.lastModified() + " to " + newLastModifiedTime + " to simulate changes to the deployment");
        war.setLastModified(newLastModifiedTime);
        ts.controller.addCompositeSuccessResponse(1);
        ts.testee.scan();

        // make sure the deployment exists after the scan
        assertTrue(war.getAbsolutePath() + " is missing after re-deployment", war.exists());
        // make sure the deployed marker exists
        File deployed = createFile(warName + FileSystemDeploymentService.DEPLOYED);
        assertTrue(deployed.getAbsolutePath() + " marker file not found after re-deployment", deployed.exists());
        // make sure the failed marker *no longer exists*
        failed = new File(tmpDir, warName + FileSystemDeploymentService.FAILED_DEPLOY);
        assertFalse(failed.getAbsolutePath() + " marker file (unexpectedly) exists after re-deployment", failed.exists());

    }

    /**
     * Tests that a deployment which had been undeployed earlier, is redeployed (i.e. picked for deployment) when the deployment
     * file is updated (i.e. timestamp changes).
     *
     * @throws Exception
     */
    @Test
    public void testUndeployedArchiveRedeployedAfterDeploymentUpdate() throws Exception {
        final String warName = "helloworld2.war";
        // create our deployment file
        File war = new File(tmpDir, warName);
        testSupport.createZip(war, 0, false, false, false, false);
        // trigger deployment
        TesteeSet ts = createTestee();
        ts.controller.addCompositeSuccessResponse(1);
        // set the auto-deploy of zipped content on the scanner
        ts.testee.setAutoDeployZippedContent(true);
        ts.testee.scan();

        // make sure the deployment exists after the scan
        assertTrue(war.getAbsolutePath() + " is missing after deployment", war.exists());

        // deployed marker file
        File deployed = new File(tmpDir, warName + FileSystemDeploymentService.DEPLOYED);
        // make sure the deployed marker exists
        assertTrue(deployed.getAbsolutePath() + " marker file not found after deployment", deployed.exists());

        // now trigger a undeployment by removed the "deployed" marker file
        assertTrue("Could not delete " + deployed.getAbsolutePath() + " marker file", deployed.delete());
        ts.controller.addCompositeSuccessResponse(1);
        ts.testee.scan();

        // make sure undeployed marker exists
        File undeployed = new File(tmpDir, warName + FileSystemDeploymentService.UNDEPLOYED);
        assertTrue(undeployed.getAbsolutePath() + " marker file not found after undeployment", undeployed.exists());

        // Now update the timestamp of the deployment war and retrigger a deployment scan expecting it to process
        // the deployment war and create a deployed (a.k.a successful) deployment marker. This simulates the case where the
        // original deployment file is redeployed and the deployment scanner should pick it up even in the presence of the
        // (previous) undeployed marker
        long newLastModifiedTime = undeployed.lastModified() + 1000;
        logger.info("Updating last modified time of war " + war + " from " + war.lastModified() + " to " + newLastModifiedTime + " to simulate changes to the deployment");
        war.setLastModified(newLastModifiedTime);
        ts.controller.addCompositeSuccessResponse(1);
        ts.testee.scan();

        // make sure the deployment exists after the scan
        assertTrue(war.getAbsolutePath() + " is missing after re-deployment", war.exists());
        // make sure the deployed marker exists
        deployed = createFile(warName + FileSystemDeploymentService.DEPLOYED);
        assertTrue(deployed.getAbsolutePath() + " marker file not found after re-deployment", deployed.exists());
        // make sure the undeployed marker *no longer exists*
        undeployed = new File(tmpDir, warName + FileSystemDeploymentService.UNDEPLOYED);
        assertFalse(undeployed.getAbsolutePath() + " marker file (unexpectedly) exists after re-deployment", undeployed.exists());

    }

    private TesteeSet createTestee(String... existingContent) throws OperationFailedException {
        return createTestee(new MockServerController(new MockDeploymentRepository(), existingContent));
    }

    private TesteeSet createTestee(final ScheduledExecutorService executorService, final String... existingContent) throws OperationFailedException {
        return createTestee(new MockServerController(new MockDeploymentRepository(), existingContent), executorService);
    }

    private TesteeSet createTestee(final MockServerController sc) throws OperationFailedException {
        return createTestee(sc, executor);
    }

    private TesteeSet createTestee(final MockServerController sc, final ScheduledExecutorService executor) throws OperationFailedException {
        final MockDeploymentRepository repo = new MockDeploymentRepository();
        final FileSystemDeploymentService testee = new FileSystemDeploymentService(null, tmpDir, null, sc, executor, repo, repo);
        testee.startScanner();
        return new TesteeSet(testee, repo, sc);
    }

    private File createFile(String fileName) throws IOException {
        return createFile(tmpDir, fileName);
    }

    private File createFile(File dir, String fileName) throws IOException {

        dir.mkdirs();

        File f = new File(dir, fileName);
        FileOutputStream fos = new FileOutputStream(f);
        try {
            PrintWriter writer = new PrintWriter(fos);
            writer.write(fileName);
            writer.close();
        }
        finally {
            fos.close();
        }
        assertTrue(f.exists());
        return f;
    }

    private File createDirectory(String name, String... children) throws IOException {
        return createDirectory(tmpDir, name, children);
    }

    private File createDirectory(File dir, String name, String... children) throws IOException {

        final File directory = new File(dir, name);
        directory.mkdirs();

        for(final String child : children) {
            createFile(directory, child);
        }

        return directory;
    }

    private static class TesteeSet {
        private final FileSystemDeploymentService testee;
        private final MockDeploymentRepository repo;
        private final MockServerController controller;

        public TesteeSet(FileSystemDeploymentService testee, MockDeploymentRepository repo, MockServerController sc) {
            this.testee = testee;
            this.repo = repo;
            this.controller = sc;
        }
    }

    private static class MockDeploymentRepository implements ServerDeploymentRepository, ContentRepository {

        private Set<byte[]> content = new HashSet<byte[]>(2);

        @Override
        public byte[] addContent(InputStream stream) throws IOException {
            byte[] bytes = new byte[20];
            random.nextBytes(bytes);
            content.add(bytes);
            return bytes;
        }

        @Override
        public VirtualFile getContent(byte[] hash) {
            throw new RuntimeException("NYI: org.jboss.as.server.deployment.scanner.FileSystemDeploymentServiceUnitTestCase.MockDeploymentRepository.getContent");
        }

        @Override
        public boolean hasContent(byte[] hash) {
            return content.contains(hash);
        }

        @Override
        public void removeContent(byte[] hash) {
            throw new RuntimeException("NYI: org.jboss.as.server.deployment.scanner.FileSystemDeploymentServiceUnitTestCase.MockDeploymentRepository.removeContent");
        }

        /** {@inheritDoc} */
        @Override
        public Closeable mountDeploymentContent(VirtualFile contents, VirtualFile mountPoint, boolean mountExploded) throws IOException {
            return new Closeable() {
                @Override
                public void close() throws IOException {
                    //
                }
            };
        }

        @Override
        public void purgeContent(List<byte[]> hashes) {
        }

    }

    private static class MockServerController implements ModelControllerClient {

        private final List<ModelNode> requests = new ArrayList<ModelNode>(1);
        private final List<Response> responses = new ArrayList<Response>(1);
        private final Map<String, byte[]> added = new HashMap<String, byte[]>();
        private final Map<String, byte[]> deployed = new HashMap<String, byte[]>();

        @Override
        public ModelNode execute(ModelNode operation) throws IOException {
            requests.add(operation);
            return processOp(operation);
        }

        @Override
        public ModelNode execute(Operation operation) throws IOException {
            return execute(operation.getOperation());
        }

        @Override
        public ModelNode execute(ModelNode operation, OperationMessageHandler messageHandler) throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public ModelNode execute(Operation operation, OperationMessageHandler messageHandler) throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public AsyncFuture<ModelNode> executeAsync(ModelNode operation, OperationMessageHandler messageHandler) {
            throw new UnsupportedOperationException();
        }

        @Override
        public AsyncFuture<ModelNode> executeAsync(Operation operation, OperationMessageHandler messageHandler) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void close() throws IOException {
            throw new UnsupportedOperationException();
        }

        private static class Response {
            private final boolean ok;
            private final ModelNode rsp;
            Response(boolean ok, ModelNode rsp) {
                this.ok = ok;
                this.rsp = rsp;
            }
        }

        MockServerController(ContentRepository repo, String... existingDeployments) {
            for (String dep : existingDeployments) {
                try {
                    added.put(dep, repo.addContent(null));
                } catch (IOException e) {
                    // impossible
                }
            }
        }

        public void addCompositeSuccessResponse(int count) {
            ModelNode rsp = new ModelNode();
            rsp.get(OUTCOME).set(SUCCESS);
            ModelNode result = rsp.get(RESULT);
            for (int i = 1; i <= count; i++) {
                result.get("step-" + i, OUTCOME).set(SUCCESS);
                result.get("step-" + i, RESULT);
            }

            responses.add(new Response(true, rsp));
        }

        public void addCompositeFailureResponse(int count, int failureStep) {

            if (count < failureStep) {
                throw new IllegalArgumentException("failureStep must be > count");
            }

            ModelNode rsp = new ModelNode();
            rsp.get(OUTCOME).set(FAILED);
            ModelNode result = rsp.get(RESULT);
            for (int i = 1; i <= count; i++) {
                String step = "step-" + i;
                if (i < failureStep) {
                    result.get(step, OUTCOME).set(FAILED);
                    result.get(step, RESULT);
                    result.get(step, ROLLED_BACK).set(true);
                }
                else if (i == failureStep){
                    result.get(step, OUTCOME).set(FAILED);
                    result.get(step, FAILURE_DESCRIPTION).set(new ModelNode().set("badness happened"));
                    result.get(step, ROLLED_BACK).set(true);
                }
                else {
                    result.get(step, OUTCOME).set(CANCELLED);
                }
            }
            rsp.get(FAILURE_DESCRIPTION).set(new ModelNode().set("badness happened"));
            rsp.get(ROLLED_BACK).set(true);

            responses.add(new Response(true, rsp));
        }

        private ModelNode getDeploymentNamesResponse() {
            ModelNode content = new ModelNode();
            content.get(OUTCOME).set(SUCCESS);
            ModelNode result = content.get(RESULT);
            result.setEmptyList();
            for (String deployment : added.keySet()) {
                result.add(deployment);
            }
            return content;
        }

        private ModelNode processOp(ModelNode op) {

            String opName = op.require(OP).asString();
            if (READ_CHILDREN_NAMES_OPERATION.equals(opName)) {
                return getDeploymentNamesResponse();
            }
            else if (COMPOSITE.equals(opName)) {
                for (ModelNode child : op.require(STEPS).asList()) {
                    opName = child.require(OP).asString();
                    if (COMPOSITE.equals(opName)) {
                        return processOp(child);
                    }

                    if (responses.isEmpty()) {
                        Assert.fail("unexpected request " + op);
                        return null; // unreachable
                    }

                    if (!responses.get(0).ok) {
                        // don't change state for a failed response
                        continue;
                    }

                    PathAddress address = PathAddress.pathAddress(child.require(OP_ADDR));
                    if (ADD.equals(opName)) {
                        // Since AS7-431 the content is no longer managed
                        //added.put(address.getLastElement().getValue(), child.require(CONTENT).require(0).require(HASH).asBytes());
                        added.put(address.getLastElement().getValue(), randomHash());
                    }
                    else if (REMOVE.equals(opName)) {
                        added.remove(address.getLastElement().getValue());
                    }
                    else if (DEPLOY.equals(opName)) {
                        String name = address.getLastElement().getValue();
                        deployed.put(name, added.get(name));
                    }
                    else if (UNDEPLOY.equals(opName)) {
                        deployed.remove(address.getLastElement().getValue());
                    }
                    else if (FULL_REPLACE_DEPLOYMENT.equals(opName)) {
                        String name = child.require(NAME).asString();
                        // Since AS7-431 the content is no longer managed
                        //byte[] hash = child.require(CONTENT).require(0).require(HASH).asBytes();
                        final byte[] hash = randomHash();
                        added.put(name, hash);
                        deployed.put(name, hash);
                    }
                    else {
                        throw new IllegalArgumentException("unexpected step " + opName);
                    }
                }
                return responses.remove(0).rsp;
            }
            else {
                throw new IllegalArgumentException("unexpected operation " + opName);
            }
        }

    }

    private static class DiscardTaskExecutor extends ScheduledThreadPoolExecutor {

        private final List<Runnable> tasks = new ArrayList<Runnable>();
        private DiscardTaskExecutor() {
            super(0);
        }

        @Override
        public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
            tasks.add(command);
            return null;
        }

        @Override
        public <T> Future<T> submit(Callable<T> tCallable) {
            return new CallOnGetFuture<T>(tCallable);
        }

        void clear() {
            tasks.clear();
        }
    }

    private static class CallOnGetFuture<T> implements Future<T> {
        final Callable<T> callable;

        private CallOnGetFuture(Callable<T> callable) {
            this.callable = callable;
        }

        @Override
        public boolean cancel(boolean b) {
            return false;
        }

        @Override
        public boolean isCancelled() {
            return false;
        }

        @Override
        public boolean isDone() {
            return false;
        }

        @Override
        public T get() throws InterruptedException, ExecutionException {
            try {
                return callable.call();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public T get(long l, TimeUnit timeUnit) throws InterruptedException, ExecutionException, TimeoutException {
            return get();
        }
    }

    private static class TimeOutFuture<T> implements Future<T> {
        final long expectedTimeout;
        final Callable<T> callable;

        private TimeOutFuture(long expectedTimeout, Callable<T> callable) {
            this.expectedTimeout = expectedTimeout;
            this.callable = callable;
        }

        @Override
        public boolean cancel(boolean b) {
            return false;
        }

        @Override
        public boolean isCancelled() {
            return false;
        }

        @Override
        public boolean isDone() {
            return false;
        }

        @Override
        public T get() throws InterruptedException, ExecutionException {
            try {
                return callable.call();
            } catch (Exception e) {
                // Ignore
                return null;
            }
        }

        @Override
        public T get(long l, TimeUnit timeUnit) throws InterruptedException, ExecutionException, TimeoutException {
            assertEquals( "Should use the configured timeout", expectedTimeout, l);
            throw new TimeoutException();
        }
    }

    private static byte[] randomHash() {
        final byte[] hash = new byte[20];
        random.nextBytes(hash);
        return hash;
    }
}

/**
 *
 */
package org.jboss.as.server.deployment.scanner;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CANCELLED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ROLLED_BACK;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationResult;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.client.ExecutionContext;
import org.jboss.as.server.ServerController;
import org.jboss.as.server.ServerEnvironment;
import org.jboss.as.server.deployment.api.DeploymentRepository;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceRegistry;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests of {@link FileSystemDeploymentService}.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class FileSystemDeploymentServiceUnitTestCase {

    private static long count = System.currentTimeMillis();

    private static final Random random = new Random(System.currentTimeMillis());

    private static final ScheduledThreadPoolExecutor executor = new DiscardTaskExecutor();

    private File tmpDir;

    @Before
    public void setup() throws Exception {
        File root = new File(System.getProperty("java.io.tmpdir"));
        root  = new File(root, getClass().getSimpleName());
        for (int i = 0; i < 100; i++) {
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
        if (tmpDir != null) {
            cleanFile(tmpDir);
            tmpDir = null;
        }
    }

    private static void cleanFile(File file) {
        if (file.exists()) {
            if (file.isDirectory()) {
                for (File child : file.listFiles()) {
                    cleanFile(child);
                }
            }
            if (!file.delete()) {
                file.deleteOnExit();
            }
        }
    }

    @AfterClass
    public static void cleanup() {
        File root = new File(System.getProperty("java.io.tmpdir"));
        root  = new File(root, FileSystemDeploymentServiceUnitTestCase.class.getSimpleName());
        cleanFile(root);
    }

    @Test
    public void testIgnoreNoMarker() throws Exception {
        File f1 = createFile("foo.war");
        TesteeSet ts = createTestee();
        ts.controller.addGetDeploymentNamesResponse();
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
        ts.controller.addGetDeploymentNamesResponse();
        ts.controller.addCompositeSuccessResponse(1);
        ts.testee.scan();
        assertEquals(1, ts.repo.content.size());
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
        ts.controller.addGetDeploymentNamesResponse();
        ts.controller.addCompositeSuccessResponse(1);
        ts.testee.scan();
        assertEquals(1, ts.repo.content.size());
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
        ts.controller.addGetDeploymentNamesResponse();
        ts.controller.addCompositeSuccessResponse(2);
        ts.testee.scan();
        assertEquals(2, ts.repo.content.size());
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
        ts.controller.addGetDeploymentNamesResponse();
        ts.controller.addCompositeFailureResponse(1, 1);
        ts.testee.scan();
        assertEquals(1, ts.repo.content.size());
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
        ts.controller.addGetDeploymentNamesResponse();
        ts.controller.addCompositeFailureResponse(2, 2);
        ts.testee.scan();
        assertEquals(2, ts.repo.content.size());
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
        ts.controller.addGetDeploymentNamesResponse();
        ts.controller.addCompositeFailureResponse(2, 1);
        // Retry fails as well
        ts.controller.addCompositeFailureResponse(1, 1);
        ts.testee.scan();
        assertEquals(2, ts.repo.content.size());
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
        ts.controller.addGetDeploymentNamesResponse();
        ts.controller.addCompositeFailureResponse(2, 1);
        // Retry succeeds
        ts.controller.addCompositeSuccessResponse(1);
        ts.testee.scan();
        assertEquals(2, ts.repo.content.size());
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

        // We expect 2 requests for children names due to subdir "nested"
        MockServerController sc = new MockServerController("ok", "nested-ok");
        sc.responses.add(sc.responses.get(0));
        createTestee(sc);

        Assert.assertFalse(f1.exists());
        Assert.assertFalse(f2.exists());
        Assert.assertTrue(f3.exists());
        Assert.assertTrue(f4.exists());
    }

    @Test
    public void testOverridePreexisting() throws Exception {
        File war = createFile("foo.war");
        File dodeploy = createFile("foo.war" + FileSystemDeploymentService.DO_DEPLOY);
        File deployed = new File(tmpDir, "foo.war" + FileSystemDeploymentService.DEPLOYED);
        TesteeSet ts = createTestee("foo.war");
        ts.controller.addGetDeploymentNamesResponse();
        ts.controller.addCompositeSuccessResponse(1);
        ts.testee.scan();
        assertEquals(1, ts.repo.content.size());
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
        ts.controller.addGetDeploymentNamesResponse();
        ts.controller.addCompositeSuccessResponse(1);
        ts.testee.scan();
        assertEquals(1, ts.repo.content.size());
        assertTrue(war.exists());
        assertFalse(dodeploy.exists());
        assertTrue(deployed.exists());

        dodeploy = createFile("foo.war" + FileSystemDeploymentService.DO_DEPLOY);
        ts.controller.addGetDeploymentNamesResponse();
        ts.controller.addCompositeSuccessResponse(1);
        ts.testee.scan();
        assertEquals(2, ts.repo.content.size());
        assertTrue(war.exists());
        assertFalse(dodeploy.exists());
        assertTrue(deployed.exists());
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
        ts.controller.addGetDeploymentNamesResponse();
        ts.controller.addCompositeSuccessResponse(2);
        ts.testee.scan();
        assertEquals(2, ts.repo.content.size());
        assertTrue(war1.exists());
        assertFalse(dodeploy1.exists());
        assertTrue(deployed1.exists());
        assertTrue(war2.exists());
        assertFalse(dodeploy2.exists());
        assertTrue(deployed2.exists());

        dodeploy1 = createFile("foo.war" + FileSystemDeploymentService.DO_DEPLOY);
        dodeploy2 = createFile("bar.war" + FileSystemDeploymentService.DO_DEPLOY);
        ts.controller.addGetDeploymentNamesResponse();
        ts.controller.addCompositeSuccessResponse(2);
        ts.testee.scan();
        assertEquals(4, ts.repo.content.size());
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
        ts.controller.addGetDeploymentNamesResponse();
        ts.controller.addCompositeSuccessResponse(1);
        ts.testee.scan();
        assertEquals(1, ts.repo.content.size());
        assertTrue(war.exists());
        assertFalse(dodeploy.exists());
        assertTrue(deployed.exists());
        assertFalse(failed.exists());

        dodeploy = createFile("foo.war" + FileSystemDeploymentService.DO_DEPLOY);
        ts.controller.addGetDeploymentNamesResponse();
        ts.controller.addCompositeFailureResponse(1, 1);
        ts.testee.scan();
        assertEquals(2, ts.repo.content.size());
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
        ts.controller.addGetDeploymentNamesResponse();
        ts.controller.addCompositeSuccessResponse(2);
        ts.testee.scan();
        assertEquals(2, ts.repo.content.size());
        assertTrue(war1.exists());
        assertFalse(dodeploy1.exists());
        assertTrue(deployed1.exists());
        assertTrue(war2.exists());
        assertFalse(dodeploy2.exists());
        assertTrue(deployed2.exists());

        dodeploy1 = createFile("foo.war" + FileSystemDeploymentService.DO_DEPLOY);
        dodeploy2 = createFile("bar.war" + FileSystemDeploymentService.DO_DEPLOY);
        ts.controller.addGetDeploymentNamesResponse();
        ts.controller.addCompositeFailureResponse(2, 1);
        // Retry fails as well
        ts.controller.addCompositeFailureResponse(1, 1);
        ts.testee.scan();
        assertEquals(4, ts.repo.content.size());
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
        ts.controller.addGetDeploymentNamesResponse();
        ts.controller.addCompositeSuccessResponse(2);
        ts.testee.scan();
        assertEquals(2, ts.repo.content.size());
        assertTrue(war1.exists());
        assertFalse(dodeploy1.exists());
        assertTrue(deployed1.exists());
        assertTrue(war2.exists());
        assertFalse(dodeploy2.exists());
        assertTrue(deployed2.exists());

        dodeploy1 = createFile("foo.war" + FileSystemDeploymentService.DO_DEPLOY);
        dodeploy2 = createFile("bar.war" + FileSystemDeploymentService.DO_DEPLOY);
        ts.controller.addGetDeploymentNamesResponse();
        ts.controller.addCompositeFailureResponse(2, 1);
        // Retry succeeds
        ts.controller.addCompositeSuccessResponse(1);
        ts.testee.scan();
        assertEquals(4, ts.repo.content.size());
        assertTrue(war1.exists());
        assertTrue(war2.exists());
        assertFalse(dodeploy1.exists());
        assertFalse(dodeploy2.exists());
        assertFalse(deployed1.exists() && deployed2.exists());
        assertTrue(failed1.exists() || failed2.exists());
    }

    @Test
    public void testUndeploy() throws Exception {
        File war = createFile("foo.war");
        File dodeploy = createFile("foo.war" + FileSystemDeploymentService.DO_DEPLOY);
        File deployed = new File(tmpDir, "foo.war" + FileSystemDeploymentService.DEPLOYED);
        TesteeSet ts = createTestee("foo.war");
        ts.controller.addGetDeploymentNamesResponse();
        ts.controller.addCompositeSuccessResponse(1);
        ts.testee.scan();
        assertEquals(1, ts.repo.content.size());
        assertTrue(war.exists());
        assertFalse(dodeploy.exists());
        assertTrue(deployed.exists());

        assertTrue(deployed.delete());
        ts.controller.addGetDeploymentNamesResponse();
        ts.controller.addCompositeSuccessResponse(1);
        ts.testee.scan();
        assertEquals(1, ts.repo.content.size());
        assertTrue(war.exists());
        assertFalse(dodeploy.exists());
        assertFalse(deployed.exists());
    }

    private TesteeSet createTestee(String... existingContent) throws OperationFailedException {
        return createTestee(new MockServerController(existingContent));
    }

    private TesteeSet createTestee(final MockServerController sc) throws OperationFailedException {
        final MockDeploymentRepository repo = new MockDeploymentRepository();
        final FileSystemDeploymentService testee = new FileSystemDeploymentService(tmpDir, 0, sc, executor, repo);
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

    private static class MockDeploymentRepository implements DeploymentRepository {

        private Set<byte[]> content = new HashSet<byte[]>(2);

        @Override
        public byte[] addDeploymentContent(String name, String runtimeName, InputStream stream) throws IOException {
            byte[] bytes = new byte[20];
            random.nextBytes(bytes);
            content.add(bytes);
            return bytes;
        }

        @Override
        public boolean hasDeploymentContent(byte[] hash) {
            return content.contains(hash);
        }

    }

    private static class MockServerController implements ServerController {

        private final List<ModelNode> requests = new ArrayList<ModelNode>(1);
        private final List<ModelNode> responses = new ArrayList<ModelNode>(1);

        MockServerController(String... existingDeployments) {
            addGetDeploymentNamesResponse(existingDeployments);
        }

        public void addCompositeSuccessResponse(int count) {
            ModelNode rsp = new ModelNode();
            rsp.get(OUTCOME).set(SUCCESS);
            ModelNode result = rsp.get(RESULT);
            for (int i = 1; i <= count; i++) {
                result.get("step-" + i, OUTCOME).set(SUCCESS);
                result.get("step-" + i, RESULT);
            }

            addResponse(rsp);
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

            addResponse(rsp);
        }

        void addResponse(ModelNode response) {
            responses.add(response);
        }

        void addGetDeploymentNamesResponse(String... deployments) {
            ModelNode content = new ModelNode();
            content.get(OUTCOME).set(SUCCESS);
            ModelNode result = content.get(RESULT);
            result.setEmptyList();
            for (String deployment : deployments) {
                result.add(deployment);
            }
            addResponse(content);
        }

        @Override
        public OperationResult execute(ExecutionContext executionContext, ResultHandler handler) {
            throw new UnsupportedOperationException("not supported");
        }

        @Override
        public ModelNode execute(ExecutionContext executionContext) throws CancellationException {
            if (responses.isEmpty()) {
                Assert.fail("unexpected request " + executionContext.getOperation());
                return null; // unreachable
            }
            requests.add(executionContext.getOperation());
            return responses.remove(0);
        }

        @Override
        public ServerEnvironment getServerEnvironment() {
            throw new UnsupportedOperationException("not supported");
        }

        @Override
        public ServiceRegistry getServiceRegistry() {
            throw new UnsupportedOperationException("not supported");
        }

        @Override
        public State getState() {
            throw new UnsupportedOperationException("not supported");
        }

    }

    private static class DiscardTaskExecutor extends ScheduledThreadPoolExecutor {

        private DiscardTaskExecutor() {
            super(0);
        }

        @Override
        public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
            return null;
        }


    }
}

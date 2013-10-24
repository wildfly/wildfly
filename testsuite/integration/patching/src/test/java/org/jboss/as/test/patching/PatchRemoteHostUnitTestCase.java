package org.jboss.as.test.patching;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CHILD_TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_CHILDREN_NAMES_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESTART;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SHUTDOWN;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;
import static org.jboss.as.patching.IoUtils.mkdir;
import static org.jboss.as.test.patching.PatchingTestUtil.AS_VERSION;
import static org.jboss.as.test.patching.PatchingTestUtil.PRODUCT;
import static org.jboss.as.test.patching.PatchingTestUtil.createPatchXMLFile;
import static org.jboss.as.test.patching.PatchingTestUtil.createZippedPatchFile;
import static org.jboss.as.test.patching.PatchingTestUtil.randomString;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.Operation;
import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.as.patching.IoUtils;
import org.jboss.as.patching.metadata.ContentModification;
import org.jboss.as.patching.metadata.Patch;
import org.jboss.as.patching.metadata.PatchBuilder;
import org.jboss.as.process.protocol.StreamUtils;
import org.jboss.as.test.integration.domain.management.util.DomainLifecycleUtil;
import org.jboss.as.test.integration.domain.management.util.DomainTestSupport;
import org.jboss.as.version.ProductConfig;
import org.jboss.dmr.ModelNode;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author Emanuel Muckenhuber
 */
public class PatchRemoteHostUnitTestCase {

    private static final ModelNode SLAVE_ADDR = new ModelNode();
    private static final ModelNode PATCH_ADDR = new ModelNode();

    private static DomainTestSupport testSupport;
    private static DomainLifecycleUtil domainMasterLifecycleUtil;
    private static DomainLifecycleUtil domainSlaveLifecycleUtil;
    private static File tempDir;

    static {
        // (host=slave)
        SLAVE_ADDR.add("host", "slave");
        SLAVE_ADDR.protect();

        // (host=slave),(core-services=patching)
        PATCH_ADDR.add("host", "slave");
        PATCH_ADDR.add("core-service", "patching");
        PATCH_ADDR.protect();
    }

    @BeforeClass
    public static void setupDomain() throws Exception {
        tempDir = mkdir(new File(System.getProperty("java.io.tmpdir")), randomString());
        testSupport = DomainTestSupport.createAndStartDefaultSupport(PatchRemoteHostUnitTestCase.class.getSimpleName());

        domainMasterLifecycleUtil = testSupport.getDomainMasterLifecycleUtil();
        domainSlaveLifecycleUtil = testSupport.getDomainSlaveLifecycleUtil();
    }

    @AfterClass
    public static void tearDownDomain() throws Exception {
        testSupport.stop();

        if (IoUtils.recursiveDelete(tempDir)) {
            tempDir.deleteOnExit();
        }

        testSupport = null;
        domainMasterLifecycleUtil = null;
        domainSlaveLifecycleUtil = null;
    }


    @Test
    public void test() throws Exception {

        final ModelControllerClient client = domainMasterLifecycleUtil.getDomainClient();

        final ModelNode patchOp = new ModelNode();
        patchOp.get(OP).set("patch");
        patchOp.get(OP_ADDR).set(PATCH_ADDR);

        final String patchID = "simple-domain-patch";
        final File patch = createPatch(patchID);
        final Operation op = OperationBuilder.create(patchOp)
                .addFileAsAttachment(patch).build();
        try {
            final ModelNode result = client.execute(op);
            validateResponse(result);
        } finally {
            StreamUtils.safeClose(op);
        }

        // Restart the slave
        restartSlave(client);

        final ModelNode patchesOp = new ModelNode();
        patchesOp.get(OP).set(READ_ATTRIBUTE_OPERATION);
        patchesOp.get(OP_ADDR).set(PATCH_ADDR);
        patchesOp.get(NAME).set("patches");

        // Check the applied patch
        final ModelNode entry = new ModelNode().set(patchID);
        Assert.assertTrue(executeForResult(client, patchesOp).asList().contains(entry));

        // Rollback
        final ModelNode rollback = new ModelNode();
        rollback.get(OP).set("rollback");
        rollback.get(OP_ADDR).set(PATCH_ADDR);
        rollback.get("patch-id").set(patchID);
        rollback.get("reset-configuration").set(false);
        executeForResult(client, rollback);
        // Restart
        restartSlave(client);
        // Check there is no patch applied
        Assert.assertTrue(executeForResult(client, patchesOp).asList().isEmpty());

    }

    File createPatch(String patchID) throws Exception {
        final String fileContent = "Hello World!";
        File oneOffPatchDir = mkdir(tempDir, patchID);
        ContentModification miscFileAdded = ContentModificationUtils.addMisc(oneOffPatchDir, patchID,
                fileContent, "awesomeDirectory", "awesomeFile");
        ProductConfig productConfig = new ProductConfig(PRODUCT, AS_VERSION, "main");
        Patch oneOffPatch = PatchBuilder.create()
                .setPatchId(patchID)
                .setDescription("A one-off patch adding a misc file.")
                .oneOffPatchIdentity(productConfig.getProductName(), productConfig.getProductVersion())
                .getParent()
                .addContentModification(miscFileAdded)
                .build();
        createPatchXMLFile(oneOffPatchDir, oneOffPatch);
        return createZippedPatchFile(oneOffPatchDir, patchID);
    }

    void restartSlave(final ModelControllerClient client) throws Exception {
        final ModelNode restart = new ModelNode();
        restart.get(OP).set(SHUTDOWN);
        restart.get(OP_ADDR).set(SLAVE_ADDR);
        restart.get(RESTART).set(true);

        // Shutdown
        executeForResult(client, restart);
        Thread.sleep(150);
        // Wait for the process to finish
        for (;;) {
            try {
                domainSlaveLifecycleUtil.getProcessExitCode();
                break;
            } catch (Exception e) {
                Thread.sleep(1000);
            }
        }
        domainSlaveLifecycleUtil.start();
        waitForHost(client, "slave");
    }

    private ModelNode executeForResult(final ModelControllerClient client, final ModelNode operation) throws IOException {
        final ModelNode result = client.execute(operation);
        return validateResponse(result);
    }

    private ModelNode validateResponse(ModelNode response) {
        return validateResponse(response, true);
    }

    private ModelNode validateResponse(ModelNode response, boolean validateResult) {

        if(! SUCCESS.equals(response.get(OUTCOME).asString())) {
            System.out.println("Failed response:");
            System.out.println(response);
            Assert.fail(response.get(FAILURE_DESCRIPTION).toString());
        }

        if (validateResult) {
            Assert.assertTrue("result exists", response.has(RESULT));
        }
        return response.get(RESULT);
    }

    private static void waitForHost(final ModelControllerClient client, final String hostName) throws Exception {
        final ModelNode operation = new ModelNode();
        operation.get(OP).set(READ_CHILDREN_NAMES_OPERATION);
        operation.get(OP_ADDR).setEmptyList();
        operation.get(CHILD_TYPE).set(HOST);
        final ModelNode host = new ModelNode().set(hostName);
        final long timeout = 30L;
        final TimeUnit timeUnit = TimeUnit.SECONDS;
        final long deadline = System.currentTimeMillis() + timeUnit.toMillis(timeout);
        for(;;) {
            final long remaining = deadline - System.currentTimeMillis();
            final ModelNode result = client.execute(operation);
            if(result.get(RESULT).asList().contains(host)) {
                return;
            }
            if(remaining <= 0) {
                Assert.fail(hostName + " did not register within 30 seconds");
            }
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                Assert.fail("Interrupted while waiting for registration of host " + hostName);
            }
        }
    }

}

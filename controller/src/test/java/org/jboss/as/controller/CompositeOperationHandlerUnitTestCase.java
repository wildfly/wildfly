/**
 *
 */
package org.jboss.as.controller;

import static junit.framework.Assert.assertEquals;
import static org.jboss.as.controller.ModelControllerImplUnitTestCase.getOperation;
import static org.jboss.as.controller.ModelControllerImplUnitTestCase.useNonRecursive;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_REQUIRES_RELOAD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_REQUIRES_RESTART;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROCESS_STATE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESPONSE_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ROLLBACK_ON_RUNTIME_FAILURE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RUNTIME_UPDATE_SKIPPED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.jboss.as.controller.operations.common.Util;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Unit tests of composite operation handling.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
//@Ignore("Composite ops not working yet")
public class CompositeOperationHandlerUnitTestCase {

    private ServiceContainer container;
    private ModelController controller;
    private AtomicBoolean sharedState;

    @Before
    public void setupController() throws InterruptedException {
        System.out.println("=========  New Test \n");

        // restore default
        useNonRecursive = false;

        container = ServiceContainer.Factory.create("test");
        ServiceTarget target = container.subTarget();
        ControlledProcessState processState = new ControlledProcessState(true);
        ModelControllerImplUnitTestCase.ModelControllerService svc = new ModelControllerImplUnitTestCase.ModelControllerService(processState);
        ServiceBuilder<ModelController> builder = target.addService(ServiceName.of("ModelController"), svc);
        builder.install();
        sharedState = svc.state;
        svc.latch.await();
        controller = svc.getValue();
        ModelNode setup = Util.getEmptyOperation("setup", new ModelNode());
        controller.execute(setup, null, null, null);

        assertEquals(ControlledProcessState.State.RUNNING, svc.processState.getState());
    }

    @After
    public void shutdownServiceContainer() {
        if (container != null) {
            container.shutdown();
            try {
                container.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            finally {
                container = null;
            }
        }
        System.out.println("======================");
    }

    @Test
    public void testModelStageGood() throws Exception {
        ModelNode step1 = getOperation("good", "attr1", 2);
        ModelNode step2 = getOperation("good", "attr2", 1);
        ModelNode result = controller.execute(getCompositeOperation(null, step1, step2), null, null, null);
        System.out.println(result);
        assertEquals("success", result.get("outcome").asString());
        assertEquals(2, result.get("result").asInt());
        assertEquals("success", result.get("result", "step-1", "outcome").asString());
        assertEquals("success", result.get("result", "step-2", "outcome").asString());
        assertEquals(1, result.get("result", "step-1", "result").asInt());
        assertEquals(2, result.get("result", "step-2", "result").asInt());

        assertEquals(2, controller.execute(getOperation("good", "attr1", 3), null, null, null).get("result").asInt());
        assertEquals(1, controller.execute(getOperation("good", "attr2", 3), null, null, null).get("result").asInt());
    }

    @Test
    public void testModelStageGoodNonRecursive() throws Exception {
        useNonRecursive = true;
        testModelStageGood();
    }

    @Test
    public void testModelStageFailure() throws Exception {
        ModelNode step1 = getOperation("good", "attr1", 2);
        ModelNode step2 = getOperation("bad", "attr2", 1);
        ModelNode result = controller.execute(getCompositeOperation(null, step1, step2), null, null, null);
        System.out.println(result);
        assertEquals(FAILED, result.get(OUTCOME).asString());
        assertTrue(result.get(FAILURE_DESCRIPTION).toString().contains("this request is bad"));


        assertEquals(1, controller.execute(getOperation("good", "attr1", 3), null, null, null).get("result").asInt());
        assertEquals(2, controller.execute(getOperation("good", "attr2", 3), null, null, null).get("result").asInt());
    }

    @Test
    public void testModelStageFailureNonRecursive() throws Exception {
        useNonRecursive = true;
        testModelStageFailure();
    }

    @Test
    public void testModelStageUnhandledFailure() throws Exception {
        ModelNode step1 = getOperation("good", "attr1", 2);
        ModelNode step2 = getOperation("evil", "attr2", 1);
        ModelNode result = controller.execute(getCompositeOperation(null, step1, step2), null, null, null);
        assertEquals(FAILED, result.get(OUTCOME).asString());

        final String description = result.get("failure-description").toString();
        assertTrue(description.contains("this handler is evil"));
        assertTrue(description.contains(" and was rolled back."));

        assertEquals(1, controller.execute(getOperation("good", "attr1", 3), null, null, null).get("result").asInt());
        assertEquals(2, controller.execute(getOperation("good", "attr2", 3), null, null, null).get("result").asInt());
    }

    @Test
    public void testModelStageUnhandledFailureNonRecursive() throws Exception {
        useNonRecursive = true;
        testModelStageUnhandledFailure();
    }

    @Test
    public void testModelStageFailureNoRollback() throws Exception {
        ModelNode step1 = getOperation("good", "attr1", 2);
        ModelNode step2 = getOperation("bad", "attr2", 1);
        ModelNode op = getCompositeOperation(null, step1, step2);
        op.get(OPERATION_HEADERS, ROLLBACK_ON_RUNTIME_FAILURE).set(false);
        ModelNode result = controller.execute(op, null, null, null);
        System.out.println(result);
        // Model stage failure should result in rollback regardless of the header
        assertEquals(FAILED, result.get(OUTCOME).asString());
        assertTrue(result.get(FAILURE_DESCRIPTION).toString().contains("this request is bad"));


        assertEquals(1, controller.execute(getOperation("good", "attr1", 3), null, null, null).get("result").asInt());
        assertEquals(2, controller.execute(getOperation("good", "attr2", 3), null, null, null).get("result").asInt());
    }

    @Test
    public void testModelStageFailureNoRollbackNonRecursive() throws Exception {
        useNonRecursive = true;
        testModelStageFailureNoRollback();
    }

    @Test
    public void testModelStageUnhandledFailureNoRollback() throws Exception {
        ModelNode step1 = getOperation("good", "attr1", 2);
        ModelNode step2 = getOperation("evil", "attr2", 1);
        ModelNode op = getCompositeOperation(null, step1, step2);
        op.get(OPERATION_HEADERS, ROLLBACK_ON_RUNTIME_FAILURE).set(false);
        ModelNode result = controller.execute(op, null, null, null);
        System.out.println(result);
        // Model stage failure should result in rollback regardless of the header
        assertEquals(FAILED, result.get(OUTCOME).asString());

        final String description = result.get("failure-description").toString();
        assertTrue(description.contains("this handler is evil"));
        assertTrue(description.contains(" and was rolled back."));

        assertEquals(1, controller.execute(getOperation("good", "attr1", 3), null, null, null).get("result").asInt());
        assertEquals(2, controller.execute(getOperation("good", "attr2", 3), null, null, null).get("result").asInt());
    }

    @Test
    public void testModelStageUnhandledFailureNoRollbackNonRecursive() throws Exception {
        useNonRecursive = true;
        testModelStageUnhandledFailureNoRollback();
    }

    @Test
    public void testRuntimeStageFailed() throws Exception {
        ModelNode step1 = getOperation("good", "attr1", 2, null, false);
        ModelNode step2 = getOperation("handleFailed", "attr2", 1, null, false);
        ModelNode result = controller.execute(getCompositeOperation(null, step1, step2), null, null, null);
        Assert.assertEquals(FAILED, result.get(OUTCOME).asString());
        final String description = result.get("failure-description").toString();
        assertTrue(description.contains("handleFailed"));
        assertTrue(description.contains(" and was rolled back."));

        assertTrue(sharedState.get());

        Assert.assertEquals(1, controller.execute(getOperation("good", "attr1", 3), null, null, null).get("result").asInt());
        Assert.assertEquals(2, controller.execute(getOperation("good", "attr2", 3), null, null, null).get("result").asInt());
    }

    @Test
    public void testRuntimeStageFailedNonRecursive() throws Exception {
        useNonRecursive = true;
        testRuntimeStageFailed();
    }

    @Test
    public void testRuntimeStageFailedNoRollback() throws Exception {
        ModelNode step1 = getOperation("good", "attr1", 2);
        ModelNode step2 = getOperation("handleFailed", "attr2", 1);
        ModelNode op = getCompositeOperation(null, step1, step2);
        op.get(OPERATION_HEADERS, ROLLBACK_ON_RUNTIME_FAILURE).set(false);
        ModelNode result = controller.execute(op, null, null, null);
        Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());

        assertFalse(sharedState.get());

        Assert.assertEquals(2, controller.execute(getOperation("good", "attr1", 3), null, null, null).get("result").asInt());
        Assert.assertEquals(1, controller.execute(getOperation("good", "attr2", 3), null, null, null).get("result").asInt());
    }

    @Test
    public void testRuntimeStageFailedNoRollbackNonRecursive() throws Exception {
        useNonRecursive = true;
        testRuntimeStageFailedNoRollback();
    }

    @Test
    public void testRuntimeStageOFENoRollback() throws Exception {
        ModelNode step1 = getOperation("good", "attr1", 2);
        ModelNode step2 = getOperation("operationFailedException", "attr2", 1);
        ModelNode op = getCompositeOperation(null, step1, step2);
        op.get(OPERATION_HEADERS, ROLLBACK_ON_RUNTIME_FAILURE).set(false);
        ModelNode result = controller.execute(op, null, null, null);
        System.out.println(result);
        Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());

        Assert.assertEquals(2, controller.execute(getOperation("good", "attr1", 3), null, null, null).get("result").asInt());
        Assert.assertEquals(1, controller.execute(getOperation("good", "attr2", 3), null, null, null).get("result").asInt());
    }

    @Test
    public void testRuntimeStageOFENoRollbackNonRecursive() throws Exception {
        useNonRecursive = true;
        testRuntimeStageOFENoRollback();
    }

    @Test
    public void testRuntimeStageUnhandledFailureNoRollback() throws Exception {
        ModelNode step1 = getOperation("good", "attr1", 2);
        ModelNode step2 = getOperation("runtimeException", "attr2", 1);
        ModelNode op = getCompositeOperation(null, step1, step2);
        op.get(OPERATION_HEADERS, ROLLBACK_ON_RUNTIME_FAILURE).set(false);
        ModelNode result = controller.execute(op, null, null, null);
        System.out.println(result);
        Assert.assertEquals(FAILED, result.get(OUTCOME).asString());
        final String description = result.get("failure-description").toString();
        assertTrue(description.contains("runtime exception"));
        assertTrue(description.contains(" and was rolled back."));

        assertFalse(sharedState.get());

        Assert.assertEquals(1, controller.execute(getOperation("good", "attr1", 3), null, null, null).get("result").asInt());
        Assert.assertEquals(2, controller.execute(getOperation("good", "attr2", 3), null, null, null).get("result").asInt());
    }

    @Test
    public void testRuntimeStageUnhandledFailureNoRollbackNonRecursive() throws Exception {
        useNonRecursive = true;
        testRuntimeStageUnhandledFailureNoRollback();
    }

    @Test
    public void testModelStageGoodNestedComposite() throws Exception {
        ModelNode step1 = getOperation("good", "attr1", 2, null, false);
        ModelNode step2 = getOperation("good", "attr2", 1, null, false);
        ModelNode comp1 = getCompositeOperation(null, step1, step2);
        ModelNode step3 = getOperation("good", "attr1", 20, null, false);
        ModelNode step4 = getOperation("good", "attr2", 10, null, false);
        ModelNode comp2 = getCompositeOperation(null, step3, step4);
        ModelNode op = getCompositeOperation(null, comp1, comp2);
//        System.out.println(op);
        ModelNode result = controller.execute(op, null, null, null);
//        System.out.println(result);
        Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());
        assertTrue(result.hasDefined(RESULT));
        assertTrue(result.get(RESULT).hasDefined("step-1"));
        assertTrue(result.get(RESULT, "step-1").hasDefined(OUTCOME));
        Assert.assertEquals(SUCCESS, result.get(RESULT, "step-1", OUTCOME).asString());
        assertTrue(result.get(RESULT).hasDefined("step-2"));
        assertTrue(result.get(RESULT, "step-2").hasDefined(OUTCOME));
        Assert.assertEquals(SUCCESS, result.get(RESULT, "step-2", OUTCOME).asString());

        Assert.assertEquals(20, controller.execute(getOperation("good", "attr1", 3), null, null, null).get(RESULT).asInt());
        Assert.assertEquals(10, controller.execute(getOperation("good", "attr2", 3), null, null, null).get(RESULT).asInt());
    }

    @Test
    public void testModelStageGoodNestedCompositeNonRecursive() throws Exception {
        useNonRecursive = true;
        testModelStageGoodNestedComposite();
    }

    @Test
    public void testModelStageFailedNestedComposite() throws Exception {
        ModelNode step1 = getOperation("good", "attr1", 2);
        ModelNode step2 = getOperation("good", "attr2", 1);
        ModelNode comp1 = getCompositeOperation(null, step1, step2);
        ModelNode step3 = getOperation("good", "attr1", 20);
        ModelNode step4 = getOperation("bad", "attr2", 10);
        ModelNode comp2 = getCompositeOperation(null, step3, step4);
        ModelNode op = getCompositeOperation(null, comp1, comp2);
//        System.out.println(op);
        ModelNode result = controller.execute(op, null, null, null);
//        System.out.println(result);
        Assert.assertEquals("failed", result.get("outcome").asString());

        Assert.assertEquals(1, controller.execute(getOperation("good", "attr1", 3), null, null, null).get("result").asInt());
        Assert.assertEquals(2, controller.execute(getOperation("good", "attr2", 3), null, null, null).get("result").asInt());
    }

    @Test
    public void testModelStageFailedNestedCompositeNonRecursive() throws Exception {
        useNonRecursive = true;
        testModelStageFailedNestedComposite();
    }

    @Test
    public void testGoodServiceComposite() throws Exception {

        assertTrue(sharedState.get());
        ModelNode step1 = getOperation("good", "attr1", 2);
        ModelNode step2 = getOperation("good-service", "attr2", 1);
        ModelNode result = controller.execute(getCompositeOperation(null, step1, step2), null, null, null);
//        System.out.println(result);
        Assert.assertEquals("success", result.get("outcome").asString());
        Assert.assertEquals(2, result.get("result").asInt());
        Assert.assertEquals("success", result.get("result", "step-1", "outcome").asString());
        Assert.assertEquals("success", result.get("result", "step-2", "outcome").asString());
        Assert.assertEquals(1, result.get("result", "step-1", "result").asInt());
        Assert.assertEquals(2, result.get("result", "step-2", "result").asInt());

        ServiceController<?> sc = container.getService(ServiceName.JBOSS.append("good-service"));
        assertNotNull(sc);
        Assert.assertEquals(ServiceController.State.UP, sc.getState());

        Assert.assertEquals(2, controller.execute(getOperation("good", "attr1", 3), null, null, null).get("result").asInt());
        Assert.assertEquals(1, controller.execute(getOperation("good", "attr2", 3), null, null, null).get("result").asInt());
    }

    @Test
    public void testGoodServiceCompositeNonRecursive() throws Exception {
        useNonRecursive = true;
        testGoodServiceComposite();
    }

    @Test
    public void testGoodServiceNestedComposite() throws Exception {
        ModelNode step1 = getOperation("good", "attr1", 2);
        ModelNode step2 = getOperation("good", "attr2", 1);
        ModelNode comp1 = getCompositeOperation(null, step1, step2);
        ModelNode step3 = getOperation("good", "attr1", 20);
        ModelNode step4 = getOperation("good-service", "attr2", 10);
        ModelNode comp2 = getCompositeOperation(null, step3, step4);
        ModelNode op = getCompositeOperation(null, comp1, comp2);
//        System.out.println(op);
        ModelNode result = controller.execute(op, null, null, null);
//        System.out.println(result);
        Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());
        assertTrue(result.hasDefined(RESULT));
        assertTrue(result.get(RESULT).hasDefined("step-1"));
        assertTrue(result.get(RESULT, "step-1").hasDefined(OUTCOME));
        Assert.assertEquals(SUCCESS, result.get(RESULT, "step-1", OUTCOME).asString());
        assertTrue(result.get(RESULT).hasDefined("step-2"));
        assertTrue(result.get(RESULT, "step-2").hasDefined(OUTCOME));
        Assert.assertEquals(SUCCESS, result.get(RESULT, "step-2", OUTCOME).asString());

        Assert.assertEquals(20, controller.execute(getOperation("good", "attr1", 3), null, null, null).get(RESULT).asInt());
        Assert.assertEquals(10, controller.execute(getOperation("good", "attr2", 3), null, null, null).get(RESULT).asInt());

        ServiceController<?> sc = container.getService(ServiceName.JBOSS.append("good-service"));
        assertNotNull(sc);
        Assert.assertEquals(ServiceController.State.UP, sc.getState());
    }

    @Test
    public void testGoodServiceNestedCompositeNonRecursive() throws Exception {
        useNonRecursive = true;
        testGoodServiceNestedComposite();
    }

    @Test
    public void testBadService() throws Exception {
        ModelNode step1 = getOperation("good", "attr1", 2);
        ModelNode step2 = getOperation("bad-service", "attr2", 1);
        ModelNode result = controller.execute(getCompositeOperation(null, step1, step2), null, null, null);
//        System.out.println(result);
        Assert.assertEquals(FAILED, result.get(OUTCOME).asString());
        assertTrue(result.hasDefined(FAILURE_DESCRIPTION));

        assertTrue(sharedState.get());

        ServiceController<?> sc = container.getService(ServiceName.JBOSS.append("bad-service"));
        if (sc != null) {
            Assert.assertEquals(ServiceController.Mode.REMOVE, sc.getMode());
        }

        Assert.assertEquals(1, controller.execute(getOperation("good", "attr1", 3), null, null, null).get("result").asInt());
        Assert.assertEquals(2, controller.execute(getOperation("good", "attr2", 3), null, null, null).get("result").asInt());
    }

    @Test
    public void testBadServiceNonRecursive() throws Exception {
        useNonRecursive = true;
        testBadService();
    }

    @Test
    public void testMissingService() throws Exception {
        ModelNode step1 = getOperation("good", "attr1", 2);
        ModelNode step2 = getOperation("missing-service", "attr2", 1);
        ModelNode result = controller.execute(getCompositeOperation(null, step1, step2), null, null, null);
//        System.out.println(result);
        Assert.assertEquals(FAILED, result.get(OUTCOME).asString());
        assertTrue(result.hasDefined(FAILURE_DESCRIPTION));

        assertTrue(sharedState.get());

        ServiceController<?> sc = container.getService(ServiceName.JBOSS.append("missing-service"));
        if (sc != null) {
            Assert.assertEquals(ServiceController.Mode.REMOVE, sc.getMode());
        }

        Assert.assertEquals(1, controller.execute(getOperation("good", "attr1", 3), null, null, null).get("result").asInt());
        Assert.assertEquals(2, controller.execute(getOperation("good", "attr2", 3), null, null, null).get("result").asInt());
    }

    @Test
    public void testMissingServiceNonRecursive() throws Exception {
        useNonRecursive = true;
        testMissingService();
    }

    @Test
    public void testBadServiceNestedComposite() throws Exception {
        ModelNode step1 = getOperation("good", "attr1", 2);
        ModelNode step2 = getOperation("good", "attr2", 1);
        ModelNode comp1 = getCompositeOperation(null, step1, step2);
        ModelNode step3 = getOperation("good", "attr1", 20);
        ModelNode step4 = getOperation("bad-service", "attr2", 10);
        ModelNode comp2 = getCompositeOperation(null, step3, step4);
        ModelNode op = getCompositeOperation(null, comp1, comp2);
//        System.out.println(op);
        ModelNode result = controller.execute(op, null, null, null);
//        System.out.println(result);
        Assert.assertEquals("failed", result.get("outcome").asString());
        assertTrue(result.hasDefined(FAILURE_DESCRIPTION));

        assertTrue(sharedState.get());

        ServiceController<?> sc = container.getService(ServiceName.JBOSS.append("bad-service"));
        if (sc != null) {
            Assert.assertEquals(ServiceController.Mode.REMOVE, sc.getMode());
        }

        Assert.assertEquals(1, controller.execute(getOperation("good", "attr1", 3), null, null, null).get("result").asInt());
        Assert.assertEquals(2, controller.execute(getOperation("good", "attr2", 3), null, null, null).get("result").asInt());
    }

    @Test
    public void testBadServiceNestedCompositeNonRecursive() throws Exception {
        useNonRecursive = true;
        testBadServiceNestedComposite();
    }

    @Test
    public void testMissingServiceNestedComposite() throws Exception {
        ModelNode step1 = getOperation("good", "attr1", 2);
        ModelNode step2 = getOperation("good", "attr2", 1);
        ModelNode comp1 = getCompositeOperation(null, step1, step2);
        ModelNode step3 = getOperation("good", "attr1", 20);
        ModelNode step4 = getOperation("missing-service", "attr2", 10);
        ModelNode comp2 = getCompositeOperation(null, step3, step4);
        ModelNode op = getCompositeOperation(null, comp1, comp2);
//        System.out.println(op);
        ModelNode result = controller.execute(op, null, null, null);
//        System.out.println(result);
        Assert.assertEquals("failed", result.get("outcome").asString());
        assertTrue(result.hasDefined(FAILURE_DESCRIPTION));

        assertTrue(sharedState.get());

        ServiceController<?> sc = container.getService(ServiceName.JBOSS.append("missing-service"));
        if (sc != null) {
            Assert.assertEquals(ServiceController.Mode.REMOVE, sc.getMode());
        }

        Assert.assertEquals(1, controller.execute(getOperation("good", "attr1", 3), null, null, null).get("result").asInt());
        Assert.assertEquals(2, controller.execute(getOperation("good", "attr2", 3), null, null, null).get("result").asInt());
    }

    @Test
    public void testMissingServiceNestedCompositeNonRecursive() throws Exception {
        useNonRecursive = true;
        testMissingServiceNestedComposite();
    }

    @Test
    public void testGoodServiceCompositeTxRollback() throws Exception {

        assertTrue(sharedState.get());
        ModelNode step1 = getOperation("good", "attr1", 2);
        ModelNode step2 = getOperation("good-service", "attr2", 1);
        ModelNode result = controller.execute(getCompositeOperation(null, step1, step2), null, ModelControllerImplUnitTestCase.RollbackTransactionControl.INSTANCE, null);
        System.out.println(result);
        Assert.assertEquals("failed", result.get("outcome").asString());
        assertTrue(result.hasDefined(FAILURE_DESCRIPTION));

        assertTrue(sharedState.get());

        ServiceController<?> sc = container.getService(ServiceName.JBOSS.append("bad-service"));
        if (sc != null) {
            Assert.assertEquals(ServiceController.Mode.REMOVE, sc.getMode());
        }

        Assert.assertEquals(1, controller.execute(getOperation("good", "attr1", 3), null, null, null).get("result").asInt());
        Assert.assertEquals(2, controller.execute(getOperation("good", "attr2", 3), null, null, null).get("result").asInt());
    }

    @Test
    public void testGoodServiceCompositeTxRollbackNonRecursive() throws Exception {
        useNonRecursive = true;
        testGoodServiceCompositeTxRollback();
    }

    @Test
    public void testGoodServiceNestedCompositeTxRollback() throws Exception {
        ModelNode step1 = getOperation("good", "attr1", 2);
        ModelNode step2 = getOperation("good", "attr2", 1);
        ModelNode comp1 = getCompositeOperation(null, step1, step2);
        ModelNode step3 = getOperation("good", "attr1", 20);
        ModelNode step4 = getOperation("good-service", "attr2", 10);
        ModelNode comp2 = getCompositeOperation(null, step3, step4);
        ModelNode op = getCompositeOperation(null, comp1, comp2);
//        System.out.println(op);
        ModelNode result = controller.execute(op, null, ModelControllerImplUnitTestCase.RollbackTransactionControl.INSTANCE, null);
        System.out.println(result);
        Assert.assertEquals("failed", result.get("outcome").asString());
        assertTrue(result.hasDefined(FAILURE_DESCRIPTION));

        assertTrue(sharedState.get());

        ServiceController<?> sc = container.getService(ServiceName.JBOSS.append("bad-service"));
        if (sc != null) {
            Assert.assertEquals(ServiceController.Mode.REMOVE, sc.getMode());
        }

        Assert.assertEquals(1, controller.execute(getOperation("good", "attr1", 3), null, null, null).get("result").asInt());
        Assert.assertEquals(2, controller.execute(getOperation("good", "attr2", 3), null, null, null).get("result").asInt());
    }

    @Test
    public void testGoodServiceNestedCompositeTxRollbackNonRecursive() throws Exception {
        useNonRecursive = true;
        testGoodServiceNestedCompositeTxRollback();
    }

    @Test
//    @Ignore("AS7-1103 Fails intermittently for unknown reasons")
    public void testReloadRequired() throws Exception {
        ModelNode step1 = getOperation("reload-required", "attr1", 5);
        ModelNode step2 = getOperation("good", "attr2", 1);
        ModelNode comp = getCompositeOperation(null, step1, step2);
        ModelNode result = controller.execute(comp, null, null, null);
        System.out.println(result);
        Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());
        assertTrue(result.get(RESULT, "step-1", RESPONSE_HEADERS, RUNTIME_UPDATE_SKIPPED).asBoolean());
        assertTrue(result.get(RESULT, "step-1", RESPONSE_HEADERS, OPERATION_REQUIRES_RELOAD).asBoolean());
        Assert.assertEquals(ControlledProcessState.State.RELOAD_REQUIRED.toString(), result.get(RESPONSE_HEADERS, PROCESS_STATE).asString());

        result = controller.execute(getOperation("good", "attr1", 5), null, null, null);
        Assert.assertEquals("success", result.get(OUTCOME).asString());
        Assert.assertEquals(5, result.get(RESULT).asInt());
    }

    @Test
//    @Ignore("AS7-1103 Fails intermittently for unknown reasons")
    public void testReloadRequiredNonRecursive() throws Exception {
        useNonRecursive = true;
        testReloadRequired();
    }

    @Test
//    @Ignore("AS7-1103 Fails intermittently for unknown reasons")
    public void testRestartRequired() throws Exception {
        ModelNode step1 = getOperation("restart-required", "attr1", 5);
        ModelNode step2 = getOperation("good", "attr2", 1);
        ModelNode comp = getCompositeOperation(null, step1, step2);
        ModelNode result = controller.execute(comp, null, null, null);
        System.out.println(result);
        Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());
        assertTrue(result.get(RESULT, "step-1", RESPONSE_HEADERS, RUNTIME_UPDATE_SKIPPED).asBoolean());
        assertTrue(result.get(RESULT, "step-1", RESPONSE_HEADERS, OPERATION_REQUIRES_RESTART).asBoolean());
        Assert.assertEquals(ControlledProcessState.State.RESTART_REQUIRED.toString(), result.get(RESPONSE_HEADERS, PROCESS_STATE).asString());

        result = controller.execute(getOperation("good", "attr1", 3), null, null, null);
        Assert.assertEquals("success", result.get(OUTCOME).asString());
        Assert.assertEquals(5, result.get(RESULT).asInt());
    }

    @Test
//    @Ignore("AS7-1103 Fails intermittently for unknown reasons")
    public void testRestartRequiredNonRecursive() throws Exception {
        useNonRecursive = true;
        testRestartRequired();
    }

    @Test
    public void testReloadRequiredTxRollback() throws Exception {
        ModelNode step1 = getOperation("reload-required", "attr1", 5);
        ModelNode step2 = getOperation("good", "attr2", 1);
        ModelNode comp = getCompositeOperation(null, step1, step2);
        ModelNode result = controller.execute(comp, null, ModelControllerImplUnitTestCase.RollbackTransactionControl.INSTANCE, null);
        System.out.println(result);
        Assert.assertEquals(FAILED, result.get(OUTCOME).asString());
        assertTrue(result.get(RESULT, "step-1", RESPONSE_HEADERS, RUNTIME_UPDATE_SKIPPED).asBoolean());
        assertFalse(result.get(RESULT, "step-1", RESPONSE_HEADERS).hasDefined(OPERATION_REQUIRES_RELOAD));
        assertFalse(result.get(RESPONSE_HEADERS).hasDefined(PROCESS_STATE));

        // Confirm model was unchanged
        result = controller.execute(getOperation("good", "attr1", 1), null, null, null);
        Assert.assertEquals("success", result.get(OUTCOME).asString());
        Assert.assertEquals(1, result.get(RESULT).asInt());
    }

    @Test
    public void testReloadRequiredTxRollbackNonRecursive() throws Exception {
        useNonRecursive = true;
        testReloadRequiredTxRollback();
    }

    @Test
    public void testRestartRequiredTxRollback() throws Exception {
        ModelNode step1 = getOperation("restart-required", "attr1", 5);
        ModelNode step2 = getOperation("good", "attr2", 1);
        ModelNode comp = getCompositeOperation(null, step1, step2);
        ModelNode result = controller.execute(comp, null, ModelControllerImplUnitTestCase.RollbackTransactionControl.INSTANCE, null);
        System.out.println(result);
        Assert.assertEquals(FAILED, result.get(OUTCOME).asString());
        assertTrue(result.get(RESULT, "step-1", RESPONSE_HEADERS, RUNTIME_UPDATE_SKIPPED).asBoolean());
        assertFalse(result.get(RESULT, "step-1", RESPONSE_HEADERS).hasDefined(OPERATION_REQUIRES_RESTART));
        assertFalse(result.get(RESPONSE_HEADERS).hasDefined(PROCESS_STATE));

        // Confirm model was unchanged
        result = controller.execute(getOperation("good", "attr1", 1), null, null, null);
        Assert.assertEquals("success", result.get(OUTCOME).asString());
        Assert.assertEquals(1, result.get(RESULT).asInt());
    }

    @Test
    public void testRestartRequiredTxRollbackNonRecursive() throws Exception {
        useNonRecursive = true;
        testRestartRequiredTxRollback();
    }

    @Test
    public void testCompositeReleasesLocks() throws Exception {
        System.out.println("------");
        assertTrue(sharedState.get());
        ModelNode step1 = getOperation("good", "attr1", 2);
        ModelNode step2 = getOperation("good-service", "attr2", 1);
        controller.execute(getCompositeOperation(null, step1, step2), null, null, null);

        System.out.println("------");

        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicInteger i = new AtomicInteger();
        new Thread(new Runnable() {
            @Override
            public void run() {
                i.set(controller.execute(getOperation("good", "attr1", 3), null, null, null).get("result").asInt());
                latch.countDown();
            }
        }).start();
        latch.await();
        assertEquals(2, i.get());
    }

    @Test
    public void testCompositeReleasesLocksNonRecursive() throws Exception {
        useNonRecursive = true;
        testCompositeReleasesLocks();
    }

    @Test
    public void testSingleStepOperation() throws Exception {
        ModelNode step = getOperation("good", "attr2", 1);
        ModelNode comp = getCompositeOperation(null, step);
        ModelNode result = controller.execute(comp, null, ModelControllerImplUnitTestCase.RollbackTransactionControl.INSTANCE, null);
        System.out.println(result);
    }

    @Test
    public void testSingleStepOperationNonRecursive() throws Exception {
        useNonRecursive = true;
        testSingleStepOperation();
    }

    public static ModelNode getCompositeOperation(Boolean rollback, ModelNode... steps) {

        ModelNode op = new ModelNode();
        op.get(OP).set("composite");
        op.get(OP_ADDR).setEmptyList();
        for (ModelNode step : steps) {
            op.get("steps").add(step);
        }
        if (rollback != null) {
            op.get("rollback-on-runtime-failure").set(rollback);
        }
        return op;
    }
}

/**
 *
 */
package org.jboss.as.server;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ROLLBACK_ON_RUNTIME_FAILURE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;
import static org.jboss.as.server.ServerModelControllerImplUnitTestCase.DESC_PROVIDER;
import static org.jboss.as.server.ServerModelControllerImplUnitTestCase.NULL_REPO;
import static org.jboss.as.server.ServerModelControllerImplUnitTestCase.createTestNode;
import static org.jboss.as.server.ServerModelControllerImplUnitTestCase.getOperation;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jboss.as.controller.client.Operation;
import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.as.server.ServerModelControllerImplUnitTestCase.BadHandler;
import org.jboss.as.server.ServerModelControllerImplUnitTestCase.BadServiceHandler;
import org.jboss.as.server.ServerModelControllerImplUnitTestCase.EvilHandler;
import org.jboss.as.server.ServerModelControllerImplUnitTestCase.GoodHandler;
import org.jboss.as.server.ServerModelControllerImplUnitTestCase.GoodServiceHandler;
import org.jboss.as.server.ServerModelControllerImplUnitTestCase.HandleFailedHandler;
import org.jboss.as.server.ServerModelControllerImplUnitTestCase.MissingServiceHandler;
import org.jboss.as.server.ServerModelControllerImplUnitTestCase.NullConfigurationPersister;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceController.State;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Unit tests of {@link BaseCompositeOperationHandler}.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class ServerCompositeOperationHandlerUnitTestCase {

    private ServiceContainer container;
    private TestModelController controller;

    @Before
    public void setupController() {
        container = ServiceContainer.Factory.create("test");
        ServiceTarget target = container.subTarget();
        controller = new TestModelController(container, target, new AtomicBoolean(true));
        container.addListener(controller.getServerStateMonitorListener());
        controller.finishBoot();
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
    }

    @Test
    public void testGoodCompositeExecution() throws Exception {
        goodCompositeExecutionTest(false);
    }

    @Test
    @Ignore
    public void testGoodCompositeExecutionAsync() throws Exception {
        goodCompositeExecutionTest(true);
    }

    private void goodCompositeExecutionTest(boolean async) throws Exception {

        Operation step1 = getOperation("good", "attr1", 2, async);
        Operation step2 = getOperation("good", "attr2", 1, async);
        ModelNode result = controller.execute(getCompositeOperation(null, step1, step2));
        assertEquals("success", result.get("outcome").asString());
        assertEquals(2, result.get("result").asInt());
        assertEquals("success", result.get("result", "step-1", "outcome").asString());
        assertEquals("success", result.get("result", "step-2", "outcome").asString());
        assertEquals(1, result.get("result", "step-1", "result").asInt());
        assertEquals(2, result.get("result", "step-2", "result").asInt());
        assertEquals("good", result.get("result", "step-1", "compensating-operation", "operation").asString());
        assertEquals("good", result.get("result", "step-2", "compensating-operation", "operation").asString());
        assertEquals(new ModelNode().setEmptyList(), result.get("result", "step-1", "compensating-operation", "address"));
        assertEquals(new ModelNode().setEmptyList(), result.get("result", "step-2", "compensating-operation", "address"));
        assertEquals("composite", result.get("compensating-operation", "operation").asString());
        assertEquals(new ModelNode().setEmptyList(), result.get("compensating-operation", "address"));
        assertEquals(2, result.get("compensating-operation", "steps").asInt());

        // 2 ops set it from true to false to true
        assertTrue(controller.state.get());

        assertEquals(2, controller.execute(getOperation("good", "attr1", 3)).get("result").asInt());
        assertEquals(1, controller.execute(getOperation("good", "attr2", 3)).get("result").asInt());
    }

    @Test
    public void testOperationFailedExecution() throws Exception {
        Operation step1 = getOperation("good", "attr1", 2);
        Operation step2 = getOperation("bad", "attr2", 1);
        ModelNode result = controller.execute(getCompositeOperation(null, step1, step2));
        assertEquals(FAILED, result.get(OUTCOME).asString());
        final String description = result.get("failure-description").toString();
        assertTrue(description.contains("this request is bad"));
        assertTrue(description.contains(" and was rolled back."));

        assertTrue(controller.state.get());

        assertEquals(1, controller.execute(getOperation("good", "attr1", 3)).get("result").asInt());
        assertEquals(2, controller.execute(getOperation("good", "attr2", 3)).get("result").asInt());
    }

    @Test
    public void testOperationFailedExecutionNoRollback() throws Exception {
        Operation step1 = getOperation("good", "attr1", 2);
        Operation step2 = getOperation("bad", "attr2", 1);
        Operation op = getCompositeOperation(null, step1, step2);
        op.getOperation().get(OPERATION_HEADERS, ROLLBACK_ON_RUNTIME_FAILURE).set(false);
        ModelNode result = controller.execute(op);
        assertEquals(SUCCESS, result.get(OUTCOME).asString());

        assertTrue(controller.state.get());

        assertEquals(2, controller.execute(getOperation("good", "attr1", 3)).get("result").asInt());
        assertEquals(1, controller.execute(getOperation("good", "attr2", 3)).get("result").asInt());
    }

    @Test
    public void testUnhandledFailureExecution() throws Exception {
        Operation step1 = getOperation("good", "attr1", 2);
        Operation step2 = getOperation("evil", "attr2", 1);
        ModelNode result = controller.execute(getCompositeOperation(null, step1, step2));
        assertEquals(FAILED, result.get(OUTCOME).asString());

        final String description = result.get("failure-description").toString();
        assertTrue(description.contains("this handler is evil"));
        assertTrue(description.contains(" and was rolled back."));

        assertTrue(controller.state.get());

        assertEquals(1, controller.execute(getOperation("good", "attr1", 3)).get("result").asInt());
        assertEquals(2, controller.execute(getOperation("good", "attr2", 3)).get("result").asInt());
    }

    @Test
    public void testUnhandledFailureExecutionNoRollback() throws Exception {
        Operation step1 = getOperation("good", "attr1", 2);
        Operation step2 = getOperation("evil", "attr2", 1);
        Operation op = getCompositeOperation(null, step1, step2);
        op.getOperation().get(OPERATION_HEADERS, ROLLBACK_ON_RUNTIME_FAILURE).set(false);
        ModelNode result = controller.execute(op);
        assertEquals(SUCCESS, result.get(OUTCOME).asString());

        assertTrue(controller.state.get());

        assertEquals(2, controller.execute(getOperation("good", "attr1", 3)).get("result").asInt());
        assertEquals(1, controller.execute(getOperation("good", "attr2", 3)).get("result").asInt());
    }

    @Test
    public void testHandleFailedExecution() throws Exception {
        handleFailedExecutionTest(false);
    }

    @Test
    @Ignore
    public void testHandleFailedExecutionAsync() throws Exception {
        handleFailedExecutionTest(true);
    }

    private void handleFailedExecutionTest(boolean async) throws Exception {
        Operation step1 = getOperation("good", "attr1", 2, async);
        Operation step2 = getOperation("handleFailed", "attr2", 1, async);
        ModelNode result = controller.execute(getCompositeOperation(null, step1, step2));
        assertEquals(FAILED, result.get(OUTCOME).asString());
        final String description = result.get("failure-description").toString();
        assertTrue(description.contains("handleFailed"));
        assertTrue(description.contains(" and was rolled back."));

        assertTrue(controller.state.get());

        assertEquals(1, controller.execute(getOperation("good", "attr1", 3)).get("result").asInt());
        assertEquals(2, controller.execute(getOperation("good", "attr2", 3)).get("result").asInt());
    }

    @Test
    public void testHandleFailedExecutionNoRollback() throws Exception {
        Operation step1 = getOperation("good", "attr1", 2);
        Operation step2 = getOperation("handleFailed", "attr2", 1);
        Operation op = getCompositeOperation(null, step1, step2);
        op.getOperation().get(OPERATION_HEADERS, ROLLBACK_ON_RUNTIME_FAILURE).set(false);
        ModelNode result = controller.execute(op);
        assertEquals(SUCCESS, result.get(OUTCOME).asString());

        assertTrue(controller.state.get());

        assertEquals(2, controller.execute(getOperation("good", "attr1", 3)).get("result").asInt());
        assertEquals(1, controller.execute(getOperation("good", "attr2", 3)).get("result").asInt());
    }

    @Test
    public void testGoodNestedComposite() throws Exception {
        goodNestedCompositeTest(false);
    }

    @Test
    public void testGoodNestedCompositeAsync() throws Exception {
        goodNestedCompositeTest(true);
    }

    private void goodNestedCompositeTest(boolean async) throws Exception {
        Operation step1 = getOperation("good", "attr1", 2, async);
        Operation step2 = getOperation("good", "attr2", 1, async);
        Operation comp1 = getCompositeOperation(null, step1, step2);
        Operation step3 = getOperation("good", "attr1", 20, async);
        Operation step4 = getOperation("good", "attr2", 10, async);
        Operation comp2 = getCompositeOperation(null, step3, step4);
        Operation op = getCompositeOperation(null, comp1, comp2);
//        System.out.println(op);
        ModelNode result = controller.execute(op);
//        System.out.println(result);
        assertEquals(SUCCESS, result.get(OUTCOME).asString());
        assertTrue(result.hasDefined(RESULT));
        assertTrue(result.get(RESULT).hasDefined("step-1"));
        assertTrue(result.get(RESULT, "step-1").hasDefined(OUTCOME));
        assertEquals(SUCCESS, result.get(RESULT, "step-1", OUTCOME).asString());
        assertTrue(result.get(RESULT).hasDefined("step-2"));
        assertTrue(result.get(RESULT, "step-2").hasDefined(OUTCOME));
        assertEquals(SUCCESS, result.get(RESULT, "step-2", OUTCOME).asString());

        assertEquals(20, controller.execute(getOperation("good", "attr1", 3)).get(RESULT).asInt());
        assertEquals(10, controller.execute(getOperation("good", "attr2", 3)).get(RESULT).asInt());
    }

    @Test
    public void testBadNestedComposite() throws Exception {
        Operation step1 = getOperation("good", "attr1", 2);
        Operation step2 = getOperation("good", "attr2", 1);
        Operation comp1 = getCompositeOperation(null, step1, step2);
        Operation step3 = getOperation("good", "attr1", 20);
        Operation step4 = getOperation("bad", "attr2", 10);
        Operation comp2 = getCompositeOperation(null, step3, step4);
        Operation op = getCompositeOperation(null, comp1, comp2);
//        System.out.println(op);
        ModelNode result = controller.execute(op);
//        System.out.println(result);
        assertEquals("failed", result.get("outcome").asString());

        assertEquals(1, controller.execute(getOperation("good", "attr1", 3)).get("result").asInt());
        assertEquals(2, controller.execute(getOperation("good", "attr2", 3)).get("result").asInt());
    }

    @Test
    public void testGoodServiceCompositeExecution() throws Exception {

        assertTrue(controller.state.get());
        Operation step1 = getOperation("good", "attr1", 2);
        Operation step2 = getOperation("good-service", "attr2", 1);
        ModelNode result = controller.execute(getCompositeOperation(null, step1, step2));
//        System.out.println(result);
        assertEquals("success", result.get("outcome").asString());
        assertEquals(2, result.get("result").asInt());
        assertEquals("success", result.get("result", "step-1", "outcome").asString());
        assertEquals("success", result.get("result", "step-2", "outcome").asString());
        assertEquals(1, result.get("result", "step-1", "result").asInt());
        assertEquals(2, result.get("result", "step-2", "result").asInt());
        assertEquals("good", result.get("result", "step-1", "compensating-operation", "operation").asString());
        assertEquals("good-service", result.get("result", "step-2", "compensating-operation", "operation").asString());
        assertEquals(new ModelNode().setEmptyList(), result.get("result", "step-1", "compensating-operation", "address"));
        assertEquals(new ModelNode().setEmptyList(), result.get("result", "step-2", "compensating-operation", "address"));
        assertEquals("composite", result.get("compensating-operation", "operation").asString());
        assertEquals(new ModelNode().setEmptyList(), result.get("compensating-operation", "address"));
        assertEquals(2, result.get("compensating-operation", "steps").asInt());

        assertFalse(controller.state.get());

        ServiceController<?> sc = container.getService(ServiceName.JBOSS.append("good-service"));
        assertNotNull(sc);
        assertEquals(State.UP, sc.getState());

        assertEquals(2, controller.execute(getOperation("good", "attr1", 3)).get("result").asInt());
        assertEquals(1, controller.execute(getOperation("good", "attr2", 3)).get("result").asInt());
    }

    @Test
    public void testGoodServiceNestedComposite() throws Exception {
        Operation step1 = getOperation("good", "attr1", 2);
        Operation step2 = getOperation("good", "attr2", 1);
        Operation comp1 = getCompositeOperation(null, step1, step2);
        Operation step3 = getOperation("good", "attr1", 20);
        Operation step4 = getOperation("good-service", "attr2", 10);
        Operation comp2 = getCompositeOperation(null, step3, step4);
        Operation op = getCompositeOperation(null, comp1, comp2);
//        System.out.println(op);
        ModelNode result = controller.execute(op);
//        System.out.println(result);
        assertEquals(SUCCESS, result.get(OUTCOME).asString());
        assertTrue(result.hasDefined(RESULT));
        assertTrue(result.get(RESULT).hasDefined("step-1"));
        assertTrue(result.get(RESULT, "step-1").hasDefined(OUTCOME));
        assertEquals(SUCCESS, result.get(RESULT, "step-1", OUTCOME).asString());
        assertTrue(result.get(RESULT).hasDefined("step-2"));
        assertTrue(result.get(RESULT, "step-2").hasDefined(OUTCOME));
        assertEquals(SUCCESS, result.get(RESULT, "step-2", OUTCOME).asString());

        assertEquals(20, controller.execute(getOperation("good", "attr1", 3)).get(RESULT).asInt());
        assertEquals(10, controller.execute(getOperation("good", "attr2", 3)).get(RESULT).asInt());

        assertFalse(controller.state.get());

        ServiceController<?> sc = container.getService(ServiceName.JBOSS.append("good-service"));
        assertNotNull(sc);
        assertEquals(State.UP, sc.getState());
    }

    @Test
    public void testBadService() throws Exception {
        Operation step1 = getOperation("good", "attr1", 2);
        Operation step2 = getOperation("bad-service", "attr2", 1);
        ModelNode result = controller.execute(getCompositeOperation(null, step1, step2));
//        System.out.println(result);
        assertEquals(FAILED, result.get(OUTCOME).asString());
        assertTrue(result.hasDefined(FAILURE_DESCRIPTION));

        assertTrue(controller.state.get());

        ServiceController<?> sc = container.getService(ServiceName.JBOSS.append("bad-service"));
        if (sc != null) {
            assertEquals(Mode.REMOVE, sc.getMode());
        }

        assertEquals(1, controller.execute(getOperation("good", "attr1", 3)).get("result").asInt());
        assertEquals(2, controller.execute(getOperation("good", "attr2", 3)).get("result").asInt());
    }

    @Test
    public void testMissingService() throws Exception {
        Operation step1 = getOperation("good", "attr1", 2);
        Operation step2 = getOperation("missing-service", "attr2", 1);
        ModelNode result = controller.execute(getCompositeOperation(null, step1, step2));
//        System.out.println(result);
        assertEquals(FAILED, result.get(OUTCOME).asString());
        assertTrue(result.hasDefined(FAILURE_DESCRIPTION));

        assertTrue(controller.state.get());

        ServiceController<?> sc = container.getService(ServiceName.JBOSS.append("missing-service"));
        if (sc != null) {
            assertEquals(Mode.REMOVE, sc.getMode());
        }

        assertEquals(1, controller.execute(getOperation("good", "attr1", 3)).get("result").asInt());
        assertEquals(2, controller.execute(getOperation("good", "attr2", 3)).get("result").asInt());
    }

    @Test
    public void testBadServiceNestedComposite() throws Exception {
        Operation step1 = getOperation("good", "attr1", 2);
        Operation step2 = getOperation("good", "attr2", 1);
        Operation comp1 = getCompositeOperation(null, step1, step2);
        Operation step3 = getOperation("good", "attr1", 20);
        Operation step4 = getOperation("bad-service", "attr2", 10);
        Operation comp2 = getCompositeOperation(null, step3, step4);
        Operation op = getCompositeOperation(null, comp1, comp2);
//        System.out.println(op);
        ModelNode result = controller.execute(op);
//        System.out.println(result);
        assertEquals("failed", result.get("outcome").asString());
        assertTrue(result.hasDefined(FAILURE_DESCRIPTION));

        assertTrue(controller.state.get());

        ServiceController<?> sc = container.getService(ServiceName.JBOSS.append("bad-service"));
        if (sc != null) {
            assertEquals(Mode.REMOVE, sc.getMode());
        }

        assertEquals(1, controller.execute(getOperation("good", "attr1", 3)).get("result").asInt());
        assertEquals(2, controller.execute(getOperation("good", "attr2", 3)).get("result").asInt());
    }

    @Test
    public void testMissingServiceNestedComposite() throws Exception {
        Operation step1 = getOperation("good", "attr1", 2);
        Operation step2 = getOperation("good", "attr2", 1);
        Operation comp1 = getCompositeOperation(null, step1, step2);
        Operation step3 = getOperation("good", "attr1", 20);
        Operation step4 = getOperation("missing-service", "attr2", 10);
        Operation comp2 = getCompositeOperation(null, step3, step4);
        Operation op = getCompositeOperation(null, comp1, comp2);
//        System.out.println(op);
        ModelNode result = controller.execute(op);
//        System.out.println(result);
        assertEquals("failed", result.get("outcome").asString());
        assertTrue(result.hasDefined(FAILURE_DESCRIPTION));

        assertTrue(controller.state.get());

        ServiceController<?> sc = container.getService(ServiceName.JBOSS.append("missing-service"));
        if (sc != null) {
            assertEquals(Mode.REMOVE, sc.getMode());
        }

        assertEquals(1, controller.execute(getOperation("good", "attr1", 3)).get("result").asInt());
        assertEquals(2, controller.execute(getOperation("good", "attr2", 3)).get("result").asInt());
    }

    public static Operation getCompositeOperation(Boolean rollback, Operation... steps) {

        ModelNode op = new ModelNode();
        op.get("operation").set("composite");
        op.get("address").setEmptyList();
        for (Operation step : steps) {
            op.get("steps").add(step.getOperation());
        }
        if (rollback != null) {
            op.get(OPERATION_HEADERS, ROLLBACK_ON_RUNTIME_FAILURE).set(rollback);
        }
        return OperationBuilder.Factory.create(op).build();
    }

    private static class TestModelController extends ServerControllerImpl {

        private final AtomicBoolean state;
        protected TestModelController(ServiceContainer container, ServiceTarget target, AtomicBoolean state) {
            super(container, target, null, new NullConfigurationPersister(), NULL_REPO , Executors.newCachedThreadPool());
            this.state = state;
            getModel().set(createTestNode());

            getRegistry().registerOperationHandler("good", new GoodHandler(state), DESC_PROVIDER, false);
            getRegistry().registerOperationHandler("bad", new BadHandler(state), DESC_PROVIDER, false);
            getRegistry().registerOperationHandler("evil", new EvilHandler(state), DESC_PROVIDER, false);
            getRegistry().registerOperationHandler("handleFailed", new HandleFailedHandler(state), DESC_PROVIDER, false);
            getRegistry().registerOperationHandler("good-service", new GoodServiceHandler(), DESC_PROVIDER, false);
            getRegistry().registerOperationHandler("bad-service", new BadServiceHandler(), DESC_PROVIDER, false);
            getRegistry().registerOperationHandler("missing-service", new MissingServiceHandler(), DESC_PROVIDER, false);
        }
    }
}

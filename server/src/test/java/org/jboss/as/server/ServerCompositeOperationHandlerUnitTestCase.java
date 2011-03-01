/**
 *
 */
package org.jboss.as.server;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;
import static org.jboss.as.server.ServerModelControllerImplUnitTestCase.DESC_PROVIDER;
import static org.jboss.as.server.ServerModelControllerImplUnitTestCase.NULL_REPO;
import static org.jboss.as.server.ServerModelControllerImplUnitTestCase.createTestNode;
import static org.jboss.as.server.ServerModelControllerImplUnitTestCase.getOperation;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jboss.as.controller.client.ExecutionContext;
import org.jboss.as.controller.client.ExecutionContextBuilder;
import org.jboss.as.server.ServerModelControllerImplUnitTestCase.BadHandler;
import org.jboss.as.server.ServerModelControllerImplUnitTestCase.EvilHandler;
import org.jboss.as.server.ServerModelControllerImplUnitTestCase.GoodHandler;
import org.jboss.as.server.ServerModelControllerImplUnitTestCase.HandleFailedHandler;
import org.jboss.as.server.ServerModelControllerImplUnitTestCase.NullConfigurationPersister;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceTarget;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests of {@link BaseCompositeOperationHandler}.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class ServerCompositeOperationHandlerUnitTestCase {

    private ServiceContainer container;
    private TestModelController controller;

    private static final AtomicBoolean runtimeState = new AtomicBoolean(true);

    @Before
    public void setupController() {
        container = ServiceContainer.Factory.create("test");
        ServiceTarget target = container.subTarget();
        controller = new TestModelController(container, target);
        runtimeState.set(true);
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
    public void testGoodCompositeExecutionAsync() throws Exception {
        goodCompositeExecutionTest(true);
    }

    private void goodCompositeExecutionTest(boolean async) throws Exception {
        ExecutionContext step1 = getOperation("good", "attr1", 2, async);
        ExecutionContext step2 = getOperation("good", "attr2", 1, async);
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
        assertTrue(runtimeState.get());

        assertEquals(2, controller.execute(getOperation("good", "attr1", 3)).get("result").asInt());
        assertEquals(1, controller.execute(getOperation("good", "attr2", 3)).get("result").asInt());
    }

    @Test
    public void testOperationFailedExecution() throws Exception {
        ExecutionContext step1 = getOperation("good", "attr1", 2);
        ExecutionContext step2 = getOperation("bad", "attr2", 1);
        ModelNode result = controller.execute(getCompositeOperation(null, step1, step2));
        assertEquals(FAILED, result.get(OUTCOME).asString());
        assertTrue(result.get("failure-description").toString().indexOf("this request is bad") > - 1);

        assertTrue(runtimeState.get());

        assertEquals(1, controller.execute(getOperation("good", "attr1", 3)).get("result").asInt());
        assertEquals(2, controller.execute(getOperation("good", "attr2", 3)).get("result").asInt());
    }

    @Test
    public void testOperationFailedExecutionNoRollback() throws Exception {
        ExecutionContext step1 = getOperation("good", "attr1", 2);
        ExecutionContext step2 = getOperation("bad", "attr2", 1);
        ExecutionContext op = getCompositeOperation(null, step1, step2);
        op.getOperation().get("rollback-on-runtime-failure").set(false);
        ModelNode result = controller.execute(op);
        assertEquals(FAILED, result.get(OUTCOME).asString());
        assertTrue(result.get("failure-description").toString().indexOf("this request is bad") > - 1);

        assertTrue(runtimeState.get());

        assertEquals(2, controller.execute(getOperation("good", "attr1", 3)).get("result").asInt());
        assertEquals(1, controller.execute(getOperation("good", "attr2", 3)).get("result").asInt());
    }

    @Test
    public void testUnhandledFailureExecution() throws Exception {
        ExecutionContext step1 = getOperation("good", "attr1", 2);
        ExecutionContext step2 = getOperation("evil", "attr2", 1);
        ModelNode result = controller.execute(getCompositeOperation(null, step1, step2));
        assertEquals(FAILED, result.get(OUTCOME).asString());
        assertTrue(result.get("failure-description").toString().indexOf("this handler is evil") > - 1);

        assertTrue(runtimeState.get());

        assertEquals(1, controller.execute(getOperation("good", "attr1", 3)).get("result").asInt());
        assertEquals(2, controller.execute(getOperation("good", "attr2", 3)).get("result").asInt());
    }

    @Test
    public void testUnhandledFailureExecutionNoRollback() throws Exception {
        ExecutionContext step1 = getOperation("good", "attr1", 2);
        ExecutionContext step2 = getOperation("evil", "attr2", 1);
        ExecutionContext op = getCompositeOperation(null, step1, step2);
        op.getOperation().get("rollback-on-runtime-failure").set(false);
        ModelNode result = controller.execute(op);
        assertEquals(FAILED, result.get(OUTCOME).asString());
        assertTrue(result.get("failure-description").toString().indexOf("this handler is evil") > - 1);

        assertTrue(runtimeState.get());

        assertEquals(2, controller.execute(getOperation("good", "attr1", 3)).get("result").asInt());
        assertEquals(1, controller.execute(getOperation("good", "attr2", 3)).get("result").asInt());
    }

    @Test
    public void testHandleFailedExecution() throws Exception {
        handleFailedExecutionTest(false);
    }

    @Test
    public void testHandleFailedExecutionAsync() throws Exception {
        handleFailedExecutionTest(true);
    }

    private void handleFailedExecutionTest(boolean async) throws Exception {
        ExecutionContext step1 = getOperation("good", "attr1", 2, async);
        ExecutionContext step2 = getOperation("handleFailed", "attr2", 1, async);
        ModelNode result = controller.execute(getCompositeOperation(null, step1, step2));
        assertEquals(FAILED, result.get(OUTCOME).asString());
        assertTrue(result.get("failure-description").toString().indexOf("handleFailed") > - 1);

        assertTrue(runtimeState.get());

        assertEquals(1, controller.execute(getOperation("good", "attr1", 3)).get("result").asInt());
        assertEquals(2, controller.execute(getOperation("good", "attr2", 3)).get("result").asInt());
    }

    @Test
    public void testHandleFailedExecutionNoRollback() throws Exception {
        ExecutionContext step1 = getOperation("good", "attr1", 2);
        ExecutionContext step2 = getOperation("handleFailed", "attr2", 1);
        ExecutionContext op = getCompositeOperation(null, step1, step2);
        op.getOperation().get("rollback-on-runtime-failure").set(false);
        ModelNode result = controller.execute(op);
        assertEquals(FAILED, result.get(OUTCOME).asString());
        assertTrue(result.get("failure-description").toString().indexOf("handleFailed") > - 1);

        assertTrue(runtimeState.get());

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
        ExecutionContext step1 = getOperation("good", "attr1", 2, async);
        ExecutionContext step2 = getOperation("good", "attr2", 1, async);
        ExecutionContext comp1 = getCompositeOperation(null, step1, step2);
        ExecutionContext step3 = getOperation("good", "attr1", 20, async);
        ExecutionContext step4 = getOperation("good", "attr2", 10, async);
        ExecutionContext comp2 = getCompositeOperation(null, step3, step4);
        ExecutionContext op = getCompositeOperation(null, comp1, comp2);
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
        ExecutionContext step1 = getOperation("good", "attr1", 2);
        ExecutionContext step2 = getOperation("good", "attr2", 1);
        ExecutionContext comp1 = getCompositeOperation(null, step1, step2);
        ExecutionContext step3 = getOperation("good", "attr1", 20);
        ExecutionContext step4 = getOperation("bad", "attr2", 10);
        ExecutionContext comp2 = getCompositeOperation(null, step3, step4);
        ExecutionContext op = getCompositeOperation(null, comp1, comp2);
        System.out.println(op);
        ModelNode result = controller.execute(op);
        System.out.println(result);
        assertEquals("failed", result.get("outcome").asString());

        assertEquals(1, controller.execute(getOperation("good", "attr1", 3)).get("result").asInt());
        assertEquals(2, controller.execute(getOperation("good", "attr2", 3)).get("result").asInt());
    }

    public static ExecutionContext getCompositeOperation(Boolean rollback, ExecutionContext... steps) {

        ModelNode op = new ModelNode();
        op.get("operation").set("composite");
        op.get("address").setEmptyList();
        for (ExecutionContext step : steps) {
            op.get("steps").add(step.getOperation());
        }
        if (rollback != null) {
            op.get("rollback-on-runtime-failure").set(rollback);
        }
        return ExecutionContextBuilder.Factory.create(op).build();
    }

    private static class TestModelController extends ServerControllerImpl {
        protected TestModelController(ServiceContainer container, ServiceTarget target) {
            super(container, target, null, new NullConfigurationPersister(), NULL_REPO , Executors.newCachedThreadPool());

            getModel().set(createTestNode());

            getRegistry().registerOperationHandler("good", new GoodHandler(), DESC_PROVIDER, false);
            getRegistry().registerOperationHandler("bad", new BadHandler(), DESC_PROVIDER, false);
            getRegistry().registerOperationHandler("evil", new EvilHandler(), DESC_PROVIDER, false);
            getRegistry().registerOperationHandler("handleFailed", new HandleFailedHandler(), DESC_PROVIDER, false);
        }
    }
}

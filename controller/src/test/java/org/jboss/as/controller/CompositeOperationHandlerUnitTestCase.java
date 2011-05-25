/**
 *
 */
package org.jboss.as.controller;

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

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static junit.framework.Assert.assertEquals;
import static org.jboss.as.controller.ModelControllerImplUnitTestCase.getOperation;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ROLLBACK_ON_RUNTIME_FAILURE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests of composite operation handling.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
@Ignore("Composite ops not working yet")
public class CompositeOperationHandlerUnitTestCase {

    private ServiceContainer container;
    private NewModelController controller;
    private AtomicBoolean sharedState;

    @Before
    public void setupController() throws InterruptedException {
        container = ServiceContainer.Factory.create("test");
        ServiceTarget target = container.subTarget();
        ModelControllerImplUnitTestCase.ModelControllerService svc = new ModelControllerImplUnitTestCase.ModelControllerService(container);
        ServiceBuilder<NewModelController> builder = target.addService(ServiceName.of("ModelController"), svc);
        builder.install();
        sharedState = svc.state;
        svc.latch.await();
        controller = svc.getValue();
        ModelNode setup = Util.getEmptyOperation("setup", new ModelNode());
        controller.execute(setup, null, null, null);
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
        ModelNode step1 = getOperation("good", "attr1", 2);
        ModelNode step2 = getOperation("good", "attr2", 1);
        ModelNode result = controller.execute(getCompositeOperation(null, step1, step2), null, null, null);
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

        assertEquals(2, controller.execute(getOperation("good", "attr1", 3), null, null, null).get("result").asInt());
        assertEquals(1, controller.execute(getOperation("good", "attr2", 3), null, null, null).get("result").asInt());
    }

    @Test
    public void testBadCompositeExecution() throws Exception {
        ModelNode step1 = getOperation("good", "attr1", 2);
        ModelNode step2 = getOperation("bad", "attr2", 1);
        ModelNode result = controller.execute(getCompositeOperation(null, step1, step2), null, null, null);
        System.out.println(result);
        assertEquals(FAILED, result.get(OUTCOME).asString());
        assertTrue(result.get(FAILURE_DESCRIPTION).toString().indexOf("this request is bad") > - 1);


        assertEquals(1, controller.execute(getOperation("good", "attr1", 3), null, null, null).get("result").asInt());
        assertEquals(2, controller.execute(getOperation("good", "attr2", 3), null, null, null).get("result").asInt());
    }

    @Test
    public void testEvilCompositeExecution() throws Exception {
        ModelNode step1 = getOperation("good", "attr1", 2);
        ModelNode step2 = getOperation("evil", "attr2", 1);
        ModelNode result = controller.execute(getCompositeOperation(null, step1, step2), null, null, null);
        assertEquals(FAILED, result.get(OUTCOME).asString());
        assertTrue(result.get(FAILURE_DESCRIPTION).toString().indexOf("this handler is evil") > - 1);

        assertEquals(1, controller.execute(getOperation("good", "attr1", 3), null, null, null).get("result").asInt());
        assertEquals(2, controller.execute(getOperation("good", "attr2", 3), null, null, null).get("result").asInt());
    }

    @Test
    public void testOperationFailedExecution() throws Exception {
        ModelNode step1 = getOperation("good", "attr1", 2);
        ModelNode step2 = getOperation("bad", "attr2", 1);
        ModelNode result = controller.execute(getCompositeOperation(null, step1, step2), null, null, null);
        Assert.assertEquals(FAILED, result.get(OUTCOME).asString());
        final String description = result.get("failure-description").toString();
        assertTrue(description.contains("this request is bad"));
        assertTrue(description.contains(" and was rolled back."));

        assertTrue(sharedState.get());

        Assert.assertEquals(1, controller.execute(getOperation("good", "attr1", 3), null, null, null).get("result").asInt());
        Assert.assertEquals(2, controller.execute(getOperation("good", "attr2", 3), null, null, null).get("result").asInt());
    }

    @Test
    public void testOperationFailedExecutionNoRollback() throws Exception {
        ModelNode step1 = getOperation("good", "attr1", 2);
        ModelNode step2 = getOperation("bad", "attr2", 1);
        ModelNode op = getCompositeOperation(null, step1, step2);
        op.get(OPERATION_HEADERS, ROLLBACK_ON_RUNTIME_FAILURE).set(false);
        ModelNode result = controller.execute(op, null, null, null);
        Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());

        assertTrue(sharedState.get());

        Assert.assertEquals(2, controller.execute(getOperation("good", "attr1", 3), null, null, null).get("result").asInt());
        Assert.assertEquals(1, controller.execute(getOperation("good", "attr2", 3), null, null, null).get("result").asInt());
    }

    @Test
    public void testUnhandledFailureExecution() throws Exception {
        ModelNode step1 = getOperation("good", "attr1", 2);
        ModelNode step2 = getOperation("evil", "attr2", 1);
        ModelNode result = controller.execute(getCompositeOperation(null, step1, step2), null, null, null);
        Assert.assertEquals(FAILED, result.get(OUTCOME).asString());

        final String description = result.get("failure-description").toString();
        assertTrue(description.contains("this handler is evil"));
        assertTrue(description.contains(" and was rolled back."));

        assertTrue(sharedState.get());

        Assert.assertEquals(1, controller.execute(getOperation("good", "attr1", 3), null, null, null).get("result").asInt());
        Assert.assertEquals(2, controller.execute(getOperation("good", "attr2", 3), null, null, null).get("result").asInt());
    }

    @Test
    public void testUnhandledFailureExecutionNoRollback() throws Exception {
        ModelNode step1 = getOperation("good", "attr1", 2);
        ModelNode step2 = getOperation("evil", "attr2", 1);
        ModelNode op = getCompositeOperation(null, step1, step2);
        op.get(OPERATION_HEADERS, ROLLBACK_ON_RUNTIME_FAILURE).set(false);
        ModelNode result = controller.execute(op, null, null, null);
        Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());

        assertTrue(sharedState.get());

        Assert.assertEquals(2, controller.execute(getOperation("good", "attr1", 3), null, null, null).get("result").asInt());
        Assert.assertEquals(1, controller.execute(getOperation("good", "attr2", 3), null, null, null).get("result").asInt());
    }

    @Test
    public void testHandleFailedExecution() throws Exception {
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
    public void testHandleFailedExecutionNoRollback() throws Exception {
        ModelNode step1 = getOperation("good", "attr1", 2);
        ModelNode step2 = getOperation("handleFailed", "attr2", 1);
        ModelNode op = getCompositeOperation(null, step1, step2);
        op.get(OPERATION_HEADERS, ROLLBACK_ON_RUNTIME_FAILURE).set(false);
        ModelNode result = controller.execute(op, null, null, null);
        Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());

        assertTrue(sharedState.get());

        Assert.assertEquals(2, controller.execute(getOperation("good", "attr1", 3), null, null, null).get("result").asInt());
        Assert.assertEquals(1, controller.execute(getOperation("good", "attr2", 3), null, null, null).get("result").asInt());
    }

    @Test
    public void testGoodNestedComposite() throws Exception {
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
    public void testBadNestedComposite() throws Exception {
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
    public void testGoodServiceCompositeExecution() throws Exception {

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
        Assert.assertEquals("good", result.get("result", "step-1", "compensating-operation", "operation").asString());
        Assert.assertEquals("good-service", result.get("result", "step-2", "compensating-operation", "operation").asString());
        Assert.assertEquals(new ModelNode().setEmptyList(), result.get("result", "step-1", "compensating-operation", "address"));
        Assert.assertEquals(new ModelNode().setEmptyList(), result.get("result", "step-2", "compensating-operation", "address"));
        Assert.assertEquals("composite", result.get("compensating-operation", "operation").asString());
        Assert.assertEquals(new ModelNode().setEmptyList(), result.get("compensating-operation", "address"));
        Assert.assertEquals(2, result.get("compensating-operation", "steps").asInt());

        assertFalse(sharedState.get());

        ServiceController<?> sc = container.getService(ServiceName.JBOSS.append("good-service"));
        assertNotNull(sc);
        Assert.assertEquals(ServiceController.State.UP, sc.getState());

        Assert.assertEquals(2, controller.execute(getOperation("good", "attr1", 3), null, null, null).get("result").asInt());
        Assert.assertEquals(1, controller.execute(getOperation("good", "attr2", 3), null, null, null).get("result").asInt());
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

        assertFalse(sharedState.get());

        ServiceController<?> sc = container.getService(ServiceName.JBOSS.append("good-service"));
        assertNotNull(sc);
        Assert.assertEquals(ServiceController.State.UP, sc.getState());
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

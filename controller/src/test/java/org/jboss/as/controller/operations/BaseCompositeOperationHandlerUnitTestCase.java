/**
 *
 */
package org.jboss.as.controller.operations;

import static org.jboss.as.controller.BaseModelControllerUnitTestCase.DESC_PROVIDER;
import static org.jboss.as.controller.BaseModelControllerUnitTestCase.createTestNode;
import static org.jboss.as.controller.BaseModelControllerUnitTestCase.getOperation;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.OutputStream;
import java.util.List;

import org.jboss.as.controller.BaseModelControllerUnitTestCase.BadHandler;
import org.jboss.as.controller.BaseModelControllerUnitTestCase.EvilHandler;
import org.jboss.as.controller.BaseModelControllerUnitTestCase.GoodHandler;
import org.jboss.as.controller.BasicModelController;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.persistence.ConfigurationPersistenceException;
import org.jboss.as.controller.persistence.NewConfigurationPersister;
import org.jboss.dmr.ModelNode;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests of {@link BaseCompositeOperationHandler}.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class BaseCompositeOperationHandlerUnitTestCase {

    private TestModelController controller;

    @Before
    public void setupController() {
        controller = new TestModelController();
    }

    @Test
    public void testGoodCompositeExecution() throws Exception {
        ModelNode step1 = getOperation("good", "attr1", 2);
        ModelNode step2 = getOperation("good", "attr2", 1);
        ModelNode result = controller.execute(getCompositeOperation(null, step1, step2));
        assertEquals("success", result.get("outcome").asString());
        assertEquals(2, result.get("result").asInt());
        assertEquals("success", result.get("result").get(0).get("outcome").asString());
        assertEquals("success", result.get("result").get(1).get("outcome").asString());
        assertEquals(1, result.get("result").get(0).get("result").asInt());
        assertEquals(2, result.get("result").get(1).get("result").asInt());
        assertEquals("good", result.get("result").get(0).get("compensating-operation", "operation").asString());
        assertEquals("good", result.get("result").get(1).get("compensating-operation", "operation").asString());
        assertEquals(new ModelNode().setEmptyList(), result.get("result").get(0).get("compensating-operation", "address"));
        assertEquals(new ModelNode().setEmptyList(), result.get("result").get(1).get("compensating-operation", "address"));
        assertEquals("composite", result.get("compensating-operation", "operation").asString());
        assertEquals(new ModelNode().setEmptyList(), result.get("compensating-operation", "address"));
        assertEquals(2, result.get("compensating-operation", "steps").asInt());

        assertEquals(2, controller.execute(getOperation("good", "attr1", 3)).get("result").asInt());
        assertEquals(1, controller.execute(getOperation("good", "attr2", 3)).get("result").asInt());
    }

    @Test
    public void testBadCompositeExecution() throws Exception {
        ModelNode step1 = getOperation("good", "attr1", 2);
        ModelNode step2 = getOperation("bad", "attr2", 1);
        try {
            controller.execute(getCompositeOperation(null, step1, step2));
            fail("should have thrown OperationFailedException");
        }
        catch (OperationFailedException e) {
            assertTrue(e.getFailureDescription().get("failure-description").toString().indexOf("this request is bad") > - 1);
        }

        assertEquals(1, controller.execute(getOperation("good", "attr1", 3)).get("result").asInt());
        assertEquals(2, controller.execute(getOperation("good", "attr2", 3)).get("result").asInt());
    }

    @Test
    public void testEvilCompositeExecution() throws Exception {
        ModelNode step1 = getOperation("good", "attr1", 2);
        ModelNode step2 = getOperation("evil", "attr2", 1);
        try {
            controller.execute(getCompositeOperation(null, step1, step2));
            fail("should have thrown OperationFailedException");
        }
        catch (OperationFailedException e) {
            assertTrue(e.getFailureDescription().get("failure-description").toString().indexOf("this handler is evil") > - 1);
        }

        assertEquals(1, controller.execute(getOperation("good", "attr1", 3)).get("result").asInt());
        assertEquals(2, controller.execute(getOperation("good", "attr2", 3)).get("result").asInt());
    }

    public static ModelNode getCompositeOperation(Boolean rollback, ModelNode... steps) {

        ModelNode op = new ModelNode();
        op.get("operation").set("composite");
        op.get("address").setEmptyList();
        for (ModelNode step : steps) {
            op.get("steps").add(step);
        }
        if (rollback != null) {
            op.get("rollback-on-runtime-failure").set(rollback);
        }
        return op;
    }

    private static class TestModelController extends BasicModelController {
        protected TestModelController() {
            super(createTestNode(), new NullConfigurationPersister(), DESC_PROVIDER);

            getRegistry().registerOperationHandler("good", new GoodHandler(), DESC_PROVIDER, false);
            getRegistry().registerOperationHandler("bad", new BadHandler(), DESC_PROVIDER, false);
            getRegistry().registerOperationHandler("evil", new EvilHandler(), DESC_PROVIDER, false);

            getRegistry().registerOperationHandler(BaseCompositeOperationHandler.OPERATION_NAME, BaseCompositeOperationHandler.INSTANCE, DESC_PROVIDER, false);
        }
    }


    private static class NullConfigurationPersister implements NewConfigurationPersister{

        @Override
        public void store(ModelNode model) throws ConfigurationPersistenceException {
        }

        @Override
        public void marshallAsXml(ModelNode model, OutputStream output) throws ConfigurationPersistenceException {
        }

        @Override
        public List<ModelNode> load() throws ConfigurationPersistenceException {
            return null;
        }

    }
}

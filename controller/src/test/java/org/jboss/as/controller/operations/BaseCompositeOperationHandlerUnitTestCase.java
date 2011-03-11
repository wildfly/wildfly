/**
 *
 */
package org.jboss.as.controller.operations;

import static junit.framework.Assert.assertEquals;
import static org.jboss.as.controller.BaseModelControllerUnitTestCase.DESC_PROVIDER;
import static org.jboss.as.controller.BaseModelControllerUnitTestCase.createTestNode;
import static org.jboss.as.controller.BaseModelControllerUnitTestCase.getOperation;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.junit.Assert.assertTrue;

import java.io.OutputStream;
import java.util.List;

import org.jboss.as.controller.BaseModelControllerUnitTestCase.BadHandler;
import org.jboss.as.controller.BaseModelControllerUnitTestCase.EvilHandler;
import org.jboss.as.controller.BaseModelControllerUnitTestCase.GoodHandler;
import org.jboss.as.controller.BasicModelController;
import org.jboss.as.controller.client.Operation;
import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.as.controller.persistence.ConfigurationPersistenceException;
import org.jboss.as.controller.persistence.ConfigurationPersister;
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
        ModelNode step1 = getOperation("good", "attr1", 2).getOperation();
        ModelNode step2 = getOperation("good", "attr2", 1).getOperation();
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

        assertEquals(2, controller.execute(getOperation("good", "attr1", 3)).get("result").asInt());
        assertEquals(1, controller.execute(getOperation("good", "attr2", 3)).get("result").asInt());
    }

    @Test
    public void testBadCompositeExecution() throws Exception {
        ModelNode step1 = getOperation("good", "attr1", 2).getOperation();
        ModelNode step2 = getOperation("bad", "attr2", 1).getOperation();
        ModelNode result = controller.execute(getCompositeOperation(null, step1, step2));
        System.out.println(result);
        assertEquals(FAILED, result.get(OUTCOME).asString());
        assertTrue(result.get(FAILURE_DESCRIPTION).toString().indexOf("this request is bad") > - 1);


        assertEquals(1, controller.execute(getOperation("good", "attr1", 3)).get("result").asInt());
        assertEquals(2, controller.execute(getOperation("good", "attr2", 3)).get("result").asInt());
    }

    @Test
    public void testEvilCompositeExecution() throws Exception {
        ModelNode step1 = getOperation("good", "attr1", 2).getOperation();
        ModelNode step2 = getOperation("evil", "attr2", 1).getOperation();
        ModelNode result = controller.execute(getCompositeOperation(null, step1, step2));
        assertEquals(FAILED, result.get(OUTCOME).asString());
        assertTrue(result.get(FAILURE_DESCRIPTION).toString().indexOf("this handler is evil") > - 1);

        assertEquals(1, controller.execute(getOperation("good", "attr1", 3)).get("result").asInt());
        assertEquals(2, controller.execute(getOperation("good", "attr2", 3)).get("result").asInt());
    }

    @Test
    public void testGoodNestedComposite() throws Exception {
        ModelNode step1 = getOperation("good", "attr1", 2).getOperation();
        ModelNode step2 = getOperation("good", "attr2", 1).getOperation();
        Operation comp1 = getCompositeOperation(null, step1, step2);
        ModelNode step3 = getOperation("good", "attr1", 20).getOperation();
        ModelNode step4 = getOperation("good", "attr2", 10).getOperation();
        Operation comp2 = getCompositeOperation(null, step3, step4);
        Operation op = getCompositeOperation(null, comp1.getOperation(), comp2.getOperation());
        System.out.println(op);
        ModelNode result = controller.execute(op);
        System.out.println(result);
        assertEquals("success", result.get("outcome").asString());

        assertEquals(20, controller.execute(getOperation("good", "attr1", 3)).get("result").asInt());
        assertEquals(10, controller.execute(getOperation("good", "attr2", 3)).get("result").asInt());
    }

    @Test
    public void testBadNestedComposite() throws Exception {
        ModelNode step1 = getOperation("good", "attr1", 2).getOperation();
        ModelNode step2 = getOperation("good", "attr2", 1).getOperation();
        Operation comp1 = getCompositeOperation(null, step1, step2);
        ModelNode step3 = getOperation("good", "attr1", 20).getOperation();
        ModelNode step4 = getOperation("bad", "attr2", 10).getOperation();
        Operation comp2 = getCompositeOperation(null, step3, step4);
        Operation op = getCompositeOperation(null, comp1.getOperation(), comp2.getOperation());
//        System.out.println(op);
        ModelNode result = controller.execute(op);
//        System.out.println(result);
        assertEquals("failed", result.get("outcome").asString());

        assertEquals(1, controller.execute(getOperation("good", "attr1", 3)).get("result").asInt());
        assertEquals(2, controller.execute(getOperation("good", "attr2", 3)).get("result").asInt());
    }

    public static Operation getCompositeOperation(Boolean rollback, ModelNode... steps) {

        ModelNode op = new ModelNode();
        op.get(OP).set("composite");
        op.get(OP_ADDR).setEmptyList();
        for (ModelNode step : steps) {
            op.get("steps").add(step);
        }
        if (rollback != null) {
            op.get("rollback-on-runtime-failure").set(rollback);
        }
        return OperationBuilder.Factory.create(op).build();
    }

    private static class TestModelController extends BasicModelController {
        protected TestModelController() {
            super(createTestNode(), new NullConfigurationPersister(), DESC_PROVIDER);

            getRegistry().registerOperationHandler("good", new GoodHandler(), DESC_PROVIDER, false);
            getRegistry().registerOperationHandler("bad", new BadHandler(), DESC_PROVIDER, false);
            getRegistry().registerOperationHandler("evil", new EvilHandler(), DESC_PROVIDER, false);
        }
    }


    private static class NullConfigurationPersister implements ConfigurationPersister{

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

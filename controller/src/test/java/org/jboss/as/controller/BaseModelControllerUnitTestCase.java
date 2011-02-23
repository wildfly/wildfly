/**
 *
 */
package org.jboss.as.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.OutputStream;
import java.util.List;
import java.util.Locale;

import org.jboss.as.controller.BasicModelController;
import org.jboss.as.controller.BasicOperationResult;
import org.jboss.as.controller.ModelUpdateOperationHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationResult;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.operations.BaseCompositeOperationHandler;
import org.jboss.as.controller.persistence.ConfigurationPersistenceException;
import org.jboss.as.controller.persistence.NewConfigurationPersister;
import org.jboss.dmr.ModelNode;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests of {@link BaseModelController}.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class BaseModelControllerUnitTestCase {

    private TestModelController controller;

    @Before
    public void setupController() {
        controller = new TestModelController();
    }

    @Test
    public void testGoodExecution() throws Exception {
        ModelNode result = controller.execute(getOperation("good", "attr1", 5));
        assertEquals("success", result.get("outcome").asString());
        assertEquals(1, result.get("result").asInt());
        result = controller.execute(getOperation("good", "attr1", 1));
        assertEquals("success", result.get("outcome").asString());
        assertEquals(5, result.get("result").asInt());
    }

    @Test
    public void testOperationFailedExecution() throws Exception {
        ModelNode result = null;
        try {
            result = controller.execute(getOperation("bad", "attr1", 5));
            fail("should have thrown OperationFailedException");
        }
        catch (OperationFailedException e) {
            assertEquals("this request is bad", e.getFailureDescription().get("failure-description").asString());
        }
        // Confirm model was unchanged
        result = controller.execute(getOperation("good", "attr1", 1));
        assertEquals("success", result.get("outcome").asString());
        assertEquals(1, result.get("result").asInt());
    }

    @Test
    public void testUnhandledFailureExecution() throws Exception {
        ModelNode result = null;
        try {
            result = controller.execute(getOperation("evil", "attr1", 5));
            fail("should have thrown OperationFailedException");
        }
        catch (OperationFailedException e) {
            assertTrue(e.getFailureDescription().get("failure-description").toString().indexOf("this handler is evil") > - 1);
        }
        // Confirm model was unchanged
        result = controller.execute(getOperation("good", "attr1", 1));
        assertEquals("success", result.get("outcome").asString());
        assertEquals(1, result.get("result").asInt());
    }

    public static ModelNode createTestNode() {
        ModelNode model = new ModelNode();

        //Atttributes
        model.get("attr1").set(1);
        model.get("attr2").set(2);

        return model;
    }

    public static ModelNode getOperation(String opName, String attr, int val) {
        return getOperation(opName, attr, val, null);
    }

    public static ModelNode getOperation(String opName, String attr, int val, String rollbackName) {
        ModelNode op = new ModelNode();
        op.get("operation").set(opName);
        op.get("address").setEmptyList();
        op.get("name").set(attr);
        op.get("value").set(val);
        op.get("rollbackName").set(rollbackName == null ? opName : rollbackName);
        return op;
    }

    public static class GoodHandler implements ModelUpdateOperationHandler {
        @Override
        public OperationResult execute(OperationContext context, ModelNode operation, ResultHandler resultHandler)
                throws OperationFailedException {

            String name = operation.require("name").asString();
            ModelNode attr = context.getSubModel().get(name);
            int current = attr.asInt();
            attr.set(operation.require("value"));

            resultHandler.handleResultFragment(new String[0], new ModelNode().set(current));
            resultHandler.handleResultComplete();
            return new BasicOperationResult(getOperation("good", name, current, operation.get("rollbackName").asString()));
        }
    }

    public static class BadHandler implements ModelUpdateOperationHandler {
        @Override
        public OperationResult execute(OperationContext context, ModelNode operation, ResultHandler resultHandler)
                throws OperationFailedException {

            String name = operation.require("name").asString();
            ModelNode attr = context.getSubModel().get(name);
            attr.set(operation.require("value"));

            resultHandler.handleResultFragment(new String[0], new ModelNode().set("bad"));
            throw new OperationFailedException(new ModelNode().set("this request is bad"));
        }
    }

    public static class EvilHandler implements ModelUpdateOperationHandler {
        @Override
        public OperationResult execute(OperationContext context, ModelNode operation, ResultHandler resultHandler)
                throws OperationFailedException {

            String name = operation.require("name").asString();
            ModelNode attr = context.getSubModel().get(name);
            attr.set(operation.require("value"));

            throw new RuntimeException("this handler is evil");
        }
    }

    public static final DescriptionProvider DESC_PROVIDER = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            return new ModelNode();
        }
    };

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

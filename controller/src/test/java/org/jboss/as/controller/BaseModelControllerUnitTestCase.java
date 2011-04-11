/**
 *
 */
package org.jboss.as.controller;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.OutputStream;
import java.util.List;
import java.util.Locale;

import org.jboss.as.controller.client.Operation;
import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.persistence.ConfigurationPersistenceException;
import org.jboss.as.controller.persistence.ConfigurationPersister;
import org.jboss.as.controller.persistence.ConfigurationPersister.SnapshotInfo;
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
        assertEquals(SUCCESS, result.get(OUTCOME).asString());
        assertEquals(1, result.get("result").asInt());
        result = controller.execute(getOperation("good", "attr1", 1));
        assertEquals(SUCCESS, result.get(OUTCOME).asString());
        assertEquals(5, result.get(RESULT).asInt());
    }

    @Test
    public void testOperationFailedExecution() throws Exception {
        ModelNode result = controller.execute(getOperation("bad", "attr1", 5));
        assertEquals(FAILED, result.get(OUTCOME).asString());
        assertEquals("this request is bad", result.get(FAILURE_DESCRIPTION).asString());

        // Confirm model was unchanged
        result = controller.execute(getOperation("good", "attr1", 1));
        assertEquals(SUCCESS, result.get(OUTCOME).asString());
        assertEquals(1, result.get(RESULT).asInt());
    }

    @Test
    public void testUnhandledFailureExecution() throws Exception {
        ModelNode result = controller.execute(getOperation("evil", "attr1", 5));
        assertEquals(FAILED, result.get(OUTCOME).asString());
        assertTrue(result.get(FAILURE_DESCRIPTION).toString().indexOf("this handler is evil") > - 1);

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

    public static Operation getOperation(String opName, String attr, int val) {
        return getOperation(opName, attr, val, null);
    }

    public static Operation getOperation(String opName, String attr, int val, String rollbackName) {
        ModelNode op = new ModelNode();
        op.get(OP).set(opName);
        op.get(OP_ADDR).setEmptyList();
        op.get(NAME).set(attr);
        op.get(VALUE).set(val);
        op.get("rollbackName").set(rollbackName == null ? opName : rollbackName);

        return OperationBuilder.Factory.create(op).build();
    }

    public static class GoodHandler implements ModelUpdateOperationHandler {
        @Override
        public OperationResult execute(OperationContext context, ModelNode operation, ResultHandler resultHandler)
                throws OperationFailedException {

            String name = operation.require(NAME).asString();
            ModelNode attr = context.getSubModel().get(name);
            int current = attr.asInt();
            attr.set(operation.require(VALUE));

            resultHandler.handleResultFragment(new String[0], new ModelNode().set(current));
            resultHandler.handleResultComplete();
            return new BasicOperationResult(getOperation("good", name, current, operation.get("rollbackName").asString()).getOperation());
        }
    }

    public static class BadHandler implements ModelUpdateOperationHandler {
        @Override
        public OperationResult execute(OperationContext context, ModelNode operation, ResultHandler resultHandler)
                throws OperationFailedException {

            String name = operation.require(NAME).asString();
            ModelNode attr = context.getSubModel().get(name);
            attr.set(operation.require(VALUE));

            resultHandler.handleResultFragment(new String[0], new ModelNode().set("bad"));
            throw new OperationFailedException(new ModelNode().set("this request is bad"));
        }
    }

    public static class EvilHandler implements ModelUpdateOperationHandler {
        @Override
        public OperationResult execute(OperationContext context, ModelNode operation, ResultHandler resultHandler)
                throws OperationFailedException {

            String name = operation.require(NAME).asString();
            ModelNode attr = context.getSubModel().get(name);
            attr.set(operation.require(VALUE));

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

        @Override
        public void successfulBoot() throws ConfigurationPersistenceException {
        }

        @Override
        public SnapshotInfo listSnapshots() {
            return NULL_SNAPSHOT_INFO;
        }

        @Override
        public String snapshot() {
            return null;
        }

        @Override
        public void deleteSnapshot(String name) {
        }

    }
}

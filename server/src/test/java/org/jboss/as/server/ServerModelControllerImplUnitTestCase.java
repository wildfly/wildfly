/**
 *
 */
package org.jboss.as.server;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jboss.as.controller.BasicOperationResult;
import org.jboss.as.controller.ModelUpdateOperationHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationResult;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.RuntimeTask;
import org.jboss.as.controller.RuntimeTaskContext;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.persistence.ConfigurationPersistenceException;
import org.jboss.as.controller.persistence.ExtensibleConfigurationPersister;
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.as.server.deployment.api.DeploymentRepository;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.staxmapper.XMLElementWriter;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests of {@link BaseModelController}.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class ServerModelControllerImplUnitTestCase {

    private TestModelController controller;

    private static final AtomicBoolean runtimeState = new AtomicBoolean(true);

    @Before
    public void setupController() {
        ServiceContainer container = ServiceContainer.Factory.create("test");
        ServiceTarget target = container.subTarget();
        controller = new TestModelController(container, target);
        runtimeState.set(true);
    }

    @Test
    public void testGoodExecution() throws Exception {
        ModelNode result = controller.execute(getOperation("good", "attr1", 5));
        assertEquals("success", result.get("outcome").asString());
        assertEquals(1, result.get("result").asInt());

        assertFalse(runtimeState.get());

        result = controller.execute(getOperation("good", "attr1", 1));
        assertEquals("success", result.get("outcome").asString());
        assertEquals(5, result.get("result").asInt());

        assertTrue(runtimeState.get());
    }

    @Test
    public void testOperationFailedExecution() throws Exception {
        ModelNode result = controller.execute(getOperation("bad", "attr1", 5, "good"));
        assertEquals(FAILED, result.get(OUTCOME).asString());
        assertEquals("this request is bad", result.get("failure-description").asString());

        // Confirm runtime state was unchanged
        assertTrue(runtimeState.get());

        // Confirm model was unchanged
        result = controller.execute(getOperation("good", "attr1", 1));
        assertEquals("success", result.get("outcome").asString());
        assertEquals(1, result.get("result").asInt());
    }

    @Test
    public void testUnhandledFailureExecution() throws Exception {
        ModelNode result = controller.execute(getOperation("evil", "attr1", 5, "good"));
        assertEquals(FAILED, result.get(OUTCOME).asString());
        assertTrue(result.get("failure-description").toString().indexOf("this handler is evil") > - 1);

        // Confirm runtime state was unchanged
        assertTrue(runtimeState.get());

        // Confirm model was unchanged
        result = controller.execute(getOperation("good", "attr1", 1));
        assertEquals("success", result.get("outcome").asString());
        assertEquals(1, result.get("result").asInt());
    }

    @Test
    public void testHandleFailedExecution() throws Exception {
        ModelNode result = controller.execute(getOperation("handleFailed", "attr1", 5, "good"));
        assertEquals(FAILED, result.get(OUTCOME).asString());
        assertEquals("handleFailed", result.get("failure-description").asString());

        // Confirm runtime state was unchanged
        assertTrue(runtimeState.get());

        // Confirm model was unchanged
        result = controller.execute(getOperation("good", "attr1", 1));
        assertEquals("success", result.get("outcome").asString());
        assertEquals(1, result.get("result").asInt());
    }

    @Test
    public void testOperationFailedExecutionNoRollback() throws Exception {

        ModelNode op = getOperation("bad", "attr1", 5, "good");
        op.get("rollback-on-runtime-failure").set(false);
        ModelNode result = controller.execute(op);
        assertEquals(FAILED, result.get(OUTCOME).asString());
        assertEquals("this request is bad", result.get("failure-description").asString());

        // Confirm runtime state was changed
        assertFalse(runtimeState.get());

        // Confirm model was changed
        result = controller.execute(getOperation("good", "attr1", 1));
        assertEquals("success", result.get("outcome").asString());
        assertEquals(5, result.get("result").asInt());
    }

    @Test
    public void testHandleFailedExecutionNoRollback() throws Exception {

        ModelNode op = getOperation("handleFailed", "attr1", 5, "good");
        op.get("rollback-on-runtime-failure").set(false);
        ModelNode result = controller.execute(op);
        assertEquals(FAILED, result.get(OUTCOME).asString());
        assertEquals("handleFailed", result.get("failure-description").asString());

        // Confirm runtime state was changed
        assertFalse(runtimeState.get());

        // Confirm model was changed
        result = controller.execute(getOperation("good", "attr1", 1));
        assertEquals("success", result.get("outcome").asString());
        assertEquals(5, result.get("result").asInt());
    }

    @Test
    public void testUnhandledFailureExecutionNoRollback() throws Exception {

        ModelNode op = getOperation("evil", "attr1", 5, "good");
        op.get("rollback-on-runtime-failure").set(false);
        ModelNode result = controller.execute(op);
        assertEquals(FAILED, result.get(OUTCOME).asString());
        assertTrue(result.get("failure-description").toString().indexOf("this handler is evil") > - 1);

        // Confirm runtime state was changed
        assertFalse(runtimeState.get());

        // Confirm model was changed
        result = controller.execute(getOperation("good", "attr1", 1));
        assertEquals("success", result.get("outcome").asString());
        assertEquals(5, result.get("result").asInt());
    }

    public static ModelNode createTestNode() {
        ModelNode model = new ModelNode();

        //Atttributes
        model.get("attr1").set(1);
        model.get("attr2").set(2);

        return model;
    }

    @Test
    public void testPathologicalRollback() throws Exception {
        ModelNode result = controller.execute(getOperation("bad", "attr1", 5)); // don't tell it to call the 'good' op on rollback
        assertEquals(FAILED, result.get(OUTCOME).asString());
        assertEquals("this request is bad", result.get("failure-description").asString());

        // Confirm runtime state was unchanged
        assertTrue(runtimeState.get());

        // Confirm model was unchanged
        result = controller.execute(getOperation("good", "attr1", 1));
        assertEquals("success", result.get("outcome").asString());
        assertEquals(1, result.get("result").asInt());
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
        public OperationResult execute(final OperationContext context, final ModelNode operation, final ResultHandler resultHandler)
                throws OperationFailedException {

            String name = operation.require("name").asString();
            ModelNode attr = context.getSubModel().get(name);
            final int current = attr.asInt();
            attr.set(operation.require("value"));



            context.getRuntimeContext().setRuntimeTask(new RuntimeTask() {

                @Override
                public void execute(RuntimeTaskContext context) throws OperationFailedException {
                    resultHandler.handleResultFragment(new String[0], new ModelNode().set(current));
                    runtimeState.set(!runtimeState.get());
                    resultHandler.handleResultComplete();
                }
            });
            return new BasicOperationResult(getOperation("good", name, current, operation.get("rollbackName").asString()));
        }
    }

    public static class BadHandler implements ModelUpdateOperationHandler {
        @Override
        public OperationResult execute(OperationContext context, ModelNode operation, ResultHandler resultHandler)
                throws OperationFailedException {

            String name = operation.require("name").asString();
            ModelNode attr = context.getSubModel().get(name);
            int current = attr.asInt();
            attr.set(operation.require("value"));

            resultHandler.handleResultFragment(new String[0], new ModelNode().set("bad"));

            context.getRuntimeContext().setRuntimeTask(new RuntimeTask() {

                @Override
                public void execute(RuntimeTaskContext context) throws OperationFailedException {
                    runtimeState.set(!runtimeState.get());
                    throw new OperationFailedException(new ModelNode().set("this request is bad"));
                }
            });

            return new BasicOperationResult(getOperation(operation.get("rollbackName").asString(), name, current));
        }
    }

    public static class EvilHandler implements ModelUpdateOperationHandler {
        @Override
        public OperationResult execute(OperationContext context, ModelNode operation, ResultHandler resultHandler)
                throws OperationFailedException {

            String name = operation.require("name").asString();
            ModelNode attr = context.getSubModel().get(name);
            int current = attr.asInt();
            attr.set(operation.require("value"));


            context.getRuntimeContext().setRuntimeTask(new RuntimeTask() {

                @Override
                public void execute(RuntimeTaskContext context) throws OperationFailedException {
                    runtimeState.set(!runtimeState.get());
                    throw new RuntimeException("this handler is evil");
                }
            });

            return new BasicOperationResult(getOperation(operation.get("rollbackName").asString(), name, current));

        }
    }

    public static class HandleFailedHandler implements ModelUpdateOperationHandler {
        @Override
        public OperationResult execute(OperationContext context, ModelNode operation, final ResultHandler resultHandler)
                throws OperationFailedException {

            String name = operation.require("name").asString();
            ModelNode attr = context.getSubModel().get(name);
            int current = attr.asInt();
            attr.set(operation.require("value"));

            resultHandler.handleResultFragment(new String[0], new ModelNode().set("bad"));

            context.getRuntimeContext().setRuntimeTask(new RuntimeTask() {

                @Override
                public void execute(RuntimeTaskContext context) throws OperationFailedException {
                    runtimeState.set(!runtimeState.get());
                    resultHandler.handleFailed(new ModelNode().set("handleFailed"));
                }
            });

            return new BasicOperationResult(getOperation(operation.get("rollbackName").asString(), name, current));
        }
    }

    public static final DescriptionProvider DESC_PROVIDER = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            return new ModelNode();
        }
    };

    public static final DeploymentRepository NULL_REPO = new DeploymentRepository() {

        @Override
        public boolean hasDeploymentContent(byte[] hash) {
            return false;
        }

        @Override
        public byte[] addDeploymentContent(String name, String runtimeName, InputStream stream) throws IOException {
            return new byte[20];
        }
    };

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


    public static class NullConfigurationPersister implements ExtensibleConfigurationPersister{

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
        public void registerSubsystemWriter(String name, XMLElementWriter<SubsystemMarshallingContext> writer) {

        }

        @Override
        public void registerSubsystemDeploymentWriter(String name, XMLElementWriter<SubsystemMarshallingContext> writer) {

        }

    }
}

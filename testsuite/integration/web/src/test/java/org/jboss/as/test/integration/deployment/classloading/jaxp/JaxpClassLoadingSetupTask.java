package org.jboss.as.test.integration.deployment.classloading.jaxp;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.Operation;
import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.protocol.StreamUtils;
import org.jboss.as.test.module.util.TestModule;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;

/**
 * A ServerSetupTask to create the given {@link #modules} and deploy the given {@link #apps}.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public class JaxpClassLoadingSetupTask implements ServerSetupTask {

    private static Logger log = Logger.getLogger(JaxpClassLoadingSetupTask.class);

    private final TestModule[] modules;
    private final WebArchive[] apps;

    public JaxpClassLoadingSetupTask(TestModule[] modules, WebArchive[] apps) {
        super();
        this.modules = modules;
        this.apps = apps;
    }

    @Override
    public void setup(ManagementClient managementClient, String containerId) throws Exception {
        for (TestModule module : modules) {
            module.create();
        }

        final ModelNode composite = new ModelNode();
        composite.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.COMPOSITE);
        composite.get(ModelDescriptionConstants.OPERATION_HEADERS)
                .get(ModelDescriptionConstants.ROLLBACK_ON_RUNTIME_FAILURE).set(false);

        final ModelNode nested = composite.get(ModelDescriptionConstants.STEPS).setEmptyList().add();
        nested.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.COMPOSITE);
        nested.get(ModelDescriptionConstants.OP_ADDR).setEmptyList();

        final ModelNode steps = nested.get(ModelDescriptionConstants.STEPS).setEmptyList();

        for (int i = 0; i < apps.length; i++) {
            WebArchive app = apps[i];
            final ModelNode deployOne = steps.add();
            deployOne.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.ADD);
            PathAddress path = PathAddress.pathAddress(
                    PathElement.pathElement(ModelDescriptionConstants.DEPLOYMENT, app.getName() + ".war"));
            deployOne.get(ModelDescriptionConstants.OP_ADDR).set(path.toModelNode());
            deployOne.get(ModelDescriptionConstants.ENABLED).set(true);
            deployOne.get(ModelDescriptionConstants.CONTENT).add().get(ModelDescriptionConstants.INPUT_STREAM_INDEX)
                    .set(i);
        }

        final OperationBuilder operationBuilder = OperationBuilder.create(composite, true);
        final Path warsDir = Paths.get("target/wars").toAbsolutePath();
        Files.createDirectories(warsDir);
        for (WebArchive app : apps) {
            operationBuilder.addInputStream(app.as(ZipExporter.class).exportAsInputStream());
            app.as(ZipExporter.class).exportTo(warsDir.resolve(app.getName() + ".zip").toFile());
        }

        final ModelControllerClient client = managementClient.getControllerClient();
        final Operation operation = operationBuilder.build();
        try {
            final ModelNode overallResult = client.execute(operation);
            Assert.assertTrue(overallResult.asString(), ModelDescriptionConstants.SUCCESS
                    .equals(overallResult.get(ModelDescriptionConstants.OUTCOME).asString()));
        } finally {
            StreamUtils.safeClose(operation);
        }


    }

    @Override
    public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
        for (TestModule module : modules) {
            if (!log.isDebugEnabled()) {
                /* Do not remove the module if we debug */
                module.remove();
            }
        }

        final ModelNode composite = new ModelNode();
        composite.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.COMPOSITE);
        composite.get(ModelDescriptionConstants.OPERATION_HEADERS)
                .get(ModelDescriptionConstants.ROLLBACK_ON_RUNTIME_FAILURE).set(false);

        final ModelNode nested = composite.get(ModelDescriptionConstants.STEPS).setEmptyList().add();
        nested.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.COMPOSITE);
        nested.get(ModelDescriptionConstants.OP_ADDR).setEmptyList();

        final ModelNode steps = nested.get(ModelDescriptionConstants.STEPS).setEmptyList();

        for (int i = 0; i < apps.length; i++) {
            WebArchive app = apps[i];
            final ModelNode deployOne = steps.add();
            deployOne.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.REMOVE);
            PathAddress path = PathAddress.pathAddress(
                    PathElement.pathElement(ModelDescriptionConstants.DEPLOYMENT, app.getName() + ".war"));
            deployOne.get(ModelDescriptionConstants.OP_ADDR).set(path.toModelNode());
        }

        final OperationBuilder operationBuilder = OperationBuilder.create(composite, true);
        final ModelControllerClient client = managementClient.getControllerClient();
        final Operation operation = operationBuilder.build();
        try {
            final ModelNode overallResult = client.execute(operation);
            Assert.assertTrue(overallResult.asString(), ModelDescriptionConstants.SUCCESS
                    .equals(overallResult.get(ModelDescriptionConstants.OUTCOME).asString()));
        } finally {
            StreamUtils.safeClose(operation);
        }

    }

}
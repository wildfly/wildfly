/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.test.integration.management.deploy.runtime;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEPLOY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEPLOYMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INCLUDE_RUNTIME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INPUT_STREAM_INDEX;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RECURSIVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ROLLBACK_ON_RUNTIME_FAILURE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ROLLED_BACK;

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.test.integration.management.deploy.runtime.servlet.BadContextListener;
import org.jboss.as.test.integration.management.util.ModelUtil;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

public abstract class AbstractFailedDeploymentRuntimeTestCase extends AbstractRuntimeTestCase {

    static ModelControllerClient controllerClient = TestSuiteEnvironment.getModelControllerClient();

    /**
     * Create and deploy an EAR application that includes a war that has a ServletContextListener that
     * will throw an exception when invoked, thus failing the deployment. The deploy op will be configured
     * to not rollback-on-runtime-failure, leaving the deployment in place, allowing the management layer's
     * handling of the deployment to be tested.
     *
     * @param deploymentName the name that should be used for the ear deployment.
     * @param badWar the WebArchive in which the failed deployment should be inserted. Subclasses can provide
     *               other content in the WebArchive in order to test how that content is handled.
     * @param otherModules other modules that should be packaged in the ear
     * @throws IOException if one occurs
     */
    static void setup(String deploymentName, WebArchive badWar, Archive<?>... otherModules) throws IOException {

        badWar.addClass(BadContextListener.class);

        EnterpriseArchive earArchive = ShrinkWrap.create(EnterpriseArchive.class, deploymentName);
        earArchive.addAsModule(badWar);
        for (Archive<?> otherModule : otherModules) {
            earArchive.addAsModule(otherModule);
        }

        ModelNode addDeploymentOp = new ModelNode();
        addDeploymentOp.get(ModelDescriptionConstants.ADDRESS).add(DEPLOYMENT, deploymentName);
        addDeploymentOp.get(ModelDescriptionConstants.OP).set(ADD);
        addDeploymentOp.get(ModelDescriptionConstants.CONTENT).get(0).get(INPUT_STREAM_INDEX).set(0);
        addDeploymentOp.get(ModelDescriptionConstants.AUTO_START).set(true);
        ModelNode deployOp = new ModelNode();
        deployOp.get(ModelDescriptionConstants.OP).set(DEPLOY);
        deployOp.get(ModelDescriptionConstants.ADDRESS).add(DEPLOYMENT, deploymentName);
        deployOp.get(ModelDescriptionConstants.ENABLED).set(true);
        ModelNode[] steps = new ModelNode[2];
        steps[0] = addDeploymentOp;
        steps[1] = deployOp;
        ModelNode compositeOp = ModelUtil.createCompositeNode(steps);
        compositeOp.get(OPERATION_HEADERS, ROLLBACK_ON_RUNTIME_FAILURE).set(false);

        OperationBuilder ob = new OperationBuilder(compositeOp, true);
        ob.addInputStream(earArchive.as(ZipExporter.class).exportAsInputStream());

        ModelNode result = controllerClient.execute(ob.build());

        Assert.assertTrue("deploy did not fail: " + result, Operations.isSuccessfulOutcome(result));
        Assert.assertTrue(result.toString(), result.hasDefined(RESULT, "step-2", ROLLED_BACK));
        Assert.assertFalse(result.toString(), result.get(RESULT, "step-2", ROLLED_BACK).asBoolean());
        Assert.assertEquals(result.toString(), FAILED, result.get(RESULT, "step-2", OUTCOME).asString());

    }

    static void tearDown(String deploymentName) throws Exception {
        ModelNode result = controllerClient.execute(composite(
                undeploy(deploymentName),
                remove(deploymentName)
        ));
        Assert.assertTrue("Failed to undeploy: " + result, Operations.isSuccessfulOutcome(result));
    }

    /**
     * Perform a :read-resource(recursive=true,include-runtime=true) against the deployment resource and
     * validates that it is successful. Calls {@link #validateReadResourceResponse(ModelNode)} to allow subclasses
     * to perform further validation.
     *
     * @throws IOException if one occurs
     */
    @Test
    public void testReadResource() throws IOException {
        ModelNode op = new ModelNode();
        op.get(ModelDescriptionConstants.ADDRESS).add(ModelDescriptionConstants.DEPLOYMENT, getDeploymentName());
        op.get(ModelDescriptionConstants.OP).set(READ_RESOURCE_OPERATION);
        op.get(RECURSIVE).set(true);
        op.get(INCLUDE_RUNTIME).set(true);

        ModelNode response = controllerClient.execute(op);

        Assert.assertTrue("Failed to read: " + response, Operations.isSuccessfulOutcome(response));
        validateReadResourceResponse(response);
    }

    /**
     * Hook to allow subclasses to perform further validation of the response to the read-resource call made
     * by {@link #testReadResource()}
     * @param response the response to the read-resource call
     */
    void validateReadResourceResponse(ModelNode response) {
        // No-op
    }

    /**
     * Gets the name that should be used for the deployment resource.
     * @return the name
     */
    abstract String getDeploymentName();

    /**
     * Gets the name that should be used for the war subdeployment resource.
     * @return the name
     */
    abstract String getSubdeploymentName();
}

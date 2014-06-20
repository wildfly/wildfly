/*
* JBoss, Home of Professional Open Source.
* Copyright 2012, Red Hat Middleware LLC, and individual contributors
* as indicated by the @author tags. See the copyright.txt file in the
* distribution for a full listing of individual contributors.
*
* This is free software; you can redistribute it and/or modify it
* under the terms of the GNU Lesser General Public License as
* published by the Free Software Foundation; either version 2.1 of
* the License, or (at your option) any later version.
*
* This software is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
* Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public
* License along with this software; if not, write to the Free
* Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
* 02110-1301 USA, or see the FSF site: http://www.fsf.org.
*/
package org.jboss.as.core.model.test.jvm;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.JVM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING_GROUP;
import static org.jboss.as.host.controller.model.jvm.JvmAttributes.AGENT_LIB;
import static org.jboss.as.host.controller.model.jvm.JvmAttributes.AGENT_PATH;
import static org.jboss.as.host.controller.model.jvm.JvmAttributes.ENVIRONMENT_VARIABLES;
import static org.jboss.as.host.controller.model.jvm.JvmAttributes.ENV_CLASSPATH_IGNORED;
import static org.jboss.as.host.controller.model.jvm.JvmAttributes.HEAP_SIZE;
import static org.jboss.as.host.controller.model.jvm.JvmAttributes.JAVA_AGENT;
import static org.jboss.as.host.controller.model.jvm.JvmAttributes.JAVA_HOME;
import static org.jboss.as.host.controller.model.jvm.JvmAttributes.LAUNCH_COMMAND;
import static org.jboss.as.host.controller.model.jvm.JvmAttributes.MAX_HEAP_SIZE;
import static org.jboss.as.host.controller.model.jvm.JvmAttributes.MAX_PERMGEN_SIZE;
import static org.jboss.as.host.controller.model.jvm.JvmAttributes.OPTIONS;
import static org.jboss.as.host.controller.model.jvm.JvmAttributes.PERMGEN_SIZE;
import static org.jboss.as.host.controller.model.jvm.JvmAttributes.STACK_SIZE;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.jboss.as.controller.ModelController;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.core.model.test.AbstractCoreModelTest;
import org.jboss.as.core.model.test.KernelServices;
import org.jboss.as.core.model.test.KernelServicesBuilder;
import org.jboss.as.core.model.test.TestModelType;
import org.jboss.as.core.model.test.TransformersTestParameterized;
import org.jboss.as.core.model.test.TransformersTestParameterized.TransformersParameter;
import org.jboss.as.core.model.test.util.StandardServerGroupInitializers;
import org.jboss.as.core.model.test.util.TransformersTestParameter;
import org.jboss.as.model.test.FailedOperationTransformationConfig;
import org.jboss.as.model.test.ModelFixer;
import org.jboss.as.model.test.ModelTestControllerVersion;
import org.jboss.as.model.test.ModelTestUtils;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 *
 * @author <a href="http://jmesnil.net">Jeff Mesnil</a> (c) 2012 Red Hat, inc
 */
@RunWith(TransformersTestParameterized.class)
public class JvmTransformersTestCase extends AbstractCoreModelTest {

    private static final PathAddress ADDRESS =
            PathAddress.pathAddress(PathElement.pathElement(SERVER_GROUP, "test"), PathElement.pathElement(JVM, "full"));

    private final ModelVersion modelVersion;
    private final ModelTestControllerVersion testControllerVersion;

    public JvmTransformersTestCase(TransformersTestParameter params) {
        this.modelVersion = params.getModelVersion();
        this.testControllerVersion = params.getTestControllerVersion();
    }

    @TransformersParameter
    public static List<TransformersTestParameter> parameters(){
        return TransformersTestParameter.setupVersions();
    }

    @Test
    public void jvmResourceWithoutExpressions() throws Exception {
        jvmResourceTest("domain-full.xml");
    }

    @Test
    public void jvmResourceWithExpressions() throws Exception {
        jvmResourceTest("domain-with-expressions.xml");
    }

    private void jvmResourceTest(String configFile) throws Exception {
        //Boot up empty controllers with the resources needed for the ops coming from the xml to work
        KernelServicesBuilder builder = createKernelServicesBuilder(TestModelType.DOMAIN)
                .setModelInitializer(StandardServerGroupInitializers.XML_MODEL_INITIALIZER, StandardServerGroupInitializers.XML_MODEL_WRITE_SANITIZER);

        StandardServerGroupInitializers.addServerGroupInitializers(builder.createLegacyKernelServicesBuilder(modelVersion, testControllerVersion));

        KernelServices mainServices = builder.build();
        assertTrue(mainServices.isSuccessfulBoot());
        KernelServices legacyServices = mainServices.getLegacyServices(modelVersion);
        assertTrue(legacyServices.isSuccessfulBoot());

        //Get the boot operations from the xml file
        List<ModelNode> operations = builder.parseXmlResource(configFile);

        //Run the standard tests trying to execute the parsed operations.
        ModelTestUtils.checkFailedTransformedBootOperations(mainServices, modelVersion, operations, getConfig());

        boolean allowResourceTransformation = !isIgnoredResourceListAvailableAtRegistration()
                || modelVersion.getMajor() >= 3;
        try {
            checkCoreModelTransformation(mainServices,
                    modelVersion,
                    FIXER,
                    FIXER);
            if (!allowResourceTransformation) {
                Assert.fail("Resource transformation did not fail");
            }
        } catch (Exception e) {
            if (allowResourceTransformation) {
                throw e;
            }
        }

        if (modelVersion.getMajor() < 3) {
            // Get rid of launchCommand attribute to prove we can resource transform without it
            ModelNode op = Util.getWriteAttributeOperation(ADDRESS, LAUNCH_COMMAND.getName(), new ModelNode());
            mainServices.executeOperation(op, ModelController.OperationTransactionControl.COMMIT);
            checkCoreModelTransformation(mainServices,
                    modelVersion,
                    FIXER,
                    FIXER);
        }
    }

    private FailedOperationTransformationConfig getConfig() {
        FailedOperationTransformationConfig result;
        if (isFailExpressions()) {
            result = new FailedOperationTransformationConfig()
                    .addFailedAttribute(ADDRESS,
                        FailedOperationTransformationConfig.ChainedConfig.createBuilder(AGENT_PATH, HEAP_SIZE, JAVA_HOME, MAX_HEAP_SIZE,
                            PERMGEN_SIZE, MAX_PERMGEN_SIZE, STACK_SIZE, OPTIONS, ENVIRONMENT_VARIABLES,
                            ENV_CLASSPATH_IGNORED, AGENT_LIB, JAVA_AGENT, LAUNCH_COMMAND)
                            .addConfig(new FailedOperationTransformationConfig.RejectExpressionsConfig(AGENT_PATH, HEAP_SIZE, JAVA_HOME, MAX_HEAP_SIZE,
                                PERMGEN_SIZE, MAX_PERMGEN_SIZE, STACK_SIZE, OPTIONS, ENVIRONMENT_VARIABLES,
                                ENV_CLASSPATH_IGNORED, AGENT_LIB, JAVA_AGENT))
                            .addConfig(new FailedOperationTransformationConfig.NewAttributesConfig(LAUNCH_COMMAND))
                            .build());
        } else if (isFailLaunchCommand()) {
            result = new FailedOperationTransformationConfig()
                .addFailedAttribute(ADDRESS, new FailedOperationTransformationConfig.NewAttributesConfig(LAUNCH_COMMAND));
        } else {
            result = FailedOperationTransformationConfig.NO_FAILURES;
        }
        return result;
    }

    private boolean isFailLaunchCommand() {
        return modelVersion.getMajor() < 3;
    }
    private boolean isFailExpressions() {
        return modelVersion.getMajor() == 1 && modelVersion.getMinor() <=3;
    }
    private boolean isIgnoredResourceListAvailableAtRegistration() {
        return modelVersion.getMajor() >= 1 && modelVersion.getMinor() >= 4;
    }

    private ModelFixer FIXER = new ModelFixer() {

        @Override
        public ModelNode fixModel(ModelNode modelNode) {
            modelNode.remove(SOCKET_BINDING_GROUP);
            if (!isIgnoredResourceListAvailableAtRegistration()) {
                modelNode.get(SERVER_GROUP, "test", JVM, "full").remove(LAUNCH_COMMAND.getName());
            }
            return isFailExpressions() ? modelNode.resolve() : modelNode;
        }
    };
}

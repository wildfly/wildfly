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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING_GROUP;
import static org.jboss.as.host.controller.model.jvm.JvmAttributes.AGENT_PATH;
import static org.jboss.as.host.controller.model.jvm.JvmAttributes.ENVIRONMENT_VARIABLES;
import static org.jboss.as.host.controller.model.jvm.JvmAttributes.HEAP_SIZE;
import static org.jboss.as.host.controller.model.jvm.JvmAttributes.JAVA_HOME;
import static org.jboss.as.host.controller.model.jvm.JvmAttributes.MAX_HEAP_SIZE;
import static org.jboss.as.host.controller.model.jvm.JvmAttributes.MAX_PERMGEN_SIZE;
import static org.jboss.as.host.controller.model.jvm.JvmAttributes.OPTIONS;
import static org.jboss.as.host.controller.model.jvm.JvmAttributes.PERMGEN_SIZE;
import static org.jboss.as.host.controller.model.jvm.JvmAttributes.STACK_SIZE;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.core.model.test.AbstractCoreModelTest;
import org.jboss.as.core.model.test.KernelServices;
import org.jboss.as.core.model.test.KernelServicesBuilder;
import org.jboss.as.core.model.test.LegacyKernelServicesInitializer.TestControllerVersion;
import org.jboss.as.core.model.test.TestModelType;
import org.jboss.as.core.model.test.util.StandardServerGroupInitializers;
import org.jboss.as.core.model.test.util.TransformersTestParameters;
import org.jboss.as.model.test.FailedOperationTransformationConfig;
import org.jboss.as.model.test.ModelFixer;
import org.jboss.as.model.test.ModelTestUtils;
import org.jboss.dmr.ModelNode;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 *
 * @author <a href="http://jmesnil.net">Jeff Mesnil</a> (c) 2012 Red Hat, inc
 */
@RunWith(Parameterized.class)
public class JvmTransformersTestCase extends AbstractCoreModelTest {
    private final ModelVersion modelVersion;
    private final TestControllerVersion testControllerVersion;

    public JvmTransformersTestCase(TransformersTestParameters params) {
        this.modelVersion = params.getModelVersion();
        this.testControllerVersion = params.getTestControllerVersion();
    }

    @Parameters
    public static List<Object[]> parameters(){
        return TransformersTestParameters.setupVersions();
    }

    @Test
    public void jvmResourceWithExpressions() throws Exception {
        doJvmTransformer("domain-with-expressions.xml");
    }

    @Test
    public void jvmResourceWithoutExpressions() throws Exception {
        doJvmTransformer("domain-without-expressions.xml");
    }

    private void doJvmTransformer(String xmlResource) throws Exception {
        //Boot up empty controllers with the resources needed for the ops coming from the xml to work
        KernelServicesBuilder builder = createKernelServicesBuilder(TestModelType.DOMAIN)
                .setModelInitializer(StandardServerGroupInitializers.XML_MODEL_INITIALIZER, StandardServerGroupInitializers.XML_MODEL_WRITE_SANITIZER);

        StandardServerGroupInitializers.addServerGroupInitializers(builder.createLegacyKernelServicesBuilder(modelVersion, testControllerVersion));

        KernelServices mainServices = builder.build();
        assertTrue(mainServices.isSuccessfulBoot());
        KernelServices legacyServices = mainServices.getLegacyServices(modelVersion);
        assertTrue(legacyServices.isSuccessfulBoot());

        //Get the boot operations from the xml file
        List<ModelNode> operations = builder.parseXmlResource(xmlResource);

        //Run the standard tests trying to execute the parsed operations.
        ModelTestUtils.checkFailedTransformedBootOperations(mainServices, modelVersion, operations, getConfig());

        checkCoreModelTransformation(mainServices,
                modelVersion,
                new ModelFixer() {
                    @Override
                    public ModelNode fixModel(ModelNode modelNode) {
                        modelNode.remove(SOCKET_BINDING_GROUP);
                        return modelNode;
                    }
                },
                new ModelFixer() {
                    @Override
                    public ModelNode fixModel(ModelNode modelNode) {
                        modelNode.remove(SOCKET_BINDING_GROUP);
                        return isFailExpressions() ? modelNode.resolve() : modelNode;
                    }
                });
    }

    private FailedOperationTransformationConfig getConfig() {
        if (isFailExpressions()) {
            FailedOperationTransformationConfig config = new FailedOperationTransformationConfig()
                .addFailedAttribute(PathAddress.pathAddress(PathElement.pathElement("server-group", "test"), PathElement.pathElement("jvm", "default")),
                        new FailedOperationTransformationConfig.RejectExpressionsConfig(AGENT_PATH, HEAP_SIZE, JAVA_HOME, MAX_HEAP_SIZE,
                                PERMGEN_SIZE, MAX_PERMGEN_SIZE,
                                STACK_SIZE, OPTIONS, ENVIRONMENT_VARIABLES));

            return config;
        }
        return FailedOperationTransformationConfig.NO_FAILURES;
    }

    private boolean isFailExpressions() {
        return modelVersion.getMajor() == 1 && modelVersion.getMinor() <=3;
    }
}

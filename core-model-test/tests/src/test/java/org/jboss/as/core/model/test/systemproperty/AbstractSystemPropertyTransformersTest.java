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
package org.jboss.as.core.model.test.systemproperty;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.BOOT_TIME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING_GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SYSTEM_PROPERTY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;

import java.util.List;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.core.model.test.AbstractCoreModelTest;
import org.jboss.as.core.model.test.KernelServices;
import org.jboss.as.core.model.test.KernelServicesBuilder;
import org.jboss.as.core.model.test.LegacyKernelServicesInitializer;
import org.jboss.as.core.model.test.LegacyKernelServicesInitializer.TestControllerVersion;
import org.jboss.as.core.model.test.TestModelType;
import org.jboss.as.core.model.test.util.StandardServerGroupInitializers;
import org.jboss.as.core.model.test.util.TransformersTestParameters;
import org.jboss.as.model.test.FailedOperationTransformationConfig;
import org.jboss.as.model.test.ModelFixer;
import org.jboss.as.model.test.ModelTestUtils;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public abstract class AbstractSystemPropertyTransformersTest extends AbstractCoreModelTest {

    private final ModelVersion modelVersion;
    private final TestControllerVersion testControllerVersion;
    private final boolean serverGroup;
    private final ModelNode expectedUndefined;

    public AbstractSystemPropertyTransformersTest(TransformersTestParameters params, boolean serverGroup) {
        this.modelVersion = params.getModelVersion();
        this.testControllerVersion = params.getTestControllerVersion();
        this.serverGroup = serverGroup;
        this.expectedUndefined = getExpectedUndefined(params.getModelVersion());
    }

    @Parameters
    public static List<Object[]> parameters(){
        return TransformersTestParameters.setupVersions();
    }

    @Test
    public void testSystemPropertyTransformer() throws Exception {
        KernelServicesBuilder builder = createKernelServicesBuilder(TestModelType.DOMAIN)
                .setXmlResource(serverGroup ? "domain-servergroup-systemproperties.xml" : "domain-systemproperties.xml");
        if (serverGroup) {
            builder.setModelInitializer(StandardServerGroupInitializers.XML_MODEL_INITIALIZER, StandardServerGroupInitializers.XML_MODEL_WRITE_SANITIZER);
        }

        LegacyKernelServicesInitializer legacyInitializer = builder.createLegacyKernelServicesBuilder(modelVersion, testControllerVersion);
        if (serverGroup) {
            StandardServerGroupInitializers.addServerGroupInitializers(legacyInitializer);
        }

        KernelServices mainServices = builder.build();
        Assert.assertTrue(mainServices.isSuccessfulBoot());

        KernelServices legacyServices = mainServices.getLegacyServices(modelVersion);
        Assert.assertTrue(legacyServices.isSuccessfulBoot());

        ModelNode legacyModel = checkCoreModelTransformation(mainServices, modelVersion, StandardServerGroupInitializers.MODEL_FIXER, StandardServerGroupInitializers.MODEL_FIXER);
        ModelNode properties = legacyModel;
        if (serverGroup) {
            properties = legacyModel.get(SERVER_GROUP, "test");
        }
        properties = properties.get(SYSTEM_PROPERTY);
        Assert.assertEquals(expectedUndefined, properties.get("sys.prop.test.one", BOOT_TIME));
        Assert.assertEquals(1, properties.get("sys.prop.test.one", VALUE).asInt());
        Assert.assertEquals(new ModelNode(true), properties.get("sys.prop.test.two", BOOT_TIME));
        Assert.assertEquals(2, properties.get("sys.prop.test.two", VALUE).asInt());
        Assert.assertEquals(new ModelNode(false), properties.get("sys.prop.test.three", BOOT_TIME));
        Assert.assertEquals(3, properties.get("sys.prop.test.three", VALUE).asInt());
        Assert.assertEquals(expectedUndefined, properties.get("sys.prop.test.four", BOOT_TIME));
        Assert.assertFalse(properties.get("sys.prop.test.four", VALUE).isDefined());

        //Test the write attribute handler, the 'add' got tested at boot time
        PathAddress baseAddress = serverGroup ? PathAddress.pathAddress(PathElement.pathElement(SERVER_GROUP, "test")) : PathAddress.EMPTY_ADDRESS;
        PathAddress propAddress = baseAddress.append(SYSTEM_PROPERTY, "sys.prop.test.two");
        //value should just work
        ModelNode op = Util.getWriteAttributeOperation(propAddress, VALUE, new ModelNode("test12"));
        ModelTestUtils.checkOutcome(mainServices.executeOperation(modelVersion, mainServices.transformOperation(modelVersion, op)));
        Assert.assertEquals("test12", ModelTestUtils.getSubModel(legacyServices.readWholeModel(), propAddress).get(VALUE).asString());

        //boot time should be 'true' if undefined
        op = Util.getWriteAttributeOperation(propAddress, BOOT_TIME, new ModelNode());
        ModelTestUtils.checkOutcome(mainServices.executeOperation(modelVersion, mainServices.transformOperation(modelVersion, op)));
        Assert.assertTrue(ModelTestUtils.getSubModel(legacyServices.readWholeModel(), propAddress).get(BOOT_TIME).asBoolean());
        op = Util.getUndefineAttributeOperation(propAddress, BOOT_TIME);
        ModelTestUtils.checkOutcome(mainServices.executeOperation(modelVersion, mainServices.transformOperation(modelVersion, op)));
        Assert.assertTrue(ModelTestUtils.getSubModel(legacyServices.readWholeModel(), propAddress).get(BOOT_TIME).asBoolean());
    }

    @Test
    public void testSystemPropertiesWithExpressions() throws Exception {
        System.setProperty("sys.prop.test.one", "ONE");
        KernelServicesBuilder builder = createKernelServicesBuilder(TestModelType.DOMAIN);
        if (serverGroup) {
            builder.setModelInitializer(StandardServerGroupInitializers.XML_MODEL_INITIALIZER, StandardServerGroupInitializers.XML_MODEL_WRITE_SANITIZER);
        }

        LegacyKernelServicesInitializer legacyInitializer = builder.createLegacyKernelServicesBuilder(modelVersion, testControllerVersion);
        if (serverGroup) {
            StandardServerGroupInitializers.addServerGroupInitializers(legacyInitializer);
        }

        KernelServices mainServices = builder.build();
        Assert.assertTrue(mainServices.isSuccessfulBoot());

        //This passes since the boot currently does not invoke the transformers, and the rejected expression transformer which
        //would fail fails on the way out.
        KernelServices legacyServices = mainServices.getLegacyServices(modelVersion);
        Assert.assertTrue(legacyServices.isSuccessfulBoot());


        List<ModelNode> ops = builder.parseXmlResource(serverGroup ? "domain-servergroup-systemproperties-with-expressions.xml" : "domain-systemproperties-with-expressions.xml");

        FailedOperationTransformationConfig config;
        if (allowExpressions()) {
            config = FailedOperationTransformationConfig.NO_FAILURES;
        } else {
            PathAddress root = serverGroup ? PathAddress.pathAddress(PathElement.pathElement(SERVER_GROUP)) : PathAddress.EMPTY_ADDRESS;
            config = new FailedOperationTransformationConfig()
                .addFailedAttribute(root.append(PathElement.pathElement(SYSTEM_PROPERTY)), new FailedOperationTransformationConfig.RejectExpressionsConfig(VALUE));
        }
        ModelTestUtils.checkFailedTransformedBootOperations(mainServices, modelVersion, ops, config);

        checkCoreModelTransformation(mainServices, modelVersion, null, new ModelFixer() {
            @Override
            public ModelNode fixModel(ModelNode modelNode) {
                modelNode.remove(SOCKET_BINDING_GROUP);
                if (!allowExpressions()) {
                    modelNode =  modelNode.resolve();
                }
                return modelNode;
            }
        });
    }

    private ModelNode getExpectedUndefined(ModelVersion modelVersion){
        if (modelVersion.equals(ModelVersion.create(1, 4, 0))) {
            return new ModelNode();
        } else if (modelVersion.equals(ModelVersion.create(1, 2, 0)) || modelVersion.equals(ModelVersion.create(1, 3, 0))) {
            return new ModelNode(true);
        } else {
            throw new IllegalStateException("Not known model version " + modelVersion);
        }
    }

    private boolean allowExpressions() {
        return modelVersion.getMajor() >= 1 && modelVersion.getMinor() >= 4;
    }
}

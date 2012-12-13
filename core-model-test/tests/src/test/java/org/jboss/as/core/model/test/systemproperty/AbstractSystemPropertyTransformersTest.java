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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROFILE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING_GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SYSTEM_PROPERTY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;

import java.util.List;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.core.model.test.AbstractCoreModelTest;
import org.jboss.as.core.model.test.KernelServices;
import org.jboss.as.core.model.test.KernelServicesBuilder;
import org.jboss.as.core.model.test.LegacyKernelServicesInitializer;
import org.jboss.as.core.model.test.LegacyKernelServicesInitializer.TestControllerVersion;
import org.jboss.as.core.model.test.TestModelType;
import org.jboss.as.core.model.test.util.StandardServerGroupInitializers;
import org.jboss.as.core.model.test.util.TransformersTestParameters;
import org.jboss.as.model.test.ModelFixer;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public abstract class AbstractSystemPropertyTransformersTest extends AbstractCoreModelTest {

    private final ModelVersion modelVersion;
    private final TestControllerVersion testControllerVersion;
    private final String xmlResource;
    private final boolean serverGroup;
    private final ModelNode expectedUndefined;

    public AbstractSystemPropertyTransformersTest(SystemPropertyTransformersTestParameters params, String xmlResource, boolean serverGroup) {
        this.modelVersion = params.getModelVersion();
        this.testControllerVersion = params.getTestControllerVersion();
        this.xmlResource = xmlResource;
        this.serverGroup = serverGroup;
        this.expectedUndefined = params.getExpectedUndefined();
    }


    @Test
    public void testSystemPropertyTransformer() throws Exception {
        KernelServicesBuilder builder = createKernelServicesBuilder(TestModelType.DOMAIN)
                .setXmlResource(xmlResource);
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

        ModelNode legacyModel = checkCoreModelTransformation(mainServices, modelVersion, MODEL_FIXER, MODEL_FIXER);
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
    }

    private final ModelFixer MODEL_FIXER = new ModelFixer() {

        @Override
        public ModelNode fixModel(ModelNode modelNode) {
            modelNode.remove(SOCKET_BINDING_GROUP);
            modelNode.remove(PROFILE);
            return modelNode;
        }
    };

    static class SystemPropertyTransformersTestParameters extends TransformersTestParameters {
        private ModelNode expectedUndefined;
        public SystemPropertyTransformersTestParameters(TransformersTestParameters delegate, ModelNode expectedUndefined) {
            super(delegate);
            this.expectedUndefined = expectedUndefined;
        }

        public ModelNode getExpectedUndefined() {
            return expectedUndefined;
        }

    }

    static List<Object[]> createSystemPropertyTestTransformerParameters(){
        List<Object[]> params = TransformersTestParameters.setupVersions();
        for (Object[] element : params) {
            TransformersTestParameters param = (TransformersTestParameters)element[0];
            ModelNode expectedUndefined;
            if (param.getModelVersion().equals(ModelVersion.create(1, 4, 0))) {
                expectedUndefined = new ModelNode();
            } else if (param.getModelVersion().equals(ModelVersion.create(1, 2, 0))) {
                expectedUndefined = new ModelNode(true);
            } else {
                throw new IllegalStateException("Not known model version " + param.getModelVersion());
            }
            element[0] = new SystemPropertyTransformersTestParameters(param, expectedUndefined);
        }
        return params;
    }
}

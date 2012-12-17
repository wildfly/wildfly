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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROFILE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING_GROUP;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.core.model.test.AbstractCoreModelTest;
import org.jboss.as.core.model.test.KernelServices;
import org.jboss.as.core.model.test.KernelServicesBuilder;
import org.jboss.as.core.model.test.LegacyKernelServicesInitializer;
import org.jboss.as.core.model.test.LegacyKernelServicesInitializer.TestControllerVersion;
import org.jboss.as.core.model.test.ModelInitializer;
import org.jboss.as.core.model.test.ModelWriteSanitizer;
import org.jboss.as.core.model.test.TestModelType;
import org.jboss.as.core.model.test.util.TransformersTestParameters;
import org.jboss.as.model.test.ModelFixer;
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
        doJvmTransformer("domain-with-expressions.xml", testControllerVersion != TestControllerVersion.V7_1_2_FINAL);
    }

    @Test
    public void jvmResourceWithoutExpressions() throws Exception {
        doJvmTransformer("domain-without-expressions.xml", true);
    }

    private void doJvmTransformer(String xmlResource, boolean legacyMustBoot) throws Exception {
        KernelServicesBuilder builder = createKernelServicesBuilder(TestModelType.DOMAIN)
                .setXmlResource(xmlResource)
                .setModelInitializer(XML_MODEL_INITIALIZER, XML_MODEL_WRITE_SANITIZER);

        builder.createLegacyKernelServicesBuilder(modelVersion, testControllerVersion)
                .initializerCreateModelResource(PathAddress.EMPTY_ADDRESS, PathElement.pathElement(PROFILE, "test"), null)
                .initializerCreateModelResource(PathAddress.EMPTY_ADDRESS, PathElement.pathElement(SOCKET_BINDING_GROUP, "test-sockets"), new ModelNode().setEmptyObject());

        KernelServices mainServices = builder.build();
        assertTrue(mainServices.isSuccessfulBoot());

        KernelServices legacyServices = mainServices.getLegacyServices(modelVersion);
        if (legacyMustBoot) {
            assertTrue(legacyServices.isSuccessfulBoot());
            checkCoreModelTransformation(mainServices, modelVersion, MODEL_FIXER, MODEL_FIXER);
        } else {
            assertFalse(legacyServices.isSuccessfulBoot());
        }
    }

    /**
     * add "fake" profile and socket-binding-group so that the server-group specified in the XML is valid.
     */
    private static final ModelInitializer XML_MODEL_INITIALIZER = new ModelInitializer() {
        public void populateModel(Resource rootResource) {
            rootResource.registerChild(PathElement.pathElement(PROFILE, "test"), Resource.Factory.create());
            rootResource.registerChild(PathElement.pathElement(SOCKET_BINDING_GROUP, "test-sockets"), Resource.Factory.create());
        }
    };

    /**
     * Remove the resources that were added to the XML model.
     */
    private final ModelNode removeResources(ModelNode modelNode) {
        modelNode.remove(SOCKET_BINDING_GROUP);
        modelNode.remove(PROFILE);
        return modelNode;
    }

    private final ModelFixer MODEL_FIXER = new ModelFixer() {
        @Override
        public ModelNode fixModel(ModelNode modelNode) {
            return removeResources(modelNode);
        }
    };

    private final ModelWriteSanitizer XML_MODEL_WRITE_SANITIZER = new ModelWriteSanitizer() {
        @Override
        public ModelNode sanitize(ModelNode modelNode) {
            return removeResources(modelNode);
        }
    };
}

/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.core.model.test.servergroup;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROFILE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING_GROUP;

import java.util.List;

import junit.framework.Assert;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.core.model.test.AbstractCoreModelTest;
import org.jboss.as.core.model.test.KernelServices;
import org.jboss.as.core.model.test.KernelServicesBuilder;
import org.jboss.as.core.model.test.LegacyKernelServicesInitializer;
import org.jboss.as.core.model.test.TestModelType;
import org.jboss.as.core.model.test.util.ExcludeCommonOperations;
import org.jboss.as.core.model.test.util.StandardServerGroupInitializers;
import org.jboss.as.core.model.test.util.TransformersTestParameters;
import org.jboss.as.domain.controller.resources.ServerGroupResourceDefinition;
import org.jboss.as.model.test.FailedOperationTransformationConfig;
import org.jboss.as.model.test.ModelFixer;
import org.jboss.as.model.test.ModelTestControllerVersion;
import org.jboss.as.model.test.ModelTestUtils;
import org.jboss.dmr.ModelNode;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * Tests transformation of server-groups.
 *
 * @author Brian Stansberry (c) 2012 Red Hat Inc.
 */
@RunWith(Parameterized.class)
public class DomainServerGroupTransformersTestCase extends AbstractCoreModelTest {

    private final ModelVersion modelVersion;
    private final ModelTestControllerVersion testControllerVersion;

    @Parameterized.Parameters
    public static List<Object[]> parameters(){
        return TransformersTestParameters.setupVersions();
    }

    public DomainServerGroupTransformersTestCase(TransformersTestParameters params) {
        this.modelVersion = params.getModelVersion();
        this.testControllerVersion = params.getTestControllerVersion();
    }

    @Test
    public void testServerGroupsTransformer() throws Exception {

        boolean is71x = modelVersion.getMajor() == 1 && modelVersion.getMinor() < 4;
        KernelServicesBuilder builder = createKernelServicesBuilder(TestModelType.DOMAIN)
                .setModelInitializer(StandardServerGroupInitializers.XML_MODEL_INITIALIZER, StandardServerGroupInitializers.XML_MODEL_WRITE_SANITIZER)
                .createContentRepositoryContent("12345678901234567890")
                .createContentRepositoryContent("09876543210987654321")
                .setXmlResource(is71x ? "servergroup_1_3.xml" : "servergroup-with-expressions.xml");

        LegacyKernelServicesInitializer legacyInitializer =
                StandardServerGroupInitializers.addServerGroupInitializers(builder.createLegacyKernelServicesBuilder(modelVersion, testControllerVersion));
        if (is71x) {
            ExcludeCommonOperations.excludeBadOps_7_1_x(legacyInitializer);
        }

        KernelServices mainServices = builder.build();
        Assert.assertTrue(mainServices.isSuccessfulBoot());

        KernelServices legacyServices = mainServices.getLegacyServices(modelVersion);
        Assert.assertTrue(legacyServices.isSuccessfulBoot());

        checkCoreModelTransformation(mainServices, modelVersion, MODEL_FIXER, MODEL_FIXER);

    }

    @Test
    public void testRejectTransformers71x() throws Exception {

        if (modelVersion.getMajor() > 1 || modelVersion.getMinor() > 3) {
            return;
        }

        KernelServicesBuilder builder = createKernelServicesBuilder(TestModelType.DOMAIN)
                .setModelInitializer(StandardServerGroupInitializers.XML_MODEL_INITIALIZER, StandardServerGroupInitializers.XML_MODEL_WRITE_SANITIZER)
                .createContentRepositoryContent("12345678901234567890")
                .createContentRepositoryContent("09876543210987654321");

        // Add legacy subsystems
        LegacyKernelServicesInitializer legacyInitializer =
                StandardServerGroupInitializers.addServerGroupInitializers(builder.createLegacyKernelServicesBuilder(modelVersion, testControllerVersion));

        KernelServices mainServices = builder.build();

        List<ModelNode> ops = builder.parseXmlResource("servergroup_1_3-with-expressions.xml");
        ModelTestUtils.checkFailedTransformedBootOperations(mainServices, modelVersion, ops, new FailedOperationTransformationConfig()
                .addFailedAttribute(PathAddress.pathAddress(PathElement.pathElement(SERVER_GROUP)),
                        new FailedOperationTransformationConfig.RejectExpressionsConfig(
                                ServerGroupResourceDefinition.MANAGEMENT_SUBSYSTEM_ENDPOINT,
                                ServerGroupResourceDefinition.SOCKET_BINDING_PORT_OFFSET
                        ).setReadOnly(ServerGroupResourceDefinition.MANAGEMENT_SUBSYSTEM_ENDPOINT)));
    }

    private static final ModelFixer MODEL_FIXER = new ModelFixer() {

        @Override
        public ModelNode fixModel(ModelNode modelNode) {
            modelNode.remove(SOCKET_BINDING_GROUP);
            modelNode.remove(PROFILE);
            return modelNode;
        }
    };
}

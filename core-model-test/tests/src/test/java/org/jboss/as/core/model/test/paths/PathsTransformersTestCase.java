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
package org.jboss.as.core.model.test.paths;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PATH;

import java.util.List;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.services.path.PathResourceDefinition;
import org.jboss.as.core.model.test.AbstractCoreModelTest;
import org.jboss.as.core.model.test.KernelServices;
import org.jboss.as.core.model.test.KernelServicesBuilder;
import org.jboss.as.core.model.test.LegacyKernelServicesInitializer;
import org.jboss.as.core.model.test.TestModelType;
import org.jboss.as.core.model.test.util.StandardServerGroupInitializers;
import org.jboss.as.core.model.test.util.TransformersTestParameters;
import org.jboss.as.model.test.FailedOperationTransformationConfig;
import org.jboss.as.model.test.ModelTestUtils;
import org.jboss.as.model.test.ModelTestControllerVersion;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
@RunWith(Parameterized.class)
public class PathsTransformersTestCase extends AbstractCoreModelTest {
    private final ModelVersion modelVersion;
    private final ModelTestControllerVersion testControllerVersion;

    public PathsTransformersTestCase(TransformersTestParameters params) {
        this.modelVersion = params.getModelVersion();
        this.testControllerVersion = params.getTestControllerVersion();
    }

    @Parameters
    public static List<Object[]> parameters(){
        return TransformersTestParameters.setupVersions();
    }

    @Test
    public void testPathsTransformer() throws Exception {
        KernelServicesBuilder builder = createKernelServicesBuilder(TestModelType.DOMAIN)
                .setXmlResource("domain.xml");

        builder.createLegacyKernelServicesBuilder(modelVersion, testControllerVersion);

        KernelServices mainServices = builder.build();
        Assert.assertTrue(mainServices.isSuccessfulBoot());

        KernelServices legacyServices = mainServices.getLegacyServices(modelVersion);
        Assert.assertTrue(legacyServices.isSuccessfulBoot());

        checkCoreModelTransformation(mainServices, modelVersion);
    }

    @Test
    public void testRejectTransformers71x() throws Exception {

        if (modelVersion.getMajor() > 1 || modelVersion.getMinor() > 3) {
            return;
        }

        KernelServicesBuilder builder = createKernelServicesBuilder(TestModelType.DOMAIN);

        // Add legacy subsystems
        LegacyKernelServicesInitializer legacyInitializer =
                StandardServerGroupInitializers.addServerGroupInitializers(builder.createLegacyKernelServicesBuilder(modelVersion, testControllerVersion));
        // The 7.1.x descriptions are inaccurate so we can't validate the add ops against them
        legacyInitializer.setDontValidateOperations();

        KernelServices mainServices = builder.build();

        List<ModelNode> ops = builder.parseXmlResource("domain-expressions.xml");
        ModelTestUtils.checkFailedTransformedBootOperations(mainServices, modelVersion, ops, new FailedOperationTransformationConfig()
                .addFailedAttribute(PathAddress.pathAddress(PathElement.pathElement(PATH)),
                        new FailedOperationTransformationConfig.RejectExpressionsConfig(PathResourceDefinition.PATH)));
    }
}

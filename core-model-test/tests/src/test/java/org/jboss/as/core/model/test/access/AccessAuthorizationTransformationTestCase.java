/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.jboss.as.core.model.test.access;

import java.util.ArrayList;
import java.util.List;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.core.model.test.AbstractCoreModelTest;
import org.jboss.as.core.model.test.KernelServices;
import org.jboss.as.core.model.test.KernelServicesBuilder;
import org.jboss.as.core.model.test.LegacyKernelServicesInitializer;
import org.jboss.as.core.model.test.ModelInitializer;
import org.jboss.as.core.model.test.TestModelType;
import org.jboss.as.core.model.test.util.TransformersTestParameters;
import org.jboss.as.domain.management.CoreManagementResourceDefinition;
import org.jboss.as.domain.management.access.AccessAuthorizationResourceDefinition;
import org.jboss.as.model.test.ModelTestControllerVersion;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * Tests transformation of /core-service=management/access=authorization.
 *
 * @author Brian Stansberry (c) 2013 Red Hat Inc.
 */
@RunWith(Parameterized.class)
public class AccessAuthorizationTransformationTestCase extends AbstractCoreModelTest {

    private final ModelVersion modelVersion;
    private final ModelTestControllerVersion testControllerVersion;

    @Parameterized.Parameters
    public static List<Object[]> parameters(){
        //return TransformersTestParameters.setupVersions();
        //TODO remove this! - DEBUG ONLY
        List<Object[]> data = new ArrayList<Object[]>();
        data.add(new Object[] {new TransformersTestParameters(ModelVersion.create(2, 0, 0), ModelTestControllerVersion.MASTER)});
        return data;
    }

    public AccessAuthorizationTransformationTestCase(TransformersTestParameters params) {
        this.modelVersion = params.getModelVersion();
        this.testControllerVersion = params.getTestControllerVersion();
    }

    @Test
    public void testAllowNonRBAC() throws Exception {

        KernelServicesBuilder builder = createKernelServicesBuilder(TestModelType.DOMAIN)
                .setXmlResource("domain-transform-no-rbac-provider.xml");

        LegacyKernelServicesInitializer initializer = builder.createLegacyKernelServicesBuilder(modelVersion, testControllerVersion)
                .skipReverseControllerCheck();
        if (ModelVersion.compare(ModelVersion.create(1, 4, 0), modelVersion) > 0) {

        }

        KernelServices mainServices = builder.build();
        Assert.assertTrue(mainServices.isSuccessfulBoot());

        KernelServices legacyServices = mainServices.getLegacyServices(modelVersion);
        Assert.assertTrue(legacyServices.isSuccessfulBoot());

        ModelNode legacyModel = checkCoreModelTransformation(mainServices, modelVersion);

    }

    //TODO readd this test
    //@Test
    public void testRejectRBAC() throws Exception {
        if (ModelVersion.compare(ModelVersion.create(1, 4, 0), modelVersion) > 0) {
            return;
        }

        KernelServicesBuilder builder = createKernelServicesBuilder(TestModelType.DOMAIN)
                .setModelInitializer(new ModelInitializer() {
                    @Override
                    public void populateModel(Resource rootResource) {
                        Resource management = Resource.Factory.create();
                        rootResource.registerChild(PathElement.pathElement(ModelDescriptionConstants.CORE_SERVICE,
                                ModelDescriptionConstants.MANAGEMENT), management);
                        management.registerChild(PathElement.pathElement(ModelDescriptionConstants.ACCESS,
                                ModelDescriptionConstants.AUTHORIZATION), AccessAuthorizationResourceDefinition.createResource(null));
                    }
                }, null)
                .setXmlResource("domain-transform-rbac-provider.xml");

        builder.createLegacyKernelServicesBuilder(modelVersion, testControllerVersion)
                .addOperationValidationExclude("write-attribute",
                        PathAddress.pathAddress(CoreManagementResourceDefinition.PATH_ELEMENT, AccessAuthorizationResourceDefinition.PATH_ELEMENT))
                .skipReverseControllerCheck();

        KernelServices mainServices = builder.build();
        Assert.assertTrue(mainServices.isSuccessfulBoot());

        KernelServices legacyServices = mainServices.getLegacyServices(modelVersion);
        Assert.assertFalse(legacyServices.isSuccessfulBoot());

    }

}

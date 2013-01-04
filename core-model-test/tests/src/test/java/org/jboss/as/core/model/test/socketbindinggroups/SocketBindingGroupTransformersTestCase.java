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
package org.jboss.as.core.model.test.socketbindinggroups;

import java.util.List;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.core.model.test.AbstractCoreModelTest;
import org.jboss.as.core.model.test.KernelServices;
import org.jboss.as.core.model.test.KernelServicesBuilder;
import org.jboss.as.core.model.test.LegacyKernelServicesInitializer;
import org.jboss.as.core.model.test.LegacyKernelServicesInitializer.TestControllerVersion;
import org.jboss.as.core.model.test.TestModelType;
import org.jboss.as.core.model.test.util.TransformersTestParameters;
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
public class SocketBindingGroupTransformersTestCase extends AbstractCoreModelTest {

    private final ModelVersion modelVersion;
    private final TestControllerVersion testControllerVersion;

    @Parameters
    public static List<Object[]> parameters(){
        return TransformersTestParameters.setupVersions();
    }

    public SocketBindingGroupTransformersTestCase(TransformersTestParameters params) {
        this.modelVersion = params.getModelVersion();
        this.testControllerVersion = params.getTestControllerVersion();
    }

    @Test
    public void testSocketBindingGroupsTransformer() throws Exception {

        boolean below14 = modelVersion.getMajor() == 1 && modelVersion.getMinor() <= 3;

        KernelServicesBuilder builder = createKernelServicesBuilder(TestModelType.DOMAIN)
                .setXmlResource(below14 ? "domain-transformers-1.3.xml" : "domain.xml");

        LegacyKernelServicesInitializer legacyInit = builder.createLegacyKernelServicesBuilder(modelVersion, testControllerVersion);
        if (below14) {
            //The 7.1.2/3 operation validator does not like expressions very much
            legacyInit.setDontValidateOperations();
        }

        KernelServices mainServices = builder.build();
        Assert.assertTrue(mainServices.isSuccessfulBoot());

        KernelServices legacyServices = mainServices.getLegacyServices(modelVersion);
        Assert.assertTrue(legacyServices.isSuccessfulBoot());

        checkCoreModelTransformation(mainServices, modelVersion);
    }
}

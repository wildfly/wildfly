/*
* JBoss, Home of Professional Open Source.
* Copyright 2011, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.clustering.infinispan.subsystem;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;
import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.transform.OperationTransformer;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.subsystem.test.KernelServicesBuilder;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test cases for transformers used in the Infinispan subsystem
 *
 * @author <a href="tomaz.cerar@redhat.com">Tomaz Cerar</a>
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */

//@RunWith(BMUnitRunner.class)
public class InfinispanSubsystemTransformerTestCase extends OperationTestCaseBase {

    @Override
    protected String getSubsystemXml() throws IOException {
        return readResource("infinispan-transformer_1_4.xml");
    }

    @Test
//    @BMRule(name="Debugging support",
//            targetClass="^org.jboss.as.subsystem.test.SubsystemTestDelegate",
//            targetMethod="checkSubsystemModelTransformation",
//            targetLocation="AT INVOKE ModelTestUtils.compare",
//            binding="legacy:ModelNode = $1; transformed:ModelNode = $2",
//            condition="TRUE",
//            action="traceln(\"legacy = \" + legacy.toString() + \" transformed = \" + transformed.toString()")
    public void testTransformer_1_3_0() throws Exception {
        ModelVersion version = ModelVersion.create(1, 3);
        KernelServicesBuilder builder = createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT)
                .setSubsystemXml(getSubsystemXml());
        builder.createLegacyKernelServicesBuilder(null, version)
            .addMavenResourceURL("org.jboss.as:jboss-as-clustering-infinispan:7.1.2.Final");

        KernelServices mainServices = builder.build();
        Assert.assertTrue("main services did not boot", mainServices.isSuccessfulBoot()); ;

        checkSubsystemModelTransformation(mainServices, version);
    }

    @Test
//    @BMRule(name="Debugging support",
//            targetClass="^org.jboss.as.subsystem.test.SubsystemTestDelegate",
//            targetMethod="checkSubsystemModelTransformation",
//            targetLocation="AT INVOKE ModelTestUtils.compare",
//            binding="legacy:ModelNode = $1; transformed:ModelNode = $2",
//            condition="TRUE",
//            action="traceln(\"legacy = \" + legacy.toString() + \" transformed = \" + transformed.toString()")
    public void testRejectExpressions_1_3_0() throws Exception {
        // create builder for current subsystem version
        KernelServicesBuilder builder = createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT)
                .setSubsystemXml(getSubsystemXml());

        // create builder for legacy subsystem version
        ModelVersion version_1_3_0 = ModelVersion.create(1, 3, 0);
        builder.createLegacyKernelServicesBuilder(null, version_1_3_0)
            .addMavenResourceURL("org.jboss.as:jboss-as-clustering-infinispan:7.1.2.Final");

        KernelServices mainServices = builder.build();
        KernelServices legacyServices = mainServices.getLegacyServices(version_1_3_0);
        junit.framework.Assert.assertNotNull(legacyServices);

        // build an ADD command to add a cache store property using expression value
        ModelNode operation = getCacheStorePropertyAddOperation("maximal", "repl", ModelKeys.REPLICATED_CACHE, "some_property", "${some_property_value:new}");

        // perform operation on the 1.4.0 model
        ModelNode mainResult = mainServices.executeOperation(operation);
        assertEquals(mainResult.toJSONString(true), SUCCESS, mainResult.get(OUTCOME).asString());

        // perform transformed operation on the 1.3.0 model
        OperationTransformer.TransformedOperation transformedOperation = mainServices.transformOperation(version_1_3_0, operation);
        final ModelNode result = mainServices.executeOperation(version_1_3_0, transformedOperation);
        junit.framework.Assert.assertEquals("should reject the expression", FAILED, result.get(OUTCOME).asString());
    }
}

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
package org.jboss.as.clustering.jgroups.subsystem;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;
import static org.junit.Assert.assertEquals;

import java.io.IOException;

import junit.framework.Assert;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.transform.OperationTransformer;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.subsystem.test.KernelServicesBuilder;
import org.jboss.dmr.ModelNode;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Test cases for transformers used in the JGroups subsystem.
 *
 * @author <a href="tomaz.cerar@redhat.com">Tomaz Cerar</a>
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */

public class JGroupsSubsystemTransformerTestCase extends OperationTestCaseBase {

    protected String getSubsystemXml() throws IOException {
        return readResource("jgroups-transformer_1_1.xml") ;
    }

    /**
     * Tests transformation of model from 1.1.1 version into 1.1.0 version.
     *
     * This test does not pass because of an error in the 1.1.0 model:
     * The model entry 'protocols' in /subsystem=jgroups/stack=* is used
     * to store an ordered list of protocol names, but it is not registered
     * as an attribute, where as it is registered in the 1.1.1 model.
     * This breaks the test.
     *
     * @throws Exception
     */
    @Ignore
    @Test
    public void testTransformer_1_1_0() throws Exception {
        ModelVersion version = ModelVersion.create(1, 1, 0);
        KernelServicesBuilder builder = createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT)
                .setSubsystemXml(getSubsystemXml());
        builder.createLegacyKernelServicesBuilder(null, version)
            .addMavenResourceURL("org.jboss.as:jboss-as-clustering-jgroups:7.1.2.Final");

        KernelServices mainServices = builder.build();

        checkSubsystemModelTransformation(mainServices, version);
    }

    /**
     * Tests rejection of expressions in 1.1.0 model.
     *
     * @throws Exception
     */
    @Test
    public void testRejectExpressions_1_1_0() throws Exception {
        // create builder for current subsystem version
        KernelServicesBuilder builder = createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT)
                .setSubsystemXml(getSubsystemXml());

        // create builder for legacy subsystem version
        ModelVersion version_1_1_0 = ModelVersion.create(1, 1, 0);
        builder.createLegacyKernelServicesBuilder(null, version_1_1_0)
            .addMavenResourceURL("org.jboss.as:jboss-as-clustering-jgroups:7.1.2.Final");

        KernelServices mainServices = builder.build();
        KernelServices legacyServices = mainServices.getLegacyServices(version_1_1_0);
        Assert.assertNotNull(legacyServices);

        // build an ADD command to add a transport property using expression value
        ModelNode operation = getTransportPropertyAddOperation("maximal", "bundler_type", "${the_bundler_type:new}");

        // perform operation on the 1.1.1 model
        ModelNode mainResult = mainServices.executeOperation(operation);
        assertEquals(mainResult.toJSONString(true), SUCCESS, mainResult.get(OUTCOME).asString());

        // perform transformed operation on the 1.1.0 model
        OperationTransformer.TransformedOperation transformedOperation = mainServices.transformOperation(version_1_1_0, operation);
        final ModelNode result = mainServices.executeOperation(version_1_1_0, transformedOperation);
        Assert.assertEquals("should reject the expression", FAILED, result.get(OUTCOME).asString());

        // build an ADD command to add a protocol property using expression value
        ModelNode operation1 = getProtocolPropertyAddOperation("maximal", "MPING", "timeout", "${the_timeout:1000}");

        // perform operation on the 1.1.1 model
        ModelNode mainResult1 = mainServices.executeOperation(operation1);
        assertEquals(mainResult1.toJSONString(true), SUCCESS, mainResult1.get(OUTCOME).asString());

        // perform operation on the 1.1.0 model
        OperationTransformer.TransformedOperation transformedOperation1 = mainServices.transformOperation(version_1_1_0, operation1);
        final ModelNode result1 = mainServices.executeOperation(version_1_1_0, transformedOperation1);
        Assert.assertEquals("should reject the expression", FAILED, result1.get(OUTCOME).asString());

    }

}

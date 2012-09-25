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
package org.jboss.as.jpa.subsystem;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;

import java.io.IOException;

import junit.framework.Assert;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.transform.OperationTransformer;
import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.subsystem.test.KernelServicesBuilder;
import org.jboss.dmr.ModelNode;
import org.junit.Test;

/**
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class JPASubsystemTestCase extends AbstractSubsystemBaseTest {

    public JPASubsystemTestCase() {
        super(JPAExtension.SUBSYSTEM_NAME, new JPAExtension());
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        return
            "<subsystem xmlns=\"urn:jboss:domain:jpa:1.0\">" +
                "    <jpa default-datasource=\"\"/>" +
                "</subsystem>";
    }

    @Test
    public void testTransformers_1_1_0() throws Exception {
        System.setProperty("org.jboss.as.jpa.testBadExpr", "hello");

        try {
            ModelVersion oldVersion = ModelVersion.create(1, 1, 0);
            KernelServicesBuilder builder = createKernelServicesBuilder(null)
                    .setSubsystemXml(getSubsystemXml());
            builder.createLegacyKernelServicesBuilder(null, oldVersion)
                    .setExtensionClassName(JPAExtension.class.getName())
                    .addMavenResourceURL("org.jboss.as:jboss-as-jpa:7.1.2.Final");
            KernelServices mainServices = builder.build();
            KernelServices legacyServices = mainServices.getLegacyServices(oldVersion);
            Assert.assertNotNull(legacyServices);

            checkSubsystemModelTransformation(mainServices, oldVersion);

            final ModelNode operation = new ModelNode();
            operation.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
            operation.get(OP_ADDR).add(SUBSYSTEM, JPAExtension.SUBSYSTEM_NAME);
            operation.get(NAME).set(JPADefinition.DEFAULT_DATASOURCE.getName());
            operation.get(VALUE).set("${org.jboss.as.jpa.testBadExpr}");

            final ModelNode mainResult = mainServices.executeOperation(operation);
            System.out.println(mainResult);
            Assert.assertTrue(SUCCESS.equals(mainResult.get(OUTCOME).asString()));

            final OperationTransformer.TransformedOperation op = mainServices.transformOperation(oldVersion, operation);
            final ModelNode result = mainServices.executeOperation(oldVersion, op);
            Assert.assertEquals(FAILED, result.get(OUTCOME).asString());

        } finally {
            System.clearProperty("org.jboss.as.jpa.testBadExpr");
        }
    }
}

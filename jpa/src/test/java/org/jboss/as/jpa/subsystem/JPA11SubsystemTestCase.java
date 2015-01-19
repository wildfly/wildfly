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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.jpa.subsystem.JPADefinition.DEFAULT_DATASOURCE;

import java.io.IOException;
import java.util.List;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.jpa.messages.JpaLogger;
import org.jboss.as.model.test.FailedOperationTransformationConfig;
import org.jboss.as.model.test.ModelTestControllerVersion;
import org.jboss.as.model.test.ModelTestUtils;
import org.jboss.as.model.test.SingleClassFilter;
import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.subsystem.test.KernelServicesBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ValueExpression;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */

public class JPA11SubsystemTestCase extends AbstractSubsystemBaseTest {

    public JPA11SubsystemTestCase() {
        super(JPAExtension.SUBSYSTEM_NAME, new JPAExtension());
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        return readResource("subsystem-1.1.xml");
    }

    @Override
    protected String getSubsystemXsdPath() throws Exception {
        return "schema/jboss-as-jpa_1_1.xsd";
    }

    @Override
    protected String[] getSubsystemTemplatePaths() throws IOException {
        return new String[] {
                "/subsystem-templates/jpa.xml"
        };
    }

    @Test
    public void testTransformers7_1_2() throws Exception {
        testTransformers_1_1_0(ModelTestControllerVersion.V7_1_2_FINAL);
    }

    @Test
    public void testTransformers7_1_3() throws Exception {
        testTransformers_1_1_0(ModelTestControllerVersion.V7_1_3_FINAL);
    }

    private void testTransformers_1_1_0(ModelTestControllerVersion controllerVersion) throws Exception {
        ModelVersion oldVersion = ModelVersion.create(1, 1, 0);
        KernelServicesBuilder builder = createKernelServicesBuilder(null)
                .setSubsystemXmlResource("subsystem-1.1-no-expressions.xml");

        builder.createLegacyKernelServicesBuilder(null, controllerVersion, oldVersion)
                .setExtensionClassName(JPAExtension.class.getName())
                .addMavenResourceURL("org.jboss.as:jboss-as-jpa-spi:" + controllerVersion.getMavenGavVersion())
                .addMavenResourceURL("org.jboss.as:jboss-as-jpa:" + controllerVersion.getMavenGavVersion())
                .excludeFromParent(SingleClassFilter.createFilter(JpaLogger.class));

        KernelServices mainServices = builder.build();
        KernelServices legacyServices = mainServices.getLegacyServices(oldVersion);
        Assert.assertNotNull(legacyServices);
        Assert.assertTrue(mainServices.isSuccessfulBoot());
        Assert.assertTrue(legacyServices.isSuccessfulBoot());

        checkSubsystemModelTransformation(mainServices, oldVersion);
    }

    @Test
    public void testTransformersRejectExpressions7_1_2() throws Exception {
        testTransformers_1_1_0_RejectExpressions(ModelTestControllerVersion.V7_1_2_FINAL);
    }

    @Test
    public void testTransformersRejectExpressions7_1_3() throws Exception {
        testTransformers_1_1_0_RejectExpressions(ModelTestControllerVersion.V7_1_3_FINAL);
    }

    private void testTransformers_1_1_0_RejectExpressions(ModelTestControllerVersion controllerVersion) throws Exception {
        ModelVersion oldVersion = ModelVersion.create(1, 1, 0);
        KernelServicesBuilder builder = createKernelServicesBuilder(null);

        builder.createLegacyKernelServicesBuilder(null, controllerVersion, oldVersion)
                .setExtensionClassName(JPAExtension.class.getName())
                .addMavenResourceURL("org.jboss.as:jboss-as-jpa-spi:" + controllerVersion.getMavenGavVersion())
                .addMavenResourceURL("org.jboss.as:jboss-as-jpa:" + controllerVersion.getMavenGavVersion())
                .excludeFromParent(SingleClassFilter.createFilter(JpaLogger.class));


        KernelServices mainServices = builder.build();
        KernelServices legacyServices = mainServices.getLegacyServices(oldVersion);
        Assert.assertNotNull(legacyServices);
        Assert.assertTrue(mainServices.isSuccessfulBoot());
        Assert.assertTrue(legacyServices.isSuccessfulBoot());

        List<ModelNode> ops = builder.parseXmlResource("subsystem-1.1-transformers.xml");
        ModelTestUtils.checkFailedTransformedBootOperations(mainServices, oldVersion, ops,
                new FailedOperationTransformationConfig()
                    .addFailedAttribute(PathAddress.pathAddress(PathElement.pathElement(SUBSYSTEM, JPAExtension.SUBSYSTEM_NAME)),
                            new FailedOperationTransformationConfig.RejectExpressionsConfig(DEFAULT_DATASOURCE)
                                .setNotExpectedWriteFailure(JPADefinition.DEFAULT_EXTENDEDPERSISTENCE_INHERITANCE)));

        ModelNode legacyModel = legacyServices.readWholeModel().require(SUBSYSTEM).require(JPAExtension.SUBSYSTEM_NAME);
        Assert.assertEquals(1, legacyModel.keys().size());
        Assert.assertEquals("test-ds", legacyModel.get(DEFAULT_DATASOURCE.getName()).asString());

        ModelNode op = Util.getWriteAttributeOperation(PathAddress.pathAddress(PathElement.pathElement(SUBSYSTEM, JPAExtension.SUBSYSTEM_NAME)),
                CommonAttributes.DEFAULT_EXTENDEDPERSISTENCE_INHERITANCE,
                new ModelNode().set(new ValueExpression("${xxxx:SHALLOW}")));
        ModelTestUtils.checkFailed(mainServices.executeOperation(oldVersion, mainServices.transformOperation(oldVersion, op)));

        op = Util.getWriteAttributeOperation(PathAddress.pathAddress(PathElement.pathElement(SUBSYSTEM, JPAExtension.SUBSYSTEM_NAME)),
                CommonAttributes.DEFAULT_EXTENDEDPERSISTENCE_INHERITANCE,
                new ModelNode().set(new ValueExpression("SHALLOW")));
        ModelTestUtils.checkFailed(mainServices.executeOperation(oldVersion, mainServices.transformOperation(oldVersion, op)));

        op = Util.getUndefineAttributeOperation(PathAddress.pathAddress(PathElement.pathElement(SUBSYSTEM, JPAExtension.SUBSYSTEM_NAME)), CommonAttributes.DEFAULT_EXTENDEDPERSISTENCE_INHERITANCE);
        ModelTestUtils.checkOutcome(mainServices.executeOperation(oldVersion, mainServices.transformOperation(oldVersion, op)));
    }
}

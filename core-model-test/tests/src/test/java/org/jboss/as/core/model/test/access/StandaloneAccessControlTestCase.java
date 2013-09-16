/*
* JBoss, Home of Professional Open Source.
* Copyright 2013, Red Hat Middleware LLC, and individual contributors
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

import static org.jboss.as.controller.PathElement.pathElement;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ACCESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.APPLICATION_CLASSIFICATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.AUTHORIZATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CLASSIFICATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CONSTRAINT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CORE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CORE_SERVICE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SECURITY_REALM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SENSITIVITY_CLASSIFICATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VAULT_EXPRESSION;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.access.constraint.ApplicationTypeConfig;
import org.jboss.as.controller.access.constraint.SensitivityClassification;
import org.jboss.as.controller.access.management.ApplicationTypeAccessConstraintDefinition;
import org.jboss.as.controller.access.management.SensitiveTargetAccessConstraintDefinition;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.core.model.test.AbstractCoreModelTest;
import org.jboss.as.core.model.test.KernelServices;
import org.jboss.as.core.model.test.TestModelType;
import org.jboss.as.domain.management.access.ApplicationClassificationConfigResourceDefinition;
import org.jboss.as.domain.management.access.SensitivityResourceDefinition;
import org.jboss.as.model.test.ModelTestUtils;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;
import org.junit.Test;

/**
 * Simple test case to test the parsing and marshalling of the <access-control /> element within the standalone.xml
 * configuration.
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class StandaloneAccessControlTestCase extends AbstractCoreModelTest {
    private static final String SOCKET_CONFIG = SensitivityClassification.SOCKET_CONFIG.getName();

    @Test
    public void testConfiguration() throws Exception {
        //Initialize some additional constraints
        new SensitiveTargetAccessConstraintDefinition(new SensitivityClassification("play", "security-realm", true, true, true));
        new ApplicationTypeAccessConstraintDefinition(new ApplicationTypeConfig("play", "deployment", false));


        KernelServices kernelServices = createKernelServicesBuilder(TestModelType.STANDALONE)
                .setXmlResource("standalone.xml")
                .validateDescription()
                .build();
        Assert.assertTrue(kernelServices.isSuccessfulBoot());

        String marshalled = kernelServices.getPersistedSubsystemXml();
        ModelTestUtils.compareXml(ModelTestUtils.readResource(this.getClass(), "standalone.xml"), marshalled);

        //////////////////////////////////////////////////////////////////////////////////
        //Check that both set and undefined configured constraint settings get returned

        System.out.println(kernelServices.readWholeModel());
        //Sensitivity classification
        //This one is undefined
        ModelNode result = ModelTestUtils.checkOutcome(
                kernelServices.executeOperation(
                    Util.getReadAttributeOperation(PathAddress.pathAddress(
                        pathElement(CORE_SERVICE, MANAGEMENT),
                        pathElement(ACCESS, AUTHORIZATION),
                        pathElement(CONSTRAINT, SENSITIVITY_CLASSIFICATION),
                        pathElement(TYPE, CORE),
                        pathElement(CLASSIFICATION, SOCKET_CONFIG)), SensitivityResourceDefinition.CONFIGURED_REQUIRES_ADDRESSABLE.getName())));
        checkResultExists(result, new ModelNode());
        //This one is undefined
        result = ModelTestUtils.checkOutcome(
                kernelServices.executeOperation(
                    Util.getReadAttributeOperation(PathAddress.pathAddress(
                        pathElement(CORE_SERVICE, MANAGEMENT),
                        pathElement(ACCESS, AUTHORIZATION),
                        pathElement(CONSTRAINT, SENSITIVITY_CLASSIFICATION),
                        pathElement(TYPE, "play"),
                        pathElement(CLASSIFICATION, SECURITY_REALM)), SensitivityResourceDefinition.CONFIGURED_REQUIRES_ADDRESSABLE.getName())));
        checkResultExists(result, new ModelNode(false));

        //VaultExpression
        //It is defined
        PathAddress vaultAddress = PathAddress.pathAddress(
                pathElement(CORE_SERVICE, MANAGEMENT),
                pathElement(ACCESS, AUTHORIZATION),
                pathElement(CONSTRAINT, VAULT_EXPRESSION));
        result = ModelTestUtils.checkOutcome(
                kernelServices.executeOperation(
                        Util.getReadAttributeOperation(vaultAddress, SensitivityResourceDefinition.CONFIGURED_REQUIRES_READ.getName())));
        checkResultExists(result, new ModelNode(false));
        //Now undefine it and check again
        ModelTestUtils.checkOutcome(
                kernelServices.executeOperation(
                        Util.getUndefineAttributeOperation(vaultAddress, SensitivityResourceDefinition.CONFIGURED_REQUIRES_READ.getName())));
        result = ModelTestUtils.checkOutcome(
                kernelServices.executeOperation(
                        Util.getReadAttributeOperation(vaultAddress, SensitivityResourceDefinition.CONFIGURED_REQUIRES_READ.getName())));
        checkResultExists(result, new ModelNode());

        //Application classification
        //It is defined
        PathAddress applicationAddress = PathAddress.pathAddress(
                pathElement(CORE_SERVICE, MANAGEMENT),
                pathElement(ACCESS, AUTHORIZATION),
                pathElement(CONSTRAINT, APPLICATION_CLASSIFICATION),
                pathElement(TYPE, "play"),
                pathElement(CLASSIFICATION, "deployment"));
        result = ModelTestUtils.checkOutcome(
                kernelServices.executeOperation(
                        Util.getReadAttributeOperation(applicationAddress, ApplicationClassificationConfigResourceDefinition.CONFIGURED_APPLICATION.getName())));
        checkResultExists(result, new ModelNode(false));
        //Now undefine it and check again
        ModelTestUtils.checkOutcome(
                kernelServices.executeOperation(
                        Util.getUndefineAttributeOperation(applicationAddress, ApplicationClassificationConfigResourceDefinition.CONFIGURED_APPLICATION.getName())));
        result = ModelTestUtils.checkOutcome(
                kernelServices.executeOperation(
                        Util.getReadAttributeOperation(applicationAddress, ApplicationClassificationConfigResourceDefinition.CONFIGURED_APPLICATION.getName())));
        checkResultExists(result, new ModelNode());

    }

    private void checkResultExists(ModelNode result, ModelNode expected) {
        Assert.assertTrue(result.has(RESULT));
        Assert.assertEquals(expected, result.get(RESULT));
    }
}

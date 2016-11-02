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

package org.wildfly.extension.security.manager;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.model.test.FailedOperationTransformationConfig;
import org.jboss.as.model.test.ModelTestControllerVersion;
import org.jboss.as.model.test.ModelTestUtils;
import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.subsystem.test.KernelServicesBuilder;
import org.junit.Test;

/**
 * Security Manager version 3.0 subsystem tests.
 *
 * @author <a href="sguilhen@jboss.com">Stefan Guilhen</a>
 */
public class SecurityManagerSubsystem30TestCase extends AbstractSubsystemBaseTest {

    public SecurityManagerSubsystem30TestCase() {
        super(Constants.SUBSYSTEM_NAME, new SecurityManagerExtension());
    }


    @Override
    protected String getSubsystemXml() throws IOException {
        return readResource("security-manager-3.0.xml");
    }

    @Override
    protected String getSubsystemXsdPath() throws Exception {
        return "schema/wildfly-security-manager_3_0.xsd";
    }

    @Override
    protected String[] getSubsystemTemplatePaths() throws IOException {
        return new String[] {
                "/subsystem-templates/security-manager.xml"
        };
    }

    @Test
    public void testTransformersEAP70() throws Exception {
        testTransformers(ModelTestControllerVersion.EAP_7_0_0);
    }

    private void testTransformers(ModelTestControllerVersion controllerVersion) throws Exception {

        KernelServicesBuilder builder = createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT);
        ModelVersion version = ModelVersion.create(2, 0, 0);
        builder.createLegacyKernelServicesBuilder(AdditionalInitialization.MANAGEMENT, controllerVersion, version)
                .addMavenResourceURL("org.jboss.eap:wildfly-security-manager:" + controllerVersion.getMavenGavVersion());

        KernelServices mainServices = builder.build();
        assertTrue(mainServices.isSuccessfulBoot());
        KernelServices legacyServices = mainServices.getLegacyServices(version);
        assertNotNull(legacyServices);
        assertTrue(legacyServices.isSuccessfulBoot());

        PathAddress subsystemAddress = PathAddress.pathAddress(PathElement.pathElement(SUBSYSTEM, getMainSubsystemName()));
        ModelTestUtils.checkFailedTransformedBootOperations(mainServices, version,
                builder.parseXmlResource("security-manager-3.0.xml"),
                new FailedOperationTransformationConfig()
                        .addFailedAttribute(PathAddress.pathAddress(subsystemAddress, PathElement.pathElement(Constants.DEPLOYMENT_PERMISSIONS)),
                                FailedOperationTransformationConfig.REJECTED_RESOURCE));
    }
}

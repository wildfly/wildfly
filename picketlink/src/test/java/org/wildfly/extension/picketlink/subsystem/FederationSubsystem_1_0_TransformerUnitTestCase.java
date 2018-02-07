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

package org.wildfly.extension.picketlink.subsystem;

import java.util.List;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.model.test.FailedOperationTransformationConfig;
import org.jboss.as.model.test.ModelTestControllerVersion;
import org.jboss.as.model.test.ModelTestUtils;
import org.jboss.as.subsystem.test.AbstractSubsystemTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.subsystem.test.KernelServicesBuilder;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;
import org.junit.Test;
import org.wildfly.extension.picketlink.common.model.ModelElement;
import org.wildfly.extension.picketlink.federation.FederationExtension;
import org.wildfly.extension.picketlink.federation.model.keystore.KeyResourceDefinition;
import org.wildfly.extension.picketlink.federation.model.keystore.KeyStoreProviderResourceDefinition;

/**
 * @author Pedro Igor
 */
public class FederationSubsystem_1_0_TransformerUnitTestCase extends AbstractSubsystemTest {

    public FederationSubsystem_1_0_TransformerUnitTestCase() {
        super(FederationExtension.SUBSYSTEM_NAME, new FederationExtension());
    }

    @Test
    public void testTransformerEAP_6_3() throws Exception {
        ignoreThisTestIfEAPRepositoryIsNotReachable();
        testRejectionExpressions(ModelTestControllerVersion.EAP_6_3_0, "2.5.3.SP10-redhat-1");
    }

    private void testRejectionExpressions(ModelTestControllerVersion controllerVersion, String picketLinkJBossAs7Version) throws Exception {
        ModelVersion oldVersion = ModelVersion.create(1, 0, 0);
        KernelServicesBuilder builder = createKernelServicesBuilder(AdditionalInitialization.ADMIN_ONLY_HC);

        builder.createLegacyKernelServicesBuilder(AdditionalInitialization.ADMIN_ONLY_HC, controllerVersion, oldVersion)
                .setExtensionClassName(FederationExtension.class.getName())
                .configureReverseControllerCheck(AdditionalInitialization.ADMIN_ONLY_HC, null)
                .addMavenResourceURL("org.wildfly:wildfly-picketlink:" + controllerVersion.getMavenGavVersion())
                .addMavenResourceURL("org.picketlink.distribution:picketlink-jbas7:" + picketLinkJBossAs7Version)
                .addMavenResourceURL("org.picketlink:picketlink-federation:" + picketLinkJBossAs7Version)
                .dontPersistXml();

        KernelServices mainServices = builder.build();
        KernelServices legacyServices = mainServices.getLegacyServices(oldVersion);
        Assert.assertNotNull(legacyServices);
        Assert.assertTrue(mainServices.isSuccessfulBoot());
        Assert.assertTrue(legacyServices.isSuccessfulBoot());

        List<ModelNode> ops = builder.parseXmlResource("federation-subsystem-2.0.xml");

        ModelTestUtils.checkFailedTransformedBootOperations(mainServices, oldVersion, ops,
                new FailedOperationTransformationConfig()
                        .addFailedAttribute(PathAddress.pathAddress(
                                PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM, FederationExtension.SUBSYSTEM_NAME),
                                PathElement.pathElement(ModelElement.FEDERATION.getName()),
                                KeyStoreProviderResourceDefinition.INSTANCE.getPathElement(),
                                KeyResourceDefinition.INSTANCE.getPathElement()),
                                FailedOperationTransformationConfig.REJECTED_RESOURCE));
    }


}
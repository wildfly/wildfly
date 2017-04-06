/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

package org.jboss.as.mail.extension;

import static org.jboss.as.mail.extension.MailServerDefinition.CREDENTIAL_REFERENCE;
import static org.jboss.as.mail.extension.MailTransformers.MODEL_VERSION_EAP6X;
import static org.jboss.as.mail.extension.MailTransformers.MODEL_VERSION_EAP70;
import static org.jboss.as.model.test.ModelTestControllerVersion.EAP_6_4_0;
import static org.jboss.as.model.test.ModelTestControllerVersion.EAP_7_0_0;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.List;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.model.test.FailedOperationTransformationConfig;
import org.jboss.as.model.test.ModelTestControllerVersion;
import org.jboss.as.model.test.ModelTestUtils;
import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.subsystem.test.KernelServicesBuilder;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Tomaz Cerar (c) 2017 Red Hat Inc.
 */
public class MailTransformersTestCase extends AbstractSubsystemBaseTest {

    public MailTransformersTestCase() {
        super(MailExtension.SUBSYSTEM_NAME, new MailExtension());
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        return readResource("mail_3_0-transformers.xml");
    }

    @Test
    public void testTransformerEAP700() throws Exception {
        testTransformation(ModelTestControllerVersion.EAP_7_0_0, MODEL_VERSION_EAP70);
    }

    @Test
    public void testTransformerEAP640() throws Exception {
        testTransformation(ModelTestControllerVersion.EAP_6_4_0, MODEL_VERSION_EAP6X);
    }

    private KernelServices buildKernelServices(ModelTestControllerVersion controllerVersion, ModelVersion version, String... mavenResourceURLs) throws Exception {
        return this.buildKernelServices(this.getSubsystemXml(), controllerVersion, version, mavenResourceURLs);
    }

    private KernelServices buildKernelServices(String xml, ModelTestControllerVersion controllerVersion, ModelVersion version, String... mavenResourceURLs) throws Exception {
        KernelServicesBuilder builder = this.createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT).setSubsystemXml(xml);

        builder.createLegacyKernelServicesBuilder(AdditionalInitialization.MANAGEMENT, controllerVersion, version)
                .addMavenResourceURL(mavenResourceURLs)
                .skipReverseControllerCheck()
                .dontPersistXml();

        KernelServices services = builder.build();
        Assert.assertTrue(ModelTestControllerVersion.MASTER + " boot failed", services.isSuccessfulBoot());
        Assert.assertTrue(controllerVersion.getMavenGavVersion() + " boot failed", services.getLegacyServices(version).isSuccessfulBoot());
        return services;
    }
    private static String getMailGav(final ModelTestControllerVersion controller){
        String artifact = controller.getCoreVersion().equals(controller.getMavenGavVersion()) ? "jboss-as-mail" : "wildfly-mail";
        return controller.getMavenGroupId() + ":"+artifact+":" + controller.getMavenGavVersion();
    }
    private void testTransformation(final ModelTestControllerVersion controller, ModelVersion version) throws Exception {

        KernelServices services = this.buildKernelServices(controller, version, getMailGav(controller));

        // check that both versions of the legacy model are the same and valid
        checkSubsystemModelTransformation(services, version, null, false);

        ModelNode transformed = services.readTransformedModel(version);
        assertNotNull(transformed);
    }

    @Test
    public void testRejectingTransformersEAP_7_0_0() throws Exception {
        testRejectingTransformers(EAP_7_0_0, MODEL_VERSION_EAP70);
    }

    @Test
    public void testRejectingTransformersEAP_6_4_0() throws Exception {
        testRejectingTransformers(EAP_6_4_0, MODEL_VERSION_EAP6X);
    }

    private void testRejectingTransformers(ModelTestControllerVersion controllerVersion, ModelVersion targetVersion) throws Exception {
        //Boot up empty controllers with the resources needed for the ops coming from the xml to work
        KernelServicesBuilder builder = createKernelServicesBuilder(createAdditionalInitialization());
        builder.createLegacyKernelServicesBuilder(null, controllerVersion, targetVersion)
                .configureReverseControllerCheck(createAdditionalInitialization(), null)
                .addMavenResourceURL(getMailGav(controllerVersion))
                .dontPersistXml();

        KernelServices mainServices = builder.build();
        assertTrue(mainServices.isSuccessfulBoot());
        assertTrue(mainServices.getLegacyServices(targetVersion).isSuccessfulBoot());

        List<ModelNode> ops = builder.parseXmlResource("mail_3_0-reject.xml");
        PathAddress sessionAddress = PathAddress.pathAddress(MailExtension.SUBSYSTEM_PATH).append(MailExtension.MAIL_SESSION_PATH);
        ModelTestUtils.checkFailedTransformedBootOperations(mainServices, targetVersion, ops, new FailedOperationTransformationConfig()
                        .addFailedAttribute(sessionAddress.append("server"),
                                new FailedOperationTransformationConfig.NewAttributesConfig(
                                        CREDENTIAL_REFERENCE
                                )
                        ).addFailedAttribute(sessionAddress.append("custom"),
                            new FailedOperationTransformationConfig.NewAttributesConfig(
                                        CREDENTIAL_REFERENCE
                                )
                        )
                );
    }

    @Override
    protected AdditionalInitialization createAdditionalInitialization() {
        return new MailSubsystemTestBase.TransformersInitializer();
    }

    @Override
    public void testSchema() throws Exception {
        //
    }
}

/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.messaging.test;

import static org.jboss.as.messaging.MessagingExtension.VERSION_1_3_0;
import static org.jboss.as.messaging.MessagingExtension.VERSION_1_4_0;
import static org.jboss.as.messaging.test.MessagingDependencies.getHornetQDependencies;
import static org.jboss.as.messaging.test.MessagingDependencies.getMessagingGAV;
import static org.jboss.as.messaging.test.ModelFixers.PATH_FIXER;
import static org.jboss.as.model.test.ModelTestControllerVersion.EAP_6_2_0;
import static org.jboss.as.model.test.ModelTestControllerVersion.EAP_6_3_0;
import static org.jboss.as.model.test.ModelTestControllerVersion.EAP_6_4_0;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Properties;

import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.messaging.MessagingExtension;
import org.jboss.as.model.test.ModelFixer;
import org.jboss.as.model.test.ModelTestControllerVersion;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.subsystem.test.KernelServicesBuilder;
import org.junit.Test;

/**
 *  * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2012 Red Hat inc
 */
public class MessagingSubsystem30TestCase extends AbstractLegacySubsystemBaseTest {

    public MessagingSubsystem30TestCase() {
        super(MessagingExtension.SUBSYSTEM_NAME, new MessagingExtension());
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        return readResource("subsystem_3_0_expressions.xml");
    }

    @Override
    protected String getSubsystemXsdPath() throws Exception {
        return "schema/jboss-as-messaging_3_0.xsd";
    }

    @Override
    protected String[] getSubsystemTemplatePaths() throws IOException {
        return new String[] {
            "/subsystem-templates/messaging.xml",
            "/subsystem-templates/messaging-hornetq-colocated.xml"
        };
    }

    @Test
    @Override
    public void testSchemaOfSubsystemTemplates() throws Exception {
        super.testSchemaOfSubsystemTemplates();
    }

    @Override
    protected Properties getResolvedProperties() {
        Properties properties = new Properties();
        properties.put("messaging.cluster.user.name", "myClusterUser");
        properties.put("messaging.cluster.user.password", "myClusterPassword");
        return properties;
    }

    ////////////////////////////////////////
    //      Tests for EAP versions        //
    //                                    //
    // put most recent version at the top //
    ////////////////////////////////////////

    @Test
    public void testTransformersEAP_6_4_0() throws Exception {
        testTransformers(EAP_6_4_0, VERSION_1_4_0, PATH_FIXER);
    }

    @Test
    public void testRejectExpressionsEAP_6_4_0() throws Exception {
        KernelServicesBuilder builder = createKernelServicesBuilder(EAP_6_4_0, VERSION_1_4_0, PATH_FIXER, "empty_subsystem_3_0.xml");

        doTestRejectExpressions_1_4_0(builder);
    }

    @Test
    public void testTransformersEAP_6_3_0() throws Exception {
        testTransformers(EAP_6_3_0, VERSION_1_3_0, PATH_FIXER);
    }

    @Test
    public void testRejectExpressionsEAP_6_3_0() throws Exception {
        KernelServicesBuilder builder = createKernelServicesBuilder(EAP_6_3_0, VERSION_1_3_0, PATH_FIXER, "empty_subsystem_3_0.xml");

        doTestRejectExpressions_1_3_0(builder);
    }

    @Test
    public void testTransformersEAP_6_2_0() throws Exception {
        testTransformers(EAP_6_2_0, VERSION_1_3_0, PATH_FIXER);
    }

    @Test
    public void testRejectExpressionsEAP_6_2_0() throws Exception {
        KernelServicesBuilder builder = createKernelServicesBuilder(EAP_6_2_0, VERSION_1_3_0, PATH_FIXER, "empty_subsystem_3_0.xml");

        doTestRejectExpressions_1_3_0(builder);
    }

  private void doTestRejectExpressions_1_3_0(KernelServicesBuilder builder) throws Exception {
        KernelServices mainServices = builder.build();
        assertTrue(mainServices.isSuccessfulBoot());
        KernelServices legacyServices = mainServices.getLegacyServices(VERSION_1_3_0);
        assertNotNull(legacyServices);
        assertTrue(legacyServices.isSuccessfulBoot());
    }

    private void doTestRejectExpressions_1_4_0(KernelServicesBuilder builder) throws Exception {
        KernelServices mainServices = builder.build();
        assertTrue(mainServices.isSuccessfulBoot());
        KernelServices legacyServices = mainServices.getLegacyServices(VERSION_1_4_0);
        assertNotNull(legacyServices);
        assertTrue(legacyServices.isSuccessfulBoot());
    }

    private KernelServicesBuilder createKernelServicesBuilder(ModelTestControllerVersion controllerVersion, ModelVersion messagingVersion, ModelFixer fixer, String xmlFileName) throws IOException, XMLStreamException, ClassNotFoundException {
        KernelServicesBuilder builder = createKernelServicesBuilder(createAdditionalInitialization())
                .setSubsystemXmlResource(xmlFileName);
        // create builder for legacy subsystem version
        builder.createLegacyKernelServicesBuilder(createAdditionalInitialization(), controllerVersion, messagingVersion)
                .addMavenResourceURL(getMessagingGAV(controllerVersion))
                .configureReverseControllerCheck(createAdditionalInitialization(), fixer)
                .addMavenResourceURL(getHornetQDependencies(controllerVersion))
                .dontPersistXml();
        return builder;
    }

    private void testTransformers(ModelTestControllerVersion controllerVersion, ModelVersion messagingVersion, ModelFixer fixer) throws Exception {
        testTransformers(controllerVersion, messagingVersion, fixer, null);
    }

    private void testTransformers(ModelTestControllerVersion controllerVersion, ModelVersion messagingVersion, ModelFixer fixer, ModelFixer legacyModelFixer) throws Exception {
        //Boot up empty controllers with the resources needed for the ops coming from the xml to work
        KernelServicesBuilder builder = createKernelServicesBuilder(createAdditionalInitialization())
                .setSubsystemXmlResource("subsystem_3_0.xml");
        builder.createLegacyKernelServicesBuilder(createAdditionalInitialization(), controllerVersion, messagingVersion)
                .addMavenResourceURL(getMessagingGAV(controllerVersion))
                .addMavenResourceURL(getHornetQDependencies(controllerVersion))
                .configureReverseControllerCheck(createAdditionalInitialization(), fixer)
                .dontPersistXml();

        KernelServices mainServices = builder.build();
        assertTrue(mainServices.isSuccessfulBoot());
        assertTrue(mainServices.getLegacyServices(messagingVersion).isSuccessfulBoot());

        checkSubsystemModelTransformation(mainServices, messagingVersion, legacyModelFixer);
    }
}

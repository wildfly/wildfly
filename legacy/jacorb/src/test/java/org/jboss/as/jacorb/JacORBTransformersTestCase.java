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
package org.jboss.as.jacorb;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.jacorb.JacORBSubsystemDefinitions.INTEROP_CHUNK_RMI_VALUETYPES;
import static org.jboss.as.jacorb.JacORBSubsystemDefinitions.INTEROP_COMET;
import static org.jboss.as.jacorb.JacORBSubsystemDefinitions.INTEROP_INDIRECT_ENCODING_DISABLE;
import static org.jboss.as.jacorb.JacORBSubsystemDefinitions.INTEROP_IONA;
import static org.jboss.as.jacorb.JacORBSubsystemDefinitions.INTEROP_LAX_BOOLEAN_ENCODING;
import static org.jboss.as.jacorb.JacORBSubsystemDefinitions.INTEROP_STRICT_CHECK_ON_TC_CREATION;
import static org.jboss.as.jacorb.JacORBSubsystemDefinitions.INTEROP_SUN;
import static org.jboss.as.jacorb.JacORBSubsystemDefinitions.NAMING_EXPORT_CORBALOC;
import static org.jboss.as.jacorb.JacORBSubsystemDefinitions.NAMING_ROOT_CONTEXT;
import static org.jboss.as.jacorb.JacORBSubsystemDefinitions.ORB_CACHE_POA_NAMES;
import static org.jboss.as.jacorb.JacORBSubsystemDefinitions.ORB_CACHE_TYPECODES;
import static org.jboss.as.jacorb.JacORBSubsystemDefinitions.ORB_CONN_CLIENT_TIMEOUT;
import static org.jboss.as.jacorb.JacORBSubsystemDefinitions.ORB_CONN_MAX_MANAGED_BUF_SIZE;
import static org.jboss.as.jacorb.JacORBSubsystemDefinitions.ORB_CONN_MAX_SERVER_CONNECTIONS;
import static org.jboss.as.jacorb.JacORBSubsystemDefinitions.ORB_CONN_OUTBUF_CACHE_TIMEOUT;
import static org.jboss.as.jacorb.JacORBSubsystemDefinitions.ORB_CONN_OUTBUF_SIZE;
import static org.jboss.as.jacorb.JacORBSubsystemDefinitions.ORB_CONN_RETRIES;
import static org.jboss.as.jacorb.JacORBSubsystemDefinitions.ORB_CONN_RETRY_INTERVAL;
import static org.jboss.as.jacorb.JacORBSubsystemDefinitions.ORB_CONN_SERVER_TIMEOUT;
import static org.jboss.as.jacorb.JacORBSubsystemDefinitions.ORB_GIOP_MINOR_VERSION;
import static org.jboss.as.jacorb.JacORBSubsystemDefinitions.ORB_INIT_SECURITY;
import static org.jboss.as.jacorb.JacORBSubsystemDefinitions.ORB_INIT_TX;
import static org.jboss.as.jacorb.JacORBSubsystemDefinitions.ORB_NAME;
import static org.jboss.as.jacorb.JacORBSubsystemDefinitions.ORB_PRINT_VERSION;
import static org.jboss.as.jacorb.JacORBSubsystemDefinitions.ORB_USE_BOM;
import static org.jboss.as.jacorb.JacORBSubsystemDefinitions.ORB_USE_IMR;
import static org.jboss.as.jacorb.JacORBSubsystemDefinitions.POA_MONITORING;
import static org.jboss.as.jacorb.JacORBSubsystemDefinitions.POA_QUEUE_MAX;
import static org.jboss.as.jacorb.JacORBSubsystemDefinitions.POA_QUEUE_MIN;
import static org.jboss.as.jacorb.JacORBSubsystemDefinitions.POA_QUEUE_WAIT;
import static org.jboss.as.jacorb.JacORBSubsystemDefinitions.POA_REQUEST_PROC_MAX_THREADS;
import static org.jboss.as.jacorb.JacORBSubsystemDefinitions.POA_REQUEST_PROC_POOL_SIZE;
import static org.jboss.as.jacorb.JacORBSubsystemDefinitions.PROPERTIES;
import static org.jboss.as.jacorb.JacORBSubsystemDefinitions.SECURITY_ADD_COMPONENT_INTERCEPTOR;
import static org.jboss.as.jacorb.JacORBSubsystemDefinitions.SECURITY_CLIENT_REQUIRES;
import static org.jboss.as.jacorb.JacORBSubsystemDefinitions.SECURITY_CLIENT_SUPPORTS;
import static org.jboss.as.jacorb.JacORBSubsystemDefinitions.SECURITY_SECURITY_DOMAIN;
import static org.jboss.as.jacorb.JacORBSubsystemDefinitions.SECURITY_SERVER_REQUIRES;
import static org.jboss.as.jacorb.JacORBSubsystemDefinitions.SECURITY_SERVER_SUPPORTS;
import static org.jboss.as.jacorb.JacORBSubsystemDefinitions.SECURITY_SUPPORT_SSL;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.model.test.FailedOperationTransformationConfig;
import org.jboss.as.model.test.ModelFixer;
import org.jboss.as.model.test.ModelTestControllerVersion;
import org.jboss.as.model.test.ModelTestUtils;
import org.jboss.as.subsystem.test.AbstractSubsystemTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.subsystem.test.KernelServicesBuilder;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class JacORBTransformersTestCase extends AbstractSubsystemTest {

    public JacORBTransformersTestCase() {
        super(JacORBExtension.SUBSYSTEM_NAME, new JacORBExtension());
    }

    @Test
    public void testTransformers712() throws Exception {
        testTransformers(ModelTestControllerVersion.V7_1_2_FINAL);
    }

    @Test
    public void testTransformers713() throws Exception {
        testTransformers(ModelTestControllerVersion.V7_1_3_FINAL);
    }

    private void testTransformers(ModelTestControllerVersion controllerVersion) throws Exception {
        KernelServicesBuilder builder = createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT)
                .setSubsystemXml(readResource("subsystem-1.2.xml"));

        // Add legacy subsystems
        ModelVersion version_1_1_0 = ModelVersion.create(1, 1, 0);
        builder.createLegacyKernelServicesBuilder(null, controllerVersion, version_1_1_0)
                .addMavenResourceURL("org.jboss.as:jboss-as-jacorb:" + controllerVersion.getMavenGavVersion());

        KernelServices mainServices = builder.build();
        assertTrue(mainServices.isSuccessfulBoot());
        KernelServices legacyServices = mainServices.getLegacyServices(version_1_1_0);
        assertNotNull(legacyServices);
        assertTrue(legacyServices.isSuccessfulBoot());

        ModelNode transformed = checkSubsystemModelTransformation(mainServices, version_1_1_0).get(SUBSYSTEM, JacORBExtension.SUBSYSTEM_NAME);
        Assert.assertEquals("off", transformed.get("security").asString());
        List<ModelNode> properties = transformed.get(ModelDescriptionConstants.PROPERTIES).asList();
        Assert.assertEquals(1, properties.size());
        Assert.assertEquals("some_value", properties.get(0).get("some_property").asString());
    }

    @Test
    public void testTransformersSecurityIdentity712() throws Exception {
        testTransformersSecurityIdentity_1_1_0(ModelTestControllerVersion.V7_1_2_FINAL);
    }

    @Test
    public void testTransformersSecurityIdentity713() throws Exception {
        testTransformersSecurityIdentity_1_1_0(ModelTestControllerVersion.V7_1_3_FINAL);
    }

    private void testTransformersSecurityIdentity_1_1_0(ModelTestControllerVersion controllerVersion) throws Exception {
        KernelServicesBuilder builder = createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT)
                .setSubsystemXml(readResource("subsystem-1.2-security-identity.xml"));

        // Add legacy subsystems
        ModelVersion version_1_1_0 = ModelVersion.create(1, 1, 0);
        builder.createLegacyKernelServicesBuilder(AdditionalInitialization.MANAGEMENT, controllerVersion, version_1_1_0)
                .addMavenResourceURL("org.jboss.as:jboss-as-jacorb:" + controllerVersion.getMavenGavVersion());

        KernelServices mainServices = builder.build();
        assertTrue(mainServices.isSuccessfulBoot());
        KernelServices legacyServices = mainServices.getLegacyServices(version_1_1_0);
        assertNotNull(legacyServices);
        assertTrue(legacyServices.isSuccessfulBoot());

        ModelNode transformed = checkSubsystemModelTransformation(mainServices, version_1_1_0).get(SUBSYSTEM, JacORBExtension.SUBSYSTEM_NAME);
        Assert.assertEquals("on", transformed.get("security").asString());
        List<ModelNode> properties = transformed.get(ModelDescriptionConstants.PROPERTIES).asList();
        Assert.assertEquals(1, properties.size());
        Assert.assertEquals("some_value", properties.get(0).get("some_property").asString());
    }

    @Test
    public void testTransformersSecurityClient712() throws Exception {
        testTransformersSecurityClient_1_1_0(ModelTestControllerVersion.V7_1_2_FINAL);
    }

    @Test
    public void testTransformersSecurityClient713() throws Exception {
        testTransformersSecurityClient_1_1_0(ModelTestControllerVersion.V7_1_3_FINAL);
    }

    private void testTransformersSecurityClient_1_1_0(ModelTestControllerVersion controllerVersion) throws Exception {
        //security=client is not allowed on 7.1.2

        KernelServicesBuilder builder = createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT);

        // Add legacy subsystems
        ModelVersion version_1_1_0 = ModelVersion.create(1, 1, 0);
        builder.createLegacyKernelServicesBuilder(AdditionalInitialization.MANAGEMENT, controllerVersion, version_1_1_0)
                .addMavenResourceURL("org.jboss.as:jboss-as-jacorb:" + controllerVersion.getMavenGavVersion());

        KernelServices mainServices = builder.build();
        assertTrue(mainServices.isSuccessfulBoot());
        KernelServices legacyServices = mainServices.getLegacyServices(version_1_1_0);
        assertNotNull(legacyServices);
        assertTrue(legacyServices.isSuccessfulBoot());

        FailedOperationTransformationConfig config = new FailedOperationTransformationConfig()
        .addFailedAttribute(PathAddress.pathAddress(PathElement.pathElement(SUBSYSTEM, "jacorb")),
                new FailedOperationTransformationConfig.AttributesPathAddressConfig(JacORBSubsystemConstants.ORB_INIT_SECURITY) {

                    @Override
                    protected boolean isAttributeWritable(String attributeName) {
                        return true;
                    }

                    @Override
                    protected boolean checkValue(String attrName, ModelNode attribute, boolean isWriteAttribute) {
                        return attribute.asString().equals("client");
                    }

                    @Override
                    protected ModelNode correctValue(ModelNode toResolve, boolean isWriteAttribute) {
                        return new ModelNode("off");
                    }
                });

        ModelTestUtils.checkFailedTransformedBootOperations(mainServices, version_1_1_0, builder.parseXmlResource("subsystem-1.2-security-client.xml"), config);
        checkSubsystemModelTransformation(mainServices, version_1_1_0, new ModelFixer() {
            @Override
            public ModelNode fixModel(ModelNode modelNode) {
                //Set back to the value before the failed operation config kicked in
                Assert.assertEquals("off", modelNode.get(JacORBSubsystemConstants.ORB_INIT_SECURITY).asString());
                modelNode.get(JacORBSubsystemConstants.ORB_INIT_SECURITY).set("client");
                return modelNode;
            }
        });
    }


    @Test
    public void testTransformersRejectedExpressions712() throws Exception {
        testTransformersRejectedExpressions(ModelTestControllerVersion.V7_1_2_FINAL);
    }


    @Test
    public void testTransformersRejectedExpressions713() throws Exception {
        testTransformersRejectedExpressions(ModelTestControllerVersion.V7_1_3_FINAL);
    }

    private void testTransformersRejectedExpressions(ModelTestControllerVersion controllerVersion) throws Exception {

        KernelServicesBuilder builder = createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT);

        // Add legacy subsystems
        ModelVersion version_1_1_0 = ModelVersion.create(1, 1, 0);
        builder.createLegacyKernelServicesBuilder(AdditionalInitialization.MANAGEMENT, controllerVersion, version_1_1_0)
                .addMavenResourceURL("org.jboss.as:jboss-as-jacorb:" + controllerVersion.getMavenGavVersion());

        KernelServices mainServices = builder.build();
        assertTrue(mainServices.isSuccessfulBoot());
        KernelServices legacyServices = mainServices.getLegacyServices(version_1_1_0);
        assertNotNull(legacyServices);
        assertTrue(legacyServices.isSuccessfulBoot());

        FailedOperationTransformationConfig config = new FailedOperationTransformationConfig()
        .addFailedAttribute(PathAddress.pathAddress(PathElement.pathElement(SUBSYSTEM, "jacorb")),
                new FailedOperationTransformationConfig.RejectExpressionsConfig(ORB_NAME, ORB_PRINT_VERSION, ORB_USE_IMR, ORB_USE_BOM, ORB_CACHE_TYPECODES,
                        ORB_CACHE_POA_NAMES, ORB_GIOP_MINOR_VERSION, ORB_CONN_RETRIES, ORB_CONN_RETRY_INTERVAL, ORB_CONN_CLIENT_TIMEOUT,
                        ORB_CONN_SERVER_TIMEOUT, ORB_CONN_MAX_SERVER_CONNECTIONS, ORB_CONN_MAX_MANAGED_BUF_SIZE, ORB_CONN_OUTBUF_SIZE, ORB_CONN_OUTBUF_CACHE_TIMEOUT, ORB_INIT_SECURITY, ORB_INIT_TX, POA_MONITORING, POA_QUEUE_WAIT, POA_QUEUE_MIN,
                        POA_QUEUE_MAX, POA_REQUEST_PROC_POOL_SIZE, POA_REQUEST_PROC_MAX_THREADS, NAMING_ROOT_CONTEXT, NAMING_EXPORT_CORBALOC,
                        INTEROP_SUN, INTEROP_COMET, INTEROP_IONA, INTEROP_CHUNK_RMI_VALUETYPES, INTEROP_LAX_BOOLEAN_ENCODING,
                        INTEROP_INDIRECT_ENCODING_DISABLE, INTEROP_STRICT_CHECK_ON_TC_CREATION, PROPERTIES, SECURITY_SUPPORT_SSL, SECURITY_SECURITY_DOMAIN,
                        SECURITY_ADD_COMPONENT_INTERCEPTOR, SECURITY_CLIENT_SUPPORTS, SECURITY_CLIENT_REQUIRES, SECURITY_SERVER_SUPPORTS, SECURITY_SERVER_REQUIRES)
                );



        ModelTestUtils.checkFailedTransformedBootOperations(mainServices, version_1_1_0, builder.parseXmlResource("expressions-1.2.xml"), config);
    }

    @Test
    public void testTransformersIORSettings712() throws Exception {
        testTransformersIORSettings(ModelTestControllerVersion.V7_1_2_FINAL);
    }

    @Test
    public void testTransformersIORSettings713() throws Exception {
        testTransformersIORSettings(ModelTestControllerVersion.V7_1_3_FINAL);
    }

    private void testTransformersIORSettings(ModelTestControllerVersion controllerVersion) throws Exception {

        KernelServicesBuilder builder = createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT);

        ModelVersion version = ModelVersion.create(1, 1, 0);
        // Add legacy subsystems
        builder.createLegacyKernelServicesBuilder(AdditionalInitialization.MANAGEMENT, controllerVersion, version)
                .addMavenResourceURL("org.jboss.as:jboss-as-jacorb:" + controllerVersion.getMavenGavVersion());

        KernelServices mainServices = builder.build();
        assertTrue(mainServices.isSuccessfulBoot());
        KernelServices legacyServices = mainServices.getLegacyServices(version);
        assertNotNull(legacyServices);
        assertTrue(legacyServices.isSuccessfulBoot());

        ModelTestUtils.checkFailedTransformedBootOperations(
                mainServices,
                version,
                builder.parseXmlResource("subsystem-1.4-ior-settings.xml"),
                new FailedOperationTransformationConfig()
                        .addFailedAttribute(
                                PathAddress.pathAddress(JacORBSubsystemResource.INSTANCE.getPathElement(),
                                        JacORBSubsystemResource.INSTANCE.getPathElement()),
                                FailedOperationTransformationConfig.REJECTED_RESOURCE));
    }
}
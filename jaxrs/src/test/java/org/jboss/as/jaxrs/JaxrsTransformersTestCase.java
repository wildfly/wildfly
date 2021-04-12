/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2020, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.jaxrs;

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
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @author <a href="mailto:ema@rehdat.com>Jim Ma</a>
 * @author <a href="mailto:alessio.soldano@jboss.com>Alessio Soldano</a>
 * @author <a href="rsigal@redhat.com>Ron Sigal</a>
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class JaxrsTransformersTestCase extends AbstractSubsystemBaseTest {

    private static final String JAXRS_1_0 = "jaxrs-1.0.xml";
    private static final String JAXRS_2_0 = "jaxrs-2.0-expressions.xml";

    public JaxrsTransformersTestCase() {
        super(JaxrsExtension.SUBSYSTEM_NAME, new JaxrsExtension());
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        return readResource(JAXRS_2_0);
    }

    @Test
    public void testTransformers700() throws Exception {
        testTransformers(ModelTestControllerVersion.EAP_7_0_0, JAXRS_1_0);
    }

    @Test
    public void testTransformers710() throws Exception {
        testTransformers(ModelTestControllerVersion.EAP_7_1_0, JAXRS_1_0);
    }

    @Test
    public void testTransformers720() throws Exception {
        testTransformers(ModelTestControllerVersion.EAP_7_2_0, JAXRS_1_0);
    }

    @Test
    public void testTransformers730() throws Exception {
        testTransformers(ModelTestControllerVersion.EAP_7_3_0, JAXRS_1_0);
    }

    @Test
    public void testRejections700() throws Exception {
        testRejections(ModelTestControllerVersion.EAP_7_0_0, JAXRS_2_0, getFailedTransformationConfig());
    }

    @Test
    public void testRejections710() throws Exception {
        testRejections(ModelTestControllerVersion.EAP_7_1_0, JAXRS_2_0, getFailedTransformationConfig());
    }

    @Test
    public void testRejections720() throws Exception {
        testRejections(ModelTestControllerVersion.EAP_7_2_0, JAXRS_2_0, getFailedTransformationConfig());
    }

    @Test
    public void testRejections730() throws Exception {
        testRejections(ModelTestControllerVersion.EAP_7_3_0, "jaxrs-2.0-expressions.xml", getFailedTransformationConfig());
    }

    @SuppressWarnings("SameParameterValue")
    private void testTransformers(final ModelTestControllerVersion controllerVersion, final String xmlFileName) throws Exception {
        final ModelVersion version = controllerVersion.getSubsystemModelVersion(getMainSubsystemName());
        // create builder for current subsystem version
        KernelServicesBuilder builder = createKernelServicesBuilder(createAdditionalInitialization()).setSubsystemXmlResource(xmlFileName);

        // create builder for legacy subsystem version
        builder.createLegacyKernelServicesBuilder(null, controllerVersion, version)
                .addMavenResourceURL(controllerVersion.getMavenGroupId() + ":wildfly-jaxrs:" + controllerVersion.getMavenGavVersion())
                .configureReverseControllerCheck(AdditionalInitialization.MANAGEMENT, null);

        KernelServices mainServices = builder.build();
        KernelServices legacyServices = mainServices.getLegacyServices(version);

        Assert.assertNotNull(legacyServices);
        Assert.assertTrue("main services did not boot", mainServices.isSuccessfulBoot());
        Assert.assertTrue(legacyServices.isSuccessfulBoot());

        checkSubsystemModelTransformation(mainServices, version);
    }

    @SuppressWarnings("SameParameterValue")
    private void testRejections(final ModelTestControllerVersion controllerVersion, final String xmlFileName, final FailedOperationTransformationConfig config) throws Exception {
        final ModelVersion version = controllerVersion.getSubsystemModelVersion(getMainSubsystemName());
        // create builder for current subsystem version
        KernelServicesBuilder builder = createKernelServicesBuilder(createAdditionalInitialization());

        // create builder for legacy subsystem version
        builder.createLegacyKernelServicesBuilder(null, controllerVersion, version)
                .addMavenResourceURL(controllerVersion.getMavenGroupId() + ":wildfly-jaxrs:" + controllerVersion.getMavenGavVersion())
                .configureReverseControllerCheck(AdditionalInitialization.MANAGEMENT, null)
                .dontPersistXml();

        KernelServices mainServices = builder.build();
        KernelServices legacyServices = mainServices.getLegacyServices(version);

        Assert.assertNotNull(legacyServices);
        Assert.assertTrue("main services did not boot", mainServices.isSuccessfulBoot());
        Assert.assertTrue(legacyServices.isSuccessfulBoot());

        List<ModelNode> xmlOps = builder.parseXmlResource(xmlFileName);
        ModelTestUtils.checkFailedTransformedBootOperations(mainServices, version, xmlOps, config);
    }

    private FailedOperationTransformationConfig getFailedTransformationConfig() {
        PathAddress subsystemAddress = PathAddress.pathAddress(JaxrsExtension.SUBSYSTEM_PATH);
        return new FailedOperationTransformationConfig()
                .addFailedAttribute(subsystemAddress,
                        new FailedOperationTransformationConfig.NewAttributesConfig(
                                JaxrsAttribute.JAXRS_2_0_REQUEST_MATCHING,
                                JaxrsAttribute.RESTEASY_ADD_CHARSET,
                                JaxrsAttribute.RESTEASY_BUFFER_EXCEPTION_ENTITY,
                                JaxrsAttribute.RESTEASY_DISABLE_HTML_SANITIZER,
                                JaxrsAttribute.RESTEASY_DISABLE_PROVIDERS,
                                JaxrsAttribute.RESTEASY_DOCUMENT_EXPAND_ENTITY_REFERENCES,
                                JaxrsAttribute.RESTEASY_DOCUMENT_SECURE_DISABLE_DTDS,
                                JaxrsAttribute.RESTEASY_DOCUMENT_SECURE_PROCESSING_FEATURE,
                                JaxrsAttribute.RESTEASY_GZIP_MAX_INPUT,
                                JaxrsAttribute.RESTEASY_JNDI_RESOURCES,
                                JaxrsAttribute.RESTEASY_LANGUAGE_MAPPINGS,
                                JaxrsAttribute.RESTEASY_MEDIA_TYPE_MAPPINGS,
                                JaxrsAttribute.RESTEASY_MEDIA_TYPE_PARAM_MAPPING,
                                JaxrsAttribute.RESTEASY_PREFER_JACKSON_OVER_JSONB,
                                JaxrsAttribute.RESTEASY_PROVIDERS,
                                JaxrsAttribute.RESTEASY_RFC7232_PRECONDITIONS,
                                JaxrsAttribute.RESTEASY_ROLE_BASED_SECURITY,
                                JaxrsAttribute.RESTEASY_SECURE_RANDOM_MAX_USE,
                                JaxrsAttribute.RESTEASY_USE_BUILTIN_PROVIDERS,
                                JaxrsAttribute.RESTEASY_USE_CONTAINER_FORM_PARAMS,
                                JaxrsAttribute.RESTEASY_WIDER_REQUEST_MATCHING
                        ));
    }

}

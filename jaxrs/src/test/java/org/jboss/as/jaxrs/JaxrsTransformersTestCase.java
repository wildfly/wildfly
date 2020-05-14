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

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.RunningMode;
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
 */
public class JaxrsTransformersTestCase extends AbstractSubsystemBaseTest {

    public JaxrsTransformersTestCase() {
        super(JaxrsExtension.SUBSYSTEM_NAME, new JaxrsExtension());
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        return readResource("jaxrs-2.0.xml");
    }

    @Test
    public void testTransformers720() throws Exception {
        testTransformers_2_0_to_1_0(ModelTestControllerVersion.EAP_7_2_0);
    }

    @Test
    public void testRejections_1_0_0() throws Exception {
        testRejections_2_0_0(ModelTestControllerVersion.EAP_7_2_0);
    }

    /**
     * Tests transformation of model from version 2.0 to version 1.0.
     */
    private void testTransformers_2_0_to_1_0(ModelTestControllerVersion controllerVersion) throws Exception {
        // create builder for current subsystem version
        KernelServicesBuilder builder = createKernelServicesBuilder(createAdditionalInitialization()).setSubsystemXmlResource("jaxrs-1.0.xml");

        // create builder for legacy subsystem version
        builder.createLegacyKernelServicesBuilder(null, controllerVersion, JaxrsExtension.MODEL_VERSION_1_0_0)
        .addMavenResourceURL("org.jboss.eap:wildfly-jaxrs:" + controllerVersion.getMavenGavVersion())
        .configureReverseControllerCheck(AdditionalInitialization.MANAGEMENT, null);

        KernelServices mainServices = builder.build();
        KernelServices legacyServices = mainServices.getLegacyServices(JaxrsExtension.MODEL_VERSION_1_0_0);

        Assert.assertNotNull(legacyServices);
        Assert.assertTrue("main services did not boot", mainServices.isSuccessfulBoot());
        Assert.assertTrue(legacyServices.isSuccessfulBoot());

        checkSubsystemModelTransformation(mainServices, JaxrsExtension.MODEL_VERSION_1_0_0);
    }

    /**
     * Tests rejections of attributes introduced in model version 2.0.
     */
    private void testRejections_2_0_0(ModelTestControllerVersion controllerVersion) throws Exception {
        // create builder for current subsystem version
        KernelServicesBuilder builder = createKernelServicesBuilder(createAdditionalInitialization());

        // create builder for legacy subsystem version
        builder.createLegacyKernelServicesBuilder(null, controllerVersion, JaxrsExtension.MODEL_VERSION_1_0_0)
        .addMavenResourceURL("org.jboss.eap:wildfly-jaxrs:" + controllerVersion.getMavenGavVersion())
        .configureReverseControllerCheck(AdditionalInitialization.MANAGEMENT, null)
        .dontPersistXml();

        KernelServices mainServices = builder.build();
        KernelServices legacyServices = mainServices.getLegacyServices(JaxrsExtension.MODEL_VERSION_1_0_0);

        Assert.assertNotNull(legacyServices);
        Assert.assertTrue("main services did not boot", mainServices.isSuccessfulBoot());
        Assert.assertTrue(legacyServices.isSuccessfulBoot());

        List<ModelNode> xmlOps = builder.parseXmlResource("jaxrs-2.0.xml");
        ModelTestUtils.checkFailedTransformedBootOperations(mainServices, JaxrsExtension.MODEL_VERSION_1_0_0, xmlOps, getFailedTransformationConfig());
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

    protected AdditionalInitialization createAdditionalInitialization() {
        return new AdditionalInitialization() {
            @Override
            protected RunningMode getRunningMode() {
                return RunningMode.ADMIN_ONLY;
            }
        };
    }

}

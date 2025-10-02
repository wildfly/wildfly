/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jaxrs.cfg;

import java.net.URI;
import java.util.Map;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.integration.jaxrs.cfg.resources.TestApplication;
import org.jboss.as.test.integration.jaxrs.cfg.resources.TestError;
import org.jboss.as.test.integration.jaxrs.cfg.resources.TestErrorResource;
import org.jboss.as.test.integration.jaxrs.cfg.resources.TestErrors;
import org.jboss.as.test.shared.util.AssumeTestGroupUtil;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests the {@code resteasy-document-secure-processing-feature} attribute works as expected.
 * <p>
 * Note that order used for the methods is for performance reasons of only having to reload the server once. If a reload
 * becomes not required, the sequence is not important.
 * </p>
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@RunAsClient
@ServerSetup(ResteasyDocumentSecureProcessingFeatureTestCase.WriteAttributesServerSetupTask.class)
// @Tag("jakarta.xml.binding")
public class ResteasyDocumentSecureProcessingFeatureTestCase extends AbstractResteasyAttributeTest {

    static class WriteAttributesServerSetupTask extends AbstractWriteAttributesServerSetupTask {
        public WriteAttributesServerSetupTask() {
            // These are deprecated attributes, however they do block the resteasy-secure-processing-feature
            super(Map.of("resteasy-document-expand-entity-references", ModelNode.TRUE, "resteasy-document-secure-disableDTDs", ModelNode.FALSE));
        }
    }

    private static final String EXPONENTIAL_ENTITY = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<!DOCTYPE testErrors [\n"
            + " <!ENTITY testError \"testError\">\n"
            + " <!ELEMENT testErrors (#PCDATA)>\n"
            + " <!ENTITY testError1 \"&testError;&testError;&testError;&testError;&testError;&testError;&testError;&testError;&testError;&testError;\">\n"
            + " <!ENTITY testError2 \"&testError1;&testError1;&testError1;&testError1;&testError1;&testError1;&testError1;&testError1;&testError1;&testError1;\">\n"
            + " <!ENTITY testError3 \"&testError2;&testError2;&testError2;&testError2;&testError2;&testError2;&testError2;&testError2;&testError2;&testError2;\">\n"
            + " <!ENTITY testError4 \"&testError3;&testError3;&testError3;&testError3;&testError3;&testError3;&testError3;&testError3;&testError3;&testError3;\">\n"
            + " <!ENTITY testError5 \"&testError4;&testError4;&testError4;&testError4;&testError4;&testError4;&testError4;&testError4;&testError4;&testError4;\">\n"
            + "]>\n"
            + "<testErrors><testError>&testError5;</testError></testErrors>";

    @ArquillianResource
    private URI uri;

    public ResteasyDocumentSecureProcessingFeatureTestCase() {
        super("resteasy-document-secure-processing-feature");
    }

    @Deployment(testable = false)
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class, ResteasyDocumentSecureProcessingFeatureTestCase.class.getSimpleName() + ".war")
                .addClasses(TestApplication.class, TestErrors.class, TestError.class, TestErrorResource.class);
    }

    @Test
    @InSequence(1)
    public void checkDefault() {
        try (Client client = ClientBuilder.newClient()) {
            try (Response response = client.target(uriBuilder()).request().post(Entity.xml(EXPONENTIAL_ENTITY))) {
                Assert.assertEquals(500, response.getStatus());
            }
        }
    }

    @Test
    @InSequence(2)
    public void checkFalse() throws Exception {
        AssumeTestGroupUtil.assumeSecurityManagerDisabled();
        // Java 24+ when disabling XMLConstants.FEATURE_SECURE_PROCESSING, it doesn't really make the values for
        // properties like jdk.xml.entityExpansionLimit unlimited, see https://docs.oracle.com/en/java/javase/25/docs/api/java.xml/module-summary.html#Properties
        // For now we'll just disable this test for Java 24+. This property/attribute may just need to be disabled.
        // A review of these properties needs to be done in RESTEasy. Then we need to review the attributes in WildFly.
        org.jboss.as.test.shared.AssumeTestGroupUtil.assumeJDKVersionBefore(24);
        writeAttribute(ModelNode.FALSE);
        try (Client client = ClientBuilder.newClient()) {
            try (Response response = client.target(uriBuilder()).request().post(Entity.xml(EXPONENTIAL_ENTITY))) {
                Assert.assertEquals(200, response.getStatus());
                final TestErrors errors = response.readEntity(TestErrors.class);
                // We should have one 1 error, with a very large value so we'll just ensure we have one error
                Assert.assertEquals(1, errors.getTestErrors().size());
            }
        }
    }

    private UriBuilder uriBuilder() {
        return UriBuilder.fromUri(uri).path("/test/xml");
    }
}

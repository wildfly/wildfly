/*
 * Copyright (C) 2019 Red Hat, inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */
package org.jboss.as.test.integration.jaxrs.cfg;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.jaxrs.JaxrsAttribute;
import org.jboss.as.jaxrs.JaxrsConstants;
import org.jboss.as.test.integration.jaxrs.packaging.war.WebXml;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.dmr.Property;
import org.jboss.dmr.ValueExpression;
import org.jboss.resteasy.plugins.providers.FileProvider;
import org.jboss.resteasy.plugins.providers.SourceProvider;
import org.jboss.resteasy.plugins.providers.StringTextStar;
import org.jboss.resteasy.plugins.server.servlet.ResteasyContextParameters;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests for setting RESTEasy context parameters using the Wildfly management model.
 *
 * @author <a href="rsigal@redhat.com">Ron Sigal</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup(ResteasyAttributeTestCase.AttributeTestCaseDeploymentSetup.class)
public class ResteasyAttributeTestCase {

    private static ModelControllerClient modelControllerClient;
    private static final Map<AttributeDefinition, ModelNode> expectedValues = new HashMap<AttributeDefinition, ModelNode>();
    private static final Map<ModelType, Class<?>> modelTypeMap = new HashMap<ModelType, Class<?>>();
    private static Client jaxrsClient;

    @ArquillianResource
    private static URL url;

    static {
        modelTypeMap.put(ModelType.BOOLEAN, Boolean.class);
        modelTypeMap.put(ModelType.INT, Integer.class);
        modelTypeMap.put(ModelType.STRING, String.class);
        modelTypeMap.put(ModelType.LIST, String.class);
        modelTypeMap.put(ModelType.OBJECT, String.class);
    }

    private static final ModelNode ADDRESS = Operations.createAddress("subsystem", "jaxrs");
    private static final String OUTCOME = "outcome";
    private static final String SUCCESS = "success";
    private static final String FAILED = "failed";
    private static final String RESTEASY_MEDIA_TYPE_PARAM_MAPPING_CONTEXT_VALUE = "resteasy.media.type.param.mapping.context.value";

    private static final ModelNode VALUE_EXPRESSION_BOOLEAN_TRUE = new ModelNode(new ValueExpression("${rest.test.dummy:true}"));
    private static final ModelNode VALUE_EXPRESSION_BOOLEAN_FALSE = new ModelNode(new ValueExpression("${rest.test.dummy:false}"));
    private static final ModelNode VALUE_EXPRESSION_INT = new ModelNode(new ValueExpression("${rest.test.dummy:1717}"));
    private static final ModelNode VALUE_EXPRESSION_STRING = new ModelNode(new ValueExpression("${rest.test.dummy:xyz}"));
    private static final ModelNode VALUE_EXPRESSION_FILENAME_1 = new ModelNode(new ValueExpression("${rest.test.dummy:" + FileProvider.class.getName() + "}"));
    private static final ModelNode VALUE_EXPRESSION_FILENAME_2 = new ModelNode(new ValueExpression("${rest.test.dummy:" + SourceProvider.class.getName() + "}"));
    private static final ModelNode VALUE_EXPRESSION_SPANISH = new ModelNode(new ValueExpression("${rest.test.dummy:es-es}"));
    private static final ModelNode VALUE_EXPRESSION_APPLICATION_UNUSUAL = new ModelNode(new ValueExpression("${rest.test.dummy:application/unusual}"));
    private static final ModelNode VALUE_EXPRESSION_RESOURCE = new ModelNode(new ValueExpression("${rest.test.dummy:java:global/jaxrsnoap/" + EJB_Resource1.class.getSimpleName() + "}"));
    private static final ModelNode VALUE_EXPRESSION_PROVIDER = new ModelNode(new ValueExpression("${rest.test.dummy:" + StringTextStar.class.getName() + "}"));

    static class AttributeTestCaseDeploymentSetup implements ServerSetupTask {

        @Override
        public void setup(final ManagementClient managementClient, final String containerId) throws Exception {
            modelControllerClient = managementClient.getControllerClient();
            setAttributeValues();
        }

        @Override
        public void tearDown(final ManagementClient managementClient, final String containerId) throws Exception {
            resetAttributeValues();
        }
    }
    /**
     * Define the deployment
     *
     * @return The deployment archive
     */
    @Deployment
    public static Archive<?> createDeployment() throws Exception {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "jaxrsnoap.war");
        war.addClasses(ResteasyAttributeResource.class);
        war.addClasses(EJB_Resource1.class, EJB_Resource2.class);

        // Put "resteasy.media.type.param.mapping" in web.xml to verify that it overrides value set in Wildfly management model.
        war.addAsWebInfResource(WebXml.get(
                        "<context-param>\n" +
                        "    <param-name>resteasy.media.type.param.mapping</param-name>\n" +
                        "    <param-value>" + RESTEASY_MEDIA_TYPE_PARAM_MAPPING_CONTEXT_VALUE + "</param-value>\n" +
                        "</context-param>" +
                        "<servlet-mapping>\n" +
                        "    <servlet-name>javax.ws.rs.core.Application</servlet-name>\n" +
                        "        <url-pattern>/myjaxrs/*</url-pattern>\n" +
                        "    </servlet-mapping>\n" +
                "\n"), "web.xml");
        return war;
    }

    @BeforeClass
    public static void beforeClass() throws IOException {
        jaxrsClient = ClientBuilder.newClient();
    }

    @AfterClass
    public static void afterClass() throws IOException {
        jaxrsClient.close();
    }

    //////////////////////////////////////////////////////////////////////////////////////
    /////////////////////            Configuration methods            ////////////////////
    //////////////////////////////////////////////////////////////////////////////////////

    /*
     * Set attributes to testable values.
     *
     * Note that StringTextStar is added to 'resteasy.providers" to compensate for the fact that
     * "resteasy.use.builtin.providers" is set to "false".
     */
    static void setAttributeValues() throws IOException {
        setAttributeValue(JaxrsAttribute.JAXRS_2_0_REQUEST_MATCHING, VALUE_EXPRESSION_BOOLEAN_TRUE);
        setAttributeValue(JaxrsAttribute.RESTEASY_ADD_CHARSET, VALUE_EXPRESSION_BOOLEAN_FALSE);
        setAttributeValue(JaxrsAttribute.RESTEASY_BUFFER_EXCEPTION_ENTITY, VALUE_EXPRESSION_BOOLEAN_FALSE);
        setAttributeValue(JaxrsAttribute.RESTEASY_DISABLE_HTML_SANITIZER, VALUE_EXPRESSION_BOOLEAN_TRUE);

        ModelNode list = new ModelNode().setEmptyList();
        list.add(VALUE_EXPRESSION_FILENAME_1);
        list.add(VALUE_EXPRESSION_FILENAME_2);
        setAttributeValue(JaxrsAttribute.RESTEASY_DISABLE_PROVIDERS, list);
        list.clear();

        setAttributeValue(JaxrsAttribute.RESTEASY_DOCUMENT_EXPAND_ENTITY_REFERENCES, VALUE_EXPRESSION_BOOLEAN_TRUE);
        setAttributeValue(JaxrsAttribute.RESTEASY_DOCUMENT_SECURE_DISABLE_DTDS, VALUE_EXPRESSION_BOOLEAN_FALSE);
        setAttributeValue(JaxrsAttribute.RESTEASY_DOCUMENT_SECURE_PROCESSING_FEATURE, VALUE_EXPRESSION_BOOLEAN_FALSE);
        setAttributeValue(JaxrsAttribute.RESTEASY_GZIP_MAX_INPUT, VALUE_EXPRESSION_INT);

        list.add(VALUE_EXPRESSION_RESOURCE);
        list.add("java:global/jaxrsnoap/" + EJB_Resource2.class.getSimpleName());
        setAttributeValue(JaxrsAttribute.RESTEASY_JNDI_RESOURCES, list);
        list.clear();

        ModelNode map = new ModelNode();
        map.add(new Property("en", new ModelNode("en-US")));
        map.add(new Property("es", VALUE_EXPRESSION_SPANISH));
        map.add(new Property("fr", new ModelNode("fr")));
        setAttributeValue(JaxrsAttribute.RESTEASY_LANGUAGE_MAPPINGS, map);
        map.clear();

        map.add(new Property("unusual", VALUE_EXPRESSION_APPLICATION_UNUSUAL));
        map.add(new Property("xml", new ModelNode("application/xml")));
        setAttributeValue(JaxrsAttribute.RESTEASY_MEDIA_TYPE_MAPPINGS, map);
        map.clear();

        setAttributeValue(JaxrsAttribute.RESTEASY_PREFER_JACKSON_OVER_JSONB, VALUE_EXPRESSION_BOOLEAN_TRUE);
        setAttributeValue(JaxrsAttribute.RESTEASY_MEDIA_TYPE_PARAM_MAPPING, VALUE_EXPRESSION_STRING);

        list.add(VALUE_EXPRESSION_PROVIDER);
        setAttributeValue(JaxrsAttribute.RESTEASY_PROVIDERS, list);
        list.clear();

        setAttributeValue(JaxrsAttribute.RESTEASY_RFC7232_PRECONDITIONS, VALUE_EXPRESSION_BOOLEAN_TRUE);
        setAttributeValue(JaxrsAttribute.RESTEASY_ROLE_BASED_SECURITY, VALUE_EXPRESSION_BOOLEAN_TRUE);
        setAttributeValue(JaxrsAttribute.RESTEASY_SECURE_RANDOM_MAX_USE, VALUE_EXPRESSION_INT);
        setAttributeValue(JaxrsAttribute.RESTEASY_USE_BUILTIN_PROVIDERS, VALUE_EXPRESSION_BOOLEAN_FALSE);
        setAttributeValue(JaxrsAttribute.RESTEASY_USE_CONTAINER_FORM_PARAMS, VALUE_EXPRESSION_BOOLEAN_TRUE);
        setAttributeValue(JaxrsAttribute.RESTEASY_WIDER_REQUEST_MATCHING, VALUE_EXPRESSION_BOOLEAN_TRUE);
    }

    /*
     * Unset attributes.
     */
    static void resetAttributeValues() throws IOException {
        for (AttributeDefinition attribute : JaxrsAttribute.ATTRIBUTES) {
            undefineAttribute(attribute);
        }
    }

    static void setAttributeValue(AttributeDefinition attribute, ModelNode value) throws IOException {
        ModelNode expectedValue = null;
        ModelNode resolvedValue = value.resolve();
        switch (attribute.getType()) {

            case LIST:
                expectedValue = textifyList(resolvedValue);
                break;

            case OBJECT:
                expectedValue = textifyMap(resolvedValue);
                break;

            default:
                expectedValue = resolvedValue;
                break;
        }
        expectedValues.put(attribute, expectedValue);
        ModelNode op = Operations.createWriteAttributeOperation(ADDRESS, attribute.getName(), value);
        ModelNode result = modelControllerClient.execute(op);
        Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());
    }

    static void undefineAttribute(AttributeDefinition attribute) throws IOException {
        ModelNode op = Operations.createUndefineAttributeOperation(ADDRESS, attribute.getName());
        ModelNode result = modelControllerClient.execute(op);
        Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());
    }

    private static ModelNode textifyList(ModelNode modelNode) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (ModelNode value : modelNode.asList()) {
            if (first) {
                first = false;
            } else {
                sb.append(",");
            }
            sb.append(value.asString());
        }
        return new ModelNode(sb.toString());
    }

    private static ModelNode textifyMap(ModelNode modelNode) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (String key : modelNode.asObject().keys()) {
            ModelNode value = modelNode.asObject().get(key);
            if (first) {
                first = false;
            } else {
                sb.append(",");
            }
            sb.append(key + ":" + value.asString());
        }
        return new ModelNode(sb.toString());
    }

    //////////////////////////////////////////////////////////////////////////////////////
    /////////////////////            Test methods                     ////////////////////
    //////////////////////////////////////////////////////////////////////////////////////
    /**
     * Verify that all values are passed correctly to RESTEasy.
     */
    @Test
    public void testAttributes() throws IOException {
        WebTarget target = jaxrsClient.target(url.toString() + "myjaxrs/attribute");
        for (AttributeDefinition attribute : JaxrsAttribute.ATTRIBUTES) {
            testAttribute(target, attribute);
        }
    }

    void testAttribute(WebTarget target, AttributeDefinition attribute) {
        String resteasyName = attribute.getName();
        if (resteasyName.equals(JaxrsConstants.RESTEASY_PREFER_JACKSON_OVER_JSONB)) {
            resteasyName = ResteasyContextParameters.RESTEASY_PREFER_JACKSON_OVER_JSONB;
        } else {
            resteasyName = changeHyphensToDots(resteasyName);
        }
        Response response = target.path(resteasyName).request().get();
        Assert.assertEquals(200, response.getStatus());
        Object result = response.readEntity(modelTypeMap.get(attribute.getType()));

        switch (attribute.getType()) {

            case BOOLEAN:
                Assert.assertEquals(expectedValues.get(attribute).asBoolean(), result);
                return;

            case INT:
                Assert.assertEquals(expectedValues.get(attribute).asInt(), result);
                return;

            case STRING:
                Assert.assertEquals(expectedValues.get(attribute).asString(), result);
                return;

            case LIST:
                Assert.assertEquals(expectedValues.get(attribute).asString(), result);
                return;

            case OBJECT:
                Assert.assertEquals(expectedValues.get(attribute).asString(), result);
                return;

            default:
                Assert.fail("Unexpected ModelNode type");
        }
     }

    String changeHyphensToDots(String attribute) {
        return attribute.replace("-", ".");
    }

    /**
     * Verify that syntactically incorrect values get kicked out.
     */
    @Test
    public void testBadSyntax() throws Exception {
        for (AttributeDefinition attribute : JaxrsAttribute.ATTRIBUTES) {
            // RESTEasy accepts any string for "resteasy-media-type-param-mapping".
            if (JaxrsAttribute.RESTEASY_MEDIA_TYPE_PARAM_MAPPING.equals(attribute)) {
                continue;
            }
            ModelNode op = Operations.createWriteAttributeOperation(ADDRESS, attribute.getName(), mangleAttribute(attribute));
            ModelNode result = modelControllerClient.execute(op);
            Assert.assertEquals(FAILED, result.get(OUTCOME).asString());
        }
    }

    static String mangleAttribute(AttributeDefinition attribute) {
        switch (attribute.getType()) {

            case BOOLEAN:
                return "abc";

            case INT:
                return "def";

            case LIST:
                return "ghi";

            case OBJECT:
                return "jkl";

            default:
                throw new RuntimeException("Unexpected ModelNode type: " + attribute.getType());
        }
    }

    /**
     * Verify that updating parameters doesn't affect existing deployments.
     */
    @Test
    public void testExistingDeploymentsUnchanged() throws IOException {

        // Get current value of "resteasy.add.charset".
        WebTarget base = jaxrsClient.target(url.toString() + "myjaxrs/attribute");
        Builder builder = base.path(changeHyphensToDots(JaxrsAttribute.RESTEASY_ADD_CHARSET.getName())).request();
        Response response = builder.get();
        Assert.assertEquals(200, response.getStatus());
        boolean oldValue = response.readEntity(boolean.class);

        // Update "resteasy.add.charset" value to a new value.
        ModelNode op = Operations.createWriteAttributeOperation(ADDRESS, JaxrsAttribute.RESTEASY_ADD_CHARSET.getName(), !oldValue);
        ModelNode result = modelControllerClient.execute(op);
        Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());

        // Verify that value of "resteasy.add.charset" hasn't changed in existing deployment.
        response = builder.get();
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals(oldValue, response.readEntity(boolean.class));

        // Reset "resteasy.add.charset" to original value.
        op = Operations.createWriteAttributeOperation(ADDRESS, JaxrsAttribute.RESTEASY_ADD_CHARSET.getName(), oldValue);
        result = modelControllerClient.execute(op);
        Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());
    }
}

/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jaxrs.cfg;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Invocation.Builder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.jaxrs.JaxrsAttribute;
import org.jboss.as.jaxrs.JaxrsConstants;
import org.jboss.as.test.integration.jaxrs.packaging.war.WebXml;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.as.test.shared.SnapshotRestoreSetupTask;
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

    private static final Map<AttributeDefinition, ModelNode> expectedValues = new HashMap<AttributeDefinition, ModelNode>();
    private static final Map<ModelType, Class<?>> modelTypeMap = new HashMap<ModelType, Class<?>>();
    private static Client jaxrsClient;

    @ArquillianResource
    private static URL url;
    @ArquillianResource
    private ManagementClient client;

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

    static class AttributeTestCaseDeploymentSetup extends SnapshotRestoreSetupTask {
        @Override
        protected void doSetup(final ManagementClient client, final String containerId) throws Exception {
            setAttributeValues(client.getControllerClient());
            ServerReload.reloadIfRequired(client);
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
                        "    <servlet-name>jakarta.ws.rs.core.Application</servlet-name>\n" +
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
    static void setAttributeValues(final ModelControllerClient client) throws IOException {
        setAttributeValue(client, JaxrsAttribute.JAXRS_2_0_REQUEST_MATCHING, VALUE_EXPRESSION_BOOLEAN_TRUE);
        setAttributeValue(client, JaxrsAttribute.RESTEASY_ADD_CHARSET, VALUE_EXPRESSION_BOOLEAN_FALSE);
        setAttributeValue(client, JaxrsAttribute.RESTEASY_BUFFER_EXCEPTION_ENTITY, VALUE_EXPRESSION_BOOLEAN_FALSE);
        setAttributeValue(client, JaxrsAttribute.RESTEASY_DISABLE_HTML_SANITIZER, VALUE_EXPRESSION_BOOLEAN_TRUE);

        ModelNode list = new ModelNode().setEmptyList();
        list.add(VALUE_EXPRESSION_FILENAME_1);
        list.add(VALUE_EXPRESSION_FILENAME_2);
        setAttributeValue(client, JaxrsAttribute.RESTEASY_DISABLE_PROVIDERS, list);
        list.clear();

        setAttributeValue(client, JaxrsAttribute.RESTEASY_DOCUMENT_EXPAND_ENTITY_REFERENCES, VALUE_EXPRESSION_BOOLEAN_TRUE);
        setAttributeValue(client, JaxrsAttribute.RESTEASY_DOCUMENT_SECURE_DISABLE_DTDS, VALUE_EXPRESSION_BOOLEAN_FALSE);
        setAttributeValue(client, JaxrsAttribute.RESTEASY_DOCUMENT_SECURE_PROCESSING_FEATURE, VALUE_EXPRESSION_BOOLEAN_FALSE);
        setAttributeValue(client, JaxrsAttribute.RESTEASY_GZIP_MAX_INPUT, VALUE_EXPRESSION_INT);

        list.add(VALUE_EXPRESSION_RESOURCE);
        list.add("java:global/jaxrsnoap/" + EJB_Resource2.class.getSimpleName());
        setAttributeValue(client, JaxrsAttribute.RESTEASY_JNDI_RESOURCES, list);
        list.clear();

        ModelNode map = new ModelNode();
        map.add(new Property("en", new ModelNode("en-US")));
        map.add(new Property("es", VALUE_EXPRESSION_SPANISH));
        map.add(new Property("fr", new ModelNode("fr")));
        setAttributeValue(client, JaxrsAttribute.RESTEASY_LANGUAGE_MAPPINGS, map);
        map.clear();

        map.add(new Property("unusual", VALUE_EXPRESSION_APPLICATION_UNUSUAL));
        map.add(new Property("xml", new ModelNode("application/xml")));
        setAttributeValue(client, JaxrsAttribute.RESTEASY_MEDIA_TYPE_MAPPINGS, map);
        map.clear();

        setAttributeValue(client, JaxrsAttribute.RESTEASY_PREFER_JACKSON_OVER_JSONB, VALUE_EXPRESSION_BOOLEAN_TRUE);
        setAttributeValue(client, JaxrsAttribute.RESTEASY_MEDIA_TYPE_PARAM_MAPPING, VALUE_EXPRESSION_STRING);

        list.add(VALUE_EXPRESSION_PROVIDER);
        setAttributeValue(client, JaxrsAttribute.RESTEASY_PROVIDERS, list);
        list.clear();

        setAttributeValue(client, JaxrsAttribute.RESTEASY_RFC7232_PRECONDITIONS, VALUE_EXPRESSION_BOOLEAN_TRUE);
        setAttributeValue(client, JaxrsAttribute.RESTEASY_ROLE_BASED_SECURITY, VALUE_EXPRESSION_BOOLEAN_TRUE);
        setAttributeValue(client, JaxrsAttribute.RESTEASY_SECURE_RANDOM_MAX_USE, VALUE_EXPRESSION_INT);
        setAttributeValue(client, JaxrsAttribute.RESTEASY_USE_BUILTIN_PROVIDERS, VALUE_EXPRESSION_BOOLEAN_FALSE);
        setAttributeValue(client, JaxrsAttribute.RESTEASY_USE_CONTAINER_FORM_PARAMS, VALUE_EXPRESSION_BOOLEAN_TRUE);
        setAttributeValue(client, JaxrsAttribute.RESTEASY_WIDER_REQUEST_MATCHING, VALUE_EXPRESSION_BOOLEAN_TRUE);
    }

    static void setAttributeValue(final ModelControllerClient client, AttributeDefinition attribute, ModelNode value) throws IOException {
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
        ModelNode result = client.execute(op);
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
            // Ignore these attributes as they are not set in the context as the attributes are not public. These are
            // also tested elsewhere
            if ("tracing-type".equals(attribute.getName()) || "tracing-threshold".equals(attribute.getName())
                    || "resteasy-patchfilter-disabled".equals(attribute.getName())) {
                continue;
            }
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
        Assert.assertEquals("Failed to get value for " + resteasyName,200, response.getStatus());
        Object result = response.readEntity(modelTypeMap.get(attribute.getType()));

        final String msg = "Invalid value found for " + resteasyName;
        switch (attribute.getType()) {

            case BOOLEAN:
                Assert.assertEquals(msg, expectedValues.get(attribute).asBoolean(), result);
                return;

            case INT:
                Assert.assertEquals(msg, expectedValues.get(attribute).asInt(), result);
                return;

            case STRING:
                Assert.assertEquals(msg, expectedValues.get(attribute).asString(), result);
                return;

            case LIST:
                Assert.assertEquals(msg, expectedValues.get(attribute).asString(), result);
                return;

            case OBJECT:
                Assert.assertEquals(msg, expectedValues.get(attribute).asString(), result);
                return;

            default:
                Assert.fail("Unexpected ModelNode type for " + resteasyName);
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
            // Strings need to be ignored
            if (attribute.getType() == ModelType.STRING) {
                continue;
            }
            ModelNode op = Operations.createWriteAttributeOperation(ADDRESS, attribute.getName(), mangleAttribute(attribute));
            ModelNode result = client.getControllerClient().execute(op);
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
        ModelNode result = client.getControllerClient().execute(op);
        Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());

        // Verify that value of "resteasy.add.charset" hasn't changed in existing deployment.
        response = builder.get();
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals(oldValue, response.readEntity(boolean.class));

        // Reset "resteasy.add.charset" to original value.
        op = Operations.createWriteAttributeOperation(ADDRESS, JaxrsAttribute.RESTEASY_ADD_CHARSET.getName(), oldValue);
        result = client.getControllerClient().execute(op);
        Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());
    }
}

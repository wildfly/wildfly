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
import java.util.Map.Entry;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.jaxrs.JaxrsContextParamHandler;
import org.jboss.as.test.integration.jaxrs.packaging.war.WebXml;
import org.jboss.as.test.integration.management.base.AbstractMgmtServerSetupTask;
import org.jboss.as.test.integration.management.base.ContainerResourceMgmtTestBase;
import org.jboss.as.test.integration.management.util.MgmtOperationException;
import org.jboss.dmr.ModelNode;
import org.jboss.resteasy.plugins.providers.StringTextStar;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;
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
public class ResteasyAttributeTestCase extends ContainerResourceMgmtTestBase {
   private static ModelControllerClient modelControllerClient;
   private static Client jaxrsClient;

   @ArquillianResource
   private static URL url;

   static {
      jaxrsClient = ClientBuilder.newClient();
   }

   private static final ModelNode ADDRESS = Operations.createAddress("subsystem", "jaxrs");
   private static final String OUTCOME = "outcome";
   private static final String NAME = "name";
   private static final String SUCCESS = "success";
   private static final String FAILED = "failed";
   private static final String RESTEASY_MEDIA_TYPE_PARAM_MAPPING_CONTEXT_VALUE = "resteasy.media.type.param.mapping.context.value";

   static class AttributeTestCaseDeploymentSetup extends AbstractMgmtServerSetupTask {

      @Override
      public void doSetup(final ManagementClient managementClient) throws Exception {
         modelControllerClient = getModelControllerClient();
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
      war.addClasses(Foo.class, Bar.class, Bar5.class, Bar6.class, ResteasyAttributeResource.class);
      war.addClasses(Provider1.class, Provider2.class, Provider5.class, Provider6.class);
      war.addClasses(EJB_Resource1.class, EJB_Resource2.class);
      war.addClasses(Resource1.class, Resource2.class);
      war.addClass(JaxrsContextParamHandler.class);
      war.addClasses(XMLElementReader.class, XMLElementWriter.class);
      war.addPackage(MgmtOperationException.class.getPackage());
      war.addPackage(AttributeTestCaseDeploymentSetup.class.getPackage());
      war.addPackage(AbstractMgmtServerSetupTask.class.getPackage());
      war.addPackage(ContainerResourceMgmtTestBase.class.getPackage());
      war.addAsManifestResource(new StringAsset("Dependencies: org.jboss.as.jaxrs, org.jboss.as.controller, org.jboss.as.controller-client,org.jboss.dmr,org.jboss.as.cli,javax.inject.api,org.jboss.as.connector\n"), "MANIFEST.MF");

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
    */
   static void setAttributeValues() throws IOException {

      // Set boolean context parameters
      for (Entry<String, Boolean> entry : JaxrsContextParamHandler.getContextParametersBoolean().entrySet()) {
         ModelNode op = Operations.createWriteAttributeOperation(ADDRESS, entry.getKey(), !entry.getValue());
         ModelNode result = modelControllerClient.execute(op);
         Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());
      }

      // Set integer context parameters
      for (Entry<String, Integer> entry : JaxrsContextParamHandler.getContextParametersInteger().entrySet()) {
         ModelNode op = Operations.createWriteAttributeOperation(ADDRESS, entry.getKey(), entry.getValue() + 17);
         ModelNode result = modelControllerClient.execute(op);
         Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());
      }

      // Set string context parameters
      for (Entry<String, String> entry : JaxrsContextParamHandler.getContextParametersString().entrySet()) {
         ModelNode op = Operations.createWriteAttributeOperation(ADDRESS, entry.getKey(), "aazz");
         ModelNode result = modelControllerClient.execute(op);
         Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());
      }

      // Clear list context parameters
      for (String attribute : JaxrsContextParamHandler.getContextParametersList()) {
         ModelNode op = makeClearOperation(ADDRESS, attribute);
         ModelNode result = modelControllerClient.execute(op);
         Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());
      }

      // Set List context parameters
      executeListAdd(ADDRESS, JaxrsContextParamHandler.RESTEASY_DISABLE_PROVIDERS, Provider1.class.getName());
      executeListAdd(ADDRESS, JaxrsContextParamHandler.RESTEASY_DISABLE_PROVIDERS, Provider2.class.getName());
      executeListAdd(ADDRESS, JaxrsContextParamHandler.RESTEASY_JNDI_RESOURCES, "java:app/jaxrsnoap/" + EJB_Resource1.class.getSimpleName());
      executeListAdd(ADDRESS, JaxrsContextParamHandler.RESTEASY_JNDI_RESOURCES, "java:app/jaxrsnoap/" + EJB_Resource2.class.getSimpleName());
      executeListAdd(ADDRESS, JaxrsContextParamHandler.RESTEASY_PROVIDERS, Provider5.class.getName());
      executeListAdd(ADDRESS, JaxrsContextParamHandler.RESTEASY_PROVIDERS, Provider6.class.getName());
      executeListAdd(ADDRESS, JaxrsContextParamHandler.RESTEASY_PROVIDERS, StringTextStar.class.getName());
      executeListAdd(ADDRESS, JaxrsContextParamHandler.RESTEASY_RESOURCES, Resource1.class.getName());
      executeListAdd(ADDRESS, JaxrsContextParamHandler.RESTEASY_RESOURCES, Resource2.class.getName());

      // Clear map context parameters
      for (String attribute : JaxrsContextParamHandler.getContextParametersMap()) {
         ModelNode op = makeClearOperation(ADDRESS, attribute);
         ModelNode result = modelControllerClient.execute(op);
         Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());
      }

      // Set map context parameters
      executeListAdd(ADDRESS, JaxrsContextParamHandler.RESTEASY_LANGUAGE_MAPPINGS, "en : en-US");
      executeListAdd(ADDRESS, JaxrsContextParamHandler.RESTEASY_LANGUAGE_MAPPINGS, "es : es");
      executeListAdd(ADDRESS, JaxrsContextParamHandler.RESTEASY_LANGUAGE_MAPPINGS, "fr : fr");
      executeListAdd(ADDRESS, JaxrsContextParamHandler.RESTEASY_MEDIA_TYPE_MAPPINGS, "unusual : text/unusual");
      executeListAdd(ADDRESS, JaxrsContextParamHandler.RESTEASY_MEDIA_TYPE_MAPPINGS, "xml : application/xml");
   }

   /*
    * Reset attributes to default values.
    */
   static void resetAttributeValues() throws IOException {
      // Set boolean context parameters
      for (Entry<String, Boolean> entry : JaxrsContextParamHandler.getContextParametersBoolean().entrySet()) {
         ModelNode op = Operations.createWriteAttributeOperation(ADDRESS, entry.getKey(), entry.getValue());
         ModelNode result = modelControllerClient.execute(op);
         Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());
      }

      // Set integer context parameters
      for (Entry<String, Integer> entry : JaxrsContextParamHandler.getContextParametersInteger().entrySet()) {
         ModelNode op = Operations.createWriteAttributeOperation(ADDRESS, entry.getKey(), entry.getValue());
         ModelNode result = modelControllerClient.execute(op);
         Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());
      }

      // Set string context parameters
      for (Entry<String, String> entry : JaxrsContextParamHandler.getContextParametersString().entrySet()) {
         ModelNode op = Operations.createWriteAttributeOperation(ADDRESS, entry.getKey(), " ");
         ModelNode result = modelControllerClient.execute(op);
         Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());
      }

      // Clear list context parameters
      for (String attribute : JaxrsContextParamHandler.getContextParametersList()) {
         ModelNode op = makeClearOperation(ADDRESS, attribute);
         ModelNode result = modelControllerClient.execute(op);
         Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());
      }

      // Clear map context parameters
      for (String attribute : JaxrsContextParamHandler.getContextParametersMap()) {
         ModelNode op = makeClearOperation(ADDRESS, attribute);
         ModelNode result = modelControllerClient.execute(op);
         Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());
      }
   }

   private static ModelNode makeClearOperation(ModelNode address, String name) {
      ModelNode op = Operations.createOperation("list-clear", address);
      op.get("name").set(name);
      return op;
   }

   private static void executeListAdd(ModelNode address, String name, String value) throws IOException {
      ModelNode op = makeListAddOperation(address, name, value);
      ModelNode result = modelControllerClient.execute(op);
      Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());
   }

   private static ModelNode makeListAddOperation(ModelNode address, String name, String value) {
      ModelNode op = Operations.createOperation("list-add", address);
      op.get(NAME).set(name);
      op.get("value").set(value);
      return op;
   }

   //////////////////////////////////////////////////////////////////////////////////////
   /////////////////////            Test methods                     ////////////////////
   //////////////////////////////////////////////////////////////////////////////////////
   @Test
   public void testAttributes() throws IOException {
      WebTarget base = jaxrsClient.target(url.toString() + "myjaxrs/attribute");
      for (Entry<String, Boolean> entry : JaxrsContextParamHandler.getContextParametersBoolean().entrySet()) {
         Builder builder = base.path(entry.getKey()).request().accept("text/plain");
         Response response = builder.get();
         Assert.assertEquals(200, response.getStatus());
         Assert.assertEquals(!entry.getValue(), response.readEntity(boolean.class));
      }

      for (Entry<String, Integer> entry : JaxrsContextParamHandler.getContextParametersInteger().entrySet()) {
         Builder builder = base.path(entry.getKey()).request();
         Response response = builder.get();
         Assert.assertEquals(200, response.getStatus());
         Assert.assertTrue(entry.getValue() + 17 == response.readEntity(int.class));
      }

      for (Entry<String, String> entry : JaxrsContextParamHandler.getContextParametersString().entrySet()) {
         Builder builder = base.path(entry.getKey()).request();
         Response response = builder.get();
         Assert.assertEquals(200, response.getStatus());
         if (JaxrsContextParamHandler.RESTEASY_MEDIA_TYPE_PARAM_MAPPING.contentEquals(entry.getKey())) {
            // Verify that value set in web.xml overrides value set in Wildfly management model.
            Assert.assertEquals(RESTEASY_MEDIA_TYPE_PARAM_MAPPING_CONTEXT_VALUE , response.readEntity(String.class));
         } else {
            Assert.assertEquals("aa" + entry.getValue() + "zz", response.readEntity(String.class));
         }
      }

      verifyValue(base, ADDRESS, JaxrsContextParamHandler.RESTEASY_DISABLE_PROVIDERS, Provider1.class.getName() + "," + Provider2.class.getName());
      verifyValue(base, ADDRESS, JaxrsContextParamHandler.RESTEASY_JNDI_RESOURCES,
            "java:app/jaxrsnoap/" + EJB_Resource1.class.getSimpleName() + "," + "java:app/jaxrsnoap/" + EJB_Resource2.class.getSimpleName());
      verifyValue(base, ADDRESS, JaxrsContextParamHandler.RESTEASY_PROVIDERS,
            Provider5.class.getName() + "," + Provider6.class.getName() + "," + StringTextStar.class.getName());
      verifyValue(base, ADDRESS, JaxrsContextParamHandler.RESTEASY_RESOURCES, Resource1.class.getName() + "," + Resource2.class.getName());

      verifyValue(base, ADDRESS, JaxrsContextParamHandler.RESTEASY_LANGUAGE_MAPPINGS, "en : en-US,es : es,fr : fr");
      verifyValue(base, ADDRESS, JaxrsContextParamHandler.RESTEASY_MEDIA_TYPE_MAPPINGS, "unusual : text/unusual,xml : application/xml");
   }

   /**
    * Test list syntax using attribute "resteasy.providers" (which includes provider StringTextStar).
    */
   @Test
   public void testProvidersAdded() throws Exception {
      {
         WebTarget base = jaxrsClient.target(url.toString() + "myjaxrs/attribute/bar5");
         base.register(Provider5.class);
         Builder builder = base.request().accept("text/plain");
         Response response = builder.post(Entity.entity(new Bar5("sending"), "application/bar"));
         Assert.assertEquals(200, response.getStatus());
         Assert.assertEquals("provider5", response.readEntity(String.class));
      }
      {
         WebTarget base = jaxrsClient.target(url.toString() + "myjaxrs/attribute/bar6");
         base.register(Provider6.class);
         Builder builder = base.request().accept("text/plain");
         Response response = builder.post(Entity.entity(new Bar6("sending"), "application/bar"));
         Assert.assertEquals(200, response.getStatus());
         Assert.assertEquals("provider6", response.readEntity(String.class));
      }
   }

   /**
    * Test map syntax using attribute "resteasy.media.type.mappings" (which includes mapping "unusual : text/unusual").
    */
   @Test
   public void testMap() throws Exception {
      WebTarget base = jaxrsClient.target(url.toString() + "myjaxrs/attribute/accept.unusual");
      Builder builder = base.request().accept("text/plain");
      Response response = builder.get();
      Assert.assertEquals(200, response.getStatus());
      Assert.assertEquals("unusual", response.readEntity(String.class));
   }

   /**
    * Verify that syntactically incorrect values get kicked out.
    */
   @Test
   public void testBadSyntax() throws Exception {
      for (String attribute : JaxrsContextParamHandler.getContextParametersBoolean().keySet()) {
         ModelNode op = Operations.createWriteAttributeOperation(ADDRESS, attribute, "abc");
         ModelNode result = modelControllerClient.execute(op);
         Assert.assertEquals(FAILED, result.get(OUTCOME).asString());
      }

      for (String attribute : JaxrsContextParamHandler.getContextParametersInteger().keySet()) {
         ModelNode op = Operations.createWriteAttributeOperation(ADDRESS, attribute, "abc");
         ModelNode result = modelControllerClient.execute(op);
         Assert.assertEquals(FAILED, result.get(OUTCOME).asString());
      }

      for (String attribute : JaxrsContextParamHandler.getContextParametersMap()) {
          ModelNode op = Operations.createWriteAttributeOperation(ADDRESS, attribute, "abc");
          ModelNode result = modelControllerClient.execute(op);
          Assert.assertEquals(FAILED, result.get(OUTCOME).asString());
       }
   }

   @Test
   public void testExistingDeploymentsUnchanged() throws IOException {

      // Get current value of "resteasy.add.charset".
      WebTarget base = jaxrsClient.target(url.toString() + "myjaxrs/attribute");
      Builder builder = base.path(JaxrsContextParamHandler.RESTEASY_ADD_CHARSET).request();
      Response response = builder.get();
      Assert.assertEquals(200, response.getStatus());
      boolean oldValue = response.readEntity(boolean.class);

      // Update "resteasy.add.charset" value to a new value.
      ModelNode op = Operations.createWriteAttributeOperation(ADDRESS, JaxrsContextParamHandler.RESTEASY_ADD_CHARSET, !oldValue);
      ModelNode result = modelControllerClient.execute(op);
      Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());

      // Verify that value of "resteasy.add.charset" hasn't changed in existing deployment.
      response = builder.get();
      Assert.assertEquals(200, response.getStatus());
      Assert.assertEquals(oldValue, response.readEntity(boolean.class));

      // Reset "resteasy.add.charset" to original value.
      op = Operations.createWriteAttributeOperation(ADDRESS, JaxrsContextParamHandler.RESTEASY_ADD_CHARSET, oldValue);
      result = modelControllerClient.execute(op);
      Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());
   }

   void verifyValue(WebTarget base, ModelNode address, String name, String expectedValue) throws IOException {
      Builder builder = base.path(name).request();
      Response response = builder.get();
      Assert.assertEquals(200, response.getStatus());
      Assert.assertEquals(expectedValue, response.readEntity(String.class));
   }
}

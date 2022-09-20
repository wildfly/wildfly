/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc. and individual contributors
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
package org.jboss.as.test.integration.management.deploy.runtime;

import java.net.URL;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.as.test.integration.management.deploy.runtime.jaxrs.HelloApplication;
import org.jboss.as.test.integration.management.deploy.runtime.jaxrs.HelloResource;
import org.jboss.as.test.integration.management.deploy.runtime.jaxrs.PureProxyApiService;
import org.jboss.as.test.integration.management.deploy.runtime.jaxrs.PureProxyEndPoint;
import org.jboss.as.test.integration.management.deploy.runtime.jaxrs.SubHelloResource;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEPLOYMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.hamcrest.MatcherAssert.assertThat;

import org.hamcrest.CoreMatchers;
import org.jboss.as.controller.operations.common.Util;

@RunWith(Arquillian.class)
@RunAsClient
public class JaxrsRuntimeNameTestCase extends AbstractRuntimeTestCase {

    private static final String CONSUMES = "consumes";
    private static final String DEPLOYMENT_NAME = "hello-rs.war";
    private static final String JAVA_METHOD = "java-method";
    private static final String PRODUCES = "produces";
    private static final String RESOURCE_CLASS = "resource-class";
    private static final String RESOURCE_METHODS = "resource-methods";
    private static final String RESOURCE_PATH = "resource-path";
    private static final String RESOURCE_PATHS = "rest-resource-paths";
    private static final String REST_RESOURCE_NAME = "rest-resource";
    private static final String SUB_RESOURCE_LOCATORS = "sub-resource-locators";
    private static final String SUBSYSTEM_NAME = "jaxrs";
    private static final ModelControllerClient controllerClient = TestSuiteEnvironment.getModelControllerClient();

    @ArquillianResource
    private URL url;

    @Deployment(testable = false)
    public static Archive<?> deploy() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, DEPLOYMENT_NAME);
        war.addClass(HelloApplication.class);
        war.addClass(HelloResource.class);
        war.addClass(SubHelloResource.class);
        war.addClass(PureProxyApiService.class);
        war.addClass(PureProxyEndPoint.class);
        return war;
    }

    private String performCall(String urlPattern) throws Exception {
        return HttpRequest.get(url + urlPattern, 10, TimeUnit.SECONDS);
    }

    @Test
    public void testSubResource() throws Exception {
        assertThat(performCall("hello"), is("Hello World!"));
    }

    @Test
    public void testStepByStep() throws Exception {
        PathAddress deploymentAddress = PathAddress.pathAddress(DEPLOYMENT, DEPLOYMENT_NAME);
        ModelNode readResource = Util.createOperation(READ_RESOURCE_OPERATION, deploymentAddress);
        ModelNode result = controllerClient.execute(readResource);

        assertThat("Failed to list resources: " + result, Operations.isSuccessfulOutcome(result), is(true));
        PathAddress subsystemAddress = deploymentAddress.append(SUBSYSTEM, SUBSYSTEM_NAME);
        readResource = Util.createOperation(READ_RESOURCE_OPERATION, subsystemAddress);
        result = controllerClient.execute(readResource);
        assertThat("Failed to list resources: " + result, Operations.isSuccessfulOutcome(result), is(true));

        readResource = Util.createOperation("show-resources", subsystemAddress);
        result = controllerClient.execute(readResource);
        assertThat("Failed to list resources: " + result, Operations.isSuccessfulOutcome(result), is(true));
        List<ModelNode> jaxrsResources = Operations.readResult(result).asList();
        assertThat(jaxrsResources, is(notNullValue()));
        assertThat(jaxrsResources.size(), is(5));
        int count = 0;
        for (ModelNode jaxrsResource: jaxrsResources) {
            if (jaxrsResource.get(RESOURCE_CLASS).asString().equals(HelloResource.class.getName())) {
                count++;
                String path = jaxrsResource.get(RESOURCE_PATH).asString();
                switch (path) {
                    case "/update":
                        assertThat(jaxrsResource.toString(), jaxrsResource.get(RESOURCE_METHODS).asList().get(0).asString(), is("PUT /hello-rs/hello/update - org.jboss.as.test.integration.management.deploy.runtime.jaxrs.HelloResource.updateMessage(...)"));
                        break;
                    case "/json":
                        assertThat(jaxrsResource.toString(), jaxrsResource.get(RESOURCE_METHODS).asList().get(0).asString(), is("GET /hello-rs/hello/json - org.jboss.as.test.integration.management.deploy.runtime.jaxrs.HelloResource.getHelloWorldJSON()"));
                        break;
                    case "/xml":
                        assertThat(jaxrsResource.toString(), jaxrsResource.get(RESOURCE_METHODS).asList().get(0).asString(), is("GET /hello-rs/hello/xml - org.jboss.as.test.integration.management.deploy.runtime.jaxrs.HelloResource.getHelloWorldXML()"));
                        break;
                    case "/":
                        assertThat(jaxrsResource.toString(), jaxrsResource.get(RESOURCE_METHODS).asList().get(0).asString(), is("GET /hello-rs/hello/ - org.jboss.as.test.integration.management.deploy.runtime.jaxrs.HelloResource.getHelloWorld()"));
                        break;
                    default:
                        assertThat(jaxrsResource.toString(), false, is(true));
                }
            } else if (jaxrsResource.get(RESOURCE_CLASS).asString().equals(PureProxyApiService.class.getName())) {
                count++;
                String path = jaxrsResource.get(RESOURCE_PATH).asString();
                assertThat(jaxrsResource.toString(), path, is("pure/proxy/test/{a}/{b}"));
                assertThat(jaxrsResource.toString(), jaxrsResource.get(RESOURCE_METHODS).asList().get(0).asString(), is("GET /hello-rs/hello/pure/proxy/test/{a}/{b} - org.jboss.as.test.integration.management.deploy.runtime.jaxrs.PureProxyApiService.test(...)"));
            }
        }
        assertThat(count, is(5));
    }

    @Test
    public void testReadRestResource() throws Exception {
        ModelNode removeResource =  Util.createRemoveOperation(PathAddress.pathAddress(DEPLOYMENT, DEPLOYMENT_NAME).append(SUBSYSTEM, SUBSYSTEM_NAME));
        assertThat(Operations.getFailureDescription(controllerClient.execute(removeResource)).asString(), CoreMatchers.containsString("WFLYCTL0031"));
        ModelNode readResource =  Util.createOperation(READ_RESOURCE_OPERATION, PathAddress.pathAddress(DEPLOYMENT, DEPLOYMENT_NAME)
                .append(SUBSYSTEM, SUBSYSTEM_NAME)
                .append(REST_RESOURCE_NAME, HelloResource.class.getCanonicalName()));
        readResource.get(ModelDescriptionConstants.INCLUDE_RUNTIME).set(true);
        ModelNode result = controllerClient.execute(readResource);
        assertThat("Failed to list resources: " + result, Operations.isSuccessfulOutcome(result), is(true));

        ModelNode res = Operations.readResult(result);
        assertThat(res.isDefined(), is(true));

        assertThat(res.get(RESOURCE_CLASS).asString(), is(HelloResource.class.getCanonicalName()));
        List<ModelNode> subResList = res.get(RESOURCE_PATHS).asList();
        assertThat(subResList.size(), is(4));

        ModelNode rootRes = subResList.get(0); // '/'
        assertThat(rootRes.get(RESOURCE_PATH).asString(), is("/"));
        assertThat(rootRes.get(JAVA_METHOD).asString(), is("java.lang.String " + HelloResource.class.getCanonicalName() + ".getHelloWorld()"));
        assertThat(rootRes.get(CONSUMES).isDefined(), is(false));
        assertThat(rootRes.get(PRODUCES).asList().size(), is(1));
        assertThat(rootRes.get(PRODUCES).asList().get(0).asString(), is("text/plain"));
        assertThat(rootRes.get(RESOURCE_METHODS).asList().size(), is(1));
        assertThat(rootRes.get(RESOURCE_METHODS).asList().get(0).asString(), is("GET /hello-rs/hello/"));

        ModelNode jsonRes = subResList.get(1);// '/json'
        assertThat(jsonRes.get(RESOURCE_PATH).asString(), is("/json"));
        assertThat(jsonRes.get(JAVA_METHOD).asString(), is("jakarta.json.JsonObject " + HelloResource.class.getCanonicalName() + ".getHelloWorldJSON()"));
        assertThat(jsonRes.get(CONSUMES).isDefined(), is(false));
        assertThat(jsonRes.get(PRODUCES).asList().size(), is(1));
        assertThat(jsonRes.get(PRODUCES).asList().get(0).asString(), is("application/json"));
        assertThat(jsonRes.get(RESOURCE_METHODS).asList().size(), is(1));
        assertThat(jsonRes.get(RESOURCE_METHODS).asList().get(0).asString(), is("GET /hello-rs/hello/json"));

        ModelNode updateRes = subResList.get(2);// '/update'
        assertThat(updateRes.get(RESOURCE_PATH).asString(), is("/update"));
        assertThat(updateRes.get(JAVA_METHOD).asString(), is("void " + HelloResource.class.getCanonicalName() + ".updateMessage(@QueryParam java.lang.String content = 'Hello')"));
        assertThat(updateRes.get(PRODUCES).isDefined(), is(false));
        assertThat(updateRes.get(CONSUMES).asList().size(), is(1));
        assertThat(updateRes.get(CONSUMES).asList().get(0).asString(), is("text/plain"));
        assertThat(updateRes.get(RESOURCE_METHODS).asList().size(), is(1));
        assertThat(updateRes.get(RESOURCE_METHODS).asList().get(0).asString(), is("PUT /hello-rs/hello/update"));

        ModelNode xmlRes = subResList.get(3);// '/xml'
        assertThat(xmlRes.get(RESOURCE_PATH).asString(), is("/xml"));
        assertThat(xmlRes.get(JAVA_METHOD).asString(), is("java.lang.String " + HelloResource.class.getCanonicalName() + ".getHelloWorldXML()"));
        assertThat(xmlRes.get(CONSUMES).isDefined(), is(false));
        assertThat(xmlRes.get(PRODUCES).asList().size(), is(1));
        assertThat(xmlRes.get(PRODUCES).asList().get(0).asString(), is("application/xml"));
        assertThat(xmlRes.get(RESOURCE_METHODS).asList().size(), is(1));
        assertThat(xmlRes.get(RESOURCE_METHODS).asList().get(0).asString(), is("GET /hello-rs/hello/xml"));

        List<ModelNode> subLocatorList = res.get(SUB_RESOURCE_LOCATORS).asList();
        assertThat(subLocatorList.size(), is(1));
        ModelNode subLocatorRes = subLocatorList.get(0);
        assertThat(subLocatorRes.get(RESOURCE_CLASS).asString(), is(SubHelloResource.class.getCanonicalName()));
        List<ModelNode> subResInsideSubLocator = subLocatorRes.get(RESOURCE_PATHS).asList();
        assertThat(subResInsideSubLocator.size(), is(2));

        ModelNode subRootHi = subResInsideSubLocator.get(0);
        assertThat(subRootHi.get(RESOURCE_PATH).asString(), is("/sub/"));
        assertThat(subRootHi.get(JAVA_METHOD).asString(), is("java.lang.String " + SubHelloResource.class.getCanonicalName() + ".hi()"));
        assertThat(subRootHi.get(CONSUMES).isDefined(), is(false));
        assertThat(subRootHi.get(PRODUCES).asList().size(), is(1));
        assertThat(subRootHi.get(PRODUCES).asList().get(0).asString(), is("text/plain"));
        assertThat(subRootHi.get(RESOURCE_METHODS).asList().size(), is(1));
        assertThat(subRootHi.get(RESOURCE_METHODS).asList().get(0).asString(), is("GET /hello-rs/hello/sub/"));

        ModelNode pingNode = subResInsideSubLocator.get(1);
        assertThat(pingNode.get(RESOURCE_PATH).asString(), is("/sub/ping/{name}"));
        assertThat(pingNode.get(JAVA_METHOD).asString(), is("java.lang.String " + SubHelloResource.class.getCanonicalName() + ".ping(@PathParam java.lang.String name = 'JBoss')"));
        assertThat(pingNode.get(CONSUMES).isDefined(), is(false));
        assertThat(pingNode.get(PRODUCES).asList().size(), is(1));
        assertThat(pingNode.get(PRODUCES).asList().get(0).asString(), is("text/plain"));
        assertThat(pingNode.get(RESOURCE_METHODS).asList().size(), is(1));
        assertThat(pingNode.get(RESOURCE_METHODS).asList().get(0).asString(), is("GET /hello-rs/hello/sub/ping/{name}"));

    }

    @Test
    public void testReadRestEndPointIntf() throws Exception {
        assertThat(performCall("hello/pure/proxy/test/Hello/World"), is("Hello World"));
        ModelNode readResource =  Util.createOperation(READ_RESOURCE_OPERATION, PathAddress.pathAddress(DEPLOYMENT, DEPLOYMENT_NAME)
                .append(SUBSYSTEM, SUBSYSTEM_NAME)
                .append(REST_RESOURCE_NAME, PureProxyEndPoint.class.getCanonicalName()));
        readResource.get(ModelDescriptionConstants.INCLUDE_RUNTIME).set(true);
        ModelNode result = controllerClient.execute(readResource);
        assertThat("Failed to list resources: " + result, Operations.isSuccessfulOutcome(result), is(true));

        ModelNode res = Operations.readResult(result);
        assertThat(res.isDefined(), is(true));

        assertThat(res.get(RESOURCE_CLASS).asString(), is(PureProxyEndPoint.class.getCanonicalName()));
        List<ModelNode> subResList = res.get(RESOURCE_PATHS).asList();
        assertThat(subResList.size(), is(1));

        ModelNode rootRes = subResList.get(0);
        assertThat(rootRes.get(RESOURCE_PATH).asString(), is("pure/proxy/test/{a}/{b}"));
        assertThat(rootRes.get(JAVA_METHOD).asString(), is("java.lang.String " + PureProxyEndPoint.class.getCanonicalName() + ".test(@PathParam java.lang.String a, @PathParam java.lang.String b)"));
        assertThat(rootRes.get(CONSUMES).isDefined(), is(false));
        assertThat(rootRes.get(PRODUCES).isDefined(), is(false));
        assertThat(rootRes.get(RESOURCE_METHODS).asList().size(), is(1));
        assertThat(rootRes.get(RESOURCE_METHODS).asList().get(0).asString(), is("GET /hello-rs/hello/pure/proxy/test/{a}/{b}"));

    }

    @Test
    public void testRecursive() throws Exception {
        ModelNode readResource =  Util.createOperation(READ_RESOURCE_OPERATION, PathAddress.pathAddress(DEPLOYMENT, DEPLOYMENT_NAME));
        readResource.get(ModelDescriptionConstants.INCLUDE_RUNTIME).set(true);
        readResource.get(ModelDescriptionConstants.RECURSIVE).set(true);
        ModelNode result = controllerClient.execute(readResource);
        assertThat("Failed to list resources: " + result, Operations.isSuccessfulOutcome(result), is(true));
    }

}

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
import static org.junit.Assert.assertThat;

import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.jaxrs.JaxrsDeploymentDefinition;

@RunWith(Arquillian.class)
@RunAsClient
public class JaxrsRuntimeNameTestCase extends AbstractRuntimeTestCase {

    private static final String DEPLOYMENT_NAME = "hello-rs.war";
    private static final ModelControllerClient controllerClient = TestSuiteEnvironment.getModelControllerClient();

    @ArquillianResource
    private URL url;

    @Deployment(testable = false)
    public static Archive<?> deploy() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, DEPLOYMENT_NAME);
        war.addClass(HelloApplication.class);
        war.addClass(HelloResource.class);
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
        PathAddress subsystemAddress = deploymentAddress.append(SUBSYSTEM, "jaxrs");
        readResource = Util.createOperation(READ_RESOURCE_OPERATION, subsystemAddress);
        result = controllerClient.execute(readResource);
        assertThat("Failed to list resources: " + result, Operations.isSuccessfulOutcome(result), is(true));

        readResource = Util.createOperation(JaxrsDeploymentDefinition.SHOW_RESOURCES, subsystemAddress);
        result = controllerClient.execute(readResource);
        assertThat("Failed to list resources: " + result, Operations.isSuccessfulOutcome(result), is(true));
        List<ModelNode> jaxrsResources = Operations.readResult(result).asList();
        assertThat(jaxrsResources, is(notNullValue()));
        assertThat(jaxrsResources.size(), is(4));
        for (ModelNode jaxrsResource : jaxrsResources) {
            assertThat(jaxrsResource.toString(), jaxrsResource.get(JaxrsDeploymentDefinition.CLASSNAME.getName()).asString(), is(HelloResource.class.getName()));
            String path = jaxrsResource.get(JaxrsDeploymentDefinition.PATH.getName()).asString();
            switch (path) {
                case "/update":
                    assertThat(jaxrsResource.toString(), jaxrsResource.get(JaxrsDeploymentDefinition.METHODS.getName()).asList().get(0).asString(), is("PUT /hello-rs/hello/update - org.jboss.as.test.integration.management.deploy.runtime.jaxrs.HelloResource.updateMessage(...)"));
                    break;
                case "/json":
                    assertThat(jaxrsResource.toString(), jaxrsResource.get(JaxrsDeploymentDefinition.METHODS.getName()).asList().get(0).asString(), is("GET /hello-rs/hello/json - org.jboss.as.test.integration.management.deploy.runtime.jaxrs.HelloResource.getHelloWorldJSON()"));
                    break;
                case "/xml":
                    assertThat(jaxrsResource.toString(), jaxrsResource.get(JaxrsDeploymentDefinition.METHODS.getName()).asList().get(0).asString(), is("GET /hello-rs/hello/xml - org.jboss.as.test.integration.management.deploy.runtime.jaxrs.HelloResource.getHelloWorldXML()"));
                    break;
                case "/":
                    assertThat(jaxrsResource.toString(), jaxrsResource.get(JaxrsDeploymentDefinition.METHODS.getName()).asList().get(0).asString(), is("GET /hello-rs/hello/ - org.jboss.as.test.integration.management.deploy.runtime.jaxrs.HelloResource.getHelloWorld()"));
                    break;
                default:
                    assertThat(jaxrsResource.toString(), false, is(true));
            }
        }

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

/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.naming;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.naming.subsystem.NamingSubsystemModel.BINDING_TYPE;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.PathAddress;
import org.junit.runner.RunWith;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;

import org.jboss.as.arquillian.api.ServerSetup;

import java.net.URL;

/**
 * @author Parul Sharma
 */

@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup({PropertiesBindingTestCase.PropertiesBinding.class})
public class PropertiesBindingTestCase {

    @ArquillianResource
    private URL url;

    static class PropertiesBinding implements ServerSetupTask {

        @Override
        public void setup(ManagementClient managementClient, String containerId) throws Exception {
            PathAddress address = PathAddress.pathAddress().append(SUBSYSTEM, "naming").append("binding", "java:global/myAppConfig");
            ModelNode bindingAdd = Util.createAddOperation(address);

            bindingAdd.get(BINDING_TYPE).set(PROPERTIES);
            ModelNode propertyNode = new ModelNode();
            propertyNode.get(NAME).set("configname1");
            propertyNode.get(VALUE).set("configvalue1");
            propertyNode.get(TYPE).set("String");
            ModelNode propertyNode1 = new ModelNode();
            propertyNode1.get(NAME).set("configname2");
            propertyNode1.get(VALUE).set("100");
            propertyNode1.get(TYPE).set("int");
            bindingAdd.get(PROPERTIES).add(propertyNode);
            bindingAdd.get(PROPERTIES).add(propertyNode1);
            ModelNode result = managementClient.getControllerClient().execute(bindingAdd);
            Assert.assertEquals(result, "Success");

        }

        @Override
        public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
            ModelNode bindingRemove = new ModelNode();
            bindingRemove.get(BINDING_TYPE).set(PROPERTIES);
            bindingRemove.get(PROPERTIES).set("java:global/myAppConfig");
            bindingRemove.get(OP).set(REMOVE);
            managementClient.getControllerClient().execute(bindingRemove);


        }
    }

    @Deployment
    public static WebArchive deploy() {
        return ShrinkWrap.create(WebArchive.class, "PropertiesBindingTestCase.war")
                .addClasses(PropertiesBindingTestCase.class, LookupProperties.class, HttpRequest.class);
    }

    @Test
    public void testPropertiesBinding() throws Exception {
        String res = HttpRequest.get(url.toExternalForm() + "/properties", 10, SECONDS);
        boolean result = res.contains("configname1=configvalue1 String" + "\n" + "configname2=100 Integer");
        Assert.assertEquals(result, true);

    }

}
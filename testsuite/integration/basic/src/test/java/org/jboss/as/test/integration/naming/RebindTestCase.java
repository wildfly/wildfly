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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ALLOW_RESOURCE_SERVICE_RESTART;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.naming.subsystem.NamingSubsystemModel.BINDING;
import static org.jboss.as.naming.subsystem.NamingSubsystemModel.BINDING_TYPE;
import static org.jboss.as.naming.subsystem.NamingSubsystemModel.LOOKUP;
import static org.jboss.as.naming.subsystem.NamingSubsystemModel.SIMPLE;
import static org.jboss.as.naming.subsystem.NamingSubsystemModel.TYPE;
import static org.jboss.as.naming.subsystem.NamingSubsystemModel.VALUE;

import java.net.URL;
import javax.ejb.EJB;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.naming.subsystem.NamingExtension;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test case for binding of {@link URL} (see AS7-5140). Uses AS controller to do the bind, lookup is through an EJB.
 *
 * @author Stuart Douglas
 */
@RunWith(Arquillian.class)
public class RebindTestCase {

    @ArquillianResource
    private ManagementClient managementClient;

    @EJB(mappedName = "java:global/RebindTestCase/BindingLookupBean")
    private BindingLookupBean bean;

    @Deployment
    public static Archive<?> deploy() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "RebindTestCase.jar");
        jar.addClasses(RebindTestCase.class, BindingLookupBean.class);
        jar.addAsManifestResource(new StringAsset("Dependencies: org.jboss.as.controller\n"), "MANIFEST.MF");
        return jar;
    }

    @Test
    public void testRebinding() throws Exception {

        final String name = "java:global/rebind";
        final String lookup = "java:global/lookup";
        final ModelNode address = new ModelNode();
        address.add(SUBSYSTEM, NamingExtension.SUBSYSTEM_NAME);
        address.add(BINDING, name);


        final ModelNode lookupAddress = new ModelNode();
        lookupAddress.add(SUBSYSTEM, NamingExtension.SUBSYSTEM_NAME);
        lookupAddress.add(BINDING, lookup);
        // bind a URL
        ModelNode operation = new ModelNode();
        operation.get(OP).set(ADD);
        operation.get(OP_ADDR).set(address);
        operation.get(BINDING_TYPE).set(SIMPLE);
        operation.get(VALUE).set("http://localhost");
        operation.get(TYPE).set(URL.class.getName());
        try {
            ModelNode addResult = managementClient.getControllerClient().execute(operation);
            Assert.assertFalse(addResult.get(FAILURE_DESCRIPTION).toString(), addResult.get(FAILURE_DESCRIPTION).isDefined());
            Assert.assertEquals("http://localhost", bean.lookupBind(name).toString());

            operation = new ModelNode();
            operation.get(OP).set("rebind");
            operation.get(OP_ADDR).set(address);
            operation.get(BINDING_TYPE).set(SIMPLE);
            operation.get(VALUE).set("http://localhost2");
            operation.get(TYPE).set(URL.class.getName());
            addResult = managementClient.getControllerClient().execute(operation);
            Assert.assertFalse(addResult.get(FAILURE_DESCRIPTION).toString(), addResult.get(FAILURE_DESCRIPTION).isDefined());
            Assert.assertEquals("http://localhost2", bean.lookupBind(name).toString());

            operation = new ModelNode();
            operation.get(OP).set("rebind");
            operation.get(OP_ADDR).set(address);
            operation.get(BINDING_TYPE).set(SIMPLE);
            operation.get(VALUE).set("2");
            operation.get(TYPE).set(Integer.class.getName());

            addResult = managementClient.getControllerClient().execute(operation);
            Assert.assertFalse(addResult.get(FAILURE_DESCRIPTION).toString(), addResult.get(FAILURE_DESCRIPTION).isDefined());
            Assert.assertEquals(2, bean.lookupBind(name));

            operation = new ModelNode();
            operation.get(OP).set(ADD);
            operation.get(OP_ADDR).set(lookupAddress);
            operation.get(BINDING_TYPE).set(SIMPLE);
            String lookedUp = "looked up";
            operation.get(VALUE).set(lookedUp);
            operation.get(TYPE).set(String.class.getName());
            addResult = managementClient.getControllerClient().execute(operation);
            Assert.assertFalse(addResult.get(FAILURE_DESCRIPTION).toString(), addResult.get(FAILURE_DESCRIPTION).isDefined());


            operation = new ModelNode();
            operation.get(OP).set("rebind");
            operation.get(OP_ADDR).set(address);
            operation.get(BINDING_TYPE).set(LOOKUP);
            operation.get(LOOKUP).set(lookup);

            addResult = managementClient.getControllerClient().execute(operation);
            Assert.assertFalse(addResult.get(FAILURE_DESCRIPTION).toString(), addResult.get(FAILURE_DESCRIPTION).isDefined());
            Assert.assertEquals(lookedUp, bean.lookupBind(name));

            operation = new ModelNode();
            operation.get(OP).set(READ_RESOURCE_OPERATION);
            operation.get(OP_ADDR).set(address);

            addResult = managementClient.getControllerClient().execute(operation);
            Assert.assertFalse(addResult.get(FAILURE_DESCRIPTION).toString(), addResult.get(FAILURE_DESCRIPTION).isDefined());
            Assert.assertEquals("java:global/lookup", addResult.get(RESULT).get(LOOKUP).asString());

        } finally {
            // unbind it
            final ModelNode bindingRemove = new ModelNode();
            bindingRemove.get(OP).set(REMOVE);
            bindingRemove.get(OP_ADDR).set(address);
            bindingRemove.get(OPERATION_HEADERS).get(ALLOW_RESOURCE_SERVICE_RESTART).set(true);
            ModelNode removeResult = managementClient.getControllerClient().execute(bindingRemove);
            Assert.assertFalse(removeResult.get(FAILURE_DESCRIPTION).toString(), removeResult.get(FAILURE_DESCRIPTION)
                    .isDefined());

            bindingRemove.get(OP_ADDR).set(lookupAddress);
            removeResult = managementClient.getControllerClient().execute(bindingRemove);
            Assert.assertFalse(removeResult.get(FAILURE_DESCRIPTION).toString(), removeResult.get(FAILURE_DESCRIPTION)
                    .isDefined());
        }
    }

}

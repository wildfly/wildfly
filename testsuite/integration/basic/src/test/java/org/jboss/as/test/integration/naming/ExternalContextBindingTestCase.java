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

import javax.naming.InitialContext;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.naming.subsystem.NamingExtension;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ALLOW_RESOURCE_SERVICE_RESTART;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.naming.subsystem.NamingSubsystemModel.BINDING;
import static org.jboss.as.naming.subsystem.NamingSubsystemModel.BINDING_TYPE;
import static org.jboss.as.naming.subsystem.NamingSubsystemModel.CACHE;
import static org.jboss.as.naming.subsystem.NamingSubsystemModel.CLASS;
import static org.jboss.as.naming.subsystem.NamingSubsystemModel.EXTERNAL_CONTEXT;
import static org.jboss.as.naming.subsystem.NamingSubsystemModel.MODULE;

/**
 * Simple test for external context binding. We just bind a new initial context, more tests are needed
 * with actual LDAP contexts
 */
@RunWith(Arquillian.class)
@ServerSetup(ExternalContextBindingTestCase.ObjectFactoryWithEnvironmentBindingTestCaseServerSetup.class)
public class ExternalContextBindingTestCase {

    private static Logger LOGGER = Logger.getLogger(ExternalContextBindingTestCase.class);

    private static final String MODULE_NAME = "org.jboss.as.naming";

    static class ObjectFactoryWithEnvironmentBindingTestCaseServerSetup implements ServerSetupTask {

        @Override
        public void setup(final ManagementClient managementClient, final String containerId) throws Exception {

            // bind the object factory
            ModelNode address = createAddress("nocache");
            ModelNode bindingAdd = new ModelNode();
            bindingAdd.get(OP).set(ADD);
            bindingAdd.get(OP_ADDR).set(address);
            bindingAdd.get(BINDING_TYPE).set(EXTERNAL_CONTEXT);
            bindingAdd.get(MODULE).set(MODULE_NAME);
            bindingAdd.get(CLASS).set(InitialContext.class.getName());
            ModelNode addResult = managementClient.getControllerClient().execute(bindingAdd);
            Assert.assertFalse(addResult.get(FAILURE_DESCRIPTION).toString(), addResult.get(FAILURE_DESCRIPTION).isDefined());
            LOGGER.info("Object factory bound.");

            address = createAddress("cache");
            bindingAdd = new ModelNode();
            bindingAdd.get(OP).set(ADD);
            bindingAdd.get(OP_ADDR).set(address);
            bindingAdd.get(BINDING_TYPE).set(EXTERNAL_CONTEXT);
            bindingAdd.get(MODULE).set(MODULE_NAME);
            bindingAdd.get(CACHE).set(true);
            bindingAdd.get(CLASS).set(InitialContext.class.getName());
            addResult = managementClient.getControllerClient().execute(bindingAdd);
            Assert.assertFalse(addResult.get(FAILURE_DESCRIPTION).toString(), addResult.get(FAILURE_DESCRIPTION).isDefined());
            LOGGER.info("Object factory bound.");

        }

        private ModelNode createAddress(String part) {
            final ModelNode address = new ModelNode();
            address.add(SUBSYSTEM, NamingExtension.SUBSYSTEM_NAME);
            address.add(BINDING, "java:global/" + part);
            return address;
        }

        @Override
        public void tearDown(final ManagementClient managementClient, final String containerId) throws Exception {
            // unbind the object factorys
            ModelNode bindingRemove = new ModelNode();
            bindingRemove.get(OP).set(REMOVE);
            bindingRemove.get(OP_ADDR).set(createAddress("nocache"));
            bindingRemove.get(ALLOW_RESOURCE_SERVICE_RESTART).set(true);
            ModelNode removeResult = managementClient.getControllerClient().execute(bindingRemove);
            Assert.assertFalse(removeResult.get(FAILURE_DESCRIPTION).toString(), removeResult.get(FAILURE_DESCRIPTION)
                    .isDefined());
            LOGGER.info("Object factory unbound.");

            bindingRemove = new ModelNode();
            bindingRemove.get(OP).set(REMOVE);
            bindingRemove.get(OP_ADDR).set(createAddress("cache"));
            bindingRemove.get(ALLOW_RESOURCE_SERVICE_RESTART).set(true);
            removeResult = managementClient.getControllerClient().execute(bindingRemove);
            Assert.assertFalse(removeResult.get(FAILURE_DESCRIPTION).toString(), removeResult.get(FAILURE_DESCRIPTION)
                    .isDefined());
            LOGGER.info("Object factory unbound.");
        }
    }


    @Deployment
    public static JavaArchive deploy() {
        return ShrinkWrap.create(JavaArchive.class, "externalContextBindingTest.jar")
                .addClasses(ExternalContextBindingTestCase.class, LookupEjb.class);
    }

    @Test
    public void testWithoutCache() throws Exception {
        InitialContext context = new InitialContext();
        InitialContext lookupContext = (InitialContext) context.lookup("java:global/nocache");
        Assert.assertNotNull(lookupContext);
        LookupEjb ejb = (LookupEjb) lookupContext.lookup("java:module/LookupEjb");
        Assert.assertNotNull(ejb);
        InitialContext newLookupContext = (InitialContext) context.lookup("java:global/nocache");
        Assert.assertNotSame(lookupContext, newLookupContext);
        Assert.assertEquals(InitialContext.class, lookupContext.getClass());
    }

    @Test
    public void testWithCache() throws Exception {
        InitialContext context = new InitialContext();
        InitialContext lookupContext = (InitialContext) context.lookup("java:global/cache");
        Assert.assertNotNull(lookupContext);
        LookupEjb ejb = (LookupEjb) lookupContext.lookup("java:module/LookupEjb");
        Assert.assertNotNull(ejb);
        InitialContext newLookupContext = (InitialContext) context.lookup("java:global/cache");
        Assert.assertSame(lookupContext, newLookupContext);

        //close should have no effect
        lookupContext.close();
        ejb = (LookupEjb) lookupContext.lookup("java:module/LookupEjb");
        Assert.assertNotNull(ejb);
        Assert.assertNotSame(InitialContext.class, lookupContext.getClass());
    }
}

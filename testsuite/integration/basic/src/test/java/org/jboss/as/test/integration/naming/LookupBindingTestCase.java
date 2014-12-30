/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.naming.subsystem.NamingExtension;
import org.jboss.as.naming.subsystem.NamingSubsystemModel;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.naming.InitialContext;
import javax.naming.LinkRef;
import java.io.IOException;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.*;
import static org.jboss.as.naming.subsystem.NamingSubsystemModel.BINDING;

/**
 * Test case for naming subsystem config's "lookup" type binding.
 *
 * @author Eduardo Martins
 */
@RunWith(Arquillian.class)
@ServerSetup(LookupBindingTestCase.ServerSetup.class)
public class LookupBindingTestCase {

    private static Logger LOGGER = Logger.getLogger(LookupBindingTestCase.class);

    private static final String binding1Name = "java:global/WFLY368/1";
    private static final String binding1Value = "...";
    private static final String binding2Name = "java:global/WFLY368/2";
    private static final String binding2Value = binding1Name;
    
    @Deployment
    public static JavaArchive deploy() {
        return ShrinkWrap.create(JavaArchive.class, "lookupBindingTestCase.jar")
                .addClass(LookupBindingTestCase.class);
    }

    @Test
    public void testLookupBinding() throws Exception {
        final InitialContext context = new InitialContext();
        Assert.assertEquals(binding1Value, context.lookup(binding1Name));
        Assert.assertEquals(binding1Value, context.lookup(binding2Name));
        Assert.assertEquals(binding1Name, ((LinkRef)context.lookupLink(binding2Name)).getLinkName());
    }

    static class ServerSetup implements ServerSetupTask {

        @Override
        public void setup(final ManagementClient managementClient, final String containerId) throws Exception {
            // add binding 1
            final ModelNode binding1Add = new ModelNode();
            binding1Add.get(OP).set(ADD);
            binding1Add.get(OP_ADDR).set(createAddress(binding1Name));
            binding1Add.get(NamingSubsystemModel.BINDING_TYPE).set(NamingSubsystemModel.SIMPLE);
            binding1Add.get(NamingSubsystemModel.VALUE).set(binding1Value);
            binding1Add.get(NamingSubsystemModel.TYPE).set(String.class.getName());
            final ModelNode binding1AddResult = managementClient.getControllerClient().execute(binding1Add);
            Assert.assertFalse(binding1AddResult.get(FAILURE_DESCRIPTION).toString(), binding1AddResult.get(FAILURE_DESCRIPTION).isDefined());
            LOGGER.info("Binding "+binding1Name+" added.");
            // add binding 2
            final ModelNode binding2Add = new ModelNode();
            binding2Add.get(OP).set(ADD);
            binding2Add.get(OP_ADDR).set(createAddress(binding2Name));
            binding2Add.get(NamingSubsystemModel.BINDING_TYPE).set(NamingSubsystemModel.LOOKUP);
            binding2Add.get(NamingSubsystemModel.LOOKUP).set(binding1Name);
            final ModelNode binding2AddResult = managementClient.getControllerClient().execute(binding2Add);
            Assert.assertFalse(binding2AddResult.get(FAILURE_DESCRIPTION).toString(), binding2AddResult.get(FAILURE_DESCRIPTION).isDefined());
            LOGGER.info("Binding "+binding2Name+" added.");
        }

        private ModelNode createAddress(String bindingName) {
            final ModelNode address = new ModelNode();
            address.add(SUBSYSTEM, NamingExtension.SUBSYSTEM_NAME);
            address.add(BINDING, bindingName);
            return address;
        }

        @Override
        public void tearDown(final ManagementClient managementClient, final String containerId) throws Exception {
            try {
                removeBinding(managementClient, binding2Name);
            } finally {
                removeBinding(managementClient, binding1Name);
            }
        }

        private void removeBinding(ManagementClient managementClient, String bindingName) throws IOException {
            final ModelNode bindingRemove = new ModelNode();
            bindingRemove.get(OP).set(REMOVE);
            bindingRemove.get(OP_ADDR).set(createAddress(bindingName));
            bindingRemove.get(ALLOW_RESOURCE_SERVICE_RESTART).set(true);
            final ModelNode bindingRemoveResult = managementClient.getControllerClient().execute(bindingRemove);
            Assert.assertFalse(bindingRemoveResult.get(FAILURE_DESCRIPTION).toString(), bindingRemoveResult.get(FAILURE_DESCRIPTION)
                    .isDefined());
            LOGGER.info("Binding "+bindingName+" removed.");
        }
    }

}

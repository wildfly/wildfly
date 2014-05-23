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

package org.jboss.as.test.integration.messaging.mgmt;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ATTRIBUTES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INCLUDE_DEFAULTS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_DESCRIPTION_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.jboss.as.messaging.AddressSettingDefinition.MESSAGE_COUNTER_HISTORY_DAY_LIMIT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.Set;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.messaging.CommonAttributes;
import org.jboss.as.test.integration.management.base.ContainerResourceMgmtTestBase;
import org.jboss.dmr.ModelNode;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Emanuel Muckenhuber
 */
@RunWith(Arquillian.class)
@RunAsClient
public class AddressSettingsTestCase extends ContainerResourceMgmtTestBase {

    private static final ModelNode hornetqServerAddress;
    private static final ModelNode defaultAddress;
    private static final ModelNode address;

    static {
        hornetqServerAddress = new ModelNode();
        hornetqServerAddress.add("subsystem", "messaging");
        hornetqServerAddress.add("hornetq-server", "default");

        defaultAddress = hornetqServerAddress.clone();
        defaultAddress.add("address-setting", "#");

        address = hornetqServerAddress.clone();
        address.add("address-setting", "jms.queue.foo");
    }

    @Test
    public void testAddressSettingWrite() throws Exception {
        // /subsystem=messaging/hornetq-server=default/address-setting=test:write-attribute(name=redelivery-delay,value=50)
        final ModelNode add = new ModelNode();
        add.get(ModelDescriptionConstants.OP).set(ADD);
        add.get(ModelDescriptionConstants.OP_ADDR).set(address);

        executeOperation(add);

        final ModelNode update = new ModelNode();
        update.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION);
        update.get(ModelDescriptionConstants.OP_ADDR).set(address);
        update.get(ModelDescriptionConstants.NAME).set("redistribution-delay");
        update.get(ModelDescriptionConstants.VALUE).set(-1L);

        executeOperation(update);

        remove(address);
    }

    @Test
    public void testResolveAddressSettings() throws Exception {
        final ModelNode readResourceDescription = new ModelNode();
        readResourceDescription.get(ModelDescriptionConstants.OP_ADDR).set(defaultAddress);
        readResourceDescription.get(ModelDescriptionConstants.OP).set(READ_RESOURCE_DESCRIPTION_OPERATION);
        final ModelNode description = executeOperation(readResourceDescription, true);
        Set<String> attributeNames = description.get(ATTRIBUTES).keys();

        final ModelNode readResource = new ModelNode();
        readResource.get(ModelDescriptionConstants.OP_ADDR).set(defaultAddress);
        readResource.get(ModelDescriptionConstants.OP).set(READ_RESOURCE_OPERATION);
        final ModelNode defaultAddressSetting = executeOperation(readResource);

        // there is no address-setting for the given address but
        // we can resolve its settings based on HornetQ hierarchical
        // repository of address setting.
        final ModelNode resolve = new ModelNode();
        resolve.get(ModelDescriptionConstants.OP_ADDR).set(hornetqServerAddress);
        resolve.get(ModelDescriptionConstants.OP).set(CommonAttributes.RESOLVE_ADDRESS_SETTING);
        resolve.get(CommonAttributes.HORNETQ_ADDRESS).set("jms.queue.foo");
        ModelNode result = executeOperation(resolve);

        for (String attributeName : attributeNames) {
            assertEquals("unexpected value for " + attributeName, defaultAddressSetting.get(attributeName), result.get(attributeName));
        }
    }

    @Test
    public void testSpecificAddressSetting() throws Exception {
        // /subsystem=messaging/hornetq-server=default/address-setting=jms.queue.foo:add()
        final ModelNode add = new ModelNode();
        add.get(ModelDescriptionConstants.OP).set(ADD);
        add.get(ModelDescriptionConstants.OP_ADDR).set(address);
        executeOperation(add);

        final ModelNode readResourceWithoutDefault = new ModelNode();
        readResourceWithoutDefault.get(ModelDescriptionConstants.OP).set(READ_RESOURCE_OPERATION);
        readResourceWithoutDefault.get(ModelDescriptionConstants.OP_ADDR).set(address);
        readResourceWithoutDefault.get(INCLUDE_DEFAULTS).set(false);
        ModelNode result = executeOperation(readResourceWithoutDefault);
        // the resource has not defined the message-counter-history-day-limit attribute
        assertFalse(result.hasDefined(MESSAGE_COUNTER_HISTORY_DAY_LIMIT.getName()));

        final ModelNode resolve = new ModelNode();
        resolve.get(ModelDescriptionConstants.OP_ADDR).set(hornetqServerAddress);
        resolve.get(ModelDescriptionConstants.OP).set(CommonAttributes.RESOLVE_ADDRESS_SETTING);
        resolve.get(CommonAttributes.HORNETQ_ADDRESS).set("jms.queue.foo");
        result = executeOperation(resolve);
        // inherit the message-counter-history-day-limit for the '#' address-setting
        assertEquals(10, result.get(MESSAGE_COUNTER_HISTORY_DAY_LIMIT.getName()).asInt());

        remove(address);
    }
}

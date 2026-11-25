/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.messaging.mgmt;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ATTRIBUTES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INCLUDE_DEFAULTS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_DESCRIPTION_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.IOException;
import java.util.Set;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.test.integration.common.jms.JMSOperations;
import org.jboss.as.test.integration.common.jms.JMSOperationsProvider;
import org.jboss.as.test.integration.management.base.ContainerResourceMgmtTestBase;
import org.jboss.as.test.integration.management.util.MgmtOperationException;
import org.jboss.dmr.ModelNode;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Emanuel Muckenhuber
 */
@RunWith(Arquillian.class)
@RunAsClient
public class AddressSettingsTestCase extends ContainerResourceMgmtTestBase {
    private static final String ACTIVEMQ_ADDRESS = "activemq-address";
    private static final String MESSAGE_COUNTER_HISTORY_DAY_LIMIT = "message-counter-history-day-limit";
    private static final String RESOLVE_ADDRESS_SETTING = "resolve-address-setting";

    private ModelNode defaultAddress;
    private ModelNode intermediateAddress;
    private ModelNode address;

    @ContainerResource
    private ManagementClient managementClient;

    private JMSOperations jmsOperations;

    @Before
    public void before() throws IOException, MgmtOperationException {
        jmsOperations = JMSOperationsProvider.getInstance(managementClient);

        defaultAddress = jmsOperations.getServerAddress().add("address-setting", "#");
        intermediateAddress = jmsOperations.getServerAddress().add("address-setting", "jms.queue.*");
        address = jmsOperations.getServerAddress().add("address-setting", "jms.queue.foo");
        try {
            remove(address);
        } catch (MgmtOperationException ex) {
            //ignore
        }
        try {
            remove(intermediateAddress);
        } catch (MgmtOperationException ex) {
            //ignore
        }
    }

    @Test
    public void testAddressSettingWrite() throws Exception {
        // <jms server address>/address-setting=jms.queue.foo:write-attribute(name=redelivery-delay,value=50)
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
        resolve.get(ModelDescriptionConstants.OP_ADDR).set(jmsOperations.getServerAddress());
        resolve.get(ModelDescriptionConstants.OP).set(RESOLVE_ADDRESS_SETTING);
        resolve.get(ACTIVEMQ_ADDRESS).set("jms.queue.foo");
        ModelNode result = executeOperation(resolve);

        for (String attributeName : attributeNames) {
            switch (attributeName) {
                case "max-read-page-bytes":
                    assertEquals("unexpected value for " + attributeName, 2*result.get("page-size-bytes").asInt(), result.get(attributeName).asInt());
                    break;
                case "max-redelivery-delay":
                    assertEquals("unexpected value for " + attributeName, 10*result.get("redelivery-delay").asLong(), result.get(attributeName).asLong());
                    break;
                default:
                    assertEquals("unexpected value for " + attributeName, defaultAddressSetting.get(attributeName), result.get(attributeName));
            }

        }
    }

    @Test
    public void testResolveMergedAddressSettings() throws Exception {
        final ModelNode readResourceDescription = new ModelNode();
        readResourceDescription.get(ModelDescriptionConstants.OP_ADDR).set(defaultAddress);
        readResourceDescription.get(ModelDescriptionConstants.OP).set(READ_RESOURCE_DESCRIPTION_OPERATION);
        final ModelNode description = executeOperation(readResourceDescription, true);
        Set<String> attributeNames = description.get(ATTRIBUTES).keys();

        final ModelNode readResource = new ModelNode();
        readResource.get(ModelDescriptionConstants.OP_ADDR).set(defaultAddress);
        readResource.get(ModelDescriptionConstants.OP).set(READ_RESOURCE_OPERATION);
        final ModelNode defaultAddressSetting = executeOperation(readResource);

        final ModelNode add = new ModelNode();
        add.get(ModelDescriptionConstants.OP).set(ADD);
        add.get(ModelDescriptionConstants.OP_ADDR).set(intermediateAddress);
        add.get("page-size-bytes").set(1024*1024);
        add.get("redelivery-delay").set(8000L);
        executeOperation(add);

        // there is no address-setting for the given address but
        // we can resolve its settings based on HornetQ hierarchical
        // repository of address setting.
        final ModelNode resolve = new ModelNode();
        resolve.get(ModelDescriptionConstants.OP_ADDR).set(jmsOperations.getServerAddress());
        resolve.get(ModelDescriptionConstants.OP).set(RESOLVE_ADDRESS_SETTING);
        resolve.get(ACTIVEMQ_ADDRESS).set("jms.queue.foo");
        ModelNode result = executeOperation(resolve);

        for (String attributeName : attributeNames) {
            switch (attributeName) {
                case "page-size-bytes":
                    assertEquals("unexpected value for " + attributeName, 1024*1024, result.get(attributeName).asInt());
                    break;
                case "max-read-page-bytes":
                    assertEquals("unexpected value for " + attributeName, 2*1024*1024, result.get(attributeName).asInt());
                    break;
                case "redelivery-delay":
                    assertEquals("unexpected value for " + attributeName, 8000L, result.get(attributeName).asLong());
                    break;
                case "max-redelivery-delay":
                    assertEquals("unexpected value for " + attributeName, 10*8000L, result.get(attributeName).asLong());
                    break;
                default:
                    assertEquals("unexpected value for " + attributeName, defaultAddressSetting.get(attributeName), result.get(attributeName));
            }
        }
    }

    @Test
    public void testSpecificAddressSetting() throws Exception {
        // <jms server address>/address-setting=jms.queue.foo:add()
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
        assertFalse(result.hasDefined(MESSAGE_COUNTER_HISTORY_DAY_LIMIT));

        final ModelNode resolve = new ModelNode();
        resolve.get(ModelDescriptionConstants.OP_ADDR).set(jmsOperations.getServerAddress());
        resolve.get(ModelDescriptionConstants.OP).set(RESOLVE_ADDRESS_SETTING);
        resolve.get(ACTIVEMQ_ADDRESS).set("jms.queue.foo");
        result = executeOperation(resolve);
        // inherit the message-counter-history-day-limit for the '#' address-setting
        assertEquals(10, result.get(MESSAGE_COUNTER_HISTORY_DAY_LIMIT).asInt());
    }
}

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

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
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

    private static final ModelNode address = new ModelNode();

    static {
        address.add("subsystem", "messaging");
        address.add("hornetq-server", "default");
        address.add("address-setting", "test");
    }

    @Test
    public void testAddressSettingWrite() throws Exception {
        // /subsystem=messaging/hornetq-server=default/address-setting=#:write-attribute(name=redelivery-delay,value=50)
        final ModelNode add = new ModelNode();
        add.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.ADD);
        add.get(ModelDescriptionConstants.OP_ADDR).set(address);

        executeOperation(add);

        final ModelNode update = new ModelNode();
        update.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION);
        update.get(ModelDescriptionConstants.OP_ADDR).set(address);
        update.get(ModelDescriptionConstants.NAME).set("redistribution-delay");
        update.get(ModelDescriptionConstants.VALUE).set(-1L);

        executeOperation(update);

        final ModelNode remove = new ModelNode();
        remove.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.REMOVE);
        remove.get(ModelDescriptionConstants.OP_ADDR).set(address);

        executeOperation(remove);
    }

}

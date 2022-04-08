/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.manualmode.messaging.ha;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INCLUDE_RUNTIME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PATH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.jboss.as.test.manualmode.messaging.ha.AbstractMessagingHATestCase.execute;

import java.io.File;
import java.security.AccessController;
import java.security.PrivilegedAction;
import org.jboss.as.controller.PathAddress;

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.test.integration.common.jms.JMSOperations;
import org.jboss.as.test.integration.common.jms.JMSOperationsProvider;
import org.jboss.dmr.ModelNode;
import org.jgroups.util.StackType;
import org.jgroups.util.Util;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;

/** *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2015 Red Hat inc.
 */
public class SharedStoreFailoverTestCase extends FailoverTestCase {

    private static final File SHARED_STORE_DIR = new File(System.getProperty("java.io.tmpdir"), "activemq");
    private static final ModelNode MASTER_STORE_ADDRESS = PathAddress.parseCLIStyleAddress("/subsystem=messaging-activemq/server=default/ha-policy=shared-store-master").toModelNode();
    private static final ModelNode SLAVE_STORE_ADDRESS = PathAddress.parseCLIStyleAddress("/subsystem=messaging-activemq/server=default/ha-policy=shared-store-slave").toModelNode();

    @BeforeClass
    public static void beforeClass() {
        AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
            Assume.assumeFalse("[WFCI-32] Disable on Windows+IPv6 until CI environment is fixed", Util.checkForWindows() && (Util.getIpStackType() == StackType.IPv6));
            return null;
        });
    }

    @Before
    @Override
    public void setUp() throws Exception {
        // remove shared store files
        deleteRecursive(SHARED_STORE_DIR);
        SHARED_STORE_DIR.mkdirs();
        super.setUp();
    }
    @Override
    protected void setUpServer1(ModelControllerClient client) throws Exception {
        // /subsystem=messaging-activemq/server=default/ha-policy=shared-store-master:add(failover-on-server-shutdown=true)
        ModelNode operation = Operations.createAddOperation(MASTER_STORE_ADDRESS);
        operation.get("failover-on-server-shutdown").set(true);
        execute(client, operation);

        configureSharedStore(client);

        JMSOperations jmsOperations = JMSOperationsProvider.getInstance(client);
        jmsOperations.createJmsQueue(jmsQueueName, "java:jboss/exported/" + jmsQueueLookup);
    }

    @Override
    protected void setUpServer2(ModelControllerClient client) throws Exception {
        // /subsystem=messaging-activemq/server=default/ha-policy=shared-store-slave:add(restart-backup=true)
        ModelNode operation = Operations.createAddOperation(SLAVE_STORE_ADDRESS);
        operation.get("restart-backup").set(true);
        execute(client, operation);

        configureSharedStore(client);

        JMSOperations jmsOperations = JMSOperationsProvider.getInstance(client);
        jmsOperations.createJmsQueue(jmsQueueName, "java:jboss/exported/" + jmsQueueLookup);
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        // remove shared store files
        deleteRecursive(SHARED_STORE_DIR);
    }

    private void configureSharedStore(ModelControllerClient client) throws Exception {
        ModelNode operation = new ModelNode();
        operation.get(OP_ADDR).add("subsystem", "messaging-activemq");
        operation.get(OP_ADDR).add("server", "default");
        operation.get(OP).set(READ_RESOURCE_OPERATION);
        operation.get(INCLUDE_RUNTIME).set(true);
        execute(client, operation);

        operation = Operations.createWriteAttributeOperation(
                PathAddress.parseCLIStyleAddress("/subsystem=messaging-activemq/server=default").toModelNode(),
                "cluster-password",
                "guest");
        execute(client, operation);

        for (String path : new String[] {"journal-directory",
                "large-messages-directory",
                "bindings-directory",
                "paging-directory"
        }) {
            // /subsystem=messaging-activemq/server=default/path=XXX:wite-attribute(name=path, value=YYY)
            File f = new File(SHARED_STORE_DIR, path);
            ModelNode undefineRelativeToAttribute = Operations.createWriteAttributeOperation(
            PathAddress.parseCLIStyleAddress("/subsystem=messaging-activemq/server=default/path=" + path).toModelNode(), PATH, f.getAbsolutePath());
            execute(client, undefineRelativeToAttribute);
        }
    }
}

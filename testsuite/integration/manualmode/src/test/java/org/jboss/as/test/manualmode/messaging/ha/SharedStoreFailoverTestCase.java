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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INCLUDE_RUNTIME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PATH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.jboss.as.test.shared.IntermittentFailure.thisTestIsFailingIntermittently;

import java.io.File;

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.test.integration.common.jms.JMSOperations;
import org.jboss.as.test.integration.common.jms.JMSOperationsProvider;
import org.jboss.dmr.ModelNode;
import org.junit.Before;
import org.junit.BeforeClass;

/**
 * IGNORE until https://issues.apache.org/jira/browse/ARTEMIS-138 is fixed.
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2015 Red Hat inc.
 */
public class SharedStoreFailoverTestCase extends FailoverTestCase {

    private static final File SHARED_STORE_DIR = new File(System.getProperty("java.io.tmpdir"), "activemq");

    @BeforeClass
    public static void beforeClass() {
        thisTestIsFailingIntermittently("WFLY-5531");
    }

    @Before
    @Override
    public void setUp() throws Exception {

        if (!SHARED_STORE_DIR.exists()) {
            SHARED_STORE_DIR.mkdirs();
        }
        super.setUp();
    }
    @Override
    protected void setUpServer1(ModelControllerClient client) throws Exception {
        // /subsystem=messaging-activemq/server=default/ha-policy=shared-store-master:add(failover-on-server-shutdown=true)
        ModelNode operation = new ModelNode();
        operation.get(OP_ADDR).add("subsystem", "messaging-activemq");
        operation.get(OP_ADDR).add("server", "default");
        operation.get(OP_ADDR).add("ha-policy", "shared-store-master");
        operation.get(OP).set(ADD);
        operation.get("failover-on-server-shutdown").set(true);
        execute(client, operation);

        configureSharedStore(client);

        JMSOperations jmsOperations = JMSOperationsProvider.getInstance(client);
        jmsOperations.createJmsQueue(jmsQueueName, "java:jboss/exported/" + jmsQueueLookup);
    }

    @Override
    protected void setUpServer2(ModelControllerClient client) throws Exception {
        // /subsystem=messaging-activemq/server=default/ha-policy=shared-store-slave:add(restart-backup=true)
        ModelNode operation = new ModelNode();
        operation.get(OP_ADDR).add("subsystem", "messaging-activemq");
        operation.get(OP_ADDR).add("server", "default");
        operation.get(OP_ADDR).add("ha-policy", "shared-store-slave");
        operation.get(OP).set(ADD);
        operation.get("restart-backup").set(true);
        execute(client, operation);

        configureSharedStore(client);

        JMSOperations jmsOperations = JMSOperationsProvider.getInstance(client);
        jmsOperations.createJmsQueue(jmsQueueName, "java:jboss/exported/" + jmsQueueLookup);

    }

    @Override
    public void tearDown() throws Exception {
        // remove shared store files
        deleteRecursive(SHARED_STORE_DIR);

        super.tearDown();
    }

    private void configureSharedStore(ModelControllerClient client) throws Exception {
        ModelNode operation = new ModelNode();
        operation.get(OP_ADDR).add("subsystem", "messaging-activemq");
        operation.get(OP_ADDR).add("server", "default");
        operation.get(OP).set(READ_RESOURCE_OPERATION);
        operation.get(INCLUDE_RUNTIME).set(true);
        execute(client, operation);

        for (String path : new String[] {"journal-directory",
                "large-messages-directory",
                "bindings-directory",
                "paging-directory"
        }) {
            // /subsystem=messaging-activemq/server=default/path=XXX:add(path=YYY)
            ModelNode undefineRelativeToAttribute = new ModelNode();
            undefineRelativeToAttribute.get(OP_ADDR).add("subsystem", "messaging-activemq");
            undefineRelativeToAttribute.get(OP_ADDR).add("server", "default");
            undefineRelativeToAttribute.get(OP_ADDR).add("path", path);
            undefineRelativeToAttribute.get(OP).set(ADD);
            File f = new File(SHARED_STORE_DIR, path);
            undefineRelativeToAttribute.get(PATH).set(f.getAbsolutePath());
            execute(client, undefineRelativeToAttribute);
        }
    }

    private static void deleteRecursive(File file) {
        File[] files = file.listFiles();
        if(files != null) {
            File[] var2 = files;
            int var3 = files.length;

            for(int var4 = 0; var4 < var3; ++var4) {
                File f = var2[var4];
                deleteRecursive(f);
            }
        }

        file.delete();
    }
}

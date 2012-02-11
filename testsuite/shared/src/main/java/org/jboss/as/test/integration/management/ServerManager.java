/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.management;

import org.jboss.as.test.integration.management.base.AbstractMgmtTestBase;
import org.jboss.as.test.integration.management.util.WebUtil;
import org.jboss.dmr.ModelNode;
import java.util.HashSet;
import java.util.List;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import static org.jboss.as.test.integration.management.util.ModelUtil.createOpNode;

/**
 * Shared class for all mamagement operations needed by tests.
 *
 * @author Dominik Pospisil <dpospisi@redhat.com>
 * @author <a href="mailto:pskopek@redhat.com">Peter Skopek</a>
 */
public class ServerManager extends AbstractMgmtTestBase {
    private int mgmtPort;
    private String mgmtHostName;

    /**
     * Constructor using default hostname and management port.
     */
    public ServerManager() {
        this("localhost", MGMT_PORT);
    }

    /**
     * Constructor with specific hostname and management port.
     * @param mgmtHostName
     * @param mgmtPort
     */
    public ServerManager(String mgmtHostName, int mgmtPort) {
        this.mgmtHostName = mgmtHostName;
        this.mgmtPort = mgmtPort;
    }

    /**
     * Constructor with specific hostname and default management port.
     * @param mgmtHostName
     */
    public ServerManager(String mgmtHostName) {
        this.mgmtHostName = mgmtHostName;
        this.mgmtPort = MGMT_PORT;
    }


    public int getMgmtPort() {
        return mgmtPort;
    }

    public void addConnector(Connector conn, int port, String keyPEMFile, String certPEMFile, String keyStoreFile, String password) throws Exception {
        // add socket binding
        ModelNode op = getAddSocketBindingOp(conn, port);
        executeOperation(op);

        // add connector
        op = getAddConnectorOp(conn, keyPEMFile, certPEMFile, keyStoreFile, password);
        executeOperation(op);

        // check it is listed
        assertTrue(getConnectorList().contains("test-" + conn.getName() + "-connector"));
    }

    private ModelNode getAddSocketBindingOp(Connector conn, int port) {
        ModelNode op = createOpNode("socket-binding-group=standard-sockets/socket-binding=test-" + conn.getName(), "add");
        op.get("port").set(port);
        return op;
    }

    private ModelNode getAddConnectorOp(Connector conn, String keyPEMFile, String certPEMFile, String keyStoreFile, String password) {
        ModelNode op = createOpNode("subsystem=web/connector=test-" + conn.getName() + "-connector", "add");
        op.get("socket-binding").set("test-" + conn.getName());
        op.get("scheme").set(conn.getScheme());
        op.get("protocol").set(conn.getProtrocol());
        op.get("secure").set(conn.isSecure());
        op.get("enabled").set(true);
        if (conn.isSecure()) {
            ModelNode ssl = new ModelNode();
            if (conn.equals(Connector.HTTPSNATIVE)) {
                ssl.get("certificate-key-file").set(keyPEMFile);
                    ssl.get("certificate-file").set(certPEMFile);
            } else {
                ssl.get("certificate-key-file").set(keyStoreFile);
            }
            ssl.get("password").set(password);
            op.get("ssl").set(ssl);
        }
        return op;
    }

    public void removeConnector(Connector conn, String checkURL) throws Exception {
        // remove connector
        ModelNode op = getRemoveConnectorOp(conn);
        executeOperation(op);

        Thread.sleep(5000);
        // check that the connector is not live

        if (checkURL != null)
            assertFalse("Connector not removed.", WebUtil.testHttpURL(checkURL));

        // remove socket binding
        op = getRemoveSocketBindingOp(conn);
        executeOperation(op);
    }

    private ModelNode getRemoveSocketBindingOp(Connector conn) {
        ModelNode op = createOpNode("socket-binding-group=standard-sockets/socket-binding=test-" + conn.getName(), "remove");
        return op;
    }

    private ModelNode getRemoveConnectorOp(Connector conn) {
        ModelNode op = createOpNode("subsystem=web/connector=test-" + conn.getName() + "-connector", "remove");
        return op;
    }

    public HashSet<String> getConnectorList() throws Exception {
        ModelNode op = createOpNode("subsystem=web", "read-children-names");
        op.get("child-type").set("connector");
        ModelNode result = executeOperation(op);
        List<ModelNode> connectors = result.asList();
        HashSet<String> connNames = new HashSet<String>();
        for (ModelNode n : connectors) {
            connNames.add(n.asString());
        }

        return connNames;
    }


    public void initModelControllerClient() {
        initModelControllerClient(mgmtHostName, getMgmtPort());
    }
}

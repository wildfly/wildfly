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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ALLOW_RESOURCE_SERVICE_RESTART;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.COMPOSITE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CORE_SERVICE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SECURITY_REALM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_IDENTITY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SSL;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STEPS;
import static org.jboss.as.test.integration.management.util.ModelUtil.createOpNode;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.security.Constants;
import org.jboss.as.test.integration.management.base.AbstractMgmtTestBase;
import org.jboss.as.test.integration.management.util.WebUtil;
import org.jboss.dmr.ModelNode;

/**
 * Shared class for all mamagement operations needed by tests.
 *
 * @author Dominik Pospisil <dpospisi@redhat.com>
 * @author <a href="mailto:pskopek@redhat.com">Peter Skopek</a>
 */
public class ServerManager extends AbstractMgmtTestBase {

    private final ManagementClient managementClient;

    public ServerManager(final ManagementClient managementClient) {
        this.managementClient = managementClient;
    }

    @Override
    protected ModelControllerClient getModelControllerClient() {
        return managementClient.getControllerClient();
    }

    public void addListener(Listener conn, int port, String keyPEMFile, String certPEMFile, String keyStoreFile, String password) throws Exception {
        // add socket binding
        ModelNode op = getAddSocketBindingOp(conn, port);
        executeOperation(op);

        // add connector
        op = getAddListenerOp(conn, keyStoreFile, password);
        executeOperation(op);

        // check it is listed
        assertTrue(getListenerList().get(conn.getScheme()).contains("test-" + conn.getName() + "-listener"));
    }

    private ModelNode getAddSocketBindingOp(Listener conn, int port) {
        ModelNode op = createOpNode("socket-binding-group=standard-sockets/socket-binding=test-" + conn.getName(), "add");
        op.get("port").set(port);
        return op;
    }

    private ModelNode getAddListenerOp(Listener conn, String keyStoreFile, String password) {
        final ModelNode composite = Util.getEmptyOperation(COMPOSITE, new ModelNode());
        final ModelNode steps = composite.get(STEPS);
        ModelNode op = createOpNode("subsystem=undertow/server=default-server/" + conn.getScheme() + "-listener=test-" + conn.getName() + "-listener", "add");
        op.get("socket-binding").set("test-" + conn.getName());
        if (conn.isSecure()) {
            addSecurityRealm(steps, keyStoreFile, password);
            op.get("security-realm").set("ssl-realm");
        }
        steps.add(op);
        return composite;
    }

    private void addSecurityRealm(ModelNode steps, String keyStoreFile, String password) {
        final PathAddress realmAddr = PathAddress.pathAddress().append(CORE_SERVICE, MANAGEMENT)
                .append(SECURITY_REALM, "ssl-realm");
        ModelNode op = Util.createAddOperation(realmAddr);
        steps.add(op);

        // /core-service=management/security-realm=JBossTest/server-identity=ssl:add(keystore-path=server.keystore, keystore-password=123456)

        final ModelNode sslModuleNode = Util.createAddOperation(realmAddr.append(SERVER_IDENTITY, SSL));
        sslModuleNode.get("keystore-path").set(keyStoreFile);
        sslModuleNode.get(Constants.KEYSTORE_PASSWORD).set(password);
        sslModuleNode.get(OPERATION_HEADERS, ALLOW_RESOURCE_SERVICE_RESTART).set(true);
        steps.add(sslModuleNode);
    }

    private ModelNode getRemoveSecurityRealm() {
        final PathAddress realmAddr = PathAddress.pathAddress().append(CORE_SERVICE, MANAGEMENT)
                .append(SECURITY_REALM, "ssl-realm");
        return Util.createRemoveOperation(realmAddr);
    }

    public void removeListener(Listener conn, String checkURL) throws Exception {
        // remove connector
        ModelNode op = getRemoveListenerOp(conn);
        executeOperation(op);

        Thread.sleep(1000);
        // check that the connector is not live

        if (checkURL != null) { assertFalse("Listener not removed.", WebUtil.testHttpURL(checkURL)); }

        // remove socket binding
        op = getRemoveSocketBindingOp(conn);
        executeOperation(op);
    }

    private ModelNode getRemoveSocketBindingOp(Listener conn) {
        ModelNode op = createOpNode("socket-binding-group=standard-sockets/socket-binding=test-" + conn.getName(), "remove");
        op.get(OPERATION_HEADERS).get(ALLOW_RESOURCE_SERVICE_RESTART).set(true);
        return op;
    }

    private ModelNode getRemoveListenerOp(Listener conn) {
        final ModelNode composite = Util.getEmptyOperation(COMPOSITE, new ModelNode());
        final ModelNode steps = composite.get(STEPS);
        ModelNode op = createOpNode("subsystem=undertow/server=default-server/" + conn.getScheme() + "-listener=test-" + conn.getName() + "-listener", "remove");
        op.get(OPERATION_HEADERS, ALLOW_RESOURCE_SERVICE_RESTART).set(true);
        steps.add(op);
        if (conn.isSecure()) {
            steps.add(getRemoveSecurityRealm());
        }
        return composite;
    }

    private Map<String, Set<String>> getListenerList() throws Exception {
        HashMap<String, Set<String>> result = new HashMap<>();
        result.put("http", getListenersByType("http-listener"));
        result.put("https", getListenersByType("https-listener"));
        result.put("ajp", getListenersByType("ajp-listener"));

        return result;
    }

    private Set<String> getListenersByType(String type) throws Exception {
        ModelNode op = createOpNode("subsystem=undertow/server=default-server", "read-children-names");
        op.get("child-type").set(type);
        ModelNode result = executeOperation(op);
        List<ModelNode> connectors = result.asList();
        HashSet<String> connNames = new HashSet<>();
        for (ModelNode n : connectors) {
            connNames.add(n.asString());

        }
        return connNames;
    }
}

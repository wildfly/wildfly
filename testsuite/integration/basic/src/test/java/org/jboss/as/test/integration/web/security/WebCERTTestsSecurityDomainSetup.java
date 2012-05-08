/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.web.security;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ALLOW_RESOURCE_SERVICE_RESTART;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.COMPOSITE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ROLLBACK_ON_RUNTIME_FAILURE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STEPS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.security.Constants.AUTHENTICATION;
import static org.jboss.as.security.Constants.CODE;
import static org.jboss.as.security.Constants.FLAG;
import static org.jboss.as.security.Constants.JSSE;
import static org.jboss.as.security.Constants.MODULE_OPTIONS;
import static org.jboss.as.security.Constants.PASSWORD;
import static org.jboss.as.security.Constants.SECURITY_DOMAIN;
import static org.jboss.as.security.Constants.TRUSTSTORE;
import static org.jboss.as.security.Constants.URL;
import static org.jboss.as.test.integration.management.util.ModelUtil.createOpNode;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.security.Constants;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;

/**
 * {@code ServerSetupTask} for the Web CERT tests.
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
public class WebCERTTestsSecurityDomainSetup implements ServerSetupTask {

    private static final Logger log = Logger.getLogger(WebCERTTestsSecurityDomainSetup.class);
    
    private static final String APP_SECURITY_DOMAIN = "cert-test";
    
    private static final String JSSE_SECURITY_DOMAIN = "cert";

    protected static void applyUpdates(final ModelControllerClient client, final List<ModelNode> updates) {
        for (ModelNode update : updates) {
            try {
                applyUpdate(client, update, false);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    protected static void applyUpdate(final ModelControllerClient client, ModelNode update, boolean allowFailure) throws Exception {
        ModelNode result = client.execute(new OperationBuilder(update).build());
        if (result.hasDefined("outcome") && (allowFailure || "success".equals(result.get("outcome").asString()))) {
            if (result.hasDefined("result")) {
                System.out.println(result.get("result"));
            }
        } else if (result.hasDefined("failure-description")) {
            throw new RuntimeException(result.get("failure-description").toString());
        } else {
            throw new RuntimeException("Operation not successful; outcome = " + result.get("outcome"));
        }
    }

    @Override
    public void setup(ManagementClient managementClient, String containerId) throws Exception {
        log.debug("start of the domain creation");

        final List<ModelNode> updates = new ArrayList<ModelNode>();
        ModelNode op = new ModelNode();

        // Add the cert-test security domain.
        op.get(OP).set(ADD);
        op.get(OP_ADDR).add(SUBSYSTEM, "security");
        op.get(OP_ADDR).add(SECURITY_DOMAIN, APP_SECURITY_DOMAIN);
        updates.add(op);

        op = new ModelNode();
        op.get(OP).set(ADD);
        op.get(OP_ADDR).add(SUBSYSTEM, "security");
        op.get(OP_ADDR).add(SECURITY_DOMAIN, APP_SECURITY_DOMAIN);
        op.get(OP_ADDR).add(AUTHENTICATION, Constants.CLASSIC);

        ModelNode loginModule = op.get(Constants.LOGIN_MODULES).add();
        loginModule.get(CODE).set("CertificateRoles");
        loginModule.get(FLAG).set("required");
        ModelNode moduleOptions = loginModule.get(MODULE_OPTIONS);
        moduleOptions.add("securityDomain", JSSE_SECURITY_DOMAIN);
        op.get(OPERATION_HEADERS).get(ALLOW_RESOURCE_SERVICE_RESTART).set(true);
        updates.add(op);

        // Add the JSSE security domain.
        op = new ModelNode();
        op.get(OP).set(ADD);
        op.get(OP_ADDR).add(SUBSYSTEM, "security");
        op.get(OP_ADDR).add(SECURITY_DOMAIN, JSSE_SECURITY_DOMAIN);
        updates.add(op);

        op = new ModelNode();
        op.get(OP).set(ADD);
        op.get(OP_ADDR).add(SUBSYSTEM, "security");
        op.get(OP_ADDR).add(SECURITY_DOMAIN, JSSE_SECURITY_DOMAIN);
        op.get(OP_ADDR).add(JSSE, Constants.CLASSIC);

        op.get(TRUSTSTORE, PASSWORD).set("changeit");
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        URL keystore = tccl.getResource("security/jsse.keystore");
        op.get(TRUSTSTORE, URL).set(keystore.getPath());
        op.get(OPERATION_HEADERS).get(ALLOW_RESOURCE_SERVICE_RESTART).set(true);
        updates.add(op);

        // Add the HTTPS socket binding.
        op = new ModelNode();
        op.get(OP).set(ADD);
        op.get(OP_ADDR).add("socket-binding-group", "standard-sockets");
        op.get(OP_ADDR).add("socket-binding", "https-test");
        op.get("interface").set("public");
        op.get("port").set(8380);
        updates.add(op);

        // Add the HTTPS connector.
        final ModelNode composite = Util.getEmptyOperation(COMPOSITE, new ModelNode());
        final ModelNode steps = composite.get(STEPS);
        op = new ModelNode();
        op.get(OP).set(ADD);
        op.get(OP_ADDR).add(SUBSYSTEM, "web");
        op.get(OP_ADDR).add("connector", "testConnector");
        op.get("socket-binding").set("https-test");
        op.get("enabled").set(true);
        op.get("protocol").set("HTTP/1.1");
        op.get("scheme").set("https");
        op.get("secure").set(true);
        steps.add(op);
        ModelNode ssl = createOpNode("subsystem=web/connector=testConnector/ssl=configuration", "add");
        ssl.get("name").set("https-test");
        ssl.get("key-alias").set("test");
        ssl.get("password").set("changeit");
        keystore = tccl.getResource("security/server.keystore");
        ssl.get("certificate-key-file").set(keystore.getPath());
        ssl.get("ca-certificate-file").set(keystore.getPath());
        ssl.get("verify-client").set("want");
        steps.add(ssl);
        updates.add(composite);

        applyUpdates(managementClient.getControllerClient(), updates);

        log.debug("end of the domain creation");
    }

    @Override
    public void tearDown(ManagementClient managementClient, String containerId) {
        final List<ModelNode> updates = new ArrayList<ModelNode>();

        // remove the security domains.
        ModelNode op = new ModelNode();
        op.get(OP).set(REMOVE);
        op.get(OP_ADDR).add(SUBSYSTEM, "security");
        op.get(OP_ADDR).add(Constants.SECURITY_DOMAIN, APP_SECURITY_DOMAIN);
        // Don't rollback when the AS detects the war needs the module
        op.get(OPERATION_HEADERS, ROLLBACK_ON_RUNTIME_FAILURE).set(false);
        updates.add(op);

        op = new ModelNode();
        op.get(OP).set(REMOVE);
        op.get(OP_ADDR).add(SUBSYSTEM, "security");
        op.get(OP_ADDR).add(Constants.SECURITY_DOMAIN, JSSE_SECURITY_DOMAIN);
        // Don't rollback when the AS detects the war needs the module
        op.get(OPERATION_HEADERS, ROLLBACK_ON_RUNTIME_FAILURE).set(false);
        updates.add(op);

        // remove the HTTPS connector and the socket binding.
        op = new ModelNode();
        op.get(OP).set(REMOVE);
        op.get(OP_ADDR).add(SUBSYSTEM, "web");
        op.get(OP_ADDR).add("connector", "testConnector");
        updates.add(op);

        op = new ModelNode();
        op.get(OP).set(REMOVE);
        op.get(OP_ADDR).add("socket-binding-group", "standard-sockets");
        op.get(OP_ADDR).add("socket-binding", "https-test");
        updates.add(op);

        applyUpdates(managementClient.getControllerClient(), updates);
    }
}

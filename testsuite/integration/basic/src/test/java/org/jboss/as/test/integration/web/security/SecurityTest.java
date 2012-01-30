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


import static org.jboss.as.arquillian.container.Authentication.getCallbackHandler;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ALLOW_RESOURCE_SERVICE_RESTART;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ROLLBACK_ON_RUNTIME_FAILURE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.security.Constants.*;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.as.security.Constants;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.junit.AfterClass;

/**
 * Utility methods to create/remove simple security domains
 *
 * @author <a href="mailto:mmoyses@redhat.com">Marcus Moyses</a>
 */
public class SecurityTest {

    protected static String securityDomain = "web-tests";

    private static final Logger log = Logger.getLogger(SecurityTest.class);
    
    // this is removing the security domain after each test so I have to disable it
    @AfterClass
    public static void after() throws Exception {
        // remove test security domains
        removeSecurityDomain();
    }

    public static void createSecurityDomain() throws Exception {
        final ModelControllerClient client = ModelControllerClient.Factory.create(InetAddress.getByName("localhost"), 9999, getCallbackHandler());

        final List<ModelNode> updates = new ArrayList<ModelNode>();
        ModelNode op = new ModelNode();

        op.get(OP).set(ADD);
        op.get(OP_ADDR).add(SUBSYSTEM, "security");
        op.get(OP_ADDR).add(SECURITY_DOMAIN, securityDomain);
        updates.add(op);

        op = new ModelNode();
        op.get(OP).set(ADD);
        op.get(OP_ADDR).add(SUBSYSTEM, "security");
        op.get(OP_ADDR).add(SECURITY_DOMAIN, securityDomain);
        op.get(OP_ADDR).add(AUTHENTICATION, Constants.CLASSIC);

        ModelNode loginModule = op.get(Constants.LOGIN_MODULES).add();
        loginModule.get(CODE).set("UsersRoles");
        loginModule.get(FLAG).set("required");
        op.get(OPERATION_HEADERS).get(ALLOW_RESOURCE_SERVICE_RESTART).set(true);
        updates.add(op);



        applyUpdates(updates, client);
    }

    public static void createSimpleRoleMappingSecurityDomain() throws Exception {
        
        log.debug("start of createSimpleRoleMappingSecurityDomain");

        final ModelControllerClient client = ModelControllerClient.Factory.create(InetAddress.getByName("localhost"), 9999, getCallbackHandler());

        final List<ModelNode> updates = new ArrayList<ModelNode>();
        ModelNode op = new ModelNode();

        op.get(OP).set(ADD);
        op.get(OP_ADDR).add(SUBSYSTEM, "security");
        op.get(OP_ADDR).add(SECURITY_DOMAIN, securityDomain);
        updates.add(op);

        op = new ModelNode();
        op.get(OP).set(ADD);
        op.get(OP_ADDR).add(SUBSYSTEM, "security");
        op.get(OP_ADDR).add(SECURITY_DOMAIN, securityDomain);
        op.get(OP_ADDR).add(AUTHENTICATION, Constants.CLASSIC);
        op.get(OP_ADDR).add(MAPPING, CLASSIC);

        ModelNode loginModule = op.get(Constants.LOGIN_MODULES).add();
        loginModule.get(CODE).set("UsersRoles");
        loginModule.get(FLAG).set("required");

        ModelNode mappingModule =op.get(MAPPING_MODULES).add();
        mappingModule.get(CODE).set("SimpleRoles"); // see:  https://docs.jboss.org/author/display/AS71/Security+subsystem+configuration
        mappingModule.get(TYPE).set("role");
        ModelNode mappingOptions = mappingModule.get(MODULE_OPTIONS);
        ModelNode moduleOption = mappingOptions.add();
        moduleOption.add("peter", "superuser,gooduser");

        op.get(OPERATION_HEADERS).get(ALLOW_RESOURCE_SERVICE_RESTART).set(true);
        updates.add(op);

        try {
            applyUpdates(updates, client);
        }
        catch (Exception e) {
            log.error(e);
        }
        log.debug("end of createSimpleRoleMappingSecurityDomain");
    }

    public static void removeSecurityDomain() throws Exception {
        final ModelControllerClient client = ModelControllerClient.Factory.create(InetAddress.getByName("localhost"), 9999, getCallbackHandler());

        ModelNode op = new ModelNode();
        op.get(OP).set(REMOVE);
        op.get(OP_ADDR).add(SUBSYSTEM, "security");
        op.get(OP_ADDR).add(SECURITY_DOMAIN, securityDomain);
        // Don't rollback when the AS detects the war needs the module
        op.get(OPERATION_HEADERS, ROLLBACK_ON_RUNTIME_FAILURE).set(false);

        applyUpdate(op, client, true);
    }

    public static void applyUpdates(final List<ModelNode> updates, final ModelControllerClient client) throws Exception {
        for (ModelNode update : updates) {
            applyUpdate(update, client, false);
        }
    }

    public static void applyUpdate(ModelNode update, final ModelControllerClient client, boolean allowFailure) throws Exception {
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

}

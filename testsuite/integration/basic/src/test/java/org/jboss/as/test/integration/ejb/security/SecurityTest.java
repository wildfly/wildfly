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

package org.jboss.as.test.integration.ejb.security;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

import org.apache.openjpa.lib.log.Log;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.as.security.Constants;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;

import static org.jboss.as.arquillian.container.Authentication.getCallbackHandler;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ALLOW_RESOURCE_SERVICE_RESTART;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.security.Constants.AUTHENTICATION;
import static org.jboss.as.security.Constants.CODE;
import static org.jboss.as.security.Constants.FLAG;
import static org.jboss.as.security.Constants.SECURITY_DOMAIN;

/**
 * Utility methods to create/remove simple security domains
 *
 * @author <a href="mailto:mmoyses@redhat.com">Marcus Moyses</a>
 */
public class SecurityTest {

    private static final Logger logger = Logger.getLogger(SecurityTest.class);

    protected static String defaultSecurityDomainName = "ejb3-tests";

    // this is removing the security domain after each test so I have to disable it
//  @AfterClass
//  public static void after() throws Exception {
//      // remove test security domains
//      removeSecurityDomain();
//  }

    public static void createSecurityDomain() throws Exception {
        createSecurityDomain(defaultSecurityDomainName);
    }

    public static void createSecurityDomain(final String securityDomainName) throws Exception {
        createSecurityDomain(securityDomainName, true);
    }

    public static void createSecurityDomain(final String securityDomainName, boolean usersRolesRequired) throws Exception {
        final ModelControllerClient client = ModelControllerClient.Factory.create(InetAddress.getByName("localhost"), 9999, getCallbackHandler());
        try {
            final List<ModelNode> updates = new ArrayList<ModelNode>();
            ModelNode op = new ModelNode();
            op.get(OP).set(ADD);
            op.get(OP_ADDR).add(SUBSYSTEM, "security");
            op.get(OP_ADDR).add(SECURITY_DOMAIN, securityDomainName);
            updates.add(op);

            op = new ModelNode();
            op.get(OP).set(ADD);
            op.get(OP_ADDR).add(SUBSYSTEM, "security");
            op.get(OP_ADDR).add(SECURITY_DOMAIN, securityDomainName);
            op.get(OP_ADDR).add(AUTHENTICATION, Constants.CLASSIC);

            ModelNode loginModule = op.get(Constants.LOGIN_MODULES).add();
            loginModule.get(CODE).set("Remoting");
            if(usersRolesRequired) {
                loginModule.get(FLAG).set("optional");
            } else {
                loginModule.get(FLAG).set("required");
            }
            loginModule.get(Constants.MODULE_OPTIONS).add("password-stacking", "useFirstPass");

            if (usersRolesRequired) {
                loginModule = op.get(Constants.LOGIN_MODULES).add();
                loginModule.get(CODE).set("UsersRoles");
                loginModule.get(FLAG).set("required");
                loginModule.get(Constants.MODULE_OPTIONS).add("password-stacking", "useFirstPass");
            }
            op.get(OPERATION_HEADERS).get(ALLOW_RESOURCE_SERVICE_RESTART).set(true);
            updates.add(op);

            applyUpdates(updates, client);
        } finally {
            try {
                client.close();
            } catch (Exception e) {
                // ignore
                logger.debug("Ignoring exception while closing model controller client", e);
            }
        }
    }

    public static void removeSecurityDomain() throws Exception {
        removeSecurityDomain(defaultSecurityDomainName);
    }

    public static void removeSecurityDomain(final String securityDomainName) throws Exception {
        final ModelControllerClient client = ModelControllerClient.Factory.create(InetAddress.getByName("localhost"), 9999, getCallbackHandler());
        try {
            final List<ModelNode> updates = new ArrayList<ModelNode>();
            ModelNode op = new ModelNode();
            op.get(OP).set(REMOVE);
            op.get(OP_ADDR).add(SUBSYSTEM, "security");
            op.get(OP_ADDR).add(SECURITY_DOMAIN, securityDomainName);
            updates.add(op);

            applyUpdates(updates, client);
        } catch (Exception e) {
            throw e;
        }
    }

    public static void applyUpdates(final List<ModelNode> updates, final ModelControllerClient client) throws Exception {
        for (ModelNode update : updates) {
            applyUpdate(update, client);
        }
    }

    public static void applyUpdate(ModelNode update, final ModelControllerClient client) throws Exception {
        ModelNode result = client.execute(new OperationBuilder(update).build());
        if (result.hasDefined("outcome") && "success".equals(result.get("outcome").asString())) {
            if (result.hasDefined("result")) {
                logger.infof("Result %s", result.get("result"));
                System.out.println(result.get("result"));
            }
        } else if (result.hasDefined("failure-description")) {
            throw new RuntimeException(result.get("failure-description").toString());
        } else {
            throw new RuntimeException("Operation not successful; outcome = " + result.get("outcome"));
        }
    }

}

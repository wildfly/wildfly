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
package org.jboss.as.webservices.deployer;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ALLOW_RESOURCE_SERVICE_RESTART;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RELEASE_VERSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUIRED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ROLLBACK_ON_RUNTIME_FAILURE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;
import static org.jboss.as.security.Constants.AUTHENTICATION;
import static org.jboss.as.security.Constants.CLASSIC;
import static org.jboss.as.security.Constants.CODE;
import static org.jboss.as.security.Constants.FLAG;
import static org.jboss.as.security.Constants.LOGIN_MODULES;
import static org.jboss.as.security.Constants.MODULE_OPTIONS;
import static org.jboss.as.security.Constants.SECURITY_DOMAIN;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.sasl.RealmCallback;
import javax.security.sasl.RealmChoiceCallback;

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.as.controller.client.helpers.standalone.DeploymentAction;
import org.jboss.as.controller.client.helpers.standalone.DeploymentPlan;
import org.jboss.as.controller.client.helpers.standalone.DeploymentPlanBuilder;
import org.jboss.as.controller.client.helpers.standalone.ServerDeploymentActionResult;
import org.jboss.as.controller.client.helpers.standalone.ServerDeploymentManager;
import org.jboss.as.controller.client.helpers.standalone.ServerDeploymentPlanResult;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.wsf.spi.deployer.Deployer;

/**
 * Remote deployer that uses AS7 client deployment API.
 *
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 * @author <a href="mailto:alessio.soldano@jboss.com">Alessio Soldano</a>
 */
public final class RemoteDeployer implements Deployer {

    private static final Logger LOGGER = Logger.getLogger(RemoteDeployer.class);
    private static final int PORT = 9999;
    private static final String JBWS_DEPLOYER_AUTH_USER = "jbossws.deployer.authentication.username";
    private static final String JBWS_DEPLOYER_AUTH_PWD = "jbossws.deployer.authentication.password";
    private final Map<URL, String> url2Id = new HashMap<URL, String>();
    private final InetAddress address = InetAddress.getByName("127.0.0.1");
    private final CallbackHandler callbackHandler = getCallbackHandler();
    private final ServerDeploymentManager deploymentManager;

    public RemoteDeployer() throws IOException {
        deploymentManager = ServerDeploymentManager.Factory.create(address, PORT, callbackHandler);
    }

    @Override
    public void deploy(final URL archiveURL) throws Exception {
        final DeploymentPlanBuilder builder = deploymentManager.newDeploymentPlan().add(archiveURL).andDeploy();
        final DeploymentPlan plan = builder.build();
        final DeploymentAction deployAction = builder.getLastAction();
        final String uniqueId = deployAction.getDeploymentUnitUniqueName();
        executeDeploymentPlan(plan, deployAction);
        url2Id.put(archiveURL, uniqueId);
    }

    public void undeploy(final URL archiveURL) throws Exception {
        final DeploymentPlanBuilder builder = deploymentManager.newDeploymentPlan();
        final String uniqueName = url2Id.get(archiveURL);
        if (uniqueName != null) {
            final DeploymentPlan plan = builder.undeploy(uniqueName).remove(uniqueName).build();
            final DeploymentAction deployAction = builder.getLastAction();
            try {
                executeDeploymentPlan(plan, deployAction);
            } finally {
                url2Id.remove(archiveURL);
            }
        }
    }

    private void executeDeploymentPlan(final DeploymentPlan plan, final DeploymentAction deployAction) throws Exception {
        try {
            final ServerDeploymentPlanResult planResult = deploymentManager.execute(plan).get();

            if (deployAction != null) {
                final ServerDeploymentActionResult actionResult = planResult.getDeploymentActionResult(deployAction.getId());
                if (actionResult != null) {
                    final Exception deploymentException = (Exception) actionResult.getDeploymentException();
                    if (deploymentException != null)
                        throw deploymentException;
                }
            }
        } catch (final Exception e) {
            LOGGER.fatal(e.getMessage(), e);
            throw e;
        }
    }

    public String getServerVersion() throws Exception {
        final ModelNode request = new ModelNode();
        request.get(OP).set(READ_ATTRIBUTE_OPERATION);
        request.get(OP_ADDR).setEmptyList();
        request.get(NAME).set(RELEASE_VERSION);

        final ModelNode response = applyUpdate(request, getModelControllerClient());
        return response.get(RESULT).asString();
    }

    public void addSecurityDomain(String name, Map<String, String> authenticationOptions) throws Exception {
        final List<ModelNode> updates = new ArrayList<ModelNode>();

        ModelNode op = new ModelNode();
        op.get(OP).set(ADD);
        op.get(OP_ADDR).add(SUBSYSTEM, "security");
        op.get(OP_ADDR).add(SECURITY_DOMAIN, name);
        updates.add(op);

        op = new ModelNode();
        op.get(OP).set(ADD);
        op.get(OP_ADDR).add(SUBSYSTEM, "security");
        op.get(OP_ADDR).add(SECURITY_DOMAIN, name);
        op.get(OP_ADDR).add(AUTHENTICATION, CLASSIC);

        final ModelNode loginModule = op.get(LOGIN_MODULES).add();
        loginModule.get(CODE).set("UsersRoles");
        loginModule.get(FLAG).set(REQUIRED);
        op.get(OPERATION_HEADERS).get(ALLOW_RESOURCE_SERVICE_RESTART).set(true);
        updates.add(op);

        final ModelNode moduleOptions = loginModule.get(MODULE_OPTIONS);
        if (authenticationOptions != null) {
            for (final String k : authenticationOptions.keySet()) {
                moduleOptions.add(k, authenticationOptions.get(k));
            }
        }

        applyUpdates(updates, getModelControllerClient());
    }

    public void removeSecurityDomain(String name) throws Exception {
        final ModelNode op = new ModelNode();
        op.get(OP).set(REMOVE);
        op.get(OP_ADDR).add(SUBSYSTEM, "security");
        op.get(OP_ADDR).add(SECURITY_DOMAIN, name);
        op.get(OPERATION_HEADERS, ROLLBACK_ON_RUNTIME_FAILURE).set(false);

        applyUpdate(op, getModelControllerClient());
    }

    private ModelControllerClient getModelControllerClient() {
        return ModelControllerClient.Factory.create(address, PORT, callbackHandler);
    }

    private static void applyUpdates(final List<ModelNode> updates, final ModelControllerClient client) throws Exception {
        for (final ModelNode update : updates) {
            applyUpdate(update, client);
        }
    }

    private static ModelNode applyUpdate(final ModelNode update, final ModelControllerClient client) throws Exception {
        final ModelNode result = client.execute(new OperationBuilder(update).build());
        checkResult(result);
        return result;
    }

    private static void checkResult(final ModelNode result) throws Exception {
        if (result.hasDefined(OUTCOME) && SUCCESS.equals(result.get(OUTCOME).asString())) {
            if (result.hasDefined(RESULT)) {
                LOGGER.info(result.get(RESULT));
            }
        } else if (result.hasDefined(FAILURE_DESCRIPTION)) {
            throw new Exception(result.get(FAILURE_DESCRIPTION).toString());
        } else {
            throw new Exception("Operation not successful; outcome = " + result.get(OUTCOME));
        }
    }

    private static CallbackHandler getCallbackHandler() {
        final String username = getSystemProperty(JBWS_DEPLOYER_AUTH_USER, null);
        if (username == null || ("${" + JBWS_DEPLOYER_AUTH_USER + "}").equals(username)) {
           return null;
        }
        String pwd = getSystemProperty(JBWS_DEPLOYER_AUTH_PWD, null);
        if (("${" + JBWS_DEPLOYER_AUTH_PWD + "}").equals(pwd)) {
           pwd = null;
        }
        final String password = pwd;
        return new CallbackHandler() {
            public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
                for (Callback current : callbacks) {
                    if (current instanceof NameCallback) {
                        NameCallback ncb = (NameCallback) current;
                        ncb.setName(username);
                    } else if (current instanceof PasswordCallback) {
                        PasswordCallback pcb = (PasswordCallback) current;
                        pcb.setPassword(password.toCharArray());
                    } else if (current instanceof RealmCallback) {
                        RealmCallback rcb = (RealmCallback) current;
                        rcb.setText(rcb.getDefaultText());
                    } else if (current instanceof RealmChoiceCallback) {
                        // Ignored but not rejected.
                    } else {
                        throw new UnsupportedCallbackException(current);
                    }
                }
            }
        };
    }

    private static String getSystemProperty(final String name, final String defaultValue) {
        PrivilegedAction<String> action = new PrivilegedAction<String>() {
            public String run() {
                return System.getProperty(name, defaultValue);
            }
        };
        return AccessController.doPrivileged(action);
    }

}

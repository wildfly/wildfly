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

import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;

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
import org.jboss.as.controller.operations.common.Util;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.wsf.spi.deployer.Deployer;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ALLOW_RESOURCE_SERVICE_RESTART;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.COMPOSITE;
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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STEPS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;
import static org.jboss.as.security.Constants.AUTHENTICATION;
import static org.jboss.as.security.Constants.CLASSIC;
import static org.jboss.as.security.Constants.CODE;
import static org.jboss.as.security.Constants.FLAG;
import static org.jboss.as.security.Constants.LOGIN_MODULES;
import static org.jboss.as.security.Constants.MODULE_OPTIONS;
import static org.jboss.as.security.Constants.SECURITY_DOMAIN;

/**
 * Remote deployer that uses AS7 client deployment API.
 *
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 * @author <a href="mailto:alessio.soldano@jboss.com">Alessio Soldano</a>
 */
public final class RemoteDeployer implements Deployer {

    private static final Logger LOGGER = Logger.getLogger(RemoteDeployer.class);
    private static final int DEFAULT_PORT = 9999;
    private static final String JBWS_DEPLOYER_HOST = "jbossws.deployer.host";
    private static final String JBWS_DEPLOYER_PORT = "jbossws.deployer.port";
    private static final String JBWS_DEPLOYER_AUTH_USER = "jbossws.deployer.authentication.username";
    private static final String JBWS_DEPLOYER_AUTH_PWD = "jbossws.deployer.authentication.password";
    private static final CallbackHandler callbackHandler = getCallbackHandler();
    private static InetAddress address;
    private static Integer port;
    private final Map<URL, String> url2Id = new HashMap<URL, String>();
    private final Map<String, Integer> securityDomainUsers = new HashMap<String, Integer>(1);
    private final Map<String, Integer> archiveCounters = new HashMap<String, Integer>();
    private final Semaphore httpsConnSemaphore = new Semaphore(1);

    static {
        try {
            final String host = System.getProperty(JBWS_DEPLOYER_HOST);
            address = host != null ? InetAddress.getByName(host) : InetAddress.getByName("localhost");
            port = Integer.getInteger(JBWS_DEPLOYER_PORT, DEFAULT_PORT);
        } catch (final IOException e) {
            LOGGER.fatal(e.getMessage(), e);
            System.exit(1);
        }
    }

    @Override
    public void deploy(final URL archiveURL) throws Exception {
        synchronized (archiveCounters) {
            String k = archiveURL.toString();
            if (archiveCounters.containsKey(k)) {
                int count = archiveCounters.get(k);
                archiveCounters.put(k, (count + 1));
                return;
            } else {
                archiveCounters.put(k, 1);
            }
            final ServerDeploymentManager deploymentManager = newDeploymentManager();
            final DeploymentPlanBuilder builder = deploymentManager.newDeploymentPlan().add(archiveURL).andDeploy();
            final DeploymentPlan plan = builder.build();
            final DeploymentAction deployAction = builder.getLastAction();
            final String uniqueId = deployAction.getDeploymentUnitUniqueName();
            executeDeploymentPlan(plan, deployAction, deploymentManager);
            url2Id.put(archiveURL, uniqueId);
        }
    }

    @Override
    public void undeploy(final URL archiveURL) throws Exception {
        synchronized (archiveCounters) {
            String k = archiveURL.toString();
            if (archiveCounters.containsKey(k)) {
                int count = archiveCounters.get(k);
                if (count > 1) {
                    archiveCounters.put(k, (count - 1));
                    return;
                } else {
                    archiveCounters.remove(k);
                }
            } else {
                LOGGER.warn("Trying to undeploy archive " + archiveURL + " which is not currently deployed!");
                return;
            }
            final ServerDeploymentManager deploymentManager = newDeploymentManager();
            final DeploymentPlanBuilder builder = deploymentManager.newDeploymentPlan();
            final String uniqueName = url2Id.get(archiveURL);
            if (uniqueName != null) {
                final DeploymentPlan plan = builder.undeploy(uniqueName).remove(uniqueName).build();
                final DeploymentAction deployAction = builder.getLastAction();
                try {
                    executeDeploymentPlan(plan, deployAction, deploymentManager);
                } finally {
                    url2Id.remove(archiveURL);
                }
            }
        }
    }

    private void executeDeploymentPlan(final DeploymentPlan plan, final DeploymentAction deployAction,
            final ServerDeploymentManager deploymentManager) throws Exception {
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
        } finally {
            deploymentManager.close();
        }
    }

    public String getServerVersion() throws Exception {
        final ModelNode request = new ModelNode();
        request.get(OP).set(READ_ATTRIBUTE_OPERATION);
        request.get(OP_ADDR).setEmptyList();
        request.get(NAME).set(RELEASE_VERSION);

        final ModelNode response = applyUpdate(request);
        return response.get(RESULT).asString();
    }

    @Override
    public void addSecurityDomain(String name, Map<String, String> authenticationOptions) throws Exception {
        synchronized (securityDomainUsers) {
            if (securityDomainUsers.containsKey(name)) {
                int count = securityDomainUsers.get(name);
                securityDomainUsers.put(name, (count + 1));
                return;
            } else {
                securityDomainUsers.put(name, 1);
            }

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

            applyUpdates(updates);
        }
    }

    @Override
    public void removeSecurityDomain(String name) throws Exception {
        synchronized (securityDomainUsers) {
            int count = securityDomainUsers.get(name);
            if (count > 1) {
                securityDomainUsers.put(name, (count - 1));
                return;
            } else {
                securityDomainUsers.remove(name);
            }

            final ModelNode op = new ModelNode();
            op.get(OP).set(REMOVE);
            op.get(OP_ADDR).add(SUBSYSTEM, "security");
            op.get(OP_ADDR).add(SECURITY_DOMAIN, name);
            op.get(OPERATION_HEADERS, ROLLBACK_ON_RUNTIME_FAILURE).set(false);

            applyUpdate(op);
        }
    }

    public void addHttpsConnector(Map<String, String> sslOptions) throws Exception {
        httpsConnSemaphore.acquire();
        try {
            final ModelNode composite = Util.getEmptyOperation(COMPOSITE, new ModelNode());
            final ModelNode steps = composite.get(STEPS);
            ModelNode op = createOpNode("subsystem=web/connector=jbws-test-https-connector", ADD);
            op.get("socket-binding").set("https");
            op.get("scheme").set("https");
            op.get("protocol").set("HTTP/1.1");
            op.get("secure").set(true);
            op.get("enabled").set(true);
            steps.add(op);
            ModelNode ssl = createOpNode("subsystem=web/connector=jbws-test-https-connector/ssl=configuration", ADD);
            if (sslOptions != null) {
                for (final String k : sslOptions.keySet()) {
                    ssl.get(k).set(sslOptions.get(k));
                }
            }
            steps.add(ssl);
            applyUpdate(composite);
        } catch (Exception e) {
            httpsConnSemaphore.release();
            throw e;
        }
    }

    public void removeHttpsConnector() throws Exception {
        try {
            ModelNode op = createOpNode("subsystem=web/connector=jbws-test-https-connector", REMOVE);
            applyUpdate(op);
        } finally {
            httpsConnSemaphore.release();
        }
    }

    private static void applyUpdates(final List<ModelNode> updates) throws Exception {
        for (final ModelNode update : updates) {
            applyUpdate(update);
        }
    }

    private static ModelNode applyUpdate(final ModelNode update) throws Exception {
        final ModelControllerClient client = newModelControllerClient();
        try {
            final ModelNode result = client.execute(new OperationBuilder(update).build());
            checkResult(result);
            return result;
        } finally {
            client.close();
        }
    }

    private static void checkResult(final ModelNode result) throws Exception {
        if (result.hasDefined(OUTCOME) && SUCCESS.equals(result.get(OUTCOME).asString())) {
            if (result.hasDefined(RESULT)) {
                LOGGER.info(result.get(RESULT));
            }
        } else if (result.hasDefined(FAILURE_DESCRIPTION)) {
            throw new Exception(result.get(FAILURE_DESCRIPTION).asString());
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

    public static ModelNode createOpNode(String address, String operation) {
        ModelNode op = new ModelNode();
        // set address
        ModelNode list = op.get("address").setEmptyList();
        if (address != null) {
            String[] pathSegments = address.split("/");
            for (String segment : pathSegments) {
                String[] elements = segment.split("=");
                list.add(elements[0], elements[1]);
            }
        }
        op.get("operation").set(operation);
        return op;
    }

    private static String getSystemProperty(final String name, final String defaultValue) {
        PrivilegedAction<String> action = new PrivilegedAction<String>() {
            public String run() {
                return System.getProperty(name, defaultValue);
            }
        };
        return AccessController.doPrivileged(action);
    }

    private static ModelControllerClient newModelControllerClient() {
        return ModelControllerClient.Factory.create(address, port, callbackHandler);
    }

    private static ServerDeploymentManager newDeploymentManager() {
        return ServerDeploymentManager.Factory.create(address, port, callbackHandler);
    }
}

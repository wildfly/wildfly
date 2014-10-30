/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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
import java.util.Map.Entry;
import java.util.concurrent.Semaphore;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.sasl.RealmCallback;
import javax.security.sasl.RealmChoiceCallback;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.as.controller.client.helpers.standalone.DeploymentAction;
import org.jboss.as.controller.client.helpers.standalone.DeploymentPlan;
import org.jboss.as.controller.client.helpers.standalone.DeploymentPlanBuilder;
import org.jboss.as.controller.client.helpers.standalone.ServerDeploymentActionResult;
import org.jboss.as.controller.client.helpers.standalone.ServerDeploymentManager;
import org.jboss.as.controller.client.helpers.standalone.ServerDeploymentPlanResult;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.security.Constants;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.wsf.spi.deployer.Deployer;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ALLOW_RESOURCE_SERVICE_RESTART;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.AUTHENTICATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.COMPOSITE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RELEASE_VERSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUIRED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESTART;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ROLLBACK_ON_RUNTIME_FAILURE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_IDENTITY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SHUTDOWN;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SSL;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STEPS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TRUSTSTORE;
import static org.jboss.as.security.Constants.CLASSIC;
import static org.jboss.as.security.Constants.CODE;
import static org.jboss.as.security.Constants.FLAG;
import static org.jboss.as.security.Constants.AUTH_MODULE;
import static org.jboss.as.security.Constants.JASPI;
import static org.jboss.as.security.Constants.LOGIN_MODULE;
import static org.jboss.as.security.Constants.LOGIN_MODULE_STACK;
import static org.jboss.as.security.Constants.LOGIN_MODULE_STACK_REF;
import static org.jboss.as.security.Constants.MODULE_OPTIONS;
import static org.jboss.as.security.Constants.SECURITY_DOMAIN;

/**
 * Remote deployer that uses AS7 client deployment API.
 *
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 * @author <a href="mailto:alessio.soldano@jboss.com">Alessio Soldano</a>
 * @author <a href="mailto:ema@redhat.com">Jim Ma</a>
 */
public final class RemoteDeployer implements Deployer {

    private static final Logger LOGGER = Logger.getLogger(RemoteDeployer.class);
    private static final int DEFAULT_PORT = 9990;
    private static final String JBWS_DEPLOYER_HOST = "jbossws.deployer.host";
    private static final String JBWS_DEPLOYER_PORT = "jbossws.deployer.port";
    private static final String JBWS_DEPLOYER_PROTOCOL = "jbossws.deployer.protocol";
    private static final String JBWS_DEPLOYER_AUTH_USER = "jbossws.deployer.authentication.username";
    private static final String JBWS_DEPLOYER_AUTH_PWD = "jbossws.deployer.authentication.password";
    private static final String JBWS_DEPLOYER_HTTPS_LISTENER_NAME = "jbws-test-https-listener";
    private static final String JBWS_DEPLOYER_HTTPS_LISTENER_REALM_NAME = "jbws-test-https-realm";
    private static final CallbackHandler callbackHandler = getCallbackHandler();
    private static final int TIMEOUT = 60000;
    private static InetAddress address;
    private static Integer port;
    private static String protocol;
    private final Map<URL, String> url2Id = new HashMap<URL, String>();
    private final Map<String, Integer> securityDomainUsers = new HashMap<String, Integer>(1);
    private final Map<String, Integer> archiveCounters = new HashMap<String, Integer>();
    private final Semaphore httpsConnSemaphore = new Semaphore(1);

    private static final String SERVER_IDENTITY_SSL = SERVER_IDENTITY + "." + SSL + ".";
    private static final String AUTHENTICATION_TRUSTORE = AUTHENTICATION + "." + TRUSTSTORE + ".";

    static {
        try {
            final String host = System.getProperty(JBWS_DEPLOYER_HOST);
            address = host != null ? InetAddress.getByName(host) : InetAddress.getByName("localhost");
            port = Integer.getInteger(JBWS_DEPLOYER_PORT, DEFAULT_PORT);
            protocol = System.getProperty(JBWS_DEPLOYER_PROTOCOL, "http-remoting");
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
            final ModelControllerClient client = newModelControllerClient();
            final ServerDeploymentManager deploymentManager = newDeploymentManager(client);
            final DeploymentPlanBuilder builder = deploymentManager.newDeploymentPlan().add(archiveURL).andDeploy();
            final DeploymentPlan plan = builder.build();
            final DeploymentAction deployAction = builder.getLastAction();
            final String uniqueId = deployAction.getDeploymentUnitUniqueName();
            executeDeploymentPlan(plan, deployAction, client, deploymentManager);
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

            final String uniqueName = url2Id.get(archiveURL);
            if (uniqueName != null) {
                final ModelControllerClient client = newModelControllerClient();
                final ServerDeploymentManager deploymentManager = newDeploymentManager(client);
                final DeploymentPlanBuilder builder = deploymentManager.newDeploymentPlan();
                final DeploymentPlan plan = builder.undeploy(uniqueName).remove(uniqueName).build();
                final DeploymentAction deployAction = builder.getLastAction();
                try {
                    executeDeploymentPlan(plan, deployAction, client, deploymentManager);
                } finally {
                    url2Id.remove(archiveURL);
                }
            }
        }
    }

    private void executeDeploymentPlan(final DeploymentPlan plan, final DeploymentAction deployAction,
            final ModelControllerClient client, final ServerDeploymentManager deploymentManager) throws Exception {
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
            client.close();
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

            final ModelNode compositeOp = new ModelNode();
            compositeOp.get(OP).set(COMPOSITE);
            compositeOp.get(OP_ADDR).setEmptyList();
            compositeOp.get(OPERATION_HEADERS, ALLOW_RESOURCE_SERVICE_RESTART).set(true);

            ModelNode steps = compositeOp.get(STEPS);
            PathAddress address = PathAddress.pathAddress()
                    .append(SUBSYSTEM, "security")
                    .append(SECURITY_DOMAIN, name);
            ModelNode securityDomain = Util.createAddOperation(address);
            securityDomain.get(Constants.CACHE_TYPE).set("default");
            steps.add(securityDomain);
            address = address.append(Constants.AUTHENTICATION, CLASSIC);
            steps.add(Util.createAddOperation(address));
            ModelNode loginModule = Util.createAddOperation(address.append(LOGIN_MODULE, "UsersRoles"));
            loginModule.get(CODE).set("UsersRoles");
            loginModule.get(FLAG).set(REQUIRED);
            loginModule.get(OPERATION_HEADERS).get(ALLOW_RESOURCE_SERVICE_RESTART).set(true);
            final ModelNode moduleOptions = loginModule.get(MODULE_OPTIONS);
            if (authenticationOptions != null) {
                for (final String k : authenticationOptions.keySet()) {
                    moduleOptions.add(k, authenticationOptions.get(k));
                }
            }
            steps.add(loginModule);
            updates.add(compositeOp);

            applyUpdates(updates);
        }
    }

    public void addJaspiSecurityDomain(String name, String loginModuleStackName, Map<String, String> loginModuleOptions,
          String authModuleName, Map<String, String> authModuleOptions) throws Exception {
      synchronized (securityDomainUsers) {
          if (securityDomainUsers.containsKey(name)) {
              int count = securityDomainUsers.get(name);
              securityDomainUsers.put(name, (count + 1));
              return;
          } else {
              securityDomainUsers.put(name, 1);
          }

          final List<ModelNode> updates = new ArrayList<ModelNode>();

          final ModelNode compositeOp = new ModelNode();
          compositeOp.get(OP).set(COMPOSITE);
          compositeOp.get(OP_ADDR).setEmptyList();
          compositeOp.get(OPERATION_HEADERS, ALLOW_RESOURCE_SERVICE_RESTART).set(true);

          ModelNode steps = compositeOp.get(STEPS);
          PathAddress address = PathAddress.pathAddress().append(SUBSYSTEM, "security").append(SECURITY_DOMAIN, name);
          steps.add(Util.createAddOperation(address));

          PathAddress parentAddress = address.append(AUTHENTICATION, JASPI);
          steps.add(Util.createAddOperation(parentAddress));

          // step 3
          PathAddress loignStackAddress = parentAddress.append(LOGIN_MODULE_STACK, loginModuleStackName);
          ModelNode loginStack = Util.createAddOperation(loignStackAddress);
          steps.add(loginStack);

          // step 4
          ModelNode loginModule = Util.createAddOperation(loignStackAddress.append(LOGIN_MODULE, "UsersRoles"));
          loginModule.get(CODE).set("UsersRoles");
          loginModule.get(FLAG).set(REQUIRED);
          loginModule.get(OPERATION_HEADERS).get(ALLOW_RESOURCE_SERVICE_RESTART).set(true);
          final ModelNode moduleOptions = loginModule.get(MODULE_OPTIONS);
          if (loginModuleOptions != null) {
              for (final String k : loginModuleOptions.keySet()) {
                  moduleOptions.add(k, loginModuleOptions.get(k));
              }
          }
          steps.add(loginModule);

          PathAddress authModule = parentAddress.append(AUTH_MODULE, authModuleName);
          ModelNode authModuleNode = Util.createAddOperation(authModule);
          authModuleNode.get(LOGIN_MODULE_STACK_REF).set(loginModuleStackName);
          authModuleNode.get(CODE).set(authModuleName);
          ModelNode options = authModuleNode.get(MODULE_OPTIONS);
          if (authModuleOptions != null) {
              for (final String k : authModuleOptions.keySet()) {
                  options.add(k, authModuleOptions.get(k));
              }
          }
          steps.add(authModuleNode);

          updates.add(compositeOp);

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
            op.get(OPERATION_HEADERS, ALLOW_RESOURCE_SERVICE_RESTART).set(true);
            op.get(OPERATION_HEADERS, ROLLBACK_ON_RUNTIME_FAILURE).set(false);

            applyUpdate(op);
        }
    }

    public void addHttpsConnector(Map<String, String> options) throws Exception {
        final Map<String, String> sslOptionsMap = new HashMap<String, String>();
        final Map<String, String> truststoreOptionsMap = new HashMap<String, String>();
        if (options != null) {
            for (final Entry<String, String> entry : options.entrySet()) {
                final String k = entry.getKey();
                if (k.startsWith(SERVER_IDENTITY_SSL)) {
                    final String key = k.substring(SERVER_IDENTITY_SSL.length());
                    sslOptionsMap.put(key, entry.getValue());
                } else if (k.startsWith(AUTHENTICATION_TRUSTORE)) {
                    final String key = k.substring(AUTHENTICATION_TRUSTORE.length());
                    truststoreOptionsMap.put(key, entry.getValue());
                }
            }
        }
        httpsConnSemaphore.acquire();
        try {
            addSecurityRealm(JBWS_DEPLOYER_HTTPS_LISTENER_REALM_NAME, sslOptionsMap, truststoreOptionsMap);
            final ModelNode composite = Util.getEmptyOperation(COMPOSITE, new ModelNode());
            final ModelNode steps = composite.get(STEPS);
            ModelNode op = createOpNode("subsystem=undertow/server=default-server/https-listener=" + JBWS_DEPLOYER_HTTPS_LISTENER_NAME, "add");
            op.get("socket-binding").set("https");
            op.get("security-realm").set(JBWS_DEPLOYER_HTTPS_LISTENER_REALM_NAME);
            final String verifyClient = "verify-client";
            if (options != null && options.containsKey(verifyClient)) {
                op.get(verifyClient).set(options.get(verifyClient));
            }
            steps.add(op);
            applyUpdate(composite);
        } catch (Exception e) {
            httpsConnSemaphore.release();
            throw e;
        }
    }

    private static void addSecurityRealm(String realm, Map<String, String> sslOptions, Map<String, String> truststoreOptions) throws Exception {
        final ModelNode composite = Util.getEmptyOperation(COMPOSITE, new ModelNode());
        final ModelNode steps = composite.get(STEPS);
        ModelNode op = createOpNode("core-service=management/security-realm=" + realm, ADD);
        steps.add(op);
        if (!sslOptions.isEmpty()) {
            ModelNode ssl = createOpNode("core-service=management/security-realm=" + realm + "/server-identity=ssl", ADD);
            for (final Entry<String, String> entry : sslOptions.entrySet()) {
                ssl.get(entry.getKey()).set(entry.getValue());
            }
            steps.add(ssl);
        }

        if (!truststoreOptions.isEmpty()) {
            ModelNode truststore = createOpNode("core-service=management/security-realm=" + realm + "/authentication=truststore", ADD);
            for (final Entry<String, String> entry : truststoreOptions.entrySet()) {
                truststore.get(entry.getKey()).set(entry.getValue());
            }
            steps.add(truststore);
        }
        applyUpdate(composite);
    }

    public void removeHttpsConnector() throws Exception {
        try {
            ModelNode op = createOpNode("subsystem=undertow/server=default-server/https-listener=" + JBWS_DEPLOYER_HTTPS_LISTENER_NAME, REMOVE);
            op.get(OPERATION_HEADERS, ALLOW_RESOURCE_SERVICE_RESTART).set(true);
            applyUpdate(op);
            op = createOpNode("core-service=management/security-realm=" + JBWS_DEPLOYER_HTTPS_LISTENER_REALM_NAME, REMOVE);
            op.get(OPERATION_HEADERS, ALLOW_RESOURCE_SERVICE_RESTART).set(true);
            applyUpdate(op);
        } finally {
            httpsConnSemaphore.release();
        }
    }

    public void restart() throws Exception {
        ModelNode op = new ModelNode();
        op.get("operation").set(SHUTDOWN);
        op.get(RESTART).set("true");
        applyUpdate(op);
        waitForServerToRestart(TIMEOUT);
    }

    public String setSystemProperty(String propName, String propValue) throws Exception {
        if (propName == null || propName.trim().length() == 0) {
            throw new IllegalArgumentException("Empty system property name specified!");
        }
        ModelNode op = createOpNode("system-property=" + propName, READ_RESOURCE_OPERATION);
        String previousValue = null;
        try {
            ModelNode response = applyUpdate(op);
            String rawResult = response.get(RESULT).asString();
            previousValue = rawResult.substring(13, rawResult.length() - 2); //{"value" => "xyz"}
        } catch (Exception e) {
            if (!e.getMessage().contains("WFLYCTL0216")) {
                throw e;
            }
        }
        if (previousValue != null) {
            op = createOpNode("system-property=" + propName, REMOVE);
            applyUpdate(op);
        }
        if (propValue != null) {
            op = createOpNode("system-property=" + propName, ADD);
            op.get("value").set(propValue);
            applyUpdate(op);
        }
        return previousValue;
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

    private void waitForServerToRestart(int timeout) throws Exception {
        Thread.sleep(1000);
        long start = System.currentTimeMillis();
        long now;
        do {
            final ModelControllerClient client = newModelControllerClient();
            ModelNode operation = new ModelNode();
            operation.get(OP_ADDR).setEmptyList();
            operation.get(OP).set(READ_ATTRIBUTE_OPERATION);
            operation.get(NAME).set("server-state");
            try {
                ModelNode result = client.execute(operation);
                boolean normal = "running".equals(result.get(RESULT).asString());
                if (normal) {
                    return;
                }
            } catch (Exception e) {
                //NOOP
            } finally {
                client.close();
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
            }
            now = System.currentTimeMillis();
        } while (now - start < timeout);
        throw new Exception("Server did not restart!");
    }

    private static String getSystemProperty(final String name, final String defaultValue) {
        PrivilegedAction<String> action = new PrivilegedAction<String>() {
            public String run() {
                return System.getProperty(name, defaultValue);
            }
        };
        return AccessController.doPrivileged(action);
    }

    private static ModelControllerClient newModelControllerClient() throws Exception {
        return ModelControllerClient.Factory.create(protocol, address.getHostAddress(), port, callbackHandler, null, TIMEOUT);
    }

    private static ServerDeploymentManager newDeploymentManager(ModelControllerClient client) throws Exception {
        return ServerDeploymentManager.Factory.create(client);
    }
}

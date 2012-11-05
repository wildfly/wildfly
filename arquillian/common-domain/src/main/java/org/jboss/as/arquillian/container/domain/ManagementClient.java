/*
 * JBoss, Home of Professional Open Source
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.arquillian.container.domain;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.AUTO_START;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CHILD_TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEPLOYMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INCLUDE_RUNTIME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_CHILDREN_NAMES_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RECURSIVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_CONFIG;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING_GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STATUS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import javax.security.auth.callback.CallbackHandler;

import org.jboss.arquillian.container.spi.client.protocol.metadata.HTTPContext;
import org.jboss.arquillian.container.spi.client.protocol.metadata.Servlet;
import org.jboss.as.arquillian.container.domain.Domain.Server;
import org.jboss.as.arquillian.container.domain.Domain.ServerGroup;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.dmr.ModelNode;

/**
 * A helper class to join management related operations, like extract sub system ip/port (web/jmx) and deployment introspection.
 *
 * @author <a href="aslak@redhat.com">Aslak Knutsen</a>
 */
public class ManagementClient {

    private static final String SUBDEPLOYMENT = "subdeployment";
    private static final String WEB = "web";

    private static final String JMX = "jmx";

    private static final String PROTOCOL_HTTP = "http";

    private static final String NAME = "name";
    private static final String SERVLET = "servlet";

    private static final String POSTFIX_WEB = ".war";
    private static final String POSTFIX_EAR = ".ear";

    private final String mgmtAddress;
    private final int mgmtPort;
    private final ModelControllerClient client;
    private final Map<String, URI> subsystemURICache;

    // cache static RootNode
    private ModelNode rootNode = null;

    private MBeanServerConnection connection;
    private JMXConnector connector;

    public ManagementClient(ModelControllerClient client, final String mgmtAddress, final int managementPort) {
        if (client == null) {
            throw new IllegalArgumentException("Client must be specified");
        }
        this.client = client;
        this.mgmtAddress = mgmtAddress;
        this.subsystemURICache = new HashMap<String, URI>();
        this.mgmtPort = managementPort;
    }

    // -------------------------------------------------------------------------------------||
    // Public API -------------------------------------------------------------------------||
    // -------------------------------------------------------------------------------------||

    public ModelControllerClient getControllerClient() {
        return client;
    }

    public Domain createDomain(Map<String, String> containerNameMap) {
        lazyLoadRootNode();

        Domain domain = new Domain();
        for (String hostNodeName : rootNode.get(HOST).keys()) {

            for (String serverConfigName : rootNode.get(HOST).get(hostNodeName).get(SERVER_CONFIG).keys()) {
                ModelNode serverConfig = rootNode.get(HOST).get(hostNodeName).get(SERVER_CONFIG).get(serverConfigName);

                Server server = new Server(
                        serverConfig.get(NAME).asString(),
                        hostNodeName,
                        serverConfig.get(GROUP).asString(),
                        serverConfig.get(AUTO_START).asBoolean());

                if(containerNameMap.containsKey(server.getUniqueName())) {
                    server.setContainerName(containerNameMap.get(server.getUniqueName()));
                }
                domain.addServer(server);
            }
        }
        for (String serverGroupName : rootNode.get(SERVER_GROUP).keys()) {

            ServerGroup group = new ServerGroup(serverGroupName);
            if(containerNameMap.containsKey(group.getName())) {
                group.setContainerName(containerNameMap.get(group.getName()));
            }
            domain.addServerGroup(group);
        }
        return domain;
    }

    public String getServerState(Domain.Server server) {
        lazyLoadRootNode();

        ModelNode hostNode = rootNode.get(HOST).get(server.getHost());
        if (!hostNode.isDefined()) {
            throw new IllegalArgumentException("Host not found on domain " + server.getHost());
        }

        ModelNode serverConfig = hostNode.get(SERVER_CONFIG).get(server.getName());
        if (!serverConfig.isDefined()) {
            throw new IllegalArgumentException("Server " + server + " not found on host " + server.getHost());
        }
        return serverConfig.get(STATUS).asString();
    }

    public HTTPContext getHTTPDeploymentMetaData(Server server, String uniqueDeploymentName) {

        URI webURI = getProtocolURI(server, PROTOCOL_HTTP);
        HTTPContext context = new HTTPContext(webURI.getHost(), webURI.getPort());
        try {
            ModelNode deploymentNode = readResource(createHostServerDeploymentAddress(
                    server.getHost(), server.getName(), uniqueDeploymentName), false); // don't include runtime information, workaround for bug in web statistics

            if (isWebArchive(uniqueDeploymentName)) {
                extractWebArchiveContexts(context, deploymentNode);
            } else if (isEnterpriseArchive(uniqueDeploymentName)) {
                extractEnterpriseArchiveContexts(context, deploymentNode);
            }

        } catch (Exception e) {
            throw new RuntimeException("Could not extract deployment information for server: " + server + " on deployment: "
                    + uniqueDeploymentName, e);
        }
        return context;
    }

    public void startServerGroup(String groupName) {
        try {
            executeOperation("start-servers", new ModelNode().add(SERVER_GROUP, groupName));
        } catch (Exception e) {
            throw new RuntimeException("Could not start server-group " + groupName, e);
        }
    }

    public void stopServerGroup(String groupName) {
        try {
            executeOperation("stop-servers", new ModelNode().add(SERVER_GROUP, groupName));
        } catch (Exception e) {
            throw new RuntimeException("Could not stop server-group " + groupName, e);
        }
    }

    public void startServer(Server server) {
        try {
            executeOperation("start", new ModelNode().add(HOST, server.getHost()).add(SERVER_CONFIG, server.getName()));
        } catch (Exception e) {
            throw new RuntimeException("Could not start server " + server, e);
        }
    }

    public void stopServer(Server server) {
        try {
            executeOperation("stop", new ModelNode().add(HOST, server.getHost()).add(SERVER_CONFIG, server.getName()));
        } catch (Exception e) {
            throw new RuntimeException("Could not stop server " + server, e);
        }
    }

    public boolean isServerStarted(Server server) {
        try {

            ModelNode result = readAttribute("status", createHostServerConfigAddress(server.getHost(), server.getName()));
            if(result.asString().equalsIgnoreCase("STARTED")) {
                result = readAttribute("server-state", createHostServerAddress(server.getHost(), server.getName()));
                if(result.asString().equalsIgnoreCase("running")) {
                    return true;
                }
            }
            return false;

        } catch (Exception ignored) { }
        return false;
    }

    public boolean isDomainInRunningState() {
        try {
            // some random values to read in hopes the domain controller is up and running..
            ModelNode op = Util.getEmptyOperation(READ_CHILDREN_NAMES_OPERATION, PathAddress.EMPTY_ADDRESS.toModelNode());
            op.get(CHILD_TYPE).set("server-group");
            ModelNode rsp = client.execute(op);

            return SUCCESS.equals(rsp.get(OUTCOME).asString()) && rsp.get("result").asList().size() > 0;
        } catch (Exception ignored) {
            return false;
        }
    }

    public void close() {
        try {
            getControllerClient().close();
        } catch (IOException e) {
            throw new RuntimeException("Could not close connection", e);
        } finally {
            if (connector != null) {
                try {
                    connector.close();
                } catch (IOException e) {
                    throw new RuntimeException("Could not close JMX connection", e);
                }
            }
        }
    }

    // -------------------------------------------------------------------------------------||
    // Subsystem URI Lookup ---------------------------------------------------------------||
    // -------------------------------------------------------------------------------------||

    private URI getProtocolURI(Server server, String subsystem) {
        String cacheKey = server + subsystem;
        URI subsystemURI = subsystemURICache.get(cacheKey);
        if (subsystemURI != null) {
            return subsystemURI;
        }
        subsystemURI = extractProtocolURI(server, subsystem);
        subsystemURICache.put(cacheKey, subsystemURI);
        return subsystemURI;
    }

    private URI extractProtocolURI(Server server, String protocol) {
        try {
            ModelNode node = readResource(createHostServerSocketBindingsAddress(server.getHost(), server.getName(),
                    getSocketBindingGroup(server.getGroup())));

            ModelNode socketBinding = node.get(SOCKET_BINDING).get(protocol);
            return URI.create(protocol + "://" + socketBinding.get("bound-address").asString() + ":"
                    + socketBinding.get("bound-port"));

        } catch (Exception e) {
            throw new RuntimeException("Could not extract address information from server: " + server + " for protocol "
                    + protocol + ". Is the server running?", e);
        }
    }

    private void readRootNode() throws Exception {
        rootNode = readResource(new ModelNode());
    }

    private String getSocketBindingGroup(String serverGroup) {
        lazyLoadRootNode();
        return rootNode.get(SERVER_GROUP).get(serverGroup).get(SOCKET_BINDING_GROUP).asString();
    }

    // -------------------------------------------------------------------------------------||
    // Metadata Extraction Operations -----------------------------------------------------||
    // -------------------------------------------------------------------------------------||

    private boolean isEnterpriseArchive(String deploymentName) {
        return deploymentName.endsWith(POSTFIX_EAR);
    }

    private boolean isWebArchive(String deploymentName) {
        return deploymentName.endsWith(POSTFIX_WEB);
    }

    private ModelNode createHostServerAddress(String host, String server) {
        return new ModelNode().add(HOST, host).add(SERVER, server);
    }

    private ModelNode createHostServerConfigAddress(String host, String server) {
        return new ModelNode().add(HOST, host).add(SERVER_CONFIG, server);
    }

    private ModelNode createHostServerDeploymentAddress(String host, String server, String deploymentName) {
        return new ModelNode().add(HOST, host).add(SERVER, server).add(DEPLOYMENT, deploymentName);
    }

    private ModelNode createHostServerSocketBindingsAddress(String host, String server, String socketBindingGroup) {
        return new ModelNode().add(HOST, host).add(SERVER, server).add(SOCKET_BINDING_GROUP, socketBindingGroup);
    }

    private void extractEnterpriseArchiveContexts(HTTPContext context, ModelNode deploymentNode) {
        if (deploymentNode.hasDefined(SUBDEPLOYMENT)) {
            for (ModelNode subdeployment : deploymentNode.get(SUBDEPLOYMENT).asList()) {
                String deploymentName = subdeployment.keys().iterator().next();
                if (isWebArchive(deploymentName)) {
                    extractWebArchiveContexts(context, deploymentName, subdeployment.get(deploymentName));
                }
            }
        }
    }

    private void extractWebArchiveContexts(HTTPContext context, ModelNode deploymentNode) {
        extractWebArchiveContexts(context, deploymentNode.get(NAME).asString(), deploymentNode);
    }

    private void extractWebArchiveContexts(HTTPContext context, String deploymentName, ModelNode deploymentNode) {
        if (deploymentNode.hasDefined(SUBSYSTEM)) {
            ModelNode subsystem = deploymentNode.get(SUBSYSTEM);
            if (subsystem.hasDefined(WEB)) {
                ModelNode webSubSystem = subsystem.get(WEB);
                if (webSubSystem.isDefined() && webSubSystem.hasDefined("context-root")) {
                    final String contextName = webSubSystem.get("context-root").asString();
                    if (webSubSystem.hasDefined(SERVLET)) {
                        for (final ModelNode servletNode : webSubSystem.get(SERVLET).asList()) {
                            for (final String servletName : servletNode.keys()) {
                                context.add(new Servlet(servletName, toContextName(contextName)));
                            }
                        }
                    }
                    /*
                     * This is a WebApp, it has some form of webcontext whether it has a Servlet or not. AS7 does not expose jsp
                     * / default servlet in mgm api
                     */
                    context.add(new Servlet("default", toContextName(contextName)));
                }
            }
        }
    }

    private String toContextName(String deploymentName) {
        String correctedName = deploymentName;
        if (correctedName.startsWith("/")) {
            correctedName = correctedName.substring(1);
        }
        if (correctedName.indexOf(".") != -1) {
            correctedName = correctedName.substring(0, correctedName.lastIndexOf("."));
        }
        return correctedName;
    }

    // -------------------------------------------------------------------------------------||
    // Common Management API Operations ---------------------------------------------------||
    // -------------------------------------------------------------------------------------||

    private void lazyLoadRootNode() {
        try {
            if (rootNode == null) {
                readRootNode();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private ModelNode readResource(ModelNode address) throws Exception {
        return readResource(address, true);
    }

    private ModelNode readResource(ModelNode address, boolean includeRuntime) throws Exception {
        final ModelNode operation = new ModelNode();
        operation.get(OP).set(READ_RESOURCE_OPERATION);
        operation.get(RECURSIVE).set("true");
        operation.get(INCLUDE_RUNTIME).set(includeRuntime);
        operation.get(OP_ADDR).set(address);

        return executeForResult(operation);
    }

    private ModelNode readAttribute(String attribute, ModelNode address) throws Exception {
        final ModelNode operation = new ModelNode();
        operation.get(OP).set(READ_ATTRIBUTE_OPERATION);
        operation.get(NAME).set(attribute);
        operation.get(OP_ADDR).set(address);

        return executeForResult(operation);
    }

    private ModelNode executeOperation(String operation, ModelNode address) throws Exception {
        final ModelNode request = new ModelNode();
        request.get(OP).set(operation);
        request.get(OP_ADDR).set(address);

        return executeForResult(request);
    }

    private ModelNode executeForResult(final ModelNode operation) throws Exception {
        final ModelNode result = client.execute(operation);
        checkSuccessful(result, operation);
        return result.get(RESULT);
    }

    private void checkSuccessful(final ModelNode result, final ModelNode operation) throws UnSuccessfulOperationException {
        if (!SUCCESS.equals(result.get(OUTCOME).asString())) {
            System.out.println(result);
            throw new UnSuccessfulOperationException(result.get(FAILURE_DESCRIPTION).toString());
        }
    }

    private static class UnSuccessfulOperationException extends Exception {
        private static final long serialVersionUID = 1L;

        public UnSuccessfulOperationException(String message) {
            super(message);
        }
    }

    private MBeanServerConnection getConnection() {
        if (connection == null) {
            try {
                final HashMap<String, Object> env = new HashMap<String, Object>();
                env.put(CallbackHandler.class.getName(), Authentication.getCallbackHandler());
                connection = JMXConnectorFactory.connect(getRemoteJMXURL(), env).getMBeanServerConnection();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return connection;
    }

    private JMXServiceURL getRemoteJMXURL() {
        try {
            return new JMXServiceURL("service:jmx:remoting-jmx://" + NetworkUtils.formatPossibleIpv6Address(mgmtAddress) + ":"
                    + mgmtPort);
        } catch (Exception e) {
            throw new RuntimeException("Could not create JMXServiceURL:" + this, e);
        }
    }

}

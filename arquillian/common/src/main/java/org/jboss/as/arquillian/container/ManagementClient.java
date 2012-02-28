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
package org.jboss.as.arquillian.container;

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
import org.jboss.arquillian.container.spi.client.protocol.metadata.JMXContext;
import org.jboss.arquillian.container.spi.client.protocol.metadata.ProtocolMetaData;
import org.jboss.arquillian.container.spi.client.protocol.metadata.Servlet;
import org.jboss.as.controller.ControlledProcessState;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.dmr.ModelNode;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEPLOYMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RECURSIVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;

/**
 * A helper class to join management related operations, like extract sub system ip/port (web/jmx)
 * and deployment introspection.
 *
 * @author <a href="aslak@redhat.com">Aslak Knutsen</a>
 */
public class ManagementClient {

    private static final String SUBDEPLOYMENT = "subdeployment";
    private static final String WEB = "web";

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

    //-------------------------------------------------------------------------------------||
    // Public API -------------------------------------------------------------------------||
    //-------------------------------------------------------------------------------------||

    public ModelControllerClient getControllerClient() {
        return client;
    }

    public URI getSubSystemURI(String subsystem) {
        URI subsystemURI = subsystemURICache.get(subsystem);
        if (subsystemURI != null) {
            return subsystemURI;
        }
        subsystemURI = extractSubSystemURI(subsystem);
        subsystemURICache.put(subsystem, subsystemURI);
        return subsystemURI;
    }

    public ProtocolMetaData getDeploymentMetaData(String deploymentName) {
        URI webURI = getSubSystemURI(WEB);

        ProtocolMetaData metaData = new ProtocolMetaData();
        metaData.addContext(new JMXContext(getConnection()));
        HTTPContext context = new HTTPContext(webURI.getHost(), webURI.getPort());
        metaData.addContext(context);
        try {
            ModelNode deploymentNode = readResource(createDeploymentAddress(deploymentName));

            if (isWebArchive(deploymentName)) {
                extractWebArchiveContexts(context, deploymentNode);
            } else if (isEnterpriseArchive(deploymentName)) {
                extractEnterpriseArchiveContexts(context, deploymentNode);
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return metaData;
    }

    public boolean isServerInRunningState() {
        try {
            ModelNode op = Util.getEmptyOperation(READ_ATTRIBUTE_OPERATION, PathAddress.EMPTY_ADDRESS.toModelNode());
            op.get(NAME).set("server-state");

            ModelNode rsp = client.execute(op);
            return SUCCESS.equals(rsp.get(OUTCOME).asString())
                    && !ControlledProcessState.State.STARTING.toString().equals(rsp.get(RESULT).asString())
                    && !ControlledProcessState.State.STOPPING.toString().equals(rsp.get(RESULT).asString());
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

    //-------------------------------------------------------------------------------------||
    // Subsystem URI Lookup ---------------------------------------------------------------||
    //-------------------------------------------------------------------------------------||

    private URI extractSubSystemURI(String subsystem) {
        try {
            if (rootNode == null) {
                readRootNode();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        if ("web".equals(subsystem)) {
            String socketBinding = rootNode.get("subsystem").get("web").get("connector").get("http").get("socket-binding").asString();
            return getBinding("http", socketBinding);
        }
        throw new IllegalArgumentException("No handler for subsystem " + subsystem);
    }

    private void readRootNode() throws Exception {
        rootNode = readResource(new ModelNode());
    }

    private static ModelNode defined(final ModelNode node, final String message) {
        if (!node.isDefined())
            throw new IllegalStateException(message);
        return node;
    }

    private URI getBinding(final String protocol, final String socketBinding) {
        try {
            //TODO: resolve socket binding group correctly
            final String socketBindingGroupName = rootNode.get("socket-binding-group").keys().iterator().next();

            final ModelNode operation = new ModelNode();
            operation.get(OP_ADDR).get("socket-binding-group").set(socketBindingGroupName);
            operation.get(OP_ADDR).get("socket-binding").set(socketBinding);
            operation.get(OP).set(READ_ATTRIBUTE_OPERATION);
            operation.get(NAME).set("bound-address");
            final String ip = executeForResult(operation).asString();

            final ModelNode portOp = new ModelNode();
            portOp.get(OP_ADDR).get("socket-binding-group").set(socketBindingGroupName);
            portOp.get(OP_ADDR).get("socket-binding").set(socketBinding);
            portOp.get(OP).set(READ_ATTRIBUTE_OPERATION);
            portOp.get(NAME).set("bound-port");
            final int port = defined(executeForResult(portOp), socketBindingGroupName + " -> " + socketBinding + " -> bound-port is undefined").asInt();

            return URI.create(protocol + "://" + NetworkUtils.formatPossibleIpv6Address(ip) + ":" + port);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    //-------------------------------------------------------------------------------------||
    // Metadata Extraction Operations -----------------------------------------------------||
    //-------------------------------------------------------------------------------------||

    private boolean isEnterpriseArchive(String deploymentName) {
        return deploymentName.endsWith(POSTFIX_EAR);
    }

    private boolean isWebArchive(String deploymentName) {
        return deploymentName.endsWith(POSTFIX_WEB);
    }

    private ModelNode createDeploymentAddress(String deploymentName) {
        ModelNode address = new ModelNode();
        address.add(DEPLOYMENT, deploymentName);
        return address;
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
                     * This is a WebApp, it has some form of webcontext whether it has a
                     * Servlet or not. AS7 does not expose jsp / default servlet in mgm api
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
            correctedName = correctedName.substring(0,
                    correctedName.lastIndexOf("."));
        }
        return correctedName;
    }

    //-------------------------------------------------------------------------------------||
    // Common Management API Operations ---------------------------------------------------||
    //-------------------------------------------------------------------------------------||

    private ModelNode readResource(ModelNode address) throws Exception {
        final ModelNode operation = new ModelNode();
        operation.get(OP).set(READ_RESOURCE_OPERATION);
        operation.get(RECURSIVE).set("true");
        operation.get(OP_ADDR).set(address);

        return executeForResult(operation);
    }

    private ModelNode executeForResult(final ModelNode operation) throws Exception {
        final ModelNode result = client.execute(operation);
        checkSuccessful(result, operation);
        return result.get(RESULT);
    }

    private void checkSuccessful(final ModelNode result,
                                 final ModelNode operation) throws UnSuccessfulOperationException {
        if (!SUCCESS.equals(result.get(OUTCOME).asString())) {
            throw new UnSuccessfulOperationException(result.get(
                    FAILURE_DESCRIPTION).toString());
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

    public JMXServiceURL getRemoteJMXURL() {
        try {
            return new JMXServiceURL("service:jmx:remoting-jmx://" + NetworkUtils.formatPossibleIpv6Address(mgmtAddress) + ":" + mgmtPort);
        } catch (Exception e) {
            throw new RuntimeException("Could not create JMXServiceURL:" + this, e);
        }
    }

    public int getMgmtPort() {
        return mgmtPort;
    }

    public String getMgmtAddress() {
        return NetworkUtils.formatPossibleIpv6Address(mgmtAddress);
    }

    public URI getRemoteEjbURL() {
        String socketBinding = rootNode.get("subsystem").get("remoting").get("connector").get("remoting-connector").get("socket-binding").asString();
        return getBinding("remote", socketBinding);

    }
}

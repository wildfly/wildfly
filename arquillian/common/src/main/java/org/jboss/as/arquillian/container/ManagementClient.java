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
import java.util.Set;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.InvalidAttributeValueException;
import javax.management.ListenerNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServerConnection;
import javax.management.NotCompliantMBeanException;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.QueryExp;
import javax.management.ReflectionException;
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

    private URI webUri;
    private URI ejbUri;

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
        this.mgmtPort = managementPort;
    }

    //-------------------------------------------------------------------------------------||
    // Public API -------------------------------------------------------------------------||
    //-------------------------------------------------------------------------------------||

    public ModelControllerClient getControllerClient() {
        return client;
    }

    /**
     * @return The base URI or the web susbsystem. Usually http://localhost:8080
     */
    public URI getWebUri() {
        if (webUri == null) {
            try {
                if (rootNode == null) {
                    readRootNode();
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            String socketBinding = rootNode.get("subsystem").get("web").get("connector").get("http").get("socket-binding").asString();
            webUri = getBinding("http", socketBinding);
        }
        return webUri;
    }

    public ProtocolMetaData getDeploymentMetaData(String deploymentName) {
        URI webURI = getWebUri();

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
        try {                    ModelNode op = Util.getEmptyOperation(READ_ATTRIBUTE_OPERATION, PathAddress.EMPTY_ADDRESS.toModelNode());
            op.get(NAME).set("server-state");

            ModelNode rsp = client.execute(op);
            return SUCCESS.equals(rsp.get(OUTCOME).asString())
                    && !ControlledProcessState.State.STARTING.toString().equals(rsp.get(RESULT).asString())
                    && !ControlledProcessState.State.STOPPING.toString().equals(rsp.get(RESULT).asString());
        } catch (Throwable ignored) {
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
            String ip = executeForResult(operation).asString();
            //it appears some system can return a binding with the zone specifier on the end
            if(ip.contains(":") && ip.contains("%")) {
                ip = ip.split("%")[0];
            }

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

    private MBeanServerConnection getConnection() {
        if (connection == null) {
            try {
                final HashMap<String, Object> env = new HashMap<String, Object>();
                env.put(CallbackHandler.class.getName(), Authentication.getCallbackHandler());
                connection = new MBeanConnectionProxy(JMXConnectorFactory.connect(getRemoteJMXURL(), env).getMBeanServerConnection());
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
        if (ejbUri == null) {
            if (rootNode == null) {
                try {
                    readRootNode();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            String socketBinding = rootNode.get("subsystem").get("remoting").get("connector").get("remoting-connector").get("socket-binding").asString();
            ejbUri = getBinding("remote", socketBinding);
        }
        return ejbUri;
    }
    //-------------------------------------------------------------------------------------||
    // Helper classes ---------------------------------------------------------------------||
    //-------------------------------------------------------------------------------------||
    private static class UnSuccessfulOperationException extends Exception {
        private static final long serialVersionUID = 1L;

        public UnSuccessfulOperationException(String message) {
            super(message);
        }
    }

    private class MBeanConnectionProxy implements MBeanServerConnection{
        private MBeanServerConnection connection;

        /**
         * @param connection
         */
        public MBeanConnectionProxy(MBeanServerConnection connection) {
            super();
            this.connection = connection;
        }

        /**
         * @param className
         * @param name
         * @return
         * @throws ReflectionException
         * @throws InstanceAlreadyExistsException
         * @throws MBeanRegistrationException
         * @throws MBeanException
         * @throws NotCompliantMBeanException
         * @throws IOException
         * @see javax.management.MBeanServerConnection#createMBean(java.lang.String, javax.management.ObjectName)
         */
        public ObjectInstance createMBean(String className, ObjectName name) throws ReflectionException,
                InstanceAlreadyExistsException, MBeanException, NotCompliantMBeanException,
                IOException {
            checkConnection();
            return connection.createMBean(className, name);
        }

        /**
         * @param className
         * @param name
         * @param loaderName
         * @return
         * @throws ReflectionException
         * @throws InstanceAlreadyExistsException
         * @throws MBeanRegistrationException
         * @throws MBeanException
         * @throws NotCompliantMBeanException
         * @throws InstanceNotFoundException
         * @throws IOException
         * @see javax.management.MBeanServerConnection#createMBean(java.lang.String, javax.management.ObjectName, javax.management.ObjectName)
         */
        public ObjectInstance createMBean(String className, ObjectName name, ObjectName loaderName) throws ReflectionException,
                InstanceAlreadyExistsException, MBeanException, NotCompliantMBeanException,
                InstanceNotFoundException, IOException {
            checkConnection();
            return connection.createMBean(className, name, loaderName);
        }

        /**
         * @param className
         * @param name
         * @param params
         * @param signature
         * @return
         * @throws ReflectionException
         * @throws InstanceAlreadyExistsException
         * @throws MBeanRegistrationException
         * @throws MBeanException
         * @throws NotCompliantMBeanException
         * @throws IOException
         * @see javax.management.MBeanServerConnection#createMBean(java.lang.String, javax.management.ObjectName, java.lang.Object[], java.lang.String[])
         */
        public ObjectInstance createMBean(String className, ObjectName name, Object[] params, String[] signature)
                throws ReflectionException, InstanceAlreadyExistsException, MBeanException,
                NotCompliantMBeanException, IOException {
            checkConnection();
            return connection.createMBean(className, name, params, signature);
        }

        /**
         * @param className
         * @param name
         * @param loaderName
         * @param params
         * @param signature
         * @return
         * @throws ReflectionException
         * @throws InstanceAlreadyExistsException
         * @throws MBeanRegistrationException
         * @throws MBeanException
         * @throws NotCompliantMBeanException
         * @throws InstanceNotFoundException
         * @throws IOException
         * @see javax.management.MBeanServerConnection#createMBean(java.lang.String, javax.management.ObjectName, javax.management.ObjectName, java.lang.Object[], java.lang.String[])
         */
        public ObjectInstance createMBean(String className, ObjectName name, ObjectName loaderName, Object[] params,
                String[] signature) throws ReflectionException, InstanceAlreadyExistsException,
                MBeanException, NotCompliantMBeanException, InstanceNotFoundException, IOException {
            checkConnection();
            return connection.createMBean(className, name, loaderName, params, signature);
        }

        /**
         * @param name
         * @throws InstanceNotFoundException
         * @throws MBeanRegistrationException
         * @throws IOException
         * @see javax.management.MBeanServerConnection#unregisterMBean(javax.management.ObjectName)
         */
        public void unregisterMBean(ObjectName name) throws InstanceNotFoundException, MBeanRegistrationException, IOException {
            checkConnection();
            connection.unregisterMBean(name);
        }

        /**
         * @param name
         * @return
         * @throws InstanceNotFoundException
         * @throws IOException
         * @see javax.management.MBeanServerConnection#getObjectInstance(javax.management.ObjectName)
         */
        public ObjectInstance getObjectInstance(ObjectName name) throws InstanceNotFoundException, IOException {
            checkConnection();
            return connection.getObjectInstance(name);
        }

        /**
         * @param name
         * @param query
         * @return
         * @throws IOException
         * @see javax.management.MBeanServerConnection#queryMBeans(javax.management.ObjectName, javax.management.QueryExp)
         */
        public Set<ObjectInstance> queryMBeans(ObjectName name, QueryExp query) throws IOException {
            checkConnection();
            return connection.queryMBeans(name, query);
        }

        /**
         * @param name
         * @param query
         * @return
         * @throws IOException
         * @see javax.management.MBeanServerConnection#queryNames(javax.management.ObjectName, javax.management.QueryExp)
         */
        public Set<ObjectName> queryNames(ObjectName name, QueryExp query) throws IOException {
            checkConnection();
            return connection.queryNames(name, query);
        }

        /**
         * @param name
         * @return
         * @throws IOException
         * @see javax.management.MBeanServerConnection#isRegistered(javax.management.ObjectName)
         */
        public boolean isRegistered(ObjectName name) throws IOException {
            checkConnection();
            return connection.isRegistered(name);
        }

        /**
         * @return
         * @throws IOException
         * @see javax.management.MBeanServerConnection#getMBeanCount()
         */
        public Integer getMBeanCount() throws IOException {
            checkConnection();
            return connection.getMBeanCount();
        }

        /**
         * @param name
         * @param attribute
         * @return
         * @throws MBeanException
         * @throws AttributeNotFoundException
         * @throws InstanceNotFoundException
         * @throws ReflectionException
         * @throws IOException
         * @see javax.management.MBeanServerConnection#getAttribute(javax.management.ObjectName, java.lang.String)
         */
        public Object getAttribute(ObjectName name, String attribute) throws MBeanException, AttributeNotFoundException,
                InstanceNotFoundException, ReflectionException, IOException {
            checkConnection();
            return connection.getAttribute(name, attribute);
        }

        /**
         * @param name
         * @param attributes
         * @return
         * @throws InstanceNotFoundException
         * @throws ReflectionException
         * @throws IOException
         * @see javax.management.MBeanServerConnection#getAttributes(javax.management.ObjectName, java.lang.String[])
         */
        public AttributeList getAttributes(ObjectName name, String[] attributes) throws InstanceNotFoundException,
                ReflectionException, IOException {
            checkConnection();
            return connection.getAttributes(name, attributes);
        }

        /**
         * @param name
         * @param attribute
         * @throws InstanceNotFoundException
         * @throws AttributeNotFoundException
         * @throws InvalidAttributeValueException
         * @throws MBeanException
         * @throws ReflectionException
         * @throws IOException
         * @see javax.management.MBeanServerConnection#setAttribute(javax.management.ObjectName, javax.management.Attribute)
         */
        public void setAttribute(ObjectName name, Attribute attribute) throws InstanceNotFoundException,
                AttributeNotFoundException, InvalidAttributeValueException, MBeanException, ReflectionException, IOException {
            checkConnection();
            connection.setAttribute(name, attribute);
        }

        /**
         * @param name
         * @param attributes
         * @return
         * @throws InstanceNotFoundException
         * @throws ReflectionException
         * @throws IOException
         * @see javax.management.MBeanServerConnection#setAttributes(javax.management.ObjectName, javax.management.AttributeList)
         */
        public AttributeList setAttributes(ObjectName name, AttributeList attributes) throws InstanceNotFoundException,
                ReflectionException, IOException {
            checkConnection();
            return connection.setAttributes(name, attributes);
        }

        /**
         * @param name
         * @param operationName
         * @param params
         * @param signature
         * @return
         * @throws InstanceNotFoundException
         * @throws MBeanException
         * @throws ReflectionException
         * @throws IOException
         * @see javax.management.MBeanServerConnection#invoke(javax.management.ObjectName, java.lang.String, java.lang.Object[], java.lang.String[])
         */
        public Object invoke(ObjectName name, String operationName, Object[] params, String[] signature)
                throws InstanceNotFoundException, MBeanException, ReflectionException, IOException {
            checkConnection();
            return connection.invoke(name, operationName, params, signature);
        }

        /**
         * @return
         * @throws IOException
         * @see javax.management.MBeanServerConnection#getDefaultDomain()
         */
        public String getDefaultDomain() throws IOException {
            checkConnection();
            return connection.getDefaultDomain();
        }

        /**
         * @return
         * @throws IOException
         * @see javax.management.MBeanServerConnection#getDomains()
         */
        public String[] getDomains() throws IOException {
            checkConnection();
            return connection.getDomains();
        }

        /**
         * @param name
         * @param listener
         * @param filter
         * @param handback
         * @throws InstanceNotFoundException
         * @throws IOException
         * @see javax.management.MBeanServerConnection#addNotificationListener(javax.management.ObjectName, javax.management.NotificationListener, javax.management.NotificationFilter, java.lang.Object)
         */
        public void addNotificationListener(ObjectName name, NotificationListener listener, NotificationFilter filter,
                Object handback) throws InstanceNotFoundException, IOException {
            checkConnection();
            connection.addNotificationListener(name, listener, filter, handback);
        }

        /**
         * @param name
         * @param listener
         * @param filter
         * @param handback
         * @throws InstanceNotFoundException
         * @throws IOException
         * @see javax.management.MBeanServerConnection#addNotificationListener(javax.management.ObjectName, javax.management.ObjectName, javax.management.NotificationFilter, java.lang.Object)
         */
        public void addNotificationListener(ObjectName name, ObjectName listener, NotificationFilter filter, Object handback)
                throws InstanceNotFoundException, IOException {
            checkConnection();
            connection.addNotificationListener(name, listener, filter, handback);
        }

        /**
         * @param name
         * @param listener
         * @throws InstanceNotFoundException
         * @throws ListenerNotFoundException
         * @throws IOException
         * @see javax.management.MBeanServerConnection#removeNotificationListener(javax.management.ObjectName, javax.management.ObjectName)
         */
        public void removeNotificationListener(ObjectName name, ObjectName listener) throws InstanceNotFoundException,
                ListenerNotFoundException, IOException {
            checkConnection();
            connection.removeNotificationListener(name, listener);
        }

        /**
         * @param name
         * @param listener
         * @param filter
         * @param handback
         * @throws InstanceNotFoundException
         * @throws ListenerNotFoundException
         * @throws IOException
         * @see javax.management.MBeanServerConnection#removeNotificationListener(javax.management.ObjectName, javax.management.ObjectName, javax.management.NotificationFilter, java.lang.Object)
         */
        public void removeNotificationListener(ObjectName name, ObjectName listener, NotificationFilter filter, Object handback)
                throws InstanceNotFoundException, ListenerNotFoundException, IOException {
            checkConnection();
            connection.removeNotificationListener(name, listener, filter, handback);
        }

        /**
         * @param name
         * @param listener
         * @throws InstanceNotFoundException
         * @throws ListenerNotFoundException
         * @throws IOException
         * @see javax.management.MBeanServerConnection#removeNotificationListener(javax.management.ObjectName, javax.management.NotificationListener)
         */
        public void removeNotificationListener(ObjectName name, NotificationListener listener)
                throws InstanceNotFoundException, ListenerNotFoundException, IOException {
            checkConnection();
            connection.removeNotificationListener(name, listener);
        }

        /**
         * @param name
         * @param listener
         * @param filter
         * @param handback
         * @throws InstanceNotFoundException
         * @throws ListenerNotFoundException
         * @throws IOException
         * @see javax.management.MBeanServerConnection#removeNotificationListener(javax.management.ObjectName, javax.management.NotificationListener, javax.management.NotificationFilter, java.lang.Object)
         */
        public void removeNotificationListener(ObjectName name, NotificationListener listener, NotificationFilter filter,
                Object handback) throws InstanceNotFoundException, ListenerNotFoundException, IOException {
            checkConnection();
            connection.removeNotificationListener(name, listener, filter, handback);
        }

        /**
         * @param name
         * @return
         * @throws InstanceNotFoundException
         * @throws IntrospectionException
         * @throws ReflectionException
         * @throws IOException
         * @see javax.management.MBeanServerConnection#getMBeanInfo(javax.management.ObjectName)
         */
        public MBeanInfo getMBeanInfo(ObjectName name) throws InstanceNotFoundException, IntrospectionException,
                ReflectionException, IOException {
            checkConnection();
            return connection.getMBeanInfo(name);
        }

        /**
         * @param name
         * @param className
         * @return
         * @throws InstanceNotFoundException
         * @throws IOException
         * @see javax.management.MBeanServerConnection#isInstanceOf(javax.management.ObjectName, java.lang.String)
         */
        public boolean isInstanceOf(ObjectName name, String className) throws InstanceNotFoundException, IOException {
            checkConnection();
            return connection.isInstanceOf(name, className);
        }

        private void checkConnection(){
            try{
                this.connection.getMBeanCount();
                return;
            }catch(IOException ioe){
            }
            this.connection = this.getConnection();
        }
        private MBeanServerConnection getConnection() {
                try {
                    final HashMap<String, Object> env = new HashMap<String, Object>();
                    env.put(CallbackHandler.class.getName(), Authentication.getCallbackHandler());
                    connection = JMXConnectorFactory.connect(getRemoteJMXURL(), env).getMBeanServerConnection();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            return connection;
        }
    }
}

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

package org.jboss.as.test.integration.common.jms;

import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.helpers.ClientConstants;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Utility class for getting implementations of JMSOperations interface
 * @author <a href="jmartisk@redhat.com">Jan Martiska</a>
 */
public class JMSOperationsProvider {

    private static final Logger logger = Logger.getLogger(JMSOperationsProvider.class);

    private static final String PROPERTY_NAME = "jmsoperations.implementation.class";
    private static final String FILE_NAME = "jmsoperations.properties";

    /**
     * Gets an instance of a JMSOperations implementation for a particular JMS provider based on the classname
     * given by property "jmsoperations.implementation.class" in jmsoperations.properties somewhere on the classpath
     * The property should contain a fully qualified name of a class that implements JMSOperations interface
     * The setting in that file can be overriden by a system property declaration
     *
     * @param client {@link ManagementClient} to pass to the JMSOperations implementation class' constructor
     *
     * @return a JMSOperations implementation that is JMS-provider-dependent
     *
     * @deprecated use {@link #getInstance(org.jboss.as.controller.client.ModelControllerClient)} instead
     */
    @Deprecated
    public static JMSOperations getInstance(ManagementClient client) {
        String className;
        // first try to get the property from system properties
        className = System.getProperty(PROPERTY_NAME);
        // if this was not defined, try to get it from jmsoperations.properties
        if(className == null) {
            ClassLoader tccl = Thread.currentThread().getContextClassLoader();
            InputStream stream = tccl.getResourceAsStream(FILE_NAME);
            Properties propsFromFile = new Properties();
            try {
                propsFromFile.load(stream);
                className = propsFromFile.getProperty(PROPERTY_NAME);
            } catch(IOException ex) {
                throw new RuntimeException(ex);
            }
        }
        if(className == null) {
            throw new JMSOperationsException("Please specify a property " + PROPERTY_NAME + " in " + FILE_NAME);
        }
        Object jmsOperationsInstance;
        try {
            Class clazz = Class.forName(className);
            jmsOperationsInstance = clazz.getConstructor(ManagementClient.class).newInstance(client);
        } catch (Exception e) {
            throw new JMSOperationsException(e);
        }
        if(!(jmsOperationsInstance instanceof JMSOperations)) {
            throw new JMSOperationsException("Class " + className + " does not implement interface JMSOperations");
        }
        return (JMSOperations)jmsOperationsInstance;
    }

    /**
     * Gets an instance of a JMSOperations implementation for a particular JMS provider based on the classname
     * given by property "jmsoperations.implementation.class" in jmsoperations.properties somewhere on the classpath
     * The property should contain a fully qualified name of a class that implements JMSOperations interface
     * The setting in that file can be overriden by a system property declaration
     *
     * @param client {@link ModelControllerClient} to pass to the JMSOperations implementation class' constructor
     *
     * @return a JMSOperations implementation that is JMS-provider-dependent
     */
    public static JMSOperations getInstance(ModelControllerClient client) {
        String className;
        // first try to get the property from system properties
        className = System.getProperty(PROPERTY_NAME);
        // if this was not defined, try to get it from jmsoperations.properties
        if (className == null) {
            ClassLoader tccl = Thread.currentThread().getContextClassLoader();
            InputStream stream = tccl.getResourceAsStream(FILE_NAME);
            Properties propsFromFile = new Properties();
            try {
                propsFromFile.load(stream);
                className = propsFromFile.getProperty(PROPERTY_NAME);
            } catch(IOException ex) {
                throw new RuntimeException(ex);
            }
        }
        if(className == null) {
            throw new JMSOperationsException("Please specify a property " + PROPERTY_NAME + " in " + FILE_NAME);
        }
        Object jmsOperationsInstance;
        try {
            Class clazz = Class.forName(className);
            jmsOperationsInstance = clazz.getConstructor(ModelControllerClient.class).newInstance(client);
        } catch (Exception e) {
            throw new JMSOperationsException(e);
        }
        if (!(jmsOperationsInstance instanceof JMSOperations)) {
            throw new JMSOperationsException("Class " + className + " does not implement interface JMSOperations");
        }
        return (JMSOperations)jmsOperationsInstance;
    }

    static void execute(ManagementClient managementClient, final ModelNode operation) throws IOException, JMSOperationsException {
        execute(managementClient.getControllerClient(), operation);
    }

    static void execute(ModelControllerClient client, final ModelNode operation) throws IOException, JMSOperationsException {
        ModelNode result = client.execute(operation);
        if (result.hasDefined(ClientConstants.OUTCOME) && ClientConstants.SUCCESS.equals(result.get(ClientConstants.OUTCOME).asString())) {
            logger.trace("Operation successful for update = " + operation.toString());
        } else if (result.hasDefined(ClientConstants.FAILURE_DESCRIPTION)) {
            final String failureDesc = result.get(ClientConstants.FAILURE_DESCRIPTION).toString();
            throw new JMSOperationsException(failureDesc);
        } else {
            throw new JMSOperationsException("Operation not successful; outcome = " + result.get(ClientConstants.OUTCOME));
        }

    }
}

/*
 * Copyright 2016 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.as.test.multinode.security.util;

import java.net.InetSocketAddress;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.as.test.multinode.security.api.EJBInfo;
import org.jboss.as.test.multinode.security.api.RemoteEJBConfig;
import org.wildfly.naming.client.WildFlyInitialContextFactory;

/**
 * @author bmaxwell
 *
 */
public class EJBUtil {

    public static final String JMS_REMOTE_CONNECTION_FACTORY_PATH = "jms/RemoteConnectionFactory";

    public static String getEjbLookupPath(String applicationName, String moduleName, String ejbName, Class ejbInterface) {
        if (applicationName == null)
            applicationName = "";
        if (moduleName == null)
            moduleName = "";
        return String.format("ejb:%s/%s/%s!%s", applicationName, moduleName, ejbName, ejbInterface.getName());
    }

    public static String getEjbJndiLookupPath(String applicationName, String moduleName, String ejbName, Class ejbInterface) {
        if (applicationName == null)
            applicationName = "";
        if (moduleName == null)
            moduleName = "";
        return String.format("%s/%s/%s!%s", applicationName, moduleName, ejbName, ejbInterface.getName());
    }

//    public static ConnectionFactory getRemoteConnectionFactory(Context ctx) throws NamingException {
//        return (ConnectionFactory) ctx.lookup(JMS_REMOTE_CONNECTION_FACTORY_PATH);
//    }
//
//    public static ConnectionFactory getRemoteConnectionFactory(String host, Integer port, String username, String password)
//            throws NamingException {
//        return (ConnectionFactory) getWildflyInitialContext(host, port, username, password)
//                .lookup(JMS_REMOTE_CONNECTION_FACTORY_PATH);
//    }
//
//    public static void sendMessage(Context ctx, String username, String password, String queueJNDIAddress) throws Exception {
//        ConnectionFactory connectionFactory = getRemoteConnectionFactory(ctx);
//        Connection connection = connectionFactory.createConnection(username, password);
//        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
//        MessageProducer producer = session.createProducer((Destination) ctx.lookup(queueJNDIAddress));
//        producer.setDeliveryMode(DeliveryMode.PERSISTENT);
//        connection.start();
//        TextMessage textmessage = session.createTextMessage();
//        producer.send(textmessage);
//        connection.close();
//    }
//
//    public static void sendMessage(String host, Integer port, String username, String password, String queueJNDIAddress)
//            throws Exception {
//        Context ctx = getWildflyInitialContext(host, port, username, password);
//        ConnectionFactory connectionFactory = (ConnectionFactory) ctx.lookup(JMS_REMOTE_CONNECTION_FACTORY_PATH);
//        Connection connection = connectionFactory.createConnection(username, password);
//        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
//        MessageProducer producer = session.createProducer((Destination) ctx.lookup(queueJNDIAddress));
//        producer.setDeliveryMode(DeliveryMode.PERSISTENT);
//        connection.start();
//        TextMessage textmessage = session.createTextMessage();
//        producer.send(textmessage);
//        connection.close();
//    }

    public static Object lookupEjb(RemoteEJBConfig config, EJBInfo ejbInfo) throws NamingException {
        return lookupEjb(ejbInfo.getRemoteLookupPath(), config.getHost(), config.getPort(), config.getUsername(), config.getPassword());
    }

    public static Object lookupEjb(String applicationName, String moduleName, String ejbName, Class ejbInterface, String host,
            Integer port, String username, String password) throws NamingException {
        return getWildflyInitialContext(host, port, username, password)
                .lookup(getEjbLookupPath(applicationName, moduleName, ejbName, ejbInterface));
    }

    public static Object lookupEjb(String applicationName, String moduleName, String ejbName, Class ejbInterface,
            String protocol, String host, Integer port, String username, String password) throws NamingException {
        return getWildflyInitialContext(protocol, host, port, username, password)
                .lookup(getEjbLookupPath(applicationName, moduleName, ejbName, ejbInterface));
    }

    public static Object lookupEjb(String ejbPath, String protocol, String host, Integer port, String username, String password)
            throws NamingException {
        return getWildflyInitialContext(protocol, host, port, username, password).lookup(ejbPath);
    }

    public static Object lookupEjb(String ejbPath, String host, Integer port, String username, String password)
            throws NamingException {
        return getWildflyInitialContext(host, port, username, password).lookup(ejbPath);
    }

    public static Context getWildflyInitialContext(String host, Integer port, String username, String password)
            throws NamingException {
        Properties environment = getWildflyInitialContextxProperties(host, port, username, password);
        return new InitialContext(environment);
    }

    public static Context getWildflyInitialContext(String protocol, String host, Integer port, String username, String password)
            throws NamingException {
        Properties environment = getWildflyInitialContextxProperties(protocol, host, port, username, password);
        return new InitialContext(environment);
    }

    public static Properties getWildflyInitialContextxProperties(String host, Integer port, String username, String password) {
        return getWildflyInitialContextxProperties(null, host, port, username, password);
    }

    public static Properties getWildflyInitialContextxProperties(String protocol, String host, Integer port, String username,
            String password) {
        Properties props = new Properties();
        if (protocol == null)
            protocol = "remote+http";
        if (host == null)
            host = "localhost";
        if (port == null)
            port = 8080;

        props.put(Context.INITIAL_CONTEXT_FACTORY, WildFlyInitialContextFactory.class.getName());
        props.put(Context.PROVIDER_URL, String.format("%s://%s:%d", protocol, host, port));
        if (username != null)
            props.put(Context.SECURITY_PRINCIPAL, username);
        if (password != null)
            props.put(Context.SECURITY_CREDENTIALS, password);
        return props;
    }

    // remote naming
    public static Context newRemoteNamingInitialContext(String providerUrl, String username, String password) throws Exception {
        Hashtable<String, String> env = new Hashtable<String, String>();
        env.put(Context.URL_PKG_PREFIXES, "org.jboss.ejb.client.naming");
        env.put("java.naming.factory.initial", "org.jboss.naming.remote.client.InitialContextFactory");
        env.put("java.naming.provider.url", providerUrl);
        env.put("jboss.naming.client.ejb.context", "true");

        if (username != null)
            env.put(Context.SECURITY_PRINCIPAL, username);
        if (password != null)
            env.put(Context.SECURITY_CREDENTIALS, password);

        return new InitialContext(env);
    }

    public static void closeContext(Context ctx) {
        if (ctx != null) {
            try {
                ctx.close();
            } catch (Throwable t) {
            }
        }
    }

    public static void closeScopedContext(Context scopedContext) {
        if (scopedContext != null) {
            try {
                ((Context) scopedContext.lookup("ejb:")).close();
            } catch (Throwable t) {
            }
            try {
                closeContext(scopedContext);
            } catch (Throwable t) {
            }
        }
    }

    public static final String JBOSS_SOURCE_ADDRESS = "jboss.source-address";

    // only works in EAP 7.1+
    public static InetSocketAddress getClientSourceAddress(Map<String, Object> contextData) {
        InetSocketAddress clientSourceAddress = (InetSocketAddress) contextData.get(JBOSS_SOURCE_ADDRESS);
        return clientSourceAddress;
    }

    public static String getJBossNodeName() {
        return System.getProperty("jboss.node.name");
    }
}
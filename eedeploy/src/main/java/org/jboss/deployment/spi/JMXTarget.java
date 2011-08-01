/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.deployment.spi;

import java.net.InetAddress;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;

import javax.enterprise.deploy.shared.ModuleType;
import javax.enterprise.deploy.spi.TargetModuleID;
import javax.enterprise.deploy.spi.exceptions.TargetException;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.logging.Logger;

/**
 * A Target that deploys using the JMX adaptor to communicate with the MainDeployer using file URLs to the deployments. This
 * target is selected by including a targetType=jmx param in the DeploymentManager deployURI.
 *
 * @author Thomas.Diesler@jboss.org
 * @author Scott.Stark@jboss.com
 *
 */
public class JMXTarget implements JBossTarget {
    private static final Logger log = Logger.getLogger(JMXTarget.class);
    public static final String DESCRIPTION = "JBoss JMX deployment target";
    /**
     * The default RMIAdaptor JNDI location
     */
    private static final String DEFAULT_ADAPTOR_PATH = "/jmx/invoker/RMIAdaptor";
    /** The JSR88 deployment manager default and uri option name */
    private static final String JSR88_MBEAN = "jboss.management.local:type=JSR88DeploymentManager,name=DefaultManager";
    private static final String JSR88_MBEAN_OPT = "jsr88MBean";

    /** The deployment target uri */
    private URI deployURI;
    /** The JNDI properties used to locate the MBeanServer */
    private Properties jndiEnv;
    private ObjectName jsr88MBean;

    /**
     * @todo merge the query parameter parsing for jndi and mbean names and test proper escaping of unsafe url chars.
     *
     * @param deployURI
     */
    public JMXTarget(URI deployURI) {
        log.debug("new JMXTarget: " + deployURI);
        try {
            String localHostName = InetAddress.getLocalHost().getHostName();

            String scheme = deployURI.getScheme();
            String host = deployURI.getHost();
            int port = deployURI.getPort();
            String path = deployURI.getPath();
            String query = deployURI.getRawQuery();

            String uri = deployURI.toASCIIString();
            if (uri.startsWith(DeploymentManagerImpl.DEPLOYER_URI)) {
                // Using JNDI defaults
                scheme = "jnp";
                host = localHostName;
                port = 1099;
                path = DEFAULT_ADAPTOR_PATH;

                try {
                    InitialContext iniCtx = new InitialContext();
                    Hashtable env = iniCtx.getEnvironment();
                    String providerURL = (String) env.get(InitialContext.PROVIDER_URL);
                    if (providerURL != null) {
                        // In case there is no schema returned
                        if (providerURL.indexOf("://") < 0)
                            providerURL = "jnp://" + providerURL;

                        URI providerURI = new URI(providerURL);
                        scheme = providerURI.getScheme();
                        host = providerURI.getHost();
                        port = providerURI.getPort();
                    }
                } catch (NamingException e) {
                    log.error(e);
                }
            }

            StringBuffer tmp = new StringBuffer(scheme + "://");
            tmp.append(host != null ? host : localHostName);
            tmp.append(port > 0 ? ":" + port : "");
            tmp.append(path != null && path.length() > 0 ? path : DEFAULT_ADAPTOR_PATH);
            tmp.append(query != null ? "?" + query : "");
            deployURI = new URI(tmp.toString());

            log.debug("URI changed to: " + deployURI);
            this.deployURI = deployURI;

            // Parse the query for options
            parseQuery();
        } catch (Exception e) {
            log.error(e);
        }

    }

    /**
     * Get the target's description
     *
     * @return the description
     */
    public String getDescription() {
        return DESCRIPTION;
    }

    /**
     * Get the target's name
     *
     * @return the name
     */
    public String getName() {
        return deployURI.toString();
    }

    /**
     * Get the target's host name
     */
    public String getHostName() {
        return deployURI.getHost();
    }

    /**
     * Deploy a given module
     */
    public void deploy(TargetModuleID targetModuleID) throws Exception {
        TargetModuleIDImpl moduleID = (TargetModuleIDImpl) targetModuleID;
        SerializableTargetModuleID smoduleID = new SerializableTargetModuleID(moduleID);
        MBeanServerConnection server = getMBeanServerConnection();
        String url = targetModuleID.getModuleID();
        Object[] args = { smoduleID };
        String[] sig = { smoduleID.getClass().getName() };
        log.info("Begin deploy: " + url);
        server.invoke(jsr88MBean, "deploy", args, sig);
        log.info("End deploy");
    }

    /**
     * Start a given module
     */
    public void start(TargetModuleID targetModuleID) throws Exception {
        MBeanServerConnection server = getMBeanServerConnection();
        URL url = new URL(targetModuleID.getModuleID());
        Object[] args = { url };
        String[] sig = { URL.class.getName() };
        log.debug("Start: " + url);
        args = new Object[] { url.toExternalForm() };
        sig = new String[] { String.class.getName() };
        log.info("Begin start: " + url);
        server.invoke(jsr88MBean, "start", args, sig);
        log.info("End start");
    }

    /**
     * Stop a given module
     */
    public void stop(TargetModuleID targetModuleID) throws Exception {
        MBeanServerConnection server = getMBeanServerConnection();
        URL url = new URL(targetModuleID.getModuleID());
        Object[] args = { url };
        String[] sig = { URL.class.getName() };
        log.debug("Stop: " + url);
        args = new Object[] { url.toExternalForm() };
        sig = new String[] { String.class.getName() };
        log.info("Begin stop: " + url);
        server.invoke(jsr88MBean, "stop", args, sig);
        log.info("End stop");
    }

    /**
     * Undeploy a given module
     */
    public void undeploy(TargetModuleID targetModuleID) throws Exception {
        MBeanServerConnection server = getMBeanServerConnection();
        String url = targetModuleID.getModuleID();
        Object[] args = { url };
        String[] sig = { String.class.getName() };
        log.info("Begin undeploy: " + url);
        server.invoke(jsr88MBean, "undeploy", args, sig);
        log.info("End undeploy");
    }

    /**
     * Retrieve the list of all J2EE application modules running or not running on the identified targets.
     */
    public TargetModuleID[] getAvailableModules(ModuleType moduleType) throws TargetException {
        try {
            List list = new ArrayList();
            MBeanServerConnection server = getMBeanServerConnection();
            Object[] args = { new Integer(moduleType.getValue()) };
            String[] sig = { int.class.getName() };
            SerializableTargetModuleID[] modules = (SerializableTargetModuleID[]) server.invoke(jsr88MBean, "getAvailableModules", args, sig);
            for (int n = 0; n < modules.length; n++) {
                SerializableTargetModuleID id = modules[n];
                String moduleID = id.getModuleID();
                boolean isRunning = id.isRunning();
                ModuleType type = ModuleType.getModuleType(id.getModuleType());
                TargetModuleIDImpl tmid = new TargetModuleIDImpl(this, moduleID, null, isRunning, type);
                convertChildren(tmid, id);
                list.add(tmid);
            }

            TargetModuleID[] targetModuleIDs = new TargetModuleID[list.size()];
            list.toArray(targetModuleIDs);
            return targetModuleIDs;
        } catch (Exception e) {
            TargetException tex = new TargetException("Failed to get available modules");
            tex.initCause(e);
            throw tex;
        }
    }

    private void convertChildren(TargetModuleIDImpl parent, SerializableTargetModuleID parentID) {
        SerializableTargetModuleID[] children = parentID.getChildModuleIDs();
        int length = children != null ? children.length : 0;
        for (int n = 0; n < length; n++) {
            SerializableTargetModuleID id = children[n];
            String moduleID = id.getModuleID();
            boolean isRunning = id.isRunning();
            ModuleType type = ModuleType.getModuleType(id.getModuleType());
            TargetModuleIDImpl child = new TargetModuleIDImpl(this, moduleID, parent, isRunning, type);
            parent.addChildTargetModuleID(child);
            convertChildren(child, id);
        }
    }

    // Get MBeanServerConnection
    private MBeanServerConnection getMBeanServerConnection() throws NamingException {
        Properties env = buildJNDIEnv();
        String lookupPath = deployURI.getPath();
        log.debug("JNDI lookup: " + lookupPath);
        log.trace("Creating InitialContext with env: " + env);
        InitialContext ctx = new InitialContext(env);
        MBeanServerConnection server = (MBeanServerConnection) ctx.lookup(lookupPath);
        return server;
    }

    /**
     * Parse the query portion of the deployment URI to look for options: jsr88MBean : specifies the JSR88 mbean service that
     * provides the deployment manager deploy/start/stop/undeploy operations.
     */
    private void parseQuery() throws Exception {
        String query = deployURI.getRawQuery();
        log.debug("DeployURI.rawQuery: " + query);
        Properties params = new Properties();
        if (query != null) {
            /*
             * Break the raw query into the name=value pairs. This processing is too fragile to how the URI was built as it
             * expects that only the name/value portions of the query were encoded.
             */
            String[] options = query.split("[&=]");
            for (int n = 0; n < options.length; n += 2) {
                String name = URLDecoder.decode(options[n], "UTF-8");
                String value = URLDecoder.decode(options[n + 1], "UTF-8");
                params.setProperty(name, value);
            }
        }

        String name = params.getProperty(JSR88_MBEAN_OPT, JSR88_MBEAN);
        jsr88MBean = new ObjectName(name);
    }

    private Properties buildJNDIEnv() {
        if (jndiEnv == null) {
            jndiEnv = new Properties();

            // Parse the query string for name=value pairs to put into the env
            String query = deployURI.getQuery();
            if (query != null) {
                log.debug("Parsing query string: " + query);
                StringTokenizer tokenizer = new StringTokenizer(query, "=&");
                while (tokenizer.hasMoreTokens()) {
                    String name = tokenizer.nextToken();
                    String value = tokenizer.nextToken();
                    jndiEnv.setProperty(name, value);
                }
            }

            // Set defaults for missing properties
            if (jndiEnv.getProperty(Context.INITIAL_CONTEXT_FACTORY) == null) {
                jndiEnv.setProperty(Context.INITIAL_CONTEXT_FACTORY, "org.jnp.interfaces.NamingContextFactory");
            }
            if (jndiEnv.getProperty(Context.PROVIDER_URL) == null) {
                String host = deployURI.getHost();
                if (host == null) {
                    try {
                        host = InetAddress.getLocalHost().getHostName();
                    } catch (UnknownHostException e) {
                        host = "localhost";
                    }
                }

                int port = deployURI.getPort();
                if (port <= 0) {
                    port = 1099;
                }

                String jnpURL = "jnp://" + host + ':' + port;
                jndiEnv.setProperty(Context.PROVIDER_URL, jnpURL);
            }
            if (jndiEnv.getProperty(Context.OBJECT_FACTORIES) == null) {
                jndiEnv.setProperty(Context.OBJECT_FACTORIES, "org.jboss.naming");
            }
        }

        return jndiEnv;
    }
}

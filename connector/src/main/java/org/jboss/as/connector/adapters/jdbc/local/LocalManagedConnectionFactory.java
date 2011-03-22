/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
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

package org.jboss.as.connector.adapters.jdbc.local;

import org.jboss.as.connector.adapters.jdbc.BaseWrapperManagedConnectionFactory;
import org.jboss.as.connector.adapters.jdbc.spi.URLSelectorStrategy;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.Driver;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.resource.ResourceException;
import javax.resource.spi.ConnectionManager;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.ManagedConnection;
import javax.security.auth.Subject;


/**
 * LocalManagedConnectionFactory
 *
 * @author <a href="mailto:d_jencks@users.sourceforge.net">David Jencks</a>
 * @author <a href="mailto:adrian@jboss.com">Adrian Brock</a>
 * @version $Revision: 73443 $
 */
public class LocalManagedConnectionFactory extends BaseWrapperManagedConnectionFactory {
    private static final long serialVersionUID = 4698955390505160469L;

    private transient Driver driver;

    private String connectionURL;

    private URLSelectorStrategy urlSelector;

    /**
     * The connection properties
     */
    protected String connectionProperties;

    /**
     * Constructor
     */
    public LocalManagedConnectionFactory() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object createConnectionFactory(ConnectionManager cm) throws ResourceException {
        if (driver == null)
            throw new ResourceException("driver is null");

        if (connectionURL == null)
            throw new ResourceException("connectionURL is null");

        return super.createConnectionFactory(cm);
    }

    /**
     * Get the value of ConnectionURL.
     *
     * @return value of ConnectionURL.
     */
    public String getConnectionURL() {
        return connectionURL;
    }

    /**
     * Set the value of ConnectionURL.
     *
     * @param connectionURL Value to assign to ConnectionURL.
     */
    public void setConnectionURL(final String connectionURL) //throws ResourceException
    {
        this.connectionURL = connectionURL;

        if (urlDelimiter != null)
            initUrlSelector();
    }

    /**
     * Set the driver value.
     *
     * @param driver The driver value.
     */
    public synchronized void setDriver(final Driver driver) {
        this.driver = driver;
    }

    /**
     * Get the value of connectionProperties.
     *
     * @return value of connectionProperties.
     */
    public String getConnectionProperties() {
        return connectionProperties;
    }

    /**
     * Set the value of connectionProperties.
     *
     * @param connectionProperties Value to assign to connectionProperties.
     */
    public void setConnectionProperties(String connectionProperties) {
        this.connectionProperties = connectionProperties;
        connectionProps.clear();

        if (connectionProperties != null) {
            // Map any \ to \\
            connectionProperties = connectionProperties.replaceAll("\\\\", "\\\\\\\\");
            connectionProperties = connectionProperties.replaceAll(";", "\n");

            InputStream is = new ByteArrayInputStream(connectionProperties.getBytes());
            try {
                connectionProps.load(is);
            } catch (IOException ioe) {
                throw new RuntimeException("Could not load connection properties", ioe);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public ManagedConnection createManagedConnection(Subject subject, ConnectionRequestInfo cri)
            throws ResourceException {
        Properties props = getConnectionProperties(subject, cri);
        // Some friendly drivers (Oracle, you guessed right) modify the props you supply.
        // Since we use our copy to identify compatibility in matchManagedConnection, we need
        // a pristine copy for our own use.  So give the friendly driver a copy.
        Properties copy = (Properties) props.clone();
        boolean trace = log.isTraceEnabled();
        if (trace) {
            // Make yet another copy to mask the password
            Properties logCopy = copy;
            if (copy.getProperty("password") != null) {
                logCopy = (Properties) props.clone();
                logCopy.setProperty("password", "--hidden--");
            }
            log.trace("Using properties: " + logCopy);
        }

        if (urlSelector != null) {
            return getHALocalManagedConnection(props, copy);
        } else {
            return getLocalManagedConnection(props, copy);
        }
    }

    private LocalManagedConnection getLocalManagedConnection(Properties props, Properties copy)
            throws ResourceException {
        Connection con = null;
        try {
            String url = getConnectionURL();
            Driver d = getDriver(url);
            con = d.connect(url, copy);
            if (con == null)
                throw new ResourceException("Wrong driver class [" + d.getClass() + "] for this connection URL [" + url + "]");

            return new LocalManagedConnection(this, con, props, transactionIsolation, preparedStatementCacheSize);
        } catch (Throwable e) {
            if (con != null) {
                try {
                    con.close();
                } catch (Throwable ignored) {
                    // Ignore
                }
            }
            throw new ResourceException("Could not create connection", e);
        }
    }

    private LocalManagedConnection getHALocalManagedConnection(Properties props, Properties copy)
            throws ResourceException {
        boolean trace = log.isTraceEnabled();

        // try to get a connection as many times as many urls we have in the list
        for (int i = 0; i < urlSelector.getCustomSortedUrls().size(); ++i) {
            String url = (String) urlSelector.getUrlObject();
            if (trace) {
                log.trace("Trying to create a connection to " + url);
            }

            Connection con = null;
            try {
                Driver d = getDriver(url);
                con = d.connect(url, copy);
                if (con == null) {
                    log.warn("Wrong driver class [" + d.getClass() + "] for this connection URL: " + url);
                    urlSelector.failedUrlObject(url);
                } else {
                    return new LocalManagedConnection(this, con, props, transactionIsolation, preparedStatementCacheSize);
                }
            } catch (Exception e) {
                if (con != null) {
                    try {
                        con.close();
                    } catch (Throwable ignored) {
                        // Ignore
                    }
                }
                log.warn("Failed to create connection for " + url + ": " + e.getMessage());
                urlSelector.failedUrlObject(url);
            }
        }

        // we have supposedly tried all the urls
        throw new ResourceException("Could not create connection using any of the URLs: " +
                urlSelector.getAllUrlObjects());
    }

    /**
     * Set the URL delimiter
     *
     * @param urlDelimiter The value
     */
    @Override
    public void setURLDelimiter(String urlDelimiter) //throws ResourceException
    {
        super.urlDelimiter = urlDelimiter;
        if (getConnectionURL() != null) {
            initUrlSelector();
        }
    }

    /**
     * Init URL selector
     */
    protected void initUrlSelector() {//throws ResourceException
        boolean trace = log.isTraceEnabled();

        List<String> urlsList = new ArrayList<String>();
        String urlsStr = getConnectionURL();
        String url;
        int urlStart = 0;
        int urlEnd = urlsStr.indexOf(urlDelimiter);

        while (urlEnd > 0) {
            url = urlsStr.substring(urlStart, urlEnd);
            urlsList.add(url);
            urlStart = ++urlEnd;
            urlEnd = urlsStr.indexOf(urlDelimiter, urlEnd);

            if (trace)
                log.trace("added HA connection url: " + url);
        }

        if (urlStart != urlsStr.length()) {
            url = urlsStr.substring(urlStart, urlsStr.length());
            urlsList.add(url);

            if (trace)
                log.trace("added HA connection url: " + url);
        }

        if (getUrlSelectorStrategyClassName() == null) {
            this.urlSelector = new URLSelector(urlsList);
            log.debug("Default URLSelectorStrategy is being used : " + urlSelector);
        } else {
            this.urlSelector = (URLSelectorStrategy) loadClass(getUrlSelectorStrategyClassName(), urlsList);
            log.debug("Customized URLSelectorStrategy is being used : " + urlSelector);
        }
    }

    /**
     * Default implementation
     */
    public static class URLSelector implements URLSelectorStrategy {
        private final List<String> urls;
        private int urlIndex;
        private String url;

        /**
         * Constructor
         *
         * @param urls The urls
         */
        public URLSelector(List<String> urls) {
            if (urls == null || urls.size() == 0) {
                throw new IllegalStateException("Expected non-empty list of connection URLs but got: " + urls);
            }
            this.urls = Collections.unmodifiableList(urls);
        }

        /**
         * Get the url
         *
         * @return The value
         */
        public synchronized String getUrl() {
            if (url == null) {
                if (urlIndex == urls.size()) {
                    urlIndex = 0;
                }
                url = urls.get(urlIndex++);
            }
            return url;
        }

        /**
         * Failed an url
         *
         * @param url The value
         */
        public synchronized void failedUrl(String url) {
            if (url.equals(this.url)) {
                this.url = null;
            }
        }

        /**
         * Get all the custom url objects
         *
         * @return The value
         */
        public List<?> getCustomSortedUrls() {
            return urls;
        }

        /**
         * Fail an url object
         *
         * @param urlObject The value
         */
        public void failedUrlObject(Object urlObject) {
            failedUrl((String) urlObject);
        }

        /**
         * Get all the url objects
         *
         * @return The value
         */
        public List<?> getAllUrlObjects() {
            return urls;
        }

        /**
         * Get the url object
         *
         * @return The value
         */
        public Object getUrlObject() {
            return getUrl();
        }
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("rawtypes")
    public ManagedConnection matchManagedConnections(final Set mcs, final Subject subject,
                                                     final ConnectionRequestInfo cri) throws ResourceException {
        Properties newProps = getConnectionProperties(subject, cri);

        for (Iterator<?> i = mcs.iterator(); i.hasNext();) {
            Object o = i.next();

            if (o instanceof LocalManagedConnection) {
                LocalManagedConnection mc = (LocalManagedConnection) o;

                //First check the properties
                if (mc.getProperties().equals(newProps)) {
                    //Next check to see if we are validating on matchManagedConnections
                    if ((getValidateOnMatch() && mc.checkValid()) || !getValidateOnMatch()) {
                        return mc;
                    }
                }
            }
        }

        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        int result = 17;
        result = result * 37 + ((connectionURL == null) ? 0 : connectionURL.hashCode());
        result = result * 37 + ((driver == null) ? 0 : driver.getClass().hashCode());
        result = result * 37 + ((userName == null) ? 0 : userName.hashCode());
        result = result * 37 + ((password == null) ? 0 : password.hashCode());
        result = result * 37 + transactionIsolation;
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object other) {
        if (this == other)
            return true;

        if (getClass() != other.getClass())
            return false;

        LocalManagedConnectionFactory otherMcf = (LocalManagedConnectionFactory) other;

        return this.connectionURL.equals(otherMcf.connectionURL) && this.driver.getClass().equals(otherMcf.driver.getClass())
                && ((this.userName == null) ? otherMcf.userName == null : this.userName.equals(otherMcf.userName))
                && ((this.password == null) ? otherMcf.password == null : this.password.equals(otherMcf.password))
                && this.transactionIsolation == otherMcf.transactionIsolation;
    }

    /**
     * Check the driver for the given URL.  If it is not registered already
     * then register it.
     *
     * @param url The JDBC URL which we need a driver for.
     * @return The driver
     * @throws ResourceException Thrown if an error occurs
     */
    protected synchronized Driver getDriver(final String url) throws ResourceException {
        if (driver == null) {
            throw new ResourceException("No driver for connection [" + getJndiName() + "]");
        }
        return driver;
    }

    /**
     * Get the connection url
     *
     * @return The value
     */
    protected String internalGetConnectionURL() {
        return connectionURL;
    }
}

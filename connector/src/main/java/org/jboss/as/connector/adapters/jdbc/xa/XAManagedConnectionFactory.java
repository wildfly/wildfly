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

package org.jboss.as.connector.adapters.jdbc.xa;

import org.jboss.as.connector.adapters.jdbc.BaseWrapperManagedConnectionFactory;
import org.jboss.as.connector.adapters.jdbc.spi.URLSelectorStrategy;

import java.beans.PropertyEditor;
import java.beans.PropertyEditorManager;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.resource.ResourceException;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.ManagedConnection;
import javax.security.auth.Subject;
import javax.sql.XAConnection;
import javax.sql.XADataSource;

/**
 * XAManagedConnectionFactory
 *
 * @author <a href="mailto:d_jencks@users.sourceforge.net">David Jencks</a>
 * @author <a href="mailto:adrian@jboss.com">Adrian Brock</a>
 * @author <a href="mailto:jesper.pedersen@jboss.org">Jesper Pedersen</a>
 */
public class XAManagedConnectionFactory extends BaseWrapperManagedConnectionFactory {
    private static final long serialVersionUID = 1647927657609573729L;

    private Class<?> xaDataSourceClass;

    private String xaDataSourceProperties;

    /**
     * THe XA properties
     */
    protected final Properties xaProps = new Properties();

    private Boolean isSameRMOverrideValue;

    private XADataSource xads;

    private String urlProperty;

    private URLSelectorStrategy xadsSelector;

    /**
     * Constructor
     */
    public XAManagedConnectionFactory() {
    }

    /**
     * Get the URL property
     *
     * @return The value
     */
    public String getURLProperty() {
        return urlProperty;
    }

    /**
     * Set the URL property
     *
     * @param urlProperty The value
     * @throws ResourceException Thrown in case of an error
     */
    public void setURLProperty(String urlProperty) throws ResourceException {
        this.urlProperty = urlProperty;
        initSelector();
    }

    /**
     * Set the URL delimiter
     *
     * @param urlDelimiter The value
     * @throws ResourceException Thrown in case of an error
     */
    public void setURLDelimiter(String urlDelimiter) throws ResourceException {
        this.urlDelimiter = urlDelimiter;
        initSelector();
    }

    /**
     * Get the XaDataSourceClass value.
     *
     * @return the XaDataSourceClass value.
     */
    public Class<?> getXADataSourceClass() {
        return xaDataSourceClass;
    }

    /**
     * Set the XaDataSourceClass value.
     *
     * @param xaDataSourceClass The new XaDataSourceClass value.
     */
    public void setXADataSourceClass(Class<?> xaDataSourceClass) {
        this.xaDataSourceClass = xaDataSourceClass;
    }

    /**
     * Get the XADataSourceProperties value.
     *
     * @return the XADataSourceProperties value.
     */
    public String getXADataSourceProperties() {
        return xaDataSourceProperties;
    }

    /**
     * Set the XADataSourceProperties value.
     *
     * @param xaDataSourceProperties The new XADataSourceProperties value.
     * @throws ResourceException Thrown in case of an error
     */
    public void setXADataSourceProperties(String xaDataSourceProperties) throws ResourceException {
        this.xaDataSourceProperties = xaDataSourceProperties;
        xaProps.clear();

        if (xaDataSourceProperties != null) {
            // Map any \ to \\
            xaDataSourceProperties = xaDataSourceProperties.replaceAll("\\\\", "\\\\\\\\");

            InputStream is = new ByteArrayInputStream(xaDataSourceProperties.getBytes());
            try {
                xaProps.load(is);
            } catch (IOException ioe) {
                throw new ResourceException("Could not load connection properties", ioe);
            }
        }

        initSelector();
    }

    /**
     * Get the IsSameRMOverrideValue value.
     *
     * @return the IsSameRMOverrideValue value.
     */
    public Boolean getIsSameRMOverrideValue() {
        return isSameRMOverrideValue;
    }

    /**
     * Set the IsSameRMOverrideValue value.
     *
     * @param isSameRMOverrideValue The new IsSameRMOverrideValue value.
     */
    public void setIsSameRMOverrideValue(Boolean isSameRMOverrideValue) {
        this.isSameRMOverrideValue = isSameRMOverrideValue;
    }

    @SuppressWarnings("unchecked")
    private void initSelector() throws ResourceException {
        if (urlProperty != null && urlProperty.length() > 0) {
            String urlsStr = xaProps.getProperty(urlProperty);
            if (urlsStr != null && urlsStr.trim().length() > 0 &&
                    urlDelimiter != null && urlDelimiter.trim().length() > 0) {
                List<XAData> xaDataList = new ArrayList<XAData>(2);

                // copy xaProps
                // ctor doesn't work because iteration won't include defaults
                // Properties xaPropsCopy = new Properties(xaProps);
                Properties xaPropsCopy = new Properties();
                for (Iterator<?> i = xaProps.keySet().iterator(); i.hasNext();) {
                    Object key = i.next();
                    xaPropsCopy.put(key, xaProps.get(key));
                }

                int urlStart = 0;
                int urlEnd = urlsStr.indexOf(urlDelimiter);
                while (urlEnd > 0) {
                    String url = urlsStr.substring(urlStart, urlEnd);
                    xaPropsCopy.setProperty(urlProperty, url);
                    XADataSource xads = createXaDataSource(xaPropsCopy);
                    xaDataList.add(new XAData(xads, url));
                    urlStart = ++urlEnd;
                    urlEnd = urlsStr.indexOf(urlDelimiter, urlEnd);
                    log.debug("added XA HA connection url: " + url);
                }

                if (urlStart != urlsStr.length()) {
                    String url = urlsStr.substring(urlStart, urlsStr.length());
                    xaPropsCopy.setProperty(urlProperty, url);
                    XADataSource xads = createXaDataSource(xaPropsCopy);
                    xaDataList.add(new XAData(xads, url));
                    log.debug("added XA HA connection url: " + url);
                }

                if (getUrlSelectorStrategyClassName() == null) {
                    xadsSelector = new XADataSelector(xaDataList);
                    log.debug("Default URLSelectorStrategy is being used : " + xadsSelector);
                } else {
                    xadsSelector = (URLSelectorStrategy) loadClass(getUrlSelectorStrategyClassName(), xaDataList);
                    log.debug("Customized URLSelectorStrategy is being used : " + xadsSelector);
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private XADataSource createXaDataSource(Properties xaProps) throws ResourceException {
        if (getXADataSourceClass() == null) {
            throw new ResourceException("No XADataSourceClass supplied!");
        }

        XADataSource xads = null;
        try {
            xads = (XADataSource) xaDataSourceClass.newInstance();
            final Class<?>[] noClasses = new Class<?>[]{};
            for (Iterator<?> i = xaProps.keySet().iterator(); i.hasNext();) {
                String name = (String) i.next();
                String value = xaProps.getProperty(name);
                char firstCharName = Character.toUpperCase(name.charAt(0));
                if (name.length() > 1) {
                    name = firstCharName + name.substring(1);
                } else {
                    name = "" + firstCharName;
                }

                // This is a bad solution.  On the other hand the only known example
                // of a setter with no getter is for Oracle with password.
                // Anyway, each xadatasource implementation should get its
                // own subclass of this that explicitly sets the
                // properties individually.
                Class<?> type = null;
                try {
                    Method getter = xaDataSourceClass.getMethod("get" + name, noClasses);
                    type = getter.getReturnType();
                } catch (NoSuchMethodException e) {
                    try {
                        //HACK for now until we can rethink the XADataSourceProperties variable and pass type information
                        Method isMethod = xaDataSourceClass.getMethod("is" + name, (Class[]) null);
                        type = isMethod.getReturnType();
                    } catch (NoSuchMethodException nsme) {
                        type = String.class;
                    }
                }

                Method setter = xaDataSourceClass.getMethod("set" + name, new Class<?>[]{type});
                PropertyEditor editor = PropertyEditorManager.findEditor(type);

                if (editor == null)
                    throw new ResourceException("No property editor found for type: " + type);

                editor.setAsText(value);
                setter.invoke(xads, new Object[]{editor.getValue()});

            }
        } catch (InstantiationException ie) {
            throw new ResourceException("Could not create an XADataSource: ", ie);
        } catch (IllegalAccessException iae) {
            throw new ResourceException("Could not set a property: ", iae);
        } catch (IllegalArgumentException iae) {
            throw new ResourceException("Could not set a property: ", iae);
        } catch (InvocationTargetException ite) {
            throw new ResourceException("Could not invoke setter on XADataSource: ", ite);
        } catch (NoSuchMethodException nsme) {
            throw new ResourceException("Could not find accessor on XADataSource: ", nsme);
        }

        return xads;
    }

    /**
     * {@inheritDoc}
     */
    public ManagedConnection createManagedConnection(Subject subject, ConnectionRequestInfo cri)
            throws javax.resource.ResourceException {
        if (xadsSelector == null) {
            return getXAManagedConnection(subject, cri);
        }

        // try to get a connection as many times as many urls we have in the list
        for (int i = 0; i < xadsSelector.getCustomSortedUrls().size(); ++i) {
            XAData xaData = (XAData) xadsSelector.getUrlObject();

            if (log.isTraceEnabled())
                log.trace("Trying to create an XA connection to " + xaData.url);

            try {
                return getXAManagedConnection(subject, cri);
            } catch (ResourceException e) {
                log.warn("Failed to create an XA connection to " + xaData.url + ": " + e.getMessage());
                xadsSelector.failedUrlObject(xaData);
            }
        }

        // we have supposedly tried all the urls
        throw new ResourceException("Could not create connection using any of the URLs: " +
                xadsSelector.getAllUrlObjects());
    }

    /**
     * Get the managed connection
     *
     * @param subject The subject
     * @param cri     The connection request info
     * @return The connection
     * @throws ResourceException Thrown if an error occurs
     */
    public ManagedConnection getXAManagedConnection(Subject subject, ConnectionRequestInfo cri)
            throws ResourceException {
        XAConnection xaConnection = null;
        Properties props = getConnectionProperties(subject, cri);

        try {
            final String user = props.getProperty("user");
            final String password = props.getProperty("password");

            xaConnection = (user != null)
                    ? getXADataSource().getXAConnection(user, password)
                    : getXADataSource().getXAConnection();

            return newXAManagedConnection(props, xaConnection);
        } catch (Throwable e) {
            try {
                if (xaConnection != null)
                    xaConnection.close();
            } catch (Throwable ignored) {
                // Ignore
            }
            throw new ResourceException("Could not create connection", e);
        }
    }

    /**
     * This method can be overwritten by sublcasses to provide rm specific
     * implementation of XAManagedConnection
     *
     * @param props        The properties
     * @param xaConnection The XA connection
     * @return The managed connection
     * @throws SQLException Thrown if an error occurs
     */
    protected ManagedConnection newXAManagedConnection(Properties props, XAConnection xaConnection) throws SQLException {
        return new XAManagedConnection(this, xaConnection, props, transactionIsolation, preparedStatementCacheSize);
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("rawtypes")
    public ManagedConnection matchManagedConnections(Set mcs, Subject subject, ConnectionRequestInfo cri)
            throws ResourceException {
        Properties newProps = getConnectionProperties(subject, cri);
        for (Iterator<?> i = mcs.iterator(); i.hasNext();) {
            Object o = i.next();
            if (o instanceof XAManagedConnection) {
                XAManagedConnection mc = (XAManagedConnection) o;

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
    public int hashCode() {
        int result = 17;
        result = result * 37 + ((xaDataSourceClass == null) ? 0 : xaDataSourceClass.hashCode());
        result = result * 37 + xaProps.hashCode();
        result = result * 37 + ((userName == null) ? 0 : userName.hashCode());
        result = result * 37 + ((password == null) ? 0 : password.hashCode());
        result = result * 37 + transactionIsolation;
        return result;
    }

    /**
     * {@inheritDoc}
     */
    public boolean equals(Object other) {
        if (this == other)
            return true;

        if (getClass() != other.getClass())
            return false;

        XAManagedConnectionFactory otherMcf = (XAManagedConnectionFactory) other;
        return this.xaDataSourceClass.equals(otherMcf.xaDataSourceClass) && this.xaProps.equals(otherMcf.xaProps)
                && ((this.userName == null) ? otherMcf.userName == null : this.userName.equals(otherMcf.userName))
                && ((this.password == null) ? otherMcf.password == null : this.password.equals(otherMcf.password))
                && this.transactionIsolation == otherMcf.transactionIsolation;
    }

    /**
     * Get the XA datasource
     *
     * @return The value
     * @throws ResourceException Thrown if an error occurs
     */
    @SuppressWarnings("unchecked")
    protected synchronized XADataSource getXADataSource() throws ResourceException {
        if (xadsSelector != null) {
            XAData xada = (XAData) xadsSelector.getUrlObject();
            return xada.xads;
        }

        if (xads == null) {
            if (xaDataSourceClass == null)
                throw new ResourceException("No XADataSourceClass supplied!");

            try {
                xads = (XADataSource) xaDataSourceClass.newInstance();
                final Class<?>[] noClasses = new Class<?>[]{};

                for (Iterator<?> i = xaProps.keySet().iterator(); i.hasNext();) {
                    String name = (String) i.next();
                    String value = xaProps.getProperty(name);
                    char firstCharName = Character.toUpperCase(name.charAt(0));

                    if (name.length() > 1) {
                        name = firstCharName + name.substring(1);
                    } else {
                        name = "" + firstCharName;
                    }

                    // This is a bad solution.  On the other hand the only known example
                    // of a setter with no getter is for Oracle with password.
                    // Anyway, each xadatasource implementation should get its
                    // own subclass of this that explicitly sets the
                    // properties individually.

                    Class<?> type = null;
                    try {
                        Method getter = xaDataSourceClass.getMethod("get" + name, noClasses);
                        type = getter.getReturnType();
                    } catch (NoSuchMethodException e) {
                        try {
                            //HACK for now until we can rethink the XADataSourceProperties variable and pass type information
                            Method isMethod = xaDataSourceClass.getMethod("is" + name, (Class[]) null);
                            type = isMethod.getReturnType();
                        } catch (NoSuchMethodException nsme) {
                            type = String.class;
                        }
                    }

                    Method setter = xaDataSourceClass.getMethod("set" + name, new Class<?>[]{type});
                    PropertyEditor editor = PropertyEditorManager.findEditor(type);

                    if (editor == null)
                        throw new ResourceException("No property editor found for type: " + type);

                    editor.setAsText(value);
                    setter.invoke(xads, new Object[]{editor.getValue()});
                }
            } catch (InstantiationException ie) {
                throw new ResourceException("Could not create an XADataSource: ", ie);
            } catch (IllegalAccessException iae) {
                throw new ResourceException("Could not set a property: ", iae);
            } catch (IllegalArgumentException iae) {
                throw new ResourceException("Could not set a property: ", iae);
            } catch (InvocationTargetException ite) {
                throw new ResourceException("Could not invoke setter on XADataSource: ", ite);
            } catch (NoSuchMethodException nsme) {
                throw new ResourceException("Could not find accessor on XADataSource: ", nsme);
            }
        }
        return xads;
    }

    /**
     * Get the XA properties
     *
     * @return The value
     */
    protected Properties getXaProps() {
        return xaProps;
    }

    /**
     * Default implementation
     */
    public static class XADataSelector implements URLSelectorStrategy {
        private final List<XAData> xaDataList;
        private int xaDataIndex;
        private XAData xaData;

        /**
         * Constructor
         *
         * @param xaDataList The XAData instances
         */
        public XADataSelector(List<XAData> xaDataList) {
            if (xaDataList == null || xaDataList.size() == 0) {
                throw new IllegalStateException("Expected non-empty list of XADataSource/URL pairs but got: " + xaDataList);
            }

            this.xaDataList = xaDataList;
        }

        /**
         * Get the XAData
         *
         * @return The value
         */
        public synchronized XAData getXAData() {
            if (xaData == null) {
                if (xaDataIndex == xaDataList.size()) {
                    xaDataIndex = 0;
                }

                xaData = xaDataList.get(xaDataIndex++);
            }

            return xaData;
        }

        /**
         * Fail XAData
         *
         * @param xads The value
         */
        public synchronized void failedXAData(XAData xads) {
            if (xads.equals(this.xaData)) {
                this.xaData = null;
            }
        }

        /**
         * Get all the custom URL objects
         *
         * @return The value
         */
        public List<XAData> getCustomSortedUrls() {
            return xaDataList;
        }

        /**
         * Fail an URL objects
         *
         * @param urlObject The value
         */
        public void failedUrlObject(Object urlObject) {
            failedXAData((XAData) urlObject);
        }

        /**
         * Get all the URL objects
         *
         * @return The value
         */
        public List<XAData> getAllUrlObjects() {
            return xaDataList;
        }

        /**
         * Get the URL object
         *
         * @return The value
         */
        public Object getUrlObject() {
            return getXAData();
        }
    }

    private static class XAData {
        private final XADataSource xads;
        private final String url;

        public XAData(final XADataSource xads, final String url) {
            this.xads = xads;
            this.url = url;
        }

        public XADataSource getXads() {
            return xads;
        }

        public String getUrl() {
            return url;
        }

        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }

            if (!(o instanceof XAData)) {
                return false;
            }

            final XAData xaData = (XAData) o;

            if (!url.equals(xaData.url)) {
                return false;
            }

            return true;
        }

        public int hashCode() {
            return url.hashCode();
        }

        public String toString() {
            return "[XA URL=" + url + "]";
        }
    }
}

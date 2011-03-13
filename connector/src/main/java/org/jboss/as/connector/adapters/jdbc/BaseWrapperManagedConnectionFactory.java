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

package org.jboss.as.connector.adapters.jdbc;

import org.jboss.as.connector.adapters.jdbc.extensions.novendor.NullExceptionSorter;
import org.jboss.as.connector.adapters.jdbc.extensions.novendor.NullStaleConnectionChecker;
import org.jboss.as.connector.adapters.jdbc.extensions.novendor.NullValidConnectionChecker;
import org.jboss.as.connector.adapters.jdbc.spi.ExceptionSorter;
import org.jboss.as.connector.adapters.jdbc.spi.StaleConnectionChecker;
import org.jboss.as.connector.adapters.jdbc.spi.URLSelectorStrategy;
import org.jboss.as.connector.adapters.jdbc.spi.ValidConnectionChecker;
import org.jboss.as.connector.adapters.jdbc.util.Injection;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import javax.resource.ResourceException;
import javax.resource.spi.ConnectionManager;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.ManagedConnectionFactory;
import javax.resource.spi.ValidatingManagedConnectionFactory;
import javax.resource.spi.security.PasswordCredential;
import javax.security.auth.Subject;

import org.jboss.logging.Logger;

/**
 * BaseWrapperManagedConnectionFactory
 *
 * @author <a href="mailto:d_jencks@users.sourceforge.net">David Jencks</a>
 * @author <a href="mailto:adrian@jboss.com">Adrian Brock</a>
 * @author <a href="mailto:weston.price@jboss.com">Weston Price</a>
 * @author <a href="mailto:jesper.pedersen@jboss.org">Jesper Pedersen</a>
 * @version $Revision: 105426 $
 */

public abstract class BaseWrapperManagedConnectionFactory
        implements ManagedConnectionFactory, ValidatingManagedConnectionFactory, Serializable {
    /**
     * @since 4.0.1
     */
    static final long serialVersionUID = -84923705377702088L;

    /**
     * Track statements - false
     */
    public static final int TRACK_STATEMENTS_FALSE_INT = 0;

    /**
     * Track statements - true
     */
    public static final int TRACK_STATEMENTS_TRUE_INT = 1;

    /**
     * Track statements - no warning
     */
    public static final int TRACK_STATEMENTS_NOWARN_INT = 2;

    /**
     * Track statements - false
     */
    public static final String TRACK_STATEMENTS_FALSE = "false";

    /**
     * Track statements - true
     */
    public static final String TRACK_STATEMENTS_TRUE = "true";

    /**
     * Track statements - no warning
     */
    public static final String TRACK_STATEMENTS_NOWARN = "nowarn";

    /**
     * The logger
     */
    protected final Logger log = Logger.getLogger(getClass());

    /**
     * The print writer
     */
    private PrintWriter printWriter;

    /**
     * The user name
     */
    protected String userName;

    /**
     * The password
     */
    protected String password;

    /**
     * This is used by Local wrapper for all properties, and is left
     * in this class for ease of writing getConnectionProperties,
     * which always holds the user/pw.
     */
    protected final Properties connectionProps = new Properties();

    /**
     * The transaction isolation level
     */
    protected int transactionIsolation = -1;

    /**
     * The prepared statement cache size
     */
    protected Integer preparedStatementCacheSize = Integer.valueOf(0);

    /**
     * Query timeout enabled
     */
    protected boolean doQueryTimeout = false;

    /**
     * The variable <code>newConnectionSQL</code> holds an SQL
     * statement which if not null is executed when a new Connection is
     * obtained for a new ManagedConnection.
     */
    protected String newConnectionSQL;

    /**
     * The variable <code>checkValidConnectionSQL</code> holds an sql
     * statement that may be executed whenever a managed connection is
     * removed from the pool, to check that it is still valid.  This
     * requires setting up an mbean to execute it when notified by the
     * ConnectionManager.
     */
    protected String checkValidConnectionSQL;

    /**
     * The classname used to check whether a connection is valid
     */
    protected String validConnectionCheckerClassName;

    private String validConnectionCheckerProperties;

    /**
     * The properties injected in the class used to check whether a connection is valid
     */
    protected final Properties validConnectionCheckerProps = new Properties();

    /**
     * The instance of the valid connection checker
     */
    protected ValidConnectionChecker connectionChecker;


    /**
     * The instance of the stale connection checker
     */
    protected StaleConnectionChecker staleConnectionChecker;

    /**
     * The staleConnectionCheckerClassName
     */
    private String staleConnectionCheckerClassName;

    private String staleConnectionCheckerProperties;

    /**
     * The properties injected in the stale connection checker
     */
    protected final Properties staleConnectionCheckerProps = new Properties();

    private String exceptionSorterClassName;

    private String exceptionSorterProperties;

    private final Properties exceptionSorterProps = new Properties();

    private ExceptionSorter exceptionSorter;

    /**
     * Track statement
     */
    protected int trackStatements = TRACK_STATEMENTS_NOWARN_INT;

    /**
     * Whether to share cached prepared statements
     */
    protected Boolean sharePS = Boolean.FALSE;

    /**
     * Transaction query timeout
     */
    protected Boolean isTransactionQueryTimeout = Boolean.FALSE;

    /**
     * Query timeout
     */
    protected Integer queryTimeout = Integer.valueOf(0);

    /**
     * The variable <code>urlDelimiter</code> holds the url delimiter
     * information to be used for HA DS configuration .
     */
    protected String urlDelimiter;

    /**
     * URL selector strategy class name
     */
    protected String urlSelectorStrategyClassName;

    private URLSelectorStrategy urlSelectorStrategy;

    private Boolean validateOnMatch = Boolean.TRUE;

    /**
     * Whether to use a try lock
     */
    private Integer useTryLock = Integer.valueOf(60);

    /**
     * Spy functionality
     */
    private Boolean spy = Boolean.FALSE;

    /**
     * JNDI name
     */
    private String jndiName;

    /**
     * Constructor
     */
    public BaseWrapperManagedConnectionFactory() {
    }

    /**
     * {@inheritDoc}
     */
    public PrintWriter getLogWriter() throws ResourceException {
        return printWriter;
    }

    /**
     * {@inheritDoc}
     */
    public void setLogWriter(PrintWriter v) throws ResourceException {
        this.printWriter = v;
    }

    /**
     * {@inheritDoc}
     */
    public Object createConnectionFactory(ConnectionManager cm) throws ResourceException {
        return new WrapperDataSource(this, cm);
    }

    /**
     * {@inheritDoc}
     */
    public Object createConnectionFactory() throws ResourceException {
        throw new ResourceException("Resource Adapter does not currently support running in a non-managed environment.");
    }

    /**
     * Get the user name
     *
     * @return The value
     */
    public String getUserName() {
        return userName;
    }

    /**
     * Set the user name
     *
     * @param userName The value
     */
    public void setUserName(final String userName) {
        this.userName = userName;
    }

    /**
     * Get the password
     *
     * @return The value
     */
    public String getPassword() {
        return password;
    }

    /**
     * Set the password
     *
     * @param password The value
     */
    public void setPassword(final String password) {
        this.password = password;
    }

    /**
     * Get the prepared statement cache size
     *
     * @return The value
     */
    public Integer getPreparedStatementCacheSize() {
        return preparedStatementCacheSize;
    }

    /**
     * Set the prepared statement cache size
     *
     * @param size The value
     */
    public void setPreparedStatementCacheSize(Integer size) {
        if (size != null)
            preparedStatementCacheSize = size;
    }

    /**
     * Get the prepared statement share status
     *
     * @return The value
     */
    public Boolean getSharePreparedStatements() {
        return sharePS;
    }

    /**
     * Set the prepared statement share status
     *
     * @param sharePS The value
     */
    public void setSharePreparedStatements(Boolean sharePS) {
        if (sharePS != null)
            this.sharePS = sharePS;
    }

    /**
     * Get the transaction isolation level
     *
     * @return The value
     */
    public String getTransactionIsolation() {
        switch (this.transactionIsolation) {
            case Connection.TRANSACTION_NONE:
                return "TRANSACTION_NONE";
            case Connection.TRANSACTION_READ_COMMITTED:
                return "TRANSACTION_READ_COMMITTED";
            case Connection.TRANSACTION_READ_UNCOMMITTED:
                return "TRANSACTION_READ_UNCOMMITTED";
            case Connection.TRANSACTION_REPEATABLE_READ:
                return "TRANSACTION_REPEATABLE_READ";
            case Connection.TRANSACTION_SERIALIZABLE:
                return "TRANSACTION_SERIALIZABLE";
            case -1:
                return "DEFAULT";
            default:
                return Integer.toString(transactionIsolation);
        }
    }

    /**
     * Set the transaction isolation level
     *
     * @param transactionIsolation The value
     */
    public void setTransactionIsolation(String transactionIsolation) {
        if (transactionIsolation.equals("TRANSACTION_NONE")) {
            this.transactionIsolation = Connection.TRANSACTION_NONE;
        } else if (transactionIsolation.equals("TRANSACTION_READ_COMMITTED")) {
            this.transactionIsolation = Connection.TRANSACTION_READ_COMMITTED;
        } else if (transactionIsolation.equals("TRANSACTION_READ_UNCOMMITTED")) {
            this.transactionIsolation = Connection.TRANSACTION_READ_UNCOMMITTED;
        } else if (transactionIsolation.equals("TRANSACTION_REPEATABLE_READ")) {
            this.transactionIsolation = Connection.TRANSACTION_REPEATABLE_READ;
        } else if (transactionIsolation.equals("TRANSACTION_SERIALIZABLE")) {
            this.transactionIsolation = Connection.TRANSACTION_SERIALIZABLE;
        } else {
            try {
                this.transactionIsolation = Integer.parseInt(transactionIsolation);
            } catch (NumberFormatException nfe) {
                throw new IllegalArgumentException("Setting Isolation level to unknown state: " + transactionIsolation);
            }
        }
    }

    /**
     * Get the new connection SQL statement
     *
     * @return The value
     */
    public String getNewConnectionSQL() {
        return newConnectionSQL;
    }

    /**
     * Set the new connection SQL statement
     *
     * @param newConnectionSQL The value
     */
    public void setNewConnectionSQL(String newConnectionSQL) {
        this.newConnectionSQL = newConnectionSQL;
    }

    /**
     * Get the check valid connection SQL statement
     *
     * @return The value
     */
    public String getCheckValidConnectionSQL() {
        return checkValidConnectionSQL;
    }

    /**
     * Set the check valid connection SQL statement
     *
     * @param checkValidConnectionSQL The value
     */
    public void setCheckValidConnectionSQL(String checkValidConnectionSQL) {
        this.checkValidConnectionSQL = checkValidConnectionSQL;
    }

    /**
     * Get the stale connection checker class name
     *
     * @return The value
     */
    public String getStaleConnectionCheckerClassName() {
        return staleConnectionCheckerClassName;
    }

    /**
     * Set the stale connection checker class name
     *
     * @param value The value
     */
    public void setStaleConnectionCheckerClassName(String value) {
        staleConnectionCheckerClassName = value;
    }

    /**
     * Get the track statement value
     *
     * @return The value
     */
    public String getTrackStatements() {
        if (trackStatements == TRACK_STATEMENTS_FALSE_INT) {
            return TRACK_STATEMENTS_FALSE;
        } else if (trackStatements == TRACK_STATEMENTS_TRUE_INT) {
            return TRACK_STATEMENTS_TRUE;
        }

        return TRACK_STATEMENTS_NOWARN;
    }

    /**
     * Set the track statement value
     *
     * @param value The value
     */
    public void setTrackStatements(String value) {
        if (value == null)
            throw new IllegalArgumentException("Null value for trackStatements");

        String trimmed = value.trim();

        if (trimmed.equalsIgnoreCase(TRACK_STATEMENTS_FALSE)) {
            trackStatements = TRACK_STATEMENTS_FALSE_INT;
        } else if (trimmed.equalsIgnoreCase(TRACK_STATEMENTS_TRUE)) {
            trackStatements = TRACK_STATEMENTS_TRUE_INT;
        } else {
            trackStatements = TRACK_STATEMENTS_NOWARN_INT;
        }
    }

    /**
     * Get the validate on match value
     *
     * @return The value
     */
    public Boolean getValidateOnMatch() {
        return this.validateOnMatch;
    }

    /**
     * Set the validate on match value
     *
     * @param validateOnMatch The value
     */
    public void setValidateOnMatch(Boolean validateOnMatch) {
        if (validateOnMatch != null)
            this.validateOnMatch = validateOnMatch;
    }

    /**
     * Get the exception sorter class name
     *
     * @return The value
     */
    public String getExceptionSorterClassName() {
        return exceptionSorterClassName;
    }

    /**
     * Set the exception sorter class name
     *
     * @param exceptionSorterClassName The value
     */
    public void setExceptionSorterClassName(String exceptionSorterClassName) {
        this.exceptionSorterClassName = exceptionSorterClassName;
    }

    /**
     * Get the valid connection checker class name
     *
     * @return The value
     */
    public String getValidConnectionCheckerClassName() {
        return validConnectionCheckerClassName;
    }

    /**
     * Set the valid connection checker class name
     *
     * @param value The value
     */
    public void setValidConnectionCheckerClassName(String value) {
        validConnectionCheckerClassName = value;
    }


    /**
     * Is transaction query timeout set
     *
     * @return The value
     */
    public Boolean isTransactionQueryTimeout() {
        return isTransactionQueryTimeout;
    }

    /**
     * Set transaction query timeout
     *
     * @param value The value
     */
    public void setTransactionQueryTimeout(Boolean value) {
        if (value != null)
            isTransactionQueryTimeout = value;
    }

    /**
     * Get the query timeout
     *
     * @return The value
     */
    public Integer getQueryTimeout() {
        return queryTimeout;
    }

    /**
     * Set the query timeout
     *
     * @param timeout The value
     */
    public void setQueryTimeout(Integer timeout) {
        if (timeout != null)
            queryTimeout = timeout;
    }

    /**
     * Get the use try lock value
     *
     * @return The value
     */
    public Integer getUseTryLock() {
        return useTryLock;
    }

    /**
     * Set the use try lock value
     *
     * @param useTryLock The value
     */
    public void setUseTryLock(Integer useTryLock) {
        if (useTryLock != null)
            this.useTryLock = useTryLock;
    }

    /**
     * Get the url delimiter
     *
     * @return The value
     */
    public String getURLDelimiter() {
        return urlDelimiter;
    }

    /**
     * Set the spy value
     *
     * @param v The value
     */
    public void setSpy(Boolean v) {
        if (v != null)
            this.spy = v;
    }

    /**
     * Get the spy value
     *
     * @return The value
     */
    public Boolean getSpy() {
        return spy;
    }

    /**
     * Set the jndi name value
     *
     * @param v The value
     */
    public void setJndiName(String v) {
        if (v != null)
            this.jndiName = v;
    }

    /**
     * Get the jndi name value
     *
     * @return The value
     */
    public String getJndiName() {
        return jndiName;
    }

    /**
     * Set the url delimiter.
     *
     * @param urlDelimiter The value
     * @throws ResourceException Thrown in case of an error
     */
    public void setURLDelimiter(String urlDelimiter) throws ResourceException {
        this.urlDelimiter = urlDelimiter;
    }

    /**
     * Get the url selector strategy class name
     *
     * @return The value
     */
    public String getUrlSelectorStrategyClassName() {
        return urlSelectorStrategyClassName;
    }

    /**
     * Set the url selector strategy class name
     *
     * @param urlSelectorStrategyClassName The value
     */
    public void setUrlSelectorStrategyClassName(String urlSelectorStrategyClassName) {
        this.urlSelectorStrategyClassName = urlSelectorStrategyClassName;
    }

    /**
     * Get the url selector strategy
     *
     * @return The value
     */
    public URLSelectorStrategy getUrlSelectorStrategy() {
        return urlSelectorStrategy;
    }

    /**
     * Load the URLSelectStrategy
     *
     * @param className            The class name
     * @param constructorParameter The parameter
     * @return The URL selector strategy
     */
    public Object loadClass(String className, Object constructorParameter) {
        Object result = null;
        try {
            Class<?> clazz = Thread.currentThread().getContextClassLoader().loadClass(className);
            Class<?>[] param = {java.util.List.class};
            Constructor<?> cnstructor = clazz.getDeclaredConstructor(param);
            Object[] consParameter = {constructorParameter};
            result = cnstructor.newInstance(consParameter);
        } catch (ClassNotFoundException cnfe) {
            log.error("Class not found for URLSelectStrategy :" + className);
        } catch (InstantiationException ie) {
            log.error("Could not instantiate URLSelectorStrategy type :" + className);
        } catch (IllegalAccessException iae) {
            log.error("Check for the constructor with List parameter for URLSelectStrategy class as " + className);
        } catch (InvocationTargetException ite) {
            log.error("Constructor Invocation failing for URLSelectorStrategy " + className);
        } catch (NoSuchMethodException nsme) {
            log.error("Constructor or Method mismatch in URLSelectorStrategy :" + className);
        }

        return result;
    }

    /**
     * Get the invalid connections
     *
     * @param connectionSet The connection set
     * @return The invalid connections
     * @throws ResourceException Thrown if an error occurs
     */
    @SuppressWarnings("rawtypes")
    public Set<BaseWrapperManagedConnection> getInvalidConnections(final Set connectionSet) throws ResourceException {
        final Set<BaseWrapperManagedConnection> invalid = new HashSet<BaseWrapperManagedConnection>();

        for (Iterator<?> iter = connectionSet.iterator(); iter.hasNext();) {
            final Object anonymous = iter.next();

            if (anonymous instanceof BaseWrapperManagedConnection) {
                BaseWrapperManagedConnection mc = (BaseWrapperManagedConnection) anonymous;

                if (!mc.checkValid()) {
                    invalid.add(mc);
                }
            }
        }

        return invalid;
    }


    /**
     * Gets full set of connection properties, i.e. whatever is provided
     * in config plus "user" and "password" from subject/cri.
     * <p/>
     * <p>Note that the set is used to match connections to datasources as well
     * as to create new managed connections.
     * <p/>
     * <p>In fact, we have a problem here. Theoretically, there is a possible
     * name collision between config properties and "user"/"password".
     *
     * @param subject The subject
     * @param cri     The connection request info
     * @return The properties
     * @throws ResourceException Thrown if an error occurs
     */
    protected Properties getConnectionProperties(Subject subject, ConnectionRequestInfo cri)
            throws ResourceException {
        if (cri != null && cri.getClass() != WrappedConnectionRequestInfo.class)
            throw new ResourceException("Wrong kind of ConnectionRequestInfo: " + cri.getClass());

        Properties props = new Properties();
        props.putAll(connectionProps);
        if (subject != null) {
            if (SubjectActions.addMatchingProperties(subject, props, this))
                return props;

            throw new ResourceException("No matching credentials in Subject!");
        }

        WrappedConnectionRequestInfo lcri = (WrappedConnectionRequestInfo) cri;

        if (lcri != null) {
            props.setProperty("user", (lcri.getUserName() == null) ? "" : lcri.getUserName());
            props.setProperty("password", (lcri.getPassword() == null) ? "" : lcri.getPassword());
            return props;
        }

        if (userName != null) {
            props.setProperty("user", userName);
            props.setProperty("password", (password == null) ? "" : password);
        }

        return props;
    }

    /**
     * Is the exception fatal
     *
     * @param e The exception
     * @return True if fatal; otherwise false
     */
    boolean isExceptionFatal(SQLException e) {
        try {
            if (exceptionSorter != null)
                return exceptionSorter.isExceptionFatal(e);

            if (exceptionSorterClassName != null) {
                try {
                    ClassLoader cl = Thread.currentThread().getContextClassLoader();
                    Class<?> clazz = cl.loadClass(exceptionSorterClassName);
                    exceptionSorter = (ExceptionSorter) clazz.newInstance();
                    Injection injection = new Injection();
                    for (Entry<Object, Object> prop : exceptionSorterProps.entrySet()) {
                        injection.inject(exceptionSorter, (String) prop.getKey(), (String) prop.getValue());
                    }
                    return exceptionSorter.isExceptionFatal(e);
                } catch (Exception e2) {
                    log.warn("exception trying to create exception sorter (disabling):", e2);
                    exceptionSorter = new NullExceptionSorter();
                }
            }
        } catch (Throwable t) {
            log.warn("Error checking exception fatality: ", t);
        }
        return false;
    }

    /**
     * Is the connection valid
     *
     * @param c The connection
     * @return <code>null</code> if valid; otherwise the exception
     */
    SQLException isValidConnection(Connection c) {
        // Already got a checker
        if (connectionChecker != null)
            return connectionChecker.isValidConnection(c);

        // Class specified
        if (validConnectionCheckerClassName != null) {
            try {
                ClassLoader cl = Thread.currentThread().getContextClassLoader();
                Class<?> clazz = cl.loadClass(validConnectionCheckerClassName);
                connectionChecker = (ValidConnectionChecker) clazz.newInstance();
                Injection injection = new Injection();
                for (Entry<Object, Object> prop : validConnectionCheckerProps.entrySet()) {
                    injection.inject(connectionChecker, (String) prop.getKey(), (String) prop.getValue());
                }
                return connectionChecker.isValidConnection(c);
            } catch (Exception e) {
                log.warn("Exception trying to create connection checker (disabling):", e);
                connectionChecker = new NullValidConnectionChecker();
            }
        }

        // SQL statement specified
        if (checkValidConnectionSQL != null) {
            connectionChecker = new CheckValidConnectionSQL(checkValidConnectionSQL);
            return connectionChecker.isValidConnection(c);
        }

        // No Check
        return null;
    }


    /**
     * Is the connection stale
     *
     * @param e The exception
     * @return <code>True</code> if stale; otherwise false
     */
    boolean isStaleConnection(SQLException e) {
        if (staleConnectionChecker != null)
            return staleConnectionChecker.isStaleConnection(e);

        if (staleConnectionCheckerClassName != null) {
            try {
                ClassLoader cl = Thread.currentThread().getContextClassLoader();
                Class<?> clazz = cl.loadClass(staleConnectionCheckerClassName);
                staleConnectionChecker = (StaleConnectionChecker) clazz.newInstance();
                Injection injection = new Injection();
                for (Entry<Object, Object> prop : staleConnectionCheckerProps.entrySet()) {
                    injection.inject(staleConnectionChecker, (String) prop.getKey(), (String) prop.getValue());
                }
                return staleConnectionChecker.isStaleConnection(e);
            } catch (Exception ex2) {
                log.warn("exception trying to create stale connection checker (disabling) " +
                        staleConnectionCheckerClassName, ex2);

                staleConnectionChecker = new NullStaleConnectionChecker();
            }
        }

        return false;
    }

    /**
     * SubjectActions
     */
    static class SubjectActions implements PrivilegedAction<Boolean> {
        private final Subject subject;

        private final Properties props;

        private final ManagedConnectionFactory mcf;

        /**
         * Constructor
         *
         * @param subject The subject
         * @param props   The properties
         * @param mcf     The managed connection factory
         */
        SubjectActions(Subject subject, Properties props, ManagedConnectionFactory mcf) {
            this.subject = subject;
            this.props = props;
            this.mcf = mcf;
        }

        /**
         * Run
         *
         * @return The result
         */
        public Boolean run() {
            Iterator<?> i = subject.getPrivateCredentials().iterator();
            while (i.hasNext()) {
                Object o = i.next();
                if (o instanceof PasswordCredential) {
                    PasswordCredential cred = (PasswordCredential) o;
                    if (cred.getManagedConnectionFactory().equals(mcf)) {
                        props.setProperty("user", (cred.getUserName() == null) ? "" : cred.getUserName());

                        if (cred.getPassword() != null)
                            props.setProperty("password", new String(cred.getPassword()));

                        return Boolean.TRUE;
                    }
                }
            }
            return Boolean.FALSE;
        }

        /**
         * Add matching properties
         *
         * @param subject The subject
         * @param props   The properties
         * @param mcf     The managed connection factory
         * @return The result
         */
        static boolean addMatchingProperties(Subject subject, Properties props, ManagedConnectionFactory mcf) {
            SubjectActions action = new SubjectActions(subject, props, mcf);
            Boolean matched = AccessController.doPrivileged(action);
            return matched.booleanValue();
        }
    }

    /**
     * Get the validConnectionCheckerProps.
     *
     * @return the validConnectionCheckerProps.
     */
    public final Properties getValidConnectionCheckerProps() {
        return validConnectionCheckerProps;
    }

    /**
     * Get the staleConnectionCheckerProps.
     *
     * @return the staleConnectionCheckerProps.
     */
    public final Properties getStaleConnectionCheckerProps() {
        return staleConnectionCheckerProps;
    }

    /**
     * Get the exceptionSorterProps.
     *
     * @return the exceptionSorterProps.
     */
    public final Properties getExceptionSorterProps() {
        return exceptionSorterProps;
    }

    /**
     * Get the validConnectionCheckerProperties.
     *
     * @return the validConnectionCheckerProperties.
     */
    public final String getValidConnectionCheckerProperties() {
        return validConnectionCheckerProperties;
    }

    /**
     * Set the validConnectionCheckerProperties.
     *
     * @param validConnectionCheckerProperties
     *         The validConnectionCheckerProperties to set.
     */
    public final void setValidConnectionCheckerProperties(String validConnectionCheckerProperties) {
        this.validConnectionCheckerProperties = validConnectionCheckerProperties;
        validConnectionCheckerProps.clear();

        if (validConnectionCheckerProperties != null) {
            // Map any \ to \\
            validConnectionCheckerProperties = validConnectionCheckerProperties.replaceAll("\\\\", "\\\\\\\\");
            validConnectionCheckerProperties = validConnectionCheckerProperties.replaceAll(";", "\n");

            InputStream is = new ByteArrayInputStream(validConnectionCheckerProperties.getBytes());
            try {
                validConnectionCheckerProps.load(is);
            } catch (IOException ioe) {
                throw new RuntimeException("Could not load connection properties", ioe);
            }
        }
    }

    /**
     * Get the staleConnectionCheckerProperties.
     *
     * @return the staleConnectionCheckerProperties.
     */
    public final String getStaleConnectionCheckerProperties() {
        return staleConnectionCheckerProperties;
    }

    /**
     * Set the staleConnectionCheckerProperties.
     *
     * @param staleConnectionCheckerProperties
     *         The staleConnectionCheckerProperties to set.
     */
    public final void setStaleConnectionCheckerProperties(String staleConnectionCheckerProperties) {
        this.staleConnectionCheckerProperties = staleConnectionCheckerProperties;
        staleConnectionCheckerProps.clear();

        if (staleConnectionCheckerProperties != null) {
            // Map any \ to \\
            staleConnectionCheckerProperties = staleConnectionCheckerProperties.replaceAll("\\\\", "\\\\\\\\");
            staleConnectionCheckerProperties = staleConnectionCheckerProperties.replaceAll(";", "\n");

            InputStream is = new ByteArrayInputStream(staleConnectionCheckerProperties.getBytes());
            try {
                staleConnectionCheckerProps.load(is);
            } catch (IOException ioe) {
                throw new RuntimeException("Could not load connection properties", ioe);
            }
        }
    }

    /**
     * Get the exceptionSorterProperties.
     *
     * @return the exceptionSorterProperties.
     */
    public final String getExceptionSorterProperties() {
        return exceptionSorterProperties;
    }

    /**
     * Set the exceptionSorterProperties.
     *
     * @param exceptionSorterProperties The exceptionSorterProperties to set.
     */
    public final void setExceptionSorterProperties(String exceptionSorterProperties) {
        this.exceptionSorterProperties = exceptionSorterProperties;
        exceptionSorterProps.clear();

        if (exceptionSorterProperties != null) {
            // Map any \ to \\
            exceptionSorterProperties = exceptionSorterProperties.replaceAll("\\\\", "\\\\\\\\");
            exceptionSorterProperties = exceptionSorterProperties.replaceAll(";", "\n");

            InputStream is = new ByteArrayInputStream(exceptionSorterProperties.getBytes());
            try {
                exceptionSorterProps.load(is);
            } catch (IOException ioe) {
                throw new RuntimeException("Could not load connection properties", ioe);
            }
        }
    }
}

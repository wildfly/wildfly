/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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
package org.jboss.as.domain.management.connections.database;

import static org.jboss.as.domain.management.DomainManagementLogger.ROOT_LOGGER;
import static org.jboss.as.domain.management.DomainManagementMessages.MESSAGES;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Properties;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;

/**
 * The Database connection pool is responsible for maintain a pool of connections to a database.
 * <p>
 * The class has two constructors
 * <ul>
 * <li>Constructor takes a list of parameters and create pool of minimum connections {@code minPoolSize}
 * this make sure a minimum size of connections always is created. The {@code maxPoolSize} make sure the pool
 * can grow larger then the specified size, and in case the capacity is exceed, a requested will wait for an
 * existing connection to free up or throw an {@code IllegalStateException} on timeout. </li>
 * <li>Constructor takes a {@code datasource} as parameter. In this case the functionality for maintain the pool
 * is disable and maintain by the data source provider</li>
 * </ul>
 *</p>
 *<p>
 *For acquire a new connection use the {@code getConnection()} this return a connection from the pool, otherwise
 *it will create a new connection. Use the {@code returnConnection(DatabaseConnection)} for returning a connection
 *to the pool.
 *
 *If a connection is not returned or staled the reaper thread will clean it up after a certain time. The connection idle time
 *can be control by calling the {@code setConnectionIdleTime(milliseconds)}
 *</p>
 *
 * @author <a href="mailto:flemming.harms@gmail.com">Flemming Harms</a>
 */
public class DatabaseConnectionPool {

    public static final long MAX_WAIT_FOR_LOCK = 60000;
    public static final long MAX_IDLE_TIME_CONNECTIONS = 60000;
    public static final long MAX_DELAY_FOR_REPEAER_REQUEST = 300000;

    private final ArrayDeque<DatabaseConnection> connectionPool;
    private final String url, user, password;
    private long timeout = 60000;
    private final ConnectionReaper reaper;
    private final int maxPoolSize;
    private final int minPoolSize;
    private final DataSource ds;
    private final Semaphore available;
    private final String driver;
    private final String module;
    private volatile long connectionidletime;
    private Driver driverClass;

    /**
     * @param module - the name of the jdbc driver module it should depend on
     * @param driver - database driver class
     * @param url - database connection url
     * @param user - database user
     * @param password - database password
     * @param minPoolSize - minimum database connections that exist
     * @param maxPoolSize - maximum database connections that exist
     * @throws SQLException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws ClassNotFoundException
     * @throws InterruptedException
     * @throws IllegalArgumentException
     * @throws ModuleLoadException
     * @throws SecurityException
     * @throws NoSuchMethodException
     * @throws InvocationTargetException
     */
    public DatabaseConnectionPool(String module, String driver,String url, String user, String password, int minPoolSize, int maxPoolSize) throws SQLException, InstantiationException, IllegalAccessException, ClassNotFoundException, InterruptedException, IllegalArgumentException, ModuleLoadException, SecurityException, NoSuchMethodException, InvocationTargetException {
        this.url = url;
        this.user = user;
        this.password = password;
        this.minPoolSize = minPoolSize;
        this.maxPoolSize = maxPoolSize;
        this.timeout = MAX_WAIT_FOR_LOCK;
        this.connectionidletime = MAX_IDLE_TIME_CONNECTIONS;
        this.available = new Semaphore(this.maxPoolSize, true);
        this.driver = driver;
        this.module = module;
        connectionPool = new ArrayDeque<DatabaseConnection>(this.maxPoolSize);
        reaper = new ConnectionReaper(this,MAX_DELAY_FOR_REPEAER_REQUEST);
        ds = null;
        init();
    }

    public DatabaseConnectionPool(DataSource dataSource) {
        this.url = null;
        this.user = null;
        this.driver = "";
        this.module = "";
        this.password = null;
        this.minPoolSize = 0;
        this.maxPoolSize = 0;
        this.connectionidletime = 0;
        this.connectionPool = new ArrayDeque<DatabaseConnection>(0);
        this.available = new Semaphore(this.maxPoolSize, true);
        reaper = new ConnectionReaper(this,0);
        ds = dataSource;
    }

    protected void init() throws SQLException, InterruptedException, InstantiationException, IllegalAccessException, ClassNotFoundException, IllegalArgumentException, ModuleLoadException, SecurityException, NoSuchMethodException, InvocationTargetException {
        reaper.start();
        driverClass = getDriver(module, driver);

        if (minPoolSize > 0) {
            for (int i = 0; i < minPoolSize; i++) {
                available.acquire();
                Connection conn = getJdbcConnection();
                DatabaseConnection c = new DatabaseConnection(conn, this);
                getConnectionPool().add(c);
            }
        }
    }

    private ArrayDeque<DatabaseConnection> getConnectionPool() {
        return connectionPool;
    }

    private Connection getJdbcConnection() throws SQLException {
        Properties connectionProperties = new Properties();
        connectionProperties.put("user", user);
        connectionProperties.put("password", password);
        Connection conn = driverClass.connect(url, connectionProperties);
        return conn;
    }

    protected Driver getDriver(String module, String driverClassName) throws ModuleLoadException, ClassNotFoundException, NoSuchMethodException,
            InstantiationException, IllegalAccessException, InvocationTargetException {
        ModuleIdentifier moduleId = ModuleIdentifier.create(module);
        Module loadModule = Module.getCallerModuleLoader().loadModule(moduleId);

        final Class<? extends Driver> driverClass = loadModule.getClassLoader().loadClass(driverClassName).asSubclass(Driver.class);
        final Constructor<? extends Driver> constructor = driverClass.getConstructor();
        final Driver driver = constructor.newInstance();
        return driver;
    }


    public int getCurrentPoolSize() {
      synchronized (connectionPool) {
         return getConnectionPool().size();
      }
    }

    public void reapConnections() {
       synchronized (connectionPool) {
           long stale = System.currentTimeMillis() - getConnectionIdleTime();
           Iterator<DatabaseConnection> it = getConnectionPool().descendingIterator();
           if (ROOT_LOGGER.isTraceEnabled()) {
               ROOT_LOGGER.trace("Reaper started, look for ideal connections in the connection pool("+getCurrentPoolSize()+")");
           }
           while((it != null) && (it.hasNext())) {
               DatabaseConnection conn = it.next();
               try {
                   if((conn.inUse()) && (!conn.validate())) {
                       if (ROOT_LOGGER.isTraceEnabled()) {
                           ROOT_LOGGER.trace("Connection is not valid anymore, terminate connection");
                       }
                       closeConnection(it,conn);
                   } else if ((stale > conn.getLastUse()) && getConnectionPool().size() > minPoolSize) {
                        if (ROOT_LOGGER.isTraceEnabled()) {
                           ROOT_LOGGER.trace("Connection has been ideal for more then "+getConnectionIdleTime()+ " terminate connection");
                        }
                       closeConnection(it,conn);
                   }
               } catch (SQLException e) {
                   throw MESSAGES.reaperTerminationConnectionException(e);
               }
           }
       }
    }

    public void closeConnections() throws SQLException {
        synchronized (connectionPool) {
            for( Iterator< DatabaseConnection > it = getConnectionPool().iterator(); it.hasNext() ; ) {
               DatabaseConnection conn = it.next();
               closeConnection(it,conn);
            }
        }
    }

    private void closeConnection(Iterator<DatabaseConnection> it, DatabaseConnection conn) throws SQLException {
        if (ROOT_LOGGER.isTraceEnabled()) {
          ROOT_LOGGER.trace("Close database connection");
        }
        it.remove();
        try {
            conn.close();
            conn.terminateConnection();
        } finally {
            available.release();
        }
    }


    public Connection getConnection() throws SQLException, InterruptedException {
        if (ds == null) {
            DatabaseConnection dbConenction;
            synchronized (connectionPool) {
                if (ROOT_LOGGER.isTraceEnabled()) {
                    ROOT_LOGGER.trace("Looking for a free connection in the pool");
                }
                Iterator<DatabaseConnection> iterator = getConnectionPool().iterator();
                while (iterator.hasNext()) {
                    dbConenction = iterator.next();
                    if (dbConenction.lease()) {
                        return dbConenction;
                    }
                }

                if (ROOT_LOGGER.isTraceEnabled()) {
                    ROOT_LOGGER.trace("No connections available in the pool, acquiring a new");
                }

                if (available.tryAcquire(getTimeout(), TimeUnit.MILLISECONDS)) {
                    Connection conn = getJdbcConnection();
                    dbConenction = new DatabaseConnection(conn, this);
                    dbConenction.lease();
                    getConnectionPool().addFirst(dbConenction);
                    if (ROOT_LOGGER.isTraceEnabled()) {
                        ROOT_LOGGER.trace("Connections acquired");
                    }
                } else {
                    throw new IllegalStateException("Fail to acquire lock before timeout, all connections are used");
                }
            }

            return dbConenction;
        } else {
            return ds.getConnection();
        }

   }

    public synchronized void returnConnection(DatabaseConnection conn) {
       conn.expireLease();
    }

    /**
     * @return timeout in milliseconds
     */
    public long getTimeout() {
        return this.timeout;
    }

    /**
     * Set the timeout in milliseconds for how long getConnection shall
     * wait for a lock before it throw an exception
     * @param timeout
     */
    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    /**
     * @param delay - sets the delay between each reaper execution
     */
    public void setReaperDelay(long delay) {
       reaper.setDelay(delay);
    }

    public long getReaperDelay() {
       return reaper.getDelay();
    }


    /**
     * force the reaper to run
     */
    public void forceReaper() {
       synchronized (reaper) {
           reaper.notify();
       }
    }

    /**
     * Terminate the reaper thread
     */
    public synchronized void terminateReaper() {
       reaper.terminate();
    }

    /**
     * @return get the max idle time for connection
     */
    public long getConnectionIdleTime() {
        return connectionidletime;
    }

    /**
     * Set how long a connection can be idle before it terminated by
     * the reaper.
     *
     * @param connectiontimeout in milliseconds
     */
    public void setConnectionIdleTime(long connectiontimeout) {
        this.connectionidletime = connectiontimeout;
    }
 }
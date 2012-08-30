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

import static org.jboss.as.domain.management.DomainManagementMessages.MESSAGES;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Vector;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import javax.naming.InitialContext;
import javax.naming.NamingException;
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
 * existing connection to free up or throw an {@code InterruptedException} on timeout. </li>
 * <li>Constructor takes a {@code datasource} as parameter. In this case the functionality for maintain the pool
 * is disable and maintain by the data source provider</li>
 * </ul>
 *</p>
 *<p>
 *For acquire a new connection use the {@code getConnection()} this return a connection from the pool, otherwise
 *it will create a new connection. Use the {@code returnConnection(DatabaseConnection)} for returning a connection
 *to the pool.
 *
 *If a connection is not returned or staled the reaper thread will clean it up after a certain time. The timeout can be
 *control either by passing into the constructor or call the {@code setTimeOut(milliseconds)}
 *</p>
 *
 * @author <a href="mailto:flemming.harms@gmail.com">Flemming Harms</a>
 */
public class DatabaseConnectionPool {

    private final Vector<DatabaseConnection> connections;
    private final String url, user, password;
    private long timeout = 60000;
    private final ConnectionReaper reaper;
    private final int maxPoolSize;
    private final int minPoolSize;
    private final DataSource ds;
    private final Semaphore available;
    private final String driver;
    private final String module;
    private long connectiontimeout;


    /**
     * @param module - the name of the jdbc driver module it should depend on
     * @param driver - database driver class
     * @param url - database connection url
     * @param user - database user
     * @param password - database password
     * @param minPoolSize - minimum database connections that exist
     * @param maxPoolSize - maximum database connections that exist
     * @param timeout - the maximum time in milliseconds to wait for a lock
     * @param connectiontimeout - the maximum time for connection can be ideal
     * @param delay - the maximum delay between each reaper task
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
    public DatabaseConnectionPool(String module, String driver,String url, String user, String password, int minPoolSize, int maxPoolSize, long timeout, long connectiontimeout, long delay) throws SQLException, InstantiationException, IllegalAccessException, ClassNotFoundException, InterruptedException, IllegalArgumentException, ModuleLoadException, SecurityException, NoSuchMethodException, InvocationTargetException {
        this.url = url;
        this.user = user;
        this.password = password;
        this.minPoolSize = minPoolSize;
        this.maxPoolSize = maxPoolSize;
        this.connectiontimeout = connectiontimeout;
        this.available = new Semaphore(this.maxPoolSize, true);
        this.timeout = timeout;
        this.driver = driver;
        this.module = module;
        connections = new Vector<DatabaseConnection>(this.maxPoolSize);
        reaper = new ConnectionReaper(this,delay);
        ds = null;
        init();
    }

    public DatabaseConnectionPool(String dataSource) throws NamingException {
        this.url = null;
        this.user = null;
        this.driver = "";
        this.module = "";
        this.password = null;
        this.minPoolSize = 0;
        this.maxPoolSize = 0;
        this.connectiontimeout = 0;
        this.connections = new Vector<DatabaseConnection>(0);
        this.available = new Semaphore(this.maxPoolSize, true);
        reaper = new ConnectionReaper(this,0);

        InitialContext ctx = new InitialContext();
        ds = (DataSource) ctx.lookup(dataSource);
    }

    private void init() throws SQLException, InterruptedException, InstantiationException, IllegalAccessException, ClassNotFoundException, IllegalArgumentException, ModuleLoadException, SecurityException, NoSuchMethodException, InvocationTargetException {
        reaper.start();
        final Driver driverClass = getDriver(module, driver);

        if (minPoolSize > 0) {
            for (int i = 0; i < minPoolSize; i++) {
                available.acquire();
                Properties connectionProperties = new Properties();
                connectionProperties.put("user", user);
                connectionProperties.put("password", password);
                Connection conn = driverClass.connect(url, connectionProperties);
                DatabaseConnection c = new DatabaseConnection(conn, this);
                connections.addElement(c);
            }
        }
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
        return connections.size();
    }

    public synchronized void reapConnections() {
       long stale = System.currentTimeMillis() - getConnectiontimeout();
       Enumeration<DatabaseConnection> connlist = connections.elements();

       while((connlist != null) && (connlist.hasMoreElements())) {
           DatabaseConnection conn = connlist.nextElement();
           if((conn.inUse()) && (stale > conn.getLastUse()) && (!conn.validate())) {
               removeConnection(conn);
            } else if ((!conn.inUse()) && (stale > conn.getLastUse()) && connections.size() > minPoolSize) {
                removeConnection(conn);
                try {
                    conn.terminateConnection();
                } catch (SQLException e) {
                    throw MESSAGES.reaperTerminationConnectionException(e);
                }
            }
       }
    }

    public synchronized void closeConnections() throws SQLException {
       Enumeration<DatabaseConnection> connlist = connections.elements();
       while((connlist != null) && (connlist.hasMoreElements())) {
           DatabaseConnection conn = connlist.nextElement();
           conn.close();
           conn.terminateConnection();
           removeConnection(conn);
       }
    }

    private synchronized void removeConnection(DatabaseConnection conn) {
        if (connections.removeElement(conn)) {
            available.release();
        }
    }


    public synchronized Connection getConnection() throws SQLException, InterruptedException {
        if (ds == null) {
            DatabaseConnection c;
            for(int i = 0; i < connections.size(); i++) {
                c = connections.elementAt(i);
                if (c.lease()) {
                   return c;
                }
            }
            if (available.tryAcquire(getTimeout(), TimeUnit.MILLISECONDS)) {
                Connection conn = DriverManager.getConnection(url, user, password);
                c = new DatabaseConnection(conn, this);
                c.lease();
                connections.addElement(c);
            } else {
                throw new InterruptedException("All connections are in used");
            }
           return c;
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
     * Set the timeout in milliseconds
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

    public long getConnectiontimeout() {
        return connectiontimeout;
    }

    public void setConnectiontimeout(long connectiontimeout) {
        this.connectiontimeout = connectiontimeout;
    }
 }
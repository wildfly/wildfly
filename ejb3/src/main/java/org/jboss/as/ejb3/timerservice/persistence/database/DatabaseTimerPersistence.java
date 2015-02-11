/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ejb3.timerservice.persistence.database;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.sql.DataSource;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;

import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.as.ejb3.timerservice.CalendarTimer;
import org.jboss.as.ejb3.timerservice.TimerImpl;
import org.jboss.as.ejb3.timerservice.TimerServiceImpl;
import org.jboss.as.ejb3.timerservice.TimerState;
import org.jboss.as.ejb3.timerservice.persistence.TimeoutMethod;
import org.jboss.as.ejb3.timerservice.persistence.TimerPersistence;
import org.jboss.as.naming.ManagedReference;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.marshalling.InputStreamByteInput;
import org.jboss.marshalling.Marshaller;
import org.jboss.marshalling.MarshallerFactory;
import org.jboss.marshalling.MarshallingConfiguration;
import org.jboss.marshalling.ModularClassResolver;
import org.jboss.marshalling.OutputStreamByteOutput;
import org.jboss.marshalling.Unmarshaller;
import org.jboss.marshalling.river.RiverMarshallerFactory;
import org.jboss.modules.ModuleLoader;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.util.Base64;

/**
 * <p>
 * Database timer persistence store.
 * </p>
 *
 * @author Stuart Douglas
 * @author Wolf-Dieter Fink
 */
public class DatabaseTimerPersistence implements TimerPersistence, Service<DatabaseTimerPersistence> {

    private final InjectedValue<ManagedReferenceFactory> dataSourceInjectedValue = new InjectedValue<ManagedReferenceFactory>();
    private final InjectedValue<ModuleLoader> moduleLoader = new InjectedValue<ModuleLoader>();
    private final Map<String, TimerChangeListener> changeListeners = Collections.synchronizedMap(new HashMap<String, TimerChangeListener>());

    private final InjectedValue<java.util.Timer> timerInjectedValue = new InjectedValue<java.util.Timer>();

    private final Map<String, Set<String>> knownTimerIds = new HashMap<>();

    /** Identifier for the database dialect to be used for the timer-sql.properties */
    private String database;
    /** List of extracted known dialects*/
    private final HashSet<String> databaseDialects = new HashSet<String>();
    private final String partition;
    /** Interval in millis to refresh the timers from the persistence store*/
    private final int refreshInterval;
    /** Flag whether this instance should execute persistent timers*/
    private final boolean allowExecution;
    private volatile ManagedReference managedReference;
    private volatile DataSource dataSource;
    private volatile Properties sql;
    private MarshallerFactory factory;
    private MarshallingConfiguration configuration;
    private RefreshTask refreshTask;

    /** Names for the different SQL commands stored in the properties*/
    private static final String CREATE_TABLE = "create-table";
    private static final String CREATE_TIMER = "create-timer";
    private static final String UPDATE_TIMER = "update-timer";
    private static final String LOAD_ALL_TIMERS = "load-all-timers";
    private static final String LOAD_TIMER = "load-timer";
    private static final String DELETE_TIMER = "delete-timer";
    private static final String UPDATE_RUNNING = "update-running";

    public DatabaseTimerPersistence(final String database, String partition, int refreshInterval, boolean allowExecution) {
        this.database = database;
        this.partition = partition;
        this.refreshInterval = refreshInterval;
        this.allowExecution = allowExecution;
    }

    @Override
    public void start(final StartContext context) throws StartException {

        factory = new RiverMarshallerFactory();
        configuration = new MarshallingConfiguration();
        configuration.setClassResolver(ModularClassResolver.getInstance(moduleLoader.getValue()));

        managedReference = dataSourceInjectedValue.getValue().getReference();
        dataSource = (DataSource) managedReference.getInstance();
        final InputStream stream = DatabaseTimerPersistence.class.getClassLoader().getResourceAsStream("timer-sql.properties");
        sql = new Properties();
        try {
            sql.load(stream);
        } catch (IOException e) {
            throw new StartException(e);
        } finally {
            safeClose(stream);
        }
        extractDialects();
        investigateDialect();
        checkDatabase();
        if (refreshInterval > 0) {
            refreshTask = new RefreshTask();
            timerInjectedValue.getValue().schedule(refreshTask, refreshInterval, refreshInterval);
        }
    }

    @Override
    public synchronized void stop(final StopContext context) {
        if (refreshTask != null) {
            refreshTask.cancel();
        }
        knownTimerIds.clear();
        managedReference.release();
        managedReference = null;
        dataSource = null;
    }

    /**
     * Read the properties from the timer-sql and extract the database dialects.
     */
    private void extractDialects() {
        for (Object prop : sql.keySet()) {
            int dot = ((String)prop).indexOf('.');
            if (dot > 0) {
                databaseDialects.add(((String)prop).substring(dot+1));
            }
        }
    }

    /**
     * Check the connection MetaData and driver name to guess which database dialect
     * to use.
     */
    private void investigateDialect() {
        Connection connection = null;

        if (database == null) {
            // no database dialect from configuration guessing from MetaData
            try {
                connection = dataSource.getConnection();
                DatabaseMetaData metaData = connection.getMetaData();
                String dbProduct = metaData.getDatabaseProductName();
                database = identifyDialect(dbProduct);

                if (database == null) {
                    EjbLogger.ROOT_LOGGER.debug("Attempting to guess on driver name.");
                    database = identifyDialect(metaData.getDriverName());
                }
            } catch (Exception e) {
                EjbLogger.ROOT_LOGGER.debug("Unable to read JDBC metadata.", e);
            } finally {
                safeClose(connection);
            }
            if (database == null) {
                EjbLogger.ROOT_LOGGER.jdbcDatabaseDialectDetectionFailed(databaseDialects.toString());
            } else {
                EjbLogger.ROOT_LOGGER.debugf("Detect database dialect as '%s'.  If this is incorrect, please specify the correct dialect using the 'database' attribute in your configuration.  Supported database dialect strings are %s", database, databaseDialects);
            }
        } else {
            EjbLogger.ROOT_LOGGER.debugf("Database dialect '%s' read from configuration", database);
        }
    }

    /**
     * Use the given name and check for different database types to have a unified identifier for the dialect
     *
     * @param name A database name or even a driver name which should include the database name
     * @return A unified dialect identifier
     */
    private String identifyDialect(String name) {
        String unified = null;

        if (name != null) {
            if (name.toLowerCase().contains("postgres")) {
               unified = "postgresql";
            } else if (name.toLowerCase().contains("mysql")) {
                unified = "mysql";
            } else if (name.toLowerCase().contains("db2")) {
                unified = "db2";
            } else if (name.toLowerCase().contains("hsql") || name.toLowerCase().contains("hypersonic")) {
                unified = "hsql";
            } else if (name.toLowerCase().contains("h2")) {
                unified = "h2";
            } else if (name.toLowerCase().contains("oracle")) {
                unified = "oracle";
            }else if (name.toLowerCase().contains("microsoft")) {
                unified = "mssql";
            }else if (name.toLowerCase().contains("jconnect")) {
                unified = "sybase";
            }
         }
        EjbLogger.ROOT_LOGGER.debugf("Check dialect for '%s', result is '%s'", name, unified);
        return unified;
    }

    /**
     * Checks whether the database transaction configuration is appropriate
     * and create the timer table if necessary.
     */
    private void checkDatabase() {
        String loadTimer = sql(LOAD_TIMER);
        Connection connection = null;
        Statement statement = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        try {
            //test for the existence of the table by running the load timer query
            connection = dataSource.getConnection();
            if (connection.getTransactionIsolation() < Connection.TRANSACTION_READ_COMMITTED) {
                EjbLogger.ROOT_LOGGER.wrongTransactionIsolationConfiguredForTimer();
            }
            preparedStatement = connection.prepareStatement(loadTimer);
            preparedStatement.setString(1, "NON-EXISTENT");
            preparedStatement.setString(2, "NON-EXISTENT");
            preparedStatement.setString(3, "NON-EXISTENT");
            resultSet = preparedStatement.executeQuery();
        } catch (SQLException e) {
            //the query failed, assume it is because the table does not exist
            if (connection != null) {
                try {
                    String createTable = sql(CREATE_TABLE);
                    String[] statements = createTable.split(";");
                    for (final String sql : statements) {
                        try {
                            statement = connection.createStatement();
                            statement.executeUpdate(sql);
                        } finally {
                            safeClose(statement);
                        }
                    }
                } catch (SQLException e1) {
                    EjbLogger.ROOT_LOGGER.couldNotCreateTable(e1);
                }
            } else {
                EjbLogger.ROOT_LOGGER.couldNotCreateTable(e);
            }
        } finally {
            safeClose(resultSet);
            safeClose(preparedStatement);
            safeClose(statement);
            safeClose(connection);
        }
    }

    private String sql(final String key) {
        if (database != null) {
            String result = sql.getProperty(key + "." + database);
            if (result != null) {
                return result;
            }
        }
        return sql.getProperty(key);
    }

    @Override
    public void addTimer(final TimerImpl timerEntity) {
        String createTimer = sql(CREATE_TIMER);
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            synchronized (this) {
                knownTimerIds.get(timerEntity.getTimedObjectId()).add(timerEntity.getId());
            }
            connection = dataSource.getConnection();
            statement = connection.prepareStatement(createTimer);
            statementParameters(timerEntity, statement);
            statement.execute();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            safeClose(resultSet);
            safeClose(statement);
            safeClose(connection);
        }
    }

    @Override
    public void persistTimer(final TimerImpl timerEntity) {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            connection = dataSource.getConnection();
            if (timerEntity.getState() == TimerState.CANCELED ||
                    timerEntity.getState() == TimerState.EXPIRED) {
                String deleteTimer = sql(DELETE_TIMER);
                statement = connection.prepareStatement(deleteTimer);
                statement.setString(1, timerEntity.getTimedObjectId());
                statement.setString(2, timerEntity.getId());
                statement.setString(3, partition);
                statement.execute();
                synchronized (this) {
                    knownTimerIds.get(timerEntity.getTimedObjectId()).remove(timerEntity.getId());
                }
            } else {
                synchronized (this) {
                    knownTimerIds.get(timerEntity.getTimedObjectId()).add(timerEntity.getId());
                }
                String updateTimer = sql(UPDATE_TIMER);
                statement = connection.prepareStatement(updateTimer);
                statement.setTimestamp(1, timestamp(timerEntity.getNextExpiration()));
                statement.setTimestamp(2, timestamp(timerEntity.getPreviousRun()));
                statement.setString(3, timerEntity.getState().name());
                statement.setString(4, timerEntity.getTimedObjectId());
                statement.setString(5, timerEntity.getId());
                statement.setString(6, partition);
                statement.execute();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            safeClose(resultSet);
            safeClose(statement);
            safeClose(connection);
        }
    }

    @Override
    public boolean shouldRun(TimerImpl timer, TransactionManager tm) {
        if (!allowExecution) {
            //timers never execute on this node
            return false;
        }
        String loadTimer = sql(UPDATE_RUNNING);
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            try {
                connection = dataSource.getConnection();
                statement = connection.prepareStatement(loadTimer);
                statement.setString(1, TimerState.IN_TIMEOUT.name());
                statement.setString(2, timer.getId());
                statement.setString(3, TimerState.IN_TIMEOUT.name());
                if (timer.getNextExpiration() == null) {
                    statement.setTimestamp(4, null);
                } else {
                    statement.setTimestamp(4, new Timestamp(timer.getNextExpiration().getTime()));
                }
            } catch (SQLException e) {
                // something wrong with the preparation
                throw new RuntimeException(e);
            }
            tm.begin();
            int affected = statement.executeUpdate();
            tm.commit();
            return affected == 1;
        } catch (SQLException e) {
            // failed to update the DB
            // TODO need to analyze the Exception and suppress the Exception if 'only' the timer should not executed
            try {
                tm.rollback();
            } catch (IllegalStateException | SecurityException | SystemException rbe) {
                EjbLogger.ROOT_LOGGER.timerUpdateFailedAndRollbackNotPossible(rbe);
            }
            throw new RuntimeException(e);
        }catch (SystemException | SecurityException | IllegalStateException | RollbackException | HeuristicMixedException | HeuristicRollbackException e) {
            try {
                tm.rollback();
            } catch (IllegalStateException | SecurityException | SystemException rbe) {
                EjbLogger.ROOT_LOGGER.timerUpdateFailedAndRollbackNotPossible(rbe);
            }
            throw new RuntimeException(e);
        } catch (NotSupportedException e) {
            // happen from tm.begin, no rollback necessary
            throw new RuntimeException(e);
        } finally {
            safeClose(statement);
            safeClose(connection);
        }
    }

    @Override
    public synchronized void timerUndeployed(final String timedObjectId) {
        knownTimerIds.remove(timedObjectId);
    }

    @Override
    public List<TimerImpl> loadActiveTimers(final String timedObjectId, final TimerServiceImpl timerService) {
        String loadTimer = sql(LOAD_ALL_TIMERS);
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            connection = dataSource.getConnection();
            statement = connection.prepareStatement(loadTimer);
            statement.setString(1, timedObjectId);
            statement.setString(2, partition);
            resultSet = statement.executeQuery();
            final List<TimerImpl> timers = new ArrayList<TimerImpl>();
            while (resultSet.next()) {
                try {
                    final TimerImpl timerImpl = timerFromResult(resultSet, timerService);
                    if (timerImpl != null) {
                        timers.add(timerImpl);
                    }
                } catch (Exception e) {
                    EjbLogger.ROOT_LOGGER.timerReinstatementFailed(resultSet.getString(2), resultSet.getString(1), e);
                }
            }
            synchronized (this) {
                Set<String> ids = new HashSet<>();
                for (TimerImpl timer : timers) {
                    ids.add(timer.getId());
                }
                knownTimerIds.put(timedObjectId, ids);
            }

            return timers;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            safeClose(resultSet);
            safeClose(statement);
            safeClose(connection);
        }
    }

    @Override
    public Closeable registerChangeListener(final String timedObjectId, TimerChangeListener listener) {
        changeListeners.put(timedObjectId, listener);
        return new Closeable() {
            @Override
            public void close() throws IOException {
                changeListeners.remove(timedObjectId);
            }
        };
    }

    @Override
    public DatabaseTimerPersistence getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    private TimerImpl timerFromResult(final ResultSet resultSet, final TimerServiceImpl timerService) throws SQLException {
        boolean calendarTimer = resultSet.getBoolean(24);

        TimerImpl.Builder builder = null;
        if (calendarTimer) {
            CalendarTimer.Builder cb = CalendarTimer.builder();
            builder = cb;
            //set calendar timer specifics first
            cb.setScheduleExprSecond(resultSet.getString(10));
            cb.setScheduleExprMinute(resultSet.getString(11));
            cb.setScheduleExprHour(resultSet.getString(12));
            cb.setScheduleExprDayOfWeek(resultSet.getString(13));
            cb.setScheduleExprDayOfMonth(resultSet.getString(14));
            cb.setScheduleExprMonth(resultSet.getString(15));
            cb.setScheduleExprYear(resultSet.getString(16));
            cb.setScheduleExprStartDate(resultSet.getTimestamp(17));
            cb.setScheduleExprEndDate(resultSet.getTimestamp(18));
            cb.setScheduleExprTimezone(resultSet.getString(19));
            cb.setAutoTimer(resultSet.getBoolean(20));

            final String clazz = resultSet.getString(21);
            final String methodName = resultSet.getString(22);
            if (methodName != null) {
                final String paramString = resultSet.getString(23);
                final String[] params = paramString == null || paramString.isEmpty() ? new String[0] : paramString.split(";");
                final Method timeoutMethod = CalendarTimer.getTimeoutMethod(new TimeoutMethod(clazz, methodName, params), timerService.getTimedObjectInvoker().getValue().getClassLoader());
                if (timeoutMethod == null) {
                    EjbLogger.ROOT_LOGGER.timerReinstatementFailed(resultSet.getString(2), resultSet.getString(1), new NoSuchMethodException());
                    return null;
                }
                cb.setTimeoutMethod(timeoutMethod);
            }
        } else {
            builder = TimerImpl.builder();
        }


        builder.setId(resultSet.getString(1));
        builder.setTimedObjectId(resultSet.getString(2));
        builder.setInitialDate(resultSet.getTimestamp(3));
        builder.setRepeatInterval(resultSet.getLong(4));
        builder.setNextDate(resultSet.getTimestamp(5));
        builder.setPreviousRun(resultSet.getTimestamp(6));
        builder.setPrimaryKey(deSerialize(resultSet.getString(7)));
        builder.setInfo((Serializable) deSerialize(resultSet.getString(8)));
        builder.setTimerState(TimerState.valueOf(resultSet.getString(9)));
        builder.setPersistent(true);
        return builder.build(timerService);
    }

    private void statementParameters(final TimerImpl timerEntity, final PreparedStatement statement) throws SQLException {
        statement.setString(1, timerEntity.getId());
        statement.setString(2, timerEntity.getTimedObjectId());
        statement.setTimestamp(3, timestamp(timerEntity.getInitialExpiration()));
        statement.setLong(4, timerEntity.getInterval());
        statement.setTimestamp(5, timestamp(timerEntity.getNextExpiration()));
        statement.setTimestamp(6, timestamp(timerEntity.getPreviousRun()));
        statement.setString(7, serialize((Serializable) timerEntity.getPrimaryKey()));
        statement.setString(8, serialize(timerEntity.getTimerInfo()));
        statement.setString(9, timerEntity.getState().name());

        if (timerEntity instanceof CalendarTimer) {
            final CalendarTimer c = (CalendarTimer) timerEntity;
            statement.setString(10, c.getScheduleExpression().getSecond());
            statement.setString(11, c.getScheduleExpression().getMinute());
            statement.setString(12, c.getScheduleExpression().getHour());
            statement.setString(13, c.getScheduleExpression().getDayOfWeek());
            statement.setString(14, c.getScheduleExpression().getDayOfMonth());
            statement.setString(15, c.getScheduleExpression().getMonth());
            statement.setString(16, c.getScheduleExpression().getYear());
            statement.setTimestamp(17, timestamp(c.getScheduleExpression().getStart()));
            statement.setTimestamp(18, timestamp(c.getScheduleExpression().getEnd()));
            statement.setString(19, c.getScheduleExpression().getTimezone());
            statement.setBoolean(20, c.isAutoTimer());
            if (c.isAutoTimer()) {
                statement.setString(21, c.getTimeoutMethod().getDeclaringClass().getName());
                statement.setString(22, c.getTimeoutMethod().getName());
                StringBuilder params = new StringBuilder();
                final Class<?>[] parameterTypes = c.getTimeoutMethod().getParameterTypes();
                for (int i = 0; i < parameterTypes.length; ++i) {
                    params.append(parameterTypes[i].getName());
                    if (i != parameterTypes.length - 1) {
                        params.append(";");
                    }
                }
                statement.setString(23, params.toString());
            } else {
                statement.setString(21, null);
                statement.setString(22, null);
                statement.setString(23, null);
            }
            statement.setBoolean(24, true);
        } else {
            statement.setString(10, null);
            statement.setString(11, null);
            statement.setString(12, null);
            statement.setString(13, null);
            statement.setString(14, null);
            statement.setString(15, null);
            statement.setString(16, null);
            statement.setTimestamp(17, null);
            statement.setTimestamp(18, null);
            statement.setString(19, null);
            statement.setBoolean(20, false);
            statement.setString(21, null);
            statement.setString(22, null);
            statement.setString(23, null);
            statement.setBoolean(24, false);
        }
        statement.setString(25, partition);
    }

    private String serialize(final Serializable serializable) {
        if (serializable == null) {
            return null;
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            final Marshaller marshaller = factory.createMarshaller(configuration);
            marshaller.start(new OutputStreamByteOutput(out));
            marshaller.writeObject(serializable);
            marshaller.finish();
            out.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return Base64.encodeBytes(out.toByteArray());
    }

    public Object deSerialize(final String data) throws SQLException {
        if (data == null) {
            return null;
        }
        InputStream in = new ByteArrayInputStream(Base64.decode(data));
        try {
            final Unmarshaller unmarshaller = factory.createUnmarshaller(configuration);
            unmarshaller.start(new InputStreamByteInput(in));
            Object ret = unmarshaller.readObject();
            unmarshaller.finish();
            return ret;
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } finally {
            safeClose(in);
        }
    }

    private Timestamp timestamp(final Date date) {
        if (date == null) {
            return null;
        }
        return new Timestamp(date.getTime());
    }


    public InjectedValue<ManagedReferenceFactory> getDataSourceInjectedValue() {
        return dataSourceInjectedValue;
    }

    public InjectedValue<ModuleLoader> getModuleLoader() {
        return moduleLoader;
    }

    public InjectedValue<Timer> getTimerInjectedValue() {
        return timerInjectedValue;
    }

    private static void safeClose(final Closeable resource) {
        try {
            if (resource != null) {
                resource.close();
            }
        } catch (Throwable t) {
            EjbLogger.ROOT_LOGGER.tracef(t, "Closing resource failed");
        }
    }

    private static void safeClose(final Statement resource) {
        try {
            if (resource != null) {
                resource.close();
            }
        } catch (Throwable t) {
            EjbLogger.ROOT_LOGGER.tracef(t, "Closing resource failed");
        }
    }

    private static void safeClose(final Connection resource) {
        try {
            if (resource != null) {
                resource.close();
            }
        } catch (Throwable t) {
            EjbLogger.ROOT_LOGGER.tracef(t, "Closing resource failed");
        }
    }

    private static void safeClose(final ResultSet resource) {
        try {
            if (resource != null) {
                resource.close();
            }
        } catch (Throwable t) {
            EjbLogger.ROOT_LOGGER.tracef(t, "Closing resource failed");
        }
    }

    private class RefreshTask extends TimerTask {

        private volatile AtomicBoolean running = new AtomicBoolean();

        @Override
        public void run() {
            if (running.compareAndSet(false, true)) {
                try {
                    Set<String> timedObjects;
                    synchronized (DatabaseTimerPersistence.this) {
                        timedObjects = new HashSet<>(knownTimerIds.keySet());
                    }
                    for (String timedObjectId : timedObjects) {
                        TimerChangeListener listener = changeListeners.get(timedObjectId);
                        if (listener == null) {
                            continue;
                        }
                        final Set<String> existing;
                        synchronized (DatabaseTimerPersistence.this) {
                            existing = new HashSet<>(knownTimerIds.get(timedObjectId));
                        }
                        String loadTimer = sql(LOAD_ALL_TIMERS);
                        Connection connection = null;
                        PreparedStatement statement = null;
                        ResultSet resultSet = null;
                        try {
                            connection = dataSource.getConnection();
                            statement = connection.prepareStatement(loadTimer);
                            statement.setString(1, timedObjectId);
                            statement.setString(2, partition);
                            resultSet = statement.executeQuery();
                            while (resultSet.next()) {
                                try {
                                    String id = resultSet.getString(1);
                                    if (!existing.remove(id)) {
                                        synchronized (DatabaseTimerPersistence.this) {
                                            knownTimerIds.get(timedObjectId).add(id);
                                        }
                                        final TimerImpl timerImpl = timerFromResult(resultSet, listener.getTimerService());
                                        listener.timerAdded(timerImpl);
                                    }
                                } catch (Exception e) {
                                    EjbLogger.ROOT_LOGGER.timerReinstatementFailed(resultSet.getString(2), resultSet.getString(1), e);
                                }
                            }

                            synchronized (DatabaseTimerPersistence.this) {
                                Set<String> timers = knownTimerIds.get(timedObjectId);
                                for (String timer : existing) {
                                    timers.remove(timer);
                                    listener.timerRemoved(timer);
                                }
                            }
                        } catch (SQLException e) {
                            EjbLogger.ROOT_LOGGER.failedToRefreshTimers(timedObjectId);
                        } finally {
                            safeClose(resultSet);
                            safeClose(statement);
                            safeClose(connection);
                        }
                    }
                } finally {
                    running.set(false);
                }
            }

        }
    }
}

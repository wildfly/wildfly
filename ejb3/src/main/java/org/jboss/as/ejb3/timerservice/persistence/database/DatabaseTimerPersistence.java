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

import static org.jboss.as.ejb3.timerservice.TimerServiceImpl.safeClose;
import static org.jboss.as.ejb3.util.MethodInfoHelper.EMPTY_STRING_ARRAY;
import static org.jboss.as.ejb3.timerservice.persistence.TimeoutMethod.TIMER_PARAM_1;
import static org.jboss.as.ejb3.timerservice.persistence.TimeoutMethod.TIMER_PARAM_1_ARRAY;

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
import java.sql.Types;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;
import javax.ejb.ScheduleExpression;
import javax.sql.DataSource;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;

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
import org.wildfly.security.manager.WildFlySecurityManager;
import org.wildfly.transaction.client.ContextTransactionManager;

/**
 * <p>
 * Database timer persistence store.
 * </p>
 *
 * @author Stuart Douglas
 * @author Wolf-Dieter Fink
 * @author Joerg Baesner
 */
public class DatabaseTimerPersistence implements TimerPersistence, Service<DatabaseTimerPersistence> {
    private final InjectedValue<ManagedReferenceFactory> dataSourceInjectedValue = new InjectedValue<ManagedReferenceFactory>();
    private final InjectedValue<ModuleLoader> moduleLoader = new InjectedValue<ModuleLoader>();
    private final Map<String, TimerChangeListener> changeListeners = Collections.synchronizedMap(new HashMap<String, TimerChangeListener>());

    private final InjectedValue<java.util.Timer> timerInjectedValue = new InjectedValue<java.util.Timer>();

    private final Map<String, Set<String>> knownTimerIds = new HashMap<>();

    /** Identifier for the database dialect to be used for the timer-sql.properties */
    private String database;
    /** Name of the configured partition name*/
    private final String partition;
    /** Current node name*/
    private final String nodeName;
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

    /** database values */
    private static final String POSTGRES = "postgres";
    private static final String POSTGRESQL = "postgresql";
    private static final String MYSQL = "mysql";
    private static final String MARIADB = "mariadb";
    private static final String DB2 = "db2";
    private static final String HSQL = "hsql";
    private static final String HYPERSONIC = "hypersonic";
    private static final String H2 = "h2";
    private static final String ORACLE = "oracle";
    private static final String MSSQL = "mssql";
    private static final String SYBASE = "sybase";
    private static final String JCONNECT = "jconnect";

    /** Names for the different SQL commands stored in the properties*/
    private static final String CREATE_TABLE = "create-table";
    private static final String CREATE_TIMER = "create-timer";
    private static final String CREATE_AUTO_TIMER = "create-auto-timer";
    private static final String UPDATE_TIMER = "update-timer";
    private static final String LOAD_ALL_TIMERS = "load-all-timers";
    private static final String LOAD_TIMER = "load-timer";
    private static final String DELETE_TIMER = "delete-timer";
    private static final String UPDATE_RUNNING = "update-running";
    private static final String GET_TIMER_INFO = "get-timer-info";
    /** The format for scheduler start and end date*/
    private static final String SCHEDULER_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
    /** Pattern to pickout MSSQL */
    private static final Pattern MSSQL_PATTERN = Pattern.compile("(sqlserver|microsoft|mssql)");

    /**
     * System property {@code jboss.ejb.timer.database.clearTimerInfoCacheBeyond}
     * to configure the threshold (in minutes) to clear timer info cache
     * when database is used as the data store for the ejb timer service.
     * The default value is 15 minutes.
     * <p>
     * For example, if it is set to 10, and a timer is about to expire in
     * more than 10 minutes from now, its in-memory timer info is cleared;
     * if the timer is to expire within 10 minutes, its info is retained.
     * <p>
     * Timer info of the following types are always retained, regardless of the value of this property:
     * <ul>
     *     <li>java.lang.String
     *     <li>java.lang.Number
     *     <li>java.util.Date
     *     <li>java.lang.Character
     *     <li>enum
     * </ul>
     */
    private final long clearTimerInfoCacheBeyond = TimeUnit.MINUTES.toMillis(Long.parseLong(
            WildFlySecurityManager.getPropertyPrivileged("jboss.ejb.timer.database.clearTimerInfoCacheBeyond", "15")));

    public DatabaseTimerPersistence(final String database, String partition, String nodeName, int refreshInterval, boolean allowExecution) {
        this.database = database;
        this.partition = partition;
        this.nodeName = nodeName;
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
        investigateDialect();
        loadSqlProperties();
        checkDatabase();
        refreshTask = new RefreshTask();
        if (refreshInterval > 0) {
            timerInjectedValue.getValue().schedule(refreshTask, refreshInterval, refreshInterval);
        }
    }

    @Override
    public synchronized void stop(final StopContext context) {
        refreshTask.cancel();
        knownTimerIds.clear();
        managedReference.release();
        managedReference = null;
        dataSource = null;
    }

    /**
     * Loads timer-sql.properties into a {@code Properties}, and adjusts these
     * property entries based on the current database dialect.
     * <p>
     * If an entry key ends with a database dialect suffix for the current database dialect,
     * its value is copied to the entry with the corresponding generic key, and
     * this entry is removed.
     * <p>
     * If an entry key ends with a database dialect suffix different than the current one,
     * it is removed.
     *
     * @throws StartException if IOException when loading timer-sql.properties
     */
    private void loadSqlProperties() throws StartException {
        final InputStream stream = DatabaseTimerPersistence.class.getClassLoader().getResourceAsStream("timer-sql.properties");
        sql = new Properties();

        try {
            sql.load(stream);
        } catch (IOException e) {
            throw new StartException(e);
        } finally {
            safeClose(stream);
        }

        // Update the create-auto-timer statements for DB specifics
        switch (database) {
            case DB2:
                adjustCreateAutoTimerStatement("FROM SYSIBM.SysDummy1 ");
            break;
            case ORACLE:
                adjustCreateAutoTimerStatement("FROM DUAL ");
            break;
        }

        final Iterator<Map.Entry<Object, Object>> iterator = sql.entrySet().iterator();
        while (iterator.hasNext()) {
            final Map.Entry<Object, Object> next = iterator.next();
            final String key = (String) next.getKey();
            final int dot = key.lastIndexOf('.');

            // this may be an entry for current database, or other database dialect
            if (dot > 0) {
                final String keySuffix = key.substring(dot + 1);

                // this is an entry for the current database dialect,
                // copy its value to the corresponding generic entry
                if (keySuffix.equals(database)) {
                    final String keyWithoutSuffix = key.substring(0, dot);
                    sql.setProperty(keyWithoutSuffix, (String) next.getValue());
                }
                iterator.remove();
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
                    EjbLogger.EJB3_TIMER_LOGGER.debug("Attempting to guess on driver name.");
                    database = identifyDialect(metaData.getDriverName());
                }
            } catch (Exception e) {
                EjbLogger.EJB3_TIMER_LOGGER.debug("Unable to read JDBC metadata.", e);
            } finally {
                safeClose(connection);
            }
            if (database == null) {
                EjbLogger.EJB3_TIMER_LOGGER.databaseDialectNotConfiguredOrDetected();
            } else {
                EjbLogger.EJB3_TIMER_LOGGER.debugf("Detect database dialect as '%s'.  If this is incorrect, please specify the correct dialect using the 'database' attribute in your configuration.", database);
            }
        } else {
            EjbLogger.EJB3_TIMER_LOGGER.debugf("Database dialect '%s' read from configuration, adjusting it to match the final database valid value.", database);
            database = identifyDialect(database);
            EjbLogger.EJB3_TIMER_LOGGER.debugf("New Database dialect is '%s'.", database);
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
            name = name.toLowerCase(Locale.ROOT);
            if (name.contains(POSTGRES)) {
               unified = POSTGRESQL;
            } else if (name.contains(MYSQL)) {
                unified = MYSQL;
            } else if (name.contains(MARIADB)) {
                unified = MARIADB;
            } else if (name.contains(DB2)) {
                unified = DB2;
            } else if (name.contains(HSQL) || name.contains(HYPERSONIC)) {
                unified = HSQL;
            } else if (name.contains(H2)) {
                unified = H2;
            } else if (name.contains(ORACLE)) {
                unified = ORACLE;
            } else if (MSSQL_PATTERN.matcher(name).find()) {
                unified = MSSQL;
            } else if (name.contains(SYBASE) || name.contains(JCONNECT)) {
                unified = SYBASE;
            }
         }
        EjbLogger.EJB3_TIMER_LOGGER.debugf("Check dialect for '%s', result is '%s'", name, unified);
        return unified;
    }

    private void adjustCreateAutoTimerStatement(final String fromDummyTable) {
        final String insertQuery = sql.getProperty(CREATE_AUTO_TIMER);
        final int whereNotExists = insertQuery.indexOf("WHERE NOT EXISTS");
        if (whereNotExists > 0) {
            StringBuilder sb = new StringBuilder(insertQuery.substring(0, whereNotExists));
            sb.append(fromDummyTable).append("WHERE NOT EXISTS").append(insertQuery.substring(whereNotExists + 16));
            sql.setProperty(CREATE_AUTO_TIMER, sb.toString());
        }
    }

    /**
     * Checks whether the database transaction configuration is appropriate
     * and create the timer table if necessary.
     */
    private void checkDatabase() {
        String loadTimer = sql.getProperty(LOAD_TIMER);
        Connection connection = null;
        Statement statement = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        try {
            //test for the existence of the table by running the load timer query
            connection = dataSource.getConnection();
            if (connection.getTransactionIsolation() < Connection.TRANSACTION_READ_COMMITTED) {
                EjbLogger.EJB3_TIMER_LOGGER.wrongTransactionIsolationConfiguredForTimer();
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
                    String createTable = sql.getProperty(CREATE_TABLE);
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
                    EjbLogger.EJB3_TIMER_LOGGER.couldNotCreateTable(e1);
                }
            } else {
                EjbLogger.EJB3_TIMER_LOGGER.couldNotCreateTable(e);
            }
        } finally {
            safeClose(resultSet);
            safeClose(preparedStatement);
            safeClose(statement);
            safeClose(connection);
        }
    }

    /**
     * Loads a timer from database by its id and timed object id.
     *
     * @param timedObjectId the timed object id for the timer
     * @param timerId the timer id
     * @param timerService the active timer service
     * @return the timer loaded from database; null if nothing can be loaded
     */
    public TimerImpl loadTimer(final String timedObjectId, final String timerId, final TimerServiceImpl timerService) {
        String loadTimer = sql.getProperty(LOAD_TIMER);
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        TimerImpl timer = null;
        try {
            connection = dataSource.getConnection();
            preparedStatement = connection.prepareStatement(loadTimer);
            preparedStatement.setString(1, timedObjectId);
            preparedStatement.setString(2, timerId);
            preparedStatement.setString(3, partition);
            resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                Holder holder = timerFromResult(resultSet, timerService, timerId, null);
                if (holder != null) {
                    timer = holder.timer;
                }
            }
        } catch (SQLException e) {
            EjbLogger.EJB3_TIMER_LOGGER.failToRestoreTimersForObjectId(timerId, e);
        } finally {
            safeClose(resultSet);
            safeClose(preparedStatement);
            safeClose(connection);
        }
        return timer;
    }

    @Override
    public void addTimer(final TimerImpl timerEntity) {
        String timedObjectId = timerEntity.getTimedObjectId();
        synchronized (this) {
            if(!knownTimerIds.containsKey(timedObjectId)) {
                throw EjbLogger.EJB3_TIMER_LOGGER.timerCannotBeAdded(timerEntity);
            }
        }

        if (timerEntity.isAutoTimer()) {
            addAutoTimer((CalendarTimer) timerEntity);
            return;
        }

        String createTimer = sql.getProperty(CREATE_TIMER);
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            synchronized (this) {
                knownTimerIds.get(timerEntity.getTimedObjectId()).add(timerEntity.getId());
            }
            connection = dataSource.getConnection();
            statement = connection.prepareStatement(createTimer);
            statementParameters(timerEntity, statement);
            statement.execute();

            if (isClearTimerInfoCache(timerEntity)) {
                timerEntity.setCachedTimerInfo(Object.class);
                EjbLogger.EJB3_TIMER_LOGGER.debugf("Cleared timer info for timer: %s", timerEntity.getId());
            }
        } catch (SQLException e) {
            timerEntity.setCachedTimerInfo(null);
            throw new RuntimeException(e);
        } finally {
            safeClose(statement);
            safeClose(connection);
        }
    }

    @Override
    public void persistTimer(final TimerImpl timerEntity) {
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = dataSource.getConnection();
            if (timerEntity.getState() == TimerState.CANCELED ||
                    timerEntity.getState() == TimerState.EXPIRED) {
                String deleteTimer = sql.getProperty(DELETE_TIMER);
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
                String updateTimer = sql.getProperty(UPDATE_TIMER);
                statement = connection.prepareStatement(updateTimer);
                statement.setTimestamp(1, timestamp(timerEntity.getNextExpiration()));
                statement.setTimestamp(2, timestamp(timerEntity.getPreviousRun()));
                statement.setString(3, timerEntity.getState().name());
                setNodeName(timerEntity.getState(), statement, 4);
                // WHERE CLAUSE
                statement.setString(5, timerEntity.getTimedObjectId());
                statement.setString(6, timerEntity.getId());
                statement.setString(7, partition);
                statement.setString(8, nodeName);   // only persist if this node or empty
                statement.execute();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            safeClose(statement);
            safeClose(connection);
        }
    }

    @Override
    public boolean shouldRun(TimerImpl timer) {
        final ContextTransactionManager tm = ContextTransactionManager.getInstance();
        if (!allowExecution) {
            //timers never execute on this node
            return false;
        }
        String loadTimer = sql.getProperty(UPDATE_RUNNING);
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            tm.begin();
            try {
                connection = dataSource.getConnection();
                statement = connection.prepareStatement(loadTimer);
                statement.setString(1, TimerState.IN_TIMEOUT.name());
                setNodeName(TimerState.IN_TIMEOUT, statement, 2);
                statement.setString(3, timer.getId());
                statement.setString(4, TimerState.IN_TIMEOUT.name());
                statement.setString(5, TimerState.RETRY_TIMEOUT.name());
                if (timer.getNextExpiration() == null) {
                    statement.setTimestamp(6, null);
                } else {
                    statement.setTimestamp(6, timestamp(timer.getNextExpiration()));
                }
            } catch (SQLException e) {
                try {
                    tm.rollback();
                } catch (Exception ee){
                    EjbLogger.EJB3_TIMER_LOGGER.timerUpdateFailedAndRollbackNotPossible(ee);
                }
                // fix for WFLY-10130
                EjbLogger.EJB3_TIMER_LOGGER.exceptionCheckingIfTimerShouldRun(timer, e);
                return false;
            }

            int affected = statement.executeUpdate();
            tm.commit();
            return affected == 1;
        } catch (SQLException | SystemException | SecurityException | IllegalStateException | RollbackException | HeuristicMixedException | HeuristicRollbackException e) {
            // failed to update the DB
            try {
                tm.rollback();
            } catch (IllegalStateException | SecurityException | SystemException rbe) {
                EjbLogger.EJB3_TIMER_LOGGER.timerUpdateFailedAndRollbackNotPossible(rbe);
            }
            EjbLogger.EJB3_TIMER_LOGGER.debugf(e, "Timer %s not running due to exception ", timer);
            return false;
        } catch (NotSupportedException e) {
            // happen from tm.begin, no rollback necessary
            EjbLogger.EJB3_TIMER_LOGGER.timerNotRunning(e, timer);
            return false;
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
    public synchronized void timerDeployed(String timedObjectId) {
        knownTimerIds.put(timedObjectId, new HashSet<>());
    }

    @Override
    public List<TimerImpl> loadActiveTimers(final String timedObjectId, final TimerServiceImpl timerService) {
        if(!knownTimerIds.containsKey(timedObjectId)) {
            // if the timedObjectId has not being deployed
            EjbLogger.EJB3_TIMER_LOGGER.timerNotDeployed(timedObjectId);
            return Collections.emptyList();
        }
        String loadTimer = sql.getProperty(LOAD_ALL_TIMERS);
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            connection = dataSource.getConnection();
            statement = connection.prepareStatement(loadTimer);
            statement.setString(1, timedObjectId);
            statement.setString(2, partition);
            resultSet = statement.executeQuery();
            final List<Holder> timers = new ArrayList<>();
            while (resultSet.next()) {
                String timerId = null;
                try {
                    timerId = resultSet.getString(1);
                    final Holder timerImpl = timerFromResult(resultSet, timerService, timerId, null);
                    if (timerImpl != null) {
                        timers.add(timerImpl);
                    } else {
                        final String deleteTimer = sql.getProperty(DELETE_TIMER);
                        try (PreparedStatement deleteStatement = connection.prepareStatement(deleteTimer)) {
                            deleteStatement.setString(1, resultSet.getString(2));
                            deleteStatement.setString(2, timerId);
                            deleteStatement.setString(3, partition);
                            deleteStatement.execute();
                        }
                    }
                } catch (Exception e) {
                    EjbLogger.EJB3_TIMER_LOGGER.timerReinstatementFailed(resultSet.getString(2), timerId, e);
                }
            }
            synchronized (this) {
                // ids should be always be not null
                Set<String> ids = knownTimerIds.get(timedObjectId);
                for (Holder timer : timers) {
                    ids.add(timer.timer.getId());
                }

                for(Holder timer : timers) {
                    if(timer.requiresReset) {
                        TimerImpl ret = timer.timer;
                        EjbLogger.DEPLOYMENT_LOGGER.loadedPersistentTimerInTimeout(ret.getId(), ret.getTimedObjectId());
                        if(ret.getNextExpiration() == null) {
                            ret.setTimerState(TimerState.CANCELED, null);
                            persistTimer(ret);
                        } else {
                            ret.setTimerState(TimerState.ACTIVE, null);
                            persistTimer(ret);
                        }
                    }
                }
            }
            List<TimerImpl> ret = new ArrayList<>();
            for(Holder timer : timers) {
                ret.add(timer.timer);
            }
            return ret;
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

    public void refreshTimers() {
        refreshTask.run();
    }

    /**
     * Obtains a {@link Holder} from a row in {@code ResultSet}.
     * Caller of this method must get the timer id from the {@code ResultSet}
     * in advance and pass in as param {@code timerId}.
     * Caller of this method may get the timer state from the {@code ResultSet}
     * in advance and pass in as param {@code timerState}.
     *
     * @param resultSet  the {@code ResultSet} from database query
     * @param timerService  the associated {@code TimerServiceImpl}
     * @param timerId  the timer id, not null
     * @param timerState  the timer state from current row in the {@code ResultSet}, may be null
     * @return the {@code Holder} instance containing the timer from current {@code ResultSet} row
     * @throws SQLException on errors reading from {@code ResultSet}
     */
    private Holder timerFromResult(final ResultSet resultSet, final TimerServiceImpl timerService,
                                   final String timerId, final TimerState timerState) throws SQLException {
        boolean calendarTimer = resultSet.getBoolean(24);
        final String nodeName = resultSet.getString(25);
        boolean requiresReset = false;

        TimerImpl.Builder builder;
        if (calendarTimer) {
            CalendarTimer.Builder cb = CalendarTimer.builder();
            builder = cb;
            //set calendar timer specifics first
            final ScheduleExpression scheduleExpression = new ScheduleExpression();
            scheduleExpression.second(resultSet.getString(10));
            scheduleExpression.minute(resultSet.getString(11));
            scheduleExpression.hour(resultSet.getString(12));
            scheduleExpression.dayOfWeek(resultSet.getString(13));
            scheduleExpression.dayOfMonth(resultSet.getString(14));
            scheduleExpression.month(resultSet.getString(15));
            scheduleExpression.year(resultSet.getString(16));
            scheduleExpression.start(stringAsSchedulerDate(resultSet.getString(17), timerId));
            scheduleExpression.end(stringAsSchedulerDate(resultSet.getString(18), timerId));
            scheduleExpression.timezone(resultSet.getString(19));

            cb.setScheduleExpression(scheduleExpression);
            cb.setAutoTimer(resultSet.getBoolean(20));

            final String clazz = resultSet.getString(21);
            final String methodName = resultSet.getString(22);
            if (methodName != null) {
                final String paramString = resultSet.getString(23);
                final String[] params = paramString == null || paramString.isEmpty() ? EMPTY_STRING_ARRAY : TIMER_PARAM_1_ARRAY;
                final Method timeoutMethod = CalendarTimer.getTimeoutMethod(new TimeoutMethod(clazz, methodName, params), timerService.getTimedObjectInvoker().getValue().getClassLoader());
                if (timeoutMethod == null) {
                    EjbLogger.EJB3_TIMER_LOGGER.timerReinstatementFailed(resultSet.getString(2), timerId, new NoSuchMethodException());
                    return null;
                }
                cb.setTimeoutMethod(timeoutMethod);
            }
        } else {
            builder = TimerImpl.builder();
        }


        builder.setId(timerId);
        builder.setTimedObjectId(resultSet.getString(2));
        builder.setInitialDate(resultSet.getTimestamp(3));
        builder.setRepeatInterval(resultSet.getLong(4));
        builder.setNextDate(resultSet.getTimestamp(5));
        builder.setPreviousRun(resultSet.getTimestamp(6));
//        builder.setPrimaryKey(deSerialize(resultSet.getString(7)));
        builder.setInfo((Serializable) deSerialize(resultSet.getString(8)));
        builder.setTimerState(timerState != null ? timerState : TimerState.valueOf(resultSet.getString(9)));
        builder.setPersistent(true);

        TimerImpl ret =  builder.build(timerService);
        if (isClearTimerInfoCache(ret)) {
            ret.setCachedTimerInfo(Object.class);
            EjbLogger.EJB3_TIMER_LOGGER.debugf("Cleared timer info for timer: %s", timerId);
        }

        if (nodeName != null
                && nodeName.equals(this.nodeName)
                && (ret.getState() == TimerState.IN_TIMEOUT ||
                    ret.getState() == TimerState.RETRY_TIMEOUT)) {
            requiresReset = true;
        }
        return new Holder(ret, requiresReset);
    }

    private void statementParameters(final TimerImpl timerEntity, final PreparedStatement statement) throws SQLException {
        statement.setString(1, timerEntity.getId());
        statement.setString(2, timerEntity.getTimedObjectId());
        statement.setTimestamp(3, timestamp(timerEntity.getInitialExpiration()));
        statement.setLong(4, timerEntity.getInterval());
        statement.setTimestamp(5, timestamp(timerEntity.getNextExpiration()));
        statement.setTimestamp(6, timestamp(timerEntity.getPreviousRun()));
        statement.setString(7, null);
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
            // WFLY-9054: Oracle ojdbc6/7 store a timestamp as '06-JUL-17 01.54.00.269000000 PM'
            //            but expect 'YYYY-MM-DD hh:mm:ss.fffffffff' as all other DB
            statement.setString(17, schedulerDateAsString(c.getScheduleExpression().getStart()));
            statement.setString(18, schedulerDateAsString(c.getScheduleExpression().getEnd()));
            statement.setString(19, c.getScheduleExpression().getTimezone());
            statement.setBoolean(20, false);

            statement.setString(21, null);
            statement.setString(22, null);
            statement.setString(23, null);

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
        setNodeName(timerEntity.getState(), statement, 26);
    }

    private void addAutoTimer(final CalendarTimer timer) {
        String createTimer = sql.getProperty(CREATE_AUTO_TIMER);
        Connection connection = null;
        PreparedStatement statement = null;
        final String timerInfoString = serialize(timer.getTimerInfo());
        final Method timeoutMethod = timer.getTimeoutMethod();
        final String timeoutMethodClassName = timeoutMethod.getDeclaringClass().getName();
        final String timeoutMethodParam = timeoutMethod.getParameterCount() == 0 ? null : TIMER_PARAM_1;
        final ScheduleExpression exp = timer.getScheduleExpression();
        final String startDateString = schedulerDateAsString(exp.getStart());
        final String endDateString = schedulerDateAsString(exp.getEnd());
        try {
            connection = dataSource.getConnection();
            statement = connection.prepareStatement(createTimer);

            // insert values
            statement.setString(1, timer.getId());
            statement.setString(2, timer.getTimedObjectId());
            statement.setTimestamp(3, timestamp(timer.getNextExpiration()));
            statement.setString(4, timerInfoString);
            statement.setString(5, exp.getSecond());
            statement.setString(6, exp.getMinute());
            statement.setString(7, exp.getHour());
            statement.setString(8, exp.getDayOfWeek());
            statement.setString(9, exp.getDayOfMonth());
            statement.setString(10, exp.getMonth());
            statement.setString(11, exp.getYear());
            statement.setString(12, startDateString);
            statement.setString(13, endDateString);
            statement.setString(14, exp.getTimezone());
            statement.setBoolean(15, true);
            statement.setString(16, timeoutMethodClassName);
            statement.setString(17, timeoutMethod.getName());
            statement.setString(18, timeoutMethodParam);
            statement.setBoolean(19, true);
            statement.setString(20, partition);

            // where clause
            statement.setString(21, timer.getTimedObjectId());
            statement.setString(22, exp.getSecond());
            statement.setString(23, exp.getMinute());
            statement.setString(24, exp.getHour());
            statement.setString(25, exp.getDayOfWeek());
            statement.setString(26, exp.getDayOfMonth());
            statement.setString(27, exp.getMonth());
            statement.setString(28, exp.getYear());

            statement.setString(29, startDateString);
            statement.setString(30, startDateString);

            statement.setString(31, endDateString);
            statement.setString(32, endDateString);

            statement.setString(33, exp.getTimezone());
            statement.setString(34, exp.getTimezone());

            statement.setString(35, timeoutMethodClassName);
            statement.setString(36, timeoutMethod.getName());

            statement.setString(37, timeoutMethodParam);
            statement.setString(38, timeoutMethodParam);

            statement.setString(39, partition);

            int affectedRows = statement.executeUpdate();
            if (affectedRows < 1) {
                timer.setTimerState(TimerState.CANCELED, null);
            } else {
                synchronized (this) {
                    knownTimerIds.get(timer.getTimedObjectId()).add(timer.getId());
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            safeClose(statement);
            safeClose(connection);
        }
    }

    /**
     * Retrieves the timer info from the timer database.
     *
     * @param timer the timer whose info to be retrieved
     * @return the timer info from database; null if {@code SQLException}
     */
    public Serializable getPersistedTimerInfo(final TimerImpl timer) {
        String getTimerInfo = sql.getProperty(GET_TIMER_INFO);
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        Serializable result = null;
        try {
            connection = dataSource.getConnection();
            statement = connection.prepareStatement(getTimerInfo);
            statement.setString(1, timer.getTimedObjectId());
            statement.setString(2, timer.getId());
            resultSet = statement.executeQuery();
            if (resultSet.next()) {
                result = (Serializable) deSerialize(resultSet.getString(1));
            }
        } catch (SQLException e) {
            EjbLogger.EJB3_TIMER_LOGGER.failedToRetrieveTimerInfo(timer, e);
        } finally {
            safeClose(resultSet);
            safeClose(statement);
            safeClose(connection);
        }
        return result;
    }

    /**
     * Determines if the cached info in the timer should be cleared.
     * @param timer the timer to check
     * @return true if the cached info in the timer should be cleared; otherwise false
     */
    private boolean isClearTimerInfoCache(final TimerImpl timer) {
        if (timer.isAutoTimer()) {
            return false;
        }
        final Serializable info = timer.getCachedTimerInfo();
        if (info == null || info instanceof String || info instanceof Number
                || info instanceof Enum || info instanceof java.util.Date
                || info instanceof Character) {
            return false;
        }
        final Date nextExpiration = timer.getNextExpiration();
        if (nextExpiration == null) {
            return true;
        }
        final long howLongTillExpiry = nextExpiration.getTime() - System.currentTimeMillis();
        if (howLongTillExpiry <= clearTimerInfoCacheBeyond) {
            return false;
        }
        return true;
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
        return Base64.getEncoder().encodeToString(out.toByteArray());
    }

    public Object deSerialize(final String data) throws SQLException {
        if (data == null) {
            return null;
        }
        InputStream in = new ByteArrayInputStream(Base64.getDecoder().decode(data));
        try {
            final Unmarshaller unmarshaller = factory.createUnmarshaller(configuration);
            unmarshaller.start(new InputStreamByteInput(in));
            Object ret = unmarshaller.readObject();
            unmarshaller.finish();
            return ret;
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        } finally {
            safeClose(in);
        }
    }

    private String schedulerDateAsString(final Date date) {
        if (date == null) {
            return null;
        }
        return new SimpleDateFormat(SCHEDULER_DATE_FORMAT).format(date);
    }

    /** Convert the stored date-string from database back to Date */
    private Date stringAsSchedulerDate(final String date, final String timerId) {
        if (date == null) {
            return null;
        }
        try {
            return new SimpleDateFormat(SCHEDULER_DATE_FORMAT).parse(date);
        } catch (ParseException e) {
            EjbLogger.EJB3_TIMER_LOGGER.scheduleExpressionDateFromTimerPersistenceInvalid(timerId, e.getMessage());
            return null;
        }
    }

    private Timestamp timestamp(final Date date) {
        if (date == null) {
            return null;
        }
        long time = date.getTime();
        if(database != null && (database.equals("mysql") || database.equals("postgresql"))) {
            // truncate the milliseconds because MySQL 5.6.4+ and MariaDB 5.3+ do the same
            // and querying with a Timestamp containing milliseconds doesn't match the rows
            // with such truncated DATETIMEs
            // truncate the milliseconds because postgres timestamp does not reliably support milliseconds
            time -= time % 1000;
        }
        return new Timestamp(time);
    }

    /**
     * Set the node name for persistence if the state is IN_TIMEOUT or RETRY_TIMEOUT to show which node is current active for the timer.
     */
    private void setNodeName(final TimerState timerState, PreparedStatement statement, int paramIndex) throws SQLException {
        if(timerState == TimerState.IN_TIMEOUT || timerState == TimerState.RETRY_TIMEOUT) {
            statement.setString(paramIndex, nodeName);
        } else {
            statement.setNull(paramIndex, Types.VARCHAR);
        }
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
                        String loadTimer = sql.getProperty(LOAD_ALL_TIMERS);
                        Connection connection = null;
                        PreparedStatement statement = null;
                        ResultSet resultSet = null;
                        try {
                            connection = dataSource.getConnection();
                            statement = connection.prepareStatement(loadTimer);
                            statement.setString(1, timedObjectId);
                            statement.setString(2, partition);
                            resultSet = statement.executeQuery();
                            final TimerServiceImpl timerService = listener.getTimerService();
                            while (resultSet.next()) {
                                String id = null;
                                try {
                                    id = resultSet.getString(1);
                                    if (!existing.remove(id)) {
                                        final Holder holder = timerFromResult(resultSet, timerService, id, null);
                                        if(holder != null) {
                                            synchronized (DatabaseTimerPersistence.this) {
                                                knownTimerIds.get(timedObjectId).add(id);
                                                listener.timerAdded(holder.timer);
                                            }
                                        }
                                    } else {
                                        TimerImpl oldTimer = timerService.getTimer(id);
                                        // if it is already in memory but it is not in sync we have a problem
                                        // remove and add -> the probable cause is db glitch
                                        boolean invalidMemoryTimer = oldTimer != null && !TimerState.CREATED_ACTIVE_IN_TIMEOUT_RETRY_TIMEOUT.contains(oldTimer.getState());

                                        // if timers memory - db are in non intersect subsets of valid/invalid states. we put them in sync
                                        if (invalidMemoryTimer) {
                                            TimerState dbTimerState = TimerState.valueOf(resultSet.getString(9));
                                            boolean validDBTimer = TimerState.CREATED_ACTIVE_IN_TIMEOUT_RETRY_TIMEOUT.contains(dbTimerState);
                                            if (validDBTimer) {
                                                final Holder holder = timerFromResult(resultSet, timerService, id, dbTimerState);
                                                if (holder != null) {
                                                    synchronized (DatabaseTimerPersistence.this) {
                                                        knownTimerIds.get(timedObjectId).add(id);
                                                        listener.timerSync(oldTimer, holder.timer);
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } catch (Exception e) {
                                    EjbLogger.EJB3_TIMER_LOGGER.timerReinstatementFailed(resultSet.getString(2), id, e);
                                }
                            }

                            synchronized (DatabaseTimerPersistence.this) {
                                Set<String> timers = knownTimerIds.get(timedObjectId);
                                for (String timer : existing) {
                                    TimerImpl timer1 = timerService.getTimer(timer);
                                    if (timer1 != null && timer1.getState() != TimerState.CREATED) {
                                        timers.remove(timer);
                                        listener.timerRemoved(timer);
                                    }
                                }
                            }
                        } catch (SQLException e) {
                            EjbLogger.EJB3_TIMER_LOGGER.failedToRefreshTimers(timedObjectId);
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


    static final class Holder {
        final TimerImpl timer;
        final boolean requiresReset;

        Holder(TimerImpl timer, boolean requiresReset) {
            this.timer = timer;
            this.requiresReset = requiresReset;
        }
    }
}

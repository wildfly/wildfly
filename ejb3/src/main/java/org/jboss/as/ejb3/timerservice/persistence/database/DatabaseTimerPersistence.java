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

package org.jboss.as.ejb3.timerservice.persistence.database;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import javax.sql.DataSource;

import org.jboss.as.ejb3.EjbLogger;
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
 * @author Stuart Douglas
 */
public class DatabaseTimerPersistence implements TimerPersistence, Service<DatabaseTimerPersistence> {

    private final InjectedValue<ManagedReferenceFactory> dataSourceInjectedValue = new InjectedValue<ManagedReferenceFactory>();
    private final InjectedValue<ModuleLoader> moduleLoader = new InjectedValue<ModuleLoader>();
    private final String name;
    private volatile ManagedReference managedReference;
    private volatile DataSource dataSource;
    private volatile Properties sql;
    private MarshallerFactory factory;
    private MarshallingConfiguration configuration;

    private static final String CREATE_TABLE = "create-table";
    private static final String CREATE_TIMER = "create-timer";
    private static final String UPDATE_TIMER = "update-timer";
    private static final String LOAD_ALL_TIMERS = "load-all-timers";
    private static final String LOAD_TIMER = "load-timer";
    private static final String DELETE_TIMER = "delete-timer";

    public DatabaseTimerPersistence(final String name) {
        this.name = name;
    }

    @Override
    public void start(final StartContext context) throws StartException {

        factory = new RiverMarshallerFactory();
        configuration = new MarshallingConfiguration();
        configuration.setClassResolver(ModularClassResolver.getInstance(moduleLoader.getValue()));

        managedReference = dataSourceInjectedValue.getValue().getReference();
        dataSource = (DataSource) managedReference.getInstance();
        final InputStream stream = DatabaseTimerPersistence.class.getResourceAsStream("sql.properties");
        sql = new Properties();
        try {
            sql.load(stream);
        } catch (IOException e) {
            throw new StartException(e);
        } finally {
            safeClose(stream);
        }
        runCreateTable();
    }

    @Override
    public void stop(final StopContext context) {
        managedReference.release();
        managedReference = null;
        dataSource = null;
    }

    void runCreateTable() {
        String loadTimer = sql.getProperty(LOAD_TIMER);
        Connection connection = null;
        Statement statement = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        try {
            //test for the existence of the table by running the load timer query
            connection = dataSource.getConnection();
            preparedStatement = connection.prepareStatement(loadTimer);
            preparedStatement.setString(1, "NON-EXISTENT");
            preparedStatement.setString(2, "NON-EXISTENT");
            resultSet = preparedStatement.executeQuery();
        } catch (SQLException e) {
            //the query failed, assume it is because the table does not exist
            if (connection != null) {
                try {
                    String createTable = sql.getProperty(CREATE_TABLE);
                    statement = connection.createStatement();
                    statement.executeUpdate(createTable);
                } catch (SQLException e1) {
                    EjbLogger.EJB3_LOGGER.couldNotCreateTable(e1);
                }
            } else {
                EjbLogger.EJB3_LOGGER.couldNotCreateTable(e);
            }
        } finally {
            safeClose(resultSet);
            safeClose(preparedStatement);
            safeClose(statement);
            safeClose(connection);
        }
    }

    @Override
    public void addTimer(final TimerImpl timerEntity) {
        String createTimer = sql.getProperty(CREATE_TIMER);
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
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
                String deleteTimer = sql.getProperty(DELETE_TIMER);
                statement = connection.prepareStatement(deleteTimer);
                statement.setString(1, timerEntity.getTimedObjectId());
                statement.setString(2, timerEntity.getId());
                statement.execute();
            } else {
                String updateTimer = sql.getProperty(UPDATE_TIMER);
                statement = connection.prepareStatement(updateTimer);
                statement.setTimestamp(1, timestamp(timerEntity.getNextExpiration()));
                statement.setTimestamp(2, timestamp(timerEntity.getPreviousRun()));
                statement.setString(3, timerEntity.getState().name());
                statement.setString(4, timerEntity.getTimedObjectId());
                statement.setString(5, timerEntity.getId());
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
    public void timerUndeployed(final String timedObjectId) {

    }

    @Override
    public List<TimerImpl> loadActiveTimers(final String timedObjectId, final TimerServiceImpl timerService) {
        String loadTimer = sql.getProperty(LOAD_ALL_TIMERS);
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            connection = dataSource.getConnection();
            statement = connection.prepareStatement(loadTimer);
            statement.setString(1, timedObjectId);
            resultSet = statement.executeQuery();
            final List<TimerImpl> timers = new ArrayList<TimerImpl>();
            while (resultSet.next()) {
                try {
                    final TimerImpl timerImpl = timerFromResult(resultSet, timerService);
                    if(timerImpl != null) {
                        timers.add(timerImpl);
                    }
                } catch (Exception e) {
                    EjbLogger.ROOT_LOGGER.timerReinstatementFailed(resultSet.getString(2), resultSet.getString(1), e);
                }
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
                final Method timeoutMethod = CalendarTimer.getTimeoutMethod(new TimeoutMethod(clazz, methodName, params), timerService.getTimedObjectInvoker().getValue());
                if(timeoutMethod == null) {
                    EjbLogger.ROOT_LOGGER.timerReinstatementFailed(resultSet.getString(2), resultSet.getString(1), new NoSuchMethodException());
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
    }

    private String serialize(final Serializable serializable) {
        if(serializable == null) {
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
        if(data == null) {
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

    private static void safeClose(final Closeable resource) {
        try {
            if (resource != null) {
                resource.close();
            }
        } catch (Throwable t) {
            EjbLogger.EJB3_LOGGER.tracef(t, "Closing resource failed");
        }
    }

    private static void safeClose(final Statement resource) {
        try {
            if (resource != null) {
                resource.close();
            }
        } catch (Throwable t) {
            EjbLogger.EJB3_LOGGER.tracef(t, "Closing resource failed");
        }
    }

    private static void safeClose(final Connection resource) {
        try {
            if (resource != null) {
                resource.close();
            }
        } catch (Throwable t) {
            EjbLogger.EJB3_LOGGER.tracef(t, "Closing resource failed");
        }
    }

    private static void safeClose(final ResultSet resource) {
        try {
            if (resource != null) {
                resource.close();
            }
        } catch (Throwable t) {
            EjbLogger.EJB3_LOGGER.tracef(t, "Closing resource failed");
        }
    }
}

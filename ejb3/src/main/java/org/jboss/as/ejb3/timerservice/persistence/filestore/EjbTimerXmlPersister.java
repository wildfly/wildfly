/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat Middleware LLC, and individual contributors
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

package org.jboss.as.ejb3.timerservice.persistence.filestore;

import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.List;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.as.ejb3.timerservice.CalendarTimer;
import org.jboss.as.ejb3.timerservice.TimerImpl;
import org.jboss.marshalling.Marshaller;
import org.jboss.marshalling.MarshallerFactory;
import org.jboss.marshalling.MarshallingConfiguration;
import org.jboss.marshalling.OutputStreamByteOutput;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * @author Stuart Douglas
 */
public class EjbTimerXmlPersister implements XMLElementWriter<List<TimerImpl>> {

    static final String TIMERS = "timers";
    static final String TIMER = "timer";
    static final String CALENDAR_TIMER = "calendar-timer";
    static final String INFO = "info";
    static final String PRIMARY_KEY = "primary-key";
    static final String TIMED_OBJECT_ID = "timed-object-id";
    static final String TIMER_ID = "timer-id";
    static final String INITIAL_DATE = "initial-date";
    static final String REPEAT_INTERVAL = "repeat-interval";
    static final String NEXT_DATE = "next-date";
    static final String PREVIOUS_RUN = "previous-run";
    static final String TIMER_STATE = "timer-state";
    static final String TIMEOUT_METHOD = "timeout-method";
    static final String SCHEDULE_EXPR_SECOND = "schedule-expr-second";
    static final String SCHEDULE_EXPR_MINUTE = "schedule-expr-minute";
    static final String SCHEDULE_EXPR_HOUR = "schedule-expr-hour";
    static final String SCHEDULE_EXPR_DAY_OF_WEEK = "schedule-expr-day-of-week";
    static final String SCHEDULE_EXPR_DAY_OF_MONTH = "schedule-expr-day-of-month";
    static final String SCHEDULE_EXPR_MONTH = "schedule-expr-month";
    static final String SCHEDULE_EXPR_YEAR = "schedule-expr-year";
    static final String SCHEDULE_EXPR_START_DATE = "schedule-expr-start-date";
    static final String SCHEDULE_EXPR_END_DATE = "schedule-expr-end-date";
    static final String SCHEDULE_EXPR_TIMEZONE = "schedule-expr-timezone";
    static final String PARAMETER = "parameter";
    static final String DECLARING_CLASS = "declaring-class";
    static final String NAME = "name";
    static final String TYPE = "type";

    private final MarshallerFactory factory;
    private final MarshallingConfiguration configuration;

    public EjbTimerXmlPersister(MarshallerFactory factory, MarshallingConfiguration configuration) {
        this.factory = factory;
        this.configuration = configuration;
    }

    @Override
    public void writeContent(XMLExtendedStreamWriter writer, List<TimerImpl> timers) throws XMLStreamException {

        writer.writeStartDocument();
        writer.writeStartElement(TIMERS);
        writer.writeDefaultNamespace(EjbTimerXmlParser_1_0.NAMESPACE);
        for (TimerImpl timer : timers) {
            if (timer instanceof CalendarTimer) {
                writeCalendarTimer(writer, (CalendarTimer) timer);
            } else {
                writeTimer(writer, timer);
            }
        }
        writer.writeEndElement();
        writer.writeEndDocument();
    }


    private void writeCalendarTimer(XMLExtendedStreamWriter writer, CalendarTimer timer) throws XMLStreamException {
        String info = null;
        String primaryKey = null;
        if (timer.getInfo() != null) {
            try {
                Marshaller marshaller = factory.createMarshaller(configuration);
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                marshaller.start(new OutputStreamByteOutput(out));
                marshaller.writeObject(timer.getInfo());
                marshaller.finish();
                marshaller.flush();
                info = Base64.getEncoder().encodeToString(out.toByteArray());
            } catch (Exception e) {
                EjbLogger.EJB3_TIMER_LOGGER.failedToPersistTimer(timer, e);
                return;
            }
        }
        if (timer.getPrimaryKey() != null) {
            try {
                Marshaller marshaller = factory.createMarshaller(configuration);
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                marshaller.start(new OutputStreamByteOutput(out));
                marshaller.writeObject(timer.getPrimaryKey());
                marshaller.finish();
                marshaller.flush();
                primaryKey = Base64.getEncoder().encodeToString(out.toByteArray());
            } catch (Exception e) {
                EjbLogger.EJB3_TIMER_LOGGER.failedToPersistTimer(timer, e);
                return;
            }
        }
        writer.writeStartElement(CALENDAR_TIMER);
        writer.writeAttribute(TIMED_OBJECT_ID, timer.getTimedObjectId());
        writer.writeAttribute(TIMER_ID, timer.getId());
        if (timer.getInitialExpiration() != null) {
            writer.writeAttribute(INITIAL_DATE, Long.toString(timer.getInitialExpiration().getTime()));
        }
        if(timer.getNextExpiration() != null) {
            writer.writeAttribute(NEXT_DATE, Long.toString(timer.getNextExpiration().getTime()));
        }
        writer.writeAttribute(TIMER_STATE, timer.getState().name());

        writer.writeAttribute(SCHEDULE_EXPR_SECOND, timer.getScheduleExpression().getSecond());
        writer.writeAttribute(SCHEDULE_EXPR_MINUTE, timer.getScheduleExpression().getMinute());
        writer.writeAttribute(SCHEDULE_EXPR_HOUR, timer.getScheduleExpression().getHour());
        writer.writeAttribute(SCHEDULE_EXPR_DAY_OF_WEEK, timer.getScheduleExpression().getDayOfWeek());
        writer.writeAttribute(SCHEDULE_EXPR_DAY_OF_MONTH, timer.getScheduleExpression().getDayOfMonth());
        writer.writeAttribute(SCHEDULE_EXPR_MONTH, timer.getScheduleExpression().getMonth());
        writer.writeAttribute(SCHEDULE_EXPR_YEAR, timer.getScheduleExpression().getYear());
        if (timer.getScheduleExpression().getStart() != null) {
            writer.writeAttribute(SCHEDULE_EXPR_START_DATE, Long.toString(timer.getScheduleExpression().getStart().getTime()));
        }
        if (timer.getScheduleExpression().getEnd() != null) {
            writer.writeAttribute(SCHEDULE_EXPR_END_DATE, Long.toString(timer.getScheduleExpression().getEnd().getTime()));
        }
        if (timer.getScheduleExpression().getTimezone() != null) {
            writer.writeAttribute(SCHEDULE_EXPR_TIMEZONE, timer.getScheduleExpression().getTimezone());
        }

        if (info != null) {
            writer.writeStartElement(INFO);
            writer.writeCharacters(info);
            writer.writeEndElement();
        }
        if (primaryKey != null) {
            writer.writeStartElement(PRIMARY_KEY);
            writer.writeCharacters(primaryKey);
            writer.writeEndElement();
        }
        if (timer.isAutoTimer()) {
            writer.writeStartElement(TIMEOUT_METHOD);
            writer.writeAttribute(DECLARING_CLASS, timer.getTimeoutMethod().getDeclaringClass().getName());
            writer.writeAttribute(NAME, timer.getTimeoutMethod().getName());
            for (Class<?> param : timer.getTimeoutMethod().getParameterTypes()) {
                writer.writeStartElement(PARAMETER);
                writer.writeAttribute(TYPE, param.getName());
                writer.writeEndElement();
            }
            writer.writeEndElement();
        }
        writer.writeEndElement();
    }

    private void writeTimer(XMLExtendedStreamWriter writer, TimerImpl timer) throws XMLStreamException {
        String info = null;
        String primaryKey = null;
        if (timer.getInfo() != null) {
            try {
                Marshaller marshaller = factory.createMarshaller(configuration);
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                marshaller.start(new OutputStreamByteOutput(out));
                marshaller.writeObject(timer.getInfo());
                marshaller.finish();
                marshaller.flush();
                info = Base64.getEncoder().encodeToString(out.toByteArray());
            } catch (Exception e) {
                EjbLogger.EJB3_TIMER_LOGGER.failedToPersistTimer(timer, e);
                return;
            }
        }
        if (timer.getPrimaryKey() != null) {
            try {
                Marshaller marshaller = factory.createMarshaller(configuration);
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                marshaller.start(new OutputStreamByteOutput(out));
                marshaller.writeObject(timer.getPrimaryKey());
                marshaller.finish();
                marshaller.flush();
                primaryKey = Base64.getEncoder().encodeToString(out.toByteArray());
            } catch (Exception e) {
                EjbLogger.EJB3_TIMER_LOGGER.failedToPersistTimer(timer, e);
                return;
            }
        }
        writer.writeStartElement(TIMER);
        writer.writeAttribute(TIMED_OBJECT_ID, timer.getTimedObjectId());
        writer.writeAttribute(TIMER_ID, timer.getId());
        writer.writeAttribute(INITIAL_DATE, Long.toString(timer.getInitialExpiration().getTime()));
        writer.writeAttribute(REPEAT_INTERVAL, Long.toString(timer.getInterval()));
        if(timer.getNextExpiration() != null) {
            writer.writeAttribute(NEXT_DATE, Long.toString(timer.getNextExpiration().getTime()));
        }
        writer.writeAttribute(TIMER_STATE, timer.getState().name());
        if (info != null) {
            writer.writeStartElement(INFO);
            writer.writeCharacters(info);
            writer.writeEndElement();
        }
        if (primaryKey != null) {
            writer.writeStartElement(PRIMARY_KEY);
            writer.writeCharacters(primaryKey);
            writer.writeEndElement();
        }
        writer.writeEndElement();
    }
}

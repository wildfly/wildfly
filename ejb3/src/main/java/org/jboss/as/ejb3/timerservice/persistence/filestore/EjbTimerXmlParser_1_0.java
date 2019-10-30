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

import static javax.xml.stream.XMLStreamConstants.END_ELEMENT;
import static javax.xml.stream.XMLStreamConstants.START_ELEMENT;
import static org.jboss.as.ejb3.timerservice.persistence.filestore.EjbTimerXmlPersister.CALENDAR_TIMER;
import static org.jboss.as.ejb3.timerservice.persistence.filestore.EjbTimerXmlPersister.DECLARING_CLASS;
import static org.jboss.as.ejb3.timerservice.persistence.filestore.EjbTimerXmlPersister.INFO;
import static org.jboss.as.ejb3.timerservice.persistence.filestore.EjbTimerXmlPersister.INITIAL_DATE;
import static org.jboss.as.ejb3.timerservice.persistence.filestore.EjbTimerXmlPersister.NAME;
import static org.jboss.as.ejb3.timerservice.persistence.filestore.EjbTimerXmlPersister.NEXT_DATE;
import static org.jboss.as.ejb3.timerservice.persistence.filestore.EjbTimerXmlPersister.PARAMETER;
import static org.jboss.as.ejb3.timerservice.persistence.filestore.EjbTimerXmlPersister.PREVIOUS_RUN;
import static org.jboss.as.ejb3.timerservice.persistence.filestore.EjbTimerXmlPersister.PRIMARY_KEY;
import static org.jboss.as.ejb3.timerservice.persistence.filestore.EjbTimerXmlPersister.REPEAT_INTERVAL;
import static org.jboss.as.ejb3.timerservice.persistence.filestore.EjbTimerXmlPersister.SCHEDULE_EXPR_DAY_OF_MONTH;
import static org.jboss.as.ejb3.timerservice.persistence.filestore.EjbTimerXmlPersister.SCHEDULE_EXPR_DAY_OF_WEEK;
import static org.jboss.as.ejb3.timerservice.persistence.filestore.EjbTimerXmlPersister.SCHEDULE_EXPR_END_DATE;
import static org.jboss.as.ejb3.timerservice.persistence.filestore.EjbTimerXmlPersister.SCHEDULE_EXPR_HOUR;
import static org.jboss.as.ejb3.timerservice.persistence.filestore.EjbTimerXmlPersister.SCHEDULE_EXPR_MINUTE;
import static org.jboss.as.ejb3.timerservice.persistence.filestore.EjbTimerXmlPersister.SCHEDULE_EXPR_MONTH;
import static org.jboss.as.ejb3.timerservice.persistence.filestore.EjbTimerXmlPersister.SCHEDULE_EXPR_SECOND;
import static org.jboss.as.ejb3.timerservice.persistence.filestore.EjbTimerXmlPersister.SCHEDULE_EXPR_START_DATE;
import static org.jboss.as.ejb3.timerservice.persistence.filestore.EjbTimerXmlPersister.SCHEDULE_EXPR_TIMEZONE;
import static org.jboss.as.ejb3.timerservice.persistence.filestore.EjbTimerXmlPersister.SCHEDULE_EXPR_YEAR;
import static org.jboss.as.ejb3.timerservice.persistence.filestore.EjbTimerXmlPersister.TIMED_OBJECT_ID;
import static org.jboss.as.ejb3.timerservice.persistence.filestore.EjbTimerXmlPersister.TIMEOUT_METHOD;
import static org.jboss.as.ejb3.timerservice.persistence.filestore.EjbTimerXmlPersister.TIMER;
import static org.jboss.as.ejb3.timerservice.persistence.filestore.EjbTimerXmlPersister.TIMER_ID;
import static org.jboss.as.ejb3.timerservice.persistence.filestore.EjbTimerXmlPersister.TIMER_STATE;
import static org.jboss.as.ejb3.timerservice.persistence.filestore.EjbTimerXmlPersister.TYPE;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.as.ejb3.timerservice.CalendarTimer;
import org.jboss.as.ejb3.timerservice.TimerImpl;
import org.jboss.as.ejb3.timerservice.TimerServiceImpl;
import org.jboss.as.ejb3.timerservice.TimerState;
import org.jboss.as.ejb3.timerservice.persistence.TimeoutMethod;
import org.jboss.marshalling.ByteBufferInput;
import org.jboss.marshalling.MarshallerFactory;
import org.jboss.marshalling.MarshallingConfiguration;
import org.jboss.marshalling.Unmarshaller;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLExtendedStreamReader;

/**
 * Parser for persistent EJB timers that are stored in XML.
 *
 * @author Stuart Douglas
 */
public class EjbTimerXmlParser_1_0 implements XMLElementReader<List<TimerImpl>> {

    public static final String NAMESPACE = "urn:jboss:wildfly:ejb-timers:1.0";

    private final TimerServiceImpl timerService;
    private final MarshallerFactory factory;
    private final MarshallingConfiguration configuration;
    private final ClassLoader classLoader;

    public EjbTimerXmlParser_1_0(TimerServiceImpl timerService, MarshallerFactory factory, MarshallingConfiguration configuration, ClassLoader classLoader) {
        this.timerService = timerService;
        this.factory = factory;
        this.configuration = configuration;
        this.classLoader = classLoader;
    }

    @Override
    public void readElement(XMLExtendedStreamReader reader, List<TimerImpl> timers) throws XMLStreamException {
        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case END_ELEMENT: {
                    return;
                }
                case START_ELEMENT: {
                    switch (reader.getName().getLocalPart()) {
                        case TIMER:
                            this.parseTimer(reader, timers);
                            break;
                        case CALENDAR_TIMER:
                            this.parseCalendarTimer(reader, timers);
                            break;
                        default:
                            throw ParseUtils.unexpectedElement(reader);
                    }
                    break;
                }
            }
        }
    }

    private void parseTimer(XMLExtendedStreamReader reader, List<TimerImpl> timers) throws XMLStreamException {
        LoadableElements loadableElements = new LoadableElements();
        TimerImpl.Builder builder = TimerImpl.builder();
        builder.setPersistent(true);
        final Set<String> required = new HashSet<>(Arrays.asList(new String[]{TIMED_OBJECT_ID, TIMER_ID, INITIAL_DATE, REPEAT_INTERVAL, TIMER_STATE}));
        for (int i = 0; i < reader.getAttributeCount(); ++i) {
            String attr = reader.getAttributeValue(i);
            String attrName = reader.getAttributeLocalName(i);
            required.remove(attrName);
            boolean handled = handleCommonAttributes(builder, reader,  i);
            if (!handled) {
                switch (attrName) {
                    case REPEAT_INTERVAL:
                        builder.setRepeatInterval(Long.parseLong(attr));
                        break;
                    default:
                        throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }
        if (!required.isEmpty()) {
            throw ParseUtils.missingRequired(reader, required);
        }

        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case END_ELEMENT: {
                    try {
                        if (loadableElements.info != null) {
                            builder.setInfo((Serializable) deserialize(loadableElements.info));
                        }
                        if (loadableElements.primaryKey != null) {
                            builder.setPrimaryKey(deserialize(loadableElements.primaryKey));
                        }
                        timers.add(builder.build(timerService));
                    } catch (Exception e) {
                        EjbLogger.EJB3_TIMER_LOGGER.timerReinstatementFailed(builder.getTimedObjectId(), builder.getId(), e);
                    }
                    return;
                }
                case START_ELEMENT: {
                    boolean handled = handleCommonElements(reader, loadableElements);
                    if (!handled) {
                        throw ParseUtils.unexpectedElement(reader);
                    }
                    break;
                }
            }
        }
    }

    private Object deserialize(final String info) throws IOException, ClassNotFoundException {

        byte[] data = Base64.getDecoder().decode(info.trim());
        Unmarshaller unmarshaller = factory.createUnmarshaller(configuration);
        unmarshaller.start(new ByteBufferInput(ByteBuffer.wrap(data)));
        try {
            return unmarshaller.readObject();
        } finally {
            unmarshaller.close();
        }
    }

    private boolean handleCommonElements(XMLExtendedStreamReader reader, LoadableElements builder) throws XMLStreamException {
        boolean handled = false;
        switch (reader.getName().getLocalPart()) {
            case INFO: {
                builder.info = reader.getElementText();
                handled = true;
                break;
            }
            case PRIMARY_KEY: {
                builder.primaryKey = reader.getElementText();
                handled = true;
                break;
            }
        }
        return handled;
    }


    private void parseCalendarTimer(XMLExtendedStreamReader reader, List<TimerImpl> timers) throws XMLStreamException {
        LoadableElements loadableElements = new LoadableElements();
        CalendarTimer.Builder builder = CalendarTimer.builder();
        builder.setAutoTimer(false).setPersistent(true);
        final Set<String> required = new HashSet<>(Arrays.asList(new String[]{
                TIMED_OBJECT_ID,
                TIMER_ID,
                TIMER_STATE,
                SCHEDULE_EXPR_SECOND,
                SCHEDULE_EXPR_MINUTE,
                SCHEDULE_EXPR_HOUR,
                SCHEDULE_EXPR_DAY_OF_WEEK,
                SCHEDULE_EXPR_DAY_OF_MONTH,
                SCHEDULE_EXPR_MONTH,
                SCHEDULE_EXPR_YEAR}));
        for (int i = 0; i < reader.getAttributeCount(); ++i) {
            String attr = reader.getAttributeValue(i);
            String attrName = reader.getAttributeLocalName(i);
            required.remove(attrName);
            boolean handled = handleCommonAttributes(builder, reader, i);
            if (!handled) {
                switch (attrName) {
                    case SCHEDULE_EXPR_SECOND:
                        builder.setScheduleExprSecond(attr);
                        break;
                    case SCHEDULE_EXPR_MINUTE:
                        builder.setScheduleExprMinute(attr);
                        break;
                    case SCHEDULE_EXPR_HOUR:
                        builder.setScheduleExprHour(attr);
                        break;
                    case SCHEDULE_EXPR_DAY_OF_WEEK:
                        builder.setScheduleExprDayOfWeek(attr);
                        break;
                    case SCHEDULE_EXPR_DAY_OF_MONTH:
                        builder.setScheduleExprDayOfMonth(attr);
                        break;
                    case SCHEDULE_EXPR_MONTH:
                        builder.setScheduleExprMonth(attr);
                        break;
                    case SCHEDULE_EXPR_YEAR:
                        builder.setScheduleExprYear(attr);
                        break;
                    case SCHEDULE_EXPR_START_DATE:
                        builder.setScheduleExprStartDate(new Date(Long.parseLong(attr)));
                        break;
                    case SCHEDULE_EXPR_END_DATE:
                        builder.setScheduleExprEndDate(new Date(Long.parseLong(attr)));
                        break;
                    case SCHEDULE_EXPR_TIMEZONE:
                        builder.setScheduleExprTimezone(attr);
                        break;
                    default:
                        throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }
        if (!required.isEmpty()) {
            throw ParseUtils.missingRequired(reader, required);
        }

        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case END_ELEMENT: {
                    try {
                        if (loadableElements.info != null) {
                            builder.setInfo((Serializable) deserialize(loadableElements.info));
                        }
                        if (loadableElements.primaryKey != null) {
                            builder.setPrimaryKey(deserialize(loadableElements.primaryKey));
                        }
                        if (loadableElements.methodName != null) {
                            Method timeoutMethod = CalendarTimer.getTimeoutMethod(new TimeoutMethod(loadableElements.className, loadableElements.methodName, loadableElements.params.toArray(new String[loadableElements.params.size()])), classLoader);
                            if(timeoutMethod != null) {
                                builder.setTimeoutMethod(timeoutMethod);
                                timers.add(builder.build(timerService));
                            } else {
                                builder.setId("deleted-timer");
                                timers.add(builder.build(timerService));
                                EjbLogger.EJB3_TIMER_LOGGER.timerReinstatementFailed(builder.getTimedObjectId(), builder.getId(), null);
                            }
                        } else {
                            timers.add(builder.build(timerService));
                        }
                    } catch (Exception e) {
                        EjbLogger.EJB3_TIMER_LOGGER.timerReinstatementFailed(builder.getTimedObjectId(), builder.getId(), e);
                    }
                    return;
                }
                case START_ELEMENT: {
                    boolean handled = handleCommonElements(reader, loadableElements);
                    if (!handled) {
                        switch (reader.getName().getLocalPart()) {
                            case TIMEOUT_METHOD: {
                                builder.setAutoTimer(true);
                                parseTimeoutMethod(reader, loadableElements);
                                break;
                            }
                            default:
                                throw ParseUtils.unexpectedElement(reader);
                        }
                    }
                }
            }
        }

    }

    private void parseTimeoutMethod(XMLExtendedStreamReader reader, LoadableElements loadableElements) throws XMLStreamException {

        final Set<String> required = new HashSet<>(Arrays.asList(new String[]{DECLARING_CLASS, NAME}));
        for (int i = 0; i < reader.getAttributeCount(); ++i) {
            String attr = reader.getAttributeValue(i);
            String attrName = reader.getAttributeLocalName(i);
            required.remove(attrName);
            switch (attrName) {
                case DECLARING_CLASS:
                    loadableElements.className = attr;
                    break;
                case NAME:
                    loadableElements.methodName = attr;
                    break;
                default:
                    throw ParseUtils.unexpectedAttribute(reader, i);
            }
        }

        if (!required.isEmpty()) {
            throw ParseUtils.missingRequired(reader, required);
        }

        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case END_ELEMENT: {
                    return;
                }
                case START_ELEMENT: {
                    switch (reader.getName().getLocalPart()) {
                        case PARAMETER: {
                            handleParam(reader, loadableElements);
                            break;
                        }
                        default:
                            throw ParseUtils.unexpectedElement(reader);
                    }

                }
            }
        }

    }

    private void handleParam(XMLExtendedStreamReader reader, LoadableElements loadableElements) throws XMLStreamException {
        final Set<String> required = new HashSet<>(Arrays.asList(new String[]{TYPE}));
        for (int i = 0; i < reader.getAttributeCount(); ++i) {
            String attr = reader.getAttributeValue(i);
            String attrName = reader.getAttributeLocalName(i);
            required.remove(attrName);
            switch (attrName) {
                case TYPE:
                    loadableElements.params.add(attr);
                    break;
                default:
                    throw ParseUtils.unexpectedAttribute(reader, i);
            }
        }

        if (!required.isEmpty()) {
            throw ParseUtils.missingRequired(reader, required);
        }

        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case END_ELEMENT: {
                    return;
                }
                case START_ELEMENT: {
                    throw ParseUtils.unexpectedElement(reader);
                }
            }
        }

    }


    private boolean handleCommonAttributes(TimerImpl.Builder builder, XMLExtendedStreamReader reader, int i) {
        boolean handled = true;
        String attr = reader.getAttributeValue(i);
        switch (reader.getAttributeLocalName(i)) {
            case TIMED_OBJECT_ID:
                builder.setTimedObjectId(attr);
                break;
            case TIMER_ID:
                builder.setId(attr);
                break;
            case INITIAL_DATE:
                builder.setInitialDate(new Date(Long.parseLong(attr)));
                break;
            case NEXT_DATE:
                builder.setNextDate(new Date(Long.parseLong(attr)));
                break;
            case TIMER_STATE:
                builder.setTimerState(TimerState.valueOf(attr));
                break;
            case PREVIOUS_RUN:
                builder.setPreviousRun(new Date(Long.parseLong(attr)));
                break;
            default:
                handled = false;
        }
        return handled;
    }

    private static class LoadableElements {
        String info;
        String primaryKey;
        String className;
        String methodName;
        final List<String> params = new ArrayList<>();


    }
}

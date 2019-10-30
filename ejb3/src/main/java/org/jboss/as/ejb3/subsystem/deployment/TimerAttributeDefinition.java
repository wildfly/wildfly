/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ejb3.subsystem.deployment;


import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NILLABLE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.UNIT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE_TYPE;
import static org.jboss.as.ejb3.logging.EjbLogger.ROOT_LOGGER;

import java.util.Date;
import java.util.Locale;
import java.util.ResourceBundle;
import javax.ejb.EJBException;
import javax.ejb.NoSuchObjectLocalException;
import javax.ejb.ScheduleExpression;
import javax.ejb.Timer;
import javax.ejb.TimerService;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.jboss.as.controller.ListAttributeDefinition;
import org.jboss.as.controller.ObjectListAttributeDefinition;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.operations.validation.ModelTypeValidator;
import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.as.ejb3.component.EJBComponent;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
/**
 * Attribute definition for the list of timers associated with an EJB.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
// TODO Convert to ObjectListAttributeDefinition
public class TimerAttributeDefinition extends ListAttributeDefinition {

    public static final TimerAttributeDefinition INSTANCE = new TimerAttributeDefinition.Builder().build();

    public static final String TIME_REMAINING = "time-remaining";
    public static final String NEXT_TIMEOUT = "next-timeout";
    public static final String CALENDAR_TIMER = "calendar-timer";
    public static final String PERSISTENT = "persistent";
    public static final String SCHEDULE = "schedule";
    public static final String DAY_OF_MONTH = "day-of-month";
    public static final String DAY_OF_WEEK = "day-of-week";
    public static final String HOUR = "hour";
    public static final String MINUTE = "minute";
    public static final String SECOND = "second";
    public static final String MONTH = "month";
    public static final String YEAR = "year";
    public static final String TIMEZONE = "timezone";
    public static final String START = "start";
    public static final String END = "end";

    private TimerAttributeDefinition(Builder builder) {
        super(builder);
    }
    public static final class Builder extends ListAttributeDefinition.Builder<ObjectListAttributeDefinition.Builder, TimerAttributeDefinition>{
        public Builder() {
            super("timers", false);
        }

        @Override
        public TimerAttributeDefinition build() {
            setValidator(new ModelTypeValidator(ModelType.OBJECT));
            setStorageRuntime();

            return new TimerAttributeDefinition(this);
        }
    }


    @Override
    protected void addValueTypeDescription(ModelNode node, ResourceBundle bundle) {
        throw EjbLogger.ROOT_LOGGER.resourceBundleDescriptionsNotSupported(getName());
    }

    @Override
    protected void addAttributeValueTypeDescription(ModelNode node, ResourceDescriptionResolver resolver, Locale locale, ResourceBundle bundle) {
        addValueTypeDescription(node, resolver, locale, bundle);
    }

    @Override
    protected void addOperationParameterValueTypeDescription(ModelNode node, String operationName, ResourceDescriptionResolver resolver, Locale locale, ResourceBundle bundle) {
        addValueTypeDescription(node, resolver, locale, bundle);
    }

    @Override
    public void marshallAsElement(ModelNode resourceModel, final boolean marshalDefault, XMLStreamWriter writer) throws XMLStreamException {
        throw EjbLogger.ROOT_LOGGER.runtimeAttributeNotMarshallable(getName());
    }

    private void addValueTypeDescription(ModelNode node, ResourceDescriptionResolver resolver, Locale locale, ResourceBundle bundle) {
        final ModelNode valueTypeNode = node.get(ModelDescriptionConstants.VALUE_TYPE);
        addAttributeDescription(resolver, locale, bundle, valueTypeNode, ModelType.LONG, true, MeasurementUnit.MILLISECONDS, TIME_REMAINING);
        addAttributeDescription(resolver, locale, bundle, valueTypeNode, ModelType.LONG, true, MeasurementUnit.EPOCH_MILLISECONDS, NEXT_TIMEOUT);
        addAttributeDescription(resolver, locale, bundle, valueTypeNode, ModelType.BOOLEAN, true, null, CALENDAR_TIMER);
        addAttributeDescription(resolver, locale, bundle, valueTypeNode, ModelType.BOOLEAN, true, null, PERSISTENT);
        final ModelNode sched = addAttributeDescription(resolver, locale, bundle, valueTypeNode, ModelType.OBJECT, true, null, SCHEDULE);
        final ModelNode schedValType = sched.get(VALUE_TYPE);
        addAttributeDescription(resolver, locale, bundle, schedValType, ModelType.STRING, true, null, SCHEDULE, YEAR);
        addAttributeDescription(resolver, locale, bundle, schedValType, ModelType.STRING, true, null, SCHEDULE, MONTH);
        addAttributeDescription(resolver, locale, bundle, schedValType, ModelType.STRING, true, null, SCHEDULE, DAY_OF_MONTH);
        addAttributeDescription(resolver, locale, bundle, schedValType, ModelType.STRING, true, null, SCHEDULE, DAY_OF_WEEK);
        addAttributeDescription(resolver, locale, bundle, schedValType, ModelType.STRING, true, null, SCHEDULE, HOUR);
        addAttributeDescription(resolver, locale, bundle, schedValType, ModelType.STRING, true, null, SCHEDULE, MINUTE);
        addAttributeDescription(resolver, locale, bundle, schedValType, ModelType.STRING, true, null, SCHEDULE, SECOND);
        addAttributeDescription(resolver, locale, bundle, schedValType, ModelType.STRING, true, null, SCHEDULE, TIMEZONE);
        addAttributeDescription(resolver, locale, bundle, schedValType, ModelType.LONG, true, MeasurementUnit.EPOCH_MILLISECONDS, SCHEDULE, START);
        addAttributeDescription(resolver, locale, bundle, schedValType, ModelType.LONG, true, MeasurementUnit.EPOCH_MILLISECONDS, SCHEDULE, END);
    }

    private ModelNode addAttributeDescription(final ResourceDescriptionResolver resolver, final Locale locale, final ResourceBundle bundle,
                                              final ModelNode node, final ModelType type, final boolean nillable,
                                              final MeasurementUnit measurementUnit, final String... suffixes) {
        final ModelNode valNode = node.get(suffixes[suffixes.length -1]);
        valNode.get(DESCRIPTION).set(resolver.getResourceAttributeValueTypeDescription(getName(), locale, bundle, suffixes));
        valNode.get(TYPE).set(type);
        valNode.get(NILLABLE).set(nillable);
        if (measurementUnit != null) {
            valNode.get(UNIT).set(measurementUnit.getName());
        }
        return valNode;
    }

    public static void addTimers(final EJBComponent ejb, final ModelNode response) {
        response.setEmptyList();
        final String name = ejb.getComponentName();
        TimerService ts = ejb.getTimerService();
        if (ts != null) {
            for (Timer timer : ts.getTimers()) {
                ModelNode timerNode = response.add();
                addTimeRemaining(timer, timerNode, name);
                addNextTimeout(timer, timerNode, name);
                addCalendarTimer(timer, timerNode, name);
                addPersistent(timer, timerNode, name);
                addSchedule(timer, timerNode, name);
            }
        }
    }

    private static void addTimeRemaining(Timer timer, ModelNode timerNode, final String componentName) {
        try {
            final ModelNode detailNode = timerNode.get(TIME_REMAINING);
            long time = timer.getTimeRemaining();
            detailNode.set(time);
        } catch (IllegalStateException e) {
            // ignore
        } catch (NoSuchObjectLocalException e) {
            // ignore
        } catch (EJBException e) {
            logTimerFailure(componentName, e);
        }
    }

    private static void addNextTimeout(Timer timer, ModelNode timerNode, final String componentName) {
        try {
            final ModelNode detailNode = timerNode.get(NEXT_TIMEOUT);
            Date d = timer.getNextTimeout();
            if (d != null) {
                detailNode.set(d.getTime());
            }
        } catch (IllegalStateException e) {
            // ignore
        } catch (NoSuchObjectLocalException e) {
            // ignore
        } catch (EJBException e) {
            logTimerFailure(componentName, e);
        }
    }

    private static void addSchedule(Timer timer, ModelNode timerNode, final String componentName) {
        try {
            final ModelNode schedNode = timerNode.get(SCHEDULE);
            ScheduleExpression sched = timer.getSchedule();
            addScheduleDetailString(schedNode, sched.getYear(), YEAR);
            addScheduleDetailString(schedNode, sched.getMonth(), MONTH);
            addScheduleDetailString(schedNode, sched.getDayOfMonth(), DAY_OF_MONTH);
            addScheduleDetailString(schedNode, sched.getDayOfWeek(), DAY_OF_WEEK);
            addScheduleDetailString(schedNode, sched.getHour(), HOUR);
            addScheduleDetailString(schedNode, sched.getMinute(), MINUTE);
            addScheduleDetailString(schedNode, sched.getSecond(), SECOND);
            addScheduleDetailString(schedNode, sched.getTimezone(), TIMEZONE);
            addScheduleDetailDate(schedNode, sched.getStart(), START);
            addScheduleDetailDate(schedNode, sched.getEnd(), END);
        } catch (IllegalStateException e) {
            // ignore
        } catch (NoSuchObjectLocalException e) {
            // ignore
        } catch (EJBException e) {
            logTimerFailure(componentName, e);
        }
    }

    private static void addCalendarTimer(Timer timer, ModelNode timerNode, final String componentName) {
        try {
            final ModelNode detailNode = timerNode.get(CALENDAR_TIMER);
            boolean b = timer.isCalendarTimer();
            detailNode.set(b);
        } catch (IllegalStateException e) {
            // ignore
        } catch (NoSuchObjectLocalException e) {
            // ignore
        } catch (EJBException e) {
            logTimerFailure(componentName, e);
        }
    }

    private static void addPersistent(Timer timer, ModelNode timerNode, final String componentName) {
        try {
            final ModelNode detailNode = timerNode.get(PERSISTENT);
            boolean b = timer.isPersistent();
            detailNode.set(b);
        } catch (IllegalStateException e) {
            // ignore
        } catch (NoSuchObjectLocalException e) {
            // ignore
        } catch (EJBException e) {
            logTimerFailure(componentName, e);
        }
    }

    private static void addScheduleDetailString(ModelNode schedNode, String value, String detailName) {
        final ModelNode node = schedNode.get(detailName);
        if (value != null) {
            node.set(value);
        }
    }

    private static void addScheduleDetailDate(ModelNode schedNode, Date value, String detailName) {
        final ModelNode node = schedNode.get(detailName);
        if (value != null) {
            node.set(value.getTime());
        }
    }

    private static void logTimerFailure(final String componentName, final EJBException e) {
        ROOT_LOGGER.failToReadTimerInformation(componentName);
    }
}

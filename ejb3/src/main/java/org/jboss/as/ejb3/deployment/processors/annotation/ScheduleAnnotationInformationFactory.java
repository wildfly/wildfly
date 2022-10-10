/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.ejb3.deployment.processors.annotation;

import jakarta.ejb.Schedule;
import jakarta.ejb.Schedules;

import org.jboss.as.ee.metadata.ClassAnnotationInformationFactory;
import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.as.ejb3.timerservice.spi.AutoTimer;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.metadata.property.PropertyReplacer;

/**
 * {@link org.jboss.as.ee.metadata.ClassAnnotationInformation} for Schedule annotation
 *
 * @author Stuart Douglas
 */
public class ScheduleAnnotationInformationFactory extends ClassAnnotationInformationFactory<Schedule, AutoTimer> {

    public ScheduleAnnotationInformationFactory() {
        super(Schedule.class, Schedules.class);
    }

    @Override
    protected AutoTimer fromAnnotation(final AnnotationInstance annotationInstance, final PropertyReplacer propertyReplacer) {
        final AutoTimer timer = new AutoTimer();

        for (AnnotationValue av : annotationInstance.values()) {
            switch (ScheduleValues.valueOf(av.name())) {
                case dayOfMonth:
                    timer.getScheduleExpression().dayOfMonth(propertyReplacer.replaceProperties(av.asString()));
                    break;
                case dayOfWeek:
                    timer.getScheduleExpression().dayOfWeek(propertyReplacer.replaceProperties(av.asString()));
                    break;
                case hour:
                    timer.getScheduleExpression().hour(propertyReplacer.replaceProperties(av.asString()));
                    break;
                case info:
                    timer.getTimerConfig().setInfo(propertyReplacer.replaceProperties(av.asString()));
                    break;
                case minute:
                    timer.getScheduleExpression().minute(propertyReplacer.replaceProperties(av.asString()));
                    break;
                case month:
                    timer.getScheduleExpression().month(propertyReplacer.replaceProperties(av.asString()));
                    break;
                case persistent:
                    timer.getTimerConfig().setPersistent(av.asBoolean());
                    break;
                case second:
                    timer.getScheduleExpression().second(propertyReplacer.replaceProperties(av.asString()));
                    break;
                case timezone:
                    timer.getScheduleExpression().timezone(propertyReplacer.replaceProperties(av.asString()));
                    break;
                case year:
                    timer.getScheduleExpression().year(propertyReplacer.replaceProperties(av.asString()));
                    break;
                default:
                    throw EjbLogger.ROOT_LOGGER.invalidScheduleValue(av.name(), av.value().toString());
            }
        }
        return timer;
    }

    /**
     * Attribute names of {@code jakarta.ejb.Schedule} annotation.
     * Enum instance names must match {@code Schedule} annotation field names.
     */
    private enum ScheduleValues {
        dayOfMonth,
        dayOfWeek,
        hour,
        info,
        minute,
        month,
        persistent,
        second,
        timezone,
        year
    }
}

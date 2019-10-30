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

import javax.ejb.Schedule;
import javax.ejb.Schedules;

import org.jboss.as.ee.metadata.ClassAnnotationInformationFactory;
import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.as.ejb3.timerservice.AutoTimer;
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
        for (ScheduleValues schedulePart : ScheduleValues.values()) {
            schedulePart.set(timer, annotationInstance, propertyReplacer);
        }
        return timer;
    }


    enum ScheduleValues {
        DAY_OF_MONTH("dayOfMonth", "*") {
            protected void setString(final AutoTimer timer, final String value) {
                timer.getScheduleExpression().dayOfMonth(value);
            }
        },

        DAY_OF_WEEK("dayOfWeek", "*") {
            protected void setString(final AutoTimer timer, final String value) {
                timer.getScheduleExpression().dayOfWeek(value);
            }
        },
        HOUR("hour", "0") {
            protected void setString(final AutoTimer timer, final String value) {
                timer.getScheduleExpression().hour(value);
            }
        },
        INFO("info", null) {
            protected void setString(final AutoTimer timer, final String value) {
                timer.getTimerConfig().setInfo(value);
            }
        },
        MINUTE("minute", "0") {
            protected void setString(final AutoTimer timer, final String value) {
                timer.getScheduleExpression().minute(value);
            }
        },
        MONTH("month", "*") {
            protected void setString(final AutoTimer timer, final String value) {
                timer.getScheduleExpression().month(value);
            }
        },
        PERSISTENT("persistent", true) {
            protected void setBoolean(final AutoTimer timer, final boolean value) {
                timer.getTimerConfig().setPersistent(value);
            }
        },
        SECOND("second", "0") {
            protected void setString(final AutoTimer timer, final String value) {
                timer.getScheduleExpression().second(value);
            }
        },
        TIMEZONE("timezone", "") {
            protected void setString(final AutoTimer timer, final String value) {
                timer.getScheduleExpression().timezone(value);
            }
        },
        YEAR("year", "*") {
            protected void setString(final AutoTimer timer, final String value) {
                timer.getScheduleExpression().year(value);
            }
        },;

        private final String name;
        private final String defaultStringValue;
        private final boolean defaultBooleanValue;
        private final boolean booleanValue;

        ScheduleValues(final String name, final String defaultStringValue) {
            this.name = name;
            this.defaultStringValue = defaultStringValue;
            this.defaultBooleanValue = false;
            this.booleanValue = false;
        }

        ScheduleValues(final String name, final boolean defaultBooleanValue) {
            this.name = name;
            this.defaultStringValue = null;
            this.defaultBooleanValue = defaultBooleanValue;
            this.booleanValue = true;
        }

        public void set(final AutoTimer timer, final AnnotationInstance annotationInstance, final PropertyReplacer propertyReplacer) {
            final AnnotationValue value = annotationInstance.value(name);
            if (booleanValue) {
                if (value == null) {
                    setBoolean(timer, defaultBooleanValue);
                } else {
                    setBoolean(timer, value.asBoolean());
                }
            } else {
                if (value == null) {
                    setString(timer, defaultStringValue);
                } else {
                    setString(timer, propertyReplacer.replaceProperties(value.asString()));
                }
            }
        }

        protected void setString(final AutoTimer expression, final String value) {
            throw EjbLogger.ROOT_LOGGER.shouldBeOverridden();
        }

        protected void setBoolean(final AutoTimer expression, final boolean value) {
            throw EjbLogger.ROOT_LOGGER.shouldBeOverridden();
        }
    }
}

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
package org.jboss.as.ejb3.deployment.processors;

import org.jboss.as.ejb3.component.session.SessionBeanComponentDescription;
import org.jboss.as.ejb3.timerservice.AutoTimer;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.annotation.CompositeIndex;
import org.jboss.invocation.proxy.MethodIdentifier;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;
import org.jboss.logging.Logger;

import javax.ejb.Schedule;
import javax.ejb.Schedules;
import javax.ejb.Timeout;
import java.util.List;
import java.util.Map;

/**
 * Processes the @Timeout annotation on an EJB, and adds the configurator that is responsible for loading the method from the reflection index.
 * <p/>
 * The processor also handles auto timers (the @Schedule annotation)
 *
 * @author Stuart Douglas
 */
public class TimeoutAnnotationProcessor extends AbstractAnnotationEJBProcessor<SessionBeanComponentDescription> {

    private static final DotName TIMEOUT_ANNOTATION = DotName.createSimple(Timeout.class.getName());
    private static final DotName SCHEDULE_ANNOTATION = DotName.createSimple(Schedule.class.getName());
    private static final DotName SCHEDULES_ANNOTATION = DotName.createSimple(Schedules.class.getName());

    private static final Logger logger = Logger.getLogger(TimeoutAnnotationProcessor.class);

    private final boolean enabled;

    public TimeoutAnnotationProcessor(final boolean timerServiceEnabled) {
        enabled = timerServiceEnabled;
    }

    protected Class<SessionBeanComponentDescription> getComponentDescriptionType() {
        return SessionBeanComponentDescription.class;
    }

    protected void processAnnotations(final ClassInfo beanClass, final CompositeIndex compositeIndex, final SessionBeanComponentDescription componentDescription) throws DeploymentUnitProcessingException {

        processTimeoutAnnotation(beanClass, compositeIndex, componentDescription);
        processScheduleAnnotation(beanClass, compositeIndex, componentDescription);
    }


    private void processTimeoutAnnotation(final ClassInfo beanClass, final CompositeIndex compositeIndex, final SessionBeanComponentDescription componentDescription) throws DeploymentUnitProcessingException {

        final Map<DotName, List<AnnotationInstance>> classAnnotations = beanClass.annotations();
        if (classAnnotations != null) {
            List<AnnotationInstance> annotations = classAnnotations.get(TIMEOUT_ANNOTATION);
            if (annotations != null) {
                for (AnnotationInstance annotationInstance : annotations) {
                    AnnotationTarget target = annotationInstance.target();
                    if (target instanceof MethodInfo) {
                        componentDescription.setTimeoutMethodIdentifier(getMethodIdentifier(target));
                        return;
                    }
                }
            }
        }
        //if not found look to the super class
        final DotName superName = beanClass.superName();
        if (superName != null) {
            ClassInfo superClass = compositeIndex.getClassByName(superName);
            if (superClass != null) {
                processTimeoutAnnotation(superClass, compositeIndex, componentDescription);
            }
        }
    }

    private void processScheduleAnnotation(final ClassInfo beanClass, final CompositeIndex compositeIndex, final SessionBeanComponentDescription componentDescription) throws DeploymentUnitProcessingException {

        final Map<DotName, List<AnnotationInstance>> classAnnotations = beanClass.annotations();
        if (classAnnotations != null) {
            List<AnnotationInstance> annotations = classAnnotations.get(SCHEDULE_ANNOTATION);
            if (annotations != null) {
                for (AnnotationInstance annotationInstance : annotations) {
                    if (enabled) {
                        AnnotationTarget target = annotationInstance.target();
                        if (target instanceof MethodInfo) {
                            final MethodIdentifier identifier = getMethodIdentifier(target);
                            final AutoTimer timer = new AutoTimer();
                            for (ScheduleValues schedulePart : ScheduleValues.values()) {
                                schedulePart.set(timer, annotationInstance);
                            }
                            componentDescription.addScheduleMethodIdentifier(identifier, timer);
                        }
                    } else {
                        logger.warn("@Schedule annotation found on " + annotationInstance.target() + " but timer service is not enabled");
                    }
                }
            }
            List<AnnotationInstance> schedules = classAnnotations.get(SCHEDULES_ANNOTATION);
            if (schedules != null) {
                for (AnnotationInstance annotationInstance : schedules) {
                    AnnotationTarget target = annotationInstance.target();
                    if (enabled) {
                        if (target instanceof MethodInfo) {

                            final MethodIdentifier identifier = getMethodIdentifier(target);
                            final AnnotationInstance[] values = annotationInstance.value().asNestedArray();
                            for (AnnotationInstance schedule : values) {

                                final AutoTimer timer = new AutoTimer();
                                for (ScheduleValues schedulePart : ScheduleValues.values()) {
                                    schedulePart.set(timer, schedule);
                                }
                                componentDescription.addScheduleMethodIdentifier(identifier, timer);
                            }
                        }
                    } else {
                        logger.warn("@Schedules annotation found on " + annotationInstance.target() + " but timer service is not enabled");
                    }
                }
            }
        }
        //not the super class
        //TODO: the spec does not really seem to say how to deal with overriden methods
        final DotName superName = beanClass.superName();
        if (superName != null) {
            ClassInfo superClass = compositeIndex.getClassByName(superName);
            if (superClass != null) {
                processScheduleAnnotation(superClass, compositeIndex, componentDescription);
            }
        }
    }

    private MethodIdentifier getMethodIdentifier(final AnnotationTarget target) {
        final MethodInfo methodInfo = MethodInfo.class.cast(target);
        final String[] args = new String[methodInfo.args().length];
        for (int i = 0; i < methodInfo.args().length; i++) {
            args[i] = methodInfo.args()[i].name().toString();
        }
        return MethodIdentifier.getIdentifier(methodInfo.returnType().name().toString(), methodInfo.name(), args);
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

        public void set(final AutoTimer timer, final AnnotationInstance annotationInstance) {
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
                    setString(timer, value.asString());
                }
            }
        }

        protected void setString(final AutoTimer expression, final String value) {
            throw new IllegalStateException("Should be overridden");
        }

        protected void setBoolean(final AutoTimer expression, final boolean value) {
            throw new IllegalStateException("Should be overridden");
        }
    }
}

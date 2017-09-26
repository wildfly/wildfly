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

import java.util.Date;

import javax.ejb.NoMoreTimeoutsException;
import javax.ejb.ScheduleExpression;
import javax.ejb.TimerHandle;

import org.jboss.as.controller.ObjectListAttributeDefinition;
import org.jboss.as.controller.ObjectTypeAttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.as.ejb3.component.EJBComponent;
import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.as.ejb3.subsystem.EJB3Extension;
import org.jboss.as.ejb3.subsystem.EJB3SubsystemModel;
import org.jboss.as.ejb3.timerservice.TimerHandleImpl;
import org.jboss.as.ejb3.timerservice.TimerImpl;
import org.jboss.as.ejb3.timerservice.TimerServiceImpl;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * {@link org.jboss.as.controller.ResourceDefinition} for the timer resource for runtime ejb deployment. This definition declares operations and
 * attributes of single timer.
 *
 * @author baranowb
 */
public class TimerResourceDefinition<T extends EJBComponent> extends SimpleResourceDefinition {

    private static final ResourceDescriptionResolver RESOURCE_DESCRIPTION_RESOLVER = EJB3Extension
            .getResourceDescriptionResolver(EJB3SubsystemModel.TIMER);
    // attributes, copy of TimerAttributeDefinition
    private static final SimpleAttributeDefinition TIME_REMAINING = new SimpleAttributeDefinitionBuilder("time-remaining",
            ModelType.LONG, true).setStorageRuntime().build();

    private static final SimpleAttributeDefinition NEXT_TIMEOUT = new SimpleAttributeDefinitionBuilder("next-timeout",
            ModelType.LONG, true).setStorageRuntime().build();

    private static final SimpleAttributeDefinition CALENDAR_TIMER = new SimpleAttributeDefinitionBuilder("calendar-timer",
            ModelType.BOOLEAN, true).setStorageRuntime().build();

    private static final SimpleAttributeDefinition PERSISTENT = new SimpleAttributeDefinitionBuilder("persistent",
            ModelType.BOOLEAN, true).setStorageRuntime().build();

    private static final SimpleAttributeDefinition ACTIVE = new SimpleAttributeDefinitionBuilder("active", ModelType.BOOLEAN,
            true).setStorageRuntime().build();

    // schedule and its children
    private static final SimpleAttributeDefinition DAY_OF_MONTH = new SimpleAttributeDefinitionBuilder("day-of-month",
            ModelType.STRING, true).setStorageRuntime().build();

    private static final SimpleAttributeDefinition DAY_OF_WEEK = new SimpleAttributeDefinitionBuilder("day-of-week",
            ModelType.STRING, true).setStorageRuntime().build();

    private static final SimpleAttributeDefinition HOUR = new SimpleAttributeDefinitionBuilder("hour", ModelType.STRING, true)
            .setStorageRuntime().build();

    private static final SimpleAttributeDefinition MINUTE = new SimpleAttributeDefinitionBuilder("minute", ModelType.STRING,
            true).setStorageRuntime().build();

    private static final SimpleAttributeDefinition SECOND = new SimpleAttributeDefinitionBuilder("second", ModelType.STRING,
            true).setStorageRuntime().build();

    private static final SimpleAttributeDefinition MONTH = new SimpleAttributeDefinitionBuilder("month", ModelType.STRING, true)
            .setStorageRuntime().build();

    private static final SimpleAttributeDefinition YEAR = new SimpleAttributeDefinitionBuilder("year", ModelType.STRING, true)
            .setStorageRuntime().build();

    private static final SimpleAttributeDefinition TIMEZONE = new SimpleAttributeDefinitionBuilder("timezone",
            ModelType.STRING, true).setStorageRuntime().build();

    private static final SimpleAttributeDefinition START = new SimpleAttributeDefinitionBuilder("start", ModelType.LONG, true)
            .setStorageRuntime().build();

    private static final SimpleAttributeDefinition END = new SimpleAttributeDefinitionBuilder("end", ModelType.LONG, true)
            .setStorageRuntime().build();

    public static final ObjectListAttributeDefinition SCHEDULE = ObjectListAttributeDefinition.Builder.of(
            "schedule",
            ObjectTypeAttributeDefinition.Builder.of("schedule", YEAR, MONTH, DAY_OF_MONTH, DAY_OF_WEEK, HOUR, MINUTE, SECOND,
                    TIMEZONE, START, END).build()).build();

    // TimerConfig.info
    private static final SimpleAttributeDefinition INFO = new SimpleAttributeDefinitionBuilder("info", ModelType.STRING, true)
            .setStorageRuntime().build();

    private static final SimpleAttributeDefinition PRIMARY_KEY = new SimpleAttributeDefinitionBuilder("primary-key",
            ModelType.STRING, true).setStorageRuntime().build();

    // operations
    private static final OperationDefinition SUSPEND = new SimpleOperationDefinitionBuilder("suspend",
            RESOURCE_DESCRIPTION_RESOLVER).setRuntimeOnly().build();

    private static final OperationDefinition ACTIVATE = new SimpleOperationDefinitionBuilder("activate",
            RESOURCE_DESCRIPTION_RESOLVER).setRuntimeOnly().build();

    private static final OperationDefinition CANCEL = new SimpleOperationDefinitionBuilder("cancel",
            RESOURCE_DESCRIPTION_RESOLVER).setRuntimeOnly().build();

    private static final OperationDefinition TRIGGER = new SimpleOperationDefinitionBuilder("trigger",
            RESOURCE_DESCRIPTION_RESOLVER).setRuntimeOnly().build();

    private final AbstractEJBComponentRuntimeHandler<T> parentHandler;

    TimerResourceDefinition(AbstractEJBComponentRuntimeHandler<T> parentHandler) {
        super(EJB3SubsystemModel.TIMER_PATH, RESOURCE_DESCRIPTION_RESOLVER, null, null, OperationEntry.Flag.RESTART_NONE,
                OperationEntry.Flag.RESTART_RESOURCE_SERVICES);
        this.parentHandler = parentHandler;
    }

    @Override
    public void registerChildren(ManagementResourceRegistration resourceRegistration) {
        super.registerChildren(resourceRegistration);
    }

    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        super.registerOperations(resourceRegistration);
        resourceRegistration.registerOperationHandler(SUSPEND, new AbstractTimerHandler() {

            @Override
            void executeRuntime(OperationContext context, ModelNode operation) throws OperationFailedException {
                final TimerImpl timer = getTimer(context, operation);
                timer.suspend();
                context.completeStep(new OperationContext.RollbackHandler() {

                    @Override
                    public void handleRollback(OperationContext context, ModelNode operation) {
                        timer.scheduleTimeout(true);
                    }
                });
            }
        });

        resourceRegistration.registerOperationHandler(ACTIVATE, new AbstractTimerHandler() {

            @Override
            void executeRuntime(OperationContext context, ModelNode operation) throws OperationFailedException {
                final TimerImpl timer = getTimer(context, operation);
                if (!timer.isActive()) {
                    timer.scheduleTimeout(true);
                    context.completeStep(new OperationContext.RollbackHandler() {

                        @Override
                        public void handleRollback(OperationContext context, ModelNode operation) {
                            timer.suspend();
                        }
                    });
                } else {
                    throw EjbLogger.ROOT_LOGGER.timerIsActive(timer);
                }
            }
        });

        resourceRegistration.registerOperationHandler(CANCEL, new AbstractTimerHandler() {

            @Override
            void executeRuntime(OperationContext context, ModelNode operation) throws OperationFailedException {
                final TimerImpl timer = getTimer(context, operation);
                // this is TX aware
                timer.cancel();
                context.completeStep(OperationContext.RollbackHandler.NOOP_ROLLBACK_HANDLER);
            }
        });

        resourceRegistration.registerOperationHandler(TRIGGER, new AbstractTimerHandler() {

            @Override
            void executeRuntime(OperationContext context, ModelNode operation) throws OperationFailedException {
                // This will invoke timer in 'management-handler-thread'
                final TimerImpl timer = getTimer(context, operation);
                try {
                    timer.invokeOneOff();
                } catch (Exception e) {
                    throw EjbLogger.ROOT_LOGGER.timerInvocationFailed(e);
                }
                context.completeStep(OperationContext.RollbackHandler.NOOP_ROLLBACK_HANDLER);
            }
        });
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        super.registerAttributes(resourceRegistration);

        resourceRegistration.registerReadOnlyAttribute(TIME_REMAINING, new AbstractReadAttributeHandler() {

            @Override
            protected void readAttribute(TimerImpl timer, ModelNode toSet) {
                if (timer.isCanceled()) {
                    return;
                }
                try {
                    final long time = timer.getTimeRemaining();
                    toSet.set(time);
                } catch(NoMoreTimeoutsException nmte) {
                    // leave undefined
                    // the same will occur for next-timeout attribute, but let's log it only once
                    if(EjbLogger.ROOT_LOGGER.isDebugEnabled())
                        EjbLogger.ROOT_LOGGER.debug("No more timeouts for timer " + timer);
                }
            }

        });
        resourceRegistration.registerReadOnlyAttribute(NEXT_TIMEOUT, new AbstractReadAttributeHandler() {

            @Override
            protected void readAttribute(TimerImpl timer, ModelNode toSet) {
                if (timer.isCanceled()) {
                    return;
                }
                try {
                    final Date d = timer.getNextTimeout();
                    if (d != null) {
                        toSet.set(d.getTime());
                    }
                } catch(NoMoreTimeoutsException ignored) {
                    // leave undefined
                }
            }

        });
        resourceRegistration.registerReadOnlyAttribute(CALENDAR_TIMER, new AbstractReadAttributeHandler() {

            @Override
            protected void readAttribute(TimerImpl timer, ModelNode toSet) {
                if (timer.isCanceled()) {
                    return;
                }
                final boolean calendarTimer = timer.isCalendarTimer();
                toSet.set(calendarTimer);
            }

        });
        resourceRegistration.registerReadOnlyAttribute(PERSISTENT, new AbstractReadAttributeHandler() {

            @Override
            protected void readAttribute(TimerImpl timer, ModelNode toSet) {
                if (timer.isCanceled()) {
                    return;
                }
                final boolean persistent = timer.isPersistent();
                toSet.set(persistent);
            }

        });
        resourceRegistration.registerReadOnlyAttribute(ACTIVE, new AbstractReadAttributeHandler() {

            @Override
            protected void readAttribute(TimerImpl timer, ModelNode toSet) {
                final boolean active = timer.isActive();
                toSet.set(active);
            }

        });
        resourceRegistration.registerReadOnlyAttribute(SCHEDULE, new AbstractReadAttributeHandler() {

            @Override
            protected void readAttribute(TimerImpl timer, ModelNode toSet) {
                if (timer.isCanceled() || !timer.isCalendarTimer()) {
                    return;
                }
                ScheduleExpression sched = timer.getSchedule();
                addString(toSet, sched.getYear(), YEAR.getName());
                addString(toSet, sched.getMonth(), MONTH.getName());
                addString(toSet, sched.getDayOfMonth(), DAY_OF_MONTH.getName());
                addString(toSet, sched.getDayOfWeek(), DAY_OF_WEEK.getName());
                addString(toSet, sched.getHour(), HOUR.getName());
                addString(toSet, sched.getMinute(), MINUTE.getName());
                addString(toSet, sched.getSecond(), SECOND.getName());
                addString(toSet, sched.getTimezone(), TIMEZONE.getName());
                addDate(toSet, sched.getStart(), START.getName());
                addDate(toSet, sched.getEnd(), END.getName());
            }

            private void addString(ModelNode schedNode, String value, String name) {
                final ModelNode node = schedNode.get(name);
                if (value != null) {
                    node.set(value);
                }
            }

            private void addDate(ModelNode schedNode, Date value, String name) {
                final ModelNode node = schedNode.get(name);
                if (value != null) {
                    node.set(value.getTime());
                }
            }

        });
        resourceRegistration.registerReadOnlyAttribute(PRIMARY_KEY, new AbstractReadAttributeHandler() {

            @Override
            protected void readAttribute(TimerImpl timer, ModelNode toSet) {
                if (timer.isCanceled()) {
                    return;
                }
                final Object pk = timer.getPrimaryKey();
                if (pk != null) {
                    toSet.set(pk.toString());
                }
            }

        });
        resourceRegistration.registerReadOnlyAttribute(INFO, new AbstractReadAttributeHandler() {

            @Override
            protected void readAttribute(TimerImpl timer, ModelNode toSet) {
                if (timer.isCanceled()) {
                    return;
                }
                if (timer.getInfo() != null) {
                    toSet.set(timer.getInfo().toString());
                }
            }

        });
    }

    private abstract class AbstractTimerHandler implements OperationStepHandler {

        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {

            if (context.isNormalServer()) {
                context.addStep(new OperationStepHandler() {
                    @Override
                    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                        executeRuntime(context, operation);
                    }
                }, OperationContext.Stage.RUNTIME);
            }
        }
        protected TimerImpl getTimer(final OperationContext context, final ModelNode operation) throws OperationFailedException {
            final T ejbcomponent = parentHandler.getComponent(context, operation);
            final TimerServiceImpl timerService = (TimerServiceImpl) ejbcomponent.getTimerService();

            final PathAddress address = PathAddress.pathAddress(operation.require(ModelDescriptionConstants.OP_ADDR));
            final String timerId = address.getLastElement().getValue();
            final String timedInvokerObjectId = timerService.getTimedObjectInvoker().getValue().getTimedObjectId();
            final TimerHandle handle = new TimerHandleImpl(timerId, timedInvokerObjectId, timerService);
            try {
                return timerService.getTimer(handle);
            } catch (Exception e) {
                throw new OperationFailedException(e, null);
            }
        }

        abstract void executeRuntime(OperationContext context, ModelNode operation) throws OperationFailedException;
    }

    private abstract class AbstractReadAttributeHandler extends AbstractTimerHandler {

        void executeRuntime(final OperationContext context, final ModelNode operation) throws OperationFailedException {
            final String opName = operation.require(ModelDescriptionConstants.OP).asString();
            if (!opName.equals(ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION)) {
                throw EjbLogger.ROOT_LOGGER.unknownOperations(opName);
            }

            final TimerImpl timer = getTimer(context, operation);

            if(timer != null) {
                //the timer can expire at any point, so protect against an NPE
                readAttribute(timer, context.getResult());
            }
            context.completeStep(OperationContext.RollbackHandler.NOOP_ROLLBACK_HANDLER);
        }

        protected abstract void readAttribute(final TimerImpl timer, final ModelNode toSet);
    }
}

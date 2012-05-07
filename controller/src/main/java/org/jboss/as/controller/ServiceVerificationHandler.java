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

package org.jboss.as.controller;

import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.AbstractServiceListener;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceListener;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartException;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import static org.jboss.as.controller.ControllerMessages.MESSAGES;

/**
 * Tracks the status of a service installed by an {@link OperationStepHandler}, recording a failure desription
 * if the service has a problme starting.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ServiceVerificationHandler extends AbstractServiceListener<Object> implements ServiceListener<Object>, OperationStepHandler {
    private final Set<ServiceController<?>> set = new HashSet<ServiceController<?>>();
    private final Set<ServiceController<?>> failed = new HashSet<ServiceController<?>>();
    private final Set<ServiceController<?>> problem = new HashSet<ServiceController<?>>();
    private int outstanding;

    public synchronized void execute(final OperationContext context, final ModelNode operation) {

        // Wait for services to reach rest state.
        // Additionally...
        // Temp workaround to MSC issue of geting STARTING_TO_STARTED notification for parent service before
        // getting the PROBLEM_TO_START_REQUESTED notification for dependent services. If there are
        // services in PROBLEM state, wait up to 100ms to give them a chance to transition to START_REQUESTED.

        long start = 0;
        long settleTime = 100;
        while (outstanding > 0 || (settleTime > 0 && !problem.isEmpty())) {
            try {
                long wait = outstanding > 0 ? 0 : settleTime;
                wait(wait);
                if (outstanding == 0) {
                    if (start == 0) {
                        start = System.currentTimeMillis();
                    } else {
                        settleTime -= System.currentTimeMillis() - start;
                    }
                } else {
                    start = 0;
                    settleTime = 100;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                context.getFailureDescription().set(MESSAGES.operationCancelled());
                context.completeStep();
                return;
            }
        }

        if (!failed.isEmpty() || !problem.isEmpty()) {
            final ModelNode failureDescription = context.getFailureDescription();
            ModelNode failedList = null;
            for (ServiceController<?> controller : failed) {
                if (failedList == null) {
                    failedList = failureDescription.get(MESSAGES.failedServices());
                }
                failedList.get(controller.getName().getCanonicalName()).set(getServiceFailureDescription(controller.getStartException()));
            }
            ModelNode problemList = null;
            for (ServiceController<?> controller : problem) {
                if (!controller.getImmediateUnavailableDependencies().isEmpty()) {
                    if (problemList == null) {
                        problemList = failureDescription.get(MESSAGES.servicesMissingDependencies());
                    }
                    final StringBuilder problem = new StringBuilder();
                    problem.append(controller.getName().getCanonicalName());
                    for(Iterator<ServiceName> i = controller.getImmediateUnavailableDependencies().iterator(); i.hasNext(); ) {
                        ServiceName missing = i.next();
                        problem.append(missing.getCanonicalName());
                        if(i.hasNext()) {
                            problem.append(", ");
                        }
                    }
                    problem.append(MESSAGES.servicesMissing(problem));
                    problemList.add(problem.toString());
                }
            }
            if (context.isRollbackOnRuntimeFailure()) {
                context.setRollbackOnly();
            }
        }
        for (ServiceController<?> controller : set) {
            controller.removeListener(this);
        }
        context.completeStep(OperationContext.RollbackHandler.NOOP_ROLLBACK_HANDLER);
    }

    public synchronized void listenerAdded(final ServiceController<?> controller) {
        set.add(controller);
        if (!controller.getSubstate().isRestState()) {
            outstanding++;
        }
    }

    public synchronized void transition(final ServiceController<?> controller, final ServiceController.Transition transition) {
        switch (transition) {
            case STARTING_to_START_FAILED: {
                failed.add(controller);
                break;
            }
            case START_FAILED_to_STARTING: {
                failed.remove(controller);
                break;
            }
            case START_REQUESTED_to_PROBLEM: {
                problem.add(controller);
                break;
            }
            case PROBLEM_to_START_REQUESTED: {
                problem.remove(controller);
                break;
            }
        }
        if (transition.leavesRestState()) {
            outstanding++;
        } else if (transition.entersRestState()) {
            if (outstanding-- == 1) {
                notifyAll();
            }
        }
    }

    private static ModelNode getServiceFailureDescription(final StartException exception) {
        final ModelNode result = new ModelNode();
        if (exception != null) {
            StringBuilder sb = new StringBuilder(exception.toString());
            Throwable cause = exception.getCause();
            while (cause != null) {
                sb.append("\n    Caused by: ");
                sb.append(cause.toString());
                cause = cause.getCause();
            }
            result.set(sb.toString());
        }
        return result;
    }
}

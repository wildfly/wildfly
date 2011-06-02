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

import java.util.HashSet;
import java.util.Set;

import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.AbstractServiceListener;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceListener;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ServiceVerificationHandler extends AbstractServiceListener<Object> implements ServiceListener<Object>, NewStepHandler {
    private final Set<ServiceController<?>> set = new HashSet<ServiceController<?>>();
    private final Set<ServiceController<?>> failed = new HashSet<ServiceController<?>>();
    private final Set<ServiceController<?>> problem = new HashSet<ServiceController<?>>();
    private int outstanding;

    public synchronized void execute(final NewOperationContext context, final ModelNode operation) {

        //TODO remove this
        if (context.isBooting()) {
            context.completeStep();
            return;
        }

        while (outstanding > 0) {
            try {
                wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                context.getFailureDescription().set("Operation cancelled");
                context.completeStep();
                return;
            }
        }
        if (! failed.isEmpty() || ! problem.isEmpty()) {
            final ModelNode failureDescription = context.getFailureDescription();
            final ModelNode failedList = failureDescription.get("failed");
            for (ServiceController<?> controller : failed) {
                failedList.get(controller.getName().getCanonicalName()).set(controller.getStartException().toString());
            }
            final ModelNode problemList = failureDescription.get("transitive-problem");
            for (ServiceController<?> controller : problem) {
                problemList.add(controller.getName().getCanonicalName());
            }
            if (NewModelControllerImpl.RB_ON_RT_FAILURE.get() == Boolean.TRUE) {
                context.setRollbackOnly();
            }
        }
        for (ServiceController<?> controller : set) {
            controller.removeListener(this);
        }
        context.completeStep();
    }

    public synchronized void listenerAdded(final ServiceController<?> controller) {
        set.add(controller);
        if (! controller.getSubstate().isRestState()) {
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
            outstanding ++;
        } else if (transition.entersRestState()) {
            if (outstanding -- == 1) {
                notifyAll();
            }
        }
    }
}

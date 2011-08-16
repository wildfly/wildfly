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

package org.jboss.as.ejb3.subsystem;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;

import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.CORE_THREADS;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.MAX_THREADS;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.PATH;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.RELATIVE_TO;

import java.util.Locale;

/**
 * Adds the timer service
 * <p/>
 * @author Stuart Douglas
 */
public class TimerServiceAdd implements OperationStepHandler, DescriptionProvider {

    public static final TimerServiceAdd INSTANCE = new TimerServiceAdd();


    /**
     * Populate the <code>timerService</code> from the <code>operation</code>
     *
     * @param operation          the operation
     * @param timerServiceModel strict-max-pool ModelNode
     * @throws org.jboss.as.controller.OperationFailedException
     */

    protected void populateModel(ModelNode operation, ModelNode timerServiceModel) throws OperationFailedException {

        // max-pool-size
        if (operation.hasDefined(EJB3SubsystemModel.MAX_THREADS)) {
            int maxThreads = operation.get(EJB3SubsystemModel.MAX_THREADS).asInt();
            if (maxThreads <= 0) {
                throw new IllegalArgumentException("Invalid value: " + maxThreads + " for " + EJB3SubsystemModel.MAX_THREADS);
            }
            timerServiceModel.get(MAX_THREADS).set(maxThreads);
        }
        if (operation.hasDefined(EJB3SubsystemModel.CORE_THREADS)) {
            int coreThreads = operation.get(EJB3SubsystemModel.CORE_THREADS).asInt();
            if (coreThreads <= 0) {
                throw new IllegalArgumentException("Invalid value: " + coreThreads + " for " + EJB3SubsystemModel.CORE_THREADS);
            }
            timerServiceModel.get(CORE_THREADS).set(coreThreads);
        }

        if (operation.hasDefined(EJB3SubsystemModel.RELATIVE_TO)) {
            String relativeTo = operation.get(EJB3SubsystemModel.RELATIVE_TO).asString();
            timerServiceModel.get(RELATIVE_TO).set(relativeTo);
        }

        if (operation.hasDefined(EJB3SubsystemModel.PATH)) {
            String dataDir = operation.get(EJB3SubsystemModel.PATH).asString();
            timerServiceModel.get(PATH).set(dataDir);
        }
    }


    @Override
    public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {
        final Resource resource = context.createResource(PathAddress.pathAddress(PathElement.pathElement(EJB3SubsystemModel.TIMER_SERVICE, "default")));
        populateModel(operation, resource.getModel());
        context.completeStep();
    }

    @Override
    public ModelNode getModelDescription(Locale locale) {
        return EJB3SubsystemDescriptions.getTimerServiceAddDescription(locale);
    }
}

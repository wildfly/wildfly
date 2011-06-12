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

package org.jboss.as.domain.controller;

import org.jboss.as.controller.NewOperationContext;
import org.jboss.as.controller.NewStepHandler;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;

/**
 * Coordinates the overall execution of an operation on behalf of the domain.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class OperationCoordinatorStepHandler implements NewStepHandler {

    private final LocalHostControllerInfo localHostControllerInfo;

    OperationCoordinatorStepHandler(final LocalHostControllerInfo localHostControllerInfo) {
        this.localHostControllerInfo = localHostControllerInfo;
    }

    @Override
    public void execute(NewOperationContext context, ModelNode operation) throws OperationFailedException {
        // TODO
//        1) Determine routing
//        -- fail on writes to domain if not master
//        2) Create overall tx control
//        3) if local host affected, add DPSH step for local host
//        4) if a single other host is affected, add DPSH step for that host
//        5) if multiple hosts are affected, add a concurrent DPSH step for that host
//        6) Add a rollout plan assembly step
//        7) Add DPSH for each server (actually, for each in-series step)
//        8) Add a result creation step -- this one actually writes the response, triggers failed or succeeded

        context.completeStep();
        throw new UnsupportedOperationException();
    }
}

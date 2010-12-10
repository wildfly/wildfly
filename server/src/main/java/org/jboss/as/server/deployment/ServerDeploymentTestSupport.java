/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.server.deployment;

import org.jboss.as.model.ServerGroupDeploymentElement;
import org.jboss.as.model.UpdateResultHandler;
import org.jboss.msc.service.BatchBuilder;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceTarget;

/**
 * Test support for deployment tests.
 *
 * TODO this should be removed
 *
 * @author Emanuel Muckenhuber
 */
public class ServerDeploymentTestSupport {

    public static void deploy(ServerGroupDeploymentElement element, ServiceTarget serviceTarget, ServiceContainer container) {
        serviceTarget.addService(DeploymentUnitService.SERVICE_NAME_BASE.append(element.getUniqueName()), new DeploymentUnitService(element.getUniqueName(), null));
    }

    private static class NoOpUpdateResultHandler implements UpdateResultHandler<Void, Void> {

        private static final NoOpUpdateResultHandler INSTANCE = new NoOpUpdateResultHandler();
        @Override
        public void handleFailure(Throwable cause, Void param) {
            cause.printStackTrace();
        }
        @Override
        public void handleSuccess(Void result, Void param) {
        };
        @Override
        public void handleTimeout(Void param) {
        }
        @Override
        public void handleCancellation(Void param) {
        }
        @Override
        public void handleRollbackFailure(Throwable cause, Void param) {
        }
        @Override
        public void handleRollbackSuccess(Void param) {
        }
        @Override
        public void handleRollbackCancellation(Void param) {
        }
        @Override
        public void handleRollbackTimeout(Void param) {
        }

    }
}

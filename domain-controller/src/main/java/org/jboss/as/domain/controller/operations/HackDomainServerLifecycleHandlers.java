/*
* JBoss, Home of Professional Open Source.
* Copyright 2011, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.domain.controller.operations;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESTART_SERVERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.START_SERVERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STOP_SERVERS;

import java.util.Locale;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.domain.controller.descriptions.DomainRootDescription;
import org.jboss.as.domain.controller.descriptions.ServerGroupDescription;
import org.jboss.dmr.ModelNode;

/**
 * A skeleton operation handler for the :stop-servers, :restart-servers and :start-servers commands. This needs to go into the
 * domain model but needs access to the server inventory which lives in host-controller and thus cannot be seen.
 * Hence it delegates to the real operation step handler set up by the host controller.
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class HackDomainServerLifecycleHandlers {

    public static final String RESTART_SERVERS_NAME = RESTART_SERVERS;
    public static final String START_SERVERS_NAME = START_SERVERS;
    public static final String STOP_SERVERS_NAME = STOP_SERVERS;

    public static void initialize(OperationStepHandler stopDelegate, OperationStepHandler startDelegate, OperationStepHandler restartDelegate) {
        StopServersLifecycleHandler.DOMAIN_INSTANCE.setDelegate(stopDelegate);
        StartServersLifecycleHandler.DOMAIN_INSTANCE.setDelegate(startDelegate);
        RestartServersLifecycleHandler.DOMAIN_INSTANCE.setDelegate(restartDelegate);
        StopServersLifecycleHandler.SERVER_GROUP_INSTANCE.setDelegate(stopDelegate);
        StartServersLifecycleHandler.SERVER_GROUP_INSTANCE.setDelegate(startDelegate);
        RestartServersLifecycleHandler.SERVER_GROUP_INSTANCE.setDelegate(restartDelegate);
    }

    public static void registerDomainHandlers(ManagementResourceRegistration registration) {
        registration.registerOperationHandler(StopServersLifecycleHandler.OPERATION_NAME, StopServersLifecycleHandler.DOMAIN_INSTANCE, StopServersLifecycleHandler.DOMAIN_INSTANCE);
        registration.registerOperationHandler(StartServersLifecycleHandler.OPERATION_NAME, StartServersLifecycleHandler.DOMAIN_INSTANCE, StartServersLifecycleHandler.DOMAIN_INSTANCE);
        registration.registerOperationHandler(RestartServersLifecycleHandler.OPERATION_NAME, RestartServersLifecycleHandler.DOMAIN_INSTANCE, RestartServersLifecycleHandler.DOMAIN_INSTANCE);
    }

    public static void registerServerGroupHandlers(ManagementResourceRegistration registration) {
        registration.registerOperationHandler(StopServersLifecycleHandler.OPERATION_NAME, StopServersLifecycleHandler.SERVER_GROUP_INSTANCE, StopServersLifecycleHandler.SERVER_GROUP_INSTANCE);
        registration.registerOperationHandler(StartServersLifecycleHandler.OPERATION_NAME, StartServersLifecycleHandler.SERVER_GROUP_INSTANCE, StartServersLifecycleHandler.SERVER_GROUP_INSTANCE);
        registration.registerOperationHandler(RestartServersLifecycleHandler.OPERATION_NAME, RestartServersLifecycleHandler.SERVER_GROUP_INSTANCE, RestartServersLifecycleHandler.SERVER_GROUP_INSTANCE);
    }

    private abstract static class AbstractHackLifecyleHandler implements OperationStepHandler, DescriptionProvider {
        private volatile OperationStepHandler delegate;
        final boolean domain;

        protected AbstractHackLifecyleHandler(final boolean domain) {
            this.domain = domain;
        }

        @Override
        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
            OperationStepHandler delegate = this.delegate;
            if (delegate == null) {
                throw new IllegalStateException("No host delegate");
            }
            delegate.execute(context, operation);
        }

        /**
         * To be called when setting up the host model
         */
        void setDelegate(OperationStepHandler delegate) {
            this.delegate = delegate;
        }
    }

    private static class StopServersLifecycleHandler extends AbstractHackLifecyleHandler {
        static final String OPERATION_NAME = STOP_SERVERS_NAME;
        static final StopServersLifecycleHandler DOMAIN_INSTANCE = new StopServersLifecycleHandler(true);
        static final StopServersLifecycleHandler SERVER_GROUP_INSTANCE = new StopServersLifecycleHandler(false);

        public StopServersLifecycleHandler(final boolean domain) {
            super(domain);
        }

        @Override
        public ModelNode getModelDescription(Locale locale) {
            if (domain) {
                return DomainRootDescription.getStopServersOperation(locale);
            } else {
                return ServerGroupDescription.getStopServersOperation(locale);
            }
        }
    }

    private static class StartServersLifecycleHandler extends AbstractHackLifecyleHandler {
        static final String OPERATION_NAME = START_SERVERS_NAME;
        static final StartServersLifecycleHandler DOMAIN_INSTANCE = new StartServersLifecycleHandler(true);
        static final StartServersLifecycleHandler SERVER_GROUP_INSTANCE = new StartServersLifecycleHandler(false);

        public StartServersLifecycleHandler(final boolean domain) {
            super(domain);
        }

        @Override
        public ModelNode getModelDescription(Locale locale) {
            if (domain) {
                return DomainRootDescription.getStartServersOperation(locale);
            } else {
                return ServerGroupDescription.getStartServersOperation(locale);
            }
        }
    }

    private static class RestartServersLifecycleHandler extends AbstractHackLifecyleHandler {
        static final String OPERATION_NAME = RESTART_SERVERS_NAME;
        static final RestartServersLifecycleHandler DOMAIN_INSTANCE = new RestartServersLifecycleHandler(true);
        static final RestartServersLifecycleHandler SERVER_GROUP_INSTANCE = new RestartServersLifecycleHandler(false);

        public RestartServersLifecycleHandler(final boolean domain) {
            super(domain);
        }

        @Override
        public ModelNode getModelDescription(Locale locale) {
            if (domain) {
                return DomainRootDescription.getRestartServersOperation(locale);
            } else {
                return ServerGroupDescription.getRestartServersOperation(locale);
            }
        }
    }
}

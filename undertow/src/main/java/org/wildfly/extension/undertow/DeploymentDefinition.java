/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.undertow;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import io.undertow.server.session.SessionManager;
import io.undertow.server.session.SessionManagerStatistics;
import io.undertow.servlet.api.Deployment;
import org.jboss.as.controller.AbstractRuntimeOnlyHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.msc.service.ServiceController;
import org.wildfly.extension.undertow.deployment.UndertowDeploymentService;
import org.wildfly.extension.undertow.logging.UndertowLogger;

/**
 * @author Tomaz Cerar
 */
public class DeploymentDefinition extends SimpleResourceDefinition {
    public static final DeploymentDefinition INSTANCE = new DeploymentDefinition();

    public static final AttributeDefinition SERVER = new SimpleAttributeDefinitionBuilder("server", ModelType.STRING).setStorageRuntime().build();
    public static final AttributeDefinition CONTEXT_ROOT = new SimpleAttributeDefinitionBuilder("context-root", ModelType.STRING).setStorageRuntime().build();
    public static final AttributeDefinition VIRTUAL_HOST = new SimpleAttributeDefinitionBuilder("virtual-host", ModelType.STRING).setStorageRuntime().build();

    private DeploymentDefinition() {
        super(PathElement.pathElement(SUBSYSTEM, UndertowExtension.SUBSYSTEM_NAME),
                UndertowExtension.getResolver("deployment"));
    }


    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerReadOnlyAttribute(CONTEXT_ROOT, null);
        resourceRegistration.registerReadOnlyAttribute(VIRTUAL_HOST, null);
        resourceRegistration.registerReadOnlyAttribute(SERVER, null);
        for (SessionStat stat : SessionStat.values()) {
            resourceRegistration.registerMetric(stat.definition, SessionManagerStatsHandler.getInstance());
        }
    }

    static class SessionManagerStatsHandler extends AbstractRuntimeOnlyHandler {

        static SessionManagerStatsHandler INSTANCE = new SessionManagerStatsHandler();

        private SessionManagerStatsHandler() {
        }

        public static SessionManagerStatsHandler getInstance() {
            return INSTANCE;
        }

        @Override
        protected void executeRuntimeStep(OperationContext context, ModelNode operation) throws OperationFailedException {

            final PathAddress address = PathAddress.pathAddress(operation.get(ModelDescriptionConstants.OP_ADDR));

            final Resource web = context.readResourceFromRoot(address.subAddress(0, address.size()), false);
            final ModelNode subModel = web.getModel();

            final String host = VIRTUAL_HOST.resolveModelAttribute(context, subModel).asString();
            final String path = CONTEXT_ROOT.resolveModelAttribute(context, subModel).asString();
            final String server = SERVER.resolveModelAttribute(context, subModel).asString();

            final ServiceController<?> controller = context.getServiceRegistry(false).getService(UndertowService.deploymentServiceName(server, host, path));
            final UndertowDeploymentService deploymentService = (UndertowDeploymentService) controller.getService();
            Deployment deployment = deploymentService.getDeployment();
            SessionManager sessionManager = deployment.getSessionManager();

            SessionStat stat = SessionStat.getStat(operation.require(ModelDescriptionConstants.NAME).asString());
            SessionManagerStatistics sms = sessionManager instanceof SessionManagerStatistics ? (SessionManagerStatistics) sessionManager : null;

            if (stat == null) {
                context.getFailureDescription().set(UndertowLogger.ROOT_LOGGER.unknownMetric(operation.require(ModelDescriptionConstants.NAME).asString()));
            } else {
                ModelNode result = new ModelNode();
                switch (stat) {
                    case ACTIVE_SESSIONS:
                        result.set(sessionManager.getActiveSessions().size());
                        break;
                    case EXPIRED_SESSIONS:
                        if(sms == null) {
                            result.set(0);
                        } else {
                            result.set((int)sms.getExpiredSessionCount());
                        }
                        break;
                    case MAX_ACTIVE_SESSIONS:
                        if(sms == null) {
                            result.set(0);
                        } else {
                            result.set((int)sms.getMaxActiveSessions());
                        }
                        break;
                    case SESSIONS_CREATED:
                        if(sms == null) {
                            result.set(0);
                        } else {
                            result.set((int)sms.getCreatedSessionCount());
                        }
                        break;
                    //case DUPLICATED_SESSION_IDS:
                    //    result.set(sm.getDuplicates());
                    //    break;
                    case SESSION_AVG_ALIVE_TIME:
                        if(sms == null) {
                            result.set(0);
                        } else {
                            result.set((int)sms.getAverageSessionAliveTime());
                        }
                        break;
                    case SESSION_MAX_ALIVE_TIME:
                        if(sms == null) {
                            result.set(0);
                        } else {
                            result.set((int)sms.getMaxSessionAliveTime());
                        }
                        break;
                    case REJECTED_SESSIONS:
                        if(sms == null) {
                            result.set(0);
                        } else {
                            result.set((int)sms.getRejectedSessions());
                        }
                        break;
                    default:
                        throw new IllegalStateException(UndertowLogger.ROOT_LOGGER.unknownMetric(stat));
                }
                context.getResult().set(result);
            }

            context.stepCompleted();
        }

    }

    public enum SessionStat {
        ACTIVE_SESSIONS(new SimpleAttributeDefinitionBuilder("active-sessions", ModelType.INT, false).setStorageRuntime().build()),
        EXPIRED_SESSIONS(new SimpleAttributeDefinitionBuilder("expired-sessions", ModelType.INT, false).setStorageRuntime().build()),
        SESSIONS_CREATED(new SimpleAttributeDefinitionBuilder("sessions-created", ModelType.INT, false).setStorageRuntime().build()),
        //DUPLICATED_SESSION_IDS(new SimpleAttributeDefinition("duplicated-session-ids", ModelType.INT, false)),
        SESSION_AVG_ALIVE_TIME(new SimpleAttributeDefinitionBuilder("session-avg-alive-time", ModelType.INT, false).setStorageRuntime().build()),
        SESSION_MAX_ALIVE_TIME(new SimpleAttributeDefinitionBuilder("session-max-alive-time", ModelType.INT, false).setStorageRuntime().build()),
        REJECTED_SESSIONS(new SimpleAttributeDefinitionBuilder("rejected-sessions", ModelType.INT, false).setStorageRuntime().build()),
        MAX_ACTIVE_SESSIONS(new SimpleAttributeDefinitionBuilder("max-active-sessions", ModelType.INT, false).setStorageRuntime().build());

        private static final Map<String, SessionStat> MAP = new HashMap<>();

        static {
            for (SessionStat stat : EnumSet.allOf(SessionStat.class)) {
                MAP.put(stat.toString(), stat);
            }
        }

        final AttributeDefinition definition;

        private SessionStat(final AttributeDefinition definition) {
            this.definition = definition;
        }

        @Override
        public final String toString() {
            return definition.getName();
        }

        public static synchronized SessionStat getStat(final String stringForm) {
            return MAP.get(stringForm);
        }
    }

}

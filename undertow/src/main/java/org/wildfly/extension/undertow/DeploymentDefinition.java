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

import org.jboss.as.controller.AbstractRuntimeOnlyHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.msc.service.ServiceController;

/**
 * @author Tomaz Cerar
 * @author <a href="mailto:torben@jit-central.com">Torben Jaeger</a>
 * @created 23.2.12 18:32
 */
public class DeploymentDefinition extends SimpleResourceDefinition {
    public static final DeploymentDefinition INSTANCE = new DeploymentDefinition();

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

            final ServiceController<?> controller = context.getServiceRegistry(false).getService(UndertowService.deploymentServiceName(host, path));

            SessionStat stat = SessionStat.getStat(operation.require(ModelDescriptionConstants.NAME).asString());

            if (stat == null) {
                context.getFailureDescription().set(UndertowMessages.MESSAGES.unknownMetric(operation.require(ModelDescriptionConstants.NAME).asString()));
            } else {
                context.getResult().set("<not implemented>");
                /*final Context webContext = Context.class.cast(controller.getValue());
                ManagerBase sm = (ManagerBase) webContext.getManager();
                ModelNode result = new ModelNode();
                switch (stat) {
                    case ACTIVE_SESSIONS:
                        result.set(sm.getActiveSessions());
                        break;
                    case EXPIRED_SESSIONS:
                        result.set(sm.getExpiredSessions());
                        break;
                    case MAX_ACTIVE_SESSIONS:
                        result.set(sm.getMaxActive());
                        break;
                    case SESSIONS_CREATED:
                        result.set(sm.getSessionCounter());
                        break;
                    case DUPLICATED_SESSION_IDS:
                        result.set(sm.getDuplicates());
                        break;
                    case SESSION_AVG_ALIVE_TIME:
                        result.set(sm.getSessionAverageAliveTime());
                        break;
                    case SESSION_MAX_ALIVE_TIME:
                        result.set(sm.getSessionMaxAliveTime());
                        break;
                    case REJECTED_SESSIONS:
                        result.set(sm.getRejectedSessions());
                        break;
                    default:
                        throw new IllegalStateException(WebMessages.MESSAGES.unknownMetric(stat));
                }
                context.getResult().set(result);*/
            }

            context.stepCompleted();
        }

    }

    public enum SessionStat {
        ACTIVE_SESSIONS(new SimpleAttributeDefinition("active-sessions", ModelType.INT, false)),
        EXPIRED_SESSIONS(new SimpleAttributeDefinition("expired-sessions", ModelType.INT, false)),
        SESSIONS_CREATED(new SimpleAttributeDefinition("sessions-created", ModelType.INT, false)),
        DUPLICATED_SESSION_IDS(new SimpleAttributeDefinition("duplicated-session-ids", ModelType.INT, false)),
        SESSION_AVG_ALIVE_TIME(new SimpleAttributeDefinition("session-avg-alive-time", ModelType.INT, false)),
        SESSION_MAX_ALIVE_TIME(new SimpleAttributeDefinition("session-max-alive-time", ModelType.INT, false)),
        REJECTED_SESSIONS(new SimpleAttributeDefinition("rejected-sessions", ModelType.INT, false)),
        MAX_ACTIVE_SESSIONS(new SimpleAttributeDefinition("max-active-sessions", ModelType.INT, false));

        private static final Map<String, SessionStat> MAP = new HashMap<String, SessionStat>();

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

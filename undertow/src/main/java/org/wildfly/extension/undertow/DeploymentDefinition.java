/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.jboss.as.controller.AbstractRuntimeOnlyHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.dmr.Property;
import org.jboss.msc.service.ServiceController;
import org.wildfly.extension.undertow.deployment.UndertowDeploymentService;
import org.wildfly.extension.undertow.logging.UndertowLogger;

import io.undertow.server.session.Session;
import io.undertow.server.session.SessionManager;
import io.undertow.server.session.SessionManagerStatistics;
import io.undertow.servlet.api.Deployment;

/**
 * @author Tomaz Cerar
 */
public class DeploymentDefinition extends SimpleResourceDefinition {

    private static final ResourceDescriptionResolver DEFAULT_RESOLVER = UndertowExtension.getResolver("deployment");

    public static final DeploymentDefinition INSTANCE = new DeploymentDefinition();

    public static final AttributeDefinition SERVER = new SimpleAttributeDefinitionBuilder("server", ModelType.STRING).setStorageRuntime().build();
    public static final AttributeDefinition CONTEXT_ROOT = new SimpleAttributeDefinitionBuilder("context-root", ModelType.STRING).setStorageRuntime().build();
    public static final AttributeDefinition VIRTUAL_HOST = new SimpleAttributeDefinitionBuilder("virtual-host", ModelType.STRING).setStorageRuntime().build();
    static final AttributeDefinition SESSIOND_ID = new SimpleAttributeDefinitionBuilder(Constants.SESSION_ID, ModelType.STRING)
            .setRequired(true)
            .setAllowExpression(false)
            .build();

    static final AttributeDefinition ATTRIBUTE = new SimpleAttributeDefinitionBuilder(Constants.ATTRIBUTE, ModelType.STRING)
            .setRequired(true)
            .setAllowExpression(false)
            .build();


    static final OperationDefinition INVALIDATE_SESSION = new SimpleOperationDefinitionBuilder(Constants.INVALIDATE_SESSION, DEFAULT_RESOLVER)
            .addParameter(SESSIOND_ID)
            .setRuntimeOnly()
            .setReplyType(ModelType.BOOLEAN)
            .build();

    static final OperationDefinition LIST_SESSIONS = new SimpleOperationDefinitionBuilder(Constants.LIST_SESSIONS, DEFAULT_RESOLVER)
            .setRuntimeOnly()
            .setReplyType(ModelType.LIST)
            .setReplyValueType(ModelType.STRING)
            .build();

    static final OperationDefinition LIST_SESSION_ATTRIBUTE_NAMES = new SimpleOperationDefinitionBuilder(Constants.LIST_SESSION_ATTRIBUTE_NAMES, DEFAULT_RESOLVER)
            .setRuntimeOnly()
            .addParameter(SESSIOND_ID)
            .setReplyType(ModelType.LIST)
            .setReplyValueType(ModelType.STRING)
            .build();

    static final OperationDefinition LIST_SESSION_ATTRIBUTES = new SimpleOperationDefinitionBuilder(Constants.LIST_SESSION_ATTRIBUTES, DEFAULT_RESOLVER)
            .setRuntimeOnly()
            .addParameter(SESSIOND_ID)
            .setReplyType(ModelType.LIST)
            .setReplyValueType(ModelType.PROPERTY)
            .build();

    static final OperationDefinition GET_SESSION_ATTRIBUTE = new SimpleOperationDefinitionBuilder(Constants.GET_SESSION_ATTRIBUTE, DEFAULT_RESOLVER)
            .setRuntimeOnly()
            .addParameter(SESSIOND_ID)
            .addParameter(ATTRIBUTE)
            .setReplyType(ModelType.STRING)
            .setReplyValueType(ModelType.PROPERTY)
            .build();

    static final OperationDefinition GET_SESSION_LAST_ACCESSED_TIME = new SimpleOperationDefinitionBuilder(Constants.GET_SESSION_LAST_ACCESSED_TIME, DEFAULT_RESOLVER)
            .setRuntimeOnly()
            .addParameter(SESSIOND_ID)
            .setReplyType(ModelType.STRING)
            .build();


    static final OperationDefinition GET_SESSION_LAST_ACCESSED_TIME_MILLIS = new SimpleOperationDefinitionBuilder(Constants.GET_SESSION_LAST_ACCESSED_TIME_MILLIS, DEFAULT_RESOLVER)
            .setRuntimeOnly()
            .addParameter(SESSIOND_ID)
            .setReplyType(ModelType.LONG)
            .build();

    static final OperationDefinition GET_SESSION_CREATION_TIME = new SimpleOperationDefinitionBuilder(Constants.GET_SESSION_CREATION_TIME, DEFAULT_RESOLVER)
            .setRuntimeOnly()
            .addParameter(SESSIOND_ID)
            .setReplyType(ModelType.STRING)
            .build();


    static final OperationDefinition GET_SESSION_CREATION_TIME_MILLIS = new SimpleOperationDefinitionBuilder(Constants.GET_SESSION_CREATION_TIME_MILLIS, DEFAULT_RESOLVER)
            .setRuntimeOnly()
            .addParameter(SESSIOND_ID)
            .setReplyType(ModelType.LONG)
            .build();

    private DeploymentDefinition() {
        super(new Parameters(PathElement.pathElement(SUBSYSTEM, UndertowExtension.SUBSYSTEM_NAME), DEFAULT_RESOLVER)
                .setFeature(false));
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

    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        super.registerOperations(resourceRegistration);
        SessionManagerOperationHandler handler = new SessionManagerOperationHandler();

        resourceRegistration.registerOperationHandler(INVALIDATE_SESSION, handler);
        resourceRegistration.registerOperationHandler(LIST_SESSIONS, handler);
        resourceRegistration.registerOperationHandler(LIST_SESSION_ATTRIBUTE_NAMES, handler);
        resourceRegistration.registerOperationHandler(LIST_SESSION_ATTRIBUTES, handler);
        resourceRegistration.registerOperationHandler(GET_SESSION_ATTRIBUTE, handler);
        resourceRegistration.registerOperationHandler(GET_SESSION_LAST_ACCESSED_TIME, handler);
        resourceRegistration.registerOperationHandler(GET_SESSION_LAST_ACCESSED_TIME_MILLIS, handler);
        resourceRegistration.registerOperationHandler(GET_SESSION_CREATION_TIME, handler);
        resourceRegistration.registerOperationHandler(GET_SESSION_CREATION_TIME_MILLIS, handler);
    }

    static class SessionManagerOperationHandler extends AbstractRuntimeOnlyHandler {

        @Override
        protected void executeRuntimeStep(OperationContext operationContext, ModelNode modelNode) throws OperationFailedException {
            ModelNode result = new ModelNode();
            SessionManager sessionManager = getSessionManager(operationContext, modelNode);

            String name = modelNode.get(OP).asString();
            //list-sessions does not take a session id param
            if (name.equals(Constants.LIST_SESSIONS)) {
                result.setEmptyList();
                Set<String> sessions = sessionManager.getAllSessions();
                for (String s : sessions) {
                    result.add(s);
                }
                operationContext.getResult().set(result);
                return;
            }
            String sessionId = SESSIOND_ID.resolveModelAttribute(operationContext, modelNode).asString();
            Session session = sessionManager.getSession(sessionId);
            if (session == null && !name.equals(Constants.INVALIDATE_SESSION)) {
                throw UndertowLogger.ROOT_LOGGER.sessionNotFound(sessionId);
            }

            switch (name) {
                case Constants.INVALIDATE_SESSION: {
                    if(session == null) {
                        result.set(false);
                    } else {
                        session.invalidate(null);
                        result.set(true);
                    }
                    break;
                }
                case Constants.LIST_SESSION_ATTRIBUTE_NAMES: {
                    result.setEmptyList();
                    Set<String> sessions = session.getAttributeNames();
                    for (String s : sessions) {
                        result.add(s);
                    }
                    break;
                }
                case Constants.LIST_SESSION_ATTRIBUTES: {
                    result.setEmptyList();
                    Set<String> sessions = session.getAttributeNames();
                    for (String s : sessions) {
                        Object attribute = session.getAttribute(s);
                        ModelNode m = new ModelNode();
                        if (attribute != null) {
                            m.set(attribute.toString());
                        }
                        result.add(new Property(s, m));
                    }
                    break;
                }
                case Constants.GET_SESSION_ATTRIBUTE: {
                    String a = ATTRIBUTE.resolveModelAttribute(operationContext, modelNode).asString();
                    Object attribute = session.getAttribute(a);
                    if (attribute != null) {
                        result.set(attribute.toString());
                    }
                    break;
                }
                case Constants.GET_SESSION_LAST_ACCESSED_TIME: {
                    long accessTime = session.getLastAccessedTime();
                    result.set(DateTimeFormatter.ISO_DATE_TIME
                            .withZone(ZoneId.systemDefault())
                            .format(Instant.ofEpochMilli(accessTime)));
                    break;
                }
                case Constants.GET_SESSION_LAST_ACCESSED_TIME_MILLIS: {
                    long accessTime = session.getLastAccessedTime();
                    result.set(accessTime);
                    break;
                }
                case Constants.GET_SESSION_CREATION_TIME: {
                    long accessTime = session.getCreationTime();
                    result.set(DateTimeFormatter.ISO_DATE_TIME
                            .withZone(ZoneId.systemDefault())
                            .format(Instant.ofEpochMilli(accessTime)));
                    break;
                }
                case Constants.GET_SESSION_CREATION_TIME_MILLIS: {
                    long accessTime = session.getCreationTime();
                    result.set(accessTime);
                    break;
                }
            }

            operationContext.getResult().set(result);
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

            SessionStat stat = SessionStat.getStat(operation.require(ModelDescriptionConstants.NAME).asString());

            if (stat == null) {
                context.getFailureDescription().set(UndertowLogger.ROOT_LOGGER.unknownMetric(operation.require(ModelDescriptionConstants.NAME).asString()));
            } else {
                ModelNode result = new ModelNode();
                final ServiceController<?> controller = context.getServiceRegistry(false).getService(UndertowService.deploymentServiceName(server, host, path));
                if (controller != null && controller.getState() != ServiceController.State.UP) {//check if deployment is active at all
                    return;
                }
                final UndertowDeploymentService deploymentService = (UndertowDeploymentService) controller.getService();
                if (deploymentService == null || deploymentService.getDeployment() == null) { //we might be in shutdown and it is possible
                    return;
                }
                Deployment deployment = deploymentService.getDeployment();
                SessionManager sessionManager = deployment.getSessionManager();
                SessionManagerStatistics sms = sessionManager.getStatistics();

                switch (stat) {
                    case ACTIVE_SESSIONS:
                        result.set(sessionManager.getActiveSessions().size());
                        break;
                    case EXPIRED_SESSIONS:
                        if (sms == null) {
                            result.set(0);
                        } else {
                            result.set((int) sms.getExpiredSessionCount());
                        }
                        break;
                    case MAX_ACTIVE_SESSIONS:
                        if (sms == null) {
                            result.set(0);
                        } else {
                            result.set((int) sms.getMaxActiveSessions());
                        }
                        break;
                    case SESSIONS_CREATED:
                        if (sms == null) {
                            result.set(0);
                        } else {
                            result.set((int) sms.getCreatedSessionCount());
                        }
                        break;
                    //case DUPLICATED_SESSION_IDS:
                    //    result.set(sm.getDuplicates());
                    //    break;
                    case SESSION_AVG_ALIVE_TIME:
                        if (sms == null) {
                            result.set(0);
                        } else {
                            result.set((int) sms.getAverageSessionAliveTime() / 1000);
                        }
                        break;
                    case SESSION_MAX_ALIVE_TIME:
                        if (sms == null) {
                            result.set(0);
                        } else {
                            result.set((int) sms.getMaxSessionAliveTime() / 1000);
                        }
                        break;
                    case REJECTED_SESSIONS:
                        if (sms == null) {
                            result.set(0);
                        } else {
                            result.set((int) sms.getRejectedSessions());
                        }
                        break;
                    case HIGHEST_SESSION_COUNT:
                        if (sms == null) {
                            result.set(0);
                        } else {
                            result.set((int) sms.getHighestSessionCount());
                        }
                        break;
                    default:
                        throw new IllegalStateException(UndertowLogger.ROOT_LOGGER.unknownMetric(stat));
                }
                context.getResult().set(result);
            }
        }
    }

    private static SessionManager getSessionManager(OperationContext context, ModelNode operation) throws OperationFailedException {
        final PathAddress address = PathAddress.pathAddress(operation.get(ModelDescriptionConstants.OP_ADDR));
        final Resource web = context.readResourceFromRoot(address.subAddress(0, address.size()), false);
        final ModelNode subModel = web.getModel();
        final String host = VIRTUAL_HOST.resolveModelAttribute(context, subModel).asString();
        final String path = CONTEXT_ROOT.resolveModelAttribute(context, subModel).asString();
        final String server = SERVER.resolveModelAttribute(context, subModel).asString();

        final UndertowDeploymentService deploymentService;
        final ServiceController<?> controller = context.getServiceRegistry(false).getService(UndertowService.deploymentServiceName(server, host, path));
        if (controller != null && controller.getState() != ServiceController.State.UP) {//check if deployment is active at all
            throw UndertowLogger.ROOT_LOGGER.sessionManagerNotAvailable();
        } else {
            deploymentService = (UndertowDeploymentService) controller.getService();
            if (deploymentService == null || deploymentService.getDeployment() == null) { //we might be in shutdown and it is possible
                throw UndertowLogger.ROOT_LOGGER.sessionManagerNotAvailable();
            }
        }
        Deployment deployment = deploymentService.getDeployment();
        return deployment.getSessionManager();
    }

    public enum SessionStat {
        ACTIVE_SESSIONS(new SimpleAttributeDefinitionBuilder("active-sessions", ModelType.INT)
                .setUndefinedMetricValue(new ModelNode(0)).setStorageRuntime().build()),
        EXPIRED_SESSIONS(new SimpleAttributeDefinitionBuilder("expired-sessions", ModelType.INT)
                .setUndefinedMetricValue(new ModelNode(0)).setStorageRuntime().build()),
        SESSIONS_CREATED(new SimpleAttributeDefinitionBuilder("sessions-created", ModelType.INT)
                .setUndefinedMetricValue(new ModelNode(0)).setStorageRuntime().build()),
        //DUPLICATED_SESSION_IDS(new SimpleAttributeDefinition("duplicated-session-ids", ModelType.INT, false)),
        SESSION_AVG_ALIVE_TIME(new SimpleAttributeDefinitionBuilder("session-avg-alive-time", ModelType.INT)
                .setUndefinedMetricValue(new ModelNode(0)).setStorageRuntime().build()),
        SESSION_MAX_ALIVE_TIME(new SimpleAttributeDefinitionBuilder("session-max-alive-time", ModelType.INT)
                .setUndefinedMetricValue(new ModelNode(0)).setStorageRuntime().build()),
        REJECTED_SESSIONS(new SimpleAttributeDefinitionBuilder("rejected-sessions", ModelType.INT)
                .setUndefinedMetricValue(new ModelNode(0)).setStorageRuntime().build()),
        MAX_ACTIVE_SESSIONS(new SimpleAttributeDefinitionBuilder("max-active-sessions", ModelType.INT)
                .setUndefinedMetricValue(new ModelNode(0)).setStorageRuntime().build()),
        HIGHEST_SESSION_COUNT(new SimpleAttributeDefinitionBuilder("highest-session-count", ModelType.INT)
                .setUndefinedMetricValue(new ModelNode(0)).setStorageRuntime().build());

        private static final Map<String, SessionStat> MAP = new HashMap<>();

        static {
            for (SessionStat stat : EnumSet.allOf(SessionStat.class)) {
                MAP.put(stat.toString(), stat);
            }
        }

        final AttributeDefinition definition;

        SessionStat(final AttributeDefinition definition) {
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

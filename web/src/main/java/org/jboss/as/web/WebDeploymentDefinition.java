package org.jboss.as.web;

import org.apache.catalina.Context;
import org.apache.catalina.session.ManagerBase;
import org.jboss.as.controller.AbstractRuntimeOnlyHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.msc.service.ServiceController;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

/**
 * @author Tomaz Cerar
 * @author <a href="mailto:torben@jit-central.com">Torben Jaeger</a>
 * @created 23.2.12 18:32
 */
public class WebDeploymentDefinition extends SimpleResourceDefinition {
    public static final WebDeploymentDefinition INSTANCE = new WebDeploymentDefinition();

    private WebDeploymentDefinition() {
        super(PathElement.pathElement(SUBSYSTEM, WebExtension.SUBSYSTEM_NAME),
              WebExtension.getResourceDescriptionResolver("deployment"));
    }


    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
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

            final String host = subModel.require("virtual-host").asString();
            final String path = subModel.require("context-root").asString();

            final ServiceController<?> controller = context.getServiceRegistry(false).getService(WebSubsystemServices.deploymentServiceName(host, path));

            SessionStat stat = SessionStat.getStat(operation.require(ModelDescriptionConstants.NAME).asString());

            if (stat == null) {
                context.getFailureDescription().set(WebMessages.MESSAGES.unknownMetric(operation.require(ModelDescriptionConstants.NAME).asString()));
            } else {
                final Context webContext = Context.class.cast(controller.getValue());
                ManagerBase sm = (ManagerBase) webContext.getManager();
                ModelNode result = new ModelNode();
                switch (stat) {
                    case ACTIVE_SESSIONS:
                        // todo: what about other manager implementations?
//                  if (sm.getDistributable() && (sm instanceof DistributableSessionManager)) {
//                     result.set(((DistributableSessionManager)sm).getActiveSessionCount());
//                  }
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
                context.getResult().set(result);
            }

            context.completeStep();
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

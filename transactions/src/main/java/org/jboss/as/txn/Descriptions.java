package org.jboss.as.txn;

import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

import java.util.EnumSet;
import java.util.Locale;
import java.util.ResourceBundle;

public class Descriptions {

    static final String RESOURCE_NAME = Descriptions.class.getPackage().getName() + ".LocalDescriptions";

    static ModelNode getSubsystem(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode subsystem = new ModelNode();

        subsystem.get(ModelDescriptionConstants.DESCRIPTION).set(bundle.getString("txn"));
        subsystem.get(ModelDescriptionConstants.HEAD_COMMENT_ALLOWED).set(true);
        subsystem.get(ModelDescriptionConstants.TAIL_COMMENT_ALLOWED).set(true);
        subsystem.get(ModelDescriptionConstants.NAMESPACE).set(Namespace.TRANSACTIONS_1_0.getUriString());
        // core-environment
        subsystem.get(ModelDescriptionConstants.ATTRIBUTES, CommonAttributes.CORE_ENVIRONMENT, ModelDescriptionConstants.DESCRIPTION).set(bundle.getString("core-environment"));
        subsystem.get(ModelDescriptionConstants.ATTRIBUTES, CommonAttributes.CORE_ENVIRONMENT, ModelDescriptionConstants.TYPE).set(ModelType.OBJECT);
        subsystem.get(ModelDescriptionConstants.ATTRIBUTES, CommonAttributes.CORE_ENVIRONMENT, ModelDescriptionConstants.REQUIRED).set(true);
        // core-environment.node-identifier
        subsystem.get(ModelDescriptionConstants.ATTRIBUTES, CommonAttributes.CORE_ENVIRONMENT, ModelDescriptionConstants.VALUE_TYPE, CommonAttributes.NODE_IDENTIFIER, ModelDescriptionConstants.DESCRIPTION).set(bundle.getString("core-environment.node-identifier"));
        subsystem.get(ModelDescriptionConstants.ATTRIBUTES, CommonAttributes.CORE_ENVIRONMENT, ModelDescriptionConstants.VALUE_TYPE, CommonAttributes.NODE_IDENTIFIER, ModelDescriptionConstants.TYPE).set(ModelType.STRING);
        subsystem.get(ModelDescriptionConstants.ATTRIBUTES, CommonAttributes.CORE_ENVIRONMENT, ModelDescriptionConstants.VALUE_TYPE, CommonAttributes.NODE_IDENTIFIER, ModelDescriptionConstants.DEFAULT).set(1);
        subsystem.get(ModelDescriptionConstants.ATTRIBUTES, CommonAttributes.CORE_ENVIRONMENT, ModelDescriptionConstants.VALUE_TYPE, CommonAttributes.NODE_IDENTIFIER, ModelDescriptionConstants.REQUIRED).set(false);
        // core-environment/process-id
        subsystem.get(ModelDescriptionConstants.ATTRIBUTES, CommonAttributes.CORE_ENVIRONMENT, ModelDescriptionConstants.VALUE_TYPE, CommonAttributes.PROCESS_ID, ModelDescriptionConstants.DESCRIPTION).set(bundle.getString("core-environment.process-id"));
        subsystem.get(ModelDescriptionConstants.ATTRIBUTES, CommonAttributes.CORE_ENVIRONMENT, ModelDescriptionConstants.VALUE_TYPE, CommonAttributes.PROCESS_ID, ModelDescriptionConstants.TYPE).set(ModelType.OBJECT);
        subsystem.get(ModelDescriptionConstants.ATTRIBUTES, CommonAttributes.CORE_ENVIRONMENT, ModelDescriptionConstants.VALUE_TYPE, CommonAttributes.PROCESS_ID, ModelDescriptionConstants.MIN_LENGTH).set(1);
        subsystem.get(ModelDescriptionConstants.ATTRIBUTES, CommonAttributes.CORE_ENVIRONMENT, ModelDescriptionConstants.VALUE_TYPE, CommonAttributes.PROCESS_ID, ModelDescriptionConstants.REQUIRED).set(true);
        // core-environment/process-id/uuid
        subsystem.get(ModelDescriptionConstants.ATTRIBUTES, CommonAttributes.CORE_ENVIRONMENT, ModelDescriptionConstants.VALUE_TYPE, CommonAttributes.UUID, ModelDescriptionConstants.DESCRIPTION).set(bundle.getString("core-environment.process-id.uuid"));
        subsystem.get(ModelDescriptionConstants.ATTRIBUTES, CommonAttributes.CORE_ENVIRONMENT, ModelDescriptionConstants.VALUE_TYPE, CommonAttributes.UUID, ModelDescriptionConstants.TYPE).set(ModelType.STRING);
        subsystem.get(ModelDescriptionConstants.ATTRIBUTES, CommonAttributes.CORE_ENVIRONMENT, ModelDescriptionConstants.VALUE_TYPE, CommonAttributes.UUID, ModelDescriptionConstants.MIN_LENGTH).set(0);
        subsystem.get(ModelDescriptionConstants.ATTRIBUTES, CommonAttributes.CORE_ENVIRONMENT, ModelDescriptionConstants.VALUE_TYPE, CommonAttributes.UUID, ModelDescriptionConstants.REQUIRED).set(false);

        /* Not currently used
        subsystem.get(ATTRIBUTES, CORE_ENVIRONMENT, VALUE_TYPE, SOCKET_PROCESS_ID_MAX_PORTS, DESCRIPTION).set(bundle.getString("core-environment.socket-process-id-max-ports"));
        subsystem.get(ATTRIBUTES, CORE_ENVIRONMENT, VALUE_TYPE, SOCKET_PROCESS_ID_MAX_PORTS, TYPE).set(ModelType.INT);
        subsystem.get(ATTRIBUTES, CORE_ENVIRONMENT, VALUE_TYPE, SOCKET_PROCESS_ID_MAX_PORTS, DEFAULT).set(10);
        subsystem.get(ATTRIBUTES, CORE_ENVIRONMENT, VALUE_TYPE, SOCKET_PROCESS_ID_MAX_PORTS, REQUIRED).set(false);
        */

        subsystem.get(ModelDescriptionConstants.ATTRIBUTES, CommonAttributes.RECOVERY_ENVIRONMENT, ModelDescriptionConstants.DESCRIPTION).set(bundle.getString("recovery-environment"));
        subsystem.get(ModelDescriptionConstants.ATTRIBUTES, CommonAttributes.RECOVERY_ENVIRONMENT, ModelDescriptionConstants.TYPE).set(ModelType.OBJECT);
        subsystem.get(ModelDescriptionConstants.ATTRIBUTES, CommonAttributes.RECOVERY_ENVIRONMENT, ModelDescriptionConstants.REQUIRED).set(true);
        subsystem.get(ModelDescriptionConstants.ATTRIBUTES, CommonAttributes.RECOVERY_ENVIRONMENT, ModelDescriptionConstants.VALUE_TYPE, CommonAttributes.BINDING, ModelDescriptionConstants.DESCRIPTION).set(bundle.getString("recovery-environment.socket-binding"));
        subsystem.get(ModelDescriptionConstants.ATTRIBUTES, CommonAttributes.RECOVERY_ENVIRONMENT, ModelDescriptionConstants.VALUE_TYPE, CommonAttributes.BINDING, ModelDescriptionConstants.TYPE).set(ModelType.STRING);
        subsystem.get(ModelDescriptionConstants.ATTRIBUTES, CommonAttributes.RECOVERY_ENVIRONMENT, ModelDescriptionConstants.VALUE_TYPE, CommonAttributes.BINDING, ModelDescriptionConstants.MIN_LENGTH).set(1);
        subsystem.get(ModelDescriptionConstants.ATTRIBUTES, CommonAttributes.RECOVERY_ENVIRONMENT, ModelDescriptionConstants.VALUE_TYPE, CommonAttributes.BINDING, ModelDescriptionConstants.REQUIRED).set(true);
        subsystem.get(ModelDescriptionConstants.ATTRIBUTES, CommonAttributes.RECOVERY_ENVIRONMENT, ModelDescriptionConstants.VALUE_TYPE, CommonAttributes.STATUS_BINDING, ModelDescriptionConstants.DESCRIPTION).set(bundle.getString("recovery-environment.status-socket-binding"));
        subsystem.get(ModelDescriptionConstants.ATTRIBUTES, CommonAttributes.RECOVERY_ENVIRONMENT, ModelDescriptionConstants.VALUE_TYPE, CommonAttributes.STATUS_BINDING, ModelDescriptionConstants.TYPE).set(ModelType.STRING);
        subsystem.get(ModelDescriptionConstants.ATTRIBUTES, CommonAttributes.RECOVERY_ENVIRONMENT, ModelDescriptionConstants.VALUE_TYPE, CommonAttributes.STATUS_BINDING, ModelDescriptionConstants.MIN_LENGTH).set(1);
        subsystem.get(ModelDescriptionConstants.ATTRIBUTES, CommonAttributes.RECOVERY_ENVIRONMENT, ModelDescriptionConstants.VALUE_TYPE, CommonAttributes.STATUS_BINDING, ModelDescriptionConstants.REQUIRED).set(true);

        subsystem.get(ModelDescriptionConstants.ATTRIBUTES, CommonAttributes.RECOVERY_ENVIRONMENT, ModelDescriptionConstants.VALUE_TYPE, CommonAttributes.STATUS_BINDING, ModelDescriptionConstants.DESCRIPTION).set(bundle.getString("recovery-environment.recovery-listener"));
        subsystem.get(ModelDescriptionConstants.ATTRIBUTES, CommonAttributes.RECOVERY_ENVIRONMENT, ModelDescriptionConstants.VALUE_TYPE, CommonAttributes.STATUS_BINDING, ModelDescriptionConstants.TYPE).set(ModelType.BOOLEAN);
        subsystem.get(ModelDescriptionConstants.ATTRIBUTES, CommonAttributes.RECOVERY_ENVIRONMENT, ModelDescriptionConstants.VALUE_TYPE, CommonAttributes.STATUS_BINDING, ModelDescriptionConstants.MIN_LENGTH).set(1);
        subsystem.get(ModelDescriptionConstants.ATTRIBUTES, CommonAttributes.RECOVERY_ENVIRONMENT, ModelDescriptionConstants.VALUE_TYPE, CommonAttributes.STATUS_BINDING, ModelDescriptionConstants.REQUIRED).set(false);

        subsystem.get(ModelDescriptionConstants.ATTRIBUTES, CommonAttributes.COORDINATOR_ENVIRONMENT, ModelDescriptionConstants.DESCRIPTION).set(bundle.getString("coordinator-environment"));
        subsystem.get(ModelDescriptionConstants.ATTRIBUTES, CommonAttributes.COORDINATOR_ENVIRONMENT, ModelDescriptionConstants.TYPE).set(ModelType.OBJECT);
        subsystem.get(ModelDescriptionConstants.ATTRIBUTES, CommonAttributes.COORDINATOR_ENVIRONMENT, ModelDescriptionConstants.REQUIRED).set(false);
        subsystem.get(ModelDescriptionConstants.ATTRIBUTES, CommonAttributes.COORDINATOR_ENVIRONMENT, ModelDescriptionConstants.VALUE_TYPE, CommonAttributes.ENABLE_STATISTICS, ModelDescriptionConstants.DESCRIPTION).set(bundle.getString("coordinator-environment.enable-statistics"));
        subsystem.get(ModelDescriptionConstants.ATTRIBUTES, CommonAttributes.COORDINATOR_ENVIRONMENT, ModelDescriptionConstants.VALUE_TYPE, CommonAttributes.ENABLE_STATISTICS, ModelDescriptionConstants.TYPE).set(ModelType.BOOLEAN);
        subsystem.get(ModelDescriptionConstants.ATTRIBUTES, CommonAttributes.COORDINATOR_ENVIRONMENT, ModelDescriptionConstants.VALUE_TYPE, CommonAttributes.ENABLE_STATISTICS, ModelDescriptionConstants.REQUIRED).set(false);
        subsystem.get(ModelDescriptionConstants.ATTRIBUTES, CommonAttributes.COORDINATOR_ENVIRONMENT, ModelDescriptionConstants.VALUE_TYPE, CommonAttributes.ENABLE_STATISTICS, ModelDescriptionConstants.DEFAULT).set(true);
        subsystem.get(ModelDescriptionConstants.ATTRIBUTES, CommonAttributes.COORDINATOR_ENVIRONMENT, ModelDescriptionConstants.VALUE_TYPE, CommonAttributes.ENABLE_TSM_STATUS, ModelDescriptionConstants.DESCRIPTION).set(bundle.getString("coordinator-environment.enable-tsm-status"));
        subsystem.get(ModelDescriptionConstants.ATTRIBUTES, CommonAttributes.COORDINATOR_ENVIRONMENT, ModelDescriptionConstants.VALUE_TYPE, CommonAttributes.ENABLE_TSM_STATUS, ModelDescriptionConstants.TYPE).set(ModelType.BOOLEAN);
        subsystem.get(ModelDescriptionConstants.ATTRIBUTES, CommonAttributes.COORDINATOR_ENVIRONMENT, ModelDescriptionConstants.VALUE_TYPE, CommonAttributes.ENABLE_TSM_STATUS, ModelDescriptionConstants.REQUIRED).set(false);
        subsystem.get(ModelDescriptionConstants.ATTRIBUTES, CommonAttributes.COORDINATOR_ENVIRONMENT, ModelDescriptionConstants.VALUE_TYPE, CommonAttributes.ENABLE_TSM_STATUS, ModelDescriptionConstants.DEFAULT).set(false);
        subsystem.get(ModelDescriptionConstants.ATTRIBUTES, CommonAttributes.COORDINATOR_ENVIRONMENT, ModelDescriptionConstants.VALUE_TYPE, CommonAttributes.DEFAULT_TIMEOUT, ModelDescriptionConstants.DESCRIPTION).set(bundle.getString("coordinator-environment.default-timeout"));
        subsystem.get(ModelDescriptionConstants.ATTRIBUTES, CommonAttributes.COORDINATOR_ENVIRONMENT, ModelDescriptionConstants.VALUE_TYPE, CommonAttributes.DEFAULT_TIMEOUT, ModelDescriptionConstants.TYPE).set(ModelType.BOOLEAN);
        subsystem.get(ModelDescriptionConstants.ATTRIBUTES, CommonAttributes.COORDINATOR_ENVIRONMENT, ModelDescriptionConstants.VALUE_TYPE, CommonAttributes.DEFAULT_TIMEOUT, ModelDescriptionConstants.REQUIRED).set(false);
        subsystem.get(ModelDescriptionConstants.ATTRIBUTES, CommonAttributes.COORDINATOR_ENVIRONMENT, ModelDescriptionConstants.VALUE_TYPE, CommonAttributes.DEFAULT_TIMEOUT, ModelDescriptionConstants.DEFAULT).set(300);

        subsystem.get(ModelDescriptionConstants.ATTRIBUTES, CommonAttributes.OBJECT_STORE, ModelDescriptionConstants.DESCRIPTION).set(bundle.getString("object-store"));
        subsystem.get(ModelDescriptionConstants.ATTRIBUTES, CommonAttributes.OBJECT_STORE, ModelDescriptionConstants.TYPE).set(ModelType.OBJECT);
        subsystem.get(ModelDescriptionConstants.ATTRIBUTES, CommonAttributes.OBJECT_STORE, ModelDescriptionConstants.REQUIRED).set(false);
        subsystem.get(ModelDescriptionConstants.ATTRIBUTES, CommonAttributes.OBJECT_STORE, ModelDescriptionConstants.VALUE_TYPE, ModelDescriptionConstants.RELATIVE_TO, ModelDescriptionConstants.DESCRIPTION).set(bundle.getString("object-store.relative-to"));
        subsystem.get(ModelDescriptionConstants.ATTRIBUTES, CommonAttributes.OBJECT_STORE, ModelDescriptionConstants.VALUE_TYPE, ModelDescriptionConstants.RELATIVE_TO, ModelDescriptionConstants.TYPE).set(ModelType.STRING);
        subsystem.get(ModelDescriptionConstants.ATTRIBUTES, CommonAttributes.OBJECT_STORE, ModelDescriptionConstants.VALUE_TYPE, ModelDescriptionConstants.RELATIVE_TO, ModelDescriptionConstants.REQUIRED).set(false);
        subsystem.get(ModelDescriptionConstants.ATTRIBUTES, CommonAttributes.OBJECT_STORE, ModelDescriptionConstants.VALUE_TYPE, ModelDescriptionConstants.PATH, ModelDescriptionConstants.DESCRIPTION).set(bundle.getString("object-store.path"));
        subsystem.get(ModelDescriptionConstants.ATTRIBUTES, CommonAttributes.OBJECT_STORE, ModelDescriptionConstants.VALUE_TYPE, ModelDescriptionConstants.PATH, ModelDescriptionConstants.TYPE).set(ModelType.STRING);
        subsystem.get(ModelDescriptionConstants.ATTRIBUTES, CommonAttributes.OBJECT_STORE, ModelDescriptionConstants.VALUE_TYPE, ModelDescriptionConstants.PATH, ModelDescriptionConstants.REQUIRED).set(false);

        for (TxStatsHandler.TxStat stat : EnumSet.allOf(TxStatsHandler.TxStat.class)) {
            String statString = stat.toString();
            subsystem.get(ModelDescriptionConstants.ATTRIBUTES, statString, ModelDescriptionConstants.DESCRIPTION).set(bundle.getString(statString));
            subsystem.get(ModelDescriptionConstants.ATTRIBUTES, statString, ModelDescriptionConstants.TYPE).set(ModelType.LONG);
        }

        return subsystem;
    }

    static ModelNode getSubsystemAdd(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode op = new ModelNode();

        op.get(ModelDescriptionConstants.DESCRIPTION).set(bundle.getString("txn.add"));
        op.get(ModelDescriptionConstants.HEAD_COMMENT_ALLOWED).set(true);
        op.get(ModelDescriptionConstants.TAIL_COMMENT_ALLOWED).set(true);
        op.get(ModelDescriptionConstants.NAMESPACE).set(Namespace.TRANSACTIONS_1_0.getUriString());

        op.get(ModelDescriptionConstants.REQUEST_PROPERTIES, CommonAttributes.CORE_ENVIRONMENT, ModelDescriptionConstants.DESCRIPTION).set(bundle.getString("core-environment"));
        op.get(ModelDescriptionConstants.REQUEST_PROPERTIES, CommonAttributes.CORE_ENVIRONMENT, ModelDescriptionConstants.TYPE).set(ModelType.OBJECT);
        op.get(ModelDescriptionConstants.REQUEST_PROPERTIES, CommonAttributes.CORE_ENVIRONMENT, ModelDescriptionConstants.REQUIRED).set(true);
        op.get(ModelDescriptionConstants.REQUEST_PROPERTIES, CommonAttributes.CORE_ENVIRONMENT, ModelDescriptionConstants.VALUE_TYPE, CommonAttributes.NODE_IDENTIFIER, ModelDescriptionConstants.DESCRIPTION).set(bundle.getString("core-environment.node-identifier"));
        op.get(ModelDescriptionConstants.REQUEST_PROPERTIES, CommonAttributes.CORE_ENVIRONMENT, ModelDescriptionConstants.VALUE_TYPE, CommonAttributes.NODE_IDENTIFIER, ModelDescriptionConstants.TYPE).set(ModelType.STRING);
        op.get(ModelDescriptionConstants.REQUEST_PROPERTIES, CommonAttributes.CORE_ENVIRONMENT, ModelDescriptionConstants.VALUE_TYPE, CommonAttributes.NODE_IDENTIFIER, ModelDescriptionConstants.DEFAULT).set(1);
        op.get(ModelDescriptionConstants.REQUEST_PROPERTIES, CommonAttributes.CORE_ENVIRONMENT, ModelDescriptionConstants.VALUE_TYPE, CommonAttributes.NODE_IDENTIFIER, ModelDescriptionConstants.REQUIRED).set(false);
        // core-environment/process-id
        op.get(ModelDescriptionConstants.REQUEST_PROPERTIES, CommonAttributes.CORE_ENVIRONMENT, ModelDescriptionConstants.VALUE_TYPE, CommonAttributes.PROCESS_ID, ModelDescriptionConstants.DESCRIPTION).set(bundle.getString("core-environment.process-id"));
        op.get(ModelDescriptionConstants.REQUEST_PROPERTIES, CommonAttributes.CORE_ENVIRONMENT, ModelDescriptionConstants.VALUE_TYPE, CommonAttributes.PROCESS_ID, ModelDescriptionConstants.TYPE).set(ModelType.OBJECT);
        op.get(ModelDescriptionConstants.REQUEST_PROPERTIES, CommonAttributes.CORE_ENVIRONMENT, ModelDescriptionConstants.VALUE_TYPE, CommonAttributes.PROCESS_ID, ModelDescriptionConstants.MIN_LENGTH).set(1);
        op.get(ModelDescriptionConstants.REQUEST_PROPERTIES, CommonAttributes.CORE_ENVIRONMENT, ModelDescriptionConstants.VALUE_TYPE, CommonAttributes.PROCESS_ID, ModelDescriptionConstants.REQUIRED).set(true);
        // core-environment/process-id/uuid
        op.get(ModelDescriptionConstants.REQUEST_PROPERTIES, CommonAttributes.CORE_ENVIRONMENT, ModelDescriptionConstants.VALUE_TYPE, CommonAttributes.UUID, ModelDescriptionConstants.DESCRIPTION).set(bundle.getString("core-environment.process-id.uuid"));
        op.get(ModelDescriptionConstants.REQUEST_PROPERTIES, CommonAttributes.CORE_ENVIRONMENT, ModelDescriptionConstants.VALUE_TYPE, CommonAttributes.UUID, ModelDescriptionConstants.TYPE).set(ModelType.STRING);
        op.get(ModelDescriptionConstants.REQUEST_PROPERTIES, CommonAttributes.CORE_ENVIRONMENT, ModelDescriptionConstants.VALUE_TYPE, CommonAttributes.UUID, ModelDescriptionConstants.MIN_LENGTH).set(0);
        op.get(ModelDescriptionConstants.REQUEST_PROPERTIES, CommonAttributes.CORE_ENVIRONMENT, ModelDescriptionConstants.VALUE_TYPE, CommonAttributes.UUID, ModelDescriptionConstants.REQUIRED).set(false);

        /* Not currently used
        subsystem.get(REQUEST_PROPERTIES, CORE_ENVIRONMENT, VALUE_TYPE, SOCKET_PROCESS_ID_MAX_PORTS, DESCRIPTION).set(bundle.getString("core-environment.socket-process-id-max-ports"));
        subsystem.get(REQUEST_PROPERTIES, CORE_ENVIRONMENT, VALUE_TYPE, SOCKET_PROCESS_ID_MAX_PORTS, TYPE).set(ModelType.INT);
        subsystem.get(REQUEST_PROPERTIES, CORE_ENVIRONMENT, VALUE_TYPE, SOCKET_PROCESS_ID_MAX_PORTS, DEFAULT).set(10);
        subsystem.get(REQUEST_PROPERTIES, CORE_ENVIRONMENT, VALUE_TYPE, SOCKET_PROCESS_ID_MAX_PORTS, REQUIRED).set(false);
        */

        op.get(ModelDescriptionConstants.REQUEST_PROPERTIES, CommonAttributes.RECOVERY_ENVIRONMENT, ModelDescriptionConstants.DESCRIPTION).set(bundle.getString("recovery-environment"));
        op.get(ModelDescriptionConstants.REQUEST_PROPERTIES, CommonAttributes.RECOVERY_ENVIRONMENT, ModelDescriptionConstants.TYPE).set(ModelType.OBJECT);
        op.get(ModelDescriptionConstants.REQUEST_PROPERTIES, CommonAttributes.RECOVERY_ENVIRONMENT, ModelDescriptionConstants.REQUIRED).set(true);
        op.get(ModelDescriptionConstants.REQUEST_PROPERTIES, CommonAttributes.RECOVERY_ENVIRONMENT, ModelDescriptionConstants.VALUE_TYPE, CommonAttributes.BINDING, ModelDescriptionConstants.DESCRIPTION).set(bundle.getString("recovery-environment.socket-binding"));
        op.get(ModelDescriptionConstants.REQUEST_PROPERTIES, CommonAttributes.RECOVERY_ENVIRONMENT, ModelDescriptionConstants.VALUE_TYPE, CommonAttributes.BINDING, ModelDescriptionConstants.TYPE).set(ModelType.STRING);
        op.get(ModelDescriptionConstants.REQUEST_PROPERTIES, CommonAttributes.RECOVERY_ENVIRONMENT, ModelDescriptionConstants.VALUE_TYPE, CommonAttributes.BINDING, ModelDescriptionConstants.MIN_LENGTH).set(1);
        op.get(ModelDescriptionConstants.REQUEST_PROPERTIES, CommonAttributes.RECOVERY_ENVIRONMENT, ModelDescriptionConstants.VALUE_TYPE, CommonAttributes.BINDING, ModelDescriptionConstants.REQUIRED).set(true);
        op.get(ModelDescriptionConstants.REQUEST_PROPERTIES, CommonAttributes.RECOVERY_ENVIRONMENT, ModelDescriptionConstants.VALUE_TYPE, CommonAttributes.STATUS_BINDING, ModelDescriptionConstants.DESCRIPTION).set(bundle.getString("recovery-environment.status-socket-binding"));
        op.get(ModelDescriptionConstants.REQUEST_PROPERTIES, CommonAttributes.RECOVERY_ENVIRONMENT, ModelDescriptionConstants.VALUE_TYPE, CommonAttributes.STATUS_BINDING, ModelDescriptionConstants.TYPE).set(ModelType.STRING);
        op.get(ModelDescriptionConstants.REQUEST_PROPERTIES, CommonAttributes.RECOVERY_ENVIRONMENT, ModelDescriptionConstants.VALUE_TYPE, CommonAttributes.STATUS_BINDING, ModelDescriptionConstants.MIN_LENGTH).set(1);
        op.get(ModelDescriptionConstants.REQUEST_PROPERTIES, CommonAttributes.RECOVERY_ENVIRONMENT, ModelDescriptionConstants.VALUE_TYPE, CommonAttributes.STATUS_BINDING, ModelDescriptionConstants.REQUIRED).set(true);

        op.get(ModelDescriptionConstants.REQUEST_PROPERTIES, CommonAttributes.COORDINATOR_ENVIRONMENT, ModelDescriptionConstants.DESCRIPTION).set(bundle.getString("coordinator-environment"));
        op.get(ModelDescriptionConstants.REQUEST_PROPERTIES, CommonAttributes.COORDINATOR_ENVIRONMENT, ModelDescriptionConstants.TYPE).set(ModelType.OBJECT);
        op.get(ModelDescriptionConstants.REQUEST_PROPERTIES, CommonAttributes.COORDINATOR_ENVIRONMENT, ModelDescriptionConstants.REQUIRED).set(false);
        op.get(ModelDescriptionConstants.REQUEST_PROPERTIES, CommonAttributes.COORDINATOR_ENVIRONMENT, ModelDescriptionConstants.VALUE_TYPE, CommonAttributes.ENABLE_STATISTICS, ModelDescriptionConstants.DESCRIPTION).set(bundle.getString("coordinator-environment.enable-statistics"));
        op.get(ModelDescriptionConstants.REQUEST_PROPERTIES, CommonAttributes.COORDINATOR_ENVIRONMENT, ModelDescriptionConstants.VALUE_TYPE, CommonAttributes.ENABLE_STATISTICS, ModelDescriptionConstants.TYPE).set(ModelType.BOOLEAN);
        op.get(ModelDescriptionConstants.REQUEST_PROPERTIES, CommonAttributes.COORDINATOR_ENVIRONMENT, ModelDescriptionConstants.VALUE_TYPE, CommonAttributes.ENABLE_STATISTICS, ModelDescriptionConstants.REQUIRED).set(false);
        op.get(ModelDescriptionConstants.REQUEST_PROPERTIES, CommonAttributes.COORDINATOR_ENVIRONMENT, ModelDescriptionConstants.VALUE_TYPE, CommonAttributes.ENABLE_STATISTICS, ModelDescriptionConstants.DEFAULT).set(true);
        op.get(ModelDescriptionConstants.REQUEST_PROPERTIES, CommonAttributes.COORDINATOR_ENVIRONMENT, ModelDescriptionConstants.VALUE_TYPE, CommonAttributes.DEFAULT_TIMEOUT, ModelDescriptionConstants.DESCRIPTION).set(bundle.getString("coordinator-environment.default-timeout"));
        op.get(ModelDescriptionConstants.REQUEST_PROPERTIES, CommonAttributes.COORDINATOR_ENVIRONMENT, ModelDescriptionConstants.VALUE_TYPE, CommonAttributes.DEFAULT_TIMEOUT, ModelDescriptionConstants.TYPE).set(ModelType.INT);
        op.get(ModelDescriptionConstants.REQUEST_PROPERTIES, CommonAttributes.COORDINATOR_ENVIRONMENT, ModelDescriptionConstants.VALUE_TYPE, CommonAttributes.DEFAULT_TIMEOUT, ModelDescriptionConstants.REQUIRED).set(false);
        op.get(ModelDescriptionConstants.REQUEST_PROPERTIES, CommonAttributes.COORDINATOR_ENVIRONMENT, ModelDescriptionConstants.VALUE_TYPE, CommonAttributes.DEFAULT_TIMEOUT, ModelDescriptionConstants.DEFAULT).set(300);

        op.get(ModelDescriptionConstants.REQUEST_PROPERTIES, CommonAttributes.OBJECT_STORE, ModelDescriptionConstants.DESCRIPTION).set(bundle.getString("object-store"));
        op.get(ModelDescriptionConstants.REQUEST_PROPERTIES, CommonAttributes.OBJECT_STORE, ModelDescriptionConstants.TYPE).set(ModelType.OBJECT);
        op.get(ModelDescriptionConstants.REQUEST_PROPERTIES, CommonAttributes.OBJECT_STORE, ModelDescriptionConstants.REQUIRED).set(false);
        op.get(ModelDescriptionConstants.REQUEST_PROPERTIES, CommonAttributes.OBJECT_STORE, ModelDescriptionConstants.VALUE_TYPE, ModelDescriptionConstants.RELATIVE_TO, ModelDescriptionConstants.DESCRIPTION).set(bundle.getString("object-store.relative-to"));
        op.get(ModelDescriptionConstants.REQUEST_PROPERTIES, CommonAttributes.OBJECT_STORE, ModelDescriptionConstants.VALUE_TYPE, ModelDescriptionConstants.RELATIVE_TO, ModelDescriptionConstants.TYPE).set(ModelType.STRING);
        op.get(ModelDescriptionConstants.REQUEST_PROPERTIES, CommonAttributes.OBJECT_STORE, ModelDescriptionConstants.VALUE_TYPE, ModelDescriptionConstants.RELATIVE_TO, ModelDescriptionConstants.REQUIRED).set(false);
        op.get(ModelDescriptionConstants.REQUEST_PROPERTIES, CommonAttributes.OBJECT_STORE, ModelDescriptionConstants.VALUE_TYPE, ModelDescriptionConstants.PATH, ModelDescriptionConstants.DESCRIPTION).set(bundle.getString("object-store.path"));
        op.get(ModelDescriptionConstants.REQUEST_PROPERTIES, CommonAttributes.OBJECT_STORE, ModelDescriptionConstants.VALUE_TYPE, ModelDescriptionConstants.PATH, ModelDescriptionConstants.TYPE).set(ModelType.STRING);
        op.get(ModelDescriptionConstants.REQUEST_PROPERTIES, CommonAttributes.OBJECT_STORE, ModelDescriptionConstants.VALUE_TYPE, ModelDescriptionConstants.PATH, ModelDescriptionConstants.REQUIRED).set(false);

        op.get(ModelDescriptionConstants.REPLY_PROPERTIES).setEmptyObject();

        return op;
    }

    private static ResourceBundle getResourceBundle(Locale locale) {
        if (locale == null) {
            locale = Locale.getDefault();
        }
        return ResourceBundle.getBundle(RESOURCE_NAME, locale);
    }
}

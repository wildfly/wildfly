package org.jboss.as.txn;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REPLY_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUEST_PROPERTIES;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

public class Descriptions {

    static final String RESOURCE_NAME = Descriptions.class.getPackage().getName() + ".LocalDescriptions";

    static final List<SimpleAttributeDefinition> TransactionSubsystemAttributes = Arrays.asList(TransactionSubsystemAdd.BINDING,
            TransactionSubsystemAdd.STATUS_BINDING,
            TransactionSubsystemAdd.RECOVERY_LISTENER,
            TransactionSubsystemAdd.NODE_IDENTIFIER,
            TransactionSubsystemAdd.PROCESS_ID_UUID,
            TransactionSubsystemAdd.PROCESS_ID_SOCKET_BINDING,
            TransactionSubsystemAdd.PROCESS_ID_SOCKET_MAX_PORTS,
            TransactionSubsystemAdd.RELATIVE_TO,
            TransactionSubsystemAdd.PATH,
            TransactionSubsystemAdd.ENABLE_STATISTICS,
            TransactionSubsystemAdd.ENABLE_TSM_STATUS,
            TransactionSubsystemAdd.DEFAULT_TIMEOUT,
            TransactionSubsystemAdd.OBJECT_STORE_RELATIVE_TO,
            TransactionSubsystemAdd.OBJECT_STORE_PATH);




    static ModelNode getSubsystem(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode subsystem = new ModelNode();

        subsystem.get(ModelDescriptionConstants.DESCRIPTION).set(bundle.getString("txn"));
        subsystem.get(ModelDescriptionConstants.HEAD_COMMENT_ALLOWED).set(true);
        subsystem.get(ModelDescriptionConstants.TAIL_COMMENT_ALLOWED).set(true);
        subsystem.get(ModelDescriptionConstants.NAMESPACE).set(Namespace.TRANSACTIONS_1_0.getUriString());

        for (TxStatsHandler.TxStat stat : EnumSet.allOf(TxStatsHandler.TxStat.class)) {
            String statString = stat.toString();
            subsystem.get(ModelDescriptionConstants.ATTRIBUTES, statString, ModelDescriptionConstants.DESCRIPTION).set(bundle.getString(statString));
            subsystem.get(ModelDescriptionConstants.ATTRIBUTES, statString, ModelDescriptionConstants.TYPE).set(ModelType.LONG);
        }

        for (SimpleAttributeDefinition attribute : TransactionSubsystemAttributes) {
            attribute.addResourceAttributeDescription(bundle, null, subsystem);
        }

        return subsystem;
    }



    static ModelNode getSubsystemAdd(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode op = new ModelNode();
        op.get(ModelDescriptionConstants.DESCRIPTION).set(bundle.getString("txn.add"));
        op.get(OPERATION_NAME).set(ADD);

        for (SimpleAttributeDefinition attribute : TransactionSubsystemAttributes) {
            attribute.addOperationParameterDescription(bundle, null, op);
        }

        op.get(REPLY_PROPERTIES).setEmptyObject();

        return op;
    }


    static ModelNode getSubsystemRemove(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode op = new ModelNode();
        op.get(ModelDescriptionConstants.DESCRIPTION).set(bundle.getString("txn.remove"));
        op.get(OPERATION_NAME).set(ADD);

        op.get(REQUEST_PROPERTIES).setEmptyObject();

        op.get(REPLY_PROPERTIES).setEmptyObject();

        return op;
    }

    private static ResourceBundle getResourceBundle(Locale locale) {
        if (locale == null) {
            locale = Locale.getDefault();
        }
        return ResourceBundle.getBundle(RESOURCE_NAME, locale);
    }
}

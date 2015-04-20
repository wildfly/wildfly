package org.jboss.as.jacorb;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.jacorb.logging.JacORBLogger;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.wildfly.iiop.openjdk.AttributeConstants;
import org.wildfly.iiop.openjdk.Constants;

import java.util.LinkedList;
import java.util.List;

/**
 * @author <a href=mailto:tadamski@redhat.com>Tomasz Adamski</a>
 */
public class TransformUtils {

    private TransformUtils() {
    }

    static void checkLegacyModel(final ModelNode model) throws OperationFailedException {
        final List<String> propertiesToReject = new LinkedList<>();
        for (final AttributeDefinition attribute : JacORBSubsystemDefinitions.ON_OFF_ATTRIBUTES_TO_REJECT) {
            if (model.hasDefined(attribute.getName())
                    && model.get(attribute.getName()).equals(JacORBSubsystemDefinitions.DEFAULT_ENABLED_PROPERTY)) {
                propertiesToReject.add(attribute.getName());
            }
        }
        for (final AttributeDefinition attribute : JacORBSubsystemDefinitions.ATTRIBUTES_TO_REJECT) {
            if (model.hasDefined(attribute.getName())) {
                propertiesToReject.add(attribute.getName());
            }
        }
        if (!propertiesToReject.isEmpty()) {
            throw JacORBLogger.ROOT_LOGGER.cannotEmulateProperties(propertiesToReject);
        }
    }

    static ModelNode transformModel(final ModelNode legacyModel) {
        final ModelNode model = new ModelNode();
        for (Property property : legacyModel.asPropertyList()) {
            final String name = property.getName();
            final ModelNode legacyValue = property.getValue();
            if (property.getValue().isDefined()) {
                final ModelNode value;
                switch (name) {
                    case JacORBSubsystemConstants.ORB_GIOP_MINOR_VERSION:
                        value = new ModelNode(new StringBuilder().append("1.").append(legacyValue).toString());
                        break;
                    case JacORBSubsystemConstants.ORB_INIT_TRANSACTIONS:
                        if (legacyValue.asString().equals(JacORBSubsystemConstants.ON)) {
                            value = new ModelNode(Constants.FULL);
                        } else if (legacyValue.asString().equals(JacORBSubsystemConstants.OFF)) {
                            value = new ModelNode(Constants.NONE);
                        } else {
                            value = legacyValue;
                        }
                        break;
                    case JacORBSubsystemConstants.ORB_INIT_SECURITY:
                        if (legacyValue.asString().equals(JacORBSubsystemConstants.OFF)) {
                            value = new ModelNode(Constants.NONE);
                        } else {
                            value = legacyValue;
                        }
                        break;
                    case JacORBSubsystemConstants.SECURITY_SUPPORT_SSL:
                    case JacORBSubsystemConstants.SECURITY_ADD_COMP_VIA_INTERCEPTOR:
                    case JacORBSubsystemConstants.NAMING_EXPORT_CORBALOC:
                        if (legacyValue.asString().equals(JacORBSubsystemConstants.ON)) {
                            value = new ModelNode(AttributeConstants.TrueFalse.TRUE.toString());
                        } else {
                            value = new ModelNode(AttributeConstants.TrueFalse.FALSE.toString());
                        }
                        break;
                    default:
                        value = legacyValue;
                }
                model.get(name).set(value);
            }
        }
        return model;
    }
}

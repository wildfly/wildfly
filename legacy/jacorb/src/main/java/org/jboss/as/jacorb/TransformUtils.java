package org.jboss.as.jacorb;

import java.util.LinkedList;
import java.util.List;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.jacorb.logging.JacORBLogger;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.wildfly.iiop.openjdk.Constants;

/**
 * @author <a href=mailto:tadamski@redhat.com>Tomasz Adamski</a>
 */
public class TransformUtils {

    private TransformUtils() {
    }

    static List<String> checkLegacyModel(final ModelNode model, final boolean failOnErrors) throws OperationFailedException {
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
            if(failOnErrors) {
                throw JacORBLogger.ROOT_LOGGER.cannotEmulateProperties(propertiesToReject);
            } else {
                JacORBLogger.ROOT_LOGGER.cannotEmulatePropertiesWarning(propertiesToReject);
            }
        }
        return propertiesToReject;
    }

    static ModelNode transformModel(final ModelNode legacyModel) {
        final ModelNode model = new ModelNode();
        for (Property property : legacyModel.asPropertyList()) {
            String name = property.getName();
            final ModelNode legacyValue = property.getValue();
            if (legacyModel.isDefined()) {
                final ModelNode value;
                switch (name) {
                    case JacORBSubsystemConstants.ORB_GIOP_MINOR_VERSION:
                        name = Constants.ORB_GIOP_VERSION;
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
                            value = new ModelNode(true);
                        } else {
                            value = new ModelNode(false);
                        }
                        break;
                    default:
                        value = legacyValue;
                }
                if (!value.asString().equals(JacORBSubsystemConstants.OFF)) {
                    model.get(name).set(value);
                }
            }
        }
        return model;
    }
}

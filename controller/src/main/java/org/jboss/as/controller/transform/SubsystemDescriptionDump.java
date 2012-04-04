package org.jboss.as.controller.transform;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.descriptions.DefaultOperationDescriptionProvider;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.extension.ExtensionRegistry;
import org.jboss.as.controller.extension.SubsystemInformation;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.dmr.Property;
import org.jboss.logging.Logger;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a>
 */
public class SubsystemDescriptionDump implements OperationStepHandler {
    private static Logger log = Logger.getLogger(SubsystemDescriptionDump.class);
    private final ExtensionRegistry extensionRegistry;
    protected static final SimpleAttributeDefinition PATH = new SimpleAttributeDefinition("path", ModelType.STRING, false);
    public static final String OPERATION_NAME = "subsystem-description-dump";
    public static final DescriptionProvider DESCRIPTION = new DefaultOperationDescriptionProvider(OPERATION_NAME, new NonResolvingResourceDescriptionResolver(), PATH);

    public SubsystemDescriptionDump(final ExtensionRegistry extensionRegistry) {
        this.extensionRegistry = extensionRegistry;
    }

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        String path = PATH.resolveModelAttribute(context, operation).asString();
        dumpManagementResourceRegistration(extensionRegistry, path);
        context.completeStep();
    }

    public static void dumpManagementResourceRegistration(final ExtensionRegistry registry, final String path) throws OperationFailedException{
        ManagementResourceRegistration profile = registry.getProfileRegistration();
        try {
            for (PathElement pe : profile.getChildAddresses(PathAddress.EMPTY_ADDRESS)) {
                ManagementResourceRegistration registration = profile.getSubModel(PathAddress.pathAddress(pe));
                String subsystem = pe.getValue();
                SubsystemInformation info = registry.getSubsystemInfo(subsystem);
                ModelNode desc = readFullModelDescription(PathAddress.pathAddress(pe), registration);
                String name = subsystem + "-" + info.getManagementInterfaceMajorVersion() + "." + info.getManagementInterfaceMinorVersion() + ".dmr";
                PrintWriter pw = new PrintWriter(new File(path, name));
                desc.writeString(pw, false);
                pw.close();
            }
        } catch (IOException e) {
            throw new OperationFailedException("could not save,", e);
        }
    }

    public static ModelNode readFullModelDescription(PathAddress address, ManagementResourceRegistration reg) {
         ModelNode node = new ModelNode();
         node.get(ModelDescriptionConstants.MODEL_DESCRIPTION).set(reg.getModelDescription(PathAddress.EMPTY_ADDRESS).getModelDescription(Locale.getDefault()));
         node.get(ModelDescriptionConstants.ADDRESS).set(address.toModelNode());
         for (PathElement pe : reg.getChildAddresses(PathAddress.EMPTY_ADDRESS)) {
             ModelNode children = node.get(ModelDescriptionConstants.CHILDREN);
             ManagementResourceRegistration sub = reg.getSubModel(PathAddress.pathAddress(pe));
             children.add(readFullModelDescription(address.append(pe), sub));
         }
         return node;
     }

    private static class NonResolvingResourceDescriptionResolver implements ResourceDescriptionResolver {
        @Override
        public ResourceBundle getResourceBundle(Locale locale) {
            return new ResourceBundle() {
                @Override
                protected Object handleGetObject(String key) {
                    return key;
                }

                @Override
                public Enumeration<String> getKeys() {
                    return Collections.enumeration(new HashSet<String>());
                }
            };
        }

        @Override
        public String getResourceDescription(Locale locale, ResourceBundle bundle) {
            return "description";
        }

        @Override
        public String getResourceAttributeDescription(String attributeName, Locale locale, ResourceBundle bundle) {
            return attributeName;
        }

        @Override
        public String getResourceAttributeValueTypeDescription(String attributeName, Locale locale, ResourceBundle bundle, String... suffixes) {
            return attributeName;
        }

        @Override
        public String getOperationDescription(String operationName, Locale locale, ResourceBundle bundle) {
            return operationName;
        }

        @Override
        public String getOperationParameterDescription(String operationName, String paramName, Locale locale, ResourceBundle bundle) {
            return operationName + "-" + paramName;
        }

        @Override
        public String getOperationParameterValueTypeDescription(String operationName, String paramName, Locale locale, ResourceBundle bundle, String... suffixes) {
            return operationName + "-" + paramName;
        }

        @Override
        public String getOperationReplyDescription(String operationName, Locale locale, ResourceBundle bundle) {
            return operationName;
        }

        @Override
        public String getOperationReplyValueTypeDescription(String operationName, Locale locale, ResourceBundle bundle, String... suffixes) {
            return operationName;
        }

        @Override
        public String getChildTypeDescription(String childType, Locale locale, ResourceBundle bundle) {
            return childType;
        }
    }
}

package org.jboss.as.controller.transform;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.EnumSet;
import java.util.Locale;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleOperationDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.NonResolvingResourceDescriptionResolver;
import org.jboss.as.controller.extension.ExtensionRegistry;
import org.jboss.as.controller.extension.SubsystemInformation;
import org.jboss.as.controller.registry.ImmutableManagementResourceRegistration;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a>
 */
public class SubsystemDescriptionDump implements OperationStepHandler {
    private final ExtensionRegistry extensionRegistry;
    protected static final SimpleAttributeDefinition PATH = new SimpleAttributeDefinition("path", ModelType.STRING, false);
    public static final String OPERATION_NAME = "subsystem-description-dump";
    public static final OperationDefinition DEFINITION = new SimpleOperationDefinition(OPERATION_NAME, new NonResolvingResourceDescriptionResolver(), OperationEntry.EntryType.PRIVATE, EnumSet.of(OperationEntry.Flag.READ_ONLY), PATH);
    public SubsystemDescriptionDump(final ExtensionRegistry extensionRegistry) {
        this.extensionRegistry = extensionRegistry;
    }

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        String path = PATH.resolveModelAttribute(context, operation).asString();
        dumpManagementResourceRegistration(extensionRegistry, path);
        context.stepCompleted();
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

    public static ModelNode readFullModelDescription(PathAddress address, ImmutableManagementResourceRegistration reg) {
         ModelNode node = new ModelNode();
         node.get(ModelDescriptionConstants.MODEL_DESCRIPTION).set(reg.getModelDescription(PathAddress.EMPTY_ADDRESS).getModelDescription(Locale.getDefault()));
         node.get(ModelDescriptionConstants.ADDRESS).set(address.toModelNode());
         for (PathElement pe : reg.getChildAddresses(PathAddress.EMPTY_ADDRESS)) {
             ModelNode children = node.get(ModelDescriptionConstants.CHILDREN);
             ImmutableManagementResourceRegistration sub = reg.getSubModel(PathAddress.pathAddress(pe));
             children.add(readFullModelDescription(address.append(pe), sub));
         }
         return node;
     }

}

package org.jboss.as.controller.transform;

import org.jboss.as.controller.ControllerLogger;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.extension.ExtensionRegistry;
import org.jboss.as.controller.extension.SubsystemInformation;
import org.jboss.as.controller.registry.ImmutableManagementResourceRegistration;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;

import java.util.Map;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a>
 * @since 7.1.2
 */
public class SimpleFullModelTransformer {
    private static Logger log = Logger.getLogger(SimpleFullModelTransformer.class);
    //private DefaultSubsystemTransformer defaultSubsystemTransformer = new DefaultSubsystemTransformer();
    private ExtensionRegistry extensionRegistry;

    public SimpleFullModelTransformer(ExtensionRegistry extensionRegistry) {
        this.extensionRegistry = extensionRegistry;
    }

    public Resource transformResource(final Resource root, final ImmutableManagementResourceRegistration rootResource, final Map<String, String> subsystemVersions) {
        return resolveRecursive(root.clone(), rootResource, PathAddress.EMPTY_ADDRESS, subsystemVersions);
    }


    private Resource resolveRecursive(final Resource resource, ImmutableManagementResourceRegistration registration,
                                      PathAddress address, final Map<String, String> subsystemVersions) {
        boolean subsystem = address.size() > 0
                && !ModelDescriptionConstants.EXTENSION.equals(address.getElement(0).getKey())
                && ModelDescriptionConstants.SUBSYSTEM.equals(address.getLastElement().getKey());
        if (subsystem) {
            String subsystemName = address.getLastElement().getValue();
            if (subsystemVersions.containsKey(subsystemName)) {
                String[] version = subsystemVersions.get(subsystemName).split("\\.");
                int major = Integer.parseInt(version[0]);
                int minor = Integer.parseInt(version[1]);
                SubsystemInformation info = extensionRegistry.getSubsystemInfo(subsystemName);
                if (info.getManagementInterfaceMajorVersion() == major && info.getManagementInterfaceMinorVersion() == minor) {
                    return resource; //no need to transform
                }
                log.trace("transforming subsystem: " + subsystem + ", to model version: " + subsystemVersions.get(subsystemName));
                SubsystemTransformer transformer = extensionRegistry.getTransformerRegistry().getSubsystemTransformer(subsystemName, major, minor);
                if (transformer != null) {
                    ResourceDefinition rd = TransformerRegistry.loadSubsystemDefinition(subsystemName, major, minor);
                    ManagementResourceRegistration targetDefinition = ManagementResourceRegistration.Factory.create(rd);
                    ModelNode fullSubsystemModel = Resource.Tools.readModel(resource);
                    ModelNode transformed = transformer.transformModel(null, fullSubsystemModel);
                    return TransformerRegistry.modelToResource(targetDefinition, transformed);
                } else { //for now no default subsystem transformer
                    ControllerLogger.ROOT_LOGGER.transformerNotFound(subsystemName, major, minor);
                    //defaultSubsystemTransformer.transformSubsystem(resource, registration, subsystemName, major, minor);
                }
            }
            return resource;
        }
        for (PathElement element : registration.getChildAddresses(PathAddress.EMPTY_ADDRESS)) {
            if (element.isMultiTarget()) {
                final String childType = element.getKey();
                for (final Resource.ResourceEntry entry : resource.getChildren(childType)) {
                    final ImmutableManagementResourceRegistration childRegistration = registration.getSubModel(PathAddress.pathAddress(PathElement.pathElement(childType, entry.getName())));
                    Resource res = resolveRecursive(entry, childRegistration, address.append(entry.getPathElement()), subsystemVersions);
                    if (!res.equals(entry)) {
                        resource.removeChild(entry.getPathElement());
                        resource.registerChild(entry.getPathElement(), res);
                    }
                }
            } else {
                final Resource child = resource.getChild(element);
                final ImmutableManagementResourceRegistration childRegistration = registration.getSubModel(PathAddress.pathAddress(element));
                if (child != null) {
                    Resource res = resolveRecursive(child, childRegistration, address.append(element), subsystemVersions);
                    if (!res.equals(child)) {
                        resource.removeChild(element);
                        resource.registerChild(element, res);
                    }
                }
            }
        }
        return resource;
    }


}

package org.wildfly.clustering.diagnostics.extension;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIBE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.StandardResourceDescriptionResolver;
import org.jboss.as.controller.operations.common.GenericSubsystemDescribeHandler;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.msc.service.ServiceName;


/**
 * The cluster subsystem, which provides views on wildfly clusters.
 *
 * @author Richard Achmatowicz (c) 2013 Red Hat Inc.
 */
public class ClusteringDiagnosticsExtension implements Extension {

    public static final String NAMESPACE = "urn:jboss:domain:clustering-diagnostics:1.0";
    public static final String SUBSYSTEM_NAME = "clustering-diagnostics";

    public static final PathElement SUBSYSTEM_PATH = PathElement.pathElement(SUBSYSTEM, SUBSYSTEM_NAME);
    private static final String RESOURCE_NAME = ClusteringDiagnosticsExtension.class.getPackage().getName() + ".LocalDescriptions";
    public static final ServiceName CLUSTER_EXTENSION_SERVICE_NAME = ServiceName.JBOSS.append("clustering");

    static StandardResourceDescriptionResolver getResourceDescriptionResolver(final String keyPrefix) {
        String prefix = SUBSYSTEM_NAME + (keyPrefix == null ? "" : "." + keyPrefix);
        return new StandardResourceDescriptionResolver(prefix, RESOURCE_NAME, ClusteringDiagnosticsExtension.class.getClassLoader(), true, false);
    }

    @Override
    public void initializeParsers(ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, NAMESPACE, new ClusteringDiagnosticsSubsystemXMLReader_1_0());
    }

    @Override
    public void initialize(ExtensionContext context) {

        final SubsystemRegistration subsystem = context.registerSubsystem(SUBSYSTEM_NAME, 1, 0);
        final ManagementResourceRegistration registration = subsystem.registerSubsystemModel(new ClusteringDiagnosticsSubsystemRootResourceDefinition(context.isRuntimeOnlyRegistrationValid()));
        registration.registerOperationHandler(DESCRIBE, GenericSubsystemDescribeHandler.INSTANCE, GenericSubsystemDescribeHandler.INSTANCE, false, OperationEntry.EntryType.PRIVATE);

        subsystem.registerXMLElementWriter(new ClusteringDiagnosticsSubsystemXMLWriter());
    }
}

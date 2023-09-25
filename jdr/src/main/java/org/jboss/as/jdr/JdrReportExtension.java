/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.jdr;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.PersistentResourceXMLDescription;
import org.jboss.as.controller.PersistentResourceXMLDescriptionReader;
import org.jboss.as.controller.PersistentResourceXMLDescriptionWriter;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.access.constraint.SensitivityClassification;
import org.jboss.as.controller.access.management.SensitiveTargetAccessConstraintDefinition;
import org.jboss.as.controller.descriptions.ParentResourceDescriptionResolver;
import org.jboss.as.controller.descriptions.SubsystemResourceDescriptionResolver;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementReader;

/**
 * Extension for triggering, requesting, generating and accessing JDR reports.
 *
 * @author Brian Stansberry
 */
public class JdrReportExtension implements Extension {

    public static final String SUBSYSTEM_NAME = "jdr";

    private static final ModelVersion CURRENT_MODEL_VERSION = ModelVersion.create(2, 0, 0);

    static final PathElement SUBSYSTEM_PATH = PathElement.pathElement(SUBSYSTEM, SUBSYSTEM_NAME);

    static final ParentResourceDescriptionResolver SUBSYSTEM_RESOLVER = new SubsystemResourceDescriptionResolver(SUBSYSTEM_NAME, JdrReportExtension.class);

    static final SensitivityClassification JDR_SENSITIVITY =
            new SensitivityClassification(SUBSYSTEM_NAME, "jdr", false, false, true);

    static final SensitiveTargetAccessConstraintDefinition JDR_SENSITIVITY_DEF = new SensitiveTargetAccessConstraintDefinition(JDR_SENSITIVITY);

    private final PersistentResourceXMLDescription currentDescription = JdrReportSubsystemSchema.CURRENT.getXMLDescription();

    @Override
    public void initialize(ExtensionContext context) {
        SubsystemRegistration subsystemRegistration = context.registerSubsystem(SUBSYSTEM_NAME, CURRENT_MODEL_VERSION);

        AtomicReference<JdrReportCollector> collectorReference = context.isRuntimeOnlyRegistrationValid() ? new AtomicReference<>() : null;
        subsystemRegistration.registerSubsystemModel(new JdrReportSubsystemDefinition(collectorReference));


        subsystemRegistration.registerXMLElementWriter(new PersistentResourceXMLDescriptionWriter(this.currentDescription));
    }

    @Override
    public void initializeParsers(ExtensionParsingContext context) {
        for (JdrReportSubsystemSchema schema : EnumSet.allOf(JdrReportSubsystemSchema.class)) {
            XMLElementReader<List<ModelNode>> reader = (schema == JdrReportSubsystemSchema.CURRENT) ? new PersistentResourceXMLDescriptionReader(this.currentDescription) : schema;
            context.setSubsystemXmlMapping(SUBSYSTEM_NAME, schema.getNamespace().getUri(), reader);
        }
    }
}

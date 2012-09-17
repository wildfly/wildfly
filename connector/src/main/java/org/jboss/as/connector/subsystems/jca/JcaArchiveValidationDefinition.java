package org.jboss.as.connector.subsystems.jca;

import org.jboss.as.connector.subsystems.resourceadapters.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.registry.ManagementResourceRegistration;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2012 Red Hat Inc.
 */
public class JcaArchiveValidationDefinition extends SimpleResourceDefinition {
    static final JcaArchiveValidationDefinition INSTANCE = new JcaArchiveValidationDefinition();

    private JcaArchiveValidationDefinition() {
        super(JcaExtension.PATH_ARCHIVE_VALIDATION,
                JcaExtension.getResourceDescriptionResolver(JcaExtension.PATH_ARCHIVE_VALIDATION.getKey()),
                ArchiveValidationAdd.INSTANCE,
                ReloadRequiredRemoveStepHandler.INSTANCE);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        super.registerAttributes(resourceRegistration);

        for (final ArchiveValidationAdd.ArchiveValidationParameters parameter : ArchiveValidationAdd.ArchiveValidationParameters.values()) {
            resourceRegistration.registerReadWriteAttribute(parameter.getAttribute(), null, JcaAttributeWriteHandler.INSTANCE);
        }
    }
}

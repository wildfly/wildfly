package org.jboss.as.connector.subsystems.jca;

import org.jboss.as.controller.SimpleResourceDefinition;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2012 Red Hat Inc.
 */
public class JcaSubsystemRootDefinition extends SimpleResourceDefinition {
    static final JcaSubsystemRootDefinition INSTANCE = new JcaSubsystemRootDefinition();

    private JcaSubsystemRootDefinition() {
        super(JcaExtension.PATH_SUBSYSTEM,
                JcaExtension.getResourceDescriptionResolver(),
                JcaSubsystemAdd.INSTANCE,
                JcaSubSystemRemove.INSTANCE);
    }

}

package org.jboss.as.clustering.jgroups.subsystem;

import java.util.EnumSet;

import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;

/**
 * Handler for read and write access to the subsystem attribute default-stack.
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public class SubsystemWriteAttributeHandler extends ReloadRequiredWriteAttributeHandler {

    public static final SubsystemWriteAttributeHandler INSTANCE = new SubsystemWriteAttributeHandler();

    private SubsystemWriteAttributeHandler() {
        super(JGroupsSubsystemRootResource.DEFAULT_STACK);
    }

    public void registerAttributes(final ManagementResourceRegistration registry) {
        registry.registerReadWriteAttribute(JGroupsSubsystemRootResource.DEFAULT_STACK, null, this);
    }
}

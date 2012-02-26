package org.jboss.as.clustering.infinispan.subsystem;

import java.util.EnumSet;

import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;

/**
 * Handler for read and write access to the subsystem attribute default-cache-container.
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public class SubsystemWriteAttributeHandler extends ReloadRequiredWriteAttributeHandler {

    public static final SubsystemWriteAttributeHandler INSTANCE = new SubsystemWriteAttributeHandler();

    private SubsystemWriteAttributeHandler() {
        super(CommonAttributes.DEFAULT_CACHE_CONTAINER);
    }

    public void registerAttributes(final ManagementResourceRegistration registry) {

        final EnumSet<AttributeAccess.Flag> flags = EnumSet.of(AttributeAccess.Flag.RESTART_ALL_SERVICES);
        registry.registerReadWriteAttribute(CommonAttributes.DEFAULT_CACHE_CONTAINER.getName(), null, this, flags);
    }
}

package org.jboss.as.clustering.infinispan.subsystem;

import java.util.EnumSet;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;

/**
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public class TransportWriteAttributeHandler extends ReloadRequiredWriteAttributeHandler {

    public static final TransportWriteAttributeHandler INSTANCE = new TransportWriteAttributeHandler();

    private TransportWriteAttributeHandler() {
        super(TransportResource.TRANSPORT_ATTRIBUTES);
    }

    public void registerAttributes(final ManagementResourceRegistration registry) {

        final EnumSet<AttributeAccess.Flag> flags = EnumSet.of(AttributeAccess.Flag.RESTART_ALL_SERVICES);

        for (AttributeDefinition attr : TransportResource.TRANSPORT_ATTRIBUTES) {
           registry.registerReadWriteAttribute(attr.getName(), null, this, flags);
        }
    }
}

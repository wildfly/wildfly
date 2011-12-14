package org.jboss.as.clustering.infinispan.subsystem;

import java.util.EnumSet;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.logging.Logger;

/**
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public class TransportWriteAttributeHandler extends ReloadRequiredWriteAttributeHandler {

    private static final Logger log = Logger.getLogger(TransportWriteAttributeHandler.class.getPackage().getName());

    public static final TransportWriteAttributeHandler INSTANCE = new TransportWriteAttributeHandler();

    private TransportWriteAttributeHandler() {
        super(CommonAttributes.TRANSPORT_ATTRIBUTES);
    }

    public void registerAttributes(final ManagementResourceRegistration registry) {

        final EnumSet<AttributeAccess.Flag> flags = EnumSet.of(AttributeAccess.Flag.RESTART_ALL_SERVICES);

        for (AttributeDefinition attr : CommonAttributes.TRANSPORT_ATTRIBUTES) {
           registry.registerReadWriteAttribute(attr.getName(), null, this, flags);
        }
    }
}

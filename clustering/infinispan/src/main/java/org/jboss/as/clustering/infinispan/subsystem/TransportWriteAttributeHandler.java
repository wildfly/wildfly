package org.jboss.as.clustering.infinispan.subsystem;

import java.util.EnumSet;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelType;
import org.jboss.logging.Logger;

/**
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public class TransportWriteAttributeHandler extends ReloadRequiredWriteAttributeHandler {

    private static final Logger log = Logger.getLogger(TransportWriteAttributeHandler.class.getPackage().getName());

    public static final TransportWriteAttributeHandler INSTANCE = new TransportWriteAttributeHandler();

    // these need to be moved to a common location
    public interface attributes {
        SimpleAttributeDefinition TRANSPORT_STACK = new SimpleAttributeDefinition(ModelKeys.STACK, ModelType.STRING, true);
        SimpleAttributeDefinition TRANSPORT_EXECUTOR = new SimpleAttributeDefinition(ModelKeys.EXECUTOR, ModelType.STRING, false);
        SimpleAttributeDefinition TRANSPORT_LOCK_TIMEOUT = new SimpleAttributeDefinition(ModelKeys.LOCK_TIMEOUT, ModelType.LONG, false);
        SimpleAttributeDefinition TRANSPORT_SITE = new SimpleAttributeDefinition(ModelKeys.SITE, ModelType.STRING, false);
        SimpleAttributeDefinition TRANSPORT_RACK = new SimpleAttributeDefinition(ModelKeys.RACK, ModelType.STRING, false);
        SimpleAttributeDefinition TRANSPORT_MACHINE = new SimpleAttributeDefinition(ModelKeys.MACHINE, ModelType.STRING, false);

        AttributeDefinition[] TRANSPORT_ATTRIBUTES = {TRANSPORT_STACK, TRANSPORT_EXECUTOR, TRANSPORT_LOCK_TIMEOUT, TRANSPORT_SITE, TRANSPORT_RACK, TRANSPORT_MACHINE};
    }

    private TransportWriteAttributeHandler() {
        super(attributes.TRANSPORT_ATTRIBUTES);
    }

    public void registerAttributes(final ManagementResourceRegistration registry) {


        final EnumSet<AttributeAccess.Flag> flags = EnumSet.of(AttributeAccess.Flag.RESTART_ALL_SERVICES);

        for (AttributeDefinition attr : attributes.TRANSPORT_ATTRIBUTES) {
           registry.registerReadWriteAttribute(attr.getName(), null, this, flags);
        }
    }
}

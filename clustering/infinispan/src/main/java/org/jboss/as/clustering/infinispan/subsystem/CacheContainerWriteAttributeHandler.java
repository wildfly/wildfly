package org.jboss.as.clustering.infinispan.subsystem;

import java.util.EnumSet;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.logging.Logger;

/**
 * Attribute handler for cache-container resource.
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public class CacheContainerWriteAttributeHandler extends ReloadRequiredWriteAttributeHandler {

    private static final Logger log = Logger.getLogger(CacheContainerWriteAttributeHandler.class.getPackage().getName());

    public static final CacheContainerWriteAttributeHandler INSTANCE = new CacheContainerWriteAttributeHandler();

    private CacheContainerWriteAttributeHandler() {
        super(CommonAttributes.CACHE_CONTAINER_ATTRIBUTES);
    }

    public void registerAttributes(final ManagementResourceRegistration registry) {

        final EnumSet<AttributeAccess.Flag> flags = EnumSet.of(AttributeAccess.Flag.RESTART_ALL_SERVICES);

        for (AttributeDefinition attr : CommonAttributes.CACHE_CONTAINER_ATTRIBUTES) {
           registry.registerReadWriteAttribute(attr.getName(), null, this, flags);
        }
    }
}

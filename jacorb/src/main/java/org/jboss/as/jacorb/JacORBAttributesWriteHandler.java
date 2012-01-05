package org.jboss.as.jacorb;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;

import java.util.EnumSet;

/**
 * @author Tomaz Cerar
 * @created 5.1.12 23:30
 */
public class JacORBAttributesWriteHandler extends ReloadRequiredWriteAttributeHandler {

    static final JacORBAttributesWriteHandler INSTANCE = new JacORBAttributesWriteHandler();


    public void registerAttributes(final ManagementResourceRegistration registry) {
        final EnumSet<AttributeAccess.Flag> flags = EnumSet.of(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES);
        for (AttributeDefinition attr : JacORBSubsystemDefinitions.SUBSYSTEM_ATTRIBUTES) {
            registry.registerReadWriteAttribute(attr.getName(), null, this, flags);
        }
    }


}

package org.jboss.as.jacorb;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;

/**
 * <p>
 * Defines a resource that encompasses all the settings that are to be applied when generating IORs.
 * </p>
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
class IORSettingsDefinition extends PersistentResourceDefinition {

    static final IORSettingsDefinition INSTANCE = new IORSettingsDefinition();

    private static final List<PersistentResourceDefinition> CHILDREN = Collections.unmodifiableList(
            Arrays.asList(IORTransportConfigDefinition.INSTANCE, IORASContextDefinition.INSTANCE,
                    IORSASContextDefinition.INSTANCE));

    private IORSettingsDefinition() {
        super(PathElement.pathElement(JacORBSubsystemConstants.IOR_SETTINGS, JacORBSubsystemConstants.DEFAULT),
                JacORBExtension.getResourceDescriptionResolver(JacORBSubsystemConstants.IOR_SETTINGS),
                new AbstractAddStepHandler(), ReloadRequiredRemoveStepHandler.INSTANCE);
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return Collections.emptySet();
    }

    @Override
    protected List<? extends PersistentResourceDefinition> getChildren() {
        return CHILDREN;
    }
}

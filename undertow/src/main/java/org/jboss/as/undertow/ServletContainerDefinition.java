package org.jboss.as.undertow;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.SimplePersistentResourceDefinition;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2013 Red Hat Inc.
 */
public class ServletContainerDefinition extends SimplePersistentResourceDefinition {
    static final ServletContainerDefinition INSTANCE = new ServletContainerDefinition();

    private ServletContainerDefinition() {
        super(UndertowExtension.PATH_SERVLET_CONTAINER,
                UndertowExtension.getResolver(Constants.SERVLET_CONTAINER),
                ServletContainerAdd.INSTANCE,
                ReloadRequiredRemoveStepHandler.INSTANCE);
    }

    @Override
    public AttributeDefinition[] getAttributes() {
        return new AttributeDefinition[0];
    }

    @Override
    public PersistentResourceDefinition[] getChildren() {
        return new PersistentResourceDefinition[]{JSPDefinition.INSTANCE};
    }
}

package org.wildfly.extension.undertow;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

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
    public Collection<AttributeDefinition> getAttributes() {
        return Collections.emptySet();
    }

    @Override
    public List<? extends PersistentResourceDefinition> getChildren() {
        return Collections.singletonList(JSPDefinition.INSTANCE);

    }
}

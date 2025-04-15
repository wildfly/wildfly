/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleResourceDefinition;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2012 Red Hat Inc.
 * @author Richard Achmatowicz (c) 2020 Red Hat Inc.
 */
public class HttpListenerResourceDefinition extends AbstractHttpListenerResourceDefinition {
    static final PathElement PATH_ELEMENT = PathElement.pathElement(Constants.HTTP_LISTENER);

    static final List<AttributeDefinition> ATTRIBUTES = List.of(REDIRECT_SOCKET);

    HttpListenerResourceDefinition() {
        super(new SimpleResourceDefinition.Parameters(PATH_ELEMENT, UndertowExtension.getResolver(Constants.LISTENER))
                .setCapabilities(HTTP_UPGRADE_REGISTRY_CAPABILITY), new HttpListenerAdd());
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        List<AttributeDefinition> attributes = new ArrayList<>(ListenerResourceDefinition.ATTRIBUTES.size() + AbstractHttpListenerResourceDefinition.ATTRIBUTES.size() + ATTRIBUTES.size());
        attributes.addAll(ListenerResourceDefinition.ATTRIBUTES);
        attributes.addAll(AbstractHttpListenerResourceDefinition.ATTRIBUTES);
        attributes.addAll(ATTRIBUTES);
        return Collections.unmodifiableCollection(attributes);
    }
}

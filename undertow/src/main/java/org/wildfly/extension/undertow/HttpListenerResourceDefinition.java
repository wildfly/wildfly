/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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
                .setCapabilities(HTTP_UPGRADE_REGISTRY_CAPABILITY), HttpListenerAdd::new);
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

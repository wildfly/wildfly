/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.clustering.jgroups.subsystem;

import java.util.HashMap;
import java.util.Map;

import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public enum XMLElement {
    // must be first
    UNKNOWN(""),

    CHANNEL(ChannelResourceDefinition.WILDCARD_PATH),
    CHANNELS("channels"),
    DEFAULT_THREAD_POOL("default-thread-pool"),
    FORK(ForkResourceDefinition.WILDCARD_PATH),
    INTERNAL_THREAD_POOL("internal-thread-pool"),
    OOB_THREAD_POOL("oob-thread-pool"),
    PROPERTY(ModelDescriptionConstants.PROPERTY),
    PROTOCOL(ProtocolResourceDefinition.WILDCARD_PATH),
    RELAY(RelayResourceDefinition.WILDCARD_PATH),
    REMOTE_SITE(RemoteSiteResourceDefinition.WILDCARD_PATH),
    STACK(StackResourceDefinition.WILDCARD_PATH),
    STACKS("stacks"),
    TIMER_THREAD_POOL("timer-thread-pool"),
    TRANSPORT(TransportResourceDefinition.WILDCARD_PATH),
    ;

    private final String name;

    XMLElement(PathElement path) {
        this.name = path.isWildcard() ? path.getKey() : path.getValue();
    }

    XMLElement(String name) {
        this.name = name;
    }

    /**
     * Get the local name of this element.
     *
     * @return the local name
     */
    public String getLocalName() {
        return this.name;
    }

    private static final Map<String, XMLElement> elements = new HashMap<>();

    static {
        for (XMLElement element : values()) {
            String name = element.getLocalName();
            if (name != null) {
                elements.put(name, element);
            }
        }
    }

    public static XMLElement forName(String localName) {
        XMLElement element = elements.get(localName);
        return (element != null) ? element : UNKNOWN;
    }
}

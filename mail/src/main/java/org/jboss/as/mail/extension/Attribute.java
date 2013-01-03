/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.mail.extension;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Tomaz Cerar
 * @created 10.8.11 22:41
 */
enum Attribute {
    UNKNOWN(null),
    USERNAME(MailSubsystemModel.LOGIN_USERNAME),
    PASSWORD(MailSubsystemModel.PASSWORD),
    JNDI_NAME(MailSubsystemModel.JNDI_NAME),
    DEBUG(MailSubsystemModel.DEBUG),
    FROM(MailSubsystemModel.FROM),
    OUTBOUND_SOCKET_BINDING_REF(MailSubsystemModel.OUTBOUND_SOCKET_BINDING_REF),
    SSL(MailSubsystemModel.SSL),
    TLS(MailSubsystemModel.TLS),
    NAME(MailSubsystemModel.NAME);

    private final String name;

    private Attribute(final String name) {
        this.name = name;
    }

    /**
     * Get the local name of this element.
     *
     * @return the local name
     */
    public String getLocalName() {
        return name;
    }

    private static final Map<String, Attribute> attributes;

    static {
        final Map<String, Attribute> map = new HashMap<String, Attribute>();
        for (Attribute attribute : values()) {
            final String name = attribute.getLocalName();
            if (name != null) { map.put(name, attribute); }
        }
        attributes = map;
    }

    public static Attribute forName(String localName) {
        final Attribute attribute = attributes.get(localName);
        return attribute == null ? UNKNOWN : attribute;
    }
}

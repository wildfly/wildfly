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
package org.jboss.as.webservices.dmr;

import java.util.HashMap;
import java.util.Map;

/**
 * WS configuration elements.
 *
 * @author <a href="mailto:alessio.soldano@jboss.com">Alessio Soldano</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
enum Element {

    /** always the first **/
    UNKNOWN(null),
    SUBSYSTEM("subsystem"),
    MODIFY_WSDL_ADDRESS(Constants.MODIFY_WSDL_ADDRESS),
    WSDL_HOST(Constants.WSDL_HOST),
    WSDL_PORT(Constants.WSDL_PORT),
    WSDL_SECURE_PORT(Constants.WSDL_SECURE_PORT),
    WSDL_URI_SCHEME(Constants.WSDL_URI_SCHEME),
    WSDL_PATH_REWRITE_RULE(Constants.WSDL_PATH_REWRITE_RULE),
    CLIENT_CONFIG(Constants.CLIENT_CONFIG),
    ENDPOINT_CONFIG(Constants.ENDPOINT_CONFIG),
    CONFIG_NAME(Constants.CONFIG_NAME),
    PROPERTY(Constants.PROPERTY),
    PROPERTY_NAME(Constants.PROPERTY_NAME),
    PROPERTY_VALUE(Constants.PROPERTY_VALUE),
    PRE_HANDLER_CHAIN(Constants.PRE_HANDLER_CHAIN),
    POST_HANDLER_CHAIN(Constants.POST_HANDLER_CHAIN),
    PRE_HANDLER_CHAINS(Constants.PRE_HANDLER_CHAINS),
    POST_HANDLER_CHAINS(Constants.POST_HANDLER_CHAINS),
    HANDLER_CHAIN(Constants.HANDLER_CHAIN),
    PROTOCOL_BINDINGS(Constants.PROTOCOL_BINDINGS),
    HANDLER(Constants.HANDLER),
    HANDLER_NAME(Constants.HANDLER_NAME),
    HANDLER_CLASS(Constants.HANDLER_CLASS);

    private final String name;

    private Element(final String name) {
        this.name = name;
    }

    private static final Map<String, Element> MAP;

    static {
        final Map<String, Element> map = new HashMap<String, Element>();
        for (final Element element : values()) {
            final String name = element.getLocalName();
            if (name != null)
                map.put(name, element);
        }
        MAP = map;
    }

    static Element forName(final String localName) {
        final Element element = MAP.get(localName);
        return element == null ? UNKNOWN : element;
    }

    /**
     * Get the local name of this element.
     *
     * @return the local name
     */
    String getLocalName() {
        return name;
    }

}

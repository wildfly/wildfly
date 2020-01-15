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
package org.jboss.as.jaxrs;

import java.util.HashMap;
import java.util.Map;

/**
 * Jaxrs configuration elements.
 *
 * @author <a href="mailto:alessio.soldano@jboss.com">Alessio Soldano</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 * @author <a href="mailto:rsigal@redhat.com">Ron Sigal</a>
 */
enum JaxrsElement {
    UNKNOWN(null),
    JAXRS_2_0_REQUEST_MATCHING(JaxrsConstants.JAXRS_2_0_REQUEST_MATCHING),
    RESTEASY_ADD_CHARSET(JaxrsConstants.RESTEASY_ADD_CHARSET),
    RESTEASY_BUFFER_EXCEPTION_ENTITY(JaxrsConstants.RESTEASY_BUFFER_EXCEPTION_ENTITY),
    RESTEASY_DISABLE_HTML_SANITIZER(JaxrsConstants.RESTEASY_DISABLE_HTML_SANITIZER),
    RESTEASY_DISABLE_PROVIDERS(JaxrsConstants.RESTEASY_DISABLE_PROVIDERS),
    RESTEASY_DOCUMENT_EXPAND_ENTITY_REFERENCES(JaxrsConstants.RESTEASY_DOCUMENT_EXPAND_ENTITY_REFERENCES),
    RESTEASY_DOCUMENT_SECURE_DISABLE_DTDS(JaxrsConstants.RESTEASY_DOCUMENT_SECURE_DISABLE_DTDS),
    RESTEASY_DOCUMENT_SECURE_PROCESSING_FEATURE(JaxrsConstants.RESTEASY_DOCUMENT_SECURE_PROCESSING_FEATURE),
    RESTEASY_GZIP_MAX_INPUT(JaxrsConstants.RESTEASY_GZIP_MAX_INPUT),
    RESTEASY_JNDI_RESOURCES(JaxrsConstants.RESTEASY_JNDI_RESOURCES),
    RESTEASY_LANGUAGE_MAPPINGS(JaxrsConstants.RESTEASY_LANGUAGE_MAPPINGS),
    RESTEASY_MEDIA_TYPE_MAPPINGS(JaxrsConstants.RESTEASY_MEDIA_TYPE_MAPPINGS),
    RESTEASY_MEDIA_TYPE_PARAM_MAPPING(JaxrsConstants.RESTEASY_MEDIA_TYPE_PARAM_MAPPING),
    RESTEASY_PREFER_JACKSON_OVER_JSONB(JaxrsConstants.RESTEASY_PREFER_JACKSON_OVER_JSONB),
    RESTEASY_PROVIDERS(JaxrsConstants.RESTEASY_PROVIDERS),
    RESTEASY_RFC7232_PRECONDITIONS(JaxrsConstants.RESTEASY_RFC7232_PRECONDITIONS),
    RESTEASY_ROLE_BASED_SECURITY(JaxrsConstants.RESTEASY_ROLE_BASED_SECURITY),
    RESTEASY_SECURE_RANDOM_MAX_USE(JaxrsConstants.RESTEASY_SECURE_RANDOM_MAX_USE),
    RESTEASY_USE_BUILTIN_PROVIDERS(JaxrsConstants.RESTEASY_USE_BUILTIN_PROVIDERS),
    RESTEASY_USE_CONTAINER_FORM_PARAMS(JaxrsConstants.RESTEASY_USE_CONTAINER_FORM_PARAMS),
    RESTEASY_WIDER_REQUEST_MATCHING(JaxrsConstants.RESTEASY_WIDER_REQUEST_MATCHING);

    private final String name;

    private JaxrsElement(final String name) {
        this.name = name;
    }

    private static final Map<String, JaxrsElement> MAP;

    static {
        final Map<String, JaxrsElement> map = new HashMap<String, JaxrsElement>();
        for (final JaxrsElement element : values()) {
            final String name = element.getLocalName();
            if (name != null)
                map.put(name, element);
        }
        MAP = map;
    }

    static JaxrsElement forName(final String localName) {
        final JaxrsElement element = MAP.get(localName);
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

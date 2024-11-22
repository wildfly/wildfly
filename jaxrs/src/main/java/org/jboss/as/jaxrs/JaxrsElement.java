/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
    @Deprecated
    RESTEASY_DOCUMENT_EXPAND_ENTITY_REFERENCES(JaxrsConstants.RESTEASY_DOCUMENT_EXPAND_ENTITY_REFERENCES),
    @Deprecated
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
    RESTEASY_WIDER_REQUEST_MATCHING(JaxrsConstants.RESTEASY_WIDER_REQUEST_MATCHING),
    RESTEASY_PATCHFILTER_DISABLED(JaxrsConstants.RESTEASY_PATCHFILTER_DISABLED),
    TRACING_TYPE(JaxrsAttribute.TRACING_TYPE.getName()),
    TRACING_THRESHOLD(JaxrsAttribute.TRACING_THRESHOLD.getName()),
    ;

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

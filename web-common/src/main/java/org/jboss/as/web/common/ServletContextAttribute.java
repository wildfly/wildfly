/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.web.common;

import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.as.server.deployment.AttachmentList;

/**
 * Configuration object used to store a {@link jakarta.servlet.ServletContext} attribute name and value.
 *
 * @author John Bailey
 */
public class ServletContextAttribute {
    public static final AttachmentKey<AttachmentList<ServletContextAttribute>> ATTACHMENT_KEY = AttachmentKey.createList(ServletContextAttribute.class);

    private final String name;
    private final Object value;

    /**
     * Create a new instance.
     *
     * @param name  The attribute name
     * @param value The attribute valule
     */
    public ServletContextAttribute(final String name, final Object value) {
        this.name = name;
        this.value = value;
    }

    /**
     * Get the attribute name.
     *
     * @return the attribute name
     */
    public String getName() {
        return name;
    }

    /**
     * Get the attribute value.
     *
     * @return the attribute value
     */
    public Object getValue() {
        return value;
    }
}

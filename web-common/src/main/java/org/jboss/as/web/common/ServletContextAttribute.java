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

package org.jboss.as.web.common;

import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.as.server.deployment.AttachmentList;

/**
 * Configuration object used to store a {@link javax.servlet.ServletContext} attribute name and value.
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

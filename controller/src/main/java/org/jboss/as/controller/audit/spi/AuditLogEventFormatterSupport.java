/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

package org.jboss.as.controller.audit.spi;



/**
 * Base implementation of useful methods in {@link AuditLogEventFormatter}
 *
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public abstract class AuditLogEventFormatterSupport implements AuditLogEventFormatter {
    private final String name;
    private volatile String formattedString;


    protected AuditLogEventFormatterSupport(String name) {
        this.name = name;
    }

    /**
     * Returns the name of this AuditLogEventFormatter
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Clears the formatted string once the auditable event has been written
     */
    public void clear() {
        formattedString = null;
    }

    /**
     * Get the cached formatted string. This is to avoid having to reformat the auditable event if there are more
     * than one audit logger. Typical use will be to check that this is not null, before formatting the item and
     * caching it for reuse using {@link #cacheString(String)}
     *
     * @return the cached formatted string
     */
    protected String getCachedString() {
        return formattedString;
    }

    /**
     * Cache the formatted string so that it can be reused from {@link #getCachedString()}, to avoid reformatting
     * the auditable event if there are more than one audit logger.
     *
     * @param the formatted string to cache
     */
    protected String cacheString(String recordText) {
        this.formattedString = recordText;
        return formattedString;
    }
}

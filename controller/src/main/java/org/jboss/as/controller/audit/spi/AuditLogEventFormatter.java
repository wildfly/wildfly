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
 * Interface for audit log event formatters.
 * Methods called on this formatter instance get called with the controller's lock taken, so it is thread-safe.
 * To get support for the caching mentioned in the method comments, you can extend {@link AuditLogEventFormatterSupport}.
 * As we add support for more types of auditable events this interface might have more methods added, even among minor releases.
 *
 *
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public interface AuditLogEventFormatter {

    /**
     * Returns the name of this AuditLogEventFormatter
     *
     * @return the name
     */
    String getName();

    /**
     * Formats and caches the audit log item. If this method has already been called, the same
     * bytes should be returned until all handlers have received and logged the item and
     * the {@link #clear()} method gets called.
     *
     * @param item the log item
     * @return the formatted string
     */
    String formatAuditLogItem(ModelControllerAuditLogEvent item);

    /**
     * Formats and caches the audit log item. If this method has already been called, the same
     * bytes should be returned until all handlers have received and logged the item and
     * the {@link #clear()} method gets called.
     *
     * @param item the log item
     * @return the formatted string
     */
    String formatAuditLogItem(JmxAccessAuditLogEvent item);

    /**
     * Clears the formatted log item created by {@link #formatAuditLogItem(ModelControllerAuditLogEvent)}
     * or {@link #formatAuditLogItem(JmxAccessAuditLogEvent)} once the audit log item has been
     * fully processed.
     */
    void clear();


}

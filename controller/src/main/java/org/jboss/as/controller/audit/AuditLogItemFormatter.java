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
package org.jboss.as.controller.audit;

import java.text.SimpleDateFormat;


/**
  * All methods on this class should be called with {@link ManagedAuditLoggerImpl}'s lock taken.
  *
  * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public abstract class AuditLogItemFormatter {
    public static final String TYPE_JMX = "jmx";
    public static final String TYPE_CORE = "core";

    protected final String name;
    private volatile String formattedString;
    private volatile boolean includeDate ;
    private volatile String dateSeparator;
    //SimpleDateFormat is not good to store among threads, since it stores intermediate results in its fields
    //Methods on this class will only ever be called from one thread (see class javadoc) so although it looks shared here it is not
    private volatile SimpleDateFormat dateFormat;

    protected AuditLogItemFormatter(String name, boolean includeDate, String dateSeparator, String dateFormat) {
        this.name = name;
        this.includeDate = includeDate;
        this.dateSeparator = dateSeparator;
        this.dateFormat = new SimpleDateFormat(dateFormat);
    }

    public String getName() {
        return name;
    }

    /**
     * Sets whether the date should be included when logging the audit log item.
     *
     * @param include {@code true to include the date}
     */
    public void setIncludeDate(boolean include) {
        this.includeDate = include;
    }

    /**
     * Sets the date format. If we should not include the date, this is ignored.
     *
     * @param pattern the date format to use as understood by {@link java.text.SimpleDateFormat}
     */
    public void setDateFormat(String pattern) {
        this.dateFormat = new SimpleDateFormat(pattern);
    }

    /**
     * Sets whether the date should be included when logging the audit log item. If we should not include the date this is ignored/
     *
     * @param include {@code true to include the date}
     */
    public void setDateSeparator(String dateSeparator) {
        this.dateSeparator = dateSeparator;
    }

    /**
     * Formats and caches the audit log item. If this method has already been called, the same
     * bytes should be returned until all handlers have received and logged the item and
     * the {@link #clear()} method gets called.
     *
     * @param item the log item
     * @return the formatted string
     */
    abstract String formatAuditLogItem(AuditLogItem.ModelControllerAuditLogItem item);

    /**
     * Formats and caches the audit log item. If this method has already been called, the same
     * bytes should be returned until all handlers have received and logged the item and
     * the {@link #clear()} method gets called.
     *
     * @param item the log item
     * @return the formatted string
     */
    abstract String formatAuditLogItem(AuditLogItem.JmxAccessAuditLogItem item);

    /**
     * Clears the formatted log item created by {@link #formatAuditLogItem(org.jboss.as.controller.audit.AuditLogItem.JmxAccessAuditLogItem)}
     * or {@link #formatAuditLogItem(org.jboss.as.controller.audit.AuditLogItem.ModelControllerAuditLogItem)} once the audit log item has been
     * fully processed.
     */
    void clear() {
        formattedString = null;
    }

    protected void appendDate(StringBuilder sb, AuditLogItem auditLogItem) {
        if (includeDate) {
            sb.append(dateFormat.format(auditLogItem.getDate()));
            sb.append(dateSeparator);
        }
    }

    String getCachedString() {
        return formattedString;
    }

    String cacheString(String recordText) {
        this.formattedString = recordText;
        return formattedString;
    }
}

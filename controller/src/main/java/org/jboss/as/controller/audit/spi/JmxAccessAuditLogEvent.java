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

import java.net.InetAddress;
import java.util.Date;

import org.jboss.as.core.security.AccessMechanism;

/**
 *  An audit log event happening from accessing the jmx mbean server.
 *
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public interface JmxAccessAuditLogEvent {

    /**
     * Return the type. This will always return {@link AuditLogEventType#JMX}
     * @return the type
     */
    public AuditLogEventType getType();

    /**
     * Get the asVersion
     * @return the asVersion
     */
    String getAsVersion();

    /**
     * Get the readOnly
     * @return the readOnly
     */
    boolean isReadOnly();

    /**
     * Get the booting
     * @return the booting
     */
    boolean isBooting();

    /**
     * Get the userId
     * @return the userId
     */
    String getUserId();

    /**
     * Get the domainUUID
     * @return the domainUUID
     */
    String getDomainUUID();

    /**
     * Get the accessMechanism
     * @return the accessMechanism
     */
    AccessMechanism getAccessMechanism();

    /**
     * Get the remoteAddress
     * @return the remoteAddress
     */
    InetAddress getRemoteAddress();

    /**
     * Get the date
     * @return the date
     */
    Date getDate();

    /**
     * Get the methodName
     * @return the methodName
     */
    String getMethodName();

    /**
     * Get the methodSignature
     * @return the methodSignature
     */
    String[] getMethodSignature();

    /**
     * Get the methodParams
     * @return the methodParams
     */
    Object[] getMethodParams();

    /**
     * Get the error
     * @return the error
     */
    Throwable getError();

}
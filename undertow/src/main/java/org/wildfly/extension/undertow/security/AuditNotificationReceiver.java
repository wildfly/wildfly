/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.undertow.security;

import io.undertow.security.api.NotificationReceiver;
import io.undertow.security.api.SecurityNotification;
import io.undertow.security.api.SecurityNotification.EventType;
import io.undertow.security.idm.Account;

import java.util.HashMap;
import java.util.Map;

import org.jboss.security.audit.AuditEvent;
import org.jboss.security.audit.AuditLevel;
import org.jboss.security.audit.AuditManager;

/**
 * A {@link NotificationReceiver} implementation responsible for recording audit events for authentication attempts.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class AuditNotificationReceiver implements NotificationReceiver {

    private final AuditManager auditManager;

    public AuditNotificationReceiver(final AuditManager auditManager) {
        this.auditManager = auditManager;
    }

    @Override
    public void handleNotification(SecurityNotification notification) {
        EventType event = notification.getEventType();
        if (event == EventType.AUTHENTICATED || event == EventType.FAILED_AUTHENTICATION) {
            AuditEvent auditEvent = new AuditEvent(event == EventType.AUTHENTICATED ? AuditLevel.SUCCESS : AuditLevel.FAILURE);
            Map<String, Object> ctxMap = new HashMap<String, Object>();
            Account account = notification.getAccount();
            if (account != null) {
                ctxMap.put("principal", account.getPrincipal().getName());
            }
            ctxMap.put("message", notification.getMessage());
            /*
             * HttpServletRequest hsr = getServletRequest(); if (hsr != null) { ctxMap.put("request",
             * WebUtil.deriveUsefulInfo(hsr)); }
             */
            ctxMap.put("Source", getClass().getCanonicalName());
            auditEvent.setContextMap(ctxMap);
            auditManager.audit(auditEvent);

        }
    }

}

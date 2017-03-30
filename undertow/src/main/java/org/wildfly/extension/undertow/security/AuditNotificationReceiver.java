/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;

import io.undertow.security.api.NotificationReceiver;
import io.undertow.security.api.SecurityNotification;
import io.undertow.security.api.SecurityNotification.EventType;
import io.undertow.security.idm.Account;
import io.undertow.servlet.handlers.ServletRequestContext;
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

            ServletRequestContext src = notification.getExchange().getAttachment(ServletRequestContext.ATTACHMENT_KEY);
            if(src != null) {
                ServletRequest hsr = src.getServletRequest();
                if (hsr instanceof HttpServletRequest) {
                    ctxMap.put("request", deriveUsefulInfo((HttpServletRequest) hsr));
                }
            }
            ctxMap.put("Source", getClass().getCanonicalName());
            auditEvent.setContextMap(ctxMap);
            auditManager.audit(auditEvent);

        }
    }

    /**
     * Obtain debug information from the servlet request object
     *
     * @param httpRequest
     * @return
     */
    private static String deriveUsefulInfo(HttpServletRequest httpRequest) {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(httpRequest.getContextPath());
        sb.append(":cookies=").append(Arrays.toString(httpRequest.getCookies())).append(":headers=");
        // Append Header information
        Enumeration<?> en = httpRequest.getHeaderNames();
        while (en.hasMoreElements()) {
            String headerName = (String) en.nextElement();
            sb.append(headerName).append("=");
            // Ensure HTTP Basic Password is not logged
            if (!headerName.contains("authorization")) { sb.append(httpRequest.getHeader(headerName)).append(","); }
        }
        sb.append("]");
        // Append Request parameter information
        sb.append("[parameters=");
        Enumeration<?> enparam = httpRequest.getParameterNames();
        while (enparam.hasMoreElements()) {
            String paramName = (String) enparam.nextElement();
            String[] paramValues = httpRequest.getParameterValues(paramName);
            int len = paramValues != null ? paramValues.length : 0;
            for (int i = 0; i < len; i++) { sb.append(paramValues[i]).append("::"); }
            sb.append(",");
        }
        sb.append("][attributes=");
        // Append Request attribute information
        Enumeration<?> enu = httpRequest.getAttributeNames();
        while (enu.hasMoreElements()) {
            String attrName = (String) enu.nextElement();
            sb.append(attrName).append("=");
            sb.append(httpRequest.getAttribute(attrName)).append(",");
        }
        sb.append("]");
        return sb.toString();
    }
}

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

import io.undertow.security.api.NotificationReceiver;
import io.undertow.security.api.SecurityNotification;
import io.undertow.security.idm.Account;
import org.jboss.security.AuthenticationManager;

import javax.security.auth.Subject;
import java.security.Principal;

/**
 * Undertow security listener that invokes {@code AuthenticationManager.logout()} on logout, flushing the principal from
 * the cache if a security cache is being used.
 *
 * @author Stuart Douglas
 */
public class LogoutNotificationReceiver implements NotificationReceiver {

    private final AuthenticationManager manager;
    private final String securityDomain;

    public LogoutNotificationReceiver(AuthenticationManager manager, String securityDomain) {
        this.manager = manager;
        this.securityDomain = securityDomain;
    }

    @Override
    public void handleNotification(SecurityNotification notification) {
        if (notification.getEventType() == SecurityNotification.EventType.LOGGED_OUT) {
            Account account = notification.getAccount();
            Principal principal = (account instanceof AccountImpl) ? ((AccountImpl) account).getOriginalPrincipal() :
                    account.getPrincipal();
            if (principal != null) {
                // perform the logout of the principal using the subject currently set in the security context.
                Subject subject = SecurityActions.getSubject();
                this.manager.logout(principal, subject);
            }

            // Clear old context
            SecurityActions.clearSecurityContext();
            SecurityActions.setSecurityRoles(null);

            // Set a new one in case re-authentication is done within the same thread
            org.jboss.security.SecurityContext securityContext = SecurityActions.createSecurityContext(securityDomain);
            notification.getExchange().putAttachment(UndertowSecurityAttachments.SECURITY_CONTEXT_ATTACHMENT, securityContext);

            SecurityActions.setSecurityContextOnAssociation(securityContext);
        }
    }
}

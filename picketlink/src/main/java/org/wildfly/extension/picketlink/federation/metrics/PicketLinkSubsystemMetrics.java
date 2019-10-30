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

package org.wildfly.extension.picketlink.federation.metrics;

import org.jboss.security.audit.AuditEvent;
import org.picketlink.common.exceptions.ConfigurationException;
import org.picketlink.identity.federation.core.audit.PicketLinkAuditEvent;
import org.picketlink.identity.federation.core.audit.PicketLinkAuditEventType;
import org.picketlink.identity.federation.core.audit.PicketLinkAuditHelper;

import static org.wildfly.extension.picketlink.logging.PicketLinkLogger.ROOT_LOGGER;

/**
 * <p> This class provides ways to store metrics collected from the PicketLink providers (IDPs and SPs). </p>
 *
 * @author <a href="mailto:psilva@redhat.com">Pedro Silva</a>
 */
public class PicketLinkSubsystemMetrics extends PicketLinkAuditHelper {

    private int createdAssertionsCount;
    private int responseToSPCount;
    private int errorResponseToSPCount;
    private int errorSignValidationCount;
    private int errorTrustedDomainCount;
    private int expiredAssertionsCount;
    private int loginInitCount;
    private int loginCompleteCount;
    private int requestFromIDPCount;
    private int responseFromIDPCount;
    private int requestToIDPCount;

    public PicketLinkSubsystemMetrics(String securityDomainName) throws ConfigurationException {
        super(securityDomainName);
    }

    @Override
    public void audit(AuditEvent event) {
        PicketLinkAuditEvent picketLinkEvent = (PicketLinkAuditEvent) event;
        PicketLinkAuditEventType eventType = picketLinkEvent.getType();

        switch (eventType) {
            case CREATED_ASSERTION:
                this.createdAssertionsCount++;
                break;
            case RESPONSE_TO_SP:
                this.responseToSPCount++;
                break;
            case ERROR_RESPONSE_TO_SP:
                this.errorResponseToSPCount++;
                break;
            case ERROR_SIG_VALIDATION:
                this.errorSignValidationCount++;
                break;
            case ERROR_TRUSTED_DOMAIN:
                this.errorTrustedDomainCount++;
                break;
            case EXPIRED_ASSERTION:
                this.expiredAssertionsCount++;
                break;
            case LOGIN_INIT:
                this.loginInitCount++;
                break;
            case LOGIN_COMPLETE:
                this.loginCompleteCount++;
                break;
            case REQUEST_FROM_IDP:
                this.requestFromIDPCount++;
                break;
            case REQUEST_TO_IDP:
                this.requestToIDPCount++;
                break;
            case RESPONSE_FROM_IDP:
                this.responseFromIDPCount++;
                break;
            default:
                ROOT_LOGGER.federationIgnoringAuditEvent(eventType);
                return;
        }

        super.audit(picketLinkEvent);
    }

    public int getCreatedAssertionsCount() {
        return this.createdAssertionsCount;
    }

    public int getResponseToSPCount() {
        return this.responseToSPCount;
    }

    public int getErrorResponseToSPCount() {
        return this.errorResponseToSPCount;
    }

    public int getErrorSignValidationCount() {
        return this.errorSignValidationCount;
    }

    public int getErrorTrustedDomainCount() {
        return this.errorTrustedDomainCount;
    }

    public int getExpiredAssertionsCount() {
        return this.expiredAssertionsCount;
    }

    public int getLoginInitCount() {
        return this.loginInitCount;
    }

    public int getLoginCompleteCount() {
        return this.loginCompleteCount;
    }

    public int getRequestFromIDPCount() {
        return this.requestFromIDPCount;
    }

    public int getResponseFromIDPCount() {
        return this.responseFromIDPCount;
    }

    public int getRequestToIDPCount() {
        return this.requestToIDPCount;
    }
}

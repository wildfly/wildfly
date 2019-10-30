/*
 * Copyright 2017 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.connector.metadata.common;

import org.jboss.as.connector.metadata.api.common.Security;
import org.jboss.jca.common.api.validator.ValidateException;


/**
 * Extension of {@link org.jboss.jca.common.metadata.common.SecurityImpl} with added Elytron support.
 *
 * @author Flavia Rainone
 */
public class SecurityImpl extends org.jboss.jca.common.metadata.common.SecurityImpl implements Security {

    private static final long serialVersionUID = -4549127155646451392L;

    /**
     * Indicates if the Security data belongs to Elytron or PicketBox.
     */
    private boolean elytronEnabled;

    /**
     * Constructor
     *
     * @param securityDomain               security domain managed authentication. Security domain will refer to a PicketBox
     *                                     security domain if Elytron is disabled, or to an Elytron authentication context
     *                                     otherwise
     * @param securityDomainAndApplication securityDomain and application managed authentication. This field will refer to a
     *                                     PicketBox security domain if Elytron is disabled, or to an Elytron authentication
     *                                     context otherwise
     * @param applicationManaged           application managed authentication
     * @param elytronEnabled               in case one of {@param securityDomain} or {@param securityDomainAndApplication} is
     *                                     not null, this field will indicate if Elytron will be responsible for authentication
     * @throws ValidateException ValidateException in case of a validation error
     */
    public SecurityImpl(String securityDomain, String securityDomainAndApplication, boolean applicationManaged, boolean elytronEnabled) throws ValidateException {
        super(securityDomain, securityDomainAndApplication, applicationManaged);
        this.elytronEnabled = elytronEnabled;
    }

    /**
     * Indicates if Elytron is enabled. In this case, {@link #getSecurityDomain()} and
     * {@link #getSecurityDomainAndApplication()} both refer to an Elytron authentication context.
     *
     * @return {@code true} if is Elytron enabled
     */
    @Override
    public boolean isElytronEnabled() {
        return elytronEnabled;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return super.hashCode() + (elytronEnabled? 1: 0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (obj instanceof SecurityImpl) {
            SecurityImpl other = (SecurityImpl) obj;
            if (elytronEnabled != other.elytronEnabled)
                return false;
        }
        return super.equals(obj);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        if (!elytronEnabled)
            return super.toString();
        StringBuilder sb = new StringBuilder(1024);

        sb.append("<security>");
        sb.append("</").append(Security.Tag.ELYTRON_ENABLED).append(">");

        if (getSecurityDomain() != null) {
            sb.append("<").append(Security.Tag.AUTHENTICATION_CONTEXT).append("/>");
            sb.append(getSecurityDomain());
            sb.append("</").append(Security.Tag.AUTHENTICATION_CONTEXT).append("/>");
        } else {
            sb.append("<").append(Security.Tag.AUTHENTICATION_CONTEXT_AND_APPLICATION).append("/>");
            sb.append(getSecurityDomainAndApplication());
            sb.append("</").append(Security.Tag.AUTHENTICATION_CONTEXT_AND_APPLICATION).append("/>");
        }
        sb.append("</security>");
        return sb.toString();
    }
}

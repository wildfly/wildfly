/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
     * Deprecated. Elytron is enabled by default and this field is ignored.
     */
    private boolean elytronEnabled;

    /**
     * Constructor
     *
     * @param securityDomain               security domain managed authentication. Security domain will refer to
     *                                     an Elytron authentication context
     * @param securityDomainAndApplication securityDomain and application managed authentication. This field will refer
     *                                     to an Elytron authentication context
     * @param applicationManaged           application managed authentication
     * @throws ValidateException ValidateException in case of a validation error
     */
    public SecurityImpl(String securityDomain, String securityDomainAndApplication, boolean applicationManaged) throws ValidateException {
        super(securityDomain, securityDomainAndApplication, applicationManaged);
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
        StringBuilder sb = new StringBuilder(1024);

        sb.append("<security>");

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

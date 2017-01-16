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

import org.jboss.as.connector.metadata.api.common.Credential;
import org.jboss.jca.common.api.validator.ValidateException;

/**
 * Extension of {@link org.jboss.jca.common.metadata.common.CredentialImpl} with added Elytron support.
 *
 * @author Flavia Rainone
 */
public class CredentialImpl
        extends org.jboss.jca.common.metadata.common.CredentialImpl implements Credential {

    private static final long serialVersionUID = 7990943957924515091L;

    /**
     * Indicates if the Credential data belongs to Elytron or PicketBox.
     */
    private boolean elytronEnabled;

    /**
     * Create a new CredentialImpl.
     *
     * @param userName        user name
     * @param password        user password
     * @param securityContext specific information that helps implementation define which context this Credential belongs to
     * @param elytronEnabled  is the authentication performed by Elytron. If {@code true},  {@param securityContext}, defined as
     *                        securityDomain in super class, refers to an Elytron authentication context
     * @throws ValidateException ValidateException in case of validation error
     */
    public CredentialImpl(String userName, String password, String securityContext, boolean elytronEnabled)
            throws ValidateException {
        super(userName, password, securityContext);
        this.elytronEnabled = elytronEnabled;
    }

    /**
     * Indicates if Elytron is enabled. In this case, {@link #getSecurityDomain()}, refers to an Elytron authentication context
     *
     * @return {@code true} if is Elytron enabled
     */
    @Override
    public final boolean isElytronEnabled() {
        return elytronEnabled;
    }

    @Override
    public int hashCode() {
        return super.hashCode() + (elytronEnabled? 1: 0);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof CredentialImpl))
            return false;
        CredentialImpl other = (CredentialImpl) obj;
        return elytronEnabled == other.elytronEnabled && super.equals(other);
    }
}

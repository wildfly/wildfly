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
package org.jboss.as.connector.metadata.ds;

import java.util.Objects;

import org.jboss.as.connector.metadata.api.common.Credential;
import org.jboss.as.connector.metadata.api.ds.DsSecurity;
import org.jboss.as.connector.metadata.common.CredentialImpl;
import org.jboss.jca.common.api.metadata.common.Extension;
import org.jboss.jca.common.api.validator.ValidateException;
import org.wildfly.common.function.ExceptionSupplier;
import org.wildfly.security.credential.source.CredentialSource;

/**
 * Extension of {@link org.jboss.jca.common.metadata.ds.DsSecurityImpl} with added Elytron support.
 *
 * @author Flavia Rainone
 */
public class DsSecurityImpl extends CredentialImpl implements DsSecurity, Credential {

    private static final long serialVersionUID = 312322268048179001L;


    private final Extension reauthPlugin;



    /**
     * Create a new DsSecurityImpl.
     *
     * @param userName        user name
     * @param password        user password
     * @param securityContext specific information used by implementation to define in which context this user/password info
     *                        belongs
     * @param elytronEnabled  indicates if elytron is enabled. In this case, {@param securityContext}, defined as
     *                        securityDomain in super class, refers to an Elytron authentication context
     * @param reauthPlugin    reauthentication plugin
     * @param credentialSourceSupplier an Elytron credentia supplier
     * @throws ValidateException in case of validation error
     */
    public DsSecurityImpl(final String userName, final String password, final String securityContext, final boolean elytronEnabled,
                          final ExceptionSupplier<CredentialSource, Exception> credentialSourceSupplier, Extension reauthPlugin) throws ValidateException {
        super(userName, password, securityContext, elytronEnabled, credentialSourceSupplier);
        this.reauthPlugin = reauthPlugin;
        this.validate();
    }

    public Extension getReauthPlugin() {
        return this.reauthPlugin;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        DsSecurityImpl that = (DsSecurityImpl) o;
        return Objects.equals(reauthPlugin, that.reauthPlugin);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), reauthPlugin);
    }

    @Override
    public String toString() {
        return "DsSecurityImpl{" +
                "userName='" + getUserName() + '\'' +
                ", password='" + getPassword() + '\'' +
                ", securityDomain='" + getSecurityDomain() + '\'' +
                ", elytronEnabled=" + isElytronEnabled() +
                "reauthPlugin=" + reauthPlugin +
                '}';
    }
}


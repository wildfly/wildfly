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

import org.jboss.as.connector.logging.ConnectorLogger;
import org.jboss.as.connector.metadata.api.common.Credential;
import org.jboss.as.connector.metadata.api.ds.DsSecurity;
import org.jboss.jca.common.CommonBundle;
import org.jboss.jca.common.api.metadata.common.Extension;
import org.jboss.jca.common.api.validator.ValidateException;
import org.jboss.logging.Messages;
import org.wildfly.common.function.ExceptionSupplier;
import org.wildfly.security.credential.PasswordCredential;
import org.wildfly.security.credential.source.CredentialSource;
import org.wildfly.security.password.interfaces.ClearPassword;

/**
 * Extension of {@link org.jboss.jca.common.metadata.ds.DsSecurityImpl} with added Elytron support.
 *
 * @author Flavia Rainone
 */
public class DsSecurityImpl implements DsSecurity, Credential {

    private static final long serialVersionUID = 312322268048179001L;

    private static CommonBundle bundle = (CommonBundle) Messages.getBundle(CommonBundle.class);
    private final String userName;
    private final String password;
    private final String securityDomain;
    private final Extension reauthPlugin;

    /**
     * Indicates if the Credential data belongs to Elytron or PicketBox.
     */
    private boolean elytronEnabled;

    private final ExceptionSupplier<CredentialSource, Exception> credentialSourceSupplier;




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
            Extension reauthPlugin, final ExceptionSupplier<CredentialSource, Exception> credentialSourceSupplier) throws ValidateException {
        this.userName = userName;
        this.password = password;
        this.securityDomain = securityContext;
        this.reauthPlugin = reauthPlugin;
        this.elytronEnabled = elytronEnabled;
        this.credentialSourceSupplier = credentialSourceSupplier;
        this.validate();
    }

    public final String getSecurityDomain() {
        return this.securityDomain;
    }

    public final String resolveSecurityDomain() {
        return this.getSecurityDomain();
    }

    public final String getUserName() {
        return this.userName;
    }

    public final String getPassword() {
        if (credentialSourceSupplier != null) {
            try {
                return new String(
                        credentialSourceSupplier.get().getCredential(PasswordCredential.class).getPassword(ClearPassword.class).getPassword());
            } catch (Exception e) {
                throw ConnectorLogger.DEPLOYMENT_CONNECTOR_LOGGER.invalidCredentialSourceSupplier(e);
            }
        }
        return this.password;

    }

    public Extension getReauthPlugin() {
        return this.reauthPlugin;
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


    public void validate() throws ValidateException {
        if (this.userName != null && this.securityDomain != null) {
            throw new ValidateException(bundle.invalidSecurityConfiguration());
        }
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DsSecurityImpl that = (DsSecurityImpl) o;
        return elytronEnabled == that.elytronEnabled &&
                Objects.equals(userName, that.userName) &&
                Objects.equals(password, that.password) &&
                Objects.equals(securityDomain, that.securityDomain) &&
                Objects.equals(reauthPlugin, that.reauthPlugin) &&
                Objects.equals(credentialSourceSupplier, that.credentialSourceSupplier);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userName, password, securityDomain, reauthPlugin, elytronEnabled, credentialSourceSupplier);
    }

    @Override
    public String toString() {
        return "DsSecurityImpl{" +
                "userName='" + userName + '\'' +
                ", password='" + password + '\'' +
                ", securityDomain='" + securityDomain + '\'' +
                ", reauthPlugin=" + reauthPlugin +
                ", elytronEnabled=" + elytronEnabled +
                ", credentialSourceSupplier=" + credentialSourceSupplier +
                '}';
    }
}

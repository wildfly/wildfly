/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.connector.metadata.common;

import java.lang.invoke.MethodHandles;
import java.util.Objects;

import org.jboss.as.connector.logging.ConnectorLogger;
import org.jboss.as.connector.metadata.api.common.Credential;
import org.jboss.jca.common.CommonBundle;
import org.jboss.jca.common.api.validator.ValidateException;
import org.jboss.logging.Messages;
import org.wildfly.common.function.ExceptionSupplier;
import org.wildfly.security.credential.PasswordCredential;
import org.wildfly.security.credential.source.CredentialSource;
import org.wildfly.security.password.interfaces.ClearPassword;

/**
 * Extension of {@link org.jboss.jca.common.metadata.common.CredentialImpl} with added Elytron support.
 *
 * @author Flavia Rainone
 */
public class CredentialImpl implements Credential {

    private static final long serialVersionUID = 7990943957924515091L;

    private static CommonBundle bundle = Messages.getBundle(MethodHandles.lookup(), CommonBundle.class);
    private final String userName;
    private final String password;
    private final String securityDomain;
    private final ExceptionSupplier<CredentialSource, Exception> credentialSourceSupplier;

    /**
     * Create a new CredentialImpl.
     *
     * @param userName        user name
     * @param password        user password
     * @param securityContext specific information that helps implementation define which context this Credential belongs to
     * @throws ValidateException ValidateException in case of validation error
     */
    public CredentialImpl(final String userName, final String password, final String securityContext,
                          final ExceptionSupplier<CredentialSource, Exception> credentialSourceSupplier)
            throws ValidateException {
        this.userName = userName;
        this.password = password;
        this.securityDomain = securityContext;
        this.credentialSourceSupplier = credentialSourceSupplier;

    }

    public void validate() throws ValidateException {
        if (this.userName != null && this.securityDomain != null) {
            throw new ValidateException(bundle.invalidSecurityConfiguration());
        }
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CredentialImpl that = (CredentialImpl) o;
        return Objects.equals(userName, that.userName) &&
                Objects.equals(password, that.password) &&
                Objects.equals(securityDomain, that.securityDomain) &&
                Objects.equals(credentialSourceSupplier, that.credentialSourceSupplier);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userName, password, securityDomain, credentialSourceSupplier);
    }

    @Override
    public String toString() {
        return "CredentialImpl{" +
                "userName='" + userName + '\'' +
                ", password='" + password + '\'' +
                ", securityDomain='" + securityDomain + '\'' +
                ", credentialSourceSupplier=" + credentialSourceSupplier +
                '}';
    }
}

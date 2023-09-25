/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.picketlink.idm.model;

import java.util.HashMap;
import java.util.Map;

/**
 * <p>Enum defining alias for each supported built-in {@code org.picketlink.idm.credential.handler.CredentialHandler} provided by
 * PicketLink. The alias is used in the configuration without using the full qualified name of a type.</p>
 *
 * @author Pedro Igor
 */
public enum CredentialTypeEnum {

    // credential types
    PASSWORD_CREDENTIAL_HANDLER("PasswordHandler", "org.picketlink.idm.credential.handler.PasswordCredentialHandler"),
    LDAP_PASSWORD_CREDENTIAL_HANDLER("LDAPPasswordHandler", "org.picketlink.idm.ldap.internal.LDAPPlainTextPasswordCredentialHandler"),
    DIGEST_CREDENTIAL_HANDLER("DigestHandler", "org.picketlink.idm.credential.handler.DigestCredentialHandler"),
    X509_CERT_CREDENTIAL_HANDLER("X509CertHandler", "org.picketlink.idm.credential.handler.X509CertificateCredentialHandler");

    private static final Map<String, CredentialTypeEnum> types = new HashMap<String, CredentialTypeEnum>();

    static {
        for (CredentialTypeEnum element : values()) {
            types.put(element.getAlias(), element);
        }
    }

    private final String alias;
    private final String type;

    CredentialTypeEnum(String alias, String type) {
        this.alias = alias;
        this.type = type;
    }

    public static String forType(String alias) {
        CredentialTypeEnum resolvedType = types.get(alias);

        if (resolvedType != null) {
            return resolvedType.getType();
        }

        return null;
    }

    @Override
    public String toString() {
        return this.alias;
    }

    String getAlias() {
        return this.alias;
    }

    String getType() {
        return this.type;
    }
}

/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.security;

import java.util.HashMap;
import java.util.Map;

/**
 * Enum for the sub elements of the Security subsystem
 *
 * @author <a href="mailto:mmoyses@redhat.com">Marcus Moyses</a>
 */
enum Element {
    // must be first
    UNKNOWN(null),

    ACL(Constants.ACL),
    ACL_MODULE(Constants.ACL_MODULE),
    ADDITIONAL_PROPERTIES(Constants.ADDITIONAL_PROPERTIES),
    AUDIT(Constants.AUDIT),
    AUTH_MODULE(Constants.AUTH_MODULE),
    AUTHENTICATION("authentication"),
    AUTHENTICATION_JASPI("authentication-jaspi"),
    AUTHORIZATION("authorization"),
    IDENTITY_TRUST("identity-trust"),
    JSSE("jsse"),
    LOGIN_MODULE("login-module"),
    LOGIN_MODULE_STACK("login-module-stack"),
    MAPPING("mapping"),
    MAPPING_MODULE("mapping-module"),
    MODULE_OPTION("module-option"),
    POLICY_MODULE("policy-module"),
    PROVIDER_MODULE("provider-module"),
    PROPERTY("property"),
    SECURITY_DOMAIN("security-domain"),
    SECURITY_DOMAINS("security-domains"),
    SECURITY_MANAGEMENT("security-management"),
    SECURITY_PROPERTIES("security-properties"),
    SUBJECT_FACTORY("subject-factory"),
    TRUST_MODULE("trust-module"),
    VAULT("vault"),
    VAULT_OPTION("vault-option"),
    // ELYTRON INTEGRATION ELEMENTS
    ELYTRON_INTEGRATION(Constants.ELYTRON_INTEGRATION),
    SECURITY_REALMS(Constants.SECURITY_REALMS),
    ELYTRON_REALM(Constants.ELYTRON_REALM),
    TLS(Constants.TLS),
    ELYTRON_KEY_STORE(Constants.ELYTRON_KEY_STORE),
    ELYTRON_TRUST_STORE(Constants.ELYTRON_TRUST_STORE),
    ELYTRON_KEY_MANAGER(Constants.ELYTRON_KEY_MANAGER),
    ELYTRON_TRUST_MANAGER(Constants.ELYTRON_TRUST_MANAGER);

    private final String name;

    Element(final String name) {
        this.name = name;
    }

    /**
     * Get the local name of this element.
     *
     * @return the local name
     */
    public String getLocalName() {
        return name;
    }

    private static final Map<String, Element> MAP;

    static {
        final Map<String, Element> map = new HashMap<String, Element>();
        for (Element element : values()) {
            final String name = element.getLocalName();
            if (name != null)
                map.put(name, element);
        }
        MAP = map;
    }

    public static Element forName(String localName) {
        final Element element = MAP.get(localName);
        return element == null ? UNKNOWN : element;
    }

}

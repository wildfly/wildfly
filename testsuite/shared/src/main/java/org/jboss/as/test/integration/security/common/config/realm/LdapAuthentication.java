/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.security.common.config.realm;

/**
 *
 * @author Josef Cacek
 */
public class LdapAuthentication {

    // shared among ldap-connection and ldap authentication
    private final String connection;

    // ldap-connection attributes
    private final String searchDn;
    private final String searchCredential;
    private final String securityRealm;
    private final String url;
    private final String initialContextFactory;

    // ldap authentication attributes
    private final String advancedFilter;
    private final String baseDn;
    private final String userDn;
    private final Boolean recursive;
    private final String usernameAttribute;
    private final Boolean allowEmptyPasswords;

    // Constructors ----------------------------------------------------------

    /**
     * @param builder
     */
    private LdapAuthentication(final Builder builder) {
        this.connection = builder.connection;
        this.advancedFilter = builder.advancedFilter;
        this.baseDn = builder.baseDn;
        this.userDn = builder.userDn;
        this.recursive = builder.recursive;
        this.usernameAttribute = builder.usernameAttribute;
        this.allowEmptyPasswords = builder.allowEmptyPasswords;
        this.searchDn = builder.searchDn;
        this.searchCredential = builder.searchCredential;
        this.securityRealm = builder.securityRealm;
        this.url = builder.url;
        this.initialContextFactory = builder.initialContextFactory;
    }

    // Public methods --------------------------------------------------------

    /**
     * @return the connection
     */
    public String getConnection() {
        return connection;
    }

    /**
     * @return the advancedFilter
     */
    public String getAdvancedFilter() {
        return advancedFilter;
    }

    /**
     * @return the baseDn
     */
    public String getBaseDn() {
        return baseDn;
    }

    /**
     * @return the userDn
     */
    public String getUserDn() {
        return userDn;
    }

    /**
     * @return the usernameAttribute
     */
    public String getUsernameAttribute() {
        return usernameAttribute;
    }

    /**
     * @return the searchDn
     */
    public String getSearchDn() {
        return searchDn;
    }

    /**
     * @return the searchCredential
     */
    public String getSearchCredential() {
        return searchCredential;
    }

    /**
     * @return the securityRealm
     */
    public String getSecurityRealm() {
        return securityRealm;
    }

    /**
     * @return the url
     */
    public String getUrl() {
        return url;
    }

    /**
     * @return the initialContextFactory
     */
    public String getInitialContextFactory() {
        return initialContextFactory;
    }

    /**
     * @return the recursive
     */
    public Boolean getRecursive() {
        return recursive;
    }

    /**
     * @return the allowEmptyPasswords
     */
    public Boolean getAllowEmptyPasswords() {
        return allowEmptyPasswords;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "LdapAuthentication [connection=" + connection + ", searchDn=" + searchDn + ", searchCredential="
                + searchCredential + ", securityRealm=" + securityRealm + ", url=" + url + ", initialContextFactory="
                + initialContextFactory + ", advancedFilter=" + advancedFilter + ", baseDn=" + baseDn + ", userDn=" + userDn
                + ", recursive=" + recursive + ", usernameAttribute=" + usernameAttribute + ", allowEmptyPasswords="
                + allowEmptyPasswords + "]";
    }

    // Embedded classes ------------------------------------------------------

    public static class Builder {
        private String connection;

        private String searchDn;
        private String searchCredential;
        private String securityRealm;
        private String url;
        private String initialContextFactory;

        private String advancedFilter;
        private String baseDn;
        private String userDn;
        private Boolean recursive;
        private String usernameAttribute;
        private Boolean allowEmptyPasswords;

        public Builder connection(String connection) {
            this.connection = connection;
            return this;
        }

        public Builder advancedFilter(String advancedFilter) {
            this.advancedFilter = advancedFilter;
            return this;
        }

        public Builder baseDn(String baseDn) {
            this.baseDn = baseDn;
            return this;
        }

        public Builder userDn(String userDn) {
            this.userDn = userDn;
            return this;
        }

        public Builder recursive(Boolean recursive) {
            this.recursive = recursive;
            return this;
        }

        public Builder searchDn(String searchDn) {
            this.searchDn = searchDn;
            return this;
        }

        public Builder searchCredential(String searchCredential) {
            this.searchCredential = searchCredential;
            return this;
        }

        public Builder securityRealm(String securityRealm) {
            this.securityRealm = securityRealm;
            return this;
        }

        public Builder url(String url) {
            this.url = url;
            return this;
        }

        public Builder initialContextFactory(String initialContextFactory) {
            this.initialContextFactory = initialContextFactory;
            return this;
        }

        public Builder usernameAttribute(String usernameAttribute) {
            this.usernameAttribute = usernameAttribute;
            return this;
        }

        public Builder allowEmptyPasswords(Boolean allowEmptyPasswords) {
            this.allowEmptyPasswords = allowEmptyPasswords;
            return this;
        }

        public LdapAuthentication build() {
            return new LdapAuthentication(this);
        }
    }
}

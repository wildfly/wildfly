/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.jboss.security.ClientLoginModule;
import org.jboss.security.auth.spi.BaseCertLoginModule;
import org.jboss.security.auth.spi.CertRolesLoginModule;
import org.jboss.security.auth.spi.DatabaseCertLoginModule;
import org.jboss.security.auth.spi.DatabaseServerLoginModule;
import org.jboss.security.auth.spi.DisabledLoginModule;
import org.jboss.security.auth.spi.IdentityLoginModule;
import org.jboss.security.auth.spi.LdapExtLoginModule;
import org.jboss.security.auth.spi.LdapLoginModule;
import org.jboss.security.auth.spi.LdapUsersLoginModule;
import org.jboss.security.auth.spi.PropertiesUsersLoginModule;
import org.jboss.security.auth.spi.RoleMappingLoginModule;
import org.jboss.security.auth.spi.RunAsLoginModule;
import org.jboss.security.auth.spi.SimpleServerLoginModule;
import org.jboss.security.auth.spi.SimpleUsersLoginModule;
import org.jboss.security.auth.spi.UsersRolesLoginModule;
import org.jboss.security.mapping.providers.DeploymentRolesMappingProvider;
import org.jboss.security.mapping.providers.role.DatabaseRolesMappingProvider;
import org.jboss.security.mapping.providers.role.LdapRolesMappingProvider;
import org.jboss.security.mapping.providers.role.PropertiesRolesMappingProvider;
import org.jboss.security.mapping.providers.role.SimpleRolesMappingProvider;
import org.jboss.security.negotiation.AdvancedADLoginModule;
import org.jboss.security.negotiation.AdvancedLdapLoginModule;
import org.jboss.security.negotiation.spnego.SPNEGOLoginModule;

/**
 * A map for modules and their aliases.
 *
 * @author <a href="mailto:mmoyses@redhat.com">Marcus Moyses</a>
 */
public interface ModulesMap {

    Map<String, String> AUTHENTICATION_MAP = Collections.unmodifiableMap(new HashMap<String, String>() {

        private static final long serialVersionUID = -4524392693803008997L;

        {
            put("Client", ClientLoginModule.class.getName());
            put("Certificate", BaseCertLoginModule.class.getName());
            put("CertificateRoles", CertRolesLoginModule.class.getName());
            put("DatabaseCertificate", DatabaseCertLoginModule.class.getName());
            put("Database", DatabaseServerLoginModule.class.getName());
            put("Identity", IdentityLoginModule.class.getName());
            put("Ldap", LdapLoginModule.class.getName());
            put("LdapExtended", LdapExtLoginModule.class.getName());
            put("RoleMapping", RoleMappingLoginModule.class.getName());
            put("RunAs", RunAsLoginModule.class.getName());
            put("Simple", SimpleServerLoginModule.class.getName());
            put("UsersRoles", UsersRolesLoginModule.class.getName());
            put("Disabled", DisabledLoginModule.class.getName());
            // Authentication only modules
            put("PropertiesUsers", PropertiesUsersLoginModule.class.getName());
            put("SimpleUsers", SimpleUsersLoginModule.class.getName());
            put("CertificateUsers", BaseCertLoginModule.class.getName()); // duplicated here to maintain name pattern
            put("DatabaseUsers", DatabaseServerLoginModule.class.getName()); // duplicated here to maintain name pattern
            put("LdapUsers", LdapUsersLoginModule.class.getName());
            // Negotiation Related Modules
            put("Kerberos", "com.sun.security.auth.module.Krb5LoginModule");
            put("SPNEGO", SPNEGOLoginModule.class.getName());
            put("SPNEGOUsers", SPNEGOLoginModule.class.getName()); // duplicated here to maintain name pattern
            put("AdvancedLdap", AdvancedLdapLoginModule.class.getName());
            put("AdvancedAdLdap", AdvancedADLoginModule.class.getName());
        }
    });

    Map<String, String> MAPPING_MAP = Collections.unmodifiableMap(new HashMap<String, String>() {

        private static final long serialVersionUID = 1210873488987888574L;

        {
            put("PropertiesRoles", PropertiesRolesMappingProvider.class.getName());
            put("SimpleRoles", SimpleRolesMappingProvider.class.getName());
            put("DeploymentRoles", DeploymentRolesMappingProvider.class.getName());
            put("DatabaseRoles", DatabaseRolesMappingProvider.class.getName());
            put("LdapRoles", LdapRolesMappingProvider.class.getName());
        }

    });

}

/*
 * Copyright 2016 Red Hat, Inc.
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
package org.jboss.as.security.elytron;

import java.security.Principal;
import java.security.acl.Group;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.security.auth.Subject;

import org.jboss.as.security.logging.SecurityLogger;
import org.jboss.as.security.plugins.SecurityDomainContext;
import org.jboss.security.SecurityConstants;
import org.jboss.security.identity.Role;
import org.jboss.security.identity.RoleGroup;
import org.jboss.security.identity.plugins.SimpleRoleGroup;
import org.jboss.security.mapping.MappingContext;
import org.jboss.security.mapping.MappingType;
import org.wildfly.security.auth.SupportLevel;
import org.wildfly.security.auth.server.RealmIdentity;
import org.wildfly.security.auth.server.RealmUnavailableException;
import org.wildfly.security.auth.server.SecurityRealm;
import org.wildfly.security.authz.Attributes;
import org.wildfly.security.authz.AuthorizationIdentity;
import org.wildfly.security.authz.MapAttributes;
import org.wildfly.security.credential.Credential;
import org.wildfly.security.evidence.Evidence;
import org.wildfly.security.evidence.PasswordGuessEvidence;

/**
 * A {@link SecurityRealm} implementation that delegates credential verification to an underlying {@link SecurityDomainContext}.
 * This realm is exported as a capability by the legacy security subsystem by using the {@code elytron-realm} element that
 * is available in the {@code elytron-integration} section in the subsystem configuration. The example bellow illustrates
 * how to export a realm for the security domain {@code mydomain}:
 *
 * <pre>
 *     &lt;subsystem xmlns="urn:jboss:domain:security:2.0"&gt;
 *         &lt;security-domains&gt;
 *             &lt;security-domain name="mydomain" cache-type="default"&gt;
 *                 ...
 *             &lt;/security-domain&gt;
 *             ...
 *         &lt;/security-domains&gt;
 *         &lt;elytron-integration&gt;
 *             &lt;security-realms&gt;
 *                 &lt;elytron-realm name="LegacyRealm" legacy-jaas-config="mydomain" apply-role-mappers="false"/&gt;
 *             &lt;security-realms/&gt;
 *         &lt;/elytron-integration&gt;
 *         ...
 *     &lt;/subsystem&gt;
 * </pre>
 * <p/>
 * The value of the {@code name} attribute is used as the dynamic name of the exported realm. This is the name that must
 * be used in the {@code Elytron} subsystem to reference this realm. So, for the above example, an {@code Elytron}
 * configuration would look like this:
 *
 * <pre>
 *     &lt;subsystem xmlns="urn:wildfly:elytron:1.0"&gt;
 *         &lt;security-domains&gt;
 *             &lt;security-domain name="ApplicationDomain" default-realm="LegacyRealm"&gt;
 *                 &lt;realm name="LegacyRealm"/&gt;
 *             &lt;/security-domain&gt;
 *         &lt;/security-domains&gt;
 *         ...
 *     &lt;/subsystem&gt;
 * </pre>
 * <p/>
 * The above Elytron security domain can then be used anywhere in the Elytron subsystem (for example, to setup a
 * {@code http-authentication-factory}).
 * </p>
 * The {@code legacy-jaas-config} attribute MUST reference a valid legacy JAAS security domain. Failure to do so will result
 * in a dependency resolution error that will prevent the realm from being created.
 * </p>
 * The {@code apply-role-mappers} attribute, which defaults to {@code true}, indicates to the realm if any role mappers
 * defined in the legacy JAAS security domain should be applied to the roles retrieved from the authenticated {@link Subject}
 * when constructing the {@link AuthorizationIdentity}.
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
public class SecurityDomainContextRealm implements SecurityRealm {

    private SecurityDomainContext domainContext;

    private final boolean applyRoleMappers;

    public SecurityDomainContextRealm(final SecurityDomainContext context, final boolean applyRoleMappers) {
        this.domainContext = context;
        this.applyRoleMappers = applyRoleMappers;
    }

    @Override
    public RealmIdentity getRealmIdentity(Principal principal) throws RealmUnavailableException {
        return new PicketBoxBasedIdentity(principal);
    }

    public SupportLevel getCredentialAcquireSupport(Class<? extends Credential> credentialType, String algorithmName) throws RealmUnavailableException {
        return SupportLevel.UNSUPPORTED;
    }

    public SupportLevel getCredentialAcquireSupport(Class<? extends Credential> credentialType, String algorithmName, AlgorithmParameterSpec parameterSpec) throws RealmUnavailableException {
        return SupportLevel.UNSUPPORTED;
    }

    @Override
    public SupportLevel getEvidenceVerifySupport(Class<? extends Evidence> evidenceType, String algorithmName) throws RealmUnavailableException {
        if (PasswordGuessEvidence.class.isAssignableFrom(evidenceType)) {
            return SupportLevel.SUPPORTED;
        }
        return SupportLevel.UNSUPPORTED;
    }

    private class PicketBoxBasedIdentity implements RealmIdentity {

        private final Principal principal;

        private Subject authenticatedSubject;

        private PicketBoxBasedIdentity(final Principal principal) {
            this.principal = principal;
        }

        @Override
        public Principal getRealmIdentityPrincipal() {
            return principal;
        }

        public SupportLevel getCredentialAcquireSupport(Class<? extends Credential> credentialType, String algorithmName) throws RealmUnavailableException {
            return SecurityDomainContextRealm.this.getCredentialAcquireSupport(credentialType, algorithmName);
        }

        public SupportLevel getCredentialAcquireSupport(Class<? extends Credential> credentialType, String algorithmName, AlgorithmParameterSpec parameterSpec) throws RealmUnavailableException {
            return SecurityDomainContextRealm.this.getCredentialAcquireSupport(credentialType, algorithmName, parameterSpec);
        }

        @Override
        public <C extends Credential> C getCredential(Class<C> credentialType) throws RealmUnavailableException {
            return null;
        }

        @Override
        public SupportLevel getEvidenceVerifySupport(Class<? extends Evidence> evidenceType, String algorithmName) throws RealmUnavailableException {
            return SecurityDomainContextRealm.this.getEvidenceVerifySupport(evidenceType, algorithmName);
        }

        @Override
        public boolean verifyEvidence(Evidence evidence) throws RealmUnavailableException {
            if (domainContext == null || domainContext.getAuthenticationManager() == null) {
                throw new RealmUnavailableException();
            }
            else {
                final Subject jaasSubject = new Subject();
                Object jaasCredential = evidence;
                if (evidence instanceof PasswordGuessEvidence) {
                    jaasCredential = ((PasswordGuessEvidence) evidence).getGuess();
                }
                final boolean isValid = domainContext.getAuthenticationManager().isValid(principal, jaasCredential, jaasSubject);
                if (isValid) {
                    // set the authenticated subject when the authentication succeeds.
                    this.authenticatedSubject = jaasSubject;
                }
                return isValid;
            }
        }

        @Override
        public boolean exists() throws RealmUnavailableException {
            return this.authenticatedSubject != null;
        }

        @Override
        public AuthorizationIdentity getAuthorizationIdentity() throws RealmUnavailableException {
            if (this.authenticatedSubject == null){
                throw SecurityLogger.ROOT_LOGGER.unableToCreateAuthorizationIdentity();
            }
            Attributes attributes = null;
            /* process the JAAS subject, extracting attributes from groups that might have been set in the subject
               by the JAAS login modules (e.g. caller principal, roles) */
            final Set<Principal> principals = authenticatedSubject.getPrincipals();
            if (principals != null) {
                for (Principal principal : principals) {
                    if (principal instanceof Group) {
                        final Set<String> values = this.processGroup((Group) principal);
                        if (attributes == null) {
                            attributes = new MapAttributes();
                        }
                        attributes.addAll(principal.getName(), values);
                    }
                }
            }
            if (attributes == null)
                attributes = Attributes.EMPTY;
            return AuthorizationIdentity.basicIdentity(attributes);
        }

        private Set<String> processGroup(final Group group) {
            final Set<String> groupContents = new HashSet<>();

            // extract the principals from the Group.
            final Set<Principal> groupPrincipals = new HashSet<>();
            final Enumeration<? extends Principal> enumeration = group.members();
            while (enumeration.hasMoreElements()) {
                groupPrincipals.add(enumeration.nextElement());
            }

            // if the Group contains roles and role mapping has been enabled, map the roles found the in Group.
            if (applyRoleMappers && SecurityConstants.ROLES_IDENTIFIER.equals(group.getName()) && domainContext.getMappingManager() != null) {
                MappingContext<RoleGroup> mappingContext = domainContext.getMappingManager().getMappingContext(MappingType.ROLE.name());
                if (mappingContext != null && mappingContext.hasModules()) {
                    RoleGroup roleGroup = new SimpleRoleGroup(groupPrincipals);
                    Map<String, Object> contextMap = new HashMap<>();
                    contextMap.put(SecurityConstants.ROLES_IDENTIFIER, roleGroup);
                    if (this.principal != null) {
                        contextMap.put(SecurityConstants.PRINCIPAL_IDENTIFIER, this.principal);
                    }
                    mappingContext.performMapping(contextMap, roleGroup);

                    // at this point, roleGroup contains the mapped roles.
                    for (Role role : roleGroup.getRoles()) {
                        groupContents.add(role.getRoleName());
                    }
                }
            }
            if (groupContents.isEmpty()) {
                // Group has no roles, mapping has been disabled or no role mappers were found: simply return the Group contents.
                for (Principal rolePrincipal : groupPrincipals) {
                    groupContents.add(rolePrincipal.getName());
                }
            }
            return groupContents;
        }
    }
}

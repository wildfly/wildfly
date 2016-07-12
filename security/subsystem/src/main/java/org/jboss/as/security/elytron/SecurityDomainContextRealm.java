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
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

import javax.security.auth.Subject;

import org.jboss.as.security.plugins.SecurityDomainContext;
import org.wildfly.security.auth.principal.NamePrincipal;
import org.wildfly.security.auth.server.IdentityLocator;
import org.wildfly.security.auth.server.RealmIdentity;
import org.wildfly.security.auth.server.RealmUnavailableException;
import org.wildfly.security.auth.server.SecurityRealm;
import org.wildfly.security.auth.server.SupportLevel;
import org.wildfly.security.authz.Attributes;
import org.wildfly.security.authz.AuthorizationIdentity;
import org.wildfly.security.authz.MapAttributes;
import org.wildfly.security.credential.Credential;
import org.wildfly.security.evidence.Evidence;
import org.wildfly.security.evidence.PasswordGuessEvidence;

/**
 * A {@link org.wildfly.security.auth.server.SecurityRealm} implementation that delegates credential verification to an
 * underlying {@link org.jboss.as.security.plugins.SecurityDomainContext}. This realm is exported as a capability by the
 * legacy security subsystem by using the {@code elytron-realm} element that is available in the {@code elytron-integration}
 * section in the subsystem configuration. The example bellow illustrates how to export a realm for the security domain
 * {@code mydomain}:
 *
 * <pre>
 *     &lt;subsystem xmlns="urn:jboss:domain:security:3.0"&gt;
 *         &lt;security-domains&gt;
 *             &lt;security-domain name="mydomain" cache-type="default"&gt;
 *                 ...
 *             &lt;/security-domain&gt;
 *             ...
 *         &lt;/security-domains&gt;
 *         &lt;elytron-integration&gt;
 *             &lt;security-realms&gt;
 *                 &lt;elytron-realm name="LegacyRealm" legacy-jaas-config="mydomain"/&gt;
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
 * http-authentication-factory).
 * </p>
 * The {@code legacy-jaas-config} attribute MUST reference a valid legacy JAAS security domain. Failure to do so will result
 * in a dependency resolution error that will prevent the realm from being created.
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
public class SecurityDomainContextRealm implements SecurityRealm {

    private SecurityDomainContext domainContext;

    public SecurityDomainContextRealm(final SecurityDomainContext context) {
        this.domainContext = context;
    }

    @Override
    public RealmIdentity getRealmIdentity(IdentityLocator locator) throws RealmUnavailableException {
        return new PicketBoxBasedIdentity(locator.getName());
    }

    @Override
    public SupportLevel getCredentialAcquireSupport(Class<? extends Credential> credentialType, String algorithmName) throws RealmUnavailableException {
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

        private final String identityName;

        private Subject jaasSubject;

        private PicketBoxBasedIdentity(final String identityName) {
            this.identityName = identityName;
        }

        @Override
        public SupportLevel getCredentialAcquireSupport(Class<? extends Credential> credentialType, String algorithmName) throws RealmUnavailableException {
            return SecurityDomainContextRealm.this.getCredentialAcquireSupport(credentialType, algorithmName);
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
                jaasSubject = new Subject();
                Object jaasCredential = evidence;
                if (evidence instanceof PasswordGuessEvidence) {
                    jaasCredential = ((PasswordGuessEvidence) evidence).getGuess();
                }
                return domainContext.getAuthenticationManager().isValid(new NamePrincipal(identityName), jaasCredential, jaasSubject);
            }
        }

        @Override
        public boolean exists() throws RealmUnavailableException {
            return true;
        }

        @Override
        public AuthorizationIdentity getAuthorizationIdentity() throws RealmUnavailableException {
            Attributes attributes = null;
            if (this.jaasSubject != null) {
                /* process the JAAS subject, extracting attributes from groups that might have been set in the subject
                   by the JAAS login modules (e.g. caller principal, roles) */
                final Set<Principal> principals = jaasSubject.getPrincipals();
                if (principals != null) {
                    for (Principal principal : principals) {
                        if (principal instanceof Group) {
                            final String key = principal.getName();
                            final Set<String> values = new HashSet<>();
                            final Enumeration<? extends Principal> enumeration = ((Group) principal).members();
                            while (enumeration.hasMoreElements()) {
                                values.add(enumeration.nextElement().getName());
                            }
                            if (attributes == null) {
                                attributes = new MapAttributes();
                            }
                            attributes.addAll(key, values);
                        }
                    }
                }
            }
            if (attributes == null)
                attributes = Attributes.EMPTY;
            return AuthorizationIdentity.basicIdentity(attributes);
        }
    }
}

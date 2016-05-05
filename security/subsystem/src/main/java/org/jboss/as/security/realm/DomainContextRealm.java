/*
 *  JBoss, Home of Professional Open Source.
 *  Copyright 2015, Red Hat, Inc., and individual contributors
 *  as indicated by the @author tags. See the copyright.txt file in the
 *  distribution for a full listing of individual contributors.
 *
 *  This is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU Lesser General Public License as
 *  published by the Free Software Foundation; either version 2.1 of
 *  the License, or (at your option) any later version.
 *
 *  This software is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this software; if not, write to the Free
 *  Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 *  02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.security.realm;

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
 * <p>
 * A {@link org.wildfly.security.auth.server.SecurityRealm} implementation that delegates credential verification to the
 * underlying {@link org.jboss.as.security.plugins.SecurityDomainContext}. This realm is exported as a capability by the
 * security subsystem if the {@code export-elytron-realm} attribute is defined in the security domain configuration. The
 * example bellow illustrates how to export a realm for the security domain {@code legacy-domain}:
 *
 * <pre>
 *     &lt;security-domain name="legacy-domain" cache-type="default" export-elytron-realm="legacy-realm"&gt;
 *         ...
 *     &lt;/security-domain&gt;
 * </pre>
 *</p>
 * <p>
 * The value of {@code export-elytron-realm} attribute is used as the dynamic name of the exported realm. This is the
 * name that must be used in the {@code Elytron} subsystem to reference this {@link org.jboss.as.security.plugins.SecurityDomainContext}
 * based realm. So, for the above example, an {@code Elytron} configuration would look like this:
 *
 * <pre>
 *     &lt;subsystem xmlns="urn:wildfly:elytron:1.0"&gt;
 *         &lt;security-domains&gt;
 *             &lt;security-domain name="ApplicationDomain" default-realm="legacy-realm"&gt;
 *                 &lt;realm name="legacy-realm"/&gt;
 *             &lt;/security-domain&gt;
 *         &lt;/security-domains&gt;
 *     &lt;/subsystem&gt;
 * </pre>
 * </p>
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
public class DomainContextRealm implements SecurityRealm {

    private SecurityDomainContext domainContext;

    public DomainContextRealm(final SecurityDomainContext context) {
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
            return DomainContextRealm.this.getCredentialAcquireSupport(credentialType, algorithmName);
        }

        @Override
        public <C extends Credential> C getCredential(Class<C> credentialType) throws RealmUnavailableException {
            return null;
        }

        @Override
        public SupportLevel getEvidenceVerifySupport(Class<? extends Evidence> evidenceType, String algorithmName) throws RealmUnavailableException {
            return DomainContextRealm.this.getEvidenceVerifySupport(evidenceType, algorithmName);
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

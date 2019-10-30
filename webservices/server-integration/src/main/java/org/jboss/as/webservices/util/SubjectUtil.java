package org.jboss.as.webservices.util;
/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import java.security.AccessController;
import java.security.KeyPair;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.PrivilegedAction;
import java.security.PublicKey;
import java.security.acl.Group;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

import javax.crypto.SecretKey;
import javax.security.auth.Subject;

import org.wildfly.security.auth.principal.NamePrincipal;
import org.wildfly.security.auth.server.IdentityCredentials;
import org.wildfly.security.auth.server.SecurityDomain;
import org.wildfly.security.auth.server.SecurityIdentity;
import org.wildfly.security.authz.Roles;
import org.wildfly.security.credential.Credential;
import org.wildfly.security.credential.KeyPairCredential;
import org.wildfly.security.credential.PasswordCredential;
import org.wildfly.security.credential.PublicKeyCredential;
import org.wildfly.security.credential.SecretKeyCredential;
import org.wildfly.security.credential.X509CertificateChainPrivateCredential;
import org.wildfly.security.credential.X509CertificateChainPublicCredential;
import org.wildfly.security.manager.WildFlySecurityManager;
import org.wildfly.security.password.Password;
/**
 * Utilities for dealing with {@link Subject}.
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
public final class SubjectUtil {

    /**
     * Converts the supplied {@link SecurityIdentity} into a {@link Subject}.
     *
     * @param securityIdentity the {@link SecurityIdentity} to be converted.
     * @return the constructed {@link Subject} instance.
     */
    public static Subject fromSecurityIdentity(final SecurityIdentity securityIdentity) {
        return fromSecurityIdentity(securityIdentity, new Subject());
    }
    public static Subject fromSecurityIdentity(final SecurityIdentity securityIdentity, Subject subject) {
        if (subject == null) {
            subject = new Subject();
        }
        subject.getPrincipals().add(securityIdentity.getPrincipal());

        // add the 'Roles' group to the subject containing the identity's mapped roles.
        Group rolesGroup = new SimpleGroup("Roles");
        for (String role : securityIdentity.getRoles()) {
            rolesGroup.addMember(new NamePrincipal(role));
        }
        subject.getPrincipals().add(rolesGroup);

        // add a 'CallerPrincipal' group containing the identity's principal.
        Group callerPrincipalGroup = new SimpleGroup("CallerPrincipal");
        callerPrincipalGroup.addMember(securityIdentity.getPrincipal());
        subject.getPrincipals().add(callerPrincipalGroup);

        // process the identity's public and private credentials.
        for (Credential credential : securityIdentity.getPublicCredentials()) {
            if (credential instanceof PublicKeyCredential) {
                subject.getPublicCredentials().add(credential.castAs(PublicKeyCredential.class).getPublicKey());
            }
            else if (credential instanceof X509CertificateChainPublicCredential) {
                subject.getPublicCredentials().add(credential.castAs(X509CertificateChainPublicCredential.class).getCertificateChain());
            }
            else {
                subject.getPublicCredentials().add(credential);
            }
        }

        for (Credential credential : securityIdentity.getPrivateCredentials()) {
            if (credential instanceof PasswordCredential) {
                addPrivateCredential(subject, credential.castAs(PasswordCredential.class).getPassword());
            }
            else if (credential instanceof SecretKeyCredential) {
                addPrivateCredential(subject, credential.castAs(SecretKeyCredential.class).getSecretKey());
            }
            else if (credential instanceof KeyPairCredential) {
                addPrivateCredential(subject, credential.castAs(KeyPairCredential.class).getKeyPair());
            }
            else if (credential instanceof X509CertificateChainPrivateCredential) {
                addPrivateCredential(subject, credential.castAs(X509CertificateChainPrivateCredential.class).getCertificateChain());
            }
            else {
                addPrivateCredential(subject, credential);
            }
        }

        // add the identity itself as a private credential - integration code can interact with the SI instead of the Subject if desired.
        addPrivateCredential(subject, securityIdentity);

        return subject;
    }

    private static void addPrivateCredential(final Subject subject, final Object credential) {
        if (!WildFlySecurityManager.isChecking()) {
            subject.getPrivateCredentials().add(credential);
        }
        else {
            AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
                subject.getPrivateCredentials().add(credential);
                return null;
            });
        }
    }

    public static SecurityIdentity convertToSecurityIdentity(Subject subject, Principal principal, SecurityDomain domain,
            String roleCategory) {
        SecurityIdentity identity = domain.createAdHocIdentity(principal);
        // convert subject Group
        Set<String> roles = new HashSet<>();
        for (Principal prin : subject.getPrincipals()) {
            if (prin instanceof Group && "Roles".equalsIgnoreCase(prin.getName())) {
                Enumeration<? extends Principal> enumeration = ((Group) prin).members();
                while (enumeration.hasMoreElements()) {
                    roles.add(enumeration.nextElement().getName());
                }
            }
        }
        if (!roles.isEmpty()) {
            // identity.withRoleMapper will create NEW identity instance instead of set this roleMapper to identity
            identity = identity.withRoleMapper(roleCategory, (rolesToMap) -> Roles.fromSet(roles));
        }
        // convert public credentials
        IdentityCredentials publicCredentials = IdentityCredentials.NONE;
        for (Object credential : subject.getPublicCredentials()) {
            if (credential instanceof PublicKey) {
                publicCredentials = publicCredentials.withCredential(new PublicKeyCredential((PublicKey) credential));
            } else if (credential instanceof X509Certificate) {
                publicCredentials = publicCredentials.withCredential(new X509CertificateChainPublicCredential(
                        (X509Certificate) credential));
            } else if (credential instanceof Credential) {
                publicCredentials = publicCredentials.withCredential((Credential) credential);
            }
        }
        identity = identity.withPublicCredentials(publicCredentials);

        // convert private credentials
        IdentityCredentials privateCredentials = IdentityCredentials.NONE;
        for (Object credential : subject.getPrivateCredentials()) {
            if (credential instanceof Password) {
                privateCredentials = privateCredentials.withCredential(new PasswordCredential((Password) credential));
            } else if (credential instanceof SecretKey) {
                privateCredentials = privateCredentials.withCredential(new SecretKeyCredential((SecretKey) credential));
            } else if (credential instanceof KeyPair) {
                privateCredentials = privateCredentials.withCredential(new KeyPairCredential((KeyPair) credential));
            } else if (credential instanceof PrivateKey) {
                privateCredentials = privateCredentials.withCredential(new X509CertificateChainPrivateCredential(
                        (PrivateKey) credential));
            } else if (credential instanceof Credential) {
                privateCredentials = privateCredentials.withCredential((Credential) credential);
            }
        }
        identity = identity.withPrivateCredentials(privateCredentials);

        return identity;
    }


    private static class SimpleGroup implements Group {

        private final String name;

        private final Set<Principal> principals;

        SimpleGroup(final String name) {
            this.name = name;
            this.principals = new HashSet<>();
        }

        @Override
        public String getName() {
            return this.name;
        }

        @Override
        public boolean addMember(Principal principal) {
            return this.principals.add(principal);
        }

        @Override
        public boolean removeMember(Principal principal) {
            return this.principals.remove(principal);
        }

        @Override
        public Enumeration<? extends Principal> members() {
            return Collections.enumeration(this.principals);
        }

        @Override
        public boolean isMember(Principal principal) {
            return this.principals.contains(principal);
        }
    }
}

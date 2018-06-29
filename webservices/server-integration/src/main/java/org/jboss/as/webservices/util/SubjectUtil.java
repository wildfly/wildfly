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
import java.security.Principal;
import java.security.PrivilegedAction;
import java.security.acl.Group;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

import javax.security.auth.Subject;

import org.wildfly.security.auth.principal.NamePrincipal;
import org.wildfly.security.auth.server.SecurityIdentity;
import org.wildfly.security.credential.Credential;
import org.wildfly.security.credential.KeyPairCredential;
import org.wildfly.security.credential.PasswordCredential;
import org.wildfly.security.credential.PublicKeyCredential;
import org.wildfly.security.credential.SecretKeyCredential;
import org.wildfly.security.credential.X509CertificateChainPrivateCredential;
import org.wildfly.security.credential.X509CertificateChainPublicCredential;
import org.wildfly.security.manager.WildFlySecurityManager;

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

/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.elytron.http;

import org.wildfly.security.auth.SupportLevel;
import org.wildfly.security.auth.server.RealmIdentity;
import org.wildfly.security.auth.server.RealmUnavailableException;
import org.wildfly.security.auth.server.SecurityRealm;
import org.wildfly.security.auth.server.event.RealmEvent;
import org.wildfly.security.auth.server.event.RealmFailedAuthenticationEvent;
import org.wildfly.security.auth.server.event.RealmSuccessfulAuthenticationEvent;
import org.wildfly.security.credential.Credential;
import org.wildfly.security.evidence.Evidence;
import org.wildfly.security.evidence.PasswordGuessEvidence;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Principal;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Custom Security Realm used by the SetRequestInformationCallbackMechanismFactoryTestCase
 */
public class CustomRealm implements SecurityRealm {
    public String pathToLogFile;

    public void initialize(Map<String, String> configuration) {
        pathToLogFile = configuration.get("pathToLogFile");
    }

    // this realm does not allow acquiring credentials
    public SupportLevel getCredentialAcquireSupport(Class<? extends Credential> credentialType, String algorithmName,
                                                    AlgorithmParameterSpec parameterSpec) {
        return SupportLevel.UNSUPPORTED;
    }

    // this realm will be able to verify password evidences only
    public SupportLevel getEvidenceVerifySupport(Class<? extends Evidence> evidenceType, String algorithmName) {
        return PasswordGuessEvidence.class.isAssignableFrom(evidenceType) ? SupportLevel.POSSIBLY_SUPPORTED : SupportLevel.UNSUPPORTED;
    }

    public RealmIdentity getRealmIdentity(final Principal principal) {

        return new RealmIdentity() {
            public Principal getRealmIdentityPrincipal() {
                return principal;
            }

            public SupportLevel getCredentialAcquireSupport(Class<? extends Credential> credentialType,
                                                            String algorithmName, AlgorithmParameterSpec parameterSpec) throws RealmUnavailableException {
                return SupportLevel.UNSUPPORTED;
            }

            public <C extends Credential> C getCredential(Class<C> credentialType) throws RealmUnavailableException {
                return null;
            }

            public SupportLevel getEvidenceVerifySupport(Class<? extends Evidence> evidenceType, String algorithmName) {
                return PasswordGuessEvidence.class.isAssignableFrom(evidenceType) ? SupportLevel.SUPPORTED : SupportLevel.UNSUPPORTED;
            }

            // evidence will be accepted if it is password "mypassword"
            public boolean verifyEvidence(Evidence evidence) {
                if (evidence instanceof PasswordGuessEvidence) {
                    PasswordGuessEvidence guess = (PasswordGuessEvidence) evidence;
                    try {
                        return Arrays.equals("myadmin".toCharArray(), principal.getName().toCharArray()) && Arrays.equals("mypassword".toCharArray(), guess.getGuess());
                    } finally {
                        guess.destroy();
                    }
                }
                return false;
            }

            public boolean exists() {
                return true;
            }
        };
    }

    @Override
    public void handleRealmEvent(RealmEvent event) {
        if (event instanceof RealmSuccessfulAuthenticationEvent) {
            if (!Objects.equals(((RealmSuccessfulAuthenticationEvent) event).getRealmIdentity().getRealmIdentityPrincipal().getName(), "myadmin") ||
                    ((RealmSuccessfulAuthenticationEvent) event).getAuthorizationIdentity().getRuntimeAttributes().get("Request-URI") == null ||
                    ((RealmSuccessfulAuthenticationEvent) event).getAuthorizationIdentity().getRuntimeAttributes().get("Request-URI").get(0) == null ||
                    !((RealmSuccessfulAuthenticationEvent) event).getAuthorizationIdentity().getRuntimeAttributes().get("Request-URI").get(0).contains("localhost") ||
                    ((RealmSuccessfulAuthenticationEvent) event).getAuthorizationIdentity().getRuntimeAttributes().get("Source-Address").isEmpty()
            ) {
                writeExceptionToLogFile();
            }
        } else if (event instanceof RealmFailedAuthenticationEvent) {
            try {
                if (!Objects.equals(((RealmFailedAuthenticationEvent) event).getRealmIdentity().getRealmIdentityPrincipal().getName(), "wrongUser") ||
                        ((RealmFailedAuthenticationEvent) event).getRealmIdentity().getAuthorizationIdentity().getRuntimeAttributes().get("Request-URI") == null ||
                        ((RealmFailedAuthenticationEvent) event).getRealmIdentity().getAuthorizationIdentity().getRuntimeAttributes().get("Request-URI").get(0) == null ||
                        !((RealmFailedAuthenticationEvent) event).getRealmIdentity().getAuthorizationIdentity().getRuntimeAttributes().get("Request-URI").get(0).contains("localhost") ||
                        ((RealmFailedAuthenticationEvent) event).getRealmIdentity().getAuthorizationIdentity().getRuntimeAttributes().get("Source-Address").isEmpty()
                ) {
                    writeExceptionToLogFile();
                }
            } catch (RealmUnavailableException e) {
                writeExceptionToLogFile();
                throw new RuntimeException(e);
            }
        }
    }

    private void writeExceptionToLogFile() {
        List<String> lines = List.of("Assertion exception occurred!");
        Path file = Paths.get(pathToLogFile);
        try {
            Files.write(file, lines, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
}

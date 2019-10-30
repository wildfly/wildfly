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
package org.wildfly.test.manual.elytron.seccontext;

/**
 * Enum of ways which can be used for (re)authentication or identity propagation in Security context propagation tests.
 *
 * @see SeccontextUtil#switchIdentity(String, String, java.util.concurrent.Callable, ReAuthnType)
 * @author Josef Cacek
 */
public enum ReAuthnType {

    /**
     * Don't use any reauthentication. Just call the code.
     */
    NO_REAUTHN,
    /**
     * Configure the current identity to be forwarded with own credentials.
     * ({@code AuthenticationConfiguration.useForwardedIdentity(SecurityDomain.getCurrent())})
     */
    FORWARDED_AUTHENTICATION,
    /**
     * Configure the current identity to be forwarded as authorization (without own credentials).
     * ({@code AuthenticationConfiguration.useForwardedIdentity(SecurityDomain.getCurrent())})
     */
    FORWARDED_AUTHORIZATION,
    /**
     * Use AuthenticationConfiguration to configure new identity to be used in Elytron. ({@code AuthenticationConfiguration.useName(name).usePassword(password)})
     */
    AC_AUTHENTICATION,
    /**
     * Use AuthenticationConfiguration to configure new authorization to be used in Elytron. ({@code AuthenticationConfiguration.useAuthorizationName})
     */
    AC_AUTHORIZATION,
    /**
     * Use Elytron SecurityDomain API to (re-)authenticate to the current security domain.
     * ({@code SecurityDomain.getCurrent().authenticate(username, new PasswordGuessEvidence(password))})
     */
    SD_AUTHENTICATION,
    /**
     * Use Elytron SecurityDomain API to (re-)authenticate to the current security domain and then configure it to forward the
     * identity. (It's a wrapped {@link #FORWARDED_IDENTITY} within the {@link #SECURITY_DOMAIN_AUTHENTICATE}.)
     */
    SD_AUTHENTICATION_FORWARDED
}

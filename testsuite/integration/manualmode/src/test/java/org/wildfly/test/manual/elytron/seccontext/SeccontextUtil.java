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

import java.util.Properties;
import java.util.concurrent.Callable;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.wildfly.security.auth.client.AuthenticationConfiguration;
import org.wildfly.security.auth.client.AuthenticationContext;
import org.wildfly.security.auth.client.MatchRule;
import org.wildfly.security.auth.server.SecurityDomain;
import org.wildfly.security.evidence.PasswordGuessEvidence;
import org.wildfly.security.sasl.SaslMechanismSelector;

/**
 * Util class for Elytron security context propagation tests. It helps to switch identities and lookup EJBs.
 *
 * @author Josef Cacek
 */
public class SeccontextUtil {

    public static final String SERVER1 = "seccontext-server1";
    public static final String SERVER2 = "seccontext-server2";

    /**
     * Method which handles {@link ReAuthnType} types by using Elytron API. Based on provided type new
     * {@link AuthenticationContext} is created and given callable is called within the context.
     *
     * @param username login name used for reauthentication scenarios (or null)
     * @param password password used for reauthentication scenarios (or null)
     * @param callable logic to be executed in the requested AuthenticationContext
     * @param type reauthentication type
     * @return result of the callable call
     */
    public static <T> T switchIdentity(final String username, final String password, final Callable<T> callable,
            ReAuthnType type) throws Exception {
        if (type == null) {
            type = ReAuthnType.AUTHENTICATION_CONTEXT;
        }
        switch (type) {
            case FORWARDED_IDENTITY:
                return AuthenticationContext.empty().with(MatchRule.ALL, AuthenticationConfiguration.empty()
                        .useForwardedIdentity(SecurityDomain.getCurrent()).setSaslMechanismSelector(SaslMechanismSelector.ALL))
                        .runCallable(callable);
            case AUTHENTICATION_CONTEXT:
                AuthenticationConfiguration authCfg = AuthenticationConfiguration.empty()
                        .setSaslMechanismSelector(SaslMechanismSelector.ALL);
                if (username != null) {
                    authCfg = authCfg.useName(username);
                }
                if (password != null) {
                    authCfg = authCfg.usePassword(password);
                }
                return AuthenticationContext.empty().with(MatchRule.ALL, authCfg).runCallable(callable);
            case SECURITY_DOMAIN_AUTHENTICATE:
                return password == null ? null
                        : SecurityDomain.getCurrent().authenticate(username, new PasswordGuessEvidence(password.toCharArray()))
                                .runAs(callable);
            case SECURITY_DOMAIN_AUTHENTICATE_FORWARDED:
                final Callable<T> forwardIdentityCallable = () -> {
                    return AuthenticationContext.empty()
                            .with(MatchRule.ALL,
                                    AuthenticationConfiguration.empty().useForwardedIdentity(SecurityDomain.getCurrent())
                                            .setSaslMechanismSelector(SaslMechanismSelector.ALL))
                            .runCallable(callable);
                };
                return password == null ? null
                        : SecurityDomain.getCurrent().authenticate(username, new PasswordGuessEvidence(password.toCharArray()))
                                .runAs(forwardIdentityCallable);
            case NO_REAUTHN:
            default:
                return callable.call();
        }
    }

    /**
     * Creates "ejb:/..." name for JNDI lookup.
     *
     * @return name to be used for EJB lookup.
     */
    public static String getRemoteEjbName(String appName, String beanSimpleNameBase, String remoteInterfaceName, boolean stateful) {
        return "ejb:/" + appName + "/" + beanSimpleNameBase + (stateful ? "SFSB!" : "!") + remoteInterfaceName
                + (stateful ? "?stateful" : "");
    }

    /**
     * Do JNDI lookup.
     */
    @SuppressWarnings("unchecked")
    public static <T> T lookup(String name, String providerUrl) throws NamingException {
        final Properties jndiProperties = new Properties();
        jndiProperties.put(Context.INITIAL_CONTEXT_FACTORY, "org.wildfly.naming.client.WildFlyInitialContextFactory");
        jndiProperties.put(Context.PROVIDER_URL, providerUrl);
        final Context context = new InitialContext(jndiProperties);
        return (T) context.lookup(name);
    }
}

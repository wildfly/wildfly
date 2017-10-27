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

    /**
     * Name of the first (entry) server in arquillian configuration.
     */
    public static final String SERVER1 = "seccontext-server1";
    public static final String SERVER1_BACKUP = "seccontext-server1-backup";
    /**
     * Name of the second (target) server in arquillian configuration.
     */
    public static final String SERVER2 = "seccontext-server2";
    /**
     * Name of the third (target for server chain) server in arquillian configuration.
     */
    public static final String SERVER3 = "seccontext-server3";

    /**
     * Name of deployment which contains EntryBean EJB.
     */
    public static final String JAR_ENTRY_EJB = "entry-ejb";
    /**
     * Name of deployment which contains WhoAmI bean and WhoAmIServlet.
     */
    public static final String WAR_WHOAMI = "whoami";
    /**
     * Name of deployment which contains secured EntryServlet with BASIC HTTP authentication mechanism configured.
     */
    public static final String WAR_ENTRY_SERVLET_BASIC = "entry-servlet-basic";
    /**
     * Name of deployment which contains secured EntryServlet with FORM HTTP authentication mechanism configured.
     */
    public static final String WAR_ENTRY_SERVLET_FORM = "entry-servlet-form";
    /**
     * Name of deployment which contains secured EntryServlet with BEARER_TOKEN HTTP authentication mechanism configured.
     */
    public static final String WAR_ENTRY_SERVLET_BEARER_TOKEN = "entry-servlet-bearer";

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
        return switchIdentity(username, password, null, callable, type);
    }

    /**
     * Method which handles {@link ReAuthnType} types by using Elytron API. Based on provided type new
     * {@link AuthenticationContext} is created and given callable is called within the context.
     *
     * @param username login name used for reauthentication scenarios (or null)
     * @param password password used for reauthentication scenarios (or null)
     * @param authzName used for authorization name
     * @param callable logic to be executed in the requested AuthenticationContext
     * @param type reauthentication type
     * @return result of the callable call
     */
    public static <T> T switchIdentity(final String username, final String password, final String authzName,
        final Callable<T> callable, ReAuthnType type) throws Exception {

        if (type == null) {
            type = ReAuthnType.AC_AUTHENTICATION;
        }
        final SecurityDomain securityDomain = SecurityDomain.getCurrent();
        AuthenticationConfiguration authCfg = AuthenticationConfiguration.empty()
                .setSaslMechanismSelector(SaslMechanismSelector.ALL);
        switch (type) {
            case FORWARDED_AUTHENTICATION:
                return AuthenticationContext.empty().with(MatchRule.ALL, authCfg.useForwardedIdentity(securityDomain))
                        .runCallable(callable);
            case FORWARDED_AUTHORIZATION:
                authCfg = authCfg.useForwardedAuthorizationIdentity(securityDomain);
                // fall through
            case AC_AUTHENTICATION:
                if (username != null) {
                    authCfg = authCfg.useName(username);
                }
                if (password != null) {
                    authCfg = authCfg.usePassword(password);
                }
                return AuthenticationContext.empty().with(MatchRule.ALL, authCfg).runCallable(callable);
            case AC_AUTHORIZATION:
                if (username != null) {
                    authCfg = authCfg.useName(username);
                }
                if (password != null) {
                    authCfg = authCfg.usePassword(password);
                }
                if (authzName != null) {
                    authCfg = authCfg.useAuthorizationName(authzName);
                }
                return AuthenticationContext.empty().with(MatchRule.ALL, authCfg).runCallable(callable);
            case SD_AUTHENTICATION:
                return password == null ? null
                        : securityDomain.authenticate(username, new PasswordGuessEvidence(password.toCharArray()))
                                .runAs(callable);
            case SD_AUTHENTICATION_FORWARDED:
                final Callable<T> forwardIdentityCallable = () -> {
                    return AuthenticationContext.empty()
                            .with(MatchRule.ALL,
                                    AuthenticationConfiguration.empty()
                                    .setSaslMechanismSelector(SaslMechanismSelector.ALL)
                                    .useForwardedIdentity(securityDomain))
                            .runCallable(callable);
                };
                return password == null ? null
                        : securityDomain.authenticate(username, new PasswordGuessEvidence(password.toCharArray()))
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
    public static String getRemoteEjbName(String appName, String beanSimpleNameBase, String remoteInterfaceName,
            boolean stateful) {
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

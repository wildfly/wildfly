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

package org.jboss.as.test.multinode.security.config;

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.test.integration.management.util.CLIWrapper;

/**
 * @author bmaxwell
 *
 */
public class PlainClientConfiguration {

    private static String applicationSecurityDomain = "other";
    private static String httpAuthenticationFactory = "application-http-authentication";
    private static String authenticationConfiguration = "forwardit";
    private static String securityDomain = "ApplicationDomain";
    private static String saslMechanismSelector = "#ALL";
    private static String authenticationContext = "forwardctx";

    public static ConfigChange[] getClientConfigChanges() {
        return new ConfigChange[] {
                new Step1_AddElytronAuthenticationContext(authenticationConfiguration, securityDomain, saslMechanismSelector,
                        authenticationContext),
                new Step2_ConfigureElytronDefaultAuthenticationContext(authenticationContext),
                new Step3_AddUndertowApplicationSecurityDomain(applicationSecurityDomain, httpAuthenticationFactory) };
    }

    public static class Step1_AddElytronAuthenticationContext implements ConfigChange {

        private String authenticationConfiguration; // forwardit
        private String securityDomain; // ApplicationDomain
        private String saslMechanismSelector; // #ALL
        private String authenticationContext; // forwardctx

        public Step1_AddElytronAuthenticationContext(String authenticationConfiguration, String securityDomain,
                String saslMechanismSelector, String authenticationContext) {
            this.authenticationConfiguration = authenticationConfiguration;
            this.securityDomain = securityDomain;
            this.saslMechanismSelector = saslMechanismSelector;
            this.authenticationContext = authenticationContext;
        }

        @Override
        public void apply(ModelControllerClient client, CLIWrapper cli) throws Exception {
            cli.sendLine(String.format(
                    "/subsystem=elytron/authentication-configuration=%s:add(security-domain=%s, sasl-mechanism-selector=%s)",
                    authenticationConfiguration, securityDomain, saslMechanismSelector));

            cli.sendLine(String.format(
                    "/subsystem=elytron/authentication-context=%s:add(match-rules=[{match-no-user=true, authentication-configuration=%s}])",
                    authenticationContext, authenticationConfiguration));
        }

        @Override
        public void revert(ModelControllerClient client, CLIWrapper cli) throws Exception {
            cli.sendLine(String.format("/subsystem=elytron/authentication-context=%s:remove()", authenticationContext));

            cli.sendLine(
                    String.format("/subsystem=elytron/authentication-configuration=%s:remove()", authenticationConfiguration));
        }
    }

    public static class Step2_ConfigureElytronDefaultAuthenticationContext implements ConfigChange {

        private String defaultAuthenticationContext;

        public Step2_ConfigureElytronDefaultAuthenticationContext(String defaultAuthenticationContext) {
            this.defaultAuthenticationContext = defaultAuthenticationContext;
        }

        @Override
        public void apply(ModelControllerClient client, CLIWrapper cli) throws Exception {
            cli.sendLine("/subsystem=elytron:write-attribute(name=default-authentication-context, value="
                    + defaultAuthenticationContext + ")");
        }

        @Override
        public void revert(ModelControllerClient client, CLIWrapper cli) throws Exception {
            cli.sendLine("/subsystem=elytron:undefine-attribute(name=default-authentication-context)");
        }
    }

    public static class Step3_AddUndertowApplicationSecurityDomain implements ConfigChange {

        private String applicationSecurityDomain;
        private String httpAuthenticationFactory;

        public Step3_AddUndertowApplicationSecurityDomain(String applicationSecurityDomain, String httpAuthenticationFactory) {
            this.applicationSecurityDomain = applicationSecurityDomain;
            this.httpAuthenticationFactory = httpAuthenticationFactory;
        }

        @Override
        public void apply(ModelControllerClient client, CLIWrapper cli) throws Exception {
            cli.sendLine(String.format("/subsystem=undertow/application-security-domain=%s:add(http-authentication-factory=%s)",
                    applicationSecurityDomain, httpAuthenticationFactory));
        }

        @Override
        public void revert(ModelControllerClient client, CLIWrapper cli) throws Exception {
            cli.sendLine(
                    String.format("/subsystem=undertow/application-security-domain=%s:remove()", applicationSecurityDomain));
        }
    }
}
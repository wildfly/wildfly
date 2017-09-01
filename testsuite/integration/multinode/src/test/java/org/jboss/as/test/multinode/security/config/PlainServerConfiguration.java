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
import org.jboss.as.test.multinode.security.config.PlainServerConfiguration.Step2_ReconfigureElytronSaslAuthenticationFactory.Step3_ConfigureRemotingHttpConnectorApplicationAuthentication;

/**
 * @author bmaxwell
 *
 */
public class PlainServerConfiguration {

    public static ConfigChange[] getServerConfigChanges() {
        return new ConfigChange[] { new Step1_AddEjb3ApplicationSecurityDomain(),
                new Step2_ReconfigureElytronSaslAuthenticationFactory(),
                new Step3_ConfigureRemotingHttpConnectorApplicationAuthentication() };
    }

    // step 1
    public static class Step1_AddEjb3ApplicationSecurityDomain implements ConfigChange {
        @Override
        public void apply(ModelControllerClient client, CLIWrapper cli) throws Exception {
            cli.sendLine("/subsystem=ejb3/application-security-domain=other:add(security-domain=ApplicationDomain)");
        }

        @Override
        public void revert(ModelControllerClient client, CLIWrapper cli) throws Exception {
            cli.sendLine("/subsystem=ejb3/application-security-domain=other:remove()");
        }
    }

    // step 2
    public static class Step2_ReconfigureElytronSaslAuthenticationFactory implements ConfigChange {

        @Override
        public void apply(ModelControllerClient client, CLIWrapper cli) throws Exception {
            cli.sendLine("/subsystem=elytron/sasl-authentication-factory=application-sasl-authentication:remove()");

            StringBuilder sb = new StringBuilder(
                    "/subsystem=elytron/sasl-authentication-factory=application-sasl-authentication:add(");
            sb.append("sasl-server-factory=configured, security-domain=ApplicationDomain, ");
            sb.append("mechanism-configurations=[");
            // sb.append("{mechanism-name=JBOSS-LOCAL-USER, realm-mapper=local},");
            sb.append("{mechanism-name=PLAIN, mechanism-realm-configurations=[{realm-name=ApplicationRealm}]}");
            sb.append("])");
            cli.sendLine(sb.toString());
        }

        @Override
        public void revert(ModelControllerClient client, CLIWrapper cli) throws Exception {
            cli.sendLine("/subsystem=elytron/sasl-authentication-factory=application-sasl-authentication:remove()");
            StringBuilder sb = new StringBuilder(
                    "/subsystem=elytron/sasl-authentication-factory=application-sasl-authentication:add(");
            sb.append("sasl-server-factory=configured, security-domain=ApplicationDomain, ");
            sb.append("mechanism-configurations=[");
            sb.append("{mechanism-name=JBOSS-LOCAL-USER, realm-mapper=local},");
            sb.append("{mechanism-name=DIGEST-MD5, mechanism-realm-configurations=[{realm-name=ApplicationRealm}]}");
            sb.append("])");
            cli.sendLine(sb.toString());
        }

        // step 3
        public static class Step3_ConfigureRemotingHttpConnectorApplicationAuthentication implements ConfigChange {

            @Override
            public void apply(ModelControllerClient client, CLIWrapper cli) throws Exception {
                cli.sendLine(
                        "/subsystem=remoting/http-connector=http-remoting-connector:write-attribute(name=sasl-authentication-factory, value=application-sasl-authentication)");
            }

            @Override
            public void revert(ModelControllerClient client, CLIWrapper cli) throws Exception {
                cli.sendLine(
                        "/subsystem=remoting/http-connector=http-remoting-connector:undefine-attribute(name=sasl-authentication-factory)");
            }
        }
    }
}
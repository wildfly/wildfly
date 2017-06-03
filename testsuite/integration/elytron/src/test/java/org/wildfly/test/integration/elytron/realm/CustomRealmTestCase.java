/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.wildfly.test.integration.elytron.realm;

import java.io.File;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.management.util.CLIWrapper;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.as.test.shared.util.JarUtils;
import org.junit.runner.RunWith;

/**
 * Smoke test for Elytron Custom Realm. It tests only basic functionality of Custom Realm. <br>
 *
 * Given: Deployed secured application deployment for printing roles<br>
 * and using BASIC authentication<br>
 * and using custom-realm.
 *
 * @author olukas
 * @author Hynek Švábek <hsvabek@redhat.com>
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup({ CustomRealmTestCase.SetupTask.class })
public class CustomRealmTestCase extends AbstractCustomRealmTestCase {

    private static final String CUSTOM_REALM_MODULE_NAME = "org.jboss.customrealmimpl";

    static class SetupTask implements ServerSetupTask {
        private static final String CUSTOM_REALM_NAME = "simpleCustomRealm";
        private static final String CUSTOM_REALM_RELATED_CONFIGURATION_NAME = "elytronCustomRealmRelatedConfiguration";
        private static final String PREDEFINED_HTTP_SERVER_MECHANISM_FACTORY = "global";

        @Override
        public void setup(ManagementClient mc, String string) throws Exception {
            try (CLIWrapper cli = new CLIWrapper(true)) {

                File moduleJar = JarUtils.createJarFile("testJar", CustomRealmImpl.class);
                cli.sendLine("module add --name=" + CUSTOM_REALM_MODULE_NAME
                    + " --slot=main --dependencies=org.wildfly.security.elytron,org.wildfly.extension.elytron --resources="
                    + moduleJar.getAbsolutePath());

                StringBuilder configuration = new StringBuilder(USER_WITHOUT_ROLE + "=" + CORRECT_PASSWORD);
                configuration.append(",").append(USER_WITH_ONE_ROLE).append("=").append(CORRECT_PASSWORD);
                configuration.append(",").append(USER_WITH_ONE_ROLE + "_ROLES").append("=").append("Role1");

                cli.sendLine(
                    String.format("/subsystem=elytron/custom-realm=%s:add(class-name=%s, module=%s, configuration={%s})",
                        CUSTOM_REALM_NAME, CustomRealmImpl.class.getName(), CUSTOM_REALM_MODULE_NAME,
                        configuration.toString()));
                cli.sendLine(String.format(
                    "/subsystem=elytron/security-domain=%1$s:add(realms=[{realm=%2$s,role-decoder=groups-to-roles}],default-realm=%2$s,permission-mapper=default-permission-mapper)",
                    CUSTOM_REALM_RELATED_CONFIGURATION_NAME, CUSTOM_REALM_NAME));
                cli.sendLine(String.format(
                    "/subsystem=elytron/http-authentication-factory=%1$s:add(http-server-mechanism-factory=%2$s,security-domain=%1$s,"
                        + "mechanism-configurations=[{mechanism-name=BASIC,mechanism-realm-configurations=[{realm-name=\"%1$s\"}]}])",
                    CUSTOM_REALM_RELATED_CONFIGURATION_NAME, PREDEFINED_HTTP_SERVER_MECHANISM_FACTORY));
                cli.sendLine(
                    String.format("/subsystem=undertow/application-security-domain=%s:add(http-authentication-factory=%s)",
                        DEPLOYMENT, CUSTOM_REALM_RELATED_CONFIGURATION_NAME));
            }
            ServerReload.reloadIfRequired(mc.getControllerClient());
        }

        @Override
        public void tearDown(ManagementClient mc, String string) throws Exception {
            try (CLIWrapper cli = new CLIWrapper(true)) {
                cli.sendLine(String.format("/subsystem=undertow/application-security-domain=%s:remove()", DEPLOYMENT));
                cli.sendLine(String.format("/subsystem=elytron/http-authentication-factory=%s:remove()",
                    CUSTOM_REALM_RELATED_CONFIGURATION_NAME));
                cli.sendLine(
                    String.format("/subsystem=elytron/security-domain=%s:remove()", CUSTOM_REALM_RELATED_CONFIGURATION_NAME));
                cli.sendLine(String.format("/subsystem=elytron/custom-realm=%s:remove()", CUSTOM_REALM_NAME));
            }
            ServerReload.reloadIfRequired(mc.getControllerClient());
        }
    }
}

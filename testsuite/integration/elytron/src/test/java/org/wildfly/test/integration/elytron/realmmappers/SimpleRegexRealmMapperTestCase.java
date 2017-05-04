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
package org.wildfly.test.integration.elytron.realmmappers;

import java.net.URL;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.management.util.CLIWrapper;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.as.test.shared.ServerReload;
import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.wildfly.test.integration.elytron.realmmappers.AbstractRealmMapperTest.DEPLOYMENT;
import static org.wildfly.test.integration.elytron.realmmappers.RealmMapperServerSetupTask.CORRECT_PASSWORD;
import static org.wildfly.test.integration.elytron.realmmappers.RealmMapperServerSetupTask.REALM3;
import static org.wildfly.test.integration.elytron.realmmappers.RealmMapperServerSetupTask.USER_IN_DEFAULT_REALM;
import static org.wildfly.test.integration.elytron.realmmappers.RealmMapperServerSetupTask.USER_IN_DEFAULT_REALM_MAPPED2;
import static org.wildfly.test.integration.elytron.realmmappers.RealmMapperServerSetupTask.USER_IN_REALM1;
import static org.wildfly.test.integration.elytron.realmmappers.RealmMapperServerSetupTask.USER_IN_REALM1_WITH_REALM;
import static org.wildfly.test.integration.elytron.realmmappers.RealmMapperServerSetupTask.USER_IN_REALM1_WITH_REALM_AND_SUFFIX;
import static org.wildfly.test.integration.elytron.realmmappers.RealmMapperServerSetupTask.USER_IN_REALM3;

/**
 * Test case for 'simple-regex-realm-mapper' Elytron subsystem resource.
 *
 * @author olukas
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup({RealmMapperServerSetupTask.class, SimpleRegexRealmMapperTestCase.SetupTask.class})
public class SimpleRegexRealmMapperTestCase extends AbstractRealmMapperTest {

    private static final String COMMON_REALM_MAPPER = "commonRealmMapper";
    private static final String TWO_CAPTURE_GROUPS_REALM_MAPPER = "twoCaptureGroupsRealmMapper";
    private static final String DELEGATE_REALM_MAPPER = "delegateRealmMapper";

    private static final String DELEGATED_REALM_MAPPER = "delagetedConstantRealmMapper";

    /**
     * Test whether obtained realm is used when user matches pattern and obtained realm exists.
     *
     * @param webAppURL
     * @throws Exception
     */
    @Test
    @OperateOnDeployment(DEPLOYMENT)
    public void testUserMatch(@ArquillianResource URL webAppURL) throws Exception {
        setupRealmMapper(COMMON_REALM_MAPPER);
        try {
            assertEquals("Response body is not correct.", USER_IN_REALM1_WITH_REALM,
                    Utils.makeCallWithBasicAuthn(principalServlet(webAppURL), USER_IN_REALM1_WITH_REALM, CORRECT_PASSWORD, SC_OK));
        } finally {
            undefineRealmMapper();
        }
    }

    /**
     * Test whether default realm is used when user matches pattern and obtained realm does not exist.
     *
     * @param webAppURL
     * @throws Exception
     */
    @Test
    @OperateOnDeployment(DEPLOYMENT)
    public void testUserMatchRealmDoesNotExist(@ArquillianResource URL webAppURL) throws Exception {
        setupRealmMapper(COMMON_REALM_MAPPER);
        try {
            assertEquals("Response body is not correct.", USER_IN_DEFAULT_REALM_MAPPED2,
                    Utils.makeCallWithBasicAuthn(principalServlet(webAppURL), USER_IN_DEFAULT_REALM_MAPPED2, CORRECT_PASSWORD, SC_OK));
        } finally {
            undefineRealmMapper();
        }
    }

    /**
     * Test whether default realm is used when user does not match pattern.
     *
     * @param webAppURL
     * @throws Exception
     */
    @Test
    @OperateOnDeployment(DEPLOYMENT)
    public void testUserDoNotMatch(@ArquillianResource URL webAppURL) throws Exception {
        setupRealmMapper(COMMON_REALM_MAPPER);
        try {
            assertEquals("Response body is not correct.", USER_IN_DEFAULT_REALM,
                    Utils.makeCallWithBasicAuthn(principalServlet(webAppURL), USER_IN_DEFAULT_REALM, CORRECT_PASSWORD, SC_OK));
        } finally {
            undefineRealmMapper();
        }
    }

    /**
     * Test whether obtained realm is used even if delegate-realm-mapper is set when user matches pattern and obtained realm
     * exists.
     *
     * @param webAppURL
     * @throws Exception
     */
    @Test
    @OperateOnDeployment(DEPLOYMENT)
    public void testDoNotDelegateWhenMatch(@ArquillianResource URL webAppURL) throws Exception {
        setupRealmMapper(DELEGATE_REALM_MAPPER);
        try {
            assertEquals("Response body is not correct.", USER_IN_REALM1_WITH_REALM,
                    Utils.makeCallWithBasicAuthn(principalServlet(webAppURL), USER_IN_REALM1_WITH_REALM, CORRECT_PASSWORD, SC_OK));
            Utils.makeCallWithBasicAuthn(principalServlet(webAppURL), USER_IN_REALM1, CORRECT_PASSWORD, SC_UNAUTHORIZED);
        } finally {
            undefineRealmMapper();
        }
    }

    /**
     * Test whether mapper from delegate-realm-mapper is used when user does not match pattern.
     *
     * @param webAppURL
     * @throws Exception
     */
    @Test
    @OperateOnDeployment(DEPLOYMENT)
    public void testDelegateWhenDoNotMatch(@ArquillianResource URL webAppURL) throws Exception {
        setupRealmMapper(DELEGATE_REALM_MAPPER);
        try {
            assertEquals("Response body is not correct.", USER_IN_REALM3,
                    Utils.makeCallWithBasicAuthn(principalServlet(webAppURL), USER_IN_REALM3, CORRECT_PASSWORD, SC_OK));
        } finally {
            undefineRealmMapper();
        }
    }

    /**
     * Test whether realm is parsed from first capture group if more capture groups are used.
     *
     * @param webAppURL
     * @throws Exception
     */
    @Test
    @OperateOnDeployment(DEPLOYMENT)
    public void testMoreCaptureGroups(@ArquillianResource URL webAppURL) throws Exception {
        setupRealmMapper(TWO_CAPTURE_GROUPS_REALM_MAPPER);
        try {
            assertEquals("Response body is not correct.", USER_IN_REALM1_WITH_REALM_AND_SUFFIX,
                    Utils.makeCallWithBasicAuthn(principalServlet(webAppURL), USER_IN_REALM1_WITH_REALM_AND_SUFFIX, CORRECT_PASSWORD, SC_OK));
            Utils.makeCallWithBasicAuthn(principalServlet(webAppURL), USER_IN_REALM1_WITH_REALM, CORRECT_PASSWORD, SC_UNAUTHORIZED);
        } finally {
            undefineRealmMapper();
        }
    }

    static class SetupTask implements ServerSetupTask {

        @Override
        public void setup(ManagementClient managementClient, String containerId) throws Exception {
            try (CLIWrapper cli = new CLIWrapper(true)) {
                cli.sendLine(String.format("/subsystem=elytron/constant-realm-mapper=%s:add(realm-name=%s)",
                        DELEGATED_REALM_MAPPER, REALM3));
                cli.sendLine(String.format("/subsystem=elytron/simple-regex-realm-mapper=%s:add(pattern=\".*@(.*)\")",
                        COMMON_REALM_MAPPER));
                cli.sendLine(String.format("/subsystem=elytron/simple-regex-realm-mapper=%s:add(pattern=\".*@(.*)&(.*)\")",
                        TWO_CAPTURE_GROUPS_REALM_MAPPER));
                cli.sendLine(String.format("/subsystem=elytron/simple-regex-realm-mapper=%s:add(delegate-realm-mapper=%s,pattern=\".*@(.*)\")",
                        DELEGATE_REALM_MAPPER, DELEGATED_REALM_MAPPER));
            }
            ServerReload.reloadIfRequired(managementClient.getControllerClient());
        }

        @Override
        public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
            try (CLIWrapper cli = new CLIWrapper(true)) {
                cli.sendLine(String.format("/subsystem=elytron/simple-regex-realm-mapper=%s:remove()", DELEGATE_REALM_MAPPER));
                cli.sendLine(String.format("/subsystem=elytron/simple-regex-realm-mapper=%s:remove()",
                        TWO_CAPTURE_GROUPS_REALM_MAPPER));
                cli.sendLine(String.format("/subsystem=elytron/simple-regex-realm-mapper=%s:remove()", COMMON_REALM_MAPPER));
                cli.sendLine(String.format("/subsystem=elytron/constant-realm-mapper=%s:remove()", DELEGATED_REALM_MAPPER));
            }
            ServerReload.reloadIfRequired(managementClient.getControllerClient());
        }

    }
}

/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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
package org.wildfly.test.integration.elytron.rolemappers;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.integration.management.util.CLIWrapper;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.wildfly.test.integration.elytron.rolemappers.RoleMapperSetupUtils.PROPERTIES_REALM_NAME;
import static org.wildfly.test.integration.elytron.rolemappers.RoleMapperSetupUtils.addSecurityDomainWithRoleMapper;
import org.wildfly.test.security.common.AbstractElytronSetupTask;
import org.wildfly.test.security.common.elytron.ConfigurableElement;
import org.wildfly.test.security.common.elytron.PropertiesRealm;

/**
 * Test case for Elytron Constant Role Mapper.
 *
 * Given: Authentication to secured application is backed by Elytron Properties Realm. <br>
 *
 * @author olukas
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup({ConstantRoleMapperTestCase.ServerSetup.class})
public class ConstantRoleMapperTestCase extends AbstractRoleMapperTest {

    private static final String ONE_ROLE_MAPPER = "one-role-contant-role-mapper";
    private static final String TWO_ROLES_MAPPER = "two-roles-contant-role-mapper";

    private static final String USER_WITHOUT_ROLES = "userWithoutRoles";
    private static final String USER_WITH_ROLE1 = "userWithRole1";
    private static final String USER_WITH_ROLE2 = "userWithRole2";
    private static final String USER_WITH_TWO_ROLES = "userWithTwoRoles";
    private static final String USER_WITH_THREE_ROLES = "userWithThreeRoles";

    private static final String PASSWORD = "password";

    @Override
    protected String[] allTestedRoles() {
        return new String[]{ROLE1, ROLE2, ROLE3};
    }

    @Deployment(name = ONE_ROLE_MAPPER)
    public static WebArchive deploymentOneRole() {
        return createDeploymentForPrintingRoles(ONE_ROLE_MAPPER);
    }

    @Deployment(name = TWO_ROLES_MAPPER)
    public static WebArchive deploymentTwoRoles() {
        return createDeploymentForPrintingRoles(TWO_ROLES_MAPPER);
    }

    /**
     * Given: Constant Role Mapper which maps Role1 is added to configuration <br>
     * and roles property file maps no role for the user. <br>
     * When the user is authenticated <br>
     * then just role Role1 should be assigned to user.
     */
    @Test
    @OperateOnDeployment(ONE_ROLE_MAPPER)
    public void testOneRoleMapper_userWithoutRoles(@ArquillianResource URL webAppURL) throws Exception {
        testAssignedRoles(webAppURL, USER_WITHOUT_ROLES, PASSWORD, ROLE1);
    }

    /**
     * Given: Constant Role Mapper which maps Role1 is added to configuration.<br>
     * and roles property file maps role Role1 for the user. <br>
     * When the user is authenticated <br>
     * then just role Role1 should be assigned to the user.
     */
    @Test
    @OperateOnDeployment(ONE_ROLE_MAPPER)
    public void testOneRoleMapper_userWithSameRole(@ArquillianResource URL webAppURL) throws Exception {
        testAssignedRoles(webAppURL, USER_WITH_ROLE1, PASSWORD, ROLE1);
    }

    /**
     * Given: Constant Role Mapper which maps Role1 is added to configuration.<br>
     * and roles property file maps role Role2 for the user. <br>
     * When the user is authenticated <br>
     * then just role Role1 should be assigned to the user.
     */
    @Test
    @OperateOnDeployment(ONE_ROLE_MAPPER)
    public void testOneRoleMapper_userWithDifferentRole(@ArquillianResource URL webAppURL) throws Exception {
        testAssignedRoles(webAppURL, USER_WITH_ROLE2, PASSWORD, ROLE1);
    }

    /**
     * Given: Constant Role Mapper which maps Role1 is added to configuration.<br>
     * and roles property file maps roles Role1 and Role2 for the user. <br>
     * When the user is authenticated <br>
     * then just role Role1 should be assigned to the user.
     */
    @Test
    @OperateOnDeployment(ONE_ROLE_MAPPER)
    public void testOneRoleMapper_userWithMoreRoles(@ArquillianResource URL webAppURL) throws Exception {
        testAssignedRoles(webAppURL, USER_WITH_TWO_ROLES, PASSWORD, ROLE1);
    }

    /**
     * Given: Constant Role Mapper which maps Role1 and Role2 is added to configuration.<br>
     * and roles property file maps role Role1 for the user. <br>
     * When the user is authenticated <br>
     * then just roles Role1 and Role2 should be assigned to the user.
     */
    @Test
    @OperateOnDeployment(TWO_ROLES_MAPPER)
    public void testTwoRolesMapper_userWithLessRoles(@ArquillianResource URL webAppURL) throws Exception {
        testAssignedRoles(webAppURL, USER_WITH_ROLE1, PASSWORD, ROLE1, ROLE2);
    }

    /**
     * Given: Constant Role Mapper which maps Role1 and Role2 is added to configuration.<br>
     * and roles property file maps roles Role1 and Role3 for the user. <br>
     * When the user is authenticated <br>
     * then just roles Role1 and Role2 should be assigned to the user.
     */
    @Test
    @OperateOnDeployment(TWO_ROLES_MAPPER)
    public void testTwoRolesMapper_userWithOneSameAndOneDifferentRole(@ArquillianResource URL webAppURL) throws Exception {
        testAssignedRoles(webAppURL, USER_WITH_TWO_ROLES, PASSWORD, ROLE1, ROLE2);
    }

    /**
     * Given: Constant Role Mapper which maps Role1 and Role2 is added to configuration.<br>
     * and roles property file maps roles Role1, Role2 and Role3 for the user. <br>
     * When the user is authenticated <br>
     * then just roles Role1 and Role2 should be assigned to the user.
     */
    @Test
    @OperateOnDeployment(TWO_ROLES_MAPPER)
    public void testTwoRolesMapper_userWithMoreRoles(@ArquillianResource URL webAppURL) throws Exception {
        testAssignedRoles(webAppURL, USER_WITH_THREE_ROLES, PASSWORD, ROLE1, ROLE2);
    }

    public static class ServerSetup extends AbstractElytronSetupTask {

        @Override
        protected ConfigurableElement[] getConfigurableElements() {
            List<ConfigurableElement> elements = new ArrayList<>();

            elements.add(new ConstantRoleMappers(
                    String.format("%s:add(roles=[%s])", ONE_ROLE_MAPPER, ROLE1),
                    String.format("%s:add(roles=[%s,%s])", TWO_ROLES_MAPPER, ROLE1, ROLE2)
            ));

            elements.add(PropertiesRealm.builder().withName(PROPERTIES_REALM_NAME)
                    .withUser(USER_WITHOUT_ROLES, PASSWORD)
                    .withUser(USER_WITH_ROLE1, PASSWORD, ROLE1)
                    .withUser(USER_WITH_ROLE2, PASSWORD, ROLE2)
                    .withUser(USER_WITH_TWO_ROLES, PASSWORD, ROLE1, ROLE3)
                    .withUser(USER_WITH_THREE_ROLES, PASSWORD, ROLE1, ROLE2, ROLE3)
                    .build());
            addSecurityDomainWithRoleMapper(elements, ONE_ROLE_MAPPER);
            addSecurityDomainWithRoleMapper(elements, TWO_ROLES_MAPPER);

            return elements.toArray(new ConfigurableElement[elements.size()]);
        }

        public static class ConstantRoleMappers implements ConfigurableElement {

            private final String[] dynamicConstants;

            public ConstantRoleMappers(String... dynamicConstants) {
                this.dynamicConstants = dynamicConstants;
            }

            @Override
            public void create(CLIWrapper cli) throws Exception {
                for (String con : dynamicConstants) {
                    cli.sendLine("/subsystem=elytron/constant-role-mapper=" + con);
                }
            }

            @Override
            public void remove(CLIWrapper cli) throws Exception {
                for (String con : dynamicConstants) {
                    int opIdx = con.indexOf(':');
                    String newCon = con.substring(0, opIdx + 1) + "remove()";
                    cli.sendLine("/subsystem=elytron/constant-role-mapper=" + newCon);
                }
            }

            @Override
            public String getName() {
                return "constant-role-mapper";
            }
        }

    }
}

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
import static org.wildfly.test.integration.elytron.rolemappers.AbstractRoleMapperTest.ROLE1;
import static org.wildfly.test.integration.elytron.rolemappers.AbstractRoleMapperTest.ROLE2;
import static org.wildfly.test.integration.elytron.rolemappers.AbstractRoleMapperTest.createDeploymentForPrintingRoles;
import static org.wildfly.test.integration.elytron.rolemappers.RoleMapperSetupUtils.PROPERTIES_REALM_NAME;
import static org.wildfly.test.integration.elytron.rolemappers.RoleMapperSetupUtils.addSecurityDomainWithRoleMapper;
import org.wildfly.test.security.common.AbstractElytronSetupTask;
import org.wildfly.test.security.common.elytron.ConfigurableElement;
import org.wildfly.test.security.common.elytron.PropertiesRealm;

/**
 * Test case for Elytron Add Prefix Role Mapper.
 *
 * Given: Authentication to secured application is backed by Elytron Properties Realm <br>
 * and Properties Realm uses the Add Prefix Role Mapper for mapping roles <br>
 * and the Add Prefix Role Mapper with attribute prefix='Pre' is added to configuration.
 *
 * @author olukas
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup({AddPrefixRoleMapperTestCase.ServerSetup.class})
public class AddPrefixRoleMapperTestCase extends AbstractRoleMapperTest {

    private static final String ADD_PREFIX_MAPPER = "simple-add-prefix-role-mapper";

    private static final String ROLE_PREFIX = "Pre";

    private static final String USER_WITHOUT_ROLES = "userWithoutRoles";
    private static final String USER_WITH_ROLE1 = "userWithRole1";
    private static final String USER_WITH_TWO_ROLES = "userWithTwoRoles";

    private static final String PASSWORD = "password";

    private static final String ROLE1_WITH_PREFIX = ROLE_PREFIX + ROLE1;
    private static final String ROLE2_WITH_PREFIX = ROLE_PREFIX + ROLE2;

    @Override
    protected String[] allTestedRoles() {
        return new String[]{ROLE1, ROLE2, ROLE1_WITH_PREFIX, ROLE2_WITH_PREFIX};
    }

    @Deployment(name = ADD_PREFIX_MAPPER)
    public static WebArchive deploymentAddPrefix() {
        return createDeploymentForPrintingRoles(ADD_PREFIX_MAPPER);
    }

    /**
     * Given: Roles property file maps no role for the user. <br>
     * When the user is authenticated <br>
     * then no role should be assigned to the user. <br>
     */
    @Test
    @OperateOnDeployment(ADD_PREFIX_MAPPER)
    public void testUserWithoutRoles(@ArquillianResource URL webAppURL) throws Exception {
        assertNoRoleAssigned(webAppURL, USER_WITHOUT_ROLES, PASSWORD);
    }

    /**
     * Given: Roles property file maps role Role1 for the user. <br>
     * When the user is authenticated <br>
     * then just role PreRole1 should be assigned to the user.
     */
    @Test
    @OperateOnDeployment(ADD_PREFIX_MAPPER)
    public void testUserWithOneRole(@ArquillianResource URL webAppURL) throws Exception {
        testAssignedRoles(webAppURL, USER_WITH_ROLE1, PASSWORD, ROLE1_WITH_PREFIX);
    }

    /**
     * Given: Roles property file maps roles Role1 and Role2 for the user. <br>
     * When the user is authenticated <br>
     * then just roles PreRole1 and PreRole2 should be assigned to the user.
     */
    @Test
    @OperateOnDeployment(ADD_PREFIX_MAPPER)
    public void testUserWithTwoRoles(@ArquillianResource URL webAppURL) throws Exception {
        testAssignedRoles(webAppURL, USER_WITH_TWO_ROLES, PASSWORD, ROLE1_WITH_PREFIX, ROLE2_WITH_PREFIX);
    }

    public static class ServerSetup extends AbstractElytronSetupTask {

        @Override
        protected ConfigurableElement[] getConfigurableElements() {
            List<ConfigurableElement> elements = new ArrayList<>();

            elements.add(new AddPrefixRoleMappers(
                    String.format("%s:add(prefix=%s)", ADD_PREFIX_MAPPER, ROLE_PREFIX)
            ));

            elements.add(PropertiesRealm.builder().withName(PROPERTIES_REALM_NAME)
                    .withUser(USER_WITHOUT_ROLES, PASSWORD)
                    .withUser(USER_WITH_ROLE1, PASSWORD, ROLE1)
                    .withUser(USER_WITH_TWO_ROLES, PASSWORD, ROLE1, ROLE2)
                    .build());
            addSecurityDomainWithRoleMapper(elements, ADD_PREFIX_MAPPER);

            return elements.toArray(new ConfigurableElement[elements.size()]);
        }

        public static class AddPrefixRoleMappers implements ConfigurableElement {

            private final String[] dynamicPrefixes;

            public AddPrefixRoleMappers(String... dynamicPrefixes) {
                this.dynamicPrefixes = dynamicPrefixes;
            }

            @Override
            public void create(CLIWrapper cli) throws Exception {
                for (String pfx : dynamicPrefixes) {
                    cli.sendLine("/subsystem=elytron/add-prefix-role-mapper=" + pfx);
                }
            }

            @Override
            public void remove(CLIWrapper cli) throws Exception {
                for (String pfx : dynamicPrefixes) {
                    int opIdx = pfx.indexOf(':');
                    String newPfx = pfx.substring(0, opIdx + 1) + "remove()";
                    cli.sendLine("/subsystem=elytron/add-prefix-role-mapper=" + newPfx);
                }
            }

            @Override
            public String getName() {
                return "add-prefix-role-mapper";
            }
        }

    }
}

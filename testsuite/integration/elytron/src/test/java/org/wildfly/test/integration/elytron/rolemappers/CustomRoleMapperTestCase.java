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

import static org.wildfly.test.integration.elytron.rolemappers.RoleMapperSetupUtils.PROPERTIES_REALM_NAME;
import static org.wildfly.test.integration.elytron.rolemappers.RoleMapperSetupUtils.addSecurityDomainWithRoleMapper;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.test.integration.management.util.CLIWrapper;
import org.jboss.as.test.shared.util.JarUtils;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.test.security.common.AbstractElytronSetupTask;
import org.wildfly.test.security.common.elytron.ConfigurableElement;
import org.wildfly.test.security.common.elytron.PropertiesRealm;

/**
 * Test case for Elytron Custom Role Mapper.
 *
 * Given: Authentication to secured application is backed by Elytron Properties Realm <br>
 * and Properties Realm uses the Custom Role Mapper for mapping roles <br>
 * and the Custom Role Mapper with added to configuration.
 *
 * @author olukas
 * @author Hynek Švábek <hsvabek@redhat.com>
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup({CustomRoleMapperTestCase.ServerSetup.class})
public class CustomRoleMapperTestCase extends AbstractRoleMapperTest {

    private static final String CUSTOM_ROLE_MAPPER_MODULE_NAME = "org.jboss.customrolemapperimpl";
    private static final String CUSTOM_ROLE_MAPPER = "custom-role-mapper";

    private static final String USER_WITHOUT_ROLES = "userWithoutRoles";
    private static final String USER_WITH_ROLE1 = "userWithRole1";
    private static final String USER_WITH_ROLE2 = "userWithRole2";
    private static final String USER_WITH_TWO_ROLES = "userWithTwoRoles";

    private static final String PASSWORD = "password";

    @Override
    protected String[] allTestedRoles() {
        return new String[] { ROLE1, ROLE2, ROLE2 };
    }

    @Deployment(name = CUSTOM_ROLE_MAPPER)
    public static WebArchive deploymentAddPrefix() {
        return createDeploymentForPrintingRoles(CUSTOM_ROLE_MAPPER);
    }

    /**
     * Given: Constant Role Mapper which maps Role1 is added to configuration <br>
     * and roles property file maps no role for the user. <br>
     * When the user is authenticated <br>
     * then just role Role1 should be assigned to user.
     */
    @Test
    @OperateOnDeployment(CUSTOM_ROLE_MAPPER)
    public void testOneRoleMapper_userWithoutRoles(@ArquillianResource URL webAppURL) throws Exception {
        testAssignedRoles(webAppURL, USER_WITHOUT_ROLES, PASSWORD, ROLE1);
    }

    /**
     * Given: Custom Role Mapper which maps Role1 is added to configuration.<br>
     * and roles property file maps role Role1 for the user. <br>
     * When the user is authenticated <br>
     * then just role Role1 should be assigned to the user.
     */
    @Test
    @OperateOnDeployment(CUSTOM_ROLE_MAPPER)
    public void testOneRoleMapper_userWithSameRole(@ArquillianResource URL webAppURL) throws Exception {
        testAssignedRoles(webAppURL, USER_WITH_ROLE1, PASSWORD, ROLE1);
    }

    /**
     * Given: Custom Role Mapper which maps Role1 is added to configuration.<br>
     * and roles property file maps role Role2 for the user. <br>
     * When the user is authenticated <br>
     * then just role Role1 should be assigned to the user.
     */
    @Test
    @OperateOnDeployment(CUSTOM_ROLE_MAPPER)
    public void testOneRoleMapper_userWithDifferentRole(@ArquillianResource URL webAppURL) throws Exception {
        testAssignedRoles(webAppURL, USER_WITH_ROLE2, PASSWORD, ROLE1);
    }

    /**
     * Given: Custom Role Mapper which maps Role1 is added to configuration.<br>
     * and roles property file maps roles Role1 and Role2 for the user. <br>
     * When the user is authenticated <br>
     * then just role Role1 should be assigned to the user.
     */
    @Test
    @OperateOnDeployment(CUSTOM_ROLE_MAPPER)
    public void testOneRoleMapper_userWithMoreRoles(@ArquillianResource URL webAppURL) throws Exception {
        testAssignedRoles(webAppURL, USER_WITH_TWO_ROLES, PASSWORD, ROLE1);
    }

    public static class ServerSetup extends AbstractElytronSetupTask {

        @Override
        protected void setup(ModelControllerClient modelControllerClient) throws Exception {
            File moduleJar = JarUtils.createJarFile("testJar", CustomRoleMapperImpl.class);
            try (CLIWrapper cli = new CLIWrapper(true)) {
                cli.sendLine("module add --name=" + CUSTOM_ROLE_MAPPER_MODULE_NAME
                    + " --slot=main --dependencies=org.wildfly.security.elytron,org.wildfly.extension.elytron --resources="
                    + moduleJar.getAbsolutePath());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            super.setup(modelControllerClient);
        }

        @Override
        protected ConfigurableElement[] getConfigurableElements() {
            List<ConfigurableElement> elements = new ArrayList<>();

            elements.add(new CustomRoleMappers(
                String.format("%s:add(class-name=%s, module=%s, configuration={returnRole=%s})", CUSTOM_ROLE_MAPPER,
                    CustomRoleMapperImpl.class.getName(),
                    CUSTOM_ROLE_MAPPER_MODULE_NAME, ROLE1)
            ));

            elements.add(PropertiesRealm.builder().withName(PROPERTIES_REALM_NAME)
                    .withUser(USER_WITHOUT_ROLES, PASSWORD)
                    .withUser(USER_WITH_ROLE1, PASSWORD, ROLE1)
                    .withUser(USER_WITH_ROLE2, PASSWORD, ROLE2)
                    .withUser(USER_WITH_TWO_ROLES, PASSWORD, ROLE1, ROLE2)
                    .build());
            addSecurityDomainWithRoleMapper(elements, CUSTOM_ROLE_MAPPER);

            return elements.toArray(new ConfigurableElement[elements.size()]);
        }

        public static class CustomRoleMappers implements ConfigurableElement {

            private final String[] customRoleMappers;

            public CustomRoleMappers(String... customRolesMappers) {
                this.customRoleMappers = customRolesMappers;
            }

            @Override
            public void create(CLIWrapper cli) throws Exception {
                for (String custom : customRoleMappers) {
                    cli.sendLine("/subsystem=elytron/custom-role-mapper=" + custom);
                }
            }

            @Override
            public void remove(CLIWrapper cli) throws Exception {
                for (String custom : customRoleMappers) {
                    int opIdx = custom.indexOf(':');
                    String newPfx = custom.substring(0, opIdx + 1) + "remove()";
                    cli.sendLine("/subsystem=elytron/custom-role-mapper=" + newPfx);
                }
            }

            @Override
            public String getName() {
                return "custom-role-mapper";
            }
        }

    }
}

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
 * Test case for Elytron Aggregate Role Mapper.
 *
 * Given: Authentication to secured application is backed by Elytron Properties Realm <br>
 * and Properties Realm uses the Aggregate Role Mapper for mapping roles.
 *
 * @author olukas
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup({AggregateRoleMapperTestCase.ServerSetup.class})
public class AggregateRoleMapperTestCase extends AbstractRoleMapperTest {

    private static final String AGGREGATE_MAPPER = "simple-aggregate-mapper";

    private static final String USER = "user";
    private static final String PASSWORD = "password";

    private static final String ROLE_PREFIX1 = "1";
    private static final String ROLE_PREFIX2 = "2";

    private static final String ROLE1_WITH_CORRECT_PREFIX = ROLE_PREFIX2 + ROLE_PREFIX1 + ROLE1;
    private static final String ROLE1_WITH_WRONG_PREFIX = ROLE_PREFIX1 + ROLE_PREFIX2 + ROLE1;
    private static final String ROLE1_WITH_HALF_PREFIX = ROLE_PREFIX1 + ROLE1;
    private static final String ROLE2_WITH_CORRECT_PREFIX = ROLE_PREFIX2 + ROLE_PREFIX1 + ROLE2;
    private static final String ROLE2_WITH_WRONG_PREFIX = ROLE_PREFIX1 + ROLE_PREFIX2 + ROLE2;
    private static final String ROLE2_WITH_HALF_PREFIX = ROLE_PREFIX1 + ROLE2;

    @Override
    protected String[] allTestedRoles() {
        return new String[]{ROLE1, ROLE1_WITH_CORRECT_PREFIX, ROLE1_WITH_WRONG_PREFIX,
            ROLE1_WITH_HALF_PREFIX, ROLE2_WITH_CORRECT_PREFIX, ROLE2_WITH_WRONG_PREFIX, ROLE2_WITH_HALF_PREFIX};
    }

    @Deployment(name = AGGREGATE_MAPPER)
    public static WebArchive deploymentAggregate() {
        return createDeploymentForPrintingRoles(AGGREGATE_MAPPER);
    }

    /**
     * Given: Add Prefix Role Mapper (1) with attribute prefix='1' is added to configuration <br>
     * and Add Prefix Role Mapper (2) with attribute prefix='2' is added to configuration <br>
     * and Aggregate Role Mapper uses mentioned Add Prefix Role Mappers in order 1, 2 <br>
     * and roles property file maps roles Role1 and Role2 for the user. <br>
     * When the user is authenticated <br>
     * then just roles 21Role1 and 21Role2 should be assigned to the user (which means that both role mappers have been called
     * and their order has been correct).
     */
    @Test
    @OperateOnDeployment(AGGREGATE_MAPPER)
    public void testTwoMappers(@ArquillianResource URL webAppURL) throws Exception {
        testAssignedRoles(webAppURL, USER, PASSWORD, ROLE1_WITH_CORRECT_PREFIX, ROLE2_WITH_CORRECT_PREFIX);
    }

    public static class ServerSetup extends AbstractElytronSetupTask {

        private static final String ADD_PREFIX_ROLE_MAPPER1 = "add-prefix-role-mapper1";
        private static final String ADD_PREFIX_ROLE_MAPPER2 = "add-prefix-role-mapper2";

        @Override
        protected ConfigurableElement[] getConfigurableElements() {
            List<ConfigurableElement> elements = new ArrayList<>();

            elements.add(new AddPrefixRoleMapperTestCase.ServerSetup.AddPrefixRoleMappers(
                    String.format("%s:add(prefix=%s)", ADD_PREFIX_ROLE_MAPPER1, ROLE_PREFIX1),
                    String.format("%s:add(prefix=%s)", ADD_PREFIX_ROLE_MAPPER2, ROLE_PREFIX2)
            ));
            elements.add(new AggregateRoleMappers(
                    String.format("%s:add(role-mappers=[%s,%s])", AGGREGATE_MAPPER, ADD_PREFIX_ROLE_MAPPER1, ADD_PREFIX_ROLE_MAPPER2)
            ));

            elements.add(PropertiesRealm.builder().withName(PROPERTIES_REALM_NAME)
                    .withUser(USER, PASSWORD, ROLE1, ROLE2)
                    .build());
            addSecurityDomainWithRoleMapper(elements, AGGREGATE_MAPPER);

            return elements.toArray(new ConfigurableElement[elements.size()]);
        }

        public static class AggregateRoleMappers implements ConfigurableElement {

            private final String[] dynamicAggregates;

            public AggregateRoleMappers(String... dynamicAggregates) {
                this.dynamicAggregates = dynamicAggregates;
            }

            @Override
            public void create(CLIWrapper cli) throws Exception {
                for (String agg : dynamicAggregates) {
                    cli.sendLine("/subsystem=elytron/aggregate-role-mapper=" + agg);
                }
            }

            @Override
            public void remove(CLIWrapper cli) throws Exception {
                for (String agg : dynamicAggregates) {
                    int opIdx = agg.indexOf(':');
                    String newAgg = agg.substring(0, opIdx + 1) + "remove()";
                    cli.sendLine("/subsystem=elytron/aggregate-role-mapper=" + newAgg);
                }
            }

            @Override
            public String getName() {
                return "aggregate-role-mapper";
            }
        }

    }
}

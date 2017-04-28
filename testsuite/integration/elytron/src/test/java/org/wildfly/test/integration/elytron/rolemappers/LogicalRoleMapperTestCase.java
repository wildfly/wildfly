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
import static org.wildfly.test.integration.elytron.rolemappers.AbstractRoleMapperTest.ROLE3;
import static org.wildfly.test.integration.elytron.rolemappers.RoleMapperSetupUtils.PROPERTIES_REALM_NAME;
import static org.wildfly.test.integration.elytron.rolemappers.RoleMapperSetupUtils.addSecurityDomainWithRoleMapper;
import org.wildfly.test.security.common.AbstractElytronSetupTask;
import org.wildfly.test.security.common.elytron.ConfigurableElement;
import org.wildfly.test.security.common.elytron.PropertiesRealm;

/**
 * Test case for Elytron Logical Role Mapper.
 *
 * Given: Authentication to secured application is backed by Elytron Properties Realm.
 *
 * @author olukas
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup({LogicalRoleMapperTestCase.ServerSetup.class})
public class LogicalRoleMapperTestCase extends AbstractRoleMapperTest {

    private static final String AND_SOME_SAME_ROLES = "and-some-same-roles";
    private static final String AND_EMPTY_ROLE = "and-empty-role";
    private static final String AND_DIFFERENT_ROLES = "and-different-roles";
    private static final String AND_LEFT_IS_MISSING_ROLES = "and-left-is-missing";
    private static final String AND_RIGHT_IS_MISSING_ROLES = "and-right-is-missing";
    private static final String OR_SOME_SAME_ROLES = "or-some-same-roles";
    private static final String OR_EMPTY_ROLE = "or-empty-role";
    private static final String OR_DIFFERENT_ROLES = "or-different-roles";
    private static final String OR_LEFT_IS_MISSING_ROLES = "or-left-is-missing";
    private static final String OR_RIGHT_IS_MISSING_ROLES = "or-right-is-missing";
    private static final String MINUS_SOME_SAME_ROLES = "minus-some-same-roles";
    private static final String MINUS_EMPTY_ROLE_LEFT_ROLES = "minus-empty-role-left";
    private static final String MINUS_EMPTY_ROLE_RIGHT = "minus-empty-role-right";
    private static final String MINUS_DIFFERENT_ROLES = "minus-different-roles";
    private static final String MINUS_LEFT_IS_MISSING_ROLES = "minus-left-is-missing";
    private static final String MINUS_RIGHT_IS_MISSING_ROLES = "minus-right-is-missing";
    private static final String XOR_SOME_SAME_ROLES = "xor-some-same-roles";
    private static final String XOR_EMPTY_ROLE = "xor-empty-role";
    private static final String XOR_DIFFERENT_ROLES = "xor-different-roles";
    private static final String XOR_LEFT_IS_MISSING_ROLES = "xor-left-is-missing";
    private static final String XOR_RIGHT_IS_MISSING_ROLES = "xor-right-is-missing";

    private static final String USER = "user";
    private static final String USER_WITH_ROLE_2_3_4 = "user-with-role-2-3-4";
    private static final String PASSWORD = "password";

    @Override
    protected String[] allTestedRoles() {
        return new String[]{ROLE1, ROLE2, ROLE3, ROLE4};
    }

    @Deployment(name = AND_SOME_SAME_ROLES)
    public static WebArchive deploymentAndSomeSameRoles() {
        return createDeploymentForPrintingRoles(AND_SOME_SAME_ROLES);
    }

    @Deployment(name = AND_EMPTY_ROLE)
    public static WebArchive deploymentAndEmptyRole() {
        return createDeploymentForPrintingRoles(AND_EMPTY_ROLE);
    }

    @Deployment(name = AND_DIFFERENT_ROLES)
    public static WebArchive deploymentAndDifferentRoles() {
        return createDeploymentForPrintingRoles(AND_DIFFERENT_ROLES);
    }

    @Deployment(name = AND_LEFT_IS_MISSING_ROLES)
    public static WebArchive deploymentAndLeftIsMissing() {
        return createDeploymentForPrintingRoles(AND_LEFT_IS_MISSING_ROLES);
    }

    @Deployment(name = AND_RIGHT_IS_MISSING_ROLES)
    public static WebArchive deploymentAndRightIsMissing() {
        return createDeploymentForPrintingRoles(AND_RIGHT_IS_MISSING_ROLES);
    }

    @Deployment(name = OR_SOME_SAME_ROLES)
    public static WebArchive deploymentOrSomeSameRoles() {
        return createDeploymentForPrintingRoles(OR_SOME_SAME_ROLES);
    }

    @Deployment(name = OR_EMPTY_ROLE)
    public static WebArchive deploymentOrEmptyRole() {
        return createDeploymentForPrintingRoles(OR_EMPTY_ROLE);
    }

    @Deployment(name = OR_DIFFERENT_ROLES)
    public static WebArchive deploymentOrDifferentRoles() {
        return createDeploymentForPrintingRoles(OR_DIFFERENT_ROLES);
    }

    @Deployment(name = OR_LEFT_IS_MISSING_ROLES)
    public static WebArchive deploymentOrLeftIsMissing() {
        return createDeploymentForPrintingRoles(OR_LEFT_IS_MISSING_ROLES);
    }

    @Deployment(name = OR_RIGHT_IS_MISSING_ROLES)
    public static WebArchive deploymentOrRightIsMissing() {
        return createDeploymentForPrintingRoles(OR_RIGHT_IS_MISSING_ROLES);
    }

    @Deployment(name = MINUS_SOME_SAME_ROLES)
    public static WebArchive deploymentMinusSomeSameRoles() {
        return createDeploymentForPrintingRoles(MINUS_SOME_SAME_ROLES);
    }

    @Deployment(name = MINUS_EMPTY_ROLE_LEFT_ROLES)
    public static WebArchive deploymentMinusEmptyRoleLeft() {
        return createDeploymentForPrintingRoles(MINUS_EMPTY_ROLE_LEFT_ROLES);
    }

    @Deployment(name = MINUS_EMPTY_ROLE_RIGHT)
    public static WebArchive deploymentMinusEmptyRoleRight() {
        return createDeploymentForPrintingRoles(MINUS_EMPTY_ROLE_RIGHT);
    }

    @Deployment(name = MINUS_DIFFERENT_ROLES)
    public static WebArchive deploymentMinusDifferentRoles() {
        return createDeploymentForPrintingRoles(MINUS_DIFFERENT_ROLES);
    }

    @Deployment(name = MINUS_LEFT_IS_MISSING_ROLES)
    public static WebArchive deploymentMinusLeftIsMissing() {
        return createDeploymentForPrintingRoles(MINUS_LEFT_IS_MISSING_ROLES);
    }

    @Deployment(name = MINUS_RIGHT_IS_MISSING_ROLES)
    public static WebArchive deploymentMinusRightIsMissing() {
        return createDeploymentForPrintingRoles(MINUS_RIGHT_IS_MISSING_ROLES);
    }

    @Deployment(name = XOR_SOME_SAME_ROLES)
    public static WebArchive deploymentXorSomeSameRoles() {
        return createDeploymentForPrintingRoles(XOR_SOME_SAME_ROLES);
    }

    @Deployment(name = XOR_EMPTY_ROLE)
    public static WebArchive deploymentXorEmptyRole() {
        return createDeploymentForPrintingRoles(XOR_EMPTY_ROLE);
    }

    @Deployment(name = XOR_DIFFERENT_ROLES)
    public static WebArchive deploymentXorDifferentRoles() {
        return createDeploymentForPrintingRoles(XOR_DIFFERENT_ROLES);
    }

    @Deployment(name = XOR_LEFT_IS_MISSING_ROLES)
    public static WebArchive deploymentXorLeftIsMissing() {
        return createDeploymentForPrintingRoles(XOR_LEFT_IS_MISSING_ROLES);
    }

    @Deployment(name = XOR_RIGHT_IS_MISSING_ROLES)
    public static WebArchive deploymentXorRightIsMissing() {
        return createDeploymentForPrintingRoles(XOR_RIGHT_IS_MISSING_ROLES);
    }

    /**
     * Given: Logical Role Mapper which has configured attribute 'logicalOperation=AND' <br>
     * and attribute left maps roles Role1, Role2 and Role3 <br>
     * and attribute right maps roles Role2, Role3 and Role4. <br>
     * When the user is authenticated <br>
     * then just roles Role2 and Role3 should be assigned to the user. <br>
     */
    @Test
    @OperateOnDeployment(AND_SOME_SAME_ROLES)
    public void testAnd_someSameRoles(@ArquillianResource URL webAppURL) throws Exception {
        testAssignedRoles(webAppURL, USER, PASSWORD, ROLE2, ROLE3);
    }

    /**
     * Given: Logical Role Mapper which has configured attribute 'logicalOperation=AND' <br>
     * and attribute left maps no roles <br>
     * and attribute right maps roles Role2, Role3 and Role4. <br>
     * When the user is authenticated <br>
     * then no roles should be assigned to the user. <br>
     */
    @Test
    @OperateOnDeployment(AND_EMPTY_ROLE)
    public void testAnd_emptyRole(@ArquillianResource URL webAppURL) throws Exception {
        assertNoRoleAssigned(webAppURL, USER, PASSWORD);
    }

    /**
     * Given: Logical Role Mapper which has configured attribute 'logicalOperation=AND' <br>
     * and attribute left maps roles Role1 and Role2 <br>
     * and attribute right maps role Role3. <br>
     * When the user is authenticated <br>
     * then no roles should be assigned to the user. <br>
     */
    @Test
    @OperateOnDeployment(AND_DIFFERENT_ROLES)
    public void testAnd_differentRoles(@ArquillianResource URL webAppURL) throws Exception {
        assertNoRoleAssigned(webAppURL, USER, PASSWORD);
    }

    /**
     * Given: Logical Role Mapper which has configured attribute 'logicalOperation=AND' <br>
     * and attribute left is missing <br>
     * and attribute right maps roles Role1, Role2 and Role3 <br>
     * and roles property files map roles Role2, Role3 and Role4 for the user. <br>
     * When the user is authenticated <br>
     * then just roles Role2 and Role3 should be assigned to the user (which means that left side of operation has been taken
     * from user identity). <br>
     */
    @Test
    @OperateOnDeployment(AND_LEFT_IS_MISSING_ROLES)
    public void testAnd_leftIsMissing(@ArquillianResource URL webAppURL) throws Exception {
        testAssignedRoles(webAppURL, USER_WITH_ROLE_2_3_4, PASSWORD, ROLE2, ROLE3);
    }

    /**
     * Given: Logical Role Mapper which has configured attribute 'logicalOperation=AND' <br>
     * and attribute right is missing <br>
     * and attribute left maps roles Role1, Role2 and Role3 <br>
     * and roles property files map roles Role2, Role3 and Role4 for the user. <br>
     * When the user is authenticated <br>
     * then just roles Role2 and Role3 should be assigned to the user (which means that right side of operation has been taken
     * from user identity). <br>
     */
    @Test
    @OperateOnDeployment(AND_RIGHT_IS_MISSING_ROLES)
    public void testAnd_rightIsMissing(@ArquillianResource URL webAppURL) throws Exception {
        testAssignedRoles(webAppURL, USER_WITH_ROLE_2_3_4, PASSWORD, ROLE2, ROLE3);
    }

    /**
     * Given: Logical Role Mapper which has configured attribute 'logicalOperation=OR' <br>
     * and attribute left maps roles Role1, Role2 and Role3 <br>
     * and attribute right maps roles Role2, Role3 and Role4. <br>
     * When the user is authenticated <br>
     * then just roles Role1, Role2, Role3 and Role4 should be assigned to the user. <br>
     */
    @Test
    @OperateOnDeployment(OR_SOME_SAME_ROLES)
    public void testOr_someSameRoles(@ArquillianResource URL webAppURL) throws Exception {
        testAssignedRoles(webAppURL, USER, PASSWORD, ROLE1, ROLE2, ROLE3, ROLE4);
    }

    /**
     * Given: Logical Role Mapper which has configured attribute 'logicalOperation=OR' <br>
     * and attribute left maps no roles <br>
     * and attribute right maps roles Role2, Role3 and Role4. <br>
     * When the user is authenticated <br>
     * then just roles Role2, Role3 and Role4 should be assigned to the user. <br>
     */
    @Test
    @OperateOnDeployment(OR_EMPTY_ROLE)
    public void testOr_emptyRole(@ArquillianResource URL webAppURL) throws Exception {
        testAssignedRoles(webAppURL, USER, PASSWORD, ROLE2, ROLE3, ROLE4);
    }

    /**
     * Given: Logical Role Mapper which has configured attribute 'logicalOperation=OR' <br>
     * and attribute left maps roles Role1 and Role2 <br>
     * and attribute right maps role Role3. <br>
     * When the user is authenticated <br>
     * then just roles Role1, Role2 and Role3 should be assigned to the user. <br>
     */
    @Test
    @OperateOnDeployment(OR_DIFFERENT_ROLES)
    public void testOr_differentRoles(@ArquillianResource URL webAppURL) throws Exception {
        testAssignedRoles(webAppURL, USER, PASSWORD, ROLE1, ROLE2, ROLE3);
    }

    /**
     * Given: Logical Role Mapper which has configured attribute 'logicalOperation=OR' <br>
     * and attribute left is missing <br>
     * and attribute right maps roles Role1, Role2 and Role3 <br>
     * and roles property files map roles Role2, Role3 and Role4 for the user. <br>
     * When the user is authenticated <br>
     * then just roles Role1, Role2, Role3 and Role4 should be assigned to the user (which means that left side of operation has
     * been taken from user identity). <br>
     */
    @Test
    @OperateOnDeployment(OR_LEFT_IS_MISSING_ROLES)
    public void testOr_leftIsMissing(@ArquillianResource URL webAppURL) throws Exception {
        testAssignedRoles(webAppURL, USER_WITH_ROLE_2_3_4, PASSWORD, ROLE1, ROLE2, ROLE3, ROLE4);
    }

    /**
     * Given: Logical Role Mapper which has configured attribute 'logicalOperation=OR' <br>
     * and attribute right is missing <br>
     * and attribute left maps roles Role1, Role2 and Role3 <br>
     * and roles property files map roles Role2, Role3 and Role4 for the user. <br>
     * When the user is authenticated <br>
     * then just roles Role1, Role2, Role3 and Role4 should be assigned to the user (which means that right side of operation
     * has been taken from user identity). <br>
     */
    @Test
    @OperateOnDeployment(OR_RIGHT_IS_MISSING_ROLES)
    public void testOr_rightIsMissing(@ArquillianResource URL webAppURL) throws Exception {
        testAssignedRoles(webAppURL, USER_WITH_ROLE_2_3_4, PASSWORD, ROLE1, ROLE2, ROLE3, ROLE4);
    }

    /**
     * Given: Logical Role Mapper which has configured attribute 'logicalOperation=MINUS' <br>
     * and attribute left maps roles Role1, Role2 and Role3 <br>
     * and attribute right maps roles Role2, Role3 and Role4. <br>
     * When the user is authenticated <br>
     * then just role Role1 should be assigned to the user. <br>
     */
    @Test
    @OperateOnDeployment(MINUS_SOME_SAME_ROLES)
    public void testMinus_someSameRoles(@ArquillianResource URL webAppURL) throws Exception {
        testAssignedRoles(webAppURL, USER, PASSWORD, ROLE1);
    }

    /**
     * Given: Logical Role Mapper which has configured attribute 'logicalOperation=MINUS' <br>
     * and attribute left maps no roles <br>
     * and attribute right maps roles Role2, Role3 and Role4. <br>
     * When the user is authenticated <br>
     * then no roles should be assigned to the user. <br>
     */
    @Test
    @OperateOnDeployment(MINUS_EMPTY_ROLE_LEFT_ROLES)
    public void testMinus_emptyRoleLeft(@ArquillianResource URL webAppURL) throws Exception {
        assertNoRoleAssigned(webAppURL, USER, PASSWORD);
    }

    /**
     * Given: Logical Role Mapper which has configured attribute 'logicalOperation=MINUS' <br>
     * and attribute left maps roles Role2, Role3 and Role4 <br>
     * and attribute right maps no roles. <br>
     * When the user is authenticated <br>
     * then just role Role2, Role3 and Role4 should be assigned to the user. <br>
     */
    @Test
    @OperateOnDeployment(MINUS_EMPTY_ROLE_RIGHT)
    public void testMinus_emptyRoleRight(@ArquillianResource URL webAppURL) throws Exception {
        testAssignedRoles(webAppURL, USER, PASSWORD, ROLE2, ROLE3, ROLE4);
    }

    /**
     * Given: Logical Role Mapper which has configured attribute 'logicalOperation=MINUS' <br>
     * and attribute left maps roles Role1 and Role2 <br>
     * and attribute right maps role Role3. <br>
     * When the user is authenticated <br>
     * then just role Role1 and Role2 should be assigned to the user. <br>
     */
    @Test
    @OperateOnDeployment(MINUS_DIFFERENT_ROLES)
    public void testMinus_differentRoles(@ArquillianResource URL webAppURL) throws Exception {
        testAssignedRoles(webAppURL, USER, PASSWORD, ROLE1, ROLE2);
    }

    /**
     * Given: Logical Role Mapper which has configured attribute 'logicalOperation=MINUS' <br>
     * and attribute left is missing <br>
     * and attribute right maps roles Role1, Role2 and Role3 <br>
     * and roles property files map roles Role2, Role3 and Role4 for the user. <br>
     * When the user is authenticated <br>
     * then just role Role4 should be assigned to the user (which means that left side of operation has been taken from user
     * identity). <br>
     */
    @Test
    @OperateOnDeployment(MINUS_LEFT_IS_MISSING_ROLES)
    public void testMinus_leftIsMissing(@ArquillianResource URL webAppURL) throws Exception {
        testAssignedRoles(webAppURL, USER_WITH_ROLE_2_3_4, PASSWORD, ROLE4);
    }

    /**
     * Given: Logical Role Mapper which has configured attribute 'logicalOperation=MINUS' <br>
     * and attribute right is missing <br>
     * and attribute left maps roles Role1, Role2 and Role3 <br>
     * and roles property files map roles Role2, Role3 and Role4 for the user. <br>
     * When the user is authenticated <br>
     * then just role Role1 should be assigned to the user (which means that right side of operation has been taken from user
     * identity). <br>
     */
    @Test
    @OperateOnDeployment(MINUS_RIGHT_IS_MISSING_ROLES)
    public void testMinus_rightIsMissing(@ArquillianResource URL webAppURL) throws Exception {
        testAssignedRoles(webAppURL, USER_WITH_ROLE_2_3_4, PASSWORD, ROLE1);
    }

    /**
     * Given: Logical Role Mapper which has configured attribute 'logicalOperation=XOR' <br>
     * and attribute left maps roles Role1, Role2 and Role3 <br>
     * and attribute right maps roles Role2, Role3 and Role4. <br>
     * When the user is authenticated <br>
     * then just roles Role1 and Role4 should be assigned to the user. <br>
     */
    @Test
    @OperateOnDeployment(XOR_SOME_SAME_ROLES)
    public void testXor_someSameRoles(@ArquillianResource URL webAppURL) throws Exception {
        testAssignedRoles(webAppURL, USER, PASSWORD, ROLE1, ROLE4);
    }

    /**
     * Given: Logical Role Mapper which has configured attribute 'logicalOperation=XOR' <br>
     * and attribute left maps no roles <br>
     * and attribute right maps roles Role2, Role3 and Role4. <br>
     * When the user is authenticated <br>
     * then just roles Role2, Role3 and Role4 should be assigned to the user. <br>
     */
    @Test
    @OperateOnDeployment(XOR_EMPTY_ROLE)
    public void testXor_emptyRole(@ArquillianResource URL webAppURL) throws Exception {
        testAssignedRoles(webAppURL, USER, PASSWORD, ROLE2, ROLE3, ROLE4);
    }

    /**
     * Given: Logical Role Mapper which has configured attribute 'logicalOperation=XOR' <br>
     * and attribute left maps roles Role1 and Role2 <br>
     * and attribute right maps role Role3. <br>
     * When the user is authenticated <br>
     * then just roles Role1, Role2 and Role3 should be assigned to the user. <br>
     */
    @Test
    @OperateOnDeployment(XOR_DIFFERENT_ROLES)
    public void testXor_differentRoles(@ArquillianResource URL webAppURL) throws Exception {
        testAssignedRoles(webAppURL, USER, PASSWORD, ROLE1, ROLE2, ROLE3);
    }

    /**
     * Given: Logical Role Mapper which has configured attribute 'logicalOperation=XOR' <br>
     * and attribute left is missing <br>
     * and attribute right maps roles Role1, Role2 and Role3 <br>
     * and roles property files map roles Role2, Role3 and Role4 for the user. <br>
     * When the user is authenticated <br>
     * then just roles Role1 and Role4 should be assigned to the user (which means that left side of operation has been taken
     * from user identity). <br>
     */
    @Test
    @OperateOnDeployment(XOR_LEFT_IS_MISSING_ROLES)
    public void testXor_leftIsMissing(@ArquillianResource URL webAppURL) throws Exception {
        testAssignedRoles(webAppURL, USER_WITH_ROLE_2_3_4, PASSWORD, ROLE1, ROLE4);
    }

    /**
     * Given: Logical Role Mapper which has configured attribute 'logicalOperation=XOR' <br>
     * and attribute right is missing <br>
     * and attribute left maps roles Role1, Role2 and Role3 <br>
     * and roles property files map roles Role2, Role3 and Role4 for the user. <br>
     * When the user is authenticated <br>
     * then just roles Role1 and Role4 should be assigned to the user (which means that right side of operation has been taken
     * from user identity). <br>
     */
    @Test
    @OperateOnDeployment(XOR_RIGHT_IS_MISSING_ROLES)
    public void testXor_rightIsMissing(@ArquillianResource URL webAppURL) throws Exception {
        testAssignedRoles(webAppURL, USER_WITH_ROLE_2_3_4, PASSWORD, ROLE1, ROLE4);
    }

    public static class ServerSetup extends AbstractElytronSetupTask {

        private static final String CONSTANT_ROLE_MAPPER_1_2_3 = "contant-role-mapper-1-2-3";
        private static final String CONSTANT_ROLE_MAPPER_2_3_4 = "contant-role-mapper-2-3-4";
        private static final String CONSTANT_ROLE_MAPPER_1_2 = "contant-role-mapper-1-2";
        private static final String CONSTANT_ROLE_MAPPER_3 = "contant-role-mapper-3";

        /**
         * There is no simple way how to map no role through any mapper. Empty role can be created as empty intersection in
         * another logical role mapper. This is dependent on correctly implemented AND in logical role mapper.
         */
        private static final String EMPTY_ROLE_MAPPER_HELPER_1 = "empty-role-maper-helper-1";
        private static final String EMPTY_ROLE_MAPPER_HELPER_2 = "empty-role-maper-helper-2";
        private static final String EMPTY_ROLE_MAPPER = "empty-role-maper";

        @Override
        protected ConfigurableElement[] getConfigurableElements() {
            List<ConfigurableElement> elements = new ArrayList<>();

            elements.add(new ConstantRoleMapperTestCase.ServerSetup.ConstantRoleMappers(
                    String.format("%s:add(roles=[%s,%s,%s])", CONSTANT_ROLE_MAPPER_1_2_3, ROLE1, ROLE2, ROLE3),
                    String.format("%s:add(roles=[%s,%s,%s])", CONSTANT_ROLE_MAPPER_2_3_4, ROLE2, ROLE3, ROLE4),
                    String.format("%s:add(roles=[%s,%s])", CONSTANT_ROLE_MAPPER_1_2, ROLE1, ROLE2),
                    String.format("%s:add(roles=[%s])", CONSTANT_ROLE_MAPPER_3, ROLE3),
                    String.format("%s:add(roles=[%s])", EMPTY_ROLE_MAPPER_HELPER_1, ROLE1),
                    String.format("%s:add(roles=[%s])", EMPTY_ROLE_MAPPER_HELPER_2, ROLE2)
            ));

            elements.add(new LogicalRoleMappers(
                    String.format("%s:add(left=%s,logical-operation=and,right=%s)",
                            EMPTY_ROLE_MAPPER, EMPTY_ROLE_MAPPER_HELPER_1, EMPTY_ROLE_MAPPER_HELPER_2)
            ));

            elements.add(new LogicalRoleMappers(
                    String.format("%s:add(left=%s,logical-operation=and,right=%s)",
                            AND_SOME_SAME_ROLES, CONSTANT_ROLE_MAPPER_1_2_3, CONSTANT_ROLE_MAPPER_2_3_4),
                    String.format("%s:add(left=%s,logical-operation=and,right=%s)",
                            AND_EMPTY_ROLE, EMPTY_ROLE_MAPPER, CONSTANT_ROLE_MAPPER_2_3_4),
                    String.format("%s:add(left=%s,logical-operation=and,right=%s)",
                            AND_DIFFERENT_ROLES, CONSTANT_ROLE_MAPPER_1_2, CONSTANT_ROLE_MAPPER_3),
                    String.format("%s:add(logical-operation=and,right=%s)",
                            AND_LEFT_IS_MISSING_ROLES, CONSTANT_ROLE_MAPPER_1_2_3),
                    String.format("%s:add(left=%s,logical-operation=and)",
                            AND_RIGHT_IS_MISSING_ROLES, CONSTANT_ROLE_MAPPER_1_2_3),
                    String.format("%s:add(left=%s,logical-operation=or,right=%s)",
                            OR_SOME_SAME_ROLES, CONSTANT_ROLE_MAPPER_1_2_3, CONSTANT_ROLE_MAPPER_2_3_4),
                    String.format("%s:add(left=%s,logical-operation=or,right=%s)",
                            OR_EMPTY_ROLE, EMPTY_ROLE_MAPPER, CONSTANT_ROLE_MAPPER_2_3_4),
                    String.format("%s:add(left=%s,logical-operation=or,right=%s)",
                            OR_DIFFERENT_ROLES, CONSTANT_ROLE_MAPPER_1_2, CONSTANT_ROLE_MAPPER_3),
                    String.format("%s:add(logical-operation=or,right=%s)",
                            OR_LEFT_IS_MISSING_ROLES, CONSTANT_ROLE_MAPPER_1_2_3),
                    String.format("%s:add(left=%s,logical-operation=or)",
                            OR_RIGHT_IS_MISSING_ROLES, CONSTANT_ROLE_MAPPER_1_2_3),
                    String.format("%s:add(left=%s,logical-operation=minus,right=%s)",
                            MINUS_SOME_SAME_ROLES, CONSTANT_ROLE_MAPPER_1_2_3, CONSTANT_ROLE_MAPPER_2_3_4),
                    String.format("%s:add(left=%s,logical-operation=minus,right=%s)",
                            MINUS_EMPTY_ROLE_LEFT_ROLES, EMPTY_ROLE_MAPPER, CONSTANT_ROLE_MAPPER_2_3_4),
                    String.format("%s:add(left=%s,logical-operation=minus,right=%s)",
                            MINUS_EMPTY_ROLE_RIGHT, CONSTANT_ROLE_MAPPER_2_3_4, EMPTY_ROLE_MAPPER),
                    String.format("%s:add(left=%s,logical-operation=minus,right=%s)",
                            MINUS_DIFFERENT_ROLES, CONSTANT_ROLE_MAPPER_1_2, CONSTANT_ROLE_MAPPER_3),
                    String.format("%s:add(logical-operation=minus,right=%s)",
                            MINUS_LEFT_IS_MISSING_ROLES, CONSTANT_ROLE_MAPPER_1_2_3),
                    String.format("%s:add(left=%s,logical-operation=minus)",
                            MINUS_RIGHT_IS_MISSING_ROLES, CONSTANT_ROLE_MAPPER_1_2_3),
                    String.format("%s:add(left=%s,logical-operation=xor,right=%s)",
                            XOR_SOME_SAME_ROLES, CONSTANT_ROLE_MAPPER_1_2_3, CONSTANT_ROLE_MAPPER_2_3_4),
                    String.format("%s:add(left=%s,logical-operation=xor,right=%s)",
                            XOR_EMPTY_ROLE, EMPTY_ROLE_MAPPER, CONSTANT_ROLE_MAPPER_2_3_4),
                    String.format("%s:add(left=%s,logical-operation=xor,right=%s)",
                            XOR_DIFFERENT_ROLES, CONSTANT_ROLE_MAPPER_1_2, CONSTANT_ROLE_MAPPER_3),
                    String.format("%s:add(logical-operation=xor,right=%s)",
                            XOR_LEFT_IS_MISSING_ROLES, CONSTANT_ROLE_MAPPER_1_2_3),
                    String.format("%s:add(left=%s,logical-operation=xor)",
                            XOR_RIGHT_IS_MISSING_ROLES, CONSTANT_ROLE_MAPPER_1_2_3)
            ));

            elements.add(PropertiesRealm.builder().withName(PROPERTIES_REALM_NAME)
                    .withUser(USER, PASSWORD)
                    .withUser(USER_WITH_ROLE_2_3_4, PASSWORD, ROLE2, ROLE3, ROLE4)
                    .build());
            addSecurityDomainWithRoleMapper(elements, AND_SOME_SAME_ROLES);
            addSecurityDomainWithRoleMapper(elements, AND_EMPTY_ROLE);
            addSecurityDomainWithRoleMapper(elements, AND_DIFFERENT_ROLES);
            addSecurityDomainWithRoleMapper(elements, AND_LEFT_IS_MISSING_ROLES);
            addSecurityDomainWithRoleMapper(elements, AND_RIGHT_IS_MISSING_ROLES);
            addSecurityDomainWithRoleMapper(elements, OR_SOME_SAME_ROLES);
            addSecurityDomainWithRoleMapper(elements, OR_EMPTY_ROLE);
            addSecurityDomainWithRoleMapper(elements, OR_DIFFERENT_ROLES);
            addSecurityDomainWithRoleMapper(elements, OR_LEFT_IS_MISSING_ROLES);
            addSecurityDomainWithRoleMapper(elements, OR_RIGHT_IS_MISSING_ROLES);
            addSecurityDomainWithRoleMapper(elements, MINUS_SOME_SAME_ROLES);
            addSecurityDomainWithRoleMapper(elements, MINUS_EMPTY_ROLE_LEFT_ROLES);
            addSecurityDomainWithRoleMapper(elements, MINUS_EMPTY_ROLE_RIGHT);
            addSecurityDomainWithRoleMapper(elements, MINUS_DIFFERENT_ROLES);
            addSecurityDomainWithRoleMapper(elements, MINUS_LEFT_IS_MISSING_ROLES);
            addSecurityDomainWithRoleMapper(elements, MINUS_RIGHT_IS_MISSING_ROLES);
            addSecurityDomainWithRoleMapper(elements, XOR_SOME_SAME_ROLES);
            addSecurityDomainWithRoleMapper(elements, XOR_EMPTY_ROLE);
            addSecurityDomainWithRoleMapper(elements, XOR_DIFFERENT_ROLES);
            addSecurityDomainWithRoleMapper(elements, XOR_LEFT_IS_MISSING_ROLES);
            addSecurityDomainWithRoleMapper(elements, XOR_RIGHT_IS_MISSING_ROLES);
            return elements.toArray(new ConfigurableElement[elements.size()]);
        }

        public static class LogicalRoleMappers implements ConfigurableElement {

            private final String[] dynamicLogicals;

            public LogicalRoleMappers(String... dynamicLogicals) {
                this.dynamicLogicals = dynamicLogicals;
            }

            @Override
            public void create(CLIWrapper cli) throws Exception {
                for (String log : dynamicLogicals) {
                    cli.sendLine("/subsystem=elytron/logical-role-mapper=" + log);
                }
            }

            @Override
            public void remove(CLIWrapper cli) throws Exception {
                for (String log : dynamicLogicals) {
                    int opIdx = log.indexOf(':');
                    String newLog = log.substring(0, opIdx + 1) + "remove()";
                    cli.sendLine("/subsystem=elytron/logical-role-mapper=" + newLog);
                }
            }

            @Override
            public String getName() {
                return "logical-role-mapper";
            }
        }

    }
}

/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.jboss.as.core.model.test.access;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ACCESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.AUTHORIZATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CORE_SERVICE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INCLUDE_ALL;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REALM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ROLE_MAPPING;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TYPE;
import static org.jboss.as.domain.management.ModelDescriptionConstants.IS_CALLER_IN_ROLE;
import static org.junit.Assert.assertEquals;

import java.security.Principal;
import java.security.PrivilegedAction;
import java.util.Set;

import javax.security.auth.Subject;

import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.core.model.test.AbstractCoreModelTest;
import org.jboss.as.core.model.test.KernelServices;
import org.jboss.as.core.model.test.TestModelType;
import org.jboss.as.core.security.AccountPrincipal;
import org.jboss.as.core.security.GroupPrincipal;
import org.jboss.as.core.security.RealmPrincipal;
import org.jboss.dmr.ModelNode;
import org.junit.Before;
import org.junit.Test;

/**
 * Test case to test the role mapping behaviour (model and runtime mapping).
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class RoleMappingTestCase extends AbstractCoreModelTest {

    private static final String TEST_REALM = "TestRealm";
    private static final String OTHER_REALM = "OtherRealm";
    private static final String OTHER_USER = "OtherUser";

    private KernelServices kernelServices;
    private int uniqueCount = 0;

    @Before
    public void setUp() throws Exception {
        kernelServices = createKernelServicesBuilder(TestModelType.STANDALONE)
                .setXmlResource("constraints.xml")
                .validateDescription()
                .build();
    }

    /**
     * Test that a user is assigned a role based on their username (not realm specific).
     *
     * Also verify that assignment of a group with the same name does not result in role assignment.
     */
    @Test
    public void testIncludeByUsername() {
        final String roleName = "TestRoleOne"; // Use a unique role for each test, a failure to clean up should not affect other tests.
        final String userName = "UserOne";
        addRole(roleName, false);
        addPrincipal(roleName, MappingType.INCLUDE, PrincipalType.USER, userName, null);
        assertIsCallerInRole(roleName, null, false);

        assertIsCallerInRole(roleName, true, userName, TEST_REALM, null);
        assertIsCallerInRole(roleName, false, OTHER_USER, TEST_REALM, null, userName);

        removeRole(roleName);
    }

    /**
     * Same as testIncludeByUsername but now verify that the users realm is taken into account.
     */
    @Test
    public void testIncludeByUsernameAndRealm() {
        final String roleName = "TestRoleTwo";
        final String userName = "UserTwo";
        addRole(roleName, false);
        addPrincipal(roleName, MappingType.INCLUDE, PrincipalType.USER, userName, TEST_REALM);
        assertIsCallerInRole(roleName, null, false);

        assertIsCallerInRole(roleName, true, userName, TEST_REALM, null);
        assertIsCallerInRole(roleName, false, userName, OTHER_REALM, null);
        assertIsCallerInRole(roleName, false, OTHER_USER, TEST_REALM, null, userName);

        removeRole(roleName);
    }

    /**
     * Test that a user is assigned a role based on their group membership (not realm specific).
     *
     * Also verify that a user account with the same name does not result in role assignment.
     */
    @Test
    public void testIncludeByGroup() {
        final String roleName = "TestRoleThree";
        final String userName = "UserThree";
        final String groupName = "GroupThree";
        addRole(roleName, false);
        addPrincipal(roleName, MappingType.INCLUDE, PrincipalType.GROUP, groupName, null);
        assertIsCallerInRole(roleName, null, false);

        assertIsCallerInRole(roleName, true, userName, TEST_REALM, null, groupName);
        assertIsCallerInRole(roleName, true, userName, OTHER_REALM, null, groupName);
        assertIsCallerInRole(roleName, false, groupName, TEST_REALM, null, userName);

        removeRole(roleName);
    }

    /**
     * Same as testIncludeByGroup but now include the realm name in the match.
     */
    @Test
    public void testIncludeByGroupAndRealm() {
        final String roleName = "TestRoleFour";
        final String userName = "UserFour";
        final String groupName = "GroupFour";
        addRole(roleName, false);
        addPrincipal(roleName, MappingType.INCLUDE, PrincipalType.GROUP, groupName, TEST_REALM);
        assertIsCallerInRole(roleName, null, false);

        assertIsCallerInRole(roleName, true, userName, TEST_REALM, null, groupName);
        assertIsCallerInRole(roleName, false, userName, OTHER_REALM, null, groupName);
        assertIsCallerInRole(roleName, false, groupName, TEST_REALM, null, userName);

        removeRole(roleName);
    }

    /**
     * Test that a user matched to a role by group is not assigned the role if their username is in the exclude list.
     */
    @Test
    public void testExcludeByUsername() {
        final String roleName = "TestRoleFive";
        final String userName = "UserFive";
        final String groupName = "GroupFive";
        addRole(roleName, false);
        addPrincipal(roleName, MappingType.INCLUDE, PrincipalType.GROUP, groupName, null);
        addPrincipal(roleName, MappingType.EXCLUDE, PrincipalType.USER, userName, null);
        assertIsCallerInRole(roleName, null, false);

        assertIsCallerInRole(roleName, true, OTHER_USER, TEST_REALM, null, groupName);
        assertIsCallerInRole(roleName, false, userName, TEST_REALM, null, groupName);

        removeRole(roleName);
    }

    /**
     * Same as testExcludeByUsername except the exclusion is realm specific.
     */
    @Test
    public void testExcludeByUsernameAndRealm() {
        final String roleName = "TestRoleFive";
        final String userName = "UserFive";
        final String groupName = "GroupFive";
        addRole(roleName, false);
        addPrincipal(roleName, MappingType.INCLUDE, PrincipalType.GROUP, groupName, null);
        addPrincipal(roleName, MappingType.EXCLUDE, PrincipalType.USER, userName, TEST_REALM);
        assertIsCallerInRole(roleName, null, false);

        assertIsCallerInRole(roleName, true, OTHER_USER, TEST_REALM, null, groupName);
        assertIsCallerInRole(roleName, false, userName, TEST_REALM, null, groupName);
        assertIsCallerInRole(roleName, true, userName, OTHER_REALM, null, groupName);

        removeRole(roleName);
    }

    /**
     * Test that a user assigned a role due to group membership is excluded based on the membership of another group.
     */
    @Test
    public void testExcludeByGroup() {
        final String roleName = "TestRoleSix";
        final String userName = "UserSix";
        final String inGroupName = "GroupSix_In";
        final String outGroupName = "GroupSix_Out";
        addRole(roleName, false);
        addPrincipal(roleName, MappingType.INCLUDE, PrincipalType.GROUP, inGroupName, null);
        addPrincipal(roleName, MappingType.EXCLUDE, PrincipalType.GROUP, outGroupName, null);
        assertIsCallerInRole(roleName, null, false);

        assertIsCallerInRole(roleName, true, userName, TEST_REALM, null, inGroupName);
        assertIsCallerInRole(roleName, false, userName, TEST_REALM, null, inGroupName, outGroupName);

        removeRole(roleName);
    }

    /**
     * Same as testExcludeByGroup but the exclusion takes the realm into account.
     */
    @Test
    public void testExcludeByGroupAndRealm() {
        final String roleName = "TestRoleSeven";
        final String userName = "UserSeven";
        final String inGroupName = "GroupSeven_In";
        final String outGroupName = "GroupSeven_Out";
        addRole(roleName, false);
        addPrincipal(roleName, MappingType.INCLUDE, PrincipalType.GROUP, inGroupName, null);
        addPrincipal(roleName, MappingType.EXCLUDE, PrincipalType.GROUP, outGroupName, TEST_REALM);
        assertIsCallerInRole(roleName, null, false);

        assertIsCallerInRole(roleName, true, userName, TEST_REALM, null, inGroupName);
        assertIsCallerInRole(roleName, false, userName, TEST_REALM, null, inGroupName, outGroupName);
        assertIsCallerInRole(roleName, true, userName, OTHER_REALM, null, inGroupName, outGroupName);

        removeRole(roleName);
    }

    /**
     * Test that user assigned the SUPERUSER role can actually request a different role.
     *
     * On requesting the different role the user should not be assigned the SUPERUSER role anymore.
     */
    @Test
    public void testSuperUserAs() {
        final String roleName = "SUPERUSER";
        final String otherRole = "OPERATOR";
        final String userName = "UserThirteen";
        addRole(roleName, false);
        ModelNode addedAddress = addPrincipal(roleName, MappingType.INCLUDE, PrincipalType.USER, userName, null);

        assertIsCallerInRole(roleName, true, userName, TEST_REALM, null);
        assertIsCallerInRole(otherRole, true, userName, TEST_REALM, otherRole);
        assertIsCallerInRole(roleName, false, userName, TEST_REALM, otherRole);

        removePrincipal(addedAddress);
        removeRole(roleName);
    }

    /**
     * Test that user assigned the OPERATOR role can NOT request a different role.
     */
    @Test
    public void testOperatorAs() {
        final String roleName = "OPERATOR";
        final String otherRole = "MONITOR";
        final String userName = "UserFourteen";
        addRole(roleName, false);
        ModelNode addedAddress = addPrincipal(roleName, MappingType.INCLUDE, PrincipalType.USER, userName, null);

        assertIsCallerInRole(roleName, true, userName, TEST_REALM, null);
        assertIsCallerInRole(otherRole, false, userName, TEST_REALM, otherRole);
        assertIsCallerInRole(roleName, true, userName, TEST_REALM, otherRole);

        removePrincipal(addedAddress);
        removeRole(roleName);
    }

    /**
     * Test that an authenticated user is assigned a role where include-all = true.
     */
    @Test
    public void testIncludeAll() {
        final String roleName = "TestRoleEight"; // Use a unique role for each test, a failure to clean up should not affect other tests.
        final String userName = "UserEight";
        addRole(roleName, true);
        assertIsCallerInRole(roleName, null, false);

        assertIsCallerInRole(roleName, true, userName, TEST_REALM, null);

        removeRole(roleName);
    }

    /**
     * Test that a user matched to a role by include-all is not assigned the role if their username is in the exclude list.
     */
    @Test
    public void testIncludeAll_ExcludeByUsername() {
        final String roleName = "TestRoleNine";
        final String userName = "UserNine";
        final String groupName = "GroupNine";
        addRole(roleName, true);
        addPrincipal(roleName, MappingType.EXCLUDE, PrincipalType.USER, userName, null);
        assertIsCallerInRole(roleName, null, false);

        assertIsCallerInRole(roleName, true, OTHER_USER, TEST_REALM, null, groupName);
        assertIsCallerInRole(roleName, false, userName, TEST_REALM, null, groupName);

        removeRole(roleName);
    }

    /**
     * Test that a user matched to a role by include-all is not assigned the role if their group is in the exclude list.
     */
    @Test
    public void testIncludeAll_ExcludeByGroup() {
        final String roleName = "TestRoleTen";
        final String userName = "UserTen";
        final String groupName = "GroupTen";
        addRole(roleName, true);
        addPrincipal(roleName, MappingType.EXCLUDE, PrincipalType.GROUP, groupName, null);
        assertIsCallerInRole(roleName, null, false);

        assertIsCallerInRole(roleName, true, userName, TEST_REALM, null);
        assertIsCallerInRole(roleName, false, userName, TEST_REALM, null, groupName);

        removeRole(roleName);
    }

    /*
     * Duplicate Handling
     *
     * Tests to verify that the add operations successfully detect duplicate include/exclude definitions.
     */

    public void testDuplicateUserComplete() {
        final String roleName = "TestRoleEleven";
        final String userName = "UserEleven";

        addRole(roleName, false);
        addPrincipal(roleName, MappingType.INCLUDE, PrincipalType.USER, userName, TEST_REALM);
        addPrincipal(roleName, MappingType.INCLUDE, PrincipalType.USER, userName, TEST_REALM, true);
    }

    public void testDuplicateUserRealmLess() {
        final String roleName = "TestRoleTwelve";
        final String userName = "UserTwelve";

        addRole(roleName, false);
        addPrincipal(roleName, MappingType.INCLUDE, PrincipalType.USER, userName, null);
        addPrincipal(roleName, MappingType.INCLUDE, PrincipalType.USER, userName, TEST_REALM);
        addPrincipal(roleName, MappingType.INCLUDE, PrincipalType.USER, userName, null, true);

        addPrincipal(roleName, MappingType.EXCLUDE, PrincipalType.USER, userName, TEST_REALM);
        addPrincipal(roleName, MappingType.EXCLUDE, PrincipalType.USER, userName, null);
        addPrincipal(roleName, MappingType.EXCLUDE, PrincipalType.USER, userName, null, true);
    }

    public void testDuplicateGroupComplete() {
        final String roleName = "TestRoleThirteen";
        final String groupName = "UserThirteen";

        addRole(roleName, false);
        addPrincipal(roleName, MappingType.EXCLUDE, PrincipalType.GROUP, groupName, TEST_REALM);
        addPrincipal(roleName, MappingType.EXCLUDE, PrincipalType.GROUP, groupName, TEST_REALM, true);
    }

    public void testDuplicateGroupRealmLess() {
        final String roleName = "TestRoleFourteen";
        final String groupName = "UserFourteen";

        addRole(roleName, false);
        addPrincipal(roleName, MappingType.EXCLUDE, PrincipalType.GROUP, groupName, null);
        addPrincipal(roleName, MappingType.EXCLUDE, PrincipalType.GROUP, groupName, TEST_REALM);
        addPrincipal(roleName, MappingType.EXCLUDE, PrincipalType.GROUP, groupName, null, true);

        addPrincipal(roleName, MappingType.INCLUDE, PrincipalType.GROUP, groupName, TEST_REALM);
        addPrincipal(roleName, MappingType.INCLUDE, PrincipalType.GROUP, groupName, null);
        addPrincipal(roleName, MappingType.INCLUDE, PrincipalType.GROUP, groupName, null, true);
    }

    private void addRole(final String roleName, boolean includeAll) {
        ModelNode operation = new ModelNode();
        operation.get(OP_ADDR).add(CORE_SERVICE, MANAGEMENT).add(ACCESS, AUTHORIZATION).add(ROLE_MAPPING, roleName);
        operation.get(OP).set(ADD);
        if (includeAll) {
            operation.get(INCLUDE_ALL).set(true);
        }

        ModelNode response = kernelServices.executeOperation(operation);
        assertEquals(SUCCESS, response.get(OUTCOME).asString());
    }

    private ModelNode addPrincipal(final String roleName, final MappingType mappingType, final PrincipalType principalType, final String name, final String realm) {
        return addPrincipal(roleName, mappingType, principalType, name, realm, false);
    }

    private ModelNode addPrincipal(final String roleName, final MappingType mappingType, final PrincipalType principalType, final String name, final String realm, boolean expectFailure) {
        ModelNode operation = new ModelNode();
        operation.get(OP_ADDR).add(CORE_SERVICE, MANAGEMENT).add(ACCESS, AUTHORIZATION).add(ROLE_MAPPING, roleName).add(mappingType.toString(), uniqueCount++);
        operation.get(OP).set(ADD);
        operation.get(TYPE).set(principalType.toString());
        operation.get(NAME).set(name);
        if (realm != null) {
            operation.get(REALM).set(realm);
        }

        ModelNode response = kernelServices.executeOperation(operation);
        if (expectFailure) {
            assertEquals(FAILED, response.get(OUTCOME).asString());
        } else {
            assertEquals(SUCCESS, response.get(OUTCOME).asString());
        }

        return operation.get(OP_ADDR);
    }

    private void removePrincipal(final ModelNode address) {
        ModelNode operation = new ModelNode();
        operation.get(OP_ADDR).set(address);
        operation.get(OP).set(REMOVE);

        ModelNode response = kernelServices.executeOperation(operation);
        assertEquals(SUCCESS, response.get(OUTCOME).asString());
    }

    private void removeRole(final String roleName) {
        ModelNode operation = new ModelNode();
        operation.get(OP_ADDR).add(CORE_SERVICE, MANAGEMENT).add(ACCESS, AUTHORIZATION).add(ROLE_MAPPING, roleName);
        operation.get(OP).set(REMOVE);

        ModelNode response = kernelServices.executeOperation(operation);
        assertEquals(SUCCESS, response.get(OUTCOME).asString());
    }

    private void assertIsCallerInRole(final String roleName, final String runAsRole, final boolean expectedOutcome) {
        ModelNode operation = new ModelNode();
        operation.get(OP_ADDR).add(CORE_SERVICE, MANAGEMENT).add(ACCESS, AUTHORIZATION).add(ROLE_MAPPING, roleName);
        operation.get(OP).set(IS_CALLER_IN_ROLE);
        if (runAsRole != null) {
            ModelNode headers = operation.get(ModelDescriptionConstants.OPERATION_HEADERS);
            headers.get("roles").set(runAsRole);
        }

        ModelNode response = kernelServices.executeOperation(operation);
        assertEquals(SUCCESS, response.get(OUTCOME).asString());
        assertEquals(expectedOutcome, response.get(RESULT).asBoolean());
    }

    private void assertIsCallerInRole(final String roleName, final boolean expectedOutcome, final String userName,
            final String realm, final String runAsRole, final String... groups) {
        Subject subject = new Subject();
        Set<Principal> principals = subject.getPrincipals();
        principals.add(new User(userName, realm));
        for (String current : groups) {
            principals.add(new Group(current, realm));
        }

        Subject.doAs(subject, new PrivilegedAction<Void>() {

            @Override
            public Void run() {
                assertIsCallerInRole(roleName, runAsRole, expectedOutcome);
                return null;
            }
        });
    }

    private enum PrincipalType {
        GROUP, USER;

        @Override
        public String toString() {
            return super.toString().toLowerCase();
        }
    }

    private enum MappingType {
        EXCLUDE, INCLUDE;

        @Override
        public String toString() {
            return super.toString().toLowerCase();
        }

    }

    private static class User implements RealmPrincipal, AccountPrincipal {

        private final String realm;
        private final String name;

        private User(final String name, final String realm) {
            this.name = name;
            this.realm = realm;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getRealm() {
            return realm;
        }

    }

    private static class Group implements RealmPrincipal, GroupPrincipal {

        private final String name;
        private final String realm;

        private Group(final String name, final String realm) {
            this.name = name;
            this.realm = realm;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getRealm() {
            return realm;
        }

    }

}

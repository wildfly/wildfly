/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat Middleware LLC, and individual contributors
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

package org.jboss.as.test.integration.domain.mixed;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADDRESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.AUTHENTICATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CHILD_TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROFILE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_CHILDREN_NAMES_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING_GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.test.integration.domain.management.util.DomainTestUtils.executeForResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.client.helpers.domain.DomainClient;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.security.SecurityExtension;
import org.jboss.as.test.integration.domain.management.util.DomainTestUtils;
import org.jboss.as.test.integration.domain.mixed.eap620.DomainAdjuster620;
import org.jboss.as.test.integration.domain.mixed.eap630.DomainAdjuster630;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;


/**
 * Adjusts the domain configuration for the legacy server version.
 * The default implementation trims down the domain model to only include the following
 * profiles, socket-binding-groups and server-groups
 * <uL>
 *     <li>{@code /profile=full-ha}</li>
 *     <li>{@code /socket-binding-group=full-ha-sockets}</li>
 *     <li>{@code /server-group=other-server-group}</li>
 *     <li>{@code /server-group=main-server-group}</li>
 * </uL>
 * Subclasses can then further execute operations to put the model in a state which can be transformed
 * to a legacy version.
 *
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class DomainAdjuster {

    //We only want to test the full ha profile in these tests
    private final String FULL_HA = "full-ha";
    private final String MAIN_SERVER_GROUP = "main-server-group";
    private final String OTHER_SERVER_GROUP = "other-server-group";

    protected DomainAdjuster() {
    }

    static void adjustForVersion(final DomainClient client, final Version.AsVersion asVersion) throws Exception {

        final DomainAdjuster adjuster;
        switch (asVersion) {
            case EAP_6_2_0:
                adjuster = new DomainAdjuster620();
                break;
            case EAP_6_3_0:
                adjuster = new DomainAdjuster630();
                break;
            default:
                adjuster = new DomainAdjuster();
        }

        adjuster.adjust(client);
    }

    final void adjust(final DomainClient client) throws Exception {
        //Trim it down so we have only
        //profile=full-ha,
        //the main-server-group and other-server-group
        //socket-binding-group = full-ha-sockets
        final List<String> allProfiles = getAllChildrenOfType(client, PathAddress.EMPTY_ADDRESS, PROFILE);
        final ModelNode serverGroup = removeServerGroups(client);
        final String socketBindingGroup = serverGroup.get(SOCKET_BINDING_GROUP).asString();
        removeUnusedSocketBindingGroups(client, socketBindingGroup);

        for (String profileName : allProfiles) {
            if (profileName.equals(FULL_HA)) {
                continue;
            }
            removeProfile(client, profileName);
        }

        //Add a jaspi test security domain used later by the tests
        addJaspiTestSecurityDomain(client);

        //Version specific changes
        final List<ModelNode> adjustments = adjustForVersion(client, PathAddress.pathAddress(PROFILE, FULL_HA));
        applyVersionAdjustments(client, adjustments);
    }

    protected List<ModelNode> adjustForVersion(final DomainClient client, final PathAddress profileAddress) throws  Exception {
        return Collections.emptyList();
    }

    private void removeProfile(final DomainClient client, final String name) throws Exception {
        //The profile does not allow deletion unless all child resources have been removed, so remove all the subsystems
        //individually
        final PathAddress profileAddress = PathAddress.pathAddress(PROFILE, name);
        final List<String> subsystems = getAllChildrenOfType(client, profileAddress, SUBSYSTEM);
        for (String subsystem : subsystems) {
            removeSubsystem(client, profileAddress, subsystem);
        }
        executeForResult(Util.createRemoveOperation(profileAddress), client);
    }

    private void removeSubsystem(final DomainClient client, final PathAddress profileAddress,
                                 final String subsystem) throws Exception {
        executeForResult(Util.createRemoveOperation(profileAddress.append(SUBSYSTEM, subsystem)), client);
    }

    private List<String> getAllChildrenOfType(final DomainClient client,
                                              final PathAddress parent, final String type) throws Exception {
        final ModelNode op = Util.createEmptyOperation(READ_CHILDREN_NAMES_OPERATION, parent);
        op.get(CHILD_TYPE).set(type);
        final ModelNode result = executeForResult(op, client);
        final List<String> childNames = new ArrayList<>();
        for (ModelNode nameNode : result.asList()) {
            childNames.add(nameNode.asString());
        }
        return childNames;
    }

    private ModelNode removeServerGroups(final DomainClient client) throws  Exception {
        final ModelNode op = Util.createEmptyOperation(READ_RESOURCE_OPERATION, PathAddress.pathAddress(PathElement.pathElement(SERVER_GROUP)));
        final ModelNode results =  executeForResult(op, client);
        ModelNode group = null;
        for (ModelNode result : results.asList()) {
            String groupName = PathAddress.pathAddress(result.get(ADDRESS)).getLastElement().getValue();
            if (groupName.equals(OTHER_SERVER_GROUP)) {
                group = result.get(RESULT);
            } else {
                ModelNode remove = Util.createRemoveOperation(PathAddress.pathAddress(result.get(ADDRESS)));
                executeForResult(remove, client);
            }
        }
        Assert.assertNotNull(group);

        //Add main-server-group as a copy of other-server-group (cuts down on the amount of profiles needed)
        final ModelNode addMain = group.clone();
        final PathAddress mainServerGroupAddress = PathAddress.pathAddress(SERVER_GROUP, MAIN_SERVER_GROUP);
        addMain.get(OP).set(ADD);
        addMain.get(OP_ADDR).set(mainServerGroupAddress.toModelNode());
        executeForResult(addMain, client);

        return group;
    }

    private void removeUnusedSocketBindingGroups(final DomainClient client, final String keepGroup) throws  Exception {
        final List<String> allGroups = getAllChildrenOfType(client, PathAddress.EMPTY_ADDRESS, SOCKET_BINDING_GROUP);
        for (String groupName : allGroups) {
            if (!keepGroup.equals(groupName)) {
                ModelNode remove = Util.createRemoveOperation(PathAddress.pathAddress(PathAddress.pathAddress(SOCKET_BINDING_GROUP, groupName)));
                executeForResult(remove, client);
            }
        }
    }

    private void addJaspiTestSecurityDomain(final DomainClient client) throws Exception {
        //Before when this test was configured via xml, there was an extra security domain for testing jaspi.
        final PathAddress domain = PathAddress.pathAddress(PROFILE, FULL_HA)
                .append(SUBSYSTEM, SecurityExtension.SUBSYSTEM_NAME).append("security-domain", "jaspi-test");
        DomainTestUtils.executeForResult(Util.createAddOperation(domain), client);

        final PathAddress auth = domain.append(AUTHENTICATION, "jaspi");
        DomainTestUtils.executeForResult(Util.createAddOperation(auth), client);

        final PathAddress stack = auth.append("login-module-stack", "lm-stack");
        DomainTestUtils.executeForResult(Util.createAddOperation(stack), client);

        final ModelNode addLoginModule = Util.createAddOperation(stack.append("login-module", "lm"));
        addLoginModule.get("code").set("UsersRoles");
        addLoginModule.get("flag").set("required");
        addLoginModule.get("module").set("test-jaspi");
        final ModelNode options = addLoginModule.get("module-options");
        options.setEmptyList();
        options.add(new ModelNode().set("usersProperties", "${jboss.server.config.dir:}/application-users.properties"));
        options.add(new ModelNode().set("rolesProperties", "${jboss.server.config.dir:}/application-roles.properties"));
        DomainTestUtils.executeForResult(addLoginModule, client);

        final ModelNode addAuthModule = Util.createAddOperation(auth.append("auth-module", getJaspiTestAuthModuleName()));
        addAuthModule.get("code").set(getJaspiTestAuthModuleName());
        addAuthModule.get("login-module-stack-ref").set("lm-stack");
        addAuthModule.get("flag").set("${test.prop:optional}");
        DomainTestUtils.executeForResult(addAuthModule, client);
    }

    /**
     * Returns the class name of the http auth module. This uses the wildfly version. Adjusters for AS 7/EAP 6
     * should override this method and return
     * {@code org.wildfly.extension.undertow.security.jaspi.modules.HTTPSchemeServerAuthModule}
     *
     * @return the auth module
     */
    protected String getJaspiTestAuthModuleName() {
        return "org.wildfly.extension.undertow.security.jaspi.modules.HTTPSchemeServerAuthModule";
    }

    private void applyVersionAdjustments(DomainClient client, List<ModelNode> operations) throws Exception {
        if (operations.size() == 0) {
            return;
        }
        for (ModelNode op : operations) {
            DomainTestUtils.executeForResult(op, client);
        }
    }
}

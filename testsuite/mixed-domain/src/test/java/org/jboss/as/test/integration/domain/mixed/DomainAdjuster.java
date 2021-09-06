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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADDRESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.AUTHENTICATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.AUTO_START;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CHILD_TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.EXTENSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROFILE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_CHILDREN_NAMES_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_CONFIG;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING_GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SYSTEM_PROPERTY;
import static org.jboss.as.test.integration.domain.management.util.DomainTestUtils.executeForResult;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.client.helpers.domain.DomainClient;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.security.SecurityExtension;
import org.jboss.as.test.integration.domain.management.util.DomainTestUtils;
import org.jboss.as.test.integration.domain.mixed.eap740.DomainAdjuster740;
import org.jboss.as.test.integration.management.util.MgmtOperationException;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.junit.Assert;

/**
 * Adjusts the domain configuration for the legacy server version. The default implementation trims down the domain
 * model to only include the following profiles, socket-binding-groups and server-groups
 * <uL>
 * <li>{@code /profile=full-ha}</li>
 * <li>{@code /socket-binding-group=full-ha-sockets}</li>
 * <li>{@code /server-group=other-server-group}</li>
 * <li>{@code /server-group=main-server-group}</li>
 * </uL>
 * Subclasses can then further execute operations to put the model in a state which can be transformed to a legacy
 * version.
 *
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class DomainAdjuster {

    private final String MAIN_SERVER_GROUP = "main-server-group";
    private final String OTHER_SERVER_GROUP = "other-server-group";
    private static final Set<String> UNUSED_SERVER_GROUP_ATTRIBUTES = new HashSet<>(Arrays.asList("management-subsystem-endpoint", "deployment", "deployment-overlay", "jvm", "system-property"));

    static void adjustForVersion(final DomainClient client, final Version.AsVersion asVersion, final String profile, final boolean withMasterServers) throws Exception {

        final DomainAdjuster adjuster;
        switch (asVersion) {
            case EAP_7_4_0:
                adjuster = new DomainAdjuster740();
                break;
            default:
                adjuster = new DomainAdjuster();
        }
        adjuster.adjust(client, profile, withMasterServers);
    }

    final void adjust(final DomainClient client, String profile, boolean withMasterServers) throws Exception {
        //Trim it down so we have only
        //profile=full-ha,
        //the main-server-group and other-server-group
        //socket-binding-group = full-ha-sockets
        final List<String> allProfiles = getAllChildrenOfType(client, PathAddress.EMPTY_ADDRESS, PROFILE);
        final ModelNode serverGroup = removeServerGroups(client, profile);

        for (String profileName : allProfiles) {
            if (profile.equals(profileName)) {
                continue;
            }
            removeProfile(client, profileName);
        }
        final String socketBindingGroup = serverGroup.get(SOCKET_BINDING_GROUP).asString();
        removeUnusedSocketBindingGroups(client, socketBindingGroup);

        removeIpv4SystemProperty(client);

        // We don't want any standard host-excludes as the tests are meant to see what happens
        // with the current configs on legacy slaves
        removeHostExcludes(client);

        //Add a jaspi test security domain used later by the tests
        addJaspiTestSecurityDomain(client, profile);

        // Mixed Domain tests always use the full build instead of alternating between ee-dist and dist. If the DC is not an EAP server, we need to remove here
        // the pre-configured microprofile extensions provided by WildFly to adjust the current domain to work with a node running EAP which does not contain
        // those extensions.
        // We remove here these MP extensions and subsystems configured by default if they are in the current configuration.
        final PathAddress profileAddress = PathAddress.pathAddress(PROFILE, profile);
        removeSubsystemExtensionIfExist(client, profileAddress.append(SUBSYSTEM, "microprofile-opentracing-smallrye"), PathAddress.pathAddress(EXTENSION, "org.wildfly.extension.microprofile.opentracing-smallrye"));
        removeSubsystemExtensionIfExist(client, profileAddress.append(SUBSYSTEM, "microprofile-jwt-smallrye"), PathAddress.pathAddress(EXTENSION, "org.wildfly.extension.microprofile.jwt-smallrye"));
        removeSubsystemExtensionIfExist(client, profileAddress.append(SUBSYSTEM, "microprofile-config-smallrye"), PathAddress.pathAddress(EXTENSION, "org.wildfly.extension.microprofile.config-smallrye"));

        //Version specific changes
        final List<ModelNode> adjustments = adjustForVersion(client, PathAddress.pathAddress(PROFILE, profile), withMasterServers);
        if (withMasterServers) {
            adjustments.addAll(reconfigureServers());
        }

        applyVersionAdjustments(client, adjustments);
    }

    private void removeIpv4SystemProperty(final DomainClient client) throws Exception {
        //The standard domain configuration contains -Djava.net.preferIPv4Stack=true, remove that
        DomainTestUtils.executeForResult(
                Util.createRemoveOperation(PathAddress.pathAddress(SYSTEM_PROPERTY, "java.net.preferIPv4Stack")), client);

    }

    private void removeHostExcludes(DomainClient client) throws Exception {
        final List<String> allHostExcludes = getAllChildrenOfType(client, PathAddress.EMPTY_ADDRESS, "host-exclude");
        for (String exclude : allHostExcludes) {
            DomainTestUtils.executeForResult(
                    Util.createRemoveOperation(PathAddress.pathAddress("host-exclude", exclude)), client);
        }
    }

    /**
     * Adjust the Domain configuration to work properly with the expected slave.
     *
     * @param client           The domain client.
     * @param profileAddress   The address of the profile that is being used.
     * @param withMasterServer Whether the Dc has managed servers.
     * @return The List of Operations that need to be executed to adjust the domain.
     * @throws Exception
     */
    protected List<ModelNode> adjustForVersion(final DomainClient client, final PathAddress profileAddress, final boolean withMasterServer) throws Exception {
        return Collections.emptyList();
    }

    private void removeProfile(final DomainClient client, final String name) throws Exception {
        executeForResult(Util.createRemoveOperation(PathAddress.pathAddress(PROFILE, name)), client);
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

    private ModelNode removeServerGroups(final DomainClient client, String profile) throws Exception {
        final ModelNode op = Util.createEmptyOperation(READ_RESOURCE_OPERATION, PathAddress.pathAddress(PathElement.pathElement(SERVER_GROUP)));
        final ModelNode results = executeForResult(op, client);
        ModelNode group = null;
        for (ModelNode result : results.asList()) {
            String groupName = PathAddress.pathAddress(result.get(ADDRESS)).getLastElement().getValue();
            if (OTHER_SERVER_GROUP.equals(groupName)) {
                group = result.get(RESULT);
            } else if (MAIN_SERVER_GROUP.equals(groupName)) {
                //We'll update it afterwards because we have servers using it now
            } else {
                ModelNode remove = Util.createRemoveOperation(PathAddress.pathAddress(result.get(ADDRESS)));
                executeForResult(remove, client);
            }
        }
        Assert.assertNotNull(group);

        //Update main-server-group as a copy of other-server-group (cuts down on the amount of profiles needed)
        final PathAddress mainServerGroupAddress = PathAddress.pathAddress(SERVER_GROUP, MAIN_SERVER_GROUP);
        for (Property property : group.asPropertyList()) {
            ModelNode updateMain;
            if (!UNUSED_SERVER_GROUP_ATTRIBUTES.contains(property.getName())) {
                if (PROFILE.equals(property.getName())) {
                    updateMain = Util.getWriteAttributeOperation(mainServerGroupAddress, property.getName(), profile);
                } else {
                    if (property.getValue().isDefined()) {
                        updateMain = Util.getWriteAttributeOperation(mainServerGroupAddress, property.getName(), property.getValue());
                    } else {
                        updateMain = Util.getUndefineAttributeOperation(mainServerGroupAddress, property.getName());
                    }
                }
                executeForResult(updateMain, client);
            }
        }
        final PathAddress otherServerGroupAddress = PathAddress.pathAddress(SERVER_GROUP, OTHER_SERVER_GROUP);
        for (Property property : group.asPropertyList()) {
            ModelNode updateOther;
            if (!UNUSED_SERVER_GROUP_ATTRIBUTES.contains(property.getName())) {
                if (PROFILE.equals(property.getName())) {
                    updateOther = Util.getWriteAttributeOperation(otherServerGroupAddress, property.getName(), profile);
                } else {
                    if (property.getValue().isDefined()) {
                        updateOther = Util.getWriteAttributeOperation(mainServerGroupAddress, property.getName(), property.getValue());
                    } else {
                        updateOther = Util.getUndefineAttributeOperation(mainServerGroupAddress, property.getName());
                    }
                }
                executeForResult(updateOther, client);
            }
        }
        return group;
    }

    private void removeUnusedSocketBindingGroups(final DomainClient client, final String keepGroup) throws Exception {
        final List<String> allGroups = getAllChildrenOfType(client, PathAddress.EMPTY_ADDRESS, SOCKET_BINDING_GROUP);
        for (String groupName : allGroups) {
            if (!keepGroup.equals(groupName)) {
                ModelNode remove = Util.createRemoveOperation(PathAddress.pathAddress(PathAddress.pathAddress(SOCKET_BINDING_GROUP, groupName)));
                executeForResult(remove, client);
            }
        }
    }

    private void addJaspiTestSecurityDomain(final DomainClient client, String profile) throws Exception {
        //Before when this test was configured via xml, there was an extra security domain for testing jaspi.
        final PathAddress domain = PathAddress.pathAddress(PROFILE, profile)
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
     * Returns the class name of the http auth module. This uses the wildfly version. Adjusters for AS 7/EAP 6 should
     * override this method and return
     * {@code org.wildfly.extension.undertow.security.jaspi.modules.HTTPSchemeServerAuthModule}
     *
     * @return the auth module
     */
    protected String getJaspiTestAuthModuleName() {
        return "org.wildfly.extension.undertow.security.jaspi.modules.HTTPSchemeServerAuthModule";
    }

    private void applyVersionAdjustments(DomainClient client, List<ModelNode> operations) throws Exception {
        if (operations.isEmpty()) {
            return;
        }
        for (ModelNode op : operations) {
            DomainTestUtils.executeForResult(op, client);
        }
    }

    private Collection<? extends ModelNode> reconfigureServers() {
        final List<ModelNode> list = new ArrayList<>();
        //Reconfigure master servers
        final PathAddress masterHostAddress = PathAddress.pathAddress(HOST, "master");
        list.add(Util.getWriteAttributeOperation(masterHostAddress.append(SERVER_CONFIG, "server-one"), AUTO_START, true));
        list.add(Util.getWriteAttributeOperation(masterHostAddress.append(SERVER_CONFIG, "server-one"), ModelDescriptionConstants.GROUP, "main-server-group"));
        list.add(Util.getUndefineAttributeOperation(masterHostAddress.append(SERVER_CONFIG, "server-one"), ModelDescriptionConstants.SOCKET_BINDING_PORT_OFFSET));
        list.add(Util.getWriteAttributeOperation(masterHostAddress.append(SERVER_CONFIG, "server-two"), AUTO_START, true));
        list.add(Util.getWriteAttributeOperation(masterHostAddress.append(SERVER_CONFIG, "server-two"), ModelDescriptionConstants.SOCKET_BINDING_PORT_OFFSET, 100));
        return list;
    }

    private void removeSubsystemExtensionIfExist(DomainClient client, PathAddress subsystem, PathAddress extension) throws IOException {
        try {
            DomainTestUtils.executeForResult(Util.getReadResourceOperation(subsystem), client);
            DomainTestUtils.executeForResult(Util.createRemoveOperation(subsystem), client);
        } catch (MgmtOperationException e) {
            // ignored, the subsystem does not exist
        }
        try {
            DomainTestUtils.executeForResult(Util.getReadResourceOperation(extension), client);
            DomainTestUtils.executeForResult(Util.createRemoveOperation(extension), client);
        } catch (MgmtOperationException e) {
            // ignored, the extension does not exist
        }
    }
}

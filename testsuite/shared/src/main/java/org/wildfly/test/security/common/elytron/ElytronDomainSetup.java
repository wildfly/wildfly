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

package org.wildfly.test.security.common.elytron;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STEPS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.wildfly.test.security.common.elytron.Utils.applyRemoveAllowReload;
import static org.wildfly.test.security.common.elytron.Utils.applyUpdate;

import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.dmr.ModelNode;
import org.wildfly.extension.elytron.ElytronExtension;

/**
 * Utility methods to create/remove simple security domains
 *
 * @author <a href="mailto:jkalina@redhat.com">Jan Kalina</a>
 */
public class ElytronDomainSetup implements ServerSetupTask {

    private static final String DEFAULT_SECURITY_DOMAIN_NAME = "elytron-tests";
    private static final String DEFAULT_PERMISSION_MAPPER_NAME = "default-permission-mapper";

    private PathAddress realmAddress;

    private PathAddress domainAddress;
    private PathAddress permissionMapperAddress;
    private PathAddress roleDecoder1Address;
    private PathAddress roleDecoder2Address;
    private PathAddress aggregateRoleDecoderAddress;

    private final String usersFile;
    private final String groupsFile;
    private final String securityDomainName;
    private final String permissionMapperName;
    private final String ipAddress;

    public ElytronDomainSetup(final String usersFile, final String groupsFile) {
        this(usersFile, groupsFile, DEFAULT_SECURITY_DOMAIN_NAME, DEFAULT_PERMISSION_MAPPER_NAME, null);
    }

    public ElytronDomainSetup(final String usersFile, final String groupsFile, String securityDomainName) {
        this(usersFile, groupsFile, securityDomainName, DEFAULT_PERMISSION_MAPPER_NAME, null);
    }

    public ElytronDomainSetup(final String usersFile, final String groupsFile, final String securityDomainName, final String permissionMapperName, final String ipAddress) {
        this.usersFile = usersFile;
        this.groupsFile = groupsFile;
        this.securityDomainName = securityDomainName;
        this.permissionMapperName = permissionMapperName;
        this.ipAddress = ipAddress;
    }

    protected String getSecurityDomainName() {
        return securityDomainName;
    }

    protected String getSecurityRealmName() {
        return getSecurityDomainName() + "-ejb3-UsersRoles";
    }

    protected String getUndertowDomainName() {
        return getSecurityDomainName();
    }

    protected String getEjbDomainName() {
        return getSecurityDomainName();
    }

    protected String getSaslAuthenticationName() {
        return getSecurityDomainName();
    }

    protected String getRemotingConnectorName() {
        return "http-remoting-connector";
    }

    protected String getHttpAuthenticationName() {
        return getSecurityDomainName();
    }

    protected String getUsersFile() {
        return usersFile;
    }

    protected String getGroupsFile() {
        return groupsFile;
    }

    protected String getPermissionMapperName() {
        return permissionMapperName;
    }

    protected boolean isUsersFilePlain() {
        return true;
    }

    @Override
    public void setup(final ManagementClient managementClient, final String containerId) throws Exception {
        realmAddress = PathAddress.pathAddress()
                .append(SUBSYSTEM, ElytronExtension.SUBSYSTEM_NAME)
                .append("properties-realm", getSecurityRealmName());

        domainAddress = PathAddress.pathAddress()
                .append(SUBSYSTEM, ElytronExtension.SUBSYSTEM_NAME)
                .append("security-domain", getSecurityDomainName());

        final ModelNode compositeOp = new ModelNode();
        compositeOp.get(OP).set(ModelDescriptionConstants.COMPOSITE);
        compositeOp.get(OP_ADDR).setEmptyList();

        ModelNode steps = compositeOp.get(STEPS);

        // /subsystem=elytron/properties-realm=UsersRoles:add(users-properties={path=users.properties},groups-properties={path=roles.properties})
        ModelNode addRealm = Util.createAddOperation(realmAddress);
        addRealm.get("users-properties").get("path").set(getUsersFile());
        addRealm.get("users-properties").get("plain-text").set(isUsersFilePlain()); // not hashed
        addRealm.get("groups-properties").get("path").set(getGroupsFile());
        steps.add(addRealm);
        if (! permissionMapperName.equals(DEFAULT_PERMISSION_MAPPER_NAME)) {
            permissionMapperAddress = PathAddress.pathAddress()
                    .append(SUBSYSTEM, ElytronExtension.SUBSYSTEM_NAME)
                    .append("simple-permission-mapper", permissionMapperName);

            // /subsystem=elytron/simple-permission-mapper=ipPermissionMapper:add(permission-mappings=[{roles=[admin],
            // permission-sets=[{permission-set=login-permission}]}, {principals=[user2],permission-sets=[]}], mapping-mode="and")
            // (ensure that user1 is assigned the admin role if the IP address of the remote client matches the configured
            //  address and ensure user2 is not assigned the admin role even if the IP address of the remote client matches)
            ModelNode addPermissionMapper = Util.createAddOperation(permissionMapperAddress);
            ModelNode permissionMapping1 = new ModelNode();
            permissionMapping1.get("roles").add("Admin");
            ModelNode permissionSet = new ModelNode();
            permissionSet.get("permission-set").set("login-permission");
            permissionMapping1.get("permission-sets").add(permissionSet);
            addPermissionMapper.get("permission-mappings").add(permissionMapping1);
            ModelNode permissionMapping2 = new ModelNode();
            permissionMapping2.get("principals").add("user2");
            addPermissionMapper.get("permission-mappings").add(permissionMapping2);
            addPermissionMapper.get("mapping-mode").set("and");
            steps.add(addPermissionMapper);

            // /subsystem=elytron/source-address-role-decoder=decoder1:add(source-address=IP_ADDRESS, roles=["Admin"])
            roleDecoder1Address = PathAddress.pathAddress()
                    .append(SUBSYSTEM, ElytronExtension.SUBSYSTEM_NAME)
                    .append("source-address-role-decoder", "decoder1");
            ModelNode addRoleDecoder1 = Util.createAddOperation(roleDecoder1Address);
            addRoleDecoder1.get("source-address").set(ipAddress);
            addRoleDecoder1.get("roles").add("Admin");
            steps.add(addRoleDecoder1);

            // /subsystem=elytron/source-address-role-decoder=decoder2:add(source-address="99.99.99.99", roles=["Employee"])
            roleDecoder2Address = PathAddress.pathAddress()
                    .append(SUBSYSTEM, ElytronExtension.SUBSYSTEM_NAME)
                    .append("source-address-role-decoder", "decoder2");
            ModelNode addRoleDecoder2 = Util.createAddOperation(roleDecoder2Address);
            addRoleDecoder2.get("source-address").set("99.99.99.99");
            addRoleDecoder2.get("roles").add("Employee");
            steps.add(addRoleDecoder2);

            // /subsystem=elytron/aggregate-role-decoder=aggregateDecoder:add(role-decoders=[decoder1, decoder2])
            aggregateRoleDecoderAddress = PathAddress.pathAddress()
                    .append(SUBSYSTEM, ElytronExtension.SUBSYSTEM_NAME)
                    .append("aggregate-role-decoder", "aggregateRoleDecoder");
            ModelNode addAggregateRoleDecoder = Util.createAddOperation(aggregateRoleDecoderAddress);
            addAggregateRoleDecoder.get("role-decoders").add("decoder1");
            addAggregateRoleDecoder.get("role-decoders").add("decoder2");
            steps.add(addAggregateRoleDecoder);
        }

        // /subsystem=elytron/security-domain=EjbDomain:add(default-realm=UsersRoles, realms=[{realm=UsersRoles}])
        ModelNode addDomain = Util.createAddOperation(domainAddress);
        addDomain.get("permission-mapper").set(permissionMapperName);
        if (! permissionMapperName.equals(DEFAULT_PERMISSION_MAPPER_NAME)) {
            addDomain.get("role-decoder").set("aggregateRoleDecoder");
        }
        addDomain.get("default-realm").set(getSecurityRealmName());
        addDomain.get("realms").get(0).get("realm").set(getSecurityRealmName());
        addDomain.get("realms").get(0).get("role-decoder").set("groups-to-roles"); // use attribute "groups" as roles (defined in standalone-elytron.xml)
        addDomain.get("realms").get(1).get("realm").set("local");
        steps.add(addDomain);

        applyUpdate(managementClient.getControllerClient(), compositeOp, false);
    }

    @Override
    public void tearDown(final ManagementClient managementClient, final String containerId) {
        applyRemoveAllowReload(managementClient.getControllerClient(), domainAddress, false);
        applyRemoveAllowReload(managementClient.getControllerClient(), realmAddress, false);
        if (! permissionMapperName.equals(DEFAULT_PERMISSION_MAPPER_NAME)) {
            applyRemoveAllowReload(managementClient.getControllerClient(), permissionMapperAddress, false);
            applyRemoveAllowReload(managementClient.getControllerClient(), aggregateRoleDecoderAddress, false);
            applyRemoveAllowReload(managementClient.getControllerClient(), roleDecoder1Address, false);
            applyRemoveAllowReload(managementClient.getControllerClient(), roleDecoder2Address, false);
        }
    }

}

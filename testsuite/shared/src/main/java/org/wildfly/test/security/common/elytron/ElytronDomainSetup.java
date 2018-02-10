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

    private PathAddress realmAddress;

    private PathAddress domainAddress;

    private final String usersFile;
    private final String groupsFile;
    private final String securityDomainName;

    public ElytronDomainSetup(final String usersFile, final String groupsFile) {
        this(usersFile, groupsFile, DEFAULT_SECURITY_DOMAIN_NAME);
    }

    public ElytronDomainSetup(final String usersFile, final String groupsFile, final String securityDomainName) {
        this.usersFile = usersFile;
        this.groupsFile = groupsFile;
        this.securityDomainName = securityDomainName;
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

        // /subsystem=elytron/security-domain=EjbDomain:add(default-realm=UsersRoles, realms=[{realm=UsersRoles}])
        ModelNode addDomain = Util.createAddOperation(domainAddress);
        addDomain.get("permission-mapper").set("default-permission-mapper"); // LoginPermission for everyone (defined in standalone-elytron.xml)
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
    }

}

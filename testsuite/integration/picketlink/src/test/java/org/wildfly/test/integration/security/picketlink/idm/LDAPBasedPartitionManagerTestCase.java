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

package org.wildfly.test.integration.security.picketlink.idm;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.picketlink.idm.PartitionManager;
import org.wildfly.extension.picketlink.common.model.ModelElement;
import org.wildfly.extension.picketlink.idm.model.AttributedTypeEnum;
import org.wildfly.test.integration.security.picketlink.idm.util.AbstractIdentityManagementServerSetupTask;
import org.wildfly.test.integration.security.picketlink.idm.util.LdapMapping;
import org.wildfly.test.integration.security.picketlink.idm.util.LdapServerSetupTask;

import javax.annotation.Resource;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.wildfly.extension.picketlink.common.model.ModelElement.COMMON_CODE;
import static org.wildfly.extension.picketlink.common.model.ModelElement.COMMON_SUPPORTS_ALL;
import static org.wildfly.extension.picketlink.common.model.ModelElement.LDAP_STORE;
import static org.wildfly.extension.picketlink.common.model.ModelElement.SUPPORTED_TYPE;

/**
 * @author Pedro Igor
 */
@RunWith(Arquillian.class)
@ServerSetup({LdapServerSetupTask.class, LDAPBasedPartitionManagerTestCase.IdentityManagementServerSetupTask.class})
@Ignore
public class LDAPBasedPartitionManagerTestCase extends AbstractBasicIdentityManagementTestCase {

    static final String PARTITION_MANAGER_JNDI_NAME = "picketlink/LDAPBasedPartitionManager";

    @Deployment
    public static WebArchive createDeployment() {
        return ShrinkWrap
               .create(WebArchive.class, "test.war")
               .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
               .addAsManifestResource(new StringAsset("Dependencies: org.picketlink.idm.api meta-inf,org.jboss.dmr meta-inf,org.jboss.as.controller\n"), "MANIFEST.MF")
               .addClass(LDAPBasedPartitionManagerTestCase.class)
               .addClass(LdapServerSetupTask.class)
               .addClass(AbstractBasicIdentityManagementTestCase.class)
               .addClass(AbstractIdentityManagementServerSetupTask.class);
    }

    @Resource(mappedName = PARTITION_MANAGER_JNDI_NAME)
    private PartitionManager partitionManager;

    @Override
    @Test
    public void testPartitionManagement() throws Exception {
        // ldap store does not support partition management
    }

    @Override
    protected PartitionManager getPartitionManager() {
        return this.partitionManager;
    }

    static class IdentityManagementServerSetupTask extends AbstractIdentityManagementServerSetupTask {

        public IdentityManagementServerSetupTask() {
            super("ldap.idm", PARTITION_MANAGER_JNDI_NAME);
        }

        @Override
        protected void doCreateIdentityManagement(ModelNode identityManagementAddOperation, ModelNode operationSteps) {
            ModelNode operationAddIdentityConfiguration = Util.createAddOperation(createIdentityConfigurationPathAddress("ldap.store"));

            operationSteps.add(operationAddIdentityConfiguration);

            ModelNode operationAddIdentityStore = createIdentityStoreAddOperation(operationAddIdentityConfiguration);

            operationSteps.add(operationAddIdentityStore);

            createSupportedTypesAddOperation(operationSteps, operationAddIdentityStore);

            LdapMapping agentMapping = new LdapMapping(AttributedTypeEnum.AGENT.getAlias(), "ou=Agent,dc=jboss,dc=org", "account");

            agentMapping.addAttribute("loginName", "uid", true, false);
            agentMapping.addAttribute("createdDate", "createTimeStamp", false, true);

            operationSteps.add(agentMapping.createAddOperation(operationAddIdentityStore));

            LdapMapping userMapping = new LdapMapping(AttributedTypeEnum.USER.getAlias(), "ou=People,dc=jboss,dc=org", "inetOrgPerson, organizationalPerson");

            userMapping.addAttribute("loginName", "uid", true, false);
            userMapping.addAttribute("firstName", "cn", false, false);
            userMapping.addAttribute("lastName", "sn", false, false);
            userMapping.addAttribute("email", "mail", false, false);
            userMapping.addAttribute("createdDate", "createTimeStamp", false, true);

            operationSteps.add(userMapping.createAddOperation(operationAddIdentityStore));

            LdapMapping roleMapping = new LdapMapping(AttributedTypeEnum.ROLE.getAlias(), "ou=Roles,dc=jboss,dc=org", "groupOfNames");

            roleMapping.addAttribute("name", "cn", true, false);
            roleMapping.addAttribute("createdDate", "createTimeStamp", false, true);

            operationSteps.add(roleMapping.createAddOperation(operationAddIdentityStore));

            LdapMapping groupMapping = new LdapMapping(AttributedTypeEnum.GROUP.getAlias(), "ou=Groups,dc=jboss,dc=org", "groupOfNames");

            groupMapping.addAttribute("name", "cn", true, false);
            groupMapping.addAttribute("createdDate", "createTimeStamp", false, true);

            operationSteps.add(groupMapping.createAddOperation(operationAddIdentityStore));

            LdapMapping grantMapping = new LdapMapping(AttributedTypeEnum.GRANT.getAlias(), AttributedTypeEnum.ROLE.getAlias());

            grantMapping.addAttribute("assignee", "member", true, true);

            operationSteps.add(grantMapping.createAddOperation(operationAddIdentityStore));
        }

        private void createSupportedTypesAddOperation(ModelNode operationSteps, ModelNode operationAddIdentityStore) {
            ModelNode operationAddSupportedTypes = createSupportedAllTypesAddOperation(operationAddIdentityStore);

            operationAddSupportedTypes.get(COMMON_SUPPORTS_ALL.getName()).set(false);

            operationSteps.add(operationAddSupportedTypes);

            ModelNode identityTypeAddOperation = Util.createAddOperation(PathAddress.pathAddress(operationAddSupportedTypes.get(OP_ADDR))
                                                             .append(SUPPORTED_TYPE.getName(), AttributedTypeEnum.IDENTITY_TYPE.getAlias()));

            identityTypeAddOperation.get(COMMON_CODE.getName()).set(AttributedTypeEnum.IDENTITY_TYPE.getAlias());

            operationSteps.add(identityTypeAddOperation);

            ModelNode relationshipAddOperation = Util.createAddOperation(PathAddress.pathAddress(operationAddSupportedTypes.get(OP_ADDR))
                                                             .append(SUPPORTED_TYPE.getName(), AttributedTypeEnum.RELATIONSHIP.getAlias()));

            relationshipAddOperation.get(COMMON_CODE.getName()).set(AttributedTypeEnum.RELATIONSHIP.getAlias());

            operationSteps.add(relationshipAddOperation);
        }

        private ModelNode createIdentityStoreAddOperation(ModelNode identityConfigurationModelNode) {
            PathAddress pathAddress = PathAddress.pathAddress(identityConfigurationModelNode.get(OP_ADDR))
                                      .append(LDAP_STORE.getName(), LDAP_STORE.getName());
            ModelNode identityStore = Util.createAddOperation(pathAddress);

            identityStore.get(ModelElement.COMMON_URL.getName()).set("ldap://localhost:10389");
            identityStore.get(ModelElement.LDAP_STORE_BIND_DN.getName()).set("uid=admin,ou=system");
            identityStore.get(ModelElement.LDAP_STORE_BIND_CREDENTIAL.getName()).set("secret");
            identityStore.get(ModelElement.LDAP_STORE_BASE_DN_SUFFIX.getName()).set("dc=jboss,dc=org");

            return identityStore;
        }
    }
}

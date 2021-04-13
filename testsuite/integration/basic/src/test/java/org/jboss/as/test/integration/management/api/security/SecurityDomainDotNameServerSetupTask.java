/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.management.api.security;

import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.test.integration.security.common.CoreUtils;
import org.jboss.dmr.ModelNode;
import org.jboss.as.test.shared.SnapshotRestoreSetupTask;
import org.jboss.as.clustering.controller.Operations;

import java.util.ArrayList;
import java.util.List;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ALLOW_RESOURCE_SERVICE_RESTART;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ROLLBACK_ON_RUNTIME_FAILURE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.test.integration.management.util.ModelUtil.createOpNode;

/**
 * Server setup task for test SecurityDomainDotNameTestCase.
 * Adds and removes security domain with a name containing dots.
 *
 */
public class SecurityDomainDotNameServerSetupTask extends SnapshotRestoreSetupTask {

    private static final String DEFAULT_SECURITY_DOMAIN = "default-security-domain";
    private static final String SUBSYSTEM_NAME = "undertow";

    @Override
    public void doSetup(ManagementClient managementClient, String containerId) throws Exception {
        List<ModelNode> operations = new ArrayList<>();

        final PathAddress undertowSubsystemPathAddress = PathAddress.pathAddress(PathElement.pathElement(SUBSYSTEM, SUBSYSTEM_NAME));
        // remove the default security domain from the undertow subsystem
        final ModelNode disableDefaultSecurityDomain = new ModelNode();
        disableDefaultSecurityDomain.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.UNDEFINE_ATTRIBUTE_OPERATION);
        disableDefaultSecurityDomain.get(NAME).set(DEFAULT_SECURITY_DOMAIN);
        disableDefaultSecurityDomain.get(OP_ADDR).set(undertowSubsystemPathAddress.toModelNode());
        operations.add(disableDefaultSecurityDomain);

        // add infinispan cache
        // /subsystem=infinispan/cache-container=security:add()
        // /subsystem=infinispan/cache-container=security:write-attribute(name=default-cache, value=auth-cache)
        ModelNode addInfinispanCache = createOpNode("subsystem=infinispan/cache-container=security", ADD);
        addInfinispanCache.get("default-cache").set("auth-cache");
        operations.add(addInfinispanCache);

        // /subsystem=infinispan/cache-container=security/local-cache=auth-cache:add()
        ModelNode addLocalCache = createOpNode("subsystem=infinispan/cache-container=security/local-cache=auth-cache", ADD);
        operations.add(addLocalCache);

        // /subsystem=infinispan/cache-container=security/local-cache=auth-cache/component=expiration:write-attribute(name=lifespan, value=300000)
        ModelNode addExpirationComponent = createOpNode("subsystem=infinispan/cache-container=security/local-cache=auth-cache/component=expiration", ADD);
        addExpirationComponent.get("lifespan").set("300000");
        operations.add(addExpirationComponent);

        // /subsystem=infinispan/cache-container=security/local-cache=auth-cache/component=transaction:write-attribute(name=mode, value=BATCH)
        ModelNode addTransactionComponent = createOpNode("subsystem=infinispan/cache-container=security/local-cache=auth-cache/component=transaction", ADD);
        addTransactionComponent.get("mode").set("BATCH");
        operations.add(addTransactionComponent);

        // add security domain
        // /subsystem=security/security-domain=test.xyz.security-domain.default:add(cache-type=infinispan)
        ModelNode addSecurityDomain = createOpNode("subsystem=security/security-domain=test.xyz.security-domain.default", ADD);
        addSecurityDomain.get("cache-type").set("infinispan");
        operations.add(addSecurityDomain);

        ModelNode updateOp = Operations.createCompositeOperation(operations);
        updateOp.get(OPERATION_HEADERS, ROLLBACK_ON_RUNTIME_FAILURE).set(false);
        updateOp.get(OPERATION_HEADERS, ALLOW_RESOURCE_SERVICE_RESTART).set(true);
        CoreUtils.applyUpdate(updateOp, managementClient.getControllerClient());
    }
}

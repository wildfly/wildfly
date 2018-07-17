/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat Middleware LLC, and individual contributors
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

package org.jboss.as.test.integration.web.security.bufferFreed;

import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.clustering.controller.Operations;
import org.jboss.as.controller.client.helpers.ClientConstants;
import org.jboss.as.test.integration.management.util.ServerReload;
import org.jboss.as.test.integration.security.common.CoreUtils;
import org.jboss.as.test.shared.SnapshotRestoreSetupTask;
import org.jboss.dmr.ModelNode;

import java.util.ArrayList;
import java.util.List;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.*;
import static org.jboss.as.test.integration.management.util.ModelUtil.createOpNode;


/**
 * Server setup task for test BufferFreedTestCase. Configures SSL security.
 *
 * @author Daniel Cihak
 */
public class BufferFreedServerSetupTask extends SnapshotRestoreSetupTask {

    public static final String KEYSTORE_PASSWORD = "123$qweR";
    private static final String ALIAS = "mykey";

    @Override
    public void doSetup(ManagementClient managementClient, String s) throws Exception {
        List<ModelNode> operations = new ArrayList<>();

        // core-service=management
        // core-service=management/security-realm=ApplicationRealm/server-identity=ssl:remove
        ModelNode removeKeystore = createOpNode("core-service=management/security-realm=ApplicationRealm/server-identity=ssl", REMOVE);
        operations.add(removeKeystore);
        // core-service=management/security-realm=HTTPSRealm:add
        ModelNode addHTTPSRealm = createOpNode("core-service=management/security-realm=HTTPSRealm", ADD);
        operations.add(addHTTPSRealm);
        // core-service=management/security-realm=HTTPSRealm/server-identity=ssl:add(path=identity.jks, relative-to=jboss.server.config.dir, keystore-password=123$qweR, alias=mykey)
        ModelNode addSsl = createOpNode("core-service=management/security-realm=HTTPSRealm/server-identity=ssl", ADD);
        addSsl.get("keystore-path").set(BufferFreedTestCase.getTrustStoreFile().getAbsolutePath());
        addSsl.get("keystore-password").set(KEYSTORE_PASSWORD);
        addSsl.get("alias").set(ALIAS);
        operations.add(addSsl);
        // core-service=management/access=audit/logger=audit-log:write-attribute(name=enabled, value=true)
        ModelNode updateAuditLog = createOpNode("core-service=management/access=audit/logger=audit-log", WRITE_ATTRIBUTE_OPERATION);
        updateAuditLog.get(ClientConstants.NAME).set("enabled");
        updateAuditLog.get(ClientConstants.VALUE).set(true);
        operations.add(updateAuditLog);

        // ejb3
        // subsystem=ejb3:undefine-attribute(name=default-resource-adapter-name)
        ModelNode undefineAttrMdb1 = createOpNode("subsystem=ejb3", UNDEFINE_ATTRIBUTE_OPERATION);
        undefineAttrMdb1.get(ClientConstants.NAME).set("default-resource-adapter-name");
        operations.add(undefineAttrMdb1);
        // subsystem=ejb3:undefine-attribute(name=default-mdb-instance-pool)
        ModelNode undefineAttrMdb2 = createOpNode("subsystem=ejb3", UNDEFINE_ATTRIBUTE_OPERATION);
        undefineAttrMdb2.get(ClientConstants.NAME).set("default-mdb-instance-pool");
        operations.add(undefineAttrMdb2);
        // subsystem=ejb3/service=remote:remove()
        ModelNode removeRemoteService = new ModelNode();
        removeRemoteService.get(OP).set(REMOVE);
        removeRemoteService.get(OP_ADDR).add(SUBSYSTEM, "ejb3");
        removeRemoteService.get(OP_ADDR).add("service", "remote");
        CoreUtils.applyUpdate(removeRemoteService, managementClient.getControllerClient());
        // subsystem=ejb3/service=remote:add(connector-ref=https-remoting-connector, thread-pool-name=default)
        ModelNode addRemoteService = createOpNode("subsystem=ejb3/service=remote", ADD);
        addRemoteService.get("connector-ref").set("https-remoting-connector");
        addRemoteService.get("thread-pool-name").set("default");
        operations.add(addRemoteService);
        // subsystem=ejb3/service=iiop:remove()
        ModelNode removeIiopService = new ModelNode();
        removeIiopService.get(OP).set(REMOVE);
        removeIiopService.get(OP_ADDR).add(SUBSYSTEM, "ejb3");
        removeIiopService.get(OP_ADDR).add("service", "iiop");
        CoreUtils.applyUpdate(removeIiopService, managementClient.getControllerClient());

        // infinispan
        // subsystem=infinispan/cache-container=web/local-cache=persistent:add
        ModelNode addWebPersistentCache = createOpNode("subsystem=infinispan/cache-container=web/local-cache=persistent", ADD);
        operations.add(addWebPersistentCache);
        // subsystem=infinispan/cache-container=web/local-cache=persistent/locking=LOCKING:add(isolation=REPEATABLE_READ)
        ModelNode addWebPersistentCacheLocking = createOpNode("subsystem=infinispan/cache-container=web/local-cache=persistent/locking=LOCKING", ADD);
        addWebPersistentCacheLocking.get("isolation").set("REPEATABLE_READ");
        operations.add(addWebPersistentCacheLocking);
        // subsystem=infinispan/cache-container=web/local-cache=persistent/transaction=TRANSACTION:add(mode=BATCH)
        ModelNode addWebPersistentCacheTransaction = createOpNode("subsystem=infinispan/cache-container=web/local-cache=persistent/transaction=TRANSACTION", ADD);
        addWebPersistentCacheTransaction.get("mode").set("BATCH");
        operations.add(addWebPersistentCacheTransaction);
        // subsystem=infinispan/cache-container=web/distributed-cache=dist/file-store=FILE_STORE:add(passivation=false, purge=false)
        ModelNode addWebPersistentCacheFileStore = createOpNode("subsystem=infinispan/cache-container=web/local-cache=persistent/file-store=FILE_STORE", ADD);
        addWebPersistentCacheFileStore.get("passivation").set(false);
        addWebPersistentCacheFileStore.get("purge").set(false);
        operations.add(addWebPersistentCacheFileStore);
        // subsystem=infinispan/cache-container=web/local-cache=concurrent:add
        ModelNode addWebConcurrentCache = createOpNode("subsystem=infinispan/cache-container=web/local-cache=concurrent", ADD);
        operations.add(addWebConcurrentCache);
        // subsystem=infinispan/cache-container=web/local-cache=concurrent/file-store=FILE_STORE:add(passivation=true, purge=false)
        ModelNode addWebConcurrentCacheFileStore = createOpNode("subsystem=infinispan/cache-container=web/local-cache=concurrent/file-store=FILE_STORE", ADD);
        addWebConcurrentCacheFileStore.get("passivation").set(true);
        addWebConcurrentCacheFileStore.get("purge").set(false);
        operations.add(addWebConcurrentCacheFileStore);
        // subsystem=infinispan/cache-container=ejb/local-cache=persistent:add
        ModelNode addEjbPersistentCache = createOpNode("subsystem=infinispan/cache-container=ejb/local-cache=persistent", ADD);
        operations.add(addEjbPersistentCache);
        // subsystem=infinispan/cache-container=ejb/local-cache=persistent/locking=LOCKING:add(isolation=REPEATABLE_READ)
        ModelNode addEjbPersistentCacheLocking = createOpNode("subsystem=infinispan/cache-container=ejb/local-cache=persistent/locking=LOCKING", ADD);
        addEjbPersistentCacheLocking.get("isolation").set("REPEATABLE_READ");
        operations.add(addEjbPersistentCacheLocking);
        // subsystem=infinispan/cache-container=ejb/local-cache=persistent/transaction=TRANSACTION:add(mode=BATCH)
        ModelNode addEjbPersistentCacheTransaction = createOpNode("subsystem=infinispan/cache-container=ejb/local-cache=persistent/transaction=TRANSACTION", ADD);
        addEjbPersistentCacheTransaction.get("mode").set("BATCH");
        operations.add(addEjbPersistentCacheTransaction);
        // subsystem=infinispan/cache-container=ejb/local-cache=persistent/file-store=FILE_STORE:add(passivation=false, purge=false)
        ModelNode addEjbPersistentCacheFileStore = createOpNode("subsystem=infinispan/cache-container=ejb/local-cache=persistent/file-store=FILE_STORE", ADD);
        addEjbPersistentCacheFileStore.get("passivation").set(false);
        addEjbPersistentCacheFileStore.get("purge").set(false);
        operations.add(addEjbPersistentCacheFileStore);
        // subsystem=infinispan/cache-container=hibernate:write-attribute(name=default-cache, value=local-query)
        ModelNode updateHibernateCacheContainer = createOpNode("subsystem=infinispan/cache-container=hibernate", WRITE_ATTRIBUTE_OPERATION);
        updateHibernateCacheContainer.get(ClientConstants.NAME).set("default-cache");
        updateHibernateCacheContainer.get(ClientConstants.VALUE).set("local-query");
        operations.add(updateHibernateCacheContainer);

        // remoting
        ModelNode addRemotingConnector = createOpNode("subsystem=remoting/http-connector=https-remoting-connector", ADD);
        addRemotingConnector.get("connector-ref").set("https");
        addRemotingConnector.get("security-realm").set("ApplicationRealm");
        operations.add(addRemotingConnector);
        ModelNode addSslEnabledProperty = createOpNode("subsystem=remoting/http-connector=https-remoting-connector/property=SSL_ENABLED", ADD);
        addSslEnabledProperty.get("value").set(true);
        operations.add(addSslEnabledProperty);
        ModelNode addSslStartTlsProperty = createOpNode("subsystem=remoting/http-connector=https-remoting-connector/property=SSL_STARTTLS", ADD);
        addSslStartTlsProperty.get("value").set(false);
        operations.add(addSslStartTlsProperty);

        // undertow
        // subsystem=undertow/server=default-server/http-listener=default:undefine-attribute(name=enable-http2)
        ModelNode undefineAttrEnableHttp2_1 = createOpNode("subsystem=undertow/server=default-server/http-listener=default", UNDEFINE_ATTRIBUTE_OPERATION);
        undefineAttrEnableHttp2_1.get(ClientConstants.NAME).set("enable-http2");
        operations.add(undefineAttrEnableHttp2_1);
        // subsystem=undertow/server=default-server/https-listener=https:undefine-attribute(name=enable-http2)
        ModelNode undefineAttrEnableHttp2 = createOpNode("subsystem=undertow/server=default-server/https-listener=https", UNDEFINE_ATTRIBUTE_OPERATION);
        undefineAttrEnableHttp2.get(ClientConstants.NAME).set("enable-http2");
        operations.add(undefineAttrEnableHttp2);
        // subsystem=undertow/server=default-server/https-listener=https:write-attribute(name=security-realm, value=HTTPSRealm)
        ModelNode updateSecurityRealm = createOpNode("subsystem=undertow/server=default-server/https-listener=https", WRITE_ATTRIBUTE_OPERATION);
        updateSecurityRealm.get(ClientConstants.NAME).set("security-realm");
        updateSecurityRealm.get(ClientConstants.VALUE).set("HTTPSRealm");
        operations.add(updateSecurityRealm);

        // socket-binding-group
        // socket-binding-group=standard-sockets/socket-binding=iiop:remove()
        ModelNode removeIiop = new ModelNode();
        removeIiop.get(OP).set(REMOVE);
        removeIiop.get(OP_ADDR).add(SOCKET_BINDING_GROUP, "standard-sockets");
        removeIiop.get(OP_ADDR).add(SOCKET_BINDING, "iiop");
        operations.add(removeIiop);
        // socket-binding-group=standard-sockets/socket-binding=iiop-ssl:remove()
        ModelNode removeIiopSsl = new ModelNode();
        removeIiopSsl.get(OP).set(REMOVE);
        removeIiopSsl.get(OP_ADDR).add(SOCKET_BINDING_GROUP, "standard-sockets");
        removeIiopSsl.get(OP_ADDR).add(SOCKET_BINDING, "iiop-ssl");
        removeIiopSsl.get(OPERATION_HEADERS).get(ALLOW_RESOURCE_SERVICE_RESTART).set(true);
        operations.add(removeIiopSsl);

        ModelNode updateOp = Operations.createCompositeOperation(operations);
        updateOp.get(OPERATION_HEADERS, ROLLBACK_ON_RUNTIME_FAILURE).set(false);
        updateOp.get(OPERATION_HEADERS, ALLOW_RESOURCE_SERVICE_RESTART).set(true);
        CoreUtils.applyUpdate(updateOp, managementClient.getControllerClient());

        // subsystem=core-management:remove()
        ModelNode removeCoreManagementOp = new ModelNode();
        removeCoreManagementOp.get(OP).set(REMOVE);
        removeCoreManagementOp.get(OP_ADDR).add(SUBSYSTEM, "core-management");
        CoreUtils.applyUpdate(removeCoreManagementOp, managementClient.getControllerClient());

        // subsystem=elytron:remove()
        ModelNode removeElytronOp = new ModelNode();
        removeElytronOp.get(OP).set(REMOVE);
        removeElytronOp.get(OP_ADDR).add(SUBSYSTEM, "elytron");
        CoreUtils.applyUpdate(removeElytronOp, managementClient.getControllerClient());

        // subsystem=iiop-openjdk:remove()
        ModelNode removeIiopOp = new ModelNode();
        removeIiopOp.get(OP).set(REMOVE);
        removeIiopOp.get(OP_ADDR).add(SUBSYSTEM, "iiop-openjdk");
        CoreUtils.applyUpdate(removeIiopOp, managementClient.getControllerClient());

        // subsystem=jsr77:remove()
        ModelNode removeJsrOp = new ModelNode();
        removeJsrOp.get(OP).set(REMOVE);
        removeJsrOp.get(OP_ADDR).add(SUBSYSTEM, "jsr77");
        CoreUtils.applyUpdate(removeJsrOp, managementClient.getControllerClient());

        // subsystem=messaging-activemq:remove()
        ModelNode removeMessagingOp = new ModelNode();
        removeMessagingOp.get(OP).set(REMOVE);
        removeMessagingOp.get(OP_ADDR).add(SUBSYSTEM, "messaging-activemq");
        CoreUtils.applyUpdate(removeMessagingOp, managementClient.getControllerClient());

        // subsystem=ee-security:remove()
        ModelNode removeEeSecurityOp = new ModelNode();
        removeEeSecurityOp.get(OP).set(REMOVE);
        removeEeSecurityOp.get(OP_ADDR).add(SUBSYSTEM, "ee-security");
        CoreUtils.applyUpdate(removeEeSecurityOp, managementClient.getControllerClient());

        // subsystem=discovery:remove()
        ModelNode removeDiscoveryOp = new ModelNode();
        removeDiscoveryOp.get(OP).set(REMOVE);
        removeDiscoveryOp.get(OP_ADDR).add(SUBSYSTEM, "discovery");
        CoreUtils.applyUpdate(removeDiscoveryOp, managementClient.getControllerClient());

        ServerReload.executeReloadAndWaitForCompletion(managementClient.getControllerClient());
    }
}

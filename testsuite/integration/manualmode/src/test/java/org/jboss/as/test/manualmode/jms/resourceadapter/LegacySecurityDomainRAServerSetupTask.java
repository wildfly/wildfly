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

package org.jboss.as.test.manualmode.jms.resourceadapter;

import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.clustering.controller.Operations;
import org.jboss.as.test.integration.security.common.CoreUtils;
import org.jboss.as.test.shared.SnapshotRestoreSetupTask;
import org.jboss.dmr.ModelNode;

import java.util.ArrayList;
import java.util.List;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ALLOW_RESOURCE_SERVICE_RESTART;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ROLLBACK_ON_RUNTIME_FAILURE;
import static org.jboss.as.test.integration.management.util.ModelUtil.createOpNode;

/**
 *
 * Server setup task for test LegacySecurityDomainRATestCase.
 * Configures legacy security domain and resource adapter.
 *
 * @author Daniel Cihak
 */
public class LegacySecurityDomainRAServerSetupTask extends SnapshotRestoreSetupTask {

    @Override
    public void doSetup(ManagementClient managementClient, String s) throws Exception {
        List<ModelNode> operations = new ArrayList<>();

        // /subsystem=resource-adapters/resource-adapter=generic-jms-ra:add(module=org.jboss.genericjms,transaction-support=LocalTransaction)
        ModelNode addResourceAdapter = createOpNode("subsystem=resource-adapters/resource-adapter=generic-jms-ra", ADD);
        addResourceAdapter.get("module").set("org.jboss.genericjms");
        addResourceAdapter.get("transaction-support").set("LocalTransaction");
        operations.add(addResourceAdapter);

        // //subsystem=resource-adapters/resource-adapter=generic-jms-ra/connection-definitions=GenericJmsXA:add(class-name=org.jboss.resource.adapter.jms.JmsManagedConnectionFactory,jndi-name=java:/jms/TibcoEmsLocalTxFactory,enabled=true,use-ccm=true,min-pool-size=0,max-pool-size=20,pool-prefill=false,pool-use-strict-min=false,flush-strategy=FailingConnectionOnly,security-domain-and-application=TibcoEmsRealm)
        ModelNode addConnectionDefinitions = createOpNode("subsystem=resource-adapters/resource-adapter=generic-jms-ra/connection-definitions=GenericJmsXA", ADD);
        addConnectionDefinitions.get("class-name").set("org.jboss.resource.adapter.jms.JmsManagedConnectionFactory");
        addConnectionDefinitions.get("jndi-name").set("java:/jms/TibcoEmsLocalTxFactory");
        addConnectionDefinitions.get("enabled").set(true);
        addConnectionDefinitions.get("use-ccm").set(true);
        addConnectionDefinitions.get("min-pool-size").set(0);
        addConnectionDefinitions.get("max-pool-size").set(20);
        addConnectionDefinitions.get("pool-prefill").set(false);
        addConnectionDefinitions.get("pool-use-strict-min").set(false);
        addConnectionDefinitions.get("flush-strategy").set("FailingConnectionOnly");
        addConnectionDefinitions.get("security-domain-and-application").set("TibcoEmsRealm");
        operations.add(addConnectionDefinitions);

        // /subsystem=resource-adapters/resource-adapter=generic-jms-ra/connection-definitions=GenericJmsXA/config-properties=ConnectionFactory:add(value=QueueConnectionFactory)
        ModelNode addConfigProperty1 = createOpNode("subsystem=resource-adapters/resource-adapter=generic-jms-ra/connection-definitions=GenericJmsXA/config-properties=ConnectionFactory", ADD);
        addConfigProperty1.get("value").set("QueueConnectionFactory");
        operations.add(addConfigProperty1);

        // /subsystem=resource-adapters/resource-adapter=generic-jms-ra/connection-definitions=GenericJmsXA/config-properties=SessionDefaultType:add(value=javax.jms.Queue)
        ModelNode addConfigProperty2 = createOpNode("subsystem=resource-adapters/resource-adapter=generic-jms-ra/connection-definitions=GenericJmsXA/config-properties=SessionDefaultType", ADD);
        addConfigProperty2.get("value").set("javax.jms.Queue");
        operations.add(addConfigProperty2);

        // /subsystem=resource-adapters/resource-adapter=generic-jms-ra/connection-definitions=GenericJmsXA/config-properties=JndiParameters:add(value=java.naming.factory.initial=com.tibco.tibjms.naming.TibjmsInitialContextFactory;java.naming.provider.url=tcp:/xxxxx)
        ModelNode addConfigProperty3 = createOpNode("subsystem=resource-adapters/resource-adapter=generic-jms-ra/connection-definitions=GenericJmsXA/config-properties=JndiParameters", ADD);
        addConfigProperty3.get("value").set("java.naming.factory.initial=com.tibco.tibjms.naming.TibjmsInitialContextFactory;java.naming.provider.url=tcp:/xxxxx");
        operations.add(addConfigProperty3);

        ModelNode updateOp = Operations.createCompositeOperation(operations);
        updateOp.get(OPERATION_HEADERS, ROLLBACK_ON_RUNTIME_FAILURE).set(false);
        updateOp.get(OPERATION_HEADERS, ALLOW_RESOURCE_SERVICE_RESTART).set(true);
        CoreUtils.applyUpdate(updateOp, managementClient.getControllerClient());
    }
}

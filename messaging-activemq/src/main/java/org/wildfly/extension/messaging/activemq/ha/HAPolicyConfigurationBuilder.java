/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.messaging.activemq.ha;


import static org.wildfly.extension.messaging.activemq.CommonAttributes.HA_POLICY;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.LIVE_ONLY;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.REPLICATION_COLOCATED;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.REPLICATION_MASTER;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.REPLICATION_SLAVE;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.SHARED_STORE_COLOCATED;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.SHARED_STORE_MASTER;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.SHARED_STORE_SLAVE;

import org.apache.activemq.artemis.core.config.Configuration;
import org.apache.activemq.artemis.core.config.HAPolicyConfiguration;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2014 Red Hat inc.
 */
public class HAPolicyConfigurationBuilder {

    public static void addHAPolicyConfiguration(OperationContext context, Configuration configuration, ModelNode model) throws OperationFailedException {

        if (!model.hasDefined(HA_POLICY)) {
            return;
        }
        Property prop = model.get(HA_POLICY).asProperty();
        ModelNode haPolicy = prop.getValue();

        final HAPolicyConfiguration haPolicyConfiguration;
        String type = prop.getName();
        switch (type) {
            case LIVE_ONLY: {
                haPolicyConfiguration = LiveOnlyDefinition.buildConfiguration(context, haPolicy);
                break;
            }
            case REPLICATION_MASTER: {
                haPolicyConfiguration = ReplicationMasterDefinition.buildConfiguration(context, haPolicy);
                break;
            }
            case REPLICATION_SLAVE: {
                haPolicyConfiguration = ReplicationSlaveDefinition.buildConfiguration(context, haPolicy);
                break;
            }
            case REPLICATION_COLOCATED: {
                haPolicyConfiguration = ReplicationColocatedDefinition.buildConfiguration(context, haPolicy);
                break;
            }
            case SHARED_STORE_MASTER: {
                haPolicyConfiguration = SharedStoreMasterDefinition.buildConfiguration(context, haPolicy);
                break;
            }
            case SHARED_STORE_SLAVE: {
                haPolicyConfiguration = SharedStoreSlaveDefinition.buildConfiguration(context, haPolicy);
                break;
            }
            case SHARED_STORE_COLOCATED: {
                haPolicyConfiguration = SharedStoreColocatedDefinition.buildConfiguration(context, haPolicy);
                break;
            }
            default: {
                throw new OperationFailedException("unknown ha policy type");
            }
        }
        configuration.setHAPolicyConfiguration(haPolicyConfiguration);
    }

}

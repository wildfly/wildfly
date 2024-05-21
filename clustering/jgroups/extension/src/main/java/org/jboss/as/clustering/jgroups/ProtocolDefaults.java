/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.jgroups;

import java.util.Map;

import org.jboss.msc.service.ServiceName;
import org.jgroups.stack.Protocol;

/**
 * @author Paul Ferraro
 */
public interface ProtocolDefaults {
    ServiceName SERVICE_NAME = ServiceName.JBOSS.append("jgroups", "defaults");

    Map<String, String> getProperties(Class<? extends Protocol> protocolClass);
}

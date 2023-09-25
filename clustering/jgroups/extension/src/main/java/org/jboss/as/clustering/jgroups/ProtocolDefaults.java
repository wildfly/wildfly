/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.jgroups;

import java.util.Map;

import org.jgroups.stack.Protocol;

/**
 * @author Paul Ferraro
 */
public interface ProtocolDefaults {
    Map<String, String> getProperties(Class<? extends Protocol> protocolClass);
}

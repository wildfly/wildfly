/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.server.infinispan.dispatcher;

/**
 * Simple object that indicates that no service is registered on the remote node for which to execute the remote command.
 * @author Paul Ferraro
 */
public enum NoSuchService {
    INSTANCE;
}
/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.jgroups.spi;

import java.util.List;

import org.jgroups.stack.Protocol;

/**
 * @author Paul Ferraro
 */
public interface StackConfiguration {

    List<ProtocolConfiguration<? extends Protocol>> getProtocols();
}

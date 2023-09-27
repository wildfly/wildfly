/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.web;

/**
 * Create a local context.
 * The local context is a mutable object that will *not* replicate to other nodes.
 * @author Paul Ferraro
 */
public interface LocalContextFactory<L> {
    L createLocalContext();
}

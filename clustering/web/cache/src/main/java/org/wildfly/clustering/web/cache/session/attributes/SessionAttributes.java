/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.cache.session.attributes;

/**
 * @author Paul Ferraro
 */
public interface SessionAttributes extends org.wildfly.clustering.web.session.SessionAttributes, AutoCloseable {
    /**
     * Signals the end of the transient lifecycle of this session, typically triggered at the end of a given request.
     */
    @Override
    void close();
}

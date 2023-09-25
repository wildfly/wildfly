/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.web.session;

/**
 * Locates the affinity most appropriate for a provided session identifier.
 *
 * @author Radoslav Husar
 */
public interface AffinityLocator {

    /**
     * Locates the affinity most appropriate for a provided session identifier.
     *
     * @param sessionID a unique session identifier to be located
     * @return affinity of the corresponding instance
     */
    String locate(String sessionID);

}

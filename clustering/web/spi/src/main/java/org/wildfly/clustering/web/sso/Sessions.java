/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.web.sso;

import java.util.Set;

/**
 * Represents the sessions per deployment for which a given user is authenticated.
 * @author Paul Ferraro
 * @param <D> deployment identifier type
 * @param <S> session identifier type
 */
public interface Sessions<D, S> {
    /**
     * Returns the set of web applications for which the current user is authenticated.
     * @return a set of web applications.
     */
    Set<D> getDeployments();

    /**
     * Returns the corresponding session identifier for the specified web application.
     * @param application
     * @return
     */
    S getSession(D deployment);

    /**
     * Removes the specified web application from the set of authenticated web applications.
     * @param application
     */
    S removeSession(D deployment);

    /**
     * Adds the specified web application and session identifier to the registry of authenticated web applications.
     * @param deployment a web application
     * @param session a session
     * @return true, if the session was added, false it already exists
     */
    boolean addSession(D deployment, S session);
}

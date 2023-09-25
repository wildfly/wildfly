/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.server.service;

import org.jboss.as.controller.descriptions.ModelDescriptionConstants;

/**
 * @author Paul Ferraro
 */
public interface LocalGroupServiceConfiguratorProvider extends GroupServiceConfiguratorProvider {
    /**
     * Identifies the name of the local group.
     */
    String LOCAL = ModelDescriptionConstants.LOCAL;
}

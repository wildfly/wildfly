/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.timer;

import org.wildfly.clustering.ejb.DeploymentConfiguration;

/**
 * Encapsulates the configuration of a timer service.
 * @author Paul Ferraro
 */
public interface TimerServiceConfiguration extends DeploymentConfiguration {

    /**
     * The name of the component containing the timer service.
     * @return a component name
     */
    String getName();
}

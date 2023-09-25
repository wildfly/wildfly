/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.bean;

import org.wildfly.clustering.ejb.DeploymentConfiguration;

/**
 * Specifies the configuration of an EJB component.
 * @author Paul Ferraro
 */
public interface BeanConfiguration extends DeploymentConfiguration {

    /**
     * Returns the name of the EJB component.
     * @return the component name
     */
    String getName();
}

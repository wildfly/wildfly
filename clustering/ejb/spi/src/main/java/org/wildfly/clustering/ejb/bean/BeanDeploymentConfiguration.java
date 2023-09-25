/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.bean;

import org.wildfly.clustering.ejb.DeploymentConfiguration;

/**
 * Encapsulates configuration of a bean deployment.
 * @author Paul Ferraro
 */
public interface BeanDeploymentConfiguration extends DeploymentConfiguration, BeanDeploymentMarshallingContext {
}

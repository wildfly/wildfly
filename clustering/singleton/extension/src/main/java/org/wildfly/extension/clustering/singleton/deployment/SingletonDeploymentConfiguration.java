/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.singleton.deployment;


/**
 * Configuration of a singleton deployment.
 * @author Paul Ferraro
 */
public interface SingletonDeploymentConfiguration {

    String getPolicy();
}

/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.service.session;

import org.wildfly.clustering.web.service.deployment.WebDeploymentConfiguration;

/**
 * Encapsulates the configuration of a session manager factory.
 * @author Paul Ferraro
 */
public interface SessionManagerFactoryConfiguration<C> extends org.wildfly.clustering.session.SessionManagerFactoryConfiguration<C>, WebDeploymentConfiguration {

}

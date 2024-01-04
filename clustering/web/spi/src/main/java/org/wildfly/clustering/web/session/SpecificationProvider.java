/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.session;

/**
 * Provides servlet specification behavior for a container-agnostic distributed session manager.
 * @author Paul Ferraro
 */
public interface SpecificationProvider<S, C, AL> extends HttpSessionActivationListenerProvider<S, C, AL> {

}

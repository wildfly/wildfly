/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.cluster.ejb.stateful.bean;

import jakarta.ejb.Local;

/**
 * @author Paul Ferraro
 */
@Local
public interface ScopedIncrementor extends Incrementor {

}

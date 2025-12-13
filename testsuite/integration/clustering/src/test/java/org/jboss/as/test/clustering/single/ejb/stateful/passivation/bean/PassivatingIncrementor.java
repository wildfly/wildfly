/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.clustering.single.ejb.stateful.passivation.bean;

import org.jboss.as.test.clustering.single.ejb.stateful.bean.Incrementor;

/**
 * Remote interface for a bean that records passivation and activation events.
 *
 * @author Radoslav Husar
 */
public interface PassivatingIncrementor extends Incrementor {

    /**
     * Gets the unique identifier for this bean instance.
     */
    String getIdentifier();
}

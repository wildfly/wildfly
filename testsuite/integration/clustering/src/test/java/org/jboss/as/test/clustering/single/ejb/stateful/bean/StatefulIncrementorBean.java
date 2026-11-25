/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.clustering.single.ejb.stateful.bean;

import jakarta.ejb.Remote;
import jakarta.ejb.Stateful;

@Stateful
@Remote(Incrementor.class)
public class StatefulIncrementorBean extends IncrementorBean {
}

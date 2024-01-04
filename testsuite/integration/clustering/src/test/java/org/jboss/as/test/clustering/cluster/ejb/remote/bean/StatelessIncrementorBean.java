/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.clustering.cluster.ejb.remote.bean;

import jakarta.ejb.Remote;
import jakarta.ejb.Stateless;

/**
 * @author Paul Ferraro
 */
@Stateless
@Remote(Incrementor.class)
public class StatelessIncrementorBean extends IncrementorBean {
}

/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.cluster.ejb2.stateless.bean;

import jakarta.ejb.EJBObject;

/**
 * @author Ondrej Chaloupka
 */
public interface StatelessRemote extends EJBObject {
    String getNodeName();
}

/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.cluster.ejb2.stateful.passivation;

import jakarta.ejb.EJBObject;

/**
 * @author Ondrej Chaloupka
 */
public interface StatefulRemote extends EJBObject {
    int getNumber();

    String setNumber(int number);

    String incrementNumber();

    void setPassivationNode(String node);

    String getPassivatedBy();
}

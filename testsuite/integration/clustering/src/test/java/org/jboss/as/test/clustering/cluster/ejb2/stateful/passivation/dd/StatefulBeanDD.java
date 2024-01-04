/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.cluster.ejb2.stateful.passivation.dd;

import jakarta.ejb.SessionBean;

import org.jboss.as.test.clustering.cluster.ejb2.stateful.passivation.StatefulBeanBase;

/**
 * @author Ondrej Chaloupka
 */
public class StatefulBeanDD extends StatefulBeanBase implements SessionBean {
    private static final long serialVersionUID = 1L;
}

/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.cluster.ejb2.stateless;

import jakarta.ejb.SessionBean;

import org.jboss.as.test.clustering.cluster.ejb2.stateless.bean.StatelessBeanBase;

/**
 * @author Ondrej Chaloupka
 */
public class StatelessBeanDD extends StatelessBeanBase implements SessionBean {
    private static final long serialVersionUID = 1L;
}

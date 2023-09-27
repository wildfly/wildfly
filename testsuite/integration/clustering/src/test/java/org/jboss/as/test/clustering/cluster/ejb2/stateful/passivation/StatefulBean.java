/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.cluster.ejb2.stateful.passivation;

import jakarta.ejb.RemoteHome;
import jakarta.ejb.SessionBean;
import jakarta.ejb.Stateful;

/**
 * @author Ondrej Chaloupka
 */
@Stateful
@RemoteHome(StatefulRemoteHome.class)
public class StatefulBean extends StatefulBeanBase implements SessionBean {
    private static final long serialVersionUID = 1L;

}

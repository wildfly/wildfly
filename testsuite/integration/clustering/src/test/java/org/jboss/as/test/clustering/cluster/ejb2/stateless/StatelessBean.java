/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.cluster.ejb2.stateless;

import jakarta.ejb.Remote;
import jakarta.ejb.RemoteHome;
import jakarta.ejb.SessionBean;
import jakarta.ejb.Stateless;

import org.jboss.as.test.clustering.cluster.ejb2.stateless.bean.StatelessBeanBase;
import org.jboss.as.test.clustering.cluster.ejb2.stateless.bean.StatelessRemote;
import org.jboss.as.test.clustering.cluster.ejb2.stateless.bean.StatelessRemoteHome;

/**
 * @author Ondrej Chaloupka
 */
@Stateless
@RemoteHome(StatelessRemoteHome.class)
@Remote(StatelessRemote.class)
public class StatelessBean extends StatelessBeanBase implements SessionBean {
    private static final long serialVersionUID = 1L;
}

/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.iiop.basic;

import jakarta.ejb.RemoteHome;
import jakarta.ejb.Stateful;

/**
 * @author Stuart Douglas
 */
@RemoteHome(IIOPBasicStatefulHome.class)
@Stateful
public class IIOPBasicStatefulBean {

    private int state;

    public void ejbCreate( int state) {
        this.state = state;
    }

    public int state() {
        return state;
    }

}

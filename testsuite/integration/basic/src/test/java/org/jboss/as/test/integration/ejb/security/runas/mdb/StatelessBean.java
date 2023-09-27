/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.security.runas.mdb;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.Remote;
import jakarta.ejb.Stateless;

import org.jboss.ejb3.annotation.SecurityDomain;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
@Stateless
@Remote(MyStateless.class)
@SecurityDomain("other")
public class StatelessBean implements MyStateless {
    // static so it's shared by all
    private static String state;

    @RolesAllowed("anyone")
    public String getState() {
        return state;
    }

    @RolesAllowed("TestRole")
    public void setState(String state) {
        StatelessBean.state = state;
    }
}

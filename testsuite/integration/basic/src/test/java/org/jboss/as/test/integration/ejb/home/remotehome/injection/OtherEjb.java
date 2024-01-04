/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.home.remotehome.injection;

import jakarta.ejb.RemoteHome;
import jakarta.ejb.Stateful;

/**
 * @author Stuart Douglas
 */
@Stateful
@RemoteHome(InjectionHome.class)
public class OtherEjb {

    public void ejbCreate() {

    }

    public String message() {
        return "OtherEjb";
    }

}

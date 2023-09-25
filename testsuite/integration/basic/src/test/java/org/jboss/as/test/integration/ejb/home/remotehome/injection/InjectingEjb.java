/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.home.remotehome.injection;

import java.rmi.RemoteException;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author Stuart Douglas
 */
@Stateless
public class InjectingEjb {

    @EJB
    private InjectionHome home;

    public String getMessage() {
        try {
            return home.create().message();
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

}

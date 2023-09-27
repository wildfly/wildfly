/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.injection.multiple.view;

import jakarta.ejb.EJB;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;

/**
 * @author Jaikiran Pai
 */
@LocalBean
@Stateless
public class InjectingBean {

    @EJB
    private NoInterfaceAndWebServiceViewBean otherBean;

    public boolean isBeanInjected() {
        return this.otherBean != null;
    }

    public void invokeInjectedBean() {
        this.otherBean.doNothing();
    }
}

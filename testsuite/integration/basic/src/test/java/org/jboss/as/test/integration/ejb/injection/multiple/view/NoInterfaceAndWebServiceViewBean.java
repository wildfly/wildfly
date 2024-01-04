/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.injection.multiple.view;

import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;
import jakarta.jws.WebService;

/**
 * @author Jaikiran Pai
 */
@LocalBean
@Stateless
@WebService
public class NoInterfaceAndWebServiceViewBean {

    public void doNothing() {

    }
}

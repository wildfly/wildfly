/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.beanclass.validity;

import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;

/**
 * User: jpai
 */
@Stateless
@LocalBean
public class ProperStatelessBean {

    public void doNothing() {

    }
}

/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.transaction.annotation;

import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionManagement;

/**
 * User: jpai
 */
@TransactionManagement // don't specify an explicit value
@Stateless
public class BeanWithoutTransactionManagementValue {

    public void doNothing() {

    }
}

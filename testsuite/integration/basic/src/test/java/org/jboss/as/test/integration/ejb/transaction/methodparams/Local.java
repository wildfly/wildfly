/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.transaction.methodparams;

import jakarta.ejb.EJBLocalObject;

public interface Local extends EJBLocalObject {

    boolean test(String[] s);

    boolean test(String s);

    boolean test(int x);

}

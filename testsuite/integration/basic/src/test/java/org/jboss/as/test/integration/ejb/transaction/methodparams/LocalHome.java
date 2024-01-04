/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.transaction.methodparams;

import jakarta.ejb.EJBLocalHome;

public interface LocalHome extends EJBLocalHome {

    Local create();

}

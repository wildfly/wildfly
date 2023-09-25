/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.ejb2.reference.eararchive;

import jakarta.ejb.EJBObject;

/**
 * @author <a href="mailto:bdecoste@jboss.com">William DeCoste</a>
 */
public interface Test3 extends EJBObject, Test3Business {
    void testAccess() throws Exception;
}

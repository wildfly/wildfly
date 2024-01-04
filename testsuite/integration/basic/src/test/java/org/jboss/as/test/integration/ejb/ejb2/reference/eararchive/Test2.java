/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.ejb2.reference.eararchive;

/**
 * @author <a href="mailto:bdecoste@jboss.com">William DeCoste</a>
 */
public interface Test2 extends jakarta.ejb.EJBObject {
    void testAccess() throws Exception;
}

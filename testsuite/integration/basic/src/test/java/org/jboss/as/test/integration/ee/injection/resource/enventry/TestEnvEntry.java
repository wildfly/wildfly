/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ee.injection.resource.enventry;

import javax.naming.NamingException;

/**
 * @author <a href="mailto:bdecoste@jboss.com">William DeCoste</a>
 * @version <tt>$Revision: 80158 $</tt>
 */
public interface TestEnvEntry {

    int checkJNDI() throws NamingException;

    int getMaxExceptions();

    int getMinExceptions();

    int getNumExceptions();
}

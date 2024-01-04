/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ee.injection.resource.mail;

import javax.naming.NamingException;

/**
 * @author <a href="mailto:bdecoste@jboss.com">William DeCoste</a>
 */
public interface StatelessMail {
    void testMail() throws NamingException;

    void testMailInjection();
}

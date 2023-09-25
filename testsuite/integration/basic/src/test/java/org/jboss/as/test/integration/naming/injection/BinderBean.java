/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.naming.injection;

import jakarta.ejb.Stateless;
import javax.naming.InitialContext;
import javax.naming.LinkRef;
import javax.naming.NamingException;

/**
 * @author Eduardo Martins
 */
@Stateless
public class BinderBean implements Binder {

    private static final String SRC_NAME = "java:global/a";

    public void bindAndLink(Object value) throws NamingException {
        InitialContext initialContext = new InitialContext();
        initialContext.bind(SRC_NAME, value);
        initialContext.bind(Binder.LINK_NAME, new LinkRef(SRC_NAME));
    }

}

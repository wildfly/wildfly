/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.naming.injection;

import jakarta.ejb.Remote;
import javax.naming.NamingException;

/**
 * @author Eduardo Martins
 */
@Remote
public interface Binder {

    String LINK_NAME = "java:global/z";

    void bindAndLink(Object value) throws NamingException;

}

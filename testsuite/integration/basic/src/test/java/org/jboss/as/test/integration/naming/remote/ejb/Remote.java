/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.naming.remote.ejb;

/**
 * @author John Bailey
 */
@jakarta.ejb.Remote
public interface Remote {
    String echo(String value);
}

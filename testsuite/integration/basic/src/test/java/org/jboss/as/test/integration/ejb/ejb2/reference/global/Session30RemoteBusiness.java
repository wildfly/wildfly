/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.ejb2.reference.global;

/**
 * @author <a href="mailto:bdecoste@jboss.com">William DeCoste</a>
 */
public interface Session30RemoteBusiness {
    String access();

    String access21();

    String globalAccess21();

    String accessLocalStateful();

    String accessLocalStateful(String value);

    String accessLocalStateful(String value, Integer suffix);
}

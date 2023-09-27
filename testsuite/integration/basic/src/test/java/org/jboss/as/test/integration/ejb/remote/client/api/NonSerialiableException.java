/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.remote.client.api;

/**
 * @author Stuart Douglas
 */
public class NonSerialiableException  extends RuntimeException {

    public Object nonSerialiable = new Object();

}

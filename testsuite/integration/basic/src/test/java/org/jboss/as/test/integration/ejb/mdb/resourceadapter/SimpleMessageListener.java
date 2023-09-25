/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.mdb.resourceadapter;

/**
 * @author Jaikiran Pai
 */
public interface SimpleMessageListener {

    void onMessage(String msg);
}

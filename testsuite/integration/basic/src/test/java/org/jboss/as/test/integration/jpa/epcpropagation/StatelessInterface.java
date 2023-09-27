/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jpa.epcpropagation;

/**
 * @author <a href="mailto:bdecoste@jboss.com">William DeCoste</a>
 */
public interface StatelessInterface {
    void createEntity(Integer id, String name);

    void createEntityNoTx(Integer id, String name);

    String updateEntity(Integer id, String name);
}

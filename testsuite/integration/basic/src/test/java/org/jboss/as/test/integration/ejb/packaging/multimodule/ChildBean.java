/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.packaging.multimodule;

import jakarta.ejb.Stateful;

/**
 * @author Stuart Douglas
 */
@Stateful
public class ChildBean extends BaseBean {

    public void doStuff() {

    }

}

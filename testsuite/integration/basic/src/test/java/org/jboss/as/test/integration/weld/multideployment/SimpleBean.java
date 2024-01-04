/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.weld.multideployment;

import java.io.Serializable;

import jakarta.inject.Named;

/**
 * @author Stuart Douglas
 */
@Named
public class SimpleBean implements Serializable{

    public void ping() {
    }
}

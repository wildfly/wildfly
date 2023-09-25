/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.remote.stateful;

import jakarta.ejb.Remote;

/**
 * User: jpai
 */
@Remote
public interface StatefulCalculatorRemote {

    int add(int number);
}

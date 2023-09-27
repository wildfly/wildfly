/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.singleton.dependson;

import java.util.ArrayList;
import java.util.List;
import jakarta.ejb.Singleton;

/**
 * Counting ordering of calls.
 *
 * @author Ondrej Chaloupka
 */
@Singleton
public class CallCounterSingleton {
    private List<String> orderLog = new ArrayList<String>();

    public void addCall(String call) {
        this.orderLog.add(call);
    }

    public List<String> getCalls() {
        return this.orderLog;
    }
}

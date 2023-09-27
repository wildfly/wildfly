/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.weld.ejb.interceptor.ordering.lifecycle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ActionSequence {

    private static final List<String> actions = Collections.synchronizedList(new ArrayList<String>());

    public static void addAction(String action) {
        actions.add(action);
    }

    public static List<String> getActions() {
        return Collections.unmodifiableList(actions);
    }

    public static void reset() {
        actions.clear();
    }
}

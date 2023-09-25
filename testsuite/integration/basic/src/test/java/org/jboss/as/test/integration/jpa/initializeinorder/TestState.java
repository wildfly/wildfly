/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jpa.initializeinorder;

import java.util.ArrayList;
import java.util.List;

/**
 * TestState
 *
 * @author Scott Marlow
 */
public class TestState {
    private static final List<String> initOrder = new ArrayList<String>();

    private static boolean gotJpaInjectingBean;
    private static boolean gotEntityManagerFactory;
    private static boolean gotEntityManager;

    public static boolean isJpaInjectingBeanAvailable() {
        return gotJpaInjectingBean;
    }

    public static void setJpaInjectingBeanAvailable() {
        gotJpaInjectingBean = true;
    }

    public static boolean isEntityManagerFactoryAvailable() {
        return gotEntityManagerFactory;
    }

    public static void setEntityManagerFactoryAvailable() {
        gotEntityManagerFactory = true;
    }

    public static boolean isEntityManagerAvailable() {
        return gotEntityManager;
    }

    public static void setEntityManagerAvailable() {
        gotEntityManager = true;
    }

    public static final List<String> getInitOrder() {
        return new ArrayList(initOrder);
    }

    public static void addInitOrder(String simpleName) {
        initOrder.add(simpleName);
    }

    public static void clearInitOrder() {
        initOrder.clear();
    }
}

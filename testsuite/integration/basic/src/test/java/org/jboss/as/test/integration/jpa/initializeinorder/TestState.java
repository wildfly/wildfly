/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

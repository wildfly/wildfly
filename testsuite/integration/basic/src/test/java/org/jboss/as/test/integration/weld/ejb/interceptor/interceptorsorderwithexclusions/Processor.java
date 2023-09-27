/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.weld.ejb.interceptor.interceptorsorderwithexclusions;

/**
 * @author Marius Bogoevici
 */
public interface Processor {
    @Secured
    int add(int x, int y);

    int subtract(int x, int y);
}

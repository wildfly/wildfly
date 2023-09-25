/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.webservices.parser;

import java.util.Map;

public class TestDA4 extends TestDA1 {
    private Map<String, Integer> map;
    private boolean bool;

    public Map<String, Integer> getMap() {
        return map;
    }

    public void setMap(Map<String, Integer> map) {
        this.map = map;
    }

    public boolean isBool() {
        return bool;
    }

    public void setBool(Boolean bool) {
        this.bool = bool;
    }
}
/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.pojo.support;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class TCollections {
    private List<Integer> numbers;
    private Set<TBean> beans;
    private Map<String, TInjectee> map;

    public List<Integer> getNumbers() {
        return numbers;
    }

    public void setNumbers(List<Integer> numbers) {
        this.numbers = numbers;
    }

    public Set<TBean> getBeans() {
        return beans;
    }

    public void setBeans(Set<TBean> beans) {
        this.beans = beans;
    }

    public Map<String, TInjectee> getMap() {
        return map;
    }

    public void setMap(Map<String, TInjectee> map) {
        this.map = map;
    }

    public void start() {
        for (Integer n : numbers)
            System.out.println("n = " + n);
        for (TBean b : beans)
            System.out.println("b = " + b);
        for (Map.Entry entry : map.entrySet())
            System.out.println("k = " + entry.getKey() + ", v = " + entry.getValue());
    }
}

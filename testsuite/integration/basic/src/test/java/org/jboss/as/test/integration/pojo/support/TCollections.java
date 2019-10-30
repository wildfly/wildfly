/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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

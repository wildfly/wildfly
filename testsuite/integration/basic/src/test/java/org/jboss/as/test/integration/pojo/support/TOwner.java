/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.pojo.support;

import java.util.HashSet;
import java.util.Set;

/**
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class TOwner {
    private Set<TInjectee> injectees = new HashSet<TInjectee>();

    public void addInjectee(TInjectee i) {
        //System.out.println("add #i-b = " + injectees.size());
        injectees.add(i);
        //System.out.println("add #i-a = " + injectees.size());
    }

    public void removeInjectee(TInjectee i) {
        //System.out.println("remove #i-b = " + injectees.size());
        injectees.remove(i);
        //System.out.println("remove #i-a = " + injectees.size());
    }
}

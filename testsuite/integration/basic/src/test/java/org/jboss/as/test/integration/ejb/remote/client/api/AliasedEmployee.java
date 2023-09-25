/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.remote.client.api;

import java.util.HashSet;
import java.util.Set;

/**
 * User: jpai
 */
public class AliasedEmployee extends Employee {


    private Set<String> nickNames = new HashSet<String>();


    public AliasedEmployee(final int id, final String name) {
        super(id, name);
    }

    public void addNick(final String nick) {
        this.nickNames.add(nick);
    }

    public Set<String> getNickNames() {
        return this.nickNames;
    }

}

/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.remote.client.api;

import jakarta.ejb.Remote;
import jakarta.ejb.Stateless;

/**
 * User: jpai
 */
@Stateless
@Remote(EmployeeManager.class)
public class EmployeeBean implements EmployeeManager {

    @Override
    public AliasedEmployee addNickNames(final Employee employee, final String... nickNames) {
        final AliasedEmployee aliasedEmployee = new AliasedEmployee(employee.getId(), employee.getName());
        for (int i=0; i<nickNames.length; i++) {
            aliasedEmployee.addNick(nickNames[i]);
        }
        return aliasedEmployee;
    }

}

/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jpa.webnontxem;

import javax.naming.InitialContext;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * Employee entity class
 *
 * @author Scott Marlow
 */
@Entity
public class Employee {

    static {
        try {
            // WFLY-6441: verify that java:comp/env values can be read from web.xml when persistence provider loads entity class
            new InitialContext().lookup("java:comp/env/simpleString");
        } catch (Exception e) {
            throw new RuntimeException("unable to get java:app/env/simpleString from JPA deployer", e);
        }
    }

    @Id
    private int id;

    private String name;

    private String address;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public int getId() {

        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
}

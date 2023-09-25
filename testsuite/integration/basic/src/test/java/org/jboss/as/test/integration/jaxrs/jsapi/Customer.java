/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jaxrs.jsapi;

import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * @author Stuart Douglas
 */
@XmlRootElement
public class Customer {
    private String first;
    private String last;

    public Customer(String first, String last) {
        this.first = first;
        this.last = last;
    }

    public Customer() {

    }

    public String getFirst() {
        return first;
    }

    public void setFirst(String first) {
        this.first = first;
    }

    public String getLast() {
        return last;
    }

    public void setLast(String last) {
        this.last = last;
    }
}

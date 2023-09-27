/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jaxrs.gzip;

import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * @author Stuart Douglas
 */
@XmlRootElement
public class JaxbModel {

    private String first;
    private String last;

    public JaxbModel(String first, String last) {
        this.first = first;
        this.last = last;
    }

    public JaxbModel() {

    }

    public String getFirst() {
        return first;
    }

    public String getLast() {
        return last;
    }

    public void setFirst(String first) {
        this.first = first;
    }

    public void setLast(String last) {
        this.last = last;
    }
}

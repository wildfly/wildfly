/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jaxrs.cfg.resources;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlValue;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement
public class TestError {

    @XmlValue
    private String value;

    public TestError() {
        value = null;
    }

    public String getValue() {
        return value;
    }

    public void setValue(final String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "TestError[value=" + value + "]";
    }
}

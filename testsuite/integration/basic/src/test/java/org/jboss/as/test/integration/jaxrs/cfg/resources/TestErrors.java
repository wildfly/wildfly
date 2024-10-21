/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jaxrs.cfg.resources;

import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement
public class TestErrors {

    @XmlElement(name = "testError")
    private List<TestError> testErrors;

    public TestErrors() {
        testErrors = new ArrayList<>();
    }

    public List<TestError> getTestErrors() {
        return testErrors;
    }

    public void setTestErrors(final List<TestError> testErrors) {
        this.testErrors = testErrors;
    }

    @Override
    public String toString() {
        return "TestErrors[testErrors=" + testErrors + "]";
    }
}

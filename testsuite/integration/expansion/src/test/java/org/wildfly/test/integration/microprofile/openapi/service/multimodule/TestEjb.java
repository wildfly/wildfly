/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.openapi.service.multimodule;

import jakarta.ejb.Stateless;
import jakarta.enterprise.context.Dependent;

/**
 * @author Joachim Grimm
 */
@Stateless
@Dependent
public class TestEjb {

    public TestResponse hello(TestRequest request) {
        TestResponse testResponse = new TestResponse();
        testResponse.setValue(request.getValue() + "1");
        return testResponse;
    }
}

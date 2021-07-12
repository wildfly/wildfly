/*
 * Copyright 2020 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wildfly.test.integration.microprofile.openapi.service.multimodule;

import javax.ejb.Stateless;

/**
 * @author Joachim Grimm
 */
@Stateless
public class TestEjb {

    public TestResponse hello(TestRequest request) {
        TestResponse testResponse = new TestResponse();
        testResponse.setValue(request.getValue() + "1");
        return testResponse;
    }
}

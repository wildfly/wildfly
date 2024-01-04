/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.ee.feature.pack.layer.tests.jsonb;

public class JsonbClassFromAdapterPackageUsage implements jakarta.json.bind.adapter.JsonbAdapter<String, String> {
    @Override
    public String adaptToJson(String s) throws Exception {
        return null;
    }

    @Override
    public String adaptFromJson(String s) throws Exception {
        return null;
    }
}

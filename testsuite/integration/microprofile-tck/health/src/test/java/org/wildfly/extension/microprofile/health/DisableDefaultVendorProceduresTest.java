/*
 * Copyright (c) 2017-2021 Contributors to the Eclipse Foundation
 *
 * See the NOTICES file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.eclipse.microprofile.health.tck.vendor.procedures.enabled;

import static org.eclipse.microprofile.health.tck.DeploymentUtils.createWarFileWithClasses;

import org.eclipse.microprofile.health.tck.TCKBase;
import org.eclipse.microprofile.health.tck.deployment.CheckWithAttributes;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.testng.Assert;
import org.testng.annotations.Test;

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;

/**
 * Verifies that default vendor procedures can be disabled by setting {@code mp.health.disable-default-procedures=true}
 * in a {@code microprofile-config.properties} file, i.e. at the application deployment level.
 *
 * As other test classes in the {@code org.eclipse.microprofile.health.tck.vendor.procedures.enabled} package, this
 * should be run in a separate execution, where {@code mp.health.disable-default-procedures=true} is not set via
 * a system property or environment variable. This is a required precondition, since the test verifies that the runtime
 * can rely on the property when this is provided at application level, via a {@code microprofile-config.properties}
 * file.
 *
 * @see <a href=
 *      "https://github.com/eclipse/microprofile-health/blob/main/spec/src/main/asciidoc/protocol-wireformat.asciidoc#disabling-default-vendor-procedures">MicroProfile
 *      Health specs</a>
 * @author Fabio Burzigotti
 */
public class DisableDefaultVendorProceduresTest extends TCKBase {

    @Deployment
    public static Archive getDeployment() {
        return createWarFileWithClasses(DisableDefaultVendorProceduresTest.class.getSimpleName(),
                CheckWithAttributes.class)
                        // Here we explicitly set that all default vendor procedures should be disabled.
                        .addAsManifestResource(
                                new StringAsset("mp.health.disable-default-procedures=true"),
                                "microprofile-config.properties");
    }

    /**
     * Verifies that only application defined checks are returned when {@code mp.health.disable-default-procedures} is
     * set to {@code true}.
     */
    @Test
    @RunAsClient
    public void testSuccessResponsePayload() {
        Response response = getUrlHealthContents();

        // status code
        Assert.assertEquals(response.getStatus(), 200);

        JsonObject json = readJson(response);

        // response size
        JsonArray checks = json.getJsonArray("checks");
        Assert.assertEquals(checks.size(), 1, "Just 1 application defined check responses is expected");

        // check validation
        JsonObject check = checks.getJsonObject(0);

        assertSuccessfulCheck(check, "attributes-check");

        // response payload attributes
        JsonObject data = check.getJsonObject("data");

        Assert.assertEquals(
                data.getString("first-key"),
                "first-val");

        Assert.assertEquals(
                data.getString("second-key"),
                "second-val");

        assertOverallSuccess(json);
    }
}

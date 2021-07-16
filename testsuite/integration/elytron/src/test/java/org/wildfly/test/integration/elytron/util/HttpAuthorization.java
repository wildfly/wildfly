/*
 * Copyright 2021 Red Hat, Inc.
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

package org.wildfly.test.integration.elytron.util;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.core.HttpHeaders;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class HttpAuthorization {

    public static ClientRequestFilter basic(final String username, final String password) {
        return requestContext -> {
            final String key = username + ':' + password;
            final String authHeader = "Basic " + Base64.getEncoder().encodeToString(key.getBytes(StandardCharsets.UTF_8));
            requestContext.getHeaders().putSingle(HttpHeaders.AUTHORIZATION, authHeader);
        };
    }
}

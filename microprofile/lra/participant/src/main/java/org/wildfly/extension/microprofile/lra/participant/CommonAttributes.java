/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.microprofile.lra.participant;

interface CommonAttributes {
    String LRA_COORDINATOR_URL = "lra-coordinator-url";
    String DEFAULT_COORDINATOR_URL = "http://localhost:8080/lra-coordinator";

    String PROXY_SERVER = "proxy-server";
    String PROXY_HOST = "proxy-host";

}
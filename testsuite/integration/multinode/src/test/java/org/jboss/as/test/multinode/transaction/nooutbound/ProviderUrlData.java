/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.multinode.transaction.nooutbound;

import org.jboss.as.test.shared.TestSuiteEnvironment;

public class ProviderUrlData {
    private final String protocol, host;
    private final int port;

    public ProviderUrlData(String protocol, String host, int port) {
        this.protocol = protocol;
        this.host = host;
        this.port = port;
    }

    public String getProviderUrl() {
        String host = TestSuiteEnvironment.formatPossibleIpv6Address(this.host);
        int port = this.port;
        return String.format("%s://%s:%s%s", protocol, host, port,
                protocol.startsWith("http") ? "/wildfly-services" : "");
    }

    public String toString() {
        return getProviderUrl();
    }
}

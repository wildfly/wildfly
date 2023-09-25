/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.management.deploy.runtime.jaxrs;

/**
 * @author <a href="mailto:lgao@redhat.com">Lin Gao</a>
 */
public class PureProxyEndPoint implements PureProxyApiService {

    @Override
    public String test(String a, String b) {
        return a + " " + b;
    }

}

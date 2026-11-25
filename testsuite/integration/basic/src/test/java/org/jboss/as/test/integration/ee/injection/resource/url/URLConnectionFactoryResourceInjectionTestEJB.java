/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ee.injection.resource.url;

import static org.wildfly.common.Assert.checkNotNullParamWithNullPointerException;

import jakarta.annotation.Resource;
import jakarta.ejb.Stateless;
import java.net.URL;

/**
 * @author Eduardo Martins
 */
@Stateless
public class URLConnectionFactoryResourceInjectionTestEJB {

    @Resource(name = "overrideLookupURL", lookup = "https://www.wildfly.org")
    private URL url1;

    @Resource(name = "lookupURL", lookup = "https://www.wildfly.org")
    private URL url2;

    /**
     *
     * @throws Exception
     */
    public void validateResourceInjection() throws Exception {
        checkNotNullParamWithNullPointerException("url1", url1);
        checkNotNullParamWithNullPointerException("url2", url2);
    }

}

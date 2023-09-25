/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ee.injection.resource.resourceref;

import java.net.URL;

import jakarta.annotation.Resource;
import jakarta.ejb.Remote;
import jakarta.ejb.Stateless;

/**
 * Let's see what we can do with resources of the URL breed.
 *
 * @author <a href="mailto:carlo.dewolf@jboss.com">Carlo de Wolf</a>
 */
@Stateless
@Remote(ResUrlChecker.class)
public class ResUrlCheckerBean implements ResUrlChecker {
    // coming in via res-url
    @Resource(name = "url2")
    private URL url2;

    public URL getURL1() {
        return null;
    }

    public URL getURL2() {
        return url2;
    }

    public URL getURL3() {
        return null;
    }
}

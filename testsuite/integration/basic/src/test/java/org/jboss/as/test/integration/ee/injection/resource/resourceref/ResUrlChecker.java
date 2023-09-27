/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ee.injection.resource.resourceref;

import java.net.URL;

/**
 * @author <a href="mailto:carlo.dewolf@jboss.com">Carlo de Wolf</a>
 */
public interface ResUrlChecker {
    URL getURL1();

    URL getURL2();

    URL getURL3();
}

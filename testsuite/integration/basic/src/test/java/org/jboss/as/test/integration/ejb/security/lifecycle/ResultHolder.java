/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.security.lifecycle;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class ResultHolder {

    private static final Map<String, String> results = new HashMap<String, String>();

    static void reset() {
        results.clear();
    }

    static void addResult(final String beanMethod, final String ejbContextMethod, final String result) {
        results.put(beanMethod + ":" + ejbContextMethod, result);
    }

    static Map<String, String> getResults() {
        return Collections.unmodifiableMap(results);
    }

}

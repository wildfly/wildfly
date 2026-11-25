/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.mvc.viewengine;

import jakarta.enterprise.inject.Produces;
import org.eclipse.krazo.engine.ViewEngineConfig;

/**
 * Producer for a fake 'config' object to
 * inject into our fake {@code MockViewEngine}.
 */
public class MockViewEngineConfigProducer {

    @Produces
    @ViewEngineConfig
    public String getViewMessage() {
        return "Mock View";
    }
}

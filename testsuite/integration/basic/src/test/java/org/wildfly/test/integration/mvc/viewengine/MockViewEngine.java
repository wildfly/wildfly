/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.mvc.viewengine;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.mvc.engine.ViewEngine;
import jakarta.mvc.engine.ViewEngineContext;
import jakarta.mvc.engine.ViewEngineException;
import org.eclipse.krazo.engine.ViewEngineConfig;

/**
 * Mock variant of the kind of thing found in the
 * <a href="https://github.com/eclipse-ee4j/krazo-extensions">krazo-extensions repo</a>.
 */
@ApplicationScoped
@Priority(ViewEngine.PRIORITY_FRAMEWORK)
public class MockViewEngine implements ViewEngine {

    @Inject
    @ViewEngineConfig
    private String message;

    @Override
    public boolean supports(String view) {
        return view.endsWith(".ve");
    }

    @Override
    public void processView(ViewEngineContext viewEngineContext) throws ViewEngineException {
        try {
            viewEngineContext.getOutputStream().write(message.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new ViewEngineException(e);
        }
    }
}

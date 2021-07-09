/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2020 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package org.wildfly.ee8to9.transformer;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

import org.jboss.logging.Logger;
import org.wildfly.galleon.plugin.transformer.JakartaTransformer;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * WildFly Core DeploymentTransformer implementation that uses the WF Galleon integration with Eclipse Transformer to
 * transform.
 */
public final class DeploymentTransformer implements org.jboss.as.server.deployment.transformation.DeploymentTransformer {

    private static final Logger logger = Logger.getLogger(DeploymentTransformer.class.getPackage().getName());

    public InputStream transform(InputStream in, String name) throws IOException {
        final boolean verbose = logger.isTraceEnabled();
        if (isEnabled()) {
            // first parameter represents external configs directory - null indicates use provided transformation defaults
            // The name captures the type of file.
            return JakartaTransformer.transform(null, in, name, verbose, new JakartaTransformer.LogHandler() {
                @Override
                public void print(String format, Object... args) {
                    logger.tracef(format, args);
                }
            });
        }
        logger.tracef("Skipping processing of %s", name);
        return in;
    }

    public void transform(Path src, Path target) throws IOException {
        // no-op initially
        /*
        final boolean verbose = logger.isTraceEnabled();
        JakartaTransformer.transform(src, target, verbose, new JakartaTransformer.LogHandler() {
            @Override
            public void print(String format, Object... args) {
                logger.tracef(format, args);
            }
        });
        */
    }

    private boolean isEnabled() {
        final String value = WildFlySecurityManager.getPropertyPrivileged("org.wildfly.unsupported.skip.jakarta.transformer", "");
        return value.isEmpty() || Boolean.parseBoolean(value);
    }
}

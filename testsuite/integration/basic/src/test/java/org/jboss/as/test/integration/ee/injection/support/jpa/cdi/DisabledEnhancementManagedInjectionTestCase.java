/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ee.injection.support.jpa.cdi;

import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.spec.WebArchive;

import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tests that an entity callbacks, e.g. {@link jakarta.persistence.PostPersist}, work with Jakarta Context and Dependency
 * Injection when the {@code jboss.as.jpa.classtransformer} property is set to {@code false}.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@ApplicationScoped
@ExtendWith(ArquillianExtension.class)
public class DisabledEnhancementManagedInjectionTestCase extends AbstractManagedInjectionTestCase {

    @Deployment
    public static WebArchive deployment() {
        return createDeployment(false);
    }
}

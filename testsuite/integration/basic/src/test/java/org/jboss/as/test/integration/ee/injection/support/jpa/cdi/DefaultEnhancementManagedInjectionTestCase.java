/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ee.injection.support.jpa.cdi;

import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.runner.RunWith;

/**
 * Tests that an entity callbacks, e.g. {@link jakarta.persistence.PostPersist}, work with Jakarta Context and Dependency
 * Injection when the {@code jboss.as.jpa.classtransformer} property is unset.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@ApplicationScoped
@RunWith(Arquillian.class)
public class DefaultEnhancementManagedInjectionTestCase extends AbstractManagedInjectionTestCase {

    @Deployment
    public static WebArchive deployment() {
        return createDeployment();
    }
}

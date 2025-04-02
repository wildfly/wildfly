/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ee.injection.support.jpa.cdi;

import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Ignore;
import org.junit.runner.RunWith;

/**
 * Tests that an entity callbacks, e.g. {@link jakarta.persistence.PostPersist}, work with Jakarta Context and Dependency
 * Injection when the {@code jboss.as.jpa.classtransformer} property is set to {@code true}.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@ApplicationScoped
@RunWith(Arquillian.class)
@Ignore("This test can be enabled once the default data source can be used. Currently the test methods will throw a " +
        "NullPointerException due the jpa subsystem explicitly disabling CDI support.")
public class EnabledEnhancementManagedInjectionTestCase extends AbstractManagedInjectionTestCase {

    @Deployment
    public static WebArchive deployment() {
        return createDeployment(true);
    }
}

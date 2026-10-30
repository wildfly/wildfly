/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.faulttolerance.context.timeout;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.inject.Inject;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Test case for https://issues.redhat.com/browse/WFLY-12982 which used to fail on legacy SR FT with:
 * <p>
 * UT005023: Exception handling request to /: org.jboss.weld.context.ContextNotActiveException:
 * WELD-001303: No active contexts for scope type jakarta.enterprise.context.RequestScoped
 *
 * @author Radoslav Husar
 */
@ExtendWith(ArquillianExtension.class)
public class TimeoutContextTestCase {

    @Deployment
    public static WebArchive createTestArchive() {
        return ShrinkWrap.create(WebArchive.class, TimeoutContextTestCase.class.getSimpleName() + ".war")
                .addAsWebInfResource(new StringAsset("<beans bean-discovery-mode=\"all\"></beans>"), "beans.xml")
                .addPackage(TimeoutContextTestCase.class.getPackage())
                ;
    }

    @Inject
    TimeoutBean timeoutBean;

    @Test
    public void testRequestContextActive() throws Exception {
        assertEquals("Hello bar", timeoutBean.greet());
    }

}

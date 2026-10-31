/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.weld.ejb.interceptor.ordering;

import java.util.ArrayList;
import java.util.List;

import jakarta.inject.Inject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import jakarta.interceptor.Interceptors;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tests that session bean interceptors are executed in the following order:
 *
 * 1) Interceptors bound using {@link Interceptors} (referred to as "legacy interceptors" hereafter)
 * 2) CDI interceptors
 * 3) Target class around invoke method
 *
 * @see https://issues.jboss.org/browse/AS7-6015
 *
 * @author Jozef Hartinger
 *
 */
@ExtendWith(ArquillianExtension.class)
public class SessionBeanInterceptorOrderingTest {

    @Inject
    private InterceptedBean bean;

    @Deployment
    public static Archive<?> getDeployment() {
        return ShrinkWrap
                .create(WebArchive.class)
                .addPackage(SessionBeanInterceptorOrderingTest.class.getPackage())
                .addAsWebInfResource(
                        new StringAsset("<beans><interceptors><class>" + CdiInterceptor.class.getName()
                                + "</class></interceptors></beans>"), "beans.xml");
    }

    @Test
    public void testSessionBeanInterceptorOrdering() {
        List<String> expectedSequence = new ArrayList<String>();
        expectedSequence.add("LegacyInterceptor");
        expectedSequence.add("CdiInterceptor");
        expectedSequence.add("TargetClassInterceptor");
        expectedSequence.add("InterceptedBean");

        List<String> actualSequence = new ArrayList<String>();
        bean.ping(actualSequence);

        assertEquals(expectedSequence, actualSequence);
    }
}

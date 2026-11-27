/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.weld.ejb.interceptor.interceptorsorderwithexclusions;

import jakarta.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author Marius Bogoevici
 */
@ExtendWith(ArquillianExtension.class)
public class InterceptorOrderTest {
    @Deployment
    public static Archive<?> deploy() {
        return ShrinkWrap.create(JavaArchive.class)
                .addAsManifestResource(new StringAsset("<beans><interceptors><class>" + CdiInterceptor2.class.getName() + "</class><class>" + CdiInterceptor.class.getName() + "</class></interceptors></beans>"), "beans.xml")
                .addPackage(InterceptorOrderTest.class.getPackage());
    }

    @Inject
    Processor processor;

    @Test
    public void testOrder() {
        Counter.count = 0;
        SimpleProcessor.count = 0;
        CdiInterceptor.count = 0;
        CdiInterceptor2.count = 0;
        EjbInterceptor.count = 0;
        EjbInterceptor2.count = 0;

        int sum = processor.add(8, 13);

        Assertions.assertEquals(21, sum);
        Assertions.assertEquals(1, EjbInterceptor.count);
        Assertions.assertEquals(2, EjbInterceptor2.count);
        Assertions.assertEquals(3, CdiInterceptor2.count);
        Assertions.assertEquals(4, CdiInterceptor.count);
        Assertions.assertEquals(5, SimpleProcessor.count);
    }

    @Test
    public void testOrder2() {
        Counter.count = 0;
        SimpleProcessor.count = 0;
        CdiInterceptor.count = 0;
        CdiInterceptor2.count = 0;
        EjbInterceptor.count = 0;
        EjbInterceptor2.count = 0;

        int sum = processor.subtract(34, 13);

        Assertions.assertEquals(21, sum);
        Assertions.assertEquals(0, EjbInterceptor.count);
        Assertions.assertEquals(1, CdiInterceptor2.count);
        Assertions.assertEquals(2, CdiInterceptor.count);
        Assertions.assertEquals(3, SimpleProcessor.count);
    }

}

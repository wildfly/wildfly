package org.jboss.as.test.integration.weld.interceptor.bridgemethods;

import javax.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;


/**
 *
 */
@RunWith(Arquillian.class)
public class BridgeMethodTest {

    @Deployment
    public static Archive<?> deploy() {
        return ShrinkWrap.create(JavaArchive.class, "testBridgeMethods.jar")
                .addAsManifestResource(new StringAsset("<beans><interceptors><class>" + SomeInterceptor.class.getName() + "</class></interceptors></beans>"), "beans.xml")
                .addPackage(BridgeMethodTest.class.getPackage());
    }

    @Inject
    private SpecialService specialService;

    @Before
    public void setUp() {
        SomeInterceptor.invocationCount = 0;
    }

    @Test
    public void testBridgeMethodInterceptor() {
        specialService.doSomething("foo");
        assertEquals(1, SomeInterceptor.invocationCount);
    }

}

package org.jboss.as.test.integration.deployment.classloading.WFLY_8907;

import static org.junit.Assert.assertEquals;

import java.util.UUID;
import javax.inject.Inject;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Ensures that deserializing a VM-local remote invocation is done using the caller's class loader rather than the invoked interface's class loader.
 *
 * @see https://issues.jboss.org/browse/WFLY-8907
 */
@RunWith(Arquillian.class)
public class WFLY_8907TestCase {

    private static final String EAR_CALLEE = "callee";

    private static final String EAR_CALLER = "caller";

    @Inject
    private CallerBean caller;

    private static JavaArchive createCalleeApiJar() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "callee-api.jar");
        jar.addClasses(MyObject.class);
        return jar;
    }

    @Deployment(name = EAR_CALLEE, order = 1, testable = false)
    public static EnterpriseArchive createCalleeEar() {
        final JavaArchive ejbJar = ShrinkWrap.create(JavaArchive.class, "ejb.jar");
        ejbJar.addClasses(CalleeBean.class);

        final EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, EAR_CALLEE + ".ear");
        ear.addAsLibrary(createCalleeApiJar());
        ear.addAsModule(ejbJar);

        return ear;
    }

    @Deployment(name = EAR_CALLER, order = 2)
    public static EnterpriseArchive createCallerEar() {
        final JavaArchive ejbJar = ShrinkWrap.create(JavaArchive.class, "ejb.jar");
        ejbJar.addClasses(CallerBean.class);
        ejbJar.addClass(WFLY_8907TestCase.class);

        final EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, EAR_CALLER + ".ear");
        ear.addAsLibrary(createCalleeApiJar());
        ear.addAsModule(ejbJar);

        return ear;
    }

    @Test
    @OperateOnDeployment(EAR_CALLER)
    public void test() throws Exception {
        String expected = UUID.randomUUID().toString();
        String actual = caller.call(expected);
        assertEquals(expected, actual);
    }
}

package org.jboss.as.test.integration.ejb.descriptor.ejbnamewildcard;

import javax.ejb.EJBAccessException;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests for wildcard (*) in ejb-name element of jboss-ejb3.xml
 * @author Jan Martiska
 */
@RunWith(Arquillian.class)
public class EJBNameWildcardTestCase {

    @Deployment
    public static Archive<?> deployment() {
        return ShrinkWrap.create(JavaArchive.class, "ejb-name-wildcard-test.jar")
                .addPackage(BeanOne.class.getPackage())
                .addPackage(BeanTwo.class.getPackage())
                .addAsManifestResource(BeanOne.class.getPackage(), "jboss-ejb3.xml", "jboss-ejb3.xml");
    }

    /*
     * Try to invoke a method which requires special privileges,
     * as defined in jboss-ejb3.xml using ejb-name=*,method-name=restrictedMethod.
     * It shouldn't be allowed.
     */
    @Test(expected = EJBAccessException.class)
    public void testWildcardRestrictedMethodOnBeanOne() throws Exception {
        getRestrictedBean(BeanOne.class).wildcardRestrictedMethod();
    }

    @Test(expected = EJBAccessException.class)
    public void testWildcardRestrictedMethodOnBeanTwo() throws Exception {
        getRestrictedBean(BeanTwo.class).wildcardRestrictedMethod();
    }

    /*
     * Try to invoke a method which is excluded
     * by jboss-ejb3.xml using ejb-name=*,method-name=excludedMethod
     * and shouldn't be callable at all.
     */
    @Test(expected = EJBAccessException.class)
    public void testWildcardExcludedMethodOnBeanOne() throws Exception {
        getRestrictedBean(BeanOne.class).wildcardExcludedMethod();
    }

    @Test(expected = EJBAccessException.class)
    public void testWildcardExcludedMethodOnBeanTwo() throws Exception {
        getRestrictedBean(BeanTwo.class).wildcardExcludedMethod();
    }

    /*
     * Try to invoke a method which is excluded
     * by jboss-ejb3.xml using ejb-name=*,method-name=excludedMethod
     * and shouldn't be callable at all.
     */
    @Test(expected = EJBAccessException.class)
    public void testLocalRestrictedMethodOnBeanTwo() throws Exception {
        getRestrictedBean(BeanTwo.class).localRestrictedMethod();
    }

    @Test(expected = EJBAccessException.class)
    public void testLocalExcludedMethodOnBeanTwo() throws Exception {
        getRestrictedBean(BeanTwo.class).localExcludedMethod();
    }

    /*
     * Try to invoke not excluded / restricted methods
     */

    @Test
    public void testUnRestrictedMethodOnBeanTwo() throws Exception {
        getRestrictedBean(BeanTwo.class).unRestrictedMethod();
    }

    @Test
    public void testNotExcludedMethodOnBeanTwo() throws Exception {
        getRestrictedBean(BeanTwo.class).notExcludedMethod();
    }

    private <T> T getRestrictedBean(Class<T> clazz) throws NamingException {
        return (T) new InitialContext().lookup("java:global/ejb-name-wildcard-test/" + clazz.getSimpleName());
    }

}

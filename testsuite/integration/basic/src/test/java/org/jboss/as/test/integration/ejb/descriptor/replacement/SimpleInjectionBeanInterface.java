package org.jboss.as.test.integration.ejb.descriptor.replacement;

/**
 * Interface for simple bean for testing injections
 */
public interface SimpleInjectionBeanInterface {
    public boolean checkInjection();
    public String greetInjectedBean();
}

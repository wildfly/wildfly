package org.jboss.as.test.integration.ejb.descriptor.replacement;

/**
 * Bean with injected bean as defined by descriptor
 */
public class SimpleInjectionBean implements SimpleInjectionBeanInterface {
    private SessionBean injectedBean;
    
    /**
     * checks if bean is injected
     * @return true if bean is injected, false otherwise
     */
    public boolean checkInjection() {
        return injectedBean != null;
    }
    
    /**
     * calls method greet on injected bean
     * @return greeting returned by greet() method of injected bean
     */
    public String greetInjectedBean() {
        return injectedBean.greet();
    }
}

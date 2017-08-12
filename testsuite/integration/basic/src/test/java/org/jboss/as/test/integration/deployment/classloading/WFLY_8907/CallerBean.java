package org.jboss.as.test.integration.deployment.classloading.WFLY_8907;

import java.util.function.Function;
import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;

@Stateless
@LocalBean
public class CallerBean {

    @EJB(mappedName = "java:global/callee/ejb/CalleeBean")
    private Function<String, MyObject> callee;

    public String call(String stuff) {
        MyObject myObject = callee.apply(stuff); // Assign to a variable to provoke a ClassCastException, if class loading does not work correctly
        return myObject.stuff();
    }
}

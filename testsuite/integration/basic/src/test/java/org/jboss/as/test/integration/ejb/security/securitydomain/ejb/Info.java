package org.jboss.as.test.integration.ejb.security.securitydomain.ejb;

import javax.ejb.EJBContext;
import java.io.Serializable;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

/**
 * @author bmaxwell
 *
 */
public class Info implements Serializable {

    private List<String> path = new ArrayList<String>();
    private String testName = null;

    /**
     *
     */
    public Info(String testName) {
        this.testName = testName;
    }

    public void add(String step) {
        path.add(step);
    }

    public List<String> getPath() {
        return path;
    }

    public String getTestName() {
        return this.testName;
    }

    public Info update(String ejbName, EJBContext ejbContext, String expectedPrinciaplClassName) {
        Principal principal = ejbContext.getCallerPrincipal();
        String caller = principal == null ? "null" : principal.getName();
        String principalClass = principal == null ? "null" : principal.getClass().getName();
        if(expectedPrinciaplClassName.compareTo(principalClass) != 0)
            add(String.format("InCorrect: Principal unexected %s != %s", principalClass, expectedPrinciaplClassName));
        else
            add(String.format("Correct: %s == %s", principalClass, expectedPrinciaplClassName));
        return this;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Test: %s\n", testName));
        sb.append(String.format("Resulting path:\n"));
        for(String step : path)
            sb.append(String.format("- %s\n", step));
        return sb.toString();
    }
}
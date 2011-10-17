/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jboss.as.testsuite.integration.management.util;

/**
 *
 * @author dpospisi
 */
public class CLIOpResult {
    
    private boolean isOutcomeSuccess;
    private Object result;

    /**
     * @return the isOutcomeSuccess
     */
    public boolean isIsOutcomeSuccess() {
        return isOutcomeSuccess;
    }

    /**
     * @param isOutcomeSuccess the isOutcomeSuccess to set
     */
    public void setIsOutcomeSuccess(boolean isOutcomeSuccess) {
        this.isOutcomeSuccess = isOutcomeSuccess;
    }

    /**
     * @return the result
     */
    public Object getResult() {
        return result;
    }

    /**
     * @param result the result to set
     */
    public void setResult(Object result) {
        this.result = result;
    }
    
}

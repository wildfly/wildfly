/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jboss.as.test.integration.ejb.descriptor.replacement;

import javax.annotation.Resource;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;

/**
 * Bean used for checking transaction settings
 * @author rhatlapa
 */
public class TransactionBean {
    
    @Resource(lookup="java:jboss/TransactionManager")
    private TransactionManager transactionManager;

    public int transactionStatus() {
        try {
            return transactionManager.getStatus();
        } catch (SystemException e) {
            throw new RuntimeException(e);
        }
    }
}

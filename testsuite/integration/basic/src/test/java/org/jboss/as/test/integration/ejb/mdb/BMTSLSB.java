package org.jboss.as.test.integration.ejb.mdb;

import jakarta.annotation.Resource;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionManagement;
import jakarta.ejb.TransactionManagementType;
import jakarta.transaction.UserTransaction;

import org.jboss.logging.Logger;

/**
 * @author: Jaikiran Pai
 */
@Stateless
@TransactionManagement(value = TransactionManagementType.BEAN)
@LocalBean
public class BMTSLSB {

    private static final Logger logger = Logger.getLogger(BMTSLSB.class);

    @Resource
    private UserTransaction userTransaction;


    public void doSomethingWithUserTransaction() {
        logger.trace("Beginning UserTransaction");
        boolean utStarted = false;
        try {
            userTransaction.begin();
            utStarted = true;
            logger.trace("UserTransaction started");
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            try {
                if (utStarted) {
                    userTransaction.commit();
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}

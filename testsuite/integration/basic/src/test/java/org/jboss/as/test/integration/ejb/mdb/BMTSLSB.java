package org.jboss.as.test.integration.ejb.mdb;

import javax.annotation.Resource;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.transaction.UserTransaction;

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

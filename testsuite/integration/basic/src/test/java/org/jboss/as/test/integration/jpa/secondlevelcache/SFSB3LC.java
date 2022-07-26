package org.jboss.as.test.integration.jpa.secondlevelcache;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;

@Stateless
public class SFSB3LC {

    @EJB
    private SFSB4LC sfsb4lc;

    /**
     * Create employee in provided EntityManager
     */
    public void createEmployee(String name, String address, int id) {
        sfsb4lc.createEmployee(name, address, id);
    }

    /**
     * Performs 2 query calls, first call put entity in the cache and second should hit the cache
     *
     * @param id Employee's id in the query
     */
    public String entityCacheCheck(int id) {
        return sfsb4lc.entityCacheCheck(id);
    }

    /**
     * Update employee simulating an intermediate exception in a nested transaction:
     * <ul>
     * <li>Transaction A, calls transaction B which modifies entity X</li>
     * <li>Transaction B gets an application exception with rollback=true, so the transaction rolls back</li>
     * <li>Transaction A catches this exception, and calls the same EJB (now Transaction C since it REQUIRES_NEW)</li>
     * <li>Transaction C modifies the same record but this time doesn't throw the application exception</li>
     * </ul>
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public String testL2CacheWithRollbackAndRetry(int id, String address) {
        try {
            sfsb4lc.updateEmployeeAddress(id, address, true);
        } catch (RollbackException ex) {
            try {
                sfsb4lc.updateEmployeeAddress(id, address, false);
            } catch (Exception e) {
                return e.getMessage();
            }
        }
        return "OK";
    }
}

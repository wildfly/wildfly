package org.jboss.as.test.integration.ee.injection.resource.persistencecontextref;

import javax.ejb.Stateful;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 * SFSB
 *
 * @author Scott Marlow
 */
@Stateful
public class SFSB {
    //this one will be overridden via deployment descriptor to be otherpu
    @PersistenceContext(unitName = "mypc", name = "otherPcBinding")
    private EntityManager otherpc;

    @PersistenceContext(unitName = "mypc", name = "unsyncPcBinding")
    private EntityManager unSynchronizedPc;

    public boolean unsynchronizedIsNotJoinedToTX() {
        return unSynchronizedPc.isJoinedToTransaction();
    }

    public boolean synchronizedIsJoinedToTX() {
        return otherpc.isJoinedToTransaction();
    }


}

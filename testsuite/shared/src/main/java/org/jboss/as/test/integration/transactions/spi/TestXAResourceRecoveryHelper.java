/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2020, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */


package org.jboss.as.test.integration.transactions.spi;

import com.arjuna.ats.arjuna.recovery.RecoveryManager;
import com.arjuna.ats.arjuna.recovery.RecoveryModule;
import com.arjuna.ats.internal.jta.recovery.arjunacore.XARecoveryModule;
import com.arjuna.ats.jta.recovery.XAResourceRecoveryHelper;
import org.jboss.as.test.integration.transactions.PersistentTestXAResource;
import org.jboss.as.test.integration.transactions.TestXAResource;
import org.jboss.as.test.integration.transactions.TransactionCheckerSingleton;
import org.jboss.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.transaction.xa.XAResource;
import java.util.Vector;

/**
 * <p>
 * Singleton startup class which instantiate the {@link XAResourceRecoveryHelper}
 * for {@link TestXAResource} and {@link PersistentTestXAResource} created in the <code>testsuite/shared</code>
 * module for testing transactional behaviour.
 * </p>
 * <p>
 * The recovery helper is registered within the {@link XARecoveryModule}. That's the way which is provided
 * by Narayana to work with the {@link XAResource}s during recovery.
 * The {@link XAResourceRecoveryHelper} then provides information on unfinished {@link javax.transaction.xa.Xid}
 * of the particular {@link XAResource} that the helper is responsible for.
 * </p>
 * <p>
 * If the arquillian test deployment contains this class the recovery handling
 * is able to use the test xa resources during recovery.
 * </p>
 */
@Singleton
@Startup
public class TestXAResourceRecoveryHelper implements XAResourceRecoveryHelper {
    private static final Logger log = Logger.getLogger(TestXAResourceRecoveryHelper.class);

    // instantiated on singleton start-up
    private TestXAResource testXaResourceInstance;
    private TestXAResource persistentTestXaResourceInstance;

    @EJB
    private TransactionCheckerSingleton transactionCheckerSingleton;

    /**
     * Singleton lifecycle method.
     * Register the recovery module with the transaction manager.
     */
    @PostConstruct
    public void postConstruct() {
        log.debug("TestXAResourceRecoveryHelper starting");
        this.testXaResourceInstance = new TestXAResource(transactionCheckerSingleton);
        this.persistentTestXaResourceInstance = new PersistentTestXAResource(transactionCheckerSingleton);
        getRecoveryModule().addXAResourceRecoveryHelper(this);
    }

    /**
     * Singleton lifecycle method.
     * Unregister the recovery module from the transaction manager.
     */
    @PreDestroy
    public void preDestroy() {
        log.debug("TestXAResourceRecoveryHelper stopping");
        getRecoveryModule().removeXAResourceRecoveryHelper(this);
    }

    /**
     * Implementing {@link XAResourceRecoveryHelper#initialise(String)}.
     * Narayana does not use this.
     */
    public boolean initialise(String param) throws Exception {
        return true;
    }

    /**
     * Implementing {@link XAResourceRecoveryHelper#getXAResources()}
     * Returning the test {@link XAResource}s which are then used during recovery by {@link XARecoveryModule}
     * where the {@link XAResource#recover(int)} is invoked.
     */
    public XAResource[] getXAResources() throws Exception {
        log.debugf("getXAResources() instances: %s and %s", testXaResourceInstance, persistentTestXaResourceInstance);
        return new XAResource[]{testXaResourceInstance, persistentTestXaResourceInstance};
    }

    /**
     * A way how to to get {@link XARecoveryModule} from Narayna where the recovery helper can be registered into.
     *
     * @return Narayana instantiated {@link XARecoveryModule}
     */
    private XARecoveryModule getRecoveryModule() {
        for (RecoveryModule recoveryModule : ((Vector<RecoveryModule>) RecoveryManager.manager().getModules())) {
            if (recoveryModule instanceof XARecoveryModule) {
                return (XARecoveryModule) recoveryModule;
            }
        }
        throw new IllegalStateException("Cannot find XARecoveryModule which is necessary " +
                "for recovery initialization of the test XAResources");
    }
}
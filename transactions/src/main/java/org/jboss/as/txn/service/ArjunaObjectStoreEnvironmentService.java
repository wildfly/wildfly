/**
 *
 */
package org.jboss.as.txn.service;

import com.arjuna.ats.internal.arjuna.objectstore.hornetq.HornetqJournalEnvironmentBean;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

import com.arjuna.ats.arjuna.common.ObjectStoreEnvironmentBean;
import com.arjuna.common.internal.util.propertyservice.BeanPopulator;

import javax.management.MBeanServer;

/**
 * Configures the {@link ObjectStoreEnvironmentBean}s using an injected path.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class ArjunaObjectStoreEnvironmentService implements Service<Void> {

    private final InjectedValue<MBeanServer> mbeanServer = new InjectedValue<MBeanServer>();
    private final InjectedValue<String> pathInjector = new InjectedValue<String>();
    private final boolean useHornetqJournalStore;

    public ArjunaObjectStoreEnvironmentService(boolean useHornetqJournalStore) {
        this.useHornetqJournalStore = useHornetqJournalStore;
    }

    @Override
    public Void getValue() throws IllegalStateException, IllegalArgumentException {
        return null;
    }

    @Override
    public void start(StartContext context) throws StartException {
        String objectStoreDir = pathInjector.getValue();

         final ObjectStoreEnvironmentBean defaultActionStoreObjectStoreEnvironmentBean =
           BeanPopulator.getNamedInstance(ObjectStoreEnvironmentBean.class, "default");

        if(useHornetqJournalStore) {
            HornetqJournalEnvironmentBean hornetqJournalEnvironmentBean = BeanPopulator.getDefaultInstance(
                    com.arjuna.ats.internal.arjuna.objectstore.hornetq.HornetqJournalEnvironmentBean.class
            );
            hornetqJournalEnvironmentBean.setStoreDir(objectStoreDir+"/HornetqObjectStore");
            defaultActionStoreObjectStoreEnvironmentBean.setObjectStoreType(
                    "com.arjuna.ats.internal.arjuna.objectstore.hornetq.HornetqObjectStoreAdaptor"
            );
        } else {
            defaultActionStoreObjectStoreEnvironmentBean.setObjectStoreDir(objectStoreDir);
        }

        final ObjectStoreEnvironmentBean stateStoreObjectStoreEnvironmentBean =
            BeanPopulator.getNamedInstance(ObjectStoreEnvironmentBean.class, "stateStore");
        stateStoreObjectStoreEnvironmentBean.setObjectStoreDir(objectStoreDir);
        final ObjectStoreEnvironmentBean communicationStoreObjectStoreEnvironmentBean =
            BeanPopulator.getNamedInstance(ObjectStoreEnvironmentBean.class, "communicationStore");
        communicationStoreObjectStoreEnvironmentBean.setObjectStoreDir(objectStoreDir);
    }

    @Override
    public void stop(StopContext context) {
    }

    public InjectedValue<String> getPathInjector() {
        return pathInjector;
    }

    public InjectedValue<MBeanServer> getMbeanServer() {
        return mbeanServer;
    }
}

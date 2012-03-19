/**
 *
 */
package org.jboss.as.txn.service;

import org.jboss.as.controller.services.path.PathManager;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

import com.arjuna.ats.arjuna.common.ObjectStoreEnvironmentBean;
import com.arjuna.ats.internal.arjuna.objectstore.hornetq.HornetqJournalEnvironmentBean;
import com.arjuna.common.internal.util.propertyservice.BeanPopulator;

/**
 * Configures the {@link ObjectStoreEnvironmentBean}s using an injected path.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class ArjunaObjectStoreEnvironmentService implements Service<Void> {

    private final InjectedValue<PathManager> pathManagerInjector = new InjectedValue<PathManager>();
    private final boolean useHornetqJournalStore;
    private final String path;
    private final String pathRef;
    private volatile PathManager.Callback.Handle callbackHandle;

    public ArjunaObjectStoreEnvironmentService(boolean useHornetqJournalStore, String path, String pathRef) {
        this.useHornetqJournalStore = useHornetqJournalStore;
        this.path = path;
        this.pathRef = pathRef;
    }

    @Override
    public Void getValue() throws IllegalStateException, IllegalArgumentException {
        return null;
    }

    @Override
    public void start(StartContext context) throws StartException {
        callbackHandle = pathManagerInjector.getValue().registerCallback(pathRef, PathManager.ReloadServerCallback.create(), PathManager.Event.UPDATED, PathManager.Event.REMOVED);
        String objectStoreDir = pathManagerInjector.getValue().resolveRelativePathEntry(path, pathRef);

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
        callbackHandle.remove();
    }

    public InjectedValue<PathManager> getPathManagerInjector() {
        return pathManagerInjector;
    }
}

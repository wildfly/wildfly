/**
 *
 */
package org.jboss.as.txn;

import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

import com.arjuna.ats.arjuna.common.ObjectStoreEnvironmentBean;
import com.arjuna.common.internal.util.propertyservice.BeanPopulator;

/**
 * Configures the {@link ObjectStoreEnvironmentBean}s using an injected path.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class ArjunaObjectStoreEnvironmentService implements Service<Void> {


    private final InjectedValue<String> pathInjector = new InjectedValue<String>();

    @Override
    public Void getValue() throws IllegalStateException, IllegalArgumentException {
        return null;
    }

    @Override
    public void start(StartContext context) throws StartException {
        String objectStoreDir = pathInjector.getValue();

        final ObjectStoreEnvironmentBean nullActionStoreObjectStoreEnvironmentBean =
            BeanPopulator.getNamedInstance(ObjectStoreEnvironmentBean.class, null);
         nullActionStoreObjectStoreEnvironmentBean.setObjectStoreDir(objectStoreDir);
         final ObjectStoreEnvironmentBean defaultActionStoreObjectStoreEnvironmentBean =
           BeanPopulator.getNamedInstance(ObjectStoreEnvironmentBean.class, "default");
        defaultActionStoreObjectStoreEnvironmentBean.setObjectStoreDir(objectStoreDir);
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

    InjectedValue<String> getPathInjector() {
        return pathInjector;
    }

}

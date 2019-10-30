package org.jboss.as.naming.service;

import org.jboss.as.naming.NamingStore;
import org.jboss.as.naming.context.NamespaceContextSelector;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

import javax.naming.CompositeName;
import javax.naming.Context;
import javax.naming.NamingException;

/**
 * @author Eduardo Martins
 */
public class DefaultNamespaceContextSelectorService implements Service<Void> {

    public static final ServiceName SERVICE_NAME = ContextNames.NAMING.append("defaultNamespaceContextSelector");

    private static final CompositeName EMPTY_NAME = new CompositeName();

    private final InjectedValue<NamingStore> globalNamingStore;
    private final InjectedValue<NamingStore> jbossNamingStore;
    private final InjectedValue<NamingStore> remoteExposedNamingStore;

    public DefaultNamespaceContextSelectorService() {
        this.globalNamingStore = new InjectedValue<>();
        this.jbossNamingStore = new InjectedValue<>();
        this.remoteExposedNamingStore = new InjectedValue<>();
    }

    public InjectedValue<NamingStore> getGlobalNamingStore() {
        return globalNamingStore;
    }

    public InjectedValue<NamingStore> getJbossNamingStore() {
        return jbossNamingStore;
    }

    public InjectedValue<NamingStore> getRemoteExposedNamingStore() {
        return remoteExposedNamingStore;
    }

    @Override
    public void start(StartContext startContext) throws StartException {
        NamespaceContextSelector.setDefault(new NamespaceContextSelector() {
            public Context getContext(String identifier) {
                final NamingStore namingStore;
                if (identifier.equals("global")) {
                    namingStore = globalNamingStore.getValue();
                } else if (identifier.equals("jboss")) {
                    namingStore = jbossNamingStore.getValue();
                } else if (identifier.equals("jboss/exported")) {
                    namingStore = remoteExposedNamingStore.getValue();
                } else {
                    namingStore = null;
                }
                if (namingStore != null) {
                    try {
                        return (Context) namingStore.lookup(EMPTY_NAME);
                    } catch (NamingException e) {
                        throw new IllegalStateException(e);
                    }
                } else {
                    return null;
                }
            }
        });
    }

    @Override
    public void stop(StopContext stopContext) {
    }

    @Override
    public Void getValue() throws IllegalStateException, IllegalArgumentException {
        return null;
    }

}

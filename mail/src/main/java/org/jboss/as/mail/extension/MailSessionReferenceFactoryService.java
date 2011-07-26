package org.jboss.as.mail.extension;

import org.jboss.as.naming.ManagedReference;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.naming.ValueManagedReference;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.ImmediateValue;
import org.jboss.msc.value.InjectedValue;

import javax.mail.Session;

/**
 * Service responsible for exposing a {@link org.jboss.as.naming.ManagedReferenceFactory} for a {@link javax.mail.Session}.
 *
 * @author Tomaz Cerar
 * @created 27.7.11 0:29
 */
public class MailSessionReferenceFactoryService implements Service<ManagedReferenceFactory>, ManagedReferenceFactory {
    public static final ServiceName SERVICE_NAME_BASE = MailSessionAdd.SERVICE_NAME_BASE.append("reference-factory");
    private final InjectedValue<Session> mailSessionValue = new InjectedValue<Session>();

    private ManagedReference reference;

    public synchronized void start(StartContext startContext) throws StartException {
        reference = new ValueManagedReference(new ImmediateValue<Object>(mailSessionValue.getValue()));
    }

    public synchronized void stop(StopContext stopContext) {
        reference = null;
    }

    public synchronized ManagedReferenceFactory getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    public synchronized ManagedReference getReference() {
        return reference;
    }

    public Injector<Session> getDataSourceInjector() {
        return mailSessionValue;
    }
}

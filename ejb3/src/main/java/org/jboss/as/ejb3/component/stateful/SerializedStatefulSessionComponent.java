package org.jboss.as.ejb3.component.stateful;

import java.io.ObjectStreamException;
import java.io.Serializable;

import org.jboss.as.naming.ValueManagedReference;
import org.jboss.as.server.CurrentServiceContainer;
import org.jboss.ejb.client.SessionID;
import org.jboss.invocation.InterceptorFactoryContext;
import org.jboss.invocation.SimpleInterceptorFactoryContext;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.value.ImmediateValue;

/**
 *
 * Serialized form of a SFSB
 *
 * @author Stuart Douglas
 */
public class SerializedStatefulSessionComponent implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String serviceName;
    private final SessionID sessionID;
    private final Object instance;

    public SerializedStatefulSessionComponent(final Object instance, final SessionID sessionID, final String serviceName) {
        this.instance = instance;
        this.sessionID = sessionID;
        this.serviceName = serviceName;
    }


    private Object readResolve() throws ObjectStreamException {
        ServiceName name = ServiceName.parse(serviceName);
        ServiceController<?> service = CurrentServiceContainer.getServiceContainer().getRequiredService(name);
        StatefulSessionComponent component = (StatefulSessionComponent) service.getValue();
        final InterceptorFactoryContext context = new SimpleInterceptorFactoryContext();
        context.getContextData().put(SessionID.class, sessionID);
        return component.constructComponentInstance(new ValueManagedReference(new ImmediateValue<Object>(instance)), false, context);
    }
}

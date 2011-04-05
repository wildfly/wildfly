/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.messaging.jms;

import java.util.Map;
import org.hornetq.jms.server.JMSServerManager;
import org.jboss.as.naming.ManagedReference;
import org.jboss.as.naming.ManagedReferenceInjector;
import org.jboss.as.naming.MockContext;
import org.jboss.as.naming.NamingStore;
import org.jboss.as.naming.ValueManagedObject;
import org.jboss.as.naming.ValueManagedReference;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.naming.service.BinderService;
import org.jboss.logging.Logger;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.msc.value.Values;

/**
 * Service responsible for creating and destroying a {@code javax.jms.Topic}.
 *
 * @author Emanuel Muckenhuber
 */
public class JMSTopicService implements Service<Void> {

    private final InjectedValue<JMSServerManager> jmsServer = new InjectedValue<JMSServerManager>();

    private final String name;
    private final String[] jndi;

    public JMSTopicService(String name, String[] jndi) {
        this.name = name;
        this.jndi = jndi;
    }

    /** {@inheritDoc} */
    public synchronized void start(StartContext context) throws StartException {
        final JMSServerManager jmsManager = jmsServer.getValue();
        try {
            MockContext.pushBindingTrap();
            try {
                jmsManager.createTopic(false, name, jndi);
            } finally {
                final ServiceTarget target = context.getChildTarget();
                final Map<String, Object> bindings = MockContext.popTrappedBindings();
                for(Map.Entry<String, Object> binding : bindings.entrySet()) {
                    final BinderService binderService = new BinderService(binding.getKey());
                    target.addService(ContextNames.JAVA_CONTEXT_SERVICE_NAME.append(binding.getKey()), binderService)
                        .addDependency(ContextNames.JAVA_CONTEXT_SERVICE_NAME, NamingStore.class, binderService.getNamingStoreInjector())
                        .addInjection(binderService.getManagedObjectInjector(), new ValueManagedObject(Values.immediateValue(binding.getValue())))
                        .setInitialMode(ServiceController.Mode.ACTIVE)
                        .install();
                }
            }
        } catch (Exception e) {
            throw new StartException("failed to create queue", e);
        }
    }

    /** {@inheritDoc} */
    public synchronized void stop(StopContext context) {
        final JMSServerManager jmsManager = jmsServer.getValue();
        try {
            jmsManager.destroyTopic(name);
        } catch (Exception e) {
            Logger.getLogger("org.jboss.messaging").warnf(e ,"failed to destroy jms topic: %s", name);
        }
    }

    /** {@inheritDoc} */
    public Void getValue() throws IllegalStateException {
        return null;
    }

    InjectedValue<JMSServerManager> getJmsServer() {
        return jmsServer;
    }

}

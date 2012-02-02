package org.jboss.as.capedwarf.services;

import org.jboss.logging.Logger;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.Queue;
import javax.jms.Session;

/**
 * Servlet executor consumera service
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class ServletExecutorConsumerService implements Service<Connection> {

    public static final ServiceName NAME = ServiceName.JBOSS.append("capedwarf").append("consumer");

    private Logger log = Logger.getLogger(ServletExecutorConsumerService.class);

    private InjectedValue<ConnectionFactory> factory = new InjectedValue<ConnectionFactory>();
    private InjectedValue<Queue> queue = new InjectedValue<Queue>();

    private Connection connection;

    public void start(StartContext context) throws StartException {
        try {
            final Connection qc = factory.getValue().createConnection();
            final Session session = qc.createSession(false, Session.AUTO_ACKNOWLEDGE);
            final MessageConsumer consumer = session.createConsumer(queue.getValue());
            consumer.setMessageListener(new ServletExecutorConsumer());
            qc.start();
            connection = qc;
        } catch (Exception e) {
            throw new StartException("Cannot start JMS connection.", e);
        }
    }

    public void stop(StopContext context) {
        try {
            connection.stop();
        } catch (JMSException e) {
            log.error("Error stopping JMS connection.", e);
        } finally {
            try {
                connection.close();
            } catch (JMSException e) {
                log.warn("");
            }
        }
    }

    public Connection getValue() throws IllegalStateException, IllegalArgumentException {
        return connection;
    }

    public InjectedValue<ConnectionFactory> getFactory() {
        return factory;
    }

    public InjectedValue<Queue> getQueue() {
        return queue;
    }
}

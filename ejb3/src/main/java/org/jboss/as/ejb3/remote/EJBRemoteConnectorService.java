/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.ejb3.remote;

import org.jboss.logging.Logger;
import org.jboss.marshalling.Marshalling;
import org.jboss.marshalling.SimpleDataInput;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.remoting3.Channel;
import org.jboss.remoting3.CloseHandler;
import org.jboss.remoting3.Endpoint;
import org.jboss.remoting3.MessageInputStream;
import org.jboss.remoting3.OpenListener;
import org.jboss.remoting3.Registration;
import org.jboss.remoting3.ServiceRegistrationException;
import org.xnio.OptionMap;

import java.io.IOException;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class EJBRemoteConnectorService implements Service<EJBRemoteConnectorService> {
    private static final Logger log = Logger.getLogger(EJBRemoteConnectorService.class);

    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("ejb3", "connector");

    private final InjectedValue<Endpoint> endpointValue = new InjectedValue<Endpoint>();

    public InjectedValue<Endpoint> getEndpointInjector(){
        return endpointValue;
    }

    private volatile Registration registration;

    @Override
    public void start(StartContext context) throws StartException {
        try {
            registration = endpointValue.getValue().registerService("ejb3", new OpenListener() {
                @Override
                public void channelOpened(Channel channel) {
                    log.tracef("Welcome %s to the ejb3 channel", channel);
                    channel.addCloseHandler(new CloseHandler<Channel>() {
                        @Override
                        public void handleClose(Channel closed, IOException exception) {
                            // do nothing
                            log.tracef("channel %s closed", closed);
                        }
                    });
                    Channel.Receiver handler = new Channel.Receiver() {
                        @Override
                        public void handleError(Channel channel, IOException error) {
                            try {
                                channel.close();
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                            throw new RuntimeException("NYI: .handleError");
                        }

                        @Override
                        public void handleEnd(Channel channel) {
                            try {
                                channel.close();
                            } catch (IOException e) {
                                // ignore
                            }
                        }

                        @Override
                        public void handleMessage(Channel channel, MessageInputStream message) {
                            channel.receiveMessage(this);
                            // TODO: implement EJB wire protocol
                            final SimpleDataInput input = new SimpleDataInput(Marshalling.createByteInput(message));
                            try {
                                final String txt = input.readUTF();
                                System.out.println(txt);
                                input.close();
                            } catch (IOException e) {
                                // log it
                                log.tracef(e, "Exception on channel %s from message %s", channel, message);
                                try {
                                    // press the panic button
                                    channel.writeShutdown();
                                } catch (IOException e1) {
                                    // ignore
                                }
                            }
                        }
                    };
                    channel.receiveMessage(handler);
                }

                @Override
                public void registrationTerminated() {
                    // do nothing
                }
            }, OptionMap.EMPTY);
        } catch (ServiceRegistrationException e) {
            throw new StartException(e);
        }
    }

    @Override
    public void stop(StopContext context) {
        registration.close();
    }

    @Override
    public EJBRemoteConnectorService getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }
}

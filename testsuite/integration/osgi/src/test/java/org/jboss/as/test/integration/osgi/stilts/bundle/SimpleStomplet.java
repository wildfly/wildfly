/*
 * Copyright 2011 Red Hat, Inc, and individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.test.integration.osgi.stilts.bundle;

import org.jboss.logging.Logger;
import org.projectodd.stilts.stomp.StompException;
import org.projectodd.stilts.stomp.StompMessage;
import org.projectodd.stilts.stomp.spi.StompSession;
import org.projectodd.stilts.stomplet.Subscriber;
import org.projectodd.stilts.stomplet.simple.SimpleSubscribableStomplet;

/**
 * A simple test {@link Stomplet).
 *
 * @author thomas.diesler@jboss.com
 * @since 07-Sep-2011
 */
public class SimpleStomplet extends SimpleSubscribableStomplet {

    public static final String DESTINATION_QUEUE_ONE = "/queue/one";

    static Logger log = Logger.getLogger(SimpleStomplet.class);

    @Override
    public void onMessage(StompMessage message, StompSession session) throws StompException {
        log.infof("onMessage: %s", message);
        sendToAllSubscribers(message);
    }

    @Override
    public void onSubscribe(Subscriber subscriber) throws StompException {
        super.onSubscribe(subscriber);
        log.infof("onSubscribe: %s", subscriber);
    }

    @Override
    public void onUnsubscribe(Subscriber subscriber) throws StompException {
        log.infof("onUnsubscribe: %s", subscriber);
        super.onUnsubscribe(subscriber);
    }
}
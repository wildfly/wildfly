/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.arquillian.context;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.logging.Logger;
import org.jboss.weld.context.bound.BoundConversationContext;
import org.jboss.weld.context.bound.BoundLiteral;
import org.jboss.weld.context.bound.BoundRequestContext;
import org.jboss.weld.context.bound.BoundSessionContext;
import org.jboss.weld.context.bound.MutableBoundRequest;

/**
 * Sets up the session, request and conversation contexts for a weld deployment
 *
 * @author Stuart Douglas
 *
 */
public class WeldContextSetup implements SetupAction {

    private static final String STANDARD_BEAN_MANAGER_JNDI_NAME = "java:comp/BeanManager";

    private static final Logger log = Logger.getLogger("org.jboss.as.arquillian");

    @SuppressWarnings("unchecked")
    public void setup() {
        try {
            final BeanManager manager = (BeanManager) new InitialContext().lookup(STANDARD_BEAN_MANAGER_JNDI_NAME);

            if (manager != null) {

                final Map<String, Object> sessionMap = new ConcurrentHashMap<String, Object>();
                final Map<String, Object> requestMap = new ConcurrentHashMap<String, Object>();

                final Bean<BoundSessionContext> sessionContextBean = (Bean<BoundSessionContext>) manager.resolve(manager
                        .getBeans(BoundSessionContext.class, BoundLiteral.INSTANCE));
                CreationalContext<?> ctx = manager.createCreationalContext(sessionContextBean);
                final BoundSessionContext sessionContext = (BoundSessionContext) manager.getReference(sessionContextBean,
                        BoundSessionContext.class, ctx);
                sessionContext.associate(sessionMap);
                sessionContext.activate();

                final Bean<BoundRequestContext> requestContextBean = (Bean<BoundRequestContext>) manager.resolve(manager
                        .getBeans(BoundRequestContext.class, BoundLiteral.INSTANCE));
                ctx = manager.createCreationalContext(requestContextBean);
                final BoundRequestContext requestContext = (BoundRequestContext) manager.getReference(requestContextBean,
                        BoundRequestContext.class, ctx);
                requestContext.associate(requestMap);
                requestContext.activate();

                final Bean<BoundConversationContext> conversationContextBean = (Bean<BoundConversationContext>) manager
                        .resolve(manager.getBeans(BoundConversationContext.class, BoundLiteral.INSTANCE));
                ctx = manager.createCreationalContext(conversationContextBean);
                final BoundConversationContext conversationContext = (BoundConversationContext) manager.getReference(
                        conversationContextBean, BoundConversationContext.class, ctx);
                conversationContext.associate(new MutableBoundRequest(requestMap, sessionMap));
                // conversationContext.activate();
            }
        } catch (NamingException e) {
            log.error("Failed to setup Weld contexts", e);
        }
    }

    @SuppressWarnings("unchecked")
    public void teardown() {
        try {
            final BeanManager manager = (BeanManager) new InitialContext().lookup("java:comp/BeanManager");

            final Bean<BoundSessionContext> sessionContextBean = (Bean<BoundSessionContext>) manager.resolve(manager.getBeans(
                    BoundSessionContext.class, BoundLiteral.INSTANCE));
            CreationalContext<?> ctx = manager.createCreationalContext(sessionContextBean);
            final BoundSessionContext sessionContext = (BoundSessionContext) manager.getReference(sessionContextBean,
                    BoundSessionContext.class, ctx);
            sessionContext.deactivate();

            final Bean<BoundRequestContext> requestContextBean = (Bean<BoundRequestContext>) manager.resolve(manager.getBeans(
                    BoundSessionContext.class, BoundLiteral.INSTANCE));
            ctx = manager.createCreationalContext(requestContextBean);
            final BoundRequestContext requestContext = (BoundRequestContext) manager.getReference(requestContextBean,
                    BoundSessionContext.class, ctx);
            requestContext.deactivate();

            final Bean<BoundConversationContext> conversationContextBean = (Bean<BoundConversationContext>) manager
                    .resolve(manager.getBeans(BoundConversationContext.class, BoundLiteral.INSTANCE));
            ctx = manager.createCreationalContext(conversationContextBean);
            final BoundConversationContext conversationContext = (BoundConversationContext) manager.getReference(
                    conversationContextBean, BoundSessionContext.class, ctx);
            conversationContext.deactivate();
        } catch (NamingException e) {
            log.error("Failed to tear down Weld contexts", e);
        }
    }

}

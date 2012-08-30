/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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
package org.jboss.as.domain.management.security;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DATABASE_CONNECTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PLAIN_TEXT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SIMPLE_SELECT_TABLE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SIMPLE_SELECT_USERNAME_FIELD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SIMPLE_SELECT_USERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SIMPLE_SELECT_USERS_PASSWORD_FIELD;

import java.io.IOException;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.UnsupportedCallbackException;

import junit.framework.Assert;

import org.jboss.as.domain.management.connections.ConnectionManager;
import org.jboss.as.domain.management.connections.database.DatabaseConnectionManagerService;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.value.ImmediateValue;
import org.jboss.msc.value.InjectedValue;
import org.jboss.sasl.callback.DigestHashCallback;
import org.jboss.sasl.callback.VerifyPasswordCallback;
import org.junit.Test;
/**
 *  Database callback test case
 *
 *  @author <a href="mailto:flemming.harms@gmail.com">Flemming Harms</a>
 */
public class DatabaseCallbackHandlerTestCase extends AbstractDatabaseConnectionTestHelper {

    private DatabaseCallbackHandler databaseCallbackHandler;
    private ModelNode cNode;

    public void initCallbackHandler(final DatabaseConnectionManagerService dcs) throws Exception {
          databaseCallbackHandler = new DatabaseCallbackHandler(TEST_REALM, cNode) {
          @Override
            public InjectedValue<ConnectionManager> getConnectionManagerInjector() {
                InjectedValue<ConnectionManager> cm = new InjectedValue<ConnectionManager>();
                cm.setValue(new ImmediateValue<ConnectionManager>(dcs));
                return cm;
            }
        };

    }

    @Override
    void initAuthenticationModel(boolean plainPassword) {
        cNode = new ModelNode();
        cNode.get(OP).set(ADD);
        cNode.get(OP_ADDR).add(DATABASE_CONNECTION, "db");
        cNode.get(PLAIN_TEXT).set(plainPassword);
        cNode.get(SIMPLE_SELECT_USERS).set(true);
        cNode.get(SIMPLE_SELECT_TABLE).set("users");
        cNode.get(SIMPLE_SELECT_USERNAME_FIELD).set("user");
        cNode.get(SIMPLE_SELECT_USERS_PASSWORD_FIELD).set("password");
    }

    @Test
    public void testHandleSimplePassword() throws IOException, UnsupportedCallbackException {
        final Callback[] callbacks = {new NameCallback("test","Jack.Carter"),new VerifyPasswordCallback("eureka")};
        databaseCallbackHandler.handle(callbacks);
        VerifyPasswordCallback verifyPasswordCallback = (VerifyPasswordCallback) callbacks[1];
        Assert.assertEquals(true, verifyPasswordCallback.isVerified());
    }

    @Test
    public void testHandleBadPassword() throws IOException, UnsupportedCallbackException {
        final Callback[] callbacks = { new NameCallback("test", "Jack.Carter"), new VerifyPasswordCallback("youcanthackme") };
        databaseCallbackHandler.handle(callbacks);
        VerifyPasswordCallback verifyPasswordCallback = (VerifyPasswordCallback) callbacks[1];
        Assert.assertEquals(false, verifyPasswordCallback.isVerified());
    }

    @Test
    public void testHandleHashedPassword() throws IOException, UnsupportedCallbackException {
        final Callback[] callbacks = {new NameCallback("test","Henry.Deacon"),new VerifyPasswordCallback("eureka")};
        initAuthenticationModel(false);
        databaseCallbackHandler = new DatabaseCallbackHandler(TEST_REALM, cNode) {
          @Override
            public InjectedValue<ConnectionManager> getConnectionManagerInjector() {
                InjectedValue<ConnectionManager> cm = new InjectedValue<ConnectionManager>();
                cm.setValue(new ImmediateValue<ConnectionManager>(dcs));
                return cm;
            }
        };
        databaseCallbackHandler.handle(callbacks);
        VerifyPasswordCallback verifyPasswordCallback = (VerifyPasswordCallback) callbacks[1];
        Assert.assertEquals(true, verifyPasswordCallback.isVerified());
    }

    @Test
    public void testHandleDigestPassword() throws IOException, UnsupportedCallbackException {
        DigestHashCallback digestHashCallback = new DigestHashCallback("test");
        digestHashCallback.setHash(hashedPassword.getBytes());
        final Callback[] callbacks = {new NameCallback("test","Henry.Deacon"),digestHashCallback};
        initAuthenticationModel(false);
        databaseCallbackHandler = new DatabaseCallbackHandler(TEST_REALM, cNode) {
          @Override
            public InjectedValue<ConnectionManager> getConnectionManagerInjector() {
                InjectedValue<ConnectionManager> cm = new InjectedValue<ConnectionManager>();
                cm.setValue(new ImmediateValue<ConnectionManager>(dcs));
                return cm;
            }
        };
        databaseCallbackHandler.handle(callbacks);
        DigestHashCallback verifyPasswordCallback = (DigestHashCallback) callbacks[1];
        Assert.assertEquals(hashedPassword, verifyPasswordCallback.getHexHash());
    }

}

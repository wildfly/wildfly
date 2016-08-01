/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.webservices.config;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.URL;
import java.net.URLClassLoader;
import javax.management.MBeanServer;

import org.jboss.msc.value.ImmediateValue;
import org.jboss.wsf.spi.management.ServerConfig;
import org.jboss.wsf.spi.management.StackConfig;
import org.jboss.wsf.spi.management.StackConfigFactory;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author <a href="mailto:alessio.soldano@jboss.com>Alessio Soldano</a>
 */
public class ServerConfigImplTestCase {

    private static ClassLoader origTCCL;

    public ServerConfigImplTestCase() {
    }

    @Test
    public void testIsModifiable() throws Exception {
        ServerConfigImpl sc = newServerConfigImpl();
        sc.create();
        assertTrue(sc.isModifiable());
        sc.incrementWSDeploymentCount();
        assertFalse(sc.isModifiable());
        sc.decrementWSDeploymentCount();
        assertTrue(sc.isModifiable());
        sc.incrementWSDeploymentCount();
        sc.incrementWSDeploymentCount();
        assertFalse(sc.isModifiable());
        sc.create();
        assertTrue(sc.isModifiable());
    }

    @Test
    public void testSingleAttributeUpdate() throws Exception {
        internalTestSingleAttributeUpdate(new Callback() {
            @Override
            public void setAttribute(ServerConfig sc) throws Exception {
                sc.setModifySOAPAddress(true);
            }
        });
        internalTestSingleAttributeUpdate(new Callback() {
            @Override
            public void setAttribute(ServerConfig sc) throws Exception {
                sc.setWebServiceHost("foo");
            }
        });
        internalTestSingleAttributeUpdate(new Callback() {
            @Override
            public void setAttribute(ServerConfig sc) throws Exception {
                sc.setWebServicePort(976);
            }
        });
        internalTestSingleAttributeUpdate(new Callback() {
            @Override
            public void setAttribute(ServerConfig sc) throws Exception {
                sc.setWebServiceSecurePort(5435);
            }
        });
        internalTestSingleAttributeUpdate(new Callback() {
            @Override
            public void setAttribute(ServerConfig sc) throws Exception {
                sc.setWebServicePathRewriteRule("MY/TEST/PATH");
            }
        });
    }

    @Test
    public void testMultipleAttributesUpdate() throws Exception {
        Callback cbA = new Callback() {
            @Override
            public void setAttribute(ServerConfig sc) throws Exception {
                sc.setModifySOAPAddress(true);
            }
        };
        Callback cbB = new Callback() {
            @Override
            public void setAttribute(ServerConfig sc) throws Exception {
                sc.setWebServiceHost("foo");
            }
        };
        Callback cbC = new Callback() {
            @Override
            public void setAttribute(ServerConfig sc) throws Exception {
                sc.setWebServicePort(976);
            }
        };
        Callback cbD = new Callback() {
            @Override
            public void setAttribute(ServerConfig sc) throws Exception {
                sc.setWebServiceSecurePort(5435);
            }
        };
        Callback cbE = new Callback() {
            @Override
            public void setAttribute(ServerConfig sc) throws Exception {
                sc.setWebServicePathRewriteRule("MY/TEST/PATH");
            }
        };
        internalTestMultipleAttributeUpdate(cbA, new Callback[]{cbB, cbC, cbD, cbE});
        internalTestMultipleAttributeUpdate(cbB, new Callback[]{cbA, cbC, cbD, cbE});
        internalTestMultipleAttributeUpdate(cbC, new Callback[]{cbA, cbB, cbD, cbE});
        internalTestMultipleAttributeUpdate(cbD, new Callback[]{cbA, cbB, cbC, cbE});
        internalTestMultipleAttributeUpdate(cbE, new Callback[]{cbA, cbB, cbC, cbD});
    }

    protected void internalTestSingleAttributeUpdate(Callback cb) throws Exception {
        ServerConfigImpl sc = newServerConfigImpl();
        sc.create();
        assertTrue(sc.isModifiable());
        cb.setAttribute(sc);
        sc.incrementWSDeploymentCount();
        assertFalse(sc.isModifiable());
        try {
            cb.setAttribute(sc);
            fail();
        } catch (DisabledOperationException e) {
            //check the error message says the operation can't be done because there's an active deployment
            assertTrue("Expected WFLYWS0064 message, but got " + e.getMessage(), e.getMessage().contains("WFLYWS0064"));
        }
        sc.decrementWSDeploymentCount();
        assertTrue(sc.isModifiable());
        try {
            cb.setAttribute(sc);
            fail();
        } catch (DisabledOperationException e) {
            //check the error message says the operation can't be done because of pending former model update(s) requiring reload
            assertTrue("Expected WFLYWS0063 message, but got " + e.getMessage(), e.getMessage().contains("WFLYWS0063"));
        }
        sc.create();
        assertTrue(sc.isModifiable());
        cb.setAttribute(sc);
    }

    protected void internalTestMultipleAttributeUpdate(Callback cb1, Callback[] otherCbs) throws Exception {
        ServerConfigImpl sc = newServerConfigImpl();
        sc.create();
        assertTrue(sc.isModifiable());
        cb1.setAttribute(sc);
        sc.incrementWSDeploymentCount();
        assertFalse(sc.isModifiable());
        try {
            cb1.setAttribute(sc);
            fail();
        } catch (DisabledOperationException e) {
            //check the error message says the operation can't be done because there's an active deployment
            assertTrue("Expected WFLYWS0064 message, but got " + e.getMessage(), e.getMessage().contains("WFLYWS0064"));
        }
        sc.decrementWSDeploymentCount();
        assertTrue(sc.isModifiable());
        try {
            cb1.setAttribute(sc);
            fail();
        } catch (DisabledOperationException e) {
            //check the error message says the operation can't be done because of pending former model update(s) requiring reload
            assertTrue("Expected WFLYWS0063 message, but got " + e.getMessage(), e.getMessage().contains("WFLYWS0063"));
        }
        //other attributes are still modified properly as they're still in synch
        for (Callback cb : otherCbs) {
            cb.setAttribute(sc);
        }
    }

    private static ServerConfigImpl newServerConfigImpl() {
        ServerConfigImpl sc = ServerConfigImpl.newInstance();
        sc.getMBeanServerInjector().setValue(new ImmediateValue<MBeanServer>(null));
        return sc;
    }

    @BeforeClass
    public static void setStackConfigFactory() throws Exception {
        URL url = ServerConfigImplTestCase.class.getResource("util/");
        origTCCL = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(new URLClassLoader(new URL[]{url}));
    }

    @AfterClass
    public static void restoreStackConfigFactory() {
        Thread.currentThread().setContextClassLoader(origTCCL);
        origTCCL = null;
    }

    public interface Callback {
        void setAttribute(ServerConfig sc) throws Exception;
    }

    public static class TestStackConfigFactory extends StackConfigFactory {
        @Override
        public StackConfig getStackConfig() {
            return new TestStackConfig();
        }
    }

    public static class TestStackConfig implements StackConfig {
        @Override
        public String getImplementationTitle() {
            return null;
        }

        @Override
        public String getImplementationVersion() {
            return null;
        }

        public void validatePathRewriteRule(String rule) {
            //NOOP
        }
    }
}

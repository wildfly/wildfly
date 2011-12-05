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
package org.jboss.as.ee.component.interceptors;

/**
 * Class that maintains interceptor ordering for various interceptor chains
 *
 * @author Stuart Douglas
 */
public class
        InterceptorOrder {

    private InterceptorOrder() {

    }


    public static final class Component {


        public static final int INITIAL_INTERCEPTOR                                         = 0x100;
        public static final int TCCL_INTERCEPTOR                                            = 0x200;
        public static final int JNDI_NAMESPACE_INTERCEPTOR                                  = 0x300;
        public static final int TIMEOUT_INVOCATION_CONTEXT_INTERCEPTOR                      = 0x310;
        public static final int CDI_REQUEST_SCOPE                                           = 0x320;
        public static final int BMT_TRANSACTION_INTERCEPTOR                                 = 0x400;
        public static final int COMPONENT_CMT_INTERCEPTOR                                   = 0x410;
        public static final int SYNCHRONIZATION_INTERCEPTOR                                 = 0x500;
        public static final int REENTRANCY_INTERCEPTOR                                      = 0x501;
        public static final int ENTITY_BEAN_REMOVE_INTERCEPTOR                              = 0x502;
        public static final int JPA_SESSION_BEAN_INTERCEPTOR                                = 0x600;
        public static final int SINGLETON_CONTAINER_MANAGED_CONCURRENCY_INTERCEPTOR         = 0x700;
        public static final int CMP_RELATIONSHIP_INTERCEPTOR                                = 0x800;
        // JSR 109 - Version 1.3 - 6.2.2.4 Security
        // For EJB based service implementations, Handlers run after method level authorization has occurred.
        // JSR 109 - Version 1.3 - 6.2.2.5 Transaction
        // Handlers run under the transaction context of the component they are associated with.
        public static final int WS_HANDLERS_INTERCEPTOR                                     = 0x900;

        /**
         * All user level interceptors are added with the same priority, so they execute
         * in the order that they are added.
         */
        public static final int USER_INTERCEPTORS                                           = 0xA00;
        public static final int CDI_INTERCEPTORS                                            = 0xB00;
        public static final int TERMINAL_INTERCEPTOR                                        = 0xC00;

        private Component() {
        }

    }

    public static final class ComponentPostConstruct {

        public static final int TCCL_INTERCEPTOR = 0x100;
        public static final int EJB_SESSION_CONTEXT_INTERCEPTOR = 0x200;
        public static final int TRANSACTION_INTERCEPTOR = 0x300;
        public static final int JPA_SFSB_PRE_CREATE = 0x400;
        public static final int JNDI_NAMESPACE_INTERCEPTOR = 0x500;
        public static final int INSTANTIATION_INTERCEPTORS = 0x600;
        public static final int RESOURCE_INJECTION_INTERCEPTORS = 0x700;
        public static final int EJB_SET_CONTEXT_METHOD_INVOCATION_INTERCEPTOR = 0x800;
        public static final int WELD_INJECTION = 0x900;
        public static final int JPA_SFSB_CREATE = 0xA00;
        public static final int DEPENDENCY_INJECTION_COMPLETE = 0xA50;
        public static final int USER_INTERCEPTORS = 0xB00;
        public static final int CDI_INTERCEPTORS = 0xC00;
        public static final int SFSB_INIT_METHOD = 0xD00;
        public static final int SETUP_CONTEXT = 0xE00;
        public static final int TERMINAL_INTERCEPTOR = 0xF00;

        private ComponentPostConstruct() {
        }

    }

    public static final class ComponentPreDestroy {

        public static final int TCCL_INTERCEPTOR = 0x100;
        public static final int EJB_SESSION_CONTEXT_INTERCEPTOR = 0x200;
        public static final int TRANSACTION_INTERCEPTOR = 0x300;
        public static final int JNDI_NAMESPACE_INTERCEPTOR = 0x400;
        public static final int JPA_SFSB_DESTROY = 0x500;
        public static final int UNINJECTION_INTERCEPTORS = 0x600;
        public static final int DESTRUCTION_INTERCEPTORS = 0x700;
        public static final int USER_INTERCEPTORS = 0x800;
        public static final int CDI_INTERCEPTORS = 0x900;
        public static final int TERMINAL_INTERCEPTOR = 0xA00;

        private ComponentPreDestroy() {
        }

    }

    public static final class View {
        public static final int EJB_EXCEPTION_LOGGING_INTERCEPTOR                       = 0x000;
        public static final int TCCL_INTERCEPTOR                                        = 0x001;
        public static final int EJB_IIOP_TRANSACTION                                    = 0x020;
        public static final int JNDI_NAMESPACE_INTERCEPTOR                              = 0x050;
        public static final int NOT_BUSINESS_METHOD_EXCEPTION                           = 0x100;
        public static final int REMOTE_EXCEPTION_TRANSFORMER                            = 0x200;
        public static final int INVALID_METHOD_EXCEPTION                                = 0x201;
        public static final int SECURITY_CONTEXT                                        = 0x250;
        public static final int EJB_SECURITY_AUTHORIZATION_INTERCEPTOR                  = 0x300;
        public static final int INVOCATION_CONTEXT_INTERCEPTOR                          = 0x400;
        // should happen before the CMT/BMT interceptors
        public static final int REMOTE_TRANSACTION_PROPOGATION_INTERCEPTOR              = 0x450;
        public static final int CMT_TRANSACTION_INTERCEPTOR                             = 0x500;
        public static final int HOME_METHOD_INTERCEPTOR                                 = 0x600;
        public static final int ASSOCIATING_INTERCEPTOR                                 = 0x700;
        public static final int JPA_SFSB_INTERCEPTOR                                    = 0x800;
        public static final int SESSION_REMOVE_INTERCEPTOR                              = 0x900;
        public static final int COMPONENT_DISPATCHER                                    = 0xA00;


        private View() {
        }
    }

    public static final class Client {

        public static final int TO_STRING = 0x100;
        public static final int LOCAL_ASYNC_INVOCATION = 0x200;
        public static final int ASSOCIATING_INTERCEPTOR = 0x300;
        public static final int EJB_EQUALS_HASHCODE = 0x400;
        public static final int WRITE_REPLACE = 0x500;
        public static final int CLIENT_DISPATCHER = 0x600;

        private Client() {
        }
    }

    public static final class ClientPreDestroy {

        public static final int INSTANCE_DESTROY = 0x100;
        public static final int TERMINAL_INTERCEPTOR = 0x200;

        private ClientPreDestroy() {
        }
    }

    public static final class ClientPostConstruct {

        public static final int INSTANCE_CREATE      = 0x100;
        public static final int TERMINAL_INTERCEPTOR = 0x200;

        private ClientPostConstruct() {
        }
    }

}

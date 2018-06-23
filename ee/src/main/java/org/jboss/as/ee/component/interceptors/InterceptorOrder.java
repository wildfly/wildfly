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
public class InterceptorOrder {

    private InterceptorOrder() {

    }


    public static final class Component {

        public static final int INITIAL_INTERCEPTOR = 0x100;
        public static final int CONCURRENT_CONTEXT = 0x180;
        public static final int SYNCHRONIZATION_INTERCEPTOR = 0x500;
        public static final int REENTRANCY_INTERCEPTOR = 0x501;
        public static final int BMT_TRANSACTION_INTERCEPTOR = 0x520;
        public static final int ENTITY_BEAN_REMOVE_INTERCEPTOR = 0x550;
        public static final int JPA_SFSB_INTERCEPTOR = 0x560;
        public static final int JPA_SESSION_BEAN_INTERCEPTOR = 0x600;
        public static final int CMP_RELATIONSHIP_INTERCEPTOR = 0x800;
        // WS handlers, user and CDI interceptors plus the bean method are considered user execution time
        public static final int EJB_EXECUTION_TIME_INTERCEPTOR = 0x850;
        // JSR 109 - Version 1.3 - 6.2.2.4 Security
        // For EJB based service implementations, Handlers run after method level authorization has occurred.
        // JSR 109 - Version 1.3 - 6.2.2.5 Transaction
        // Handlers run under the transaction context of the component they are associated with.
        public static final int WS_HANDLERS_INTERCEPTOR = 0x900;
        public static final int XTS_INTERCEPTOR = 0x901;

        /**
         * All user level interceptors are added with the same priority, so they execute
         * in the order that they are added.
         */
        public static final int INTERCEPTOR_USER_INTERCEPTORS = 0xA00;
        /**
         * @Around* methods defined on the component class or its superclasses
         */
        public static final int CDI_INTERCEPTORS = 0xB00;
        public static final int COMPONENT_USER_INTERCEPTORS = 0xC00; //interceptors defined on the component class, these have to run after CDI interceptors

        public static final int TERMINAL_INTERCEPTOR = 0xD00;

        private Component() {
        }

    }


    public static final class AroundConstruct {

        public static final int CONSTRUCTION_START_INTERCEPTOR = 0xA00;
        public static final int INTERCEPTOR_AROUND_CONSTRUCT = 0xB00;
        public static final int WELD_AROUND_CONSTRUCT_INTERCEPTORS = 0xC00;
        public static final int CONSTRUCT_COMPONENT = 0xD00;
        public static final int TERMINAL_INTERCEPTOR = 0xE00;

        private AroundConstruct() {
        }

    }


    public static final class ComponentPostConstruct {

        public static final int STARTUP_COUNTDOWN_INTERCEPTOR = 0x050;
        public static final int TCCL_INTERCEPTOR = 0x100;
        public static final int CONCURRENT_CONTEXT = 0x180;
        public static final int EJB_SESSION_CONTEXT_INTERCEPTOR = 0x200;
        public static final int WELD_INJECTION_CONTEXT_INTERCEPTOR = 0x300;
        public static final int JPA_SFSB_PRE_CREATE = 0x400;
        public static final int TRANSACTION_INTERCEPTOR = 0x500;
        public static final int JNDI_NAMESPACE_INTERCEPTOR = 0x600;
        public static final int CREATE_CDI_INTERCEPTORS = 0x0680;
        public static final int INTERCEPTOR_INSTANTIATION_INTERCEPTORS = 0x700;
        public static final int INTERCEPTOR_RESOURCE_INJECTION_INTERCEPTORS = 0x800;
        public static final int INTERCEPTOR_WELD_INJECTION = 0x900;
        public static final int AROUND_CONSTRUCT_CHAIN = 0xA00;
        public static final int COMPONENT_RESOURCE_INJECTION_INTERCEPTORS = 0xB00;
        public static final int EJB_SET_CONTEXT_METHOD_INVOCATION_INTERCEPTOR = 0xC00;
        public static final int COMPONENT_WELD_INJECTION = 0xD00;
        public static final int JPA_SFSB_CREATE = 0xE00;
        public static final int REQUEST_SCOPE_ACTIVATING_INTERCEPTOR = 0xE80;
        public static final int INTERCEPTOR_USER_INTERCEPTORS = 0xF00;
        public static final int CDI_INTERCEPTORS = 0x1000;
        public static final int COMPONENT_USER_INTERCEPTORS = 0x1100;
        public static final int SFSB_INIT_METHOD = 0x1200;
        public static final int SETUP_CONTEXT = 0x1300;
        public static final int TERMINAL_INTERCEPTOR = 0x1400;

        private ComponentPostConstruct() {
        }

    }

    public static final class ComponentPreDestroy {

        public static final int TCCL_INTERCEPTOR = 0x100;
        public static final int CONCURRENT_CONTEXT = 0x180;
        public static final int EJB_SESSION_CONTEXT_INTERCEPTOR = 0x200;
        public static final int TRANSACTION_INTERCEPTOR = 0x300;
        public static final int JNDI_NAMESPACE_INTERCEPTOR = 0x400;
        public static final int JPA_SFSB_DESTROY = 0x500;
        public static final int INTERCEPTOR_UNINJECTION_INTERCEPTORS = 0x600;
        public static final int COMPONENT_UNINJECTION_INTERCEPTORS = 0x700;
        public static final int INTERCEPTOR_DESTRUCTION_INTERCEPTORS = 0x800;
        public static final int COMPONENT_DESTRUCTION_INTERCEPTORS = 0x900;
        public static final int INTERCEPTOR_USER_INTERCEPTORS = 0xA00;
        public static final int CDI_INTERCEPTORS = 0xB00;
        public static final int COMPONENT_USER_INTERCEPTORS = 0xC00;
        public static final int TERMINAL_INTERCEPTOR = 0xD00;

        private ComponentPreDestroy() {
        }

    }

    public static final class ComponentPassivation {

        public static final int TCCL_INTERCEPTOR = 0x100;
        public static final int CONCURRENT_CONTEXT = 0x180;
        public static final int EJB_SESSION_CONTEXT_INTERCEPTOR = 0x200;
        public static final int TRANSACTION_INTERCEPTOR = 0x300;
        public static final int JNDI_NAMESPACE_INTERCEPTOR = 0x400;
        public static final int INTERCEPTOR_USER_INTERCEPTORS = 0x500;
        public static final int CDI_INTERCEPTORS = 0x600;
        public static final int COMPONENT_USER_INTERCEPTORS = 0x700;
        public static final int TERMINAL_INTERCEPTOR = 0x800;

        private ComponentPassivation() {
        }

    }

    public static final class View {
        public static final int CHECKING_INTERCEPTOR = 1;
        public static final int TCCL_INTERCEPTOR = 0x003;
        public static final int INVOCATION_TYPE = 0x005;
        public static final int EJB_IIOP_TRANSACTION = 0x020;
        public static final int JNDI_NAMESPACE_INTERCEPTOR = 0x050;
        public static final int REMOTE_EXCEPTION_TRANSFORMER = 0x200;
        public static final int EJB_EXCEPTION_LOGGING_INTERCEPTOR = 0x210;
        public static final int GRACEFUL_SHUTDOWN = 0x218;
        public static final int SHUTDOWN_INTERCEPTOR = 0x220;
        public static final int INVALID_METHOD_EXCEPTION = 0x230;
        public static final int STARTUP_AWAIT_INTERCEPTOR = 0x248;
        public static final int SINGLETON_CONTAINER_MANAGED_CONCURRENCY_INTERCEPTOR = 0x240;
        // Allows users to specify user application specific "container interceptors" which run before the
        // other JBoss specific container interceptors like the security interceptor
        public static final int USER_APP_SPECIFIC_CONTAINER_INTERCEPTORS = 0x249;
        public static final int SECURITY_CONTEXT = 0x250;
        public static final int POLICY_CONTEXT = 0x260;
        public static final int SECURITY_ROLES = 0x270;
        public static final int EJB_SECURITY_AUTHORIZATION_INTERCEPTOR = 0x300;
        public static final int RUN_AS_PRINCIPAL = 0x310;
        public static final int EXTRA_PRINCIPAL_ROLES = 0x320;
        public static final int RUN_AS_ROLE = 0x330;
        public static final int SECURITY_IDENTITY_OUTFLOW = 0x340;
        // after security we take note of the invocation
        public static final int EJB_WAIT_TIME_INTERCEPTOR = 0x350;
        public static final int INVOCATION_CONTEXT_INTERCEPTOR = 0x400;
        // should happen before the CMT/BMT interceptors
        /**
         * @deprecated Remove this field once WFLY-7860 is resolved.
         */
        @Deprecated
        public static final int REMOTE_TRANSACTION_PROPAGATION_INTERCEPTOR = 0x450;
        public static final int CDI_REQUEST_SCOPE = 0x480;
        public static final int CMT_TRANSACTION_INTERCEPTOR = 0x500;
       public static final int EE_SETUP = 0x510;
        public static final int HOME_METHOD_INTERCEPTOR = 0x600;
        public static final int ASSOCIATING_INTERCEPTOR = 0x700;
        public static final int XTS_INTERCEPTOR = 0x701;
        public static final int SESSION_REMOVE_INTERCEPTOR = 0x900;
        public static final int COMPONENT_DISPATCHER = 0xA00;


        private View() {
        }
    }

    public static final class Client {

        public static final int TO_STRING = 0x100;
        public static final int NOT_BUSINESS_METHOD_EXCEPTION = 0x110;
        public static final int LOCAL_ASYNC_LOG_SAVE = 0x180;
        public static final int LOCAL_ASYNC_SECURITY_CONTEXT = 0x190;
        public static final int LOCAL_ASYNC_INVOCATION = 0x200;
        public static final int LOCAL_ASYNC_LOG_RESTORE = 0x280;
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

        public static final int INSTANCE_CREATE = 0x100;
        public static final int TERMINAL_INTERCEPTOR = 0x200;

        private ClientPostConstruct() {
        }
    }

}

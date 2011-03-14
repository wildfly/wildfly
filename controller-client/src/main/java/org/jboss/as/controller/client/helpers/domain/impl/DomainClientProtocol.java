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

package org.jboss.as.controller.client.helpers.domain.impl;

/**
 * @author John Bailey
 */
public interface DomainClientProtocol {

    int DOMAIN_CONTROLLER_CLIENT_REQUEST = 0x0A;

    int RETURN_DOMAIN_MODEL = 0x13;
    int PARAM_DOMAIN_MODEL_UPDATE = 0x14;
    int GET_DOMAIN_REQUEST = 0x15;
    int GET_DOMAIN_RESPONSE = 0x16;
    int APPLY_UPDATES_REQUEST = 0x17;
    int PARAM_APPLY_UPDATES_UPDATE_COUNT = 0x18;
    int RETURN_APPLY_UPDATES_RESULT_COUNT = 0x19;
    int RETURN_APPLY_UPDATE = 0x20;
    int APPLY_UPDATES_RESPONSE = 0x21;
    int APPLY_UPDATE_REQUEST = 0x22;
    int APPLY_UPDATE_RESPONSE = 0x23;
    int APPLY_SERVER_MODEL_UPDATE_REQUEST = 0x24;
    int PARAM_HOST_NAME = 0x25;
    int PARAM_SERVER_GROUP_NAME = 0x26;
    int PARAM_SERVER_NAME = 0x27;
    int PARAM_SERVER_MODEL_UPDATE = 0x28;
    int RETURN_APPLY_SERVER_MODEL_UPDATE = 0x29;
    int APPLY_SERVER_MODEL_UPDATE_RESPONSE = 0x30;
    int ADD_DEPLOYMENT_CONTENT_REQUEST = 0x31;
    int PARAM_DEPLOYMENT_NAME = 0x32;
    int PARAM_DEPLOYMENT_RUNTIME_NAME = 0x33;
    int PARAM_DEPLOYMENT_CONTENT = 0x34;
    int RETURN_DEPLOYMENT_HASH_LENGTH = 0x35;
    int RETURN_DEPLOYMENT_HASH = 0x36;
    int ADD_DEPLOYMENT_CONTENT_RESPONSE = 0x37;
    int CHECK_UNIQUE_DEPLOYMENT_NAME_REQUEST = 0x38;
    int PARAM_DEPLOYMENT_NAME_UNIQUE = 0x39;
    int CHECK_UNIQUE_DEPLOYMENT_NAME_RESPONSE = 0x40;
    int EXECUTE_DEPLOYMENT_PLAN_REQUEST = 0x41;
    int PARAM_DEPLOYMENT_PLAN = 0x42;
    int RETURN_DEPLOYMENT_PLAN_ID = 0x43;
    int RETURN_DEPLOYMENT_PLAN_INVALID = 0x44;
    int RETURN_DEPLOYMENT_SET_ID = 0x45;
    int RETURN_DEPLOYMENT_ACTION_ID = 0x46;
    int RETURN_DEPLOYMENT_ACTION_MODEL_RESULT = 0x47;
    int RETURN_SERVER_DEPLOYMENT = 0x48;
    int RETURN_HOST_NAME = 0x49;
    int RETURN_SERVER_GROUP_NAME = 0x50;
    int RETURN_SERVER_NAME = 0x51;
    int RETURN_SERVER_DEPLOYMENT_RESULT = 0x52;
    int RETURN_DEPLOYMENT_SET_ROLLBACK = 0x53;
    int RETURN_DEPLOYMENT_ACTION_MODEL_ROLLBACK = 0x54;
    int RETURN_SERVER_DEPLOYMENT_ROLLBACK = 0x55;
    int RETURN_DEPLOYMENT_PLAN_COMPLETE = 0x56;
    int EXECUTE_DEPLOYMENT_PLAN_RESPONSE = 0x57;
    int APPLY_HOST_UPDATES_REQUEST = 0x58;
    int PARAM_HOST_MODEL_UPDATE = 0x59;
    int RETURN_APPLY_HOST_UPDATE = 0x60;
    int APPLY_HOST_UPDATES_RESPONSE = 0x61;
    int GET_HOST_MODEL_REQUEST = 0x62;
    int RETURN_HOST_MODEL = 0x63;
    int GET_HOST_MODEL_RESPONSE = 0x64;
    int GET_HOST_CONTROLLER_NAMES_REQUEST = 0x65;
    int RETURN_HOST_CONTROLLER_COUNT = 0x66;
    int GET_HOST_CONTROLLER_NAMES_RESPONSE = 0x67;
    int GET_SERVER_MODEL_REQUEST = 0x68;
    int RETURN_SERVER_MODEL = 0x69;
    int GET_SERVER_MODEL_RESPONSE = 0x70;
    int GET_SERVER_STATUSES_REQUEST = 0x71;
    int RETURN_SERVER_STATUS_COUNT = 0x72;
    int RETURN_SERVER_STATUS = 0x73;
    int GET_SERVER_STATUSES_RESPONSE = 0x74;
    int START_SERVER_REQUEST = 0x75;
    int START_SERVER_RESPONSE = 0x76;
    int STOP_SERVER_REQUEST = 0x77;
    int PARAM_GRACEFUL_TIMEOUT = 0x78;
    int STOP_SERVER_RESPONSE = 0x79;
    int RESTART_SERVER_REQUEST = 0x7A;
    int RESTART_SERVER_RESPONSE = 0x7B;
}

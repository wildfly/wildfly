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

package org.jboss.as.domain.client.impl;

/**
 * @author John Bailey
 */
public interface DomainClientProtocol {

    int DOMAIN_CONTROLLER_CLIENT_REQUEST = 0x0A;

    int PARAM_DOMAIN_MODEL = 0x13;
    int PARAM_DOMAIN_MODEL_UPDATE = 0x30;
    int GET_DOMAIN_REQUEST = 0x44;
    int GET_DOMAIN_RESPONSE = 0x45;
    int APPLY_UPDATES_REQUEST = 0x46;
    int PARAM_APPLY_UPDATES_RESULT_COUNT = 0x47;
    int PARAM_APPLY_UPDATE_RESULT = 0x48;
    int APPLY_UPDATE_RESULT_DOMAIN_MODEL_SUCCESS = 0x00;
    int PARAM_APPLY_UPDATE_RESULT_HOST_FAILURE_COUNT = 0x01;
    int PARAM_APPLY_UPDATE_RESULT_SERVER_FAILURE_COUNT = 0x01;
    int PARAM_APPLY_UPDATE_RESULT_SERVER_RESULT_COUNT = 0x02;
    int PARAM_HOST_NAME = 0x03;
    int PARAM_SERVER_GROUP_NAME = 0x04;
    int PARAM_SERVER_NAME = 0x05;
    int PARAM_APPLY_SERVER_MODEL_UPDATE_CANCELLED = 0x06;
    int PARAM_APPLY_SERVER_MODEL_UPDATE_TIMED_OUT = 0x07;
    int PARAM_APPLY_SERVER_MODEL_UPDATE_RESULT_RETURN = 0x49;
    int PARAM_APPLY_UPDATE_RESULT_EXCEPTION = 0x50;
    int APPLY_UPDATES_RESPONSE = 0x51;
    int EXECUTE_DEPLOYMENT_PLAN_REQUEST = 0x52;
    int PARAM_DEPLOYMENT_PLAN = 0x53;
    int PARAM_DEPLOYMENT_PLAN_RESULT = 0x54;
    int EXECUTE_DEPLOYMENT_PLAN_RESPONSE = 0x55;
    int ADD_DEPLOYMENT_CONTENT_REQUEST = 0x56;
    int PARAM_DEPLOYMENT_NAME = 0x57;
    int PARAM_DEPLOYMENT_RUNTIME_NAME = 0x58;
    int PARAM_DEPLOYMENT_CONTENT = 0x59;
    int PARAM_DEPLOYMENT_HASH_LENGTH = 0x60;
    int PARAM_DEPLOYMENT_HASH = 0x61;
    int ADD_DEPLOYMENT_CONTENT_RESPONSE = 0x62;
    int CHECK_UNIQUE_DEPLOYMENT_NAME_REQUEST = 0x63;
    int PARAM_DEPLOYMENT_NAME_UNIQUE = 0x64;
    int CHECK_UNIQUE_DEPLOYMENT_NAME_RESPONSE = 0x65;
    int APPLY_UPDATE_REQUEST = 0x66;
    int PARAM_APPLY_UPDATE_RESULT_SERVER_COUNT = 0x67;
    int APPLY_UPDATE_RESPONSE = 0x68;
    int APPLY_SERVER_MODEL_UPDATE_REQUEST = 0x69;
    int PARAM_SERVER_MODEL_UPDATE = 0x70;
    int APPLY_SERVER_MODEL_UPDATE_RESPONSE = 0x71;
}

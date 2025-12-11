/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.web.websocket;

import jakarta.annotation.Resource;
import jakarta.enterprise.concurrent.ManagedExecutorService;
import jakarta.inject.Inject;
import jakarta.websocket.OnMessage;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;

/**
 * @author Stuart Douglas
 */
@ServerEndpoint("/websocket/{name}")
public class AnnotatedEndpoint {

    public static final String MESSAGE_OK = "OK";
    public static final String MESSAGE_NOT_OK = "BAD";

    @Resource
    ManagedExecutorService mes;

    @Inject
    SimpleBean bean;

    @OnMessage
    public String message(String message, @PathParam("name") String name) {
        if(name != null && name.equals("mes")) {
            if(this.mes == null) {
                return MESSAGE_NOT_OK;
            } else {
                return MESSAGE_OK;
            }
        } else if(name != null && name.equals("bean")){
            if(this.bean == null) {
                return MESSAGE_NOT_OK;
            } else {
                return MESSAGE_OK;
            }
        } else {
            return message + " " + name;
        }
    }

}

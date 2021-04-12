/*
 * Copyright 2021 JBoss by Red Hat.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wildfly.extension.microprofile.opentracing.resolver;

import io.jaegertracing.Configuration;
import io.opentracing.propagation.Format;
import org.jboss.as.controller.ExpressionResolver;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ValueExpression;
import org.junit.Assert;
import org.junit.Test;

/**
 * Testing configuration creation
 *
 * @author Emmanuel Hugonnet (c) 2021 Red Hat, Inc.
 */
public class JaegerTracerConfigurationTest {

    @Test
    public void testModelResolution() throws OperationFailedException {
        ModelNode operation = Operations.createAddOperation(PathAddress
                .pathAddress("subsystem", "microprofile-opentracing-smallrye")
                .append("jaeger-tracer", "jaeger")
                .toModelNode());
        operation.get("propagation").add("B3").add(new ValueExpression("${jaeger.propagation:JAEGER}"));

        operation.get("sampler-type").set(new ValueExpression("${jaeger.sampler.type:const}"));
        operation.get("sampler-param").set(new ValueExpression("${jaeger.sampler-param:0.8}"));
        operation.get("sampler-manager-host-port").set(new ValueExpression("${jaeger.sampler-manager-host-port:localhost:4321}"));

        operation.get("sender-endpoint").set(new ValueExpression("${jaeger.sender.endpoint:http://localhost:14268/api/traces}"));
        operation.get("sender-auth-token").set(new ValueExpression("${jaeger.sender.auth-token:myAuthToken}"));
        operation.get("sender-auth-user").set(new ValueExpression("${jaeger.sender.user:sender}"));
        operation.get("sender-auth-password").set(new ValueExpression("${jaeger.sender.password:senderPassword}"));

        operation.get("reporter-log-spans").set(new ValueExpression("${jaeger.reporter.log-spans:false}"));
        operation.get("reporter-flush-interval").set(new ValueExpression("${jaeger.reporter.flush-interval:5}"));
        operation.get("reporter-max-queue-size").set(new ValueExpression("${jaeger.reporter.max-queue-size:10}"));

        operation.get("tracer_id_128bit").set(new ValueExpression("${jaeger.tracer_id_128bit:true}"));
        operation.get("tags").add("test", "${jaeger.reporter.max-queue-sizesimple}");
        JaegerTracerConfiguration tracerConfiguration = new JaegerTracerConfiguration(ExpressionResolver.TEST_RESOLVER, "jaeger", operation, () -> null);
        Configuration config = tracerConfiguration.createConfiguration("myApplication.war");

        Assert.assertEquals(2, config.getCodec().getCodecs().size());
        Assert.assertEquals(1, config.getCodec().getBinaryCodecs().size());
        Assert.assertEquals(2, config.getCodec().getCodecs().get(Format.Builtin.HTTP_HEADERS).size());
        Assert.assertEquals(2, config.getCodec().getCodecs().get(Format.Builtin.TEXT_MAP).size());
        Assert.assertEquals(1, config.getCodec().getBinaryCodecs().get(Format.Builtin.BINARY).size());

        Assert.assertEquals("const", config.getSampler().getType());
        Assert.assertEquals(0.8d, config.getSampler().getParam().doubleValue(), 0.01);
        Assert.assertEquals("localhost:4321", config.getSampler().getManagerHostPort());

        Assert.assertEquals("http://localhost:14268/api/traces", config.getReporter().getSenderConfiguration().getEndpoint());
        Assert.assertEquals("myAuthToken", config.getReporter().getSenderConfiguration().getAuthToken());
        Assert.assertEquals("sender", config.getReporter().getSenderConfiguration().getAuthUsername());
        Assert.assertEquals("senderPassword", config.getReporter().getSenderConfiguration().getAuthPassword());

        Assert.assertEquals(false, config.getReporter().getLogSpans());
        Assert.assertEquals(5, config.getReporter().getFlushIntervalMs().intValue());
        Assert.assertEquals(10, config.getReporter().getMaxQueueSize().intValue());
    }

}

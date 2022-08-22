/*
 *  Copyright (c) 2022 The original author or authors
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of Apache License v2.0 which
 *  accompanies this distribution.
 *
 *       The Apache License v2.0 is available at
 *       http://www.opensource.org/licenses/apache2.0.php
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package org.wildfly.extension.vertx.deployment;

import io.vertx.core.Vertx;
import org.wildfly.extension.vertx.VertxProxy;
import org.wildfly.extension.vertx.VertxRegistry;

import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.InjectionPoint;

/**
 * CDI Producer which produce the Vertx instance by the member name.
 *
 * @author <a href="mailto:aoingl@gmail.com">Lin Gao</a>
 */
public class VertxProducer {

    @SuppressWarnings("unused")
    @Produces
    public Vertx getVertx(InjectionPoint ip) {
        String name = ip.getMember().getName();
        VertxProxy vp = VertxRegistry.getInstance().getByNameOrAlias(name);
        if (vp != null) {
            return vp.getVertx();
        }
        throw new RuntimeException("Cannot inject Vertx " + name + " in " + ip.getMember().getDeclaringClass());
    }

}

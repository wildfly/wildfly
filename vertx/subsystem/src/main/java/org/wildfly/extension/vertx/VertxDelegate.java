/*
 * Copyright (C) 2020 RedHat
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
package org.wildfly.extension.vertx;

import io.netty.channel.EventLoopGroup;
import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.TimeoutStream;
import io.vertx.core.Verticle;
import io.vertx.core.Vertx;
import io.vertx.core.WorkerExecutor;
import io.vertx.core.datagram.DatagramSocket;
import io.vertx.core.datagram.DatagramSocketOptions;
import io.vertx.core.dns.DnsClient;
import io.vertx.core.dns.DnsClientOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.file.FileSystem;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.NetClientOptions;
import io.vertx.core.net.NetServer;
import io.vertx.core.net.NetServerOptions;
import io.vertx.core.shareddata.SharedData;
import io.vertx.core.spi.VerticleFactory;

import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class VertxDelegate implements Vertx {

    private final Vertx vertx;

    public VertxDelegate(Vertx vertx) {
        this.vertx = vertx;
    }

    @Override
    public Context getOrCreateContext() {
        return this.vertx.getOrCreateContext();
    }

    @Override
    public NetServer createNetServer(NetServerOptions options) {
        return this.vertx.createNetServer(options);
    }

    @Override
    public NetServer createNetServer() {
        return this.vertx.createNetServer();
    }

    @Override
    public NetClient createNetClient(NetClientOptions options) {
        return this.vertx.createNetClient(options);
    }

    @Override
    public NetClient createNetClient() {
        return this.vertx.createNetClient();
    }

    @Override
    public HttpServer createHttpServer(HttpServerOptions options) {
        return this.vertx.createHttpServer(options);
    }

    @Override
    public HttpServer createHttpServer() {
        return this.vertx.createHttpServer();
    }

    @Override
    public HttpClient createHttpClient(HttpClientOptions options) {
        return this.vertx.createHttpClient(options);
    }

    @Override
    public HttpClient createHttpClient() {
        return this.vertx.createHttpClient();
    }

    @Override
    public DatagramSocket createDatagramSocket(DatagramSocketOptions options) {
        return this.vertx.createDatagramSocket(options);
    }

    @Override
    public DatagramSocket createDatagramSocket() {
        return this.vertx.createDatagramSocket();
    }

    @Override
    public FileSystem fileSystem() {
        return this.vertx.fileSystem();
    }

    @Override
    public EventBus eventBus() {
        return this.vertx.eventBus();
    }

    @Override
    public DnsClient createDnsClient(int port, String host) {
        return this.vertx.createDnsClient(port, host);
    }

    @Override
    public DnsClient createDnsClient() {
        return this.vertx.createDnsClient();
    }

    @Override
    public DnsClient createDnsClient(DnsClientOptions options) {
        return this.vertx.createDnsClient(options);
    }

    @Override
    public SharedData sharedData() {
        return this.vertx.sharedData();
    }

    @Override
    public long setTimer(long delay, Handler<Long> handler) {
        return this.vertx.setTimer(delay, handler);
    }

    @Override
    public TimeoutStream timerStream(long delay) {
        return this.vertx.timerStream(delay);
    }

    @Override
    public long setPeriodic(long delay, Handler<Long> handler) {
        return this.vertx.setPeriodic(delay, handler);
    }

    @Override
    public TimeoutStream periodicStream(long delay) {
        return this.vertx.periodicStream(delay);
    }

    @Override
    public boolean cancelTimer(long id) {
        return this.vertx.cancelTimer(id);
    }

    @Override
    public void runOnContext(Handler<Void> action) {
        this.vertx.runOnContext(action);
    }

    void closeInternal() {
        this.vertx.close();
    }

    @Override
    public Future<Void> close() {
        throw new UnsupportedOperationException("close() method not supported");
    }

    @Override
    public void close(Handler<AsyncResult<Void>> completionHandler) {
        throw new UnsupportedOperationException("close(completionHandler) method not supported");
    }

    @Override
    public Future<String> deployVerticle(Verticle verticle) {
        return this.vertx.deployVerticle(verticle);
    }

    @Override
    public void deployVerticle(Verticle verticle, Handler<AsyncResult<String>> completionHandler) {
        this.vertx.deployVerticle(verticle, completionHandler);
    }

    @Override
    public Future<String> deployVerticle(Verticle verticle, DeploymentOptions options) {
        return this.vertx.deployVerticle(verticle, options);
    }

    @Override
    public Future<String> deployVerticle(Class<? extends Verticle> verticleClass, DeploymentOptions options) {
        return this.vertx.deployVerticle(verticleClass, options);
    }

    @Override
    public Future<String> deployVerticle(Supplier<Verticle> verticleSupplier, DeploymentOptions options) {
        return this.vertx.deployVerticle(verticleSupplier, options);
    }

    @Override
    public void deployVerticle(Verticle verticle, DeploymentOptions options, Handler<AsyncResult<String>> completionHandler) {
        this.vertx.deployVerticle(verticle, options, completionHandler);
    }

    @Override
    public void deployVerticle(Class<? extends Verticle> verticleClass, DeploymentOptions options, Handler<AsyncResult<String>> completionHandler) {
        this.vertx.deployVerticle(verticleClass, options, completionHandler);
    }

    @Override
    public void deployVerticle(Supplier<Verticle> verticleSupplier, DeploymentOptions options, Handler<AsyncResult<String>> completionHandler) {
        this.vertx.deployVerticle(verticleSupplier, options, completionHandler);
    }

    @Override
    public Future<String> deployVerticle(String name) {
        return this.vertx.deployVerticle(name);
    }

    @Override
    public void deployVerticle(String name, Handler<AsyncResult<String>> completionHandler) {
        this.vertx.deployVerticle(name, completionHandler);
    }

    @Override
    public Future<String> deployVerticle(String name, DeploymentOptions options) {
        return this.vertx.deployVerticle(name, options);
    }

    @Override
    public void deployVerticle(String name, DeploymentOptions options, Handler<AsyncResult<String>> completionHandler) {
        this.vertx.deployVerticle(name, options, completionHandler);
    }

    @Override
    public Future<Void> undeploy(String deploymentID) {
        return this.vertx.undeploy(deploymentID);
    }

    @Override
    public void undeploy(String deploymentID, Handler<AsyncResult<Void>> completionHandler) {
        this.vertx.undeploy(deploymentID, completionHandler);
    }

    @Override
    public Set<String> deploymentIDs() {
        return this.vertx.deploymentIDs();
    }

    @Override
    public void registerVerticleFactory(VerticleFactory factory) {
        this.vertx.registerVerticleFactory(factory);
    }

    @Override
    public void unregisterVerticleFactory(VerticleFactory factory) {
        this.vertx.unregisterVerticleFactory(factory);
    }

    @Override
    public Set<VerticleFactory> verticleFactories() {
        return this.vertx.verticleFactories();
    }

    @Override
    public boolean isClustered() {
        return this.vertx.isClustered();
    }

    @Override
    public <T> void executeBlocking(Handler<Promise<T>> blockingCodeHandler, boolean ordered, Handler<AsyncResult<T>> asyncResultHandler) {
        this.vertx.executeBlocking(blockingCodeHandler, ordered, asyncResultHandler);
    }

    @Override
    public <T> void executeBlocking(Handler<Promise<T>> blockingCodeHandler, Handler<AsyncResult<T>> asyncResultHandler) {
        this.vertx.executeBlocking(blockingCodeHandler, asyncResultHandler);
    }

    @Override
    public <T> Future<T> executeBlocking(Handler<Promise<T>> blockingCodeHandler, boolean ordered) {
        return this.vertx.executeBlocking(blockingCodeHandler, ordered);
    }

    @Override
    public <T> Future<T> executeBlocking(Handler<Promise<T>> blockingCodeHandler) {
        return this.vertx.executeBlocking(blockingCodeHandler);
    }

    @Override
    public EventLoopGroup nettyEventLoopGroup() {
        return this.vertx.nettyEventLoopGroup();
    }

    @Override
    public WorkerExecutor createSharedWorkerExecutor(String name) {
        return this.vertx.createSharedWorkerExecutor(name);
    }

    @Override
    public WorkerExecutor createSharedWorkerExecutor(String name, int poolSize) {
        return this.vertx.createSharedWorkerExecutor(name, poolSize);
    }

    @Override
    public WorkerExecutor createSharedWorkerExecutor(String name, int poolSize, long maxExecuteTime) {
        return this.vertx.createSharedWorkerExecutor(name, poolSize, maxExecuteTime);
    }

    @Override
    public WorkerExecutor createSharedWorkerExecutor(String name, int poolSize, long maxExecuteTime, TimeUnit maxExecuteTimeUnit) {
        return this.vertx.createSharedWorkerExecutor(name, poolSize, maxExecuteTime, maxExecuteTimeUnit);
    }

    @Override
    public boolean isNativeTransportEnabled() {
        return this.vertx.isNativeTransportEnabled();
    }

    @Override
    public Vertx exceptionHandler(Handler<Throwable> handler) {
        return this.vertx.exceptionHandler(handler);
    }

    @Override
    public Handler<Throwable> exceptionHandler() {
        return this.vertx.exceptionHandler();
    }
}

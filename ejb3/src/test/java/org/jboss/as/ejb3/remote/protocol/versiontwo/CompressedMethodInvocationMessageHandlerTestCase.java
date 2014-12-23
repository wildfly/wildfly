package org.jboss.as.ejb3.remote.protocol.versiontwo;

import org.jboss.as.ee.component.ComponentView;
import org.jboss.as.ejb3.deployment.DeploymentModuleIdentifier;
import org.jboss.as.ejb3.deployment.DeploymentRepository;
import org.jboss.as.ejb3.deployment.EjbDeploymentInformation;
import org.jboss.as.ejb3.deployment.ModuleDeployment;
import org.jboss.as.ejb3.remote.protocol.versionone.ChannelAssociation;
import org.jboss.ejb.client.EJBLocator;
import org.jboss.ejb.client.StatelessEJBLocator;
import org.jboss.ejb.client.annotation.CompressionHint;
import org.jboss.marshalling.ByteOutput;
import org.jboss.marshalling.Marshaller;
import org.jboss.marshalling.MarshallerFactory;
import org.jboss.marshalling.Marshalling;
import org.jboss.marshalling.MarshallingConfiguration;
import org.jboss.marshalling.reflect.SunReflectiveCreator;
import org.jboss.marshalling.river.RiverMarshallerFactory;
import org.jboss.msc.value.InjectedValue;
import org.jboss.msc.value.Value;
import org.jboss.remoting3.Channel;
import org.jboss.remoting3.MessageOutputStream;
import org.jboss.util.NotImplementedException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.ByteArrayOutputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.assertEquals;

/**
 * @author <a href="mailto:thofman@redhat.com">Tomas Hofman</a>
 */
public class CompressedMethodInvocationMessageHandlerTestCase {

    private final byte COMPRESSED_HEADER = 0x1b;
    private final byte UNCOMPRESSED_HEADER = 0x05;

    private MarshallerFactory marshallerFactory = new RiverMarshallerFactory();
    private ExecutorService executorService = new SynchronousExecutor();
    private CompressedMethodInvocationMessageHandler handler;

    @Before
    public void beforeTest() throws NoSuchMethodException {
        handler = new CompressedMethodInvocationMessageHandler(createDeploymentRepository(RemoteCompressingBean.class, RemoteNotCompressingBean.class),
                marshallerFactory, executorService, null);
    }

    @Test
    public void testResponseCompressionEnabled() throws Exception {
        byte[] outputBytes = callMessageInvocationHandler(RemoteCompressingBean.class, "echoWithResponseCompressionEnabled");
        assertEquals(COMPRESSED_HEADER, outputBytes[0]);
    }

    @Test
    public void testResponseCompressionDisabled() throws Exception {
        byte[] outputBytes = callMessageInvocationHandler(RemoteCompressingBean.class, "echoWithResponseCompressionDisabled");
        assertEquals(UNCOMPRESSED_HEADER, outputBytes[0]);
    }

    @Test
    public void testResponseCompressionEnabledOnClassLevel() throws Exception {
        byte[] outputBytes = callMessageInvocationHandler(RemoteCompressingBean.class, "echoWithResponseCompressionOnClassLevelEnabled");
        assertEquals(COMPRESSED_HEADER, outputBytes[0]);
    }

    @Test
    public void testNoCompressionHint() throws Exception {
        byte[] outputBytes = callMessageInvocationHandler(RemoteNotCompressingBean.class, "echoWithoutCompressionHint");
        assertEquals(UNCOMPRESSED_HEADER, outputBytes[0]);
    }

    private byte[] callMessageInvocationHandler(Class remoteInterface, String methodName) throws Exception {
        // prepare input stream
        final PipedOutputStream pos = new PipedOutputStream();
        final DataOutputStream dataOutput = new DataOutputStream(pos);
        final InputStream inputStream = new PipedInputStream(pos);

        // prepare output stream
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        final ChannelAssociation channelAssociation = createChannelAssociation(outputStream);

        writeInvocationRequest(dataOutput, remoteInterface, methodName);
        handler.processMessage(channelAssociation, inputStream);

        return outputStream.toByteArray();
    }

    private <T> void writeInvocationRequest(DataOutputStream dataOutput, Class<T> remoteInterface, String methodName) throws IOException {
        dataOutput.writeShort(0); // invocation id
        dataOutput.writeUTF(methodName); // method name
        dataOutput.writeUTF("java.lang.String"); // method parameter types

        // write locator object
        final Marshaller marshaller = prepareMarshaller(dataOutput);
        final EJBLocator locator = new StatelessEJBLocator<T>(remoteInterface, "", "ejb-invocation-compression-test", CompressingBean.class.getSimpleName(), "");
        marshaller.writeObject(locator.getAppName());
        marshaller.writeObject(locator.getModuleName());
        marshaller.writeObject(locator.getDistinctName());
        marshaller.writeObject(locator.getBeanName());
        marshaller.writeObject(locator);
        marshaller.writeObject("message"); // method param
        marshaller.finish();

        dataOutput.writeByte(0); // attachments
        dataOutput.close();
    }

    private Marshaller prepareMarshaller(final DataOutput dataOutput) throws IOException {
        final MarshallingConfiguration marshallingConfiguration = new MarshallingConfiguration();
//        marshallingConfiguration.setClassTable(ProtocolV1ClassTable.INSTANCE);
//        marshallingConfiguration.setObjectTable(ProtocolV1ObjectTable.INSTANCE);
        marshallingConfiguration.setVersion(2);
        marshallingConfiguration.setSerializedCreator(new SunReflectiveCreator());
        final MarshallerFactory marshallerFactory = new RiverMarshallerFactory();
        final Marshaller marshaller = marshallerFactory.createMarshaller(marshallingConfiguration);
        final OutputStream outputStream = new OutputStream() {
            @Override
            public void write(int b) throws IOException {
                final int byteToWrite = b & 0xff;
                dataOutput.write(byteToWrite);
            }

            @Override
            public void write(final byte[] b, final int off, final int len) throws IOException {
                dataOutput.write(b, off, len);
            }

            @Override
            public void write(final byte[] b) throws IOException {
                dataOutput.write(b);
            }
        };
        final ByteOutput byteOutput = Marshalling.createByteOutput(outputStream);
        // start the marshaller
        marshaller.start(byteOutput);

        return marshaller;
    }

    private ChannelAssociation createChannelAssociation(ByteArrayOutputStream outputStream) throws Exception {
        final Channel channel = Mockito.mock(Channel.class);
        final ChannelAssociation channelAssociation = new ChannelAssociation(channel);

        final MessageOutputStream messageOutputStream = new MockedMessageOutputStream(outputStream);
        Mockito.when(channelAssociation.acquireChannelMessageOutputStream()).thenReturn(messageOutputStream);

        return channelAssociation;
    }

    private DeploymentRepository createDeploymentRepository(Class... remoteInterfaces) throws NoSuchMethodException {
        // remote views map
        final Map<String, InjectedValue<ComponentView>> remoteViews = new HashMap<String, InjectedValue<ComponentView>>();
        for (Class remoteInterface : remoteInterfaces) {
            remoteViews.put(remoteInterface.getName(), createComponentView(remoteInterface));
        }

        // DeploymentInformation
        final ClassLoader deploymentClassLoader = this.getClass().getClassLoader();
        final EjbDeploymentInformation deploymentInformation =
                new EjbDeploymentInformation(null, null, remoteViews, null, deploymentClassLoader, null);

        // ejb map
        final Map<String, EjbDeploymentInformation> ejbs = new HashMap<String, EjbDeploymentInformation>();
        ejbs.put(CompressingBean.class.getSimpleName(), deploymentInformation);

        // modules map
        final DeploymentModuleIdentifier identifier = new DeploymentModuleIdentifier("", "ejb-invocation-compression-test", "");
        final ModuleDeployment deployment = new ModuleDeployment(identifier, ejbs);
        final Map<DeploymentModuleIdentifier, ModuleDeployment> modules = new HashMap<DeploymentModuleIdentifier, ModuleDeployment>();
        modules.put(identifier, deployment);

        // deployment repository
        final DeploymentRepository deploymentRepository = Mockito.mock(DeploymentRepository.class);
        Mockito.when(deploymentRepository.getStartedModules()).thenReturn(modules);
        return deploymentRepository;
    }

    private InjectedValue<ComponentView> createComponentView(Class iface) throws NoSuchMethodException {
        final ComponentView componentView = Mockito.mock(ComponentView.class);
        final Set<Method> viewMethods = new HashSet<Method>(); // list of view methods
        Collections.addAll(viewMethods, iface.getDeclaredMethods());
        Mockito.when(componentView.getViewMethods()).thenReturn(viewMethods);

        final InjectedValue<ComponentView> value = new InjectedValue<ComponentView>();
        value.setValue(new Value<ComponentView>() {
            @Override
            public ComponentView getValue() throws IllegalStateException, IllegalArgumentException {
                return componentView;
            }
        });
        return value;
    }

    @CompressionHint
    private interface RemoteCompressingBean {
        @CompressionHint()
        String echoWithResponseCompressionEnabled(String message);
        @CompressionHint(compressResponse = false)
        String echoWithResponseCompressionDisabled(String message);
        String echoWithResponseCompressionOnClassLevelEnabled(String message);
    }

    private interface RemoteNotCompressingBean {
        String echoWithoutCompressionHint(String message);
    }

    private class CompressingBean implements RemoteCompressingBean, RemoteNotCompressingBean {

        @Override
        public String echoWithResponseCompressionEnabled(String message) {
            return message;
        }

        @Override
        public String echoWithResponseCompressionDisabled(String message) {
            return message;
        }

        @Override
        public String echoWithResponseCompressionOnClassLevelEnabled(String message) {
            return message;
        }

        @Override
        public String echoWithoutCompressionHint(String message) {
            return message;
        }
    }

    private class MockedMessageOutputStream extends MessageOutputStream {

        private OutputStream wrappedOutputStream;

        public MockedMessageOutputStream(OutputStream outputStream) {
            wrappedOutputStream = outputStream;
        }

        @Override
        public void write(int i) throws IOException {
            wrappedOutputStream.write(i);
        }

        @Override
        public void flush() throws IOException {
            wrappedOutputStream.flush();
        }

        @Override
        public void close() throws IOException {
            wrappedOutputStream.close();
        }

        @Override
        public MessageOutputStream cancel() {
            return this;
        }
    }

    class SynchronousExecutor implements ExecutorService {

        @Override
        public void shutdown() {

        }

        @Override
        public List<Runnable> shutdownNow() {
            return Collections.emptyList();
        }

        @Override
        public boolean isShutdown() {
            return false;
        }

        @Override
        public boolean isTerminated() {
            return false;
        }

        @Override
        public boolean awaitTermination(long l, TimeUnit timeUnit) throws InterruptedException {
            return false;
        }

        @Override
        public <T> Future<T> submit(Callable<T> callable) {
            throw new NotImplementedException();
        }

        @Override
        public <T> Future<T> submit(Runnable runnable, T t) {
            throw new NotImplementedException();
        }

        @Override
        public Future<?> submit(Runnable runnable) {
            runnable.run();
            return new CompletedFuture<Object>(null);
        }

        @Override
        public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> collection) throws InterruptedException {
            throw new NotImplementedException();
        }

        @Override
        public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> collection, long l, TimeUnit timeUnit) throws InterruptedException {
            throw new NotImplementedException();
        }

        @Override
        public <T> T invokeAny(Collection<? extends Callable<T>> collection) throws InterruptedException, ExecutionException {
            throw new NotImplementedException();
        }

        @Override
        public <T> T invokeAny(Collection<? extends Callable<T>> collection, long l, TimeUnit timeUnit) throws InterruptedException, ExecutionException, TimeoutException {
            throw new NotImplementedException();
        }

        @Override
        public void execute(Runnable runnable) {
            throw new NotImplementedException();
        }
    }

    class CompletedFuture<T> implements Future<T> {

        private T result;

        public CompletedFuture(T result) {
            this.result = result;
        }

        @Override
        public boolean cancel(boolean b) {
            return false;
        }

        @Override
        public boolean isCancelled() {
            return false;
        }

        @Override
        public boolean isDone() {
            return true;
        }

        @Override
        public T get() throws InterruptedException, ExecutionException {
            return result;
        }

        @Override
        public T get(long l, TimeUnit timeUnit) throws InterruptedException, ExecutionException, TimeoutException {
            return result;
        }
    }
}

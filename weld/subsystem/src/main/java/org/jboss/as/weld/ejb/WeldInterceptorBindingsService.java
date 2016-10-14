package org.jboss.as.weld.ejb;

import org.jboss.as.weld.WeldBootstrapService;
import org.jboss.as.weld.spi.ComponentInterceptorSupport;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.weld.annotated.enhanced.EnhancedAnnotatedType;
import org.jboss.weld.annotated.slim.SlimAnnotatedType;
import org.jboss.weld.bean.interceptor.InterceptorBindingsAdapter;
import org.jboss.weld.ejb.spi.InterceptorBindings;
import org.jboss.weld.injection.producer.InterceptionModelInitializer;
import org.jboss.weld.interceptor.spi.model.InterceptionModel;
import org.jboss.weld.manager.BeanManagerImpl;
import org.jboss.weld.resources.ClassTransformer;

/**
 * @author Stuart Douglas
 * @author Jozef Hartinger
 */
public class WeldInterceptorBindingsService implements Service<InterceptorBindings> {

    private volatile InterceptorBindings interceptorBindings;
    private final InjectedValue<WeldBootstrapService> weldContainer = new InjectedValue<WeldBootstrapService>();
    private final String beanArchiveId;
    private final String ejbName;
    private final Class<?> componentClass;
    private final ComponentInterceptorSupport interceptorSupport;

    public static final ServiceName SERVICE_NAME = ServiceName.of("WeldInterceptorBindingsService");

    public WeldInterceptorBindingsService(String beanArchiveId, String ejbName, Class<?> componentClass, ComponentInterceptorSupport componentInterceptorSupport) {
        this.beanArchiveId = beanArchiveId;
        this.ejbName = ejbName;
        this.componentClass = componentClass;
        this.interceptorSupport = componentInterceptorSupport;
    }

    @Override
    public void start(StartContext startContext) throws StartException {
        BeanManagerImpl beanManager = this.weldContainer.getValue().getBeanManager(beanArchiveId);
        //this is not always called with the deployments TCCL set
        //which causes weld to blow up
        interceptorBindings = getInterceptorBindings(this.ejbName, beanManager);
    }

    protected InterceptorBindings getInterceptorBindings(String ejbName, final BeanManagerImpl manager) {
        if (ejbName != null) {
            return interceptorSupport.getInterceptorBindings(ejbName, manager);
        } else {
            // This is a managed bean
            SlimAnnotatedType<?> type = (SlimAnnotatedType<?>) manager.createAnnotatedType(componentClass);
            if (!manager.getInterceptorModelRegistry().containsKey(type)) {
                EnhancedAnnotatedType<?> enhancedType = manager.getServices().get(ClassTransformer.class).getEnhancedAnnotatedType(type);
                InterceptionModelInitializer.of(manager, enhancedType, null).init();
            }
            InterceptionModel model = manager.getInterceptorModelRegistry().get(type);
            if (model != null) {
                return new InterceptorBindingsAdapter(manager.getInterceptorModelRegistry().get(type));
            }
        }
        return null;
    }


    @Override
    public void stop(StopContext stopContext) {
        this.interceptorBindings = null;
    }

    @Override
    public InterceptorBindings getValue() throws IllegalStateException, IllegalArgumentException {
        return interceptorBindings;
    }

    public InjectedValue<WeldBootstrapService> getWeldContainer() {
        return weldContainer;
    }
}

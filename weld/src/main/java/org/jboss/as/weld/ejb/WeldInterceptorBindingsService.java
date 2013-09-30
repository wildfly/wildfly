package org.jboss.as.weld.ejb;

import org.jboss.as.weld.WeldBootstrapService;
import org.jboss.as.weld.services.bootstrap.WeldEjbServices;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.weld.bean.SessionBean;
import org.jboss.weld.ejb.spi.EjbDescriptor;
import org.jboss.weld.ejb.spi.EjbServices;
import org.jboss.weld.ejb.spi.InterceptorBindings;
import org.jboss.weld.ejb.spi.helpers.ForwardingEjbServices;
import org.jboss.weld.manager.BeanManagerImpl;

/**
 * @author Stuart Douglas
 */
public class WeldInterceptorBindingsService implements Service<InterceptorBindings> {

    private volatile InterceptorBindings interceptorBindings;
    private final InjectedValue<WeldBootstrapService> weldContainer = new InjectedValue<WeldBootstrapService>();
    private final String beanArchiveId;
    private final String ejbName;

    public static final ServiceName SERVICE_NAME = ServiceName.of("WeldInterceptorBindingsService");

    public WeldInterceptorBindingsService(String beanArchiveId, String ejbName) {
        this.beanArchiveId = beanArchiveId;
        this.ejbName = ejbName;
    }

    @Override
    public void start(StartContext startContext) throws StartException {
        BeanManagerImpl beanManager = (BeanManagerImpl) this.weldContainer.getValue().getBeanManager(beanArchiveId);
        //this is not always called with the deployments TCCL set
        //which causes weld to blow up
        EjbDescriptor<Object> descriptor = beanManager.getEjbDescriptor(this.ejbName);
        SessionBean<Object> bean = null;
        if (descriptor != null) {
            bean = beanManager.getBean(descriptor);
        }
        interceptorBindings = getInterceptorBindings(this.ejbName, beanManager);
    }

    protected InterceptorBindings getInterceptorBindings(String ejbName, final BeanManagerImpl beanManager) {
        EjbServices ejbServices = beanManager.getServices().get(EjbServices.class);
        if (ejbServices instanceof ForwardingEjbServices) {
            ejbServices = ((ForwardingEjbServices) ejbServices).delegate();
        }
        InterceptorBindings interceptorBindings = null;
        if (ejbServices instanceof WeldEjbServices) {
            interceptorBindings = ((WeldEjbServices) ejbServices).getBindings(ejbName);
        }
        return interceptorBindings;
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

/*
 * Copyright 2012 Harald Wellmann.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.cdi.spi;

import static org.ops4j.pax.swissbox.core.ContextClassLoaderUtils.doWithClassLoader;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.concurrent.Callable;

import javax.enterprise.inject.spi.BeanManager;

import org.ops4j.lang.Ops4jException;
import org.ops4j.pax.cdi.api.BeanBundle;
import org.ops4j.pax.cdi.api.ContainerInitialized;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract base class for {@link CdiContainer} implementations.
 * 
 * @author Harald Wellmann
 */
public abstract class AbstractCdiContainer implements CdiContainer {

    private static Logger log = LoggerFactory.getLogger(AbstractCdiContainer.class);

    private Bundle bundle;
    private CdiContainerType containerType;

    protected AbstractCdiContainer(CdiContainerType containerType, Bundle bundle) {
        this.containerType = containerType;
        this.bundle = bundle;
    }

    protected void finishStartup() {
        try {
            doWithClassLoader(getContextClassLoader(),
                new Callable<ServiceRegistration<CdiContainer>>() {

                    @Override
                    public ServiceRegistration<CdiContainer> call() throws Exception {
                        // set bundle context on BeanBundle CDI bean
                        BeanBundle cdiBundle = getInstance().select(BeanBundle.class).get();
                        BundleContext bc = bundle.getBundleContext();
                        cdiBundle.setBundleContext(bc);

                        // register CdiContainer service
                        Dictionary<String, Object> props = new Hashtable<String, Object>();
                        props.put("bundleId", bundle.getBundleId());
                        props.put("symbolicName", bundle.getSymbolicName());

                        // fire ContainerInitialized event
                        BeanManager beanManager = getBeanManager();
                        beanManager.fireEvent(new ContainerInitialized());
                        
                        ServiceRegistration<CdiContainer> registration = bc.registerService(CdiContainer.class, AbstractCdiContainer.this,
                            props);

                        return registration;
                    }
                });
        }
        // CHECKSTYLE:SKIP
        catch (Exception exc) {
            log.error("", exc);
            throw new Ops4jException(exc);
        }
    }

    public Bundle getBundle() {
        return bundle;
    }

    public CdiContainerType getContainerType() {
        return containerType;
    }
}
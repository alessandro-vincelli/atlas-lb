package org.openstack.atlas.api.validation;

import net.sf.cglib.proxy.InvocationHandler;
import org.openstack.atlas.api.validation.util.CallStateRegistry;

import java.lang.reflect.Method;

class ValidationInterceptor implements InvocationHandler {

    private final CallStateRegistry sharedCallState;

    public ValidationInterceptor(CallStateRegistry sharedCallState) {
        this.sharedCallState = sharedCallState;
    }

    @Override
    public Object invoke(Object o, Method method, Object[] os) throws Throwable {
        sharedCallState.registerCall(method);

        return null;    //TODO: Verify that this is okay...
    }
}

package com.microsoft.azure.functions.worker.broker;

import com.microsoft.azure.functions.dihook.FunctionInstanceFactory;
import com.microsoft.azure.functions.worker.binding.*;

/**
 * Used to executor of arbitrary Java method in any JAR using reflection.
 * Thread-Safety: Multiple thread.
 */
public class EnhancedJavaMethodExecutorImpl implements JavaMethodExecutor {

    private final ClassLoader classLoader;

    private final FunctionInstanceFactory functionInstanceFactory;

    public EnhancedJavaMethodExecutorImpl(ClassLoader classLoader, FunctionInstanceFactory functionInstanceFactory) {
        this.classLoader = classLoader;
        this.functionInstanceFactory = functionInstanceFactory;
    }

    public void execute(ExecutionContextDataSource executionContextDataSource) throws Exception {
        try {
            Thread.currentThread().setContextClassLoader(this.classLoader);
            Object retValue = ParameterResolver.resolveArguments(executionContextDataSource)
                    .orElseThrow(() -> new NoSuchMethodException("Cannot locate the method signature with the given input"))
                    .invoke(() -> functionInstanceFactory.getInstance(executionContextDataSource.getContainingClass()));
            executionContextDataSource.getDataStore().setDataTargetValue(BindingDataStore.RETURN_NAME, retValue);
            executionContextDataSource.setReturnValue(retValue);
        } finally {
            Thread.currentThread().setContextClassLoader(ClassLoader.getSystemClassLoader());
        }
    }
}

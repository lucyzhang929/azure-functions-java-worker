package com.microsoft.azure.functions.worker.broker;

import com.microsoft.azure.functions.dihook.FunctionInstanceFactory;
import com.microsoft.azure.functions.worker.WorkerLogManager;
import org.apache.commons.lang3.SystemUtils;

import java.net.MalformedURLException;

public class FactoryJavaMethodExecutor {
    public static JavaMethodExecutor createJavaMethodExecutor(ClassLoader classLoader, FunctionInstanceFactory functionInstanceFactory) {
        if(SystemUtils.IS_JAVA_1_8) {
            WorkerLogManager.getSystemLogger().info("Loading JavaMethodExecutorImpl");
            return new JavaMethodExecutorImpl(functionInstanceFactory);
        } else {
            WorkerLogManager.getSystemLogger().info("Loading EnhancedJavaMethodExecutorImpl");
            return new EnhancedJavaMethodExecutorImpl(classLoader, functionInstanceFactory);
        }
    }
}

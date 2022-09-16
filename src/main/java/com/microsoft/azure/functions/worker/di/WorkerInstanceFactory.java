package com.microsoft.azure.functions.worker.di;

import com.microsoft.azure.functions.dihook.FunctionInstanceFactory;

public class WorkerInstanceFactory implements FunctionInstanceFactory {
    @Override
    public <T> T getInstance(Class<T> functionClass) throws Exception{
        return functionClass.newInstance();
    }
}

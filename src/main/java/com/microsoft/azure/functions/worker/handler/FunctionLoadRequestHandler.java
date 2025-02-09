package com.microsoft.azure.functions.worker.handler;

import java.util.*;
import java.util.logging.Level;

import com.microsoft.azure.functions.worker.WorkerLogManager;
import com.microsoft.azure.functions.worker.broker.*;
import com.microsoft.azure.functions.worker.description.*;
import com.microsoft.azure.functions.rpc.messages.*;

public class FunctionLoadRequestHandler extends MessageHandler<FunctionLoadRequest, FunctionLoadResponse.Builder> {
    public FunctionLoadRequestHandler(JavaFunctionBroker broker) {
        super(StreamingMessage::getFunctionLoadRequest,
              FunctionLoadResponse::newBuilder,
              FunctionLoadResponse.Builder::setResult,
              StreamingMessage.Builder::setFunctionLoadResponse);
        
        this.broker = broker;
    }

    @Override
    String execute(FunctionLoadRequest request, FunctionLoadResponse.Builder response) throws Exception {
        WorkerLogManager.getSystemLogger().log(Level.INFO, "FunctionLoadRequest received by the Java worker");
        final RpcFunctionMetadata metadata = request.getMetadata();
        final FunctionMethodDescriptor descriptor = createFunctionDescriptor(request.getFunctionId(), metadata);
        
        final Map<String, BindingInfo> bindings = metadata.getBindingsMap();

        response.setFunctionId(descriptor.getId());
        this.broker.loadMethod(descriptor, bindings);

        return String.format("\"%s\" loaded (ID: %s, Reflection: \"%s\"::\"%s\")", 
            descriptor.getName(), 
            descriptor.getId(), 
            descriptor.getJarPath(), 
            descriptor.getFullMethodName());
    }
    
    FunctionMethodDescriptor createFunctionDescriptor(String functionId, RpcFunctionMetadata metadata) {
        return new FunctionMethodDescriptor(functionId, metadata.getName(), metadata.getEntryPoint(), metadata.getScriptFile());      
    }

    private final JavaFunctionBroker broker;
}

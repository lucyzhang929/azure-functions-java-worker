package com.microsoft.azure.functions.worker.handler;

import com.microsoft.azure.functions.rpc.messages.*;
import com.microsoft.azure.functions.worker.broker.JavaFunctionBroker;
import com.microsoft.azure.functions.worker.reflect.EnhancedClassLoaderProvider;

import java.util.*;


public class FunctionWarmupHandler extends MessageHandler<FunctionWarmupRequest, FunctionWarmupResponse.Builder> {

    private final JavaFunctionBroker javaFunctionBroker;

    public FunctionWarmupHandler(JavaFunctionBroker javaFunctionBroker) {
        super(StreamingMessage::getFunctionWarmupRequest,
                FunctionWarmupResponse::newBuilder,
                FunctionWarmupResponse.Builder::setResult,
                StreamingMessage.Builder::setFunctionWarmupResponse);
        this.javaFunctionBroker = javaFunctionBroker;
    }

    @Override
    String execute(FunctionWarmupRequest functionWarmupRequest, FunctionWarmupResponse.Builder builder) throws Exception {

        //warm up FunctionEnvironmentReloadRequestHandler
        try {
            FunctionEnvironmentReloadRequest.Builder functionEnvironmentReloadRequestBuilder = FunctionEnvironmentReloadRequest.newBuilder();
            FunctionEnvironmentReloadRequest functionEnvironmentReloadRequest = functionEnvironmentReloadRequestBuilder.putAllEnvironmentVariables(System.getenv()).build();
            new FunctionEnvironmentReloadRequestHandler(this.javaFunctionBroker).execute(functionEnvironmentReloadRequest, null);

            //warm up FunctionLoadRequestHandler
            FunctionLoadRequest.Builder functionLoadRequestBuilder = FunctionLoadRequest.newBuilder();
            RpcFunctionMetadata.Builder rpcFunctionMetadataBuilder = RpcFunctionMetadata.newBuilder();
            rpcFunctionMetadataBuilder.setName("WarmupFunc");
            rpcFunctionMetadataBuilder.setEntryPoint("com.azfs.java.Function.run");
            String workerDirectory = functionWarmupRequest.getWorkerDirectory();
            rpcFunctionMetadataBuilder.setScriptFile(workerDirectory + "/annotationLib/java-warmup-app-1.0-SNAPSHOT.jar");
            Map<String, BindingInfo> map = new HashMap<>();
            BindingInfo warmupTrigger = BindingInfo.newBuilder().setDirection(BindingInfo.Direction.in).setDataType(BindingInfo.DataType.undefined).setType("warmupTrigger").build();
            map.put("warmupContext", warmupTrigger);
            rpcFunctionMetadataBuilder.putAllBindings(map);
            final UUID functionId = UUID.randomUUID();
            functionLoadRequestBuilder.setFunctionId(functionId.toString());
            functionLoadRequestBuilder.setMetadata(rpcFunctionMetadataBuilder);
            String loadRequestResult = new FunctionLoadRequestHandler(this.javaFunctionBroker).execute(functionLoadRequestBuilder.build(), FunctionLoadResponse.newBuilder());
            System.out.println("load request result: " + loadRequestResult);

            //warm up InvocationRequestHandler
            InvocationRequest.Builder invocationRequestBuilder = InvocationRequest.newBuilder();
            invocationRequestBuilder.setFunctionId(functionId.toString()).setInvocationId(UUID.randomUUID().toString());
            List<ParameterBinding> inputDataList = new ArrayList<>();
            ParameterBinding.Builder parameterBindingBuilder = ParameterBinding.newBuilder();
            parameterBindingBuilder.setName("warmupContext");
            parameterBindingBuilder.setData(TypedData.newBuilder().setString("warmup"));
            inputDataList.add(parameterBindingBuilder.build());
            invocationRequestBuilder.addAllInputData(inputDataList);
            String invocationResult = new InvocationRequestHandler(this.javaFunctionBroker).execute(invocationRequestBuilder.build(), InvocationResponse.newBuilder());

            System.out.println("invocation result:" + invocationResult);

            //reset classloader that used for warm up
            EnhancedClassLoaderProvider.resetClassLoaderInstance();
        } catch (Exception e) {
            System.out.println(e.getMessage());
            throw new RuntimeException(e);
        }
        return "warm up completed";
    }
}

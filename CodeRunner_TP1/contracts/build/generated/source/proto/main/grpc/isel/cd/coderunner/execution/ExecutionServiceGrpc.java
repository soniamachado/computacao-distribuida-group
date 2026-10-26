package isel.cd.coderunner.execution;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.68.0)",
    comments = "Source: execution.proto")
@io.grpc.stub.annotations.GrpcGenerated
public final class ExecutionServiceGrpc {

  private ExecutionServiceGrpc() {}

  public static final java.lang.String SERVICE_NAME = "isel.cd.coderunner.execution.ExecutionService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<isel.cd.coderunner.execution.ProgramChunk,
      isel.cd.coderunner.execution.SubmitResponse> getSubmitProgramMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "SubmitProgram",
      requestType = isel.cd.coderunner.execution.ProgramChunk.class,
      responseType = isel.cd.coderunner.execution.SubmitResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.CLIENT_STREAMING)
  public static io.grpc.MethodDescriptor<isel.cd.coderunner.execution.ProgramChunk,
      isel.cd.coderunner.execution.SubmitResponse> getSubmitProgramMethod() {
    io.grpc.MethodDescriptor<isel.cd.coderunner.execution.ProgramChunk, isel.cd.coderunner.execution.SubmitResponse> getSubmitProgramMethod;
    if ((getSubmitProgramMethod = ExecutionServiceGrpc.getSubmitProgramMethod) == null) {
      synchronized (ExecutionServiceGrpc.class) {
        if ((getSubmitProgramMethod = ExecutionServiceGrpc.getSubmitProgramMethod) == null) {
          ExecutionServiceGrpc.getSubmitProgramMethod = getSubmitProgramMethod =
              io.grpc.MethodDescriptor.<isel.cd.coderunner.execution.ProgramChunk, isel.cd.coderunner.execution.SubmitResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.CLIENT_STREAMING)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "SubmitProgram"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  isel.cd.coderunner.execution.ProgramChunk.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  isel.cd.coderunner.execution.SubmitResponse.getDefaultInstance()))
              .setSchemaDescriptor(new ExecutionServiceMethodDescriptorSupplier("SubmitProgram"))
              .build();
        }
      }
    }
    return getSubmitProgramMethod;
  }

  private static volatile io.grpc.MethodDescriptor<isel.cd.coderunner.execution.ResultRequest,
      isel.cd.coderunner.execution.ResultChunk> getGetResultMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetResult",
      requestType = isel.cd.coderunner.execution.ResultRequest.class,
      responseType = isel.cd.coderunner.execution.ResultChunk.class,
      methodType = io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
  public static io.grpc.MethodDescriptor<isel.cd.coderunner.execution.ResultRequest,
      isel.cd.coderunner.execution.ResultChunk> getGetResultMethod() {
    io.grpc.MethodDescriptor<isel.cd.coderunner.execution.ResultRequest, isel.cd.coderunner.execution.ResultChunk> getGetResultMethod;
    if ((getGetResultMethod = ExecutionServiceGrpc.getGetResultMethod) == null) {
      synchronized (ExecutionServiceGrpc.class) {
        if ((getGetResultMethod = ExecutionServiceGrpc.getGetResultMethod) == null) {
          ExecutionServiceGrpc.getGetResultMethod = getGetResultMethod =
              io.grpc.MethodDescriptor.<isel.cd.coderunner.execution.ResultRequest, isel.cd.coderunner.execution.ResultChunk>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetResult"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  isel.cd.coderunner.execution.ResultRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  isel.cd.coderunner.execution.ResultChunk.getDefaultInstance()))
              .setSchemaDescriptor(new ExecutionServiceMethodDescriptorSupplier("GetResult"))
              .build();
        }
      }
    }
    return getGetResultMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static ExecutionServiceStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<ExecutionServiceStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<ExecutionServiceStub>() {
        @java.lang.Override
        public ExecutionServiceStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new ExecutionServiceStub(channel, callOptions);
        }
      };
    return ExecutionServiceStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static ExecutionServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<ExecutionServiceBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<ExecutionServiceBlockingStub>() {
        @java.lang.Override
        public ExecutionServiceBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new ExecutionServiceBlockingStub(channel, callOptions);
        }
      };
    return ExecutionServiceBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static ExecutionServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<ExecutionServiceFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<ExecutionServiceFutureStub>() {
        @java.lang.Override
        public ExecutionServiceFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new ExecutionServiceFutureStub(channel, callOptions);
        }
      };
    return ExecutionServiceFutureStub.newStub(factory, channel);
  }

  /**
   */
  public interface AsyncService {

    /**
     * <pre>
     * c: Submeter programa via streaming de cliente
     * </pre>
     */
    default io.grpc.stub.StreamObserver<isel.cd.coderunner.execution.ProgramChunk> submitProgram(
        io.grpc.stub.StreamObserver<isel.cd.coderunner.execution.SubmitResponse> responseObserver) {
      return io.grpc.stub.ServerCalls.asyncUnimplementedStreamingCall(getSubmitProgramMethod(), responseObserver);
    }

    /**
     * <pre>
     * c: Obter resultado via streaming de servidor
     * </pre>
     */
    default void getResult(isel.cd.coderunner.execution.ResultRequest request,
        io.grpc.stub.StreamObserver<isel.cd.coderunner.execution.ResultChunk> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetResultMethod(), responseObserver);
    }
  }

  /**
   * Base class for the server implementation of the service ExecutionService.
   */
  public static abstract class ExecutionServiceImplBase
      implements io.grpc.BindableService, AsyncService {

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return ExecutionServiceGrpc.bindService(this);
    }
  }

  /**
   * A stub to allow clients to do asynchronous rpc calls to service ExecutionService.
   */
  public static final class ExecutionServiceStub
      extends io.grpc.stub.AbstractAsyncStub<ExecutionServiceStub> {
    private ExecutionServiceStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected ExecutionServiceStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new ExecutionServiceStub(channel, callOptions);
    }

    /**
     * <pre>
     * c: Submeter programa via streaming de cliente
     * </pre>
     */
    public io.grpc.stub.StreamObserver<isel.cd.coderunner.execution.ProgramChunk> submitProgram(
        io.grpc.stub.StreamObserver<isel.cd.coderunner.execution.SubmitResponse> responseObserver) {
      return io.grpc.stub.ClientCalls.asyncClientStreamingCall(
          getChannel().newCall(getSubmitProgramMethod(), getCallOptions()), responseObserver);
    }

    /**
     * <pre>
     * c: Obter resultado via streaming de servidor
     * </pre>
     */
    public void getResult(isel.cd.coderunner.execution.ResultRequest request,
        io.grpc.stub.StreamObserver<isel.cd.coderunner.execution.ResultChunk> responseObserver) {
      io.grpc.stub.ClientCalls.asyncServerStreamingCall(
          getChannel().newCall(getGetResultMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * A stub to allow clients to do synchronous rpc calls to service ExecutionService.
   */
  public static final class ExecutionServiceBlockingStub
      extends io.grpc.stub.AbstractBlockingStub<ExecutionServiceBlockingStub> {
    private ExecutionServiceBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected ExecutionServiceBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new ExecutionServiceBlockingStub(channel, callOptions);
    }

    /**
     * <pre>
     * c: Obter resultado via streaming de servidor
     * </pre>
     */
    public java.util.Iterator<isel.cd.coderunner.execution.ResultChunk> getResult(
        isel.cd.coderunner.execution.ResultRequest request) {
      return io.grpc.stub.ClientCalls.blockingServerStreamingCall(
          getChannel(), getGetResultMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do ListenableFuture-style rpc calls to service ExecutionService.
   */
  public static final class ExecutionServiceFutureStub
      extends io.grpc.stub.AbstractFutureStub<ExecutionServiceFutureStub> {
    private ExecutionServiceFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected ExecutionServiceFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new ExecutionServiceFutureStub(channel, callOptions);
    }
  }

  private static final int METHODID_GET_RESULT = 0;
  private static final int METHODID_SUBMIT_PROGRAM = 1;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final AsyncService serviceImpl;
    private final int methodId;

    MethodHandlers(AsyncService serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_GET_RESULT:
          serviceImpl.getResult((isel.cd.coderunner.execution.ResultRequest) request,
              (io.grpc.stub.StreamObserver<isel.cd.coderunner.execution.ResultChunk>) responseObserver);
          break;
        default:
          throw new AssertionError();
      }
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public io.grpc.stub.StreamObserver<Req> invoke(
        io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_SUBMIT_PROGRAM:
          return (io.grpc.stub.StreamObserver<Req>) serviceImpl.submitProgram(
              (io.grpc.stub.StreamObserver<isel.cd.coderunner.execution.SubmitResponse>) responseObserver);
        default:
          throw new AssertionError();
      }
    }
  }

  public static final io.grpc.ServerServiceDefinition bindService(AsyncService service) {
    return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
        .addMethod(
          getSubmitProgramMethod(),
          io.grpc.stub.ServerCalls.asyncClientStreamingCall(
            new MethodHandlers<
              isel.cd.coderunner.execution.ProgramChunk,
              isel.cd.coderunner.execution.SubmitResponse>(
                service, METHODID_SUBMIT_PROGRAM)))
        .addMethod(
          getGetResultMethod(),
          io.grpc.stub.ServerCalls.asyncServerStreamingCall(
            new MethodHandlers<
              isel.cd.coderunner.execution.ResultRequest,
              isel.cd.coderunner.execution.ResultChunk>(
                service, METHODID_GET_RESULT)))
        .build();
  }

  private static abstract class ExecutionServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    ExecutionServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return isel.cd.coderunner.execution.Execution.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("ExecutionService");
    }
  }

  private static final class ExecutionServiceFileDescriptorSupplier
      extends ExecutionServiceBaseDescriptorSupplier {
    ExecutionServiceFileDescriptorSupplier() {}
  }

  private static final class ExecutionServiceMethodDescriptorSupplier
      extends ExecutionServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final java.lang.String methodName;

    ExecutionServiceMethodDescriptorSupplier(java.lang.String methodName) {
      this.methodName = methodName;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.MethodDescriptor getMethodDescriptor() {
      return getServiceDescriptor().findMethodByName(methodName);
    }
  }

  private static volatile io.grpc.ServiceDescriptor serviceDescriptor;

  public static io.grpc.ServiceDescriptor getServiceDescriptor() {
    io.grpc.ServiceDescriptor result = serviceDescriptor;
    if (result == null) {
      synchronized (ExecutionServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new ExecutionServiceFileDescriptorSupplier())
              .addMethod(getSubmitProgramMethod())
              .addMethod(getGetResultMethod())
              .build();
        }
      }
    }
    return result;
  }
}

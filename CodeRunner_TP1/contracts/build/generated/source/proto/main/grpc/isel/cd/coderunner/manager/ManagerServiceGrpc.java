package isel.cd.coderunner.manager;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 * <pre>
 * Contrato a: Registo de RunServer
 * Contrato b: Obtenção de RunServer por um Client
 * </pre>
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.68.0)",
    comments = "Source: manager.proto")
@io.grpc.stub.annotations.GrpcGenerated
public final class ManagerServiceGrpc {

  private ManagerServiceGrpc() {}

  public static final java.lang.String SERVICE_NAME = "isel.cd.coderunner.manager.ManagerService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<isel.cd.coderunner.manager.RegisterRequest,
      isel.cd.coderunner.manager.RegisterResponse> getRegisterRunServerMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "RegisterRunServer",
      requestType = isel.cd.coderunner.manager.RegisterRequest.class,
      responseType = isel.cd.coderunner.manager.RegisterResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<isel.cd.coderunner.manager.RegisterRequest,
      isel.cd.coderunner.manager.RegisterResponse> getRegisterRunServerMethod() {
    io.grpc.MethodDescriptor<isel.cd.coderunner.manager.RegisterRequest, isel.cd.coderunner.manager.RegisterResponse> getRegisterRunServerMethod;
    if ((getRegisterRunServerMethod = ManagerServiceGrpc.getRegisterRunServerMethod) == null) {
      synchronized (ManagerServiceGrpc.class) {
        if ((getRegisterRunServerMethod = ManagerServiceGrpc.getRegisterRunServerMethod) == null) {
          ManagerServiceGrpc.getRegisterRunServerMethod = getRegisterRunServerMethod =
              io.grpc.MethodDescriptor.<isel.cd.coderunner.manager.RegisterRequest, isel.cd.coderunner.manager.RegisterResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "RegisterRunServer"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  isel.cd.coderunner.manager.RegisterRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  isel.cd.coderunner.manager.RegisterResponse.getDefaultInstance()))
              .setSchemaDescriptor(new ManagerServiceMethodDescriptorSupplier("RegisterRunServer"))
              .build();
        }
      }
    }
    return getRegisterRunServerMethod;
  }

  private static volatile io.grpc.MethodDescriptor<isel.cd.coderunner.manager.GetRunServerRequest,
      isel.cd.coderunner.manager.GetRunServerResponse> getGetRunServerMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetRunServer",
      requestType = isel.cd.coderunner.manager.GetRunServerRequest.class,
      responseType = isel.cd.coderunner.manager.GetRunServerResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<isel.cd.coderunner.manager.GetRunServerRequest,
      isel.cd.coderunner.manager.GetRunServerResponse> getGetRunServerMethod() {
    io.grpc.MethodDescriptor<isel.cd.coderunner.manager.GetRunServerRequest, isel.cd.coderunner.manager.GetRunServerResponse> getGetRunServerMethod;
    if ((getGetRunServerMethod = ManagerServiceGrpc.getGetRunServerMethod) == null) {
      synchronized (ManagerServiceGrpc.class) {
        if ((getGetRunServerMethod = ManagerServiceGrpc.getGetRunServerMethod) == null) {
          ManagerServiceGrpc.getGetRunServerMethod = getGetRunServerMethod =
              io.grpc.MethodDescriptor.<isel.cd.coderunner.manager.GetRunServerRequest, isel.cd.coderunner.manager.GetRunServerResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetRunServer"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  isel.cd.coderunner.manager.GetRunServerRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  isel.cd.coderunner.manager.GetRunServerResponse.getDefaultInstance()))
              .setSchemaDescriptor(new ManagerServiceMethodDescriptorSupplier("GetRunServer"))
              .build();
        }
      }
    }
    return getGetRunServerMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static ManagerServiceStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<ManagerServiceStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<ManagerServiceStub>() {
        @java.lang.Override
        public ManagerServiceStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new ManagerServiceStub(channel, callOptions);
        }
      };
    return ManagerServiceStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static ManagerServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<ManagerServiceBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<ManagerServiceBlockingStub>() {
        @java.lang.Override
        public ManagerServiceBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new ManagerServiceBlockingStub(channel, callOptions);
        }
      };
    return ManagerServiceBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static ManagerServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<ManagerServiceFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<ManagerServiceFutureStub>() {
        @java.lang.Override
        public ManagerServiceFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new ManagerServiceFutureStub(channel, callOptions);
        }
      };
    return ManagerServiceFutureStub.newStub(factory, channel);
  }

  /**
   * <pre>
   * Contrato a: Registo de RunServer
   * Contrato b: Obtenção de RunServer por um Client
   * </pre>
   */
  public interface AsyncService {

    /**
     * <pre>
     * a: RunServer regista-se fornecendo o seu IP:Porto e recebe o IP:Porto do REDIS
     * </pre>
     */
    default void registerRunServer(isel.cd.coderunner.manager.RegisterRequest request,
        io.grpc.stub.StreamObserver<isel.cd.coderunner.manager.RegisterResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getRegisterRunServerMethod(), responseObserver);
    }

    /**
     * <pre>
     * b: Client solicita um RunServer disponível (distribuição em Round-Robin)
     * </pre>
     */
    default void getRunServer(isel.cd.coderunner.manager.GetRunServerRequest request,
        io.grpc.stub.StreamObserver<isel.cd.coderunner.manager.GetRunServerResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetRunServerMethod(), responseObserver);
    }
  }

  /**
   * Base class for the server implementation of the service ManagerService.
   * <pre>
   * Contrato a: Registo de RunServer
   * Contrato b: Obtenção de RunServer por um Client
   * </pre>
   */
  public static abstract class ManagerServiceImplBase
      implements io.grpc.BindableService, AsyncService {

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return ManagerServiceGrpc.bindService(this);
    }
  }

  /**
   * A stub to allow clients to do asynchronous rpc calls to service ManagerService.
   * <pre>
   * Contrato a: Registo de RunServer
   * Contrato b: Obtenção de RunServer por um Client
   * </pre>
   */
  public static final class ManagerServiceStub
      extends io.grpc.stub.AbstractAsyncStub<ManagerServiceStub> {
    private ManagerServiceStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected ManagerServiceStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new ManagerServiceStub(channel, callOptions);
    }

    /**
     * <pre>
     * a: RunServer regista-se fornecendo o seu IP:Porto e recebe o IP:Porto do REDIS
     * </pre>
     */
    public void registerRunServer(isel.cd.coderunner.manager.RegisterRequest request,
        io.grpc.stub.StreamObserver<isel.cd.coderunner.manager.RegisterResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getRegisterRunServerMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * b: Client solicita um RunServer disponível (distribuição em Round-Robin)
     * </pre>
     */
    public void getRunServer(isel.cd.coderunner.manager.GetRunServerRequest request,
        io.grpc.stub.StreamObserver<isel.cd.coderunner.manager.GetRunServerResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGetRunServerMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * A stub to allow clients to do synchronous rpc calls to service ManagerService.
   * <pre>
   * Contrato a: Registo de RunServer
   * Contrato b: Obtenção de RunServer por um Client
   * </pre>
   */
  public static final class ManagerServiceBlockingStub
      extends io.grpc.stub.AbstractBlockingStub<ManagerServiceBlockingStub> {
    private ManagerServiceBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected ManagerServiceBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new ManagerServiceBlockingStub(channel, callOptions);
    }

    /**
     * <pre>
     * a: RunServer regista-se fornecendo o seu IP:Porto e recebe o IP:Porto do REDIS
     * </pre>
     */
    public isel.cd.coderunner.manager.RegisterResponse registerRunServer(isel.cd.coderunner.manager.RegisterRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getRegisterRunServerMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * b: Client solicita um RunServer disponível (distribuição em Round-Robin)
     * </pre>
     */
    public isel.cd.coderunner.manager.GetRunServerResponse getRunServer(isel.cd.coderunner.manager.GetRunServerRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGetRunServerMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do ListenableFuture-style rpc calls to service ManagerService.
   * <pre>
   * Contrato a: Registo de RunServer
   * Contrato b: Obtenção de RunServer por um Client
   * </pre>
   */
  public static final class ManagerServiceFutureStub
      extends io.grpc.stub.AbstractFutureStub<ManagerServiceFutureStub> {
    private ManagerServiceFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected ManagerServiceFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new ManagerServiceFutureStub(channel, callOptions);
    }

    /**
     * <pre>
     * a: RunServer regista-se fornecendo o seu IP:Porto e recebe o IP:Porto do REDIS
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<isel.cd.coderunner.manager.RegisterResponse> registerRunServer(
        isel.cd.coderunner.manager.RegisterRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getRegisterRunServerMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * b: Client solicita um RunServer disponível (distribuição em Round-Robin)
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<isel.cd.coderunner.manager.GetRunServerResponse> getRunServer(
        isel.cd.coderunner.manager.GetRunServerRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGetRunServerMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_REGISTER_RUN_SERVER = 0;
  private static final int METHODID_GET_RUN_SERVER = 1;

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
        case METHODID_REGISTER_RUN_SERVER:
          serviceImpl.registerRunServer((isel.cd.coderunner.manager.RegisterRequest) request,
              (io.grpc.stub.StreamObserver<isel.cd.coderunner.manager.RegisterResponse>) responseObserver);
          break;
        case METHODID_GET_RUN_SERVER:
          serviceImpl.getRunServer((isel.cd.coderunner.manager.GetRunServerRequest) request,
              (io.grpc.stub.StreamObserver<isel.cd.coderunner.manager.GetRunServerResponse>) responseObserver);
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
        default:
          throw new AssertionError();
      }
    }
  }

  public static final io.grpc.ServerServiceDefinition bindService(AsyncService service) {
    return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
        .addMethod(
          getRegisterRunServerMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              isel.cd.coderunner.manager.RegisterRequest,
              isel.cd.coderunner.manager.RegisterResponse>(
                service, METHODID_REGISTER_RUN_SERVER)))
        .addMethod(
          getGetRunServerMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              isel.cd.coderunner.manager.GetRunServerRequest,
              isel.cd.coderunner.manager.GetRunServerResponse>(
                service, METHODID_GET_RUN_SERVER)))
        .build();
  }

  private static abstract class ManagerServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    ManagerServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return isel.cd.coderunner.manager.Manager.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("ManagerService");
    }
  }

  private static final class ManagerServiceFileDescriptorSupplier
      extends ManagerServiceBaseDescriptorSupplier {
    ManagerServiceFileDescriptorSupplier() {}
  }

  private static final class ManagerServiceMethodDescriptorSupplier
      extends ManagerServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final java.lang.String methodName;

    ManagerServiceMethodDescriptorSupplier(java.lang.String methodName) {
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
      synchronized (ManagerServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new ManagerServiceFileDescriptorSupplier())
              .addMethod(getRegisterRunServerMethod())
              .addMethod(getGetRunServerMethod())
              .build();
        }
      }
    }
    return result;
  }
}

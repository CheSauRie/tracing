package com.example.gateway;

import com.example.proto.ClientGatewayGrpc;
import com.example.proto.GatewayReply;
import com.example.proto.GatewayRequest;
import io.grpc.stub.StreamObserver;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import net.devh.boot.grpc.server.service.GrpcService;

@GrpcService
public class GatewayGrpcService extends ClientGatewayGrpc.ClientGatewayImplBase {

    private final ObservationRegistry observationRegistry;

    public GatewayGrpcService(ObservationRegistry observationRegistry) {
        this.observationRegistry = observationRegistry;
    }

    @Override
    public void ping(GatewayRequest request, StreamObserver<GatewayReply> responseObserver) {
        Observation.createNotStarted("gateway.grpc.ping", observationRegistry).observe(() -> {
            GatewayReply reply = GatewayReply.newBuilder()
                    .setMessage("Hello " + request.getName())
                    .build();
            responseObserver.onNext(reply);
            responseObserver.onCompleted();
        });
    }
}

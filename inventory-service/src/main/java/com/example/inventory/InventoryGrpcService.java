package com.example.inventory;

import com.example.proto.InventoryReply;
import com.example.proto.InventoryRequest;
import com.example.proto.InventoryServiceGrpc;
import io.grpc.stub.StreamObserver;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import net.devh.boot.grpc.server.service.GrpcService;

@GrpcService
public class InventoryGrpcService extends InventoryServiceGrpc.InventoryServiceImplBase {

    private final ObservationRegistry observationRegistry;

    public InventoryGrpcService(ObservationRegistry observationRegistry) {
        this.observationRegistry = observationRegistry;
    }

    @Override
    public void checkInventory(InventoryRequest request, StreamObserver<InventoryReply> responseObserver) {
        Observation.createNotStarted("inventory.grpc.check", observationRegistry).observe(() -> {
            InventoryReply reply = InventoryReply.newBuilder()
                    .setItemId(request.getItemId())
                    .setStatus("AVAILABLE")
                    .build();
            responseObserver.onNext(reply);
            responseObserver.onCompleted();
        });
    }
}

package com.example.operation;

import com.example.proto.InventoryServiceGrpc;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;

@Component
public class InventoryGrpcClient {

    @GrpcClient("inventoryService")
    private InventoryServiceGrpc.InventoryServiceBlockingStub inventoryStub;

    public InventoryServiceGrpc.InventoryServiceBlockingStub stub() {
        return inventoryStub;
    }
}

package io.dapr.client.domain;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;

public class InvokeBindingRequestTest {

  private String BINDING_NAME = "STORE";

  @Test
  public void testSetMetadata(){
    InvokeBindingRequest request = new InvokeBindingRequest(BINDING_NAME, "operation");
    // Null check
    request.setMetadata(null);
    assertNull(request.getMetadata());
    // Modifiability check
    Map<String, String> metadata = new HashMap<>();
    metadata.put("test", "testval");
    request.setMetadata(metadata);
    Map<String, String> initial = request.getMetadata();
    request.setMetadata(metadata);
    assertNotSame(request.getMetadata(), initial, "Should not be same map");
  }
}
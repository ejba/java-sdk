package io.dapr.it.tracing.http;

import io.dapr.client.DaprClient;
import io.dapr.client.DaprClientBuilder;
import io.dapr.client.domain.HttpExtension;
import io.dapr.it.BaseIT;
import io.dapr.it.DaprRun;
import io.dapr.it.tracing.Validation;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import org.junit.Before;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;
import java.util.UUID;

import static io.dapr.it.tracing.OpenTelemetry.createOpenTelemetry;
import static io.dapr.it.tracing.OpenTelemetry.getReactorContext;
import static org.junit.runners.Parameterized.Parameter;
import static org.junit.runners.Parameterized.Parameters;

public class TracingIT extends BaseIT {

    /**
     * Run of a Dapr application.
     */
    private DaprRun daprRun = null;

    public void setup(boolean useGrpc) throws Exception {
        daprRun = startDaprApp(
          TracingIT.class.getSimpleName(),
          Service.SUCCESS_MESSAGE,
          Service.class,
          true,
          30000);

        if (useGrpc) {
            daprRun.switchToGRPC();
        } else {
            daprRun.switchToHTTP();
        }

        // Wait since service might be ready even after port is available.
        Thread.sleep(2000);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void testInvoke(boolean useGrpc) throws Exception {
        setup(useGrpc);

        final OpenTelemetry openTelemetry = createOpenTelemetry(OpenTelemetryConfig.SERVICE_NAME);
        final Tracer tracer = openTelemetry.getTracer(OpenTelemetryConfig.TRACER_NAME);

        final String spanName = UUID.randomUUID().toString();
        Span span = tracer.spanBuilder(spanName).setSpanKind(Span.Kind.CLIENT).startSpan();

        try (DaprClient client = new DaprClientBuilder().build()) {
            try (Scope scope = span.makeCurrent()) {
                client.invokeMethod(daprRun.getAppName(), "sleep", 1, HttpExtension.POST)
                    .contextWrite(getReactorContext())
                    .block();
            }
        }
        span.end();
        OpenTelemetrySdk.getGlobalTracerManagement().shutdown();

        Validation.validate(spanName, "calllocal/tracingit-service/sleep");
    }

}

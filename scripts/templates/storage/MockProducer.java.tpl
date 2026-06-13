package com.app.infrastructure.storage.drivers;

import {{CLIENT_IMPORT}};
import io.quarkus.test.Mock;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.mockito.Mockito;

@ApplicationScoped
public class {{DRIVER_NAME}}MockProducer {
    
    @Produces
    @Mock
    @ApplicationScoped
    public {{CLIENT_CLASS}} mock{{CLIENT_CLASS}}() {
        return Mockito.mock({{CLIENT_CLASS}}.class);
    }
}

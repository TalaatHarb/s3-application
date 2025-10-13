package net.talaatharb.s3.config;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class HelperBeansTest {

    @Test
    void testObjectMapperCreation() {
        assertNotNull(HelperBeans.buildObjectMapper());
    }
}

package com.backend.fourth.common.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import java.time.ZoneId;
import java.util.TimeZone;

/** Legacy local timestamps, QR expiry conversions and scheduled jobs share this zone. */
@Configuration
public class InstitutionTimeConfig {
    @Value("${app.institution-timezone:Africa/Lusaka}")
    private String timezone;
    @PostConstruct
    void configure() { TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.of(timezone))); }
}

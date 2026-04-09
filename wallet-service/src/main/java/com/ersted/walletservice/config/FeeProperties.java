package com.ersted.walletservice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "fee")
public class FeeProperties {
    private BigDecimal deposit;
    private BigDecimal withdrawal;
    private BigDecimal transfer;
}

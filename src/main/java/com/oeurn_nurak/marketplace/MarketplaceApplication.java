package com.oeurn_nurak.marketplace;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.time.ZoneOffset;
import java.util.TimeZone;

@SpringBootApplication
public class MarketplaceApplication {

	/** Persist and serve wall-clock timestamps in UTC; clients convert to local time. */
	static {
		TimeZone.setDefault(TimeZone.getTimeZone(ZoneOffset.UTC));
	}

	public static void main(String[] args) {
		SpringApplication.run(MarketplaceApplication.class, args);
	}

}

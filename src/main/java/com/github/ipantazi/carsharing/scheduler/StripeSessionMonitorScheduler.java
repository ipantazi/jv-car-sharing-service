package com.github.ipantazi.carsharing.scheduler;

import com.github.ipantazi.carsharing.service.payment.stripe.StripeSessionMonitorService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Profile("!concurrency-test")
@Component
@RequiredArgsConstructor
public class StripeSessionMonitorScheduler {

    private final StripeSessionMonitorService monitorService;

    @Scheduled(fixedDelayString = "${stripe.session.monitor.delay}")
    public void run() {
        monitorService.checkAndExpireSessions();
    }
}

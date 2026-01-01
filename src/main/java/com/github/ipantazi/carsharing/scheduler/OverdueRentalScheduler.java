package com.github.ipantazi.carsharing.scheduler;

import com.github.ipantazi.carsharing.service.rental.OverdueRentalChecker;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Profile("!concurrency-test")
@Component
@RequiredArgsConstructor
public class OverdueRentalScheduler {
    private final OverdueRentalChecker checker;

    // @Scheduled(cron = "0 */2 * * * *") // every minute for testing
    @Scheduled(cron = "0 0 9 * * *")
    public void run() {
        checker.checkOverdueRental();
    }
}

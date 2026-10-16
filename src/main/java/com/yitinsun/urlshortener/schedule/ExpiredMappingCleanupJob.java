package com.yitinsun.urlshortener.schedule;

import com.yitinsun.urlshortener.repo.UrlMappingRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
@Slf4j
public class ExpiredMappingCleanupJob {

    private final UrlMappingRepository repository;

    public ExpiredMappingCleanupJob(UrlMappingRepository repository) {
        this.repository = repository;
    }

    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public int deleteExpired() {
        int removed = repository.deleteExpiredBefore(Instant.now());
        if (removed > 0) {
            log.info("Removed {} expired URL mappings", removed);
        }
        return removed;
    }
}

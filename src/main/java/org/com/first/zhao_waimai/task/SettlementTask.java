package org.com.first.zhao_waimai.task;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.com.first.zhao_waimai.service.AdminSettlementService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * 每日 01:00 生成昨日结算单
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SettlementTask {

    private final AdminSettlementService adminSettlementService;

    @Scheduled(cron = "0 0 1 * * ?")
    public void generateYesterday() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        int created = adminSettlementService.generateByTask(yesterday);
        log.info("生成 {} 结算单 {} 张", yesterday, created);
    }
}

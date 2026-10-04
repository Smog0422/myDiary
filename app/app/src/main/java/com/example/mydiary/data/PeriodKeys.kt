package com.example.mydiary.data

import java.time.LocalDate

/**
 * 计算当前周期的标识键。
 * 委托给 [PeriodGenerator] 保证与同步服务使用同一算法。
 */
fun currentPeriodKeys(now: LocalDate = LocalDate.now()): Pair<String, String> {
    return Pair(PeriodGenerator.weekKey(now), PeriodGenerator.monthKey(now))
}

package com.sphere.shortvideos.helper.task

import com.sphere.shortvideos.helper.MoneyCacheHelper
import com.sphere.shortvideos.helper.mmkv.MMKVData
import com.sphere.shortvideos.helper.reward.RewardHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Date：2026/1/26
 * Describe:
 */
object TaskHelper {
    private const val MIN = 60_000L
    private const val DAY_MS = 86_400_000L

    private var havaAddWatchIndex by MMKVData("")
    private val list = listOf(MIN * 10, MIN * 15, MIN * 30, MIN * 60, MIN * 120, MIN * 240)
    private var singIn7Day = listOf<Double>()
    private var lastSingInIndex by MMKVData(-1)
    private var signInLastDate by MMKVData("")
    private var signInStreak by MMKVData(0)
    private var signInRewardRecord by MMKVData("")

    enum class SignInStatus {
        CLAIMED, CLAIMABLE, UNCLAIMED
    }

    data class SignInDayState(val day: Int, val reward: Double, val status: SignInStatus)

    private fun getClaimedSet(): MutableSet<Int> {
        if (havaAddWatchIndex.isBlank()) return mutableSetOf()
        return havaAddWatchIndex.split(",").filter { it.isNotBlank() }.mapNotNull { it.toIntOrNull() }.toMutableSet()
    }

    fun fetchCurClaimedIndex(): Int {
        val s = getClaimedSet()
        if (s.isEmpty()) return -1
        return s.max()
    }

    fun fetchTaskPopReward(): Pair<Double, String> {
        return RewardHelper.getConfigByLanguage().getTaskPopReward(MoneyCacheHelper.fetchCurMoney())
    }


    fun canClaimWatchReward(index: Int): Boolean {
        if (index !in list.indices) return false
        val watchTime = MoneyCacheHelper.watchVideoTime
        if (watchTime < list[index]) return false
        val claimed = getClaimedSet()
        return !claimed.contains(index)
    }

    private fun saveClaimedSet(set: Set<Int>) {
        havaAddWatchIndex = set.sorted().joinToString(",")
    }

    fun clickWatchReward(index: Int): Double? {
        if (index !in list.indices) return null
        val watchTime = MoneyCacheHelper.watchVideoTime
        val needTime = list[index]
        if (watchTime < needTime) return null
        val claimed = getClaimedSet()
        if (claimed.contains(index)) return null
        val rewards = MoneyCacheHelper.fetchAllWatchReword()
        val claimIndices = mutableListOf<Int>()
        if (index >= 1) { // 15分钟档及以上，未领的前置奖励一起领取
            for (i in 0..index) {
                if (watchTime >= list[i] && !claimed.contains(i)) {
                    claimIndices.add(i)
                }
            }
        } else {
            claimIndices.add(index)
        }
        if (claimIndices.isEmpty()) return null

        var totalReward = 0.0
        claimIndices.forEach { i ->
            val rewardValue = rewards.getOrNull(i)?.first ?: 0.0
            if (rewardValue > 0) {
                totalReward += rewardValue
            }
            claimed.add(i)
        }
        saveClaimedSet(claimed)
        return totalReward
    }

    fun fetchSignReword(): List<Double> {
        if (singIn7Day.isEmpty()) {
            if (lastSingInIndex == -1) {
                var money = MoneyCacheHelper.fetchCurMoney()
                if (money == 0.0) {
                    money = RewardHelper.getConfigByLanguage().getRewardNewUser().first
                }
                RewardHelper.getConfigByLanguage().signIn.forEachIndexed { index, range ->
                    if (range.isInRange(money)) {
                        lastSingInIndex = index
                    }
                }
            }
            if (lastSingInIndex == -1) {
                return emptyList()
            }
            singIn7Day = RewardHelper.getConfigByLanguage().signIn[lastSingInIndex].reward
        }
        return singIn7Day
    }


    fun fetchSignInStates(): List<SignInDayState> {
        val today = getTodayString()
        val gapDays = calcGapDays(signInLastDate, today)
        if (gapDays > 1 || (signInStreak >= 7 && gapDays >= 1)) {
            resetSignIn()
        }
        val claimedToday = gapDays == 0
        val rewards = buildSevenDayRewards(fetchSignReword())
        val recorded = getSignInRewardRecord()
        return (1..7).map { day ->
            val reward = if (day <= recorded.size) {
                recorded[day - 1]
            } else {
                rewards[day - 1]
            }
            val status = when {
                day <= signInStreak -> SignInStatus.CLAIMED
                day == signInStreak + 1 && !claimedToday -> SignInStatus.CLAIMABLE
                else -> SignInStatus.UNCLAIMED
            }
            SignInDayState(day, reward, status)
        }
    }

    fun claimSignInReward(): Double? {
        val today = getTodayString()
        val gapDays = calcGapDays(signInLastDate, today)
        if (gapDays == 0) return null
        if (gapDays > 1 || signInStreak >= 7) {
            resetSignIn()
        }
        val rewards = buildSevenDayRewards(fetchSignReword())
        val dayIndex = signInStreak
        if (dayIndex !in 0..6) return null
        val reward = rewards[dayIndex]
        val record = getSignInRewardRecord()
        if (record.size <= dayIndex) {
            while (record.size < dayIndex) {
                record.add(0.0)
            }
            record.add(reward)
        } else {
            record[dayIndex] = reward
        }
        signInStreak = (signInStreak + 1).coerceAtMost(7)
        signInLastDate = today
        saveSignInRewardRecord(record)
        return reward
    }

    private fun buildSevenDayRewards(list: List<Double>): List<Double> {
        if (list.size >= 7) return list.take(7)
        return list + List(7 - list.size) { 0.0 }
    }

    private fun getSignInRewardRecord(): MutableList<Double> {
        if (signInRewardRecord.isBlank()) return mutableListOf()
        return signInRewardRecord.split(",").filter { it.isNotBlank() }.mapNotNull { it.toDoubleOrNull() }
            .toMutableList()
    }

    private fun saveSignInRewardRecord(list: List<Double>) {
        signInRewardRecord = list.joinToString(",")
    }

    private fun resetSignIn() {
        signInStreak = 0
        signInLastDate = ""
        signInRewardRecord = ""
    }

    private fun getTodayString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    private fun calcGapDays(lastDate: String, today: String): Int {
        if (lastDate.isBlank()) return Int.MAX_VALUE
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val last = fmt.parse(lastDate) ?: return Int.MAX_VALUE
        val now = fmt.parse(today) ?: return Int.MAX_VALUE
        val diff = now.time - last.time
        return (diff / DAY_MS).toInt()
    }

}
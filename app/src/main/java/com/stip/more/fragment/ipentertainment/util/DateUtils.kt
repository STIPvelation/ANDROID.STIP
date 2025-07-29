package com.stip.stip.more.fragment.ipentertainment.util

import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Formats the remaining time in consistent English format: "n days n hours"
 */
fun formatRemainingTime(endTime: Date): String {
    val currentTime = Date()
    
    // If already ended
    if (endTime.before(currentTime)) {
        return "Ended"
    }
    
    val diffInMillis = endTime.time - currentTime.time
    val diffInDays = TimeUnit.MILLISECONDS.toDays(diffInMillis)
    val remainingHoursMillis = diffInMillis - TimeUnit.DAYS.toMillis(diffInDays)
    val diffInHours = TimeUnit.MILLISECONDS.toHours(remainingHoursMillis)
    val diffInMinutes = TimeUnit.MILLISECONDS.toMinutes(diffInMillis)
    
    return when {
        diffInDays > 0 && diffInHours > 0 -> "${diffInDays} days ${diffInHours} hours"
        diffInDays > 0 -> "${diffInDays} days"
        diffInHours > 0 -> "${diffInHours} hours"
        diffInMinutes > 0 -> "${diffInMinutes} min"
        else -> "<1 min"
    }
}

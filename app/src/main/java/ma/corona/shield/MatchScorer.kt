package ma.corona.shield

import android.content.Context
import android.util.Log

import androidx.preference.PreferenceManager
import java.util.concurrent.ThreadLocalRandom

object MatchScorer {

    const val TAG = "MatchScorer"
    const val REDUNDANCY_BONUS = 10f
    const val MAX_MATCH_BONUS = 30f
    const val MATCH_BONUS_STEP = 10f
    const val SCORE_THRESHOLD = 50f


    fun computeScore(matches: List<TagsManager.TagMatch>): Float {

        var score = 0f
        for (match in matches) {
            if (score > 0)
                score += REDUNDANCY_BONUS

            for (i in 0 until match.matches.size) {
                if (match.matches[i])
                    score += MAX_MATCH_BONUS - MATCH_BONUS_STEP * i
            }

        }
        return score

    }

    fun judgeScore(score: Float, ctx: Context): Boolean {
        val paranoid = PreferenceManager.getDefaultSharedPreferences(ctx).getBoolean("paranoid_mode", false)
        Log.v(TAG, "Paranoid: $paranoid")
        return if (paranoid) score > 0 else score >= SCORE_THRESHOLD

    }

}

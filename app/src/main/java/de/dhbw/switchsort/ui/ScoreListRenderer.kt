package de.dhbw.switchsort.ui

import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import de.dhbw.switchsort.R
import de.dhbw.switchsort.score.HighScoreEntry

/** Baut die Highscore-Liste als einfache Zeilen (kein RecyclerView, max. 10 Einträge). */
object ScoreListRenderer {

    fun render(
        list: LinearLayout,
        entries: List<HighScoreEntry>,
        formatTime: (Long) -> String
    ) {
        list.removeAllViews()
        val context = list.context
        val inflater = LayoutInflater.from(context)
        entries.forEachIndexed { index, entry ->
            val row = inflater.inflate(R.layout.item_score, list, false)
            row.findViewById<TextView>(R.id.text_rank).text =
                context.getString(R.string.number, index + 1)
            row.findViewById<TextView>(R.id.text_name).text = entry.playerName
            row.findViewById<TextView>(R.id.text_meta).text = context.getString(
                R.string.scores_meta, entry.boardSize, formatTime(entry.durationMillis)
            )
            row.findViewById<TextView>(R.id.text_points).text =
                context.getString(R.string.number, entry.score)
            if (index == 0) row.setBackgroundResource(R.drawable.bg_glass)
            list.addView(row)
        }
    }
}

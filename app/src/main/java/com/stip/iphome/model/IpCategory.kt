package com.stip.stip.iphome.model

import android.content.Context
import com.stip.stip.R

enum class IpCategory {
    PATENT, TRADEMARK, FRANCHISE, MUSIC, DANCE, ART, MOVIE, DRAMA,
    BM, GAME, CHARACTER, COMICS, OTHER;

    fun toDisplayString(context: Context): String {
        return when (this) {
            PATENT -> context.getString(R.string.category_patent)
            TRADEMARK -> context.getString(R.string.category_trademark)
            FRANCHISE -> context.getString(R.string.category_franchise)
            MUSIC -> context.getString(R.string.category_music)
            DANCE -> context.getString(R.string.category_dance)
            ART -> context.getString(R.string.category_art)
            MOVIE -> context.getString(R.string.category_movie)
            DRAMA -> context.getString(R.string.category_drama)
            BM -> context.getString(R.string.category_bm)
            GAME -> context.getString(R.string.category_game)
            CHARACTER -> context.getString(R.string.category_character)
            COMICS -> context.getString(R.string.category_comics)
            OTHER -> context.getString(R.string.category_other)
        }
    }

    companion object {
        fun fromRaw(raw: String?): IpCategory {
            return when (raw) {
                "Patent" -> PATENT
                "Trademark" -> TRADEMARK
                "Franchise" -> FRANCHISE
                "Music" -> MUSIC
                "Dance" -> DANCE
                "Art" -> ART
                "Movie" -> MOVIE
                "Drama" -> DRAMA
                "BM" -> BM
                "Game" -> GAME
                "Character" -> CHARACTER
                "Comics" -> COMICS
                else -> OTHER
            }
        }
    }
}

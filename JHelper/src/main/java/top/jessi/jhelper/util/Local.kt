package top.jessi.jhelper.util

import android.content.Context
import top.jessi.jhelper.R

/**
 * Created by Jessi on 2026/2/6 18:28
 * Email：17324719944@189.cn
 * Describe：本地化处理方法
 */
object Local {

    @JvmStatic
    fun normalizeLanguageName(context: Context, rawName: String): String {
        val name = rawName.lowercase()
        val tempName = when {
            name.contains("chinese") -> when {
                name.contains("simplified") -> context.getString(R.string.simplified_chinese)
                name.contains("traditional") -> context.getString(R.string.traditional_chinese)
                else -> context.getString(R.string.chinese)
            }

            name.contains("cyrillic") -> context.getString(R.string.cyrillic)
            name.contains("estonian") -> context.getString(R.string.estonian)
            name.contains("tamil") -> context.getString(R.string.tamil)
            name.contains("telugu") -> context.getString(R.string.telugu)
            name.contains("malayalam") -> context.getString(R.string.malayalam)
            name.contains("kannada") -> context.getString(R.string.kannada)
            name.contains("hindi") -> context.getString(R.string.hindi)
            name.contains("gallegan") -> context.getString(R.string.gallegan)
            name.contains("basque") -> context.getString(R.string.basque)
            name.contains("russian") -> context.getString(R.string.russian)
            name.contains("arabic") -> context.getString(R.string.arabic)
            name.contains("ukrainian") -> context.getString(R.string.ukrainian)
            name.contains("turkish") -> context.getString(R.string.turkish)
            name.contains("swedish") -> context.getString(R.string.swedish)
            name.contains("serbian") -> context.getString(R.string.serbian)
            name.contains("slovenian") -> context.getString(R.string.slovenian)
            name.contains("slovak") -> context.getString(R.string.slovak)
            name.contains("romanian") -> context.getString(R.string.romanian)
            name.contains("polish") -> context.getString(R.string.polish)
            name.contains("dutch") -> context.getString(R.string.dutch)
            name.contains("greek") -> context.getString(R.string.greek)
            name.contains("norwegian bokmål") || name.contains("norwegian bokmaal") -> context.getString(R.string.norwegian_bokmål)
            name.contains("macedonian") -> context.getString(R.string.macedonian)
            name.contains("latvian") -> context.getString(R.string.latvian)
            name.contains("lithuanian") -> context.getString(R.string.lithuanian)
            name.contains("halh mongolian") -> context.getString(R.string.halh_mongolian)
            name.contains("italian") -> context.getString(R.string.italian)
            name.contains("icelandic") -> context.getString(R.string.icelandic)
            name.contains("hungarian") -> context.getString(R.string.hungarian)
            name.contains("croatian") -> context.getString(R.string.croatian)
            name.contains("hebrew") -> context.getString(R.string.hebrew)
            name.contains("french") -> context.getString(R.string.french)
            name.contains("finnish") -> context.getString(R.string.finnish)
            name.contains("standard estonian") -> context.getString(R.string.standard_estonian)
            name.contains("german") -> context.getString(R.string.german)
            name.contains("bulgarian") -> context.getString(R.string.bulgarian)
            name.contains("catalan") -> context.getString(R.string.catalan)
            name.contains("czech") -> context.getString(R.string.czech)
            name.contains("danish") -> context.getString(R.string.danish)
            name.contains("vietnamese") -> context.getString(R.string.vietnamese)
            name.contains("thai") -> context.getString(R.string.thai)
            name.contains("portuguese") -> context.getString(R.string.portuguese)
            name.contains("malay") -> context.getString(R.string.malay)
            name.contains("japanese") -> context.getString(R.string.japanese)
            name.contains("korean") -> context.getString(R.string.korean)
            name.contains("mandarin") -> context.getString(R.string.mandarin)
            name.contains("indonesian") -> context.getString(R.string.indonesian)
            name.contains("spanish") -> context.getString(R.string.spanish)
            name.contains("disable") -> context.getString(R.string.disable)
            name.contains("norwegian nynorsk") -> context.getString(R.string.norwegian_nynorsk)
            name.contains("norwegian") -> context.getString(R.string.norwegian)
            name.contains("fil") -> context.getString(R.string.fil)
            name.contains("latin america") -> context.getString(R.string.latin_america)
            // Mandarin (Guoyu) AAC - [English] 因为有这种情况，所以英语最后匹配
            name.contains("english") || name.contains("british") -> context.getString(R.string.english)
            else -> rawName
        }
        val formatAreaName = when {
            name.contains("latin america") -> "$tempName (${context.getString(R.string.latin_america)})"
            name.contains("united states") -> "$tempName (${context.getString(R.string.united_states)})"
            name.contains("denmark") -> "$tempName (${context.getString(R.string.denmark)})"
            name.contains("czechia") -> "$tempName (${context.getString(R.string.czechia)})"
            name.contains("germany") -> "$tempName (${context.getString(R.string.germany)})"
            name.contains("modern") -> "$tempName (${context.getString(R.string.modern)})"
            name.contains("finland") -> "$tempName (${context.getString(R.string.finland)})"
            name.contains("france") -> "$tempName (${context.getString(R.string.france)})"
            name.contains("israel") -> "$tempName (${context.getString(R.string.israel)})"
            name.contains("hungary") -> "$tempName (${context.getString(R.string.hungary)})"
            name.contains("iceland") -> "$tempName (${context.getString(R.string.iceland)})"
            name.contains("italy") -> "$tempName (${context.getString(R.string.italy)})"
            name.contains("cyrillic") -> "$tempName (${context.getString(R.string.cyrillic)})"
            name.contains("norway") -> "$tempName (${context.getString(R.string.norway)})"
            name.contains("netherlands") -> "$tempName (${context.getString(R.string.netherlands)})"
            name.contains("poland") -> "$tempName (${context.getString(R.string.poland)})"
            name.contains("romania") -> "$tempName (${context.getString(R.string.romania)})"
            name.contains("slovakia") -> "$tempName (${context.getString(R.string.slovakia)})"
            name.contains("latin") -> "$tempName (${context.getString(R.string.latin)})"
            name.contains("sweden") -> "$tempName (${context.getString(R.string.sweden)})"
            name.contains("türkiye") -> "$tempName (${context.getString(R.string.türkiye)})"
            name.contains("portugal") || rawName == "PT" -> "$tempName (${context.getString(R.string.portugal)})"
            name.contains("spain") -> "$tempName (${context.getString(R.string.spain)})"
            name.contains("brazil") || rawName == "BR" -> "$tempName (${context.getString(R.string.brazil)})"
            name.contains("hong kong") -> "$tempName (${context.getString(R.string.hong_kong)})"
            name.contains("world") -> "$tempName (${context.getString(R.string.world)})"
            name.contains("india") -> "$tempName (${context.getString(R.string.india)})"
            name.contains("thailand") -> "$tempName (${context.getString(R.string.thailand)})"
            name.contains("malaysia") -> "$tempName (${context.getString(R.string.malaysia)})"
            name.contains("indonesia") -> "$tempName (${context.getString(R.string.indonesia)})"
            else -> tempName
        }
        val newName = if (name.contains("sdh")) {
            "$formatAreaName [${context.getString(R.string.deaf_subtitles)}]"
                .replace("sdh", "")
                .replace("SDH", "")
        } else if (name.endsWith("ad")) {
            "$formatAreaName [${context.getString(R.string.audio_description)}]"
                .replace("ad", "")
                .replace("AD", "")
        } else if (name.contains("stripped")) {
            "$formatAreaName [${context.getString(R.string.stripped)}]"
                .replace("stripped", "")
                .replace("Stripped", "")
        } else if (name.contains("dubtitle")) {
            "$formatAreaName [${context.getString(R.string.dubtitle)}]"
                .replace("dubtitle", "")
                .replace("Dubtitle", "")
        } else if (name.contains("default")) {
            "$formatAreaName [${context.getString(R.string.default_name)}]"
                .replace("default", "")
                .replace("Default", "")
        } else if (name.contains("track")) {
            "[$formatAreaName]".replace("track", context.getString(R.string.track))
                .replace("Track", context.getString(R.string.track))
        } else {
            formatAreaName
        }
        return newName
    }

}
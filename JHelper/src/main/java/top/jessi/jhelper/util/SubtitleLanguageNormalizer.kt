package top.jessi.jhelper.util

import android.content.Context
import top.jessi.jhelper.R

/**
 * Created by Jessi on 2026/2/7 11:41
 * Email：17324719944@189.cn
 * Describe：
 */

/**
 * 字幕语言名称标准化工具
 *
 * 负责：
 * 1. 语言识别（Chinese / English / Hindi ...）
 * 2. 地区识别（Brazil / Hong Kong / Latin America ...）
 * 3. 字幕特性（SDH / AD / Dubtitle / Stripped / Default）
 *
 * Created for long-term maintainability
 */
object SubtitleLanguageNormalizer {

    // ---------- 数据结构 ----------

    private data class LangRule(
        val keywords: List<String>,
        val resId: Int
    )

    private data class FeatureRule(
        val keyword: String,
        val resId: Int
    )

    // ---------- 语言规则（顺序很重要）----------
    private val LANGUAGE_RULES = listOf(
        LangRule(listOf("chinese simplified", "simplified chinese"), R.string.simplified_chinese),
        LangRule(listOf("chinese traditional", "traditional chinese"), R.string.traditional_chinese),
        LangRule(listOf("chinese"), R.string.chinese),

        LangRule(listOf("cyrillic"), R.string.cyrillic),
        LangRule(listOf("standard estonian"), R.string.standard_estonian),
        LangRule(listOf("estonian"), R.string.estonian),

        LangRule(listOf("tamil"), R.string.tamil),
        LangRule(listOf("telugu"), R.string.telugu),
        LangRule(listOf("malayalam"), R.string.malayalam),
        LangRule(listOf("kannada"), R.string.kannada),
        LangRule(listOf("hindi"), R.string.hindi),

        LangRule(listOf("gallegan"), R.string.gallegan),
        LangRule(listOf("basque"), R.string.basque),
        LangRule(listOf("russian"), R.string.russian),
        LangRule(listOf("arabic"), R.string.arabic),
        LangRule(listOf("ukrainian"), R.string.ukrainian),

        LangRule(listOf("turkish"), R.string.turkish),
        LangRule(listOf("swedish"), R.string.swedish),
        LangRule(listOf("serbian"), R.string.serbian),
        LangRule(listOf("slovenian"), R.string.slovenian),
        LangRule(listOf("slovak"), R.string.slovak),
        LangRule(listOf("romanian"), R.string.romanian),
        LangRule(listOf("polish"), R.string.polish),
        LangRule(listOf("dutch"), R.string.dutch),
        LangRule(listOf("greek"), R.string.greek),

        LangRule(listOf("norwegian bokmål", "norwegian bokmaal"), R.string.norwegian_bokmål),
        LangRule(listOf("norwegian nynorsk"), R.string.norwegian_nynorsk),
        LangRule(listOf("norwegian"), R.string.norwegian),

        LangRule(listOf("macedonian"), R.string.macedonian),
        LangRule(listOf("latvian"), R.string.latvian),
        LangRule(listOf("lithuanian"), R.string.lithuanian),
        LangRule(listOf("halh mongolian"), R.string.halh_mongolian),

        LangRule(listOf("italian"), R.string.italian),
        LangRule(listOf("icelandic"), R.string.icelandic),
        LangRule(listOf("hungarian"), R.string.hungarian),
        LangRule(listOf("croatian"), R.string.croatian),
        LangRule(listOf("hebrew"), R.string.hebrew),
        LangRule(listOf("french"), R.string.french),
        LangRule(listOf("finnish"), R.string.finnish),
        LangRule(listOf("german"), R.string.german),
        LangRule(listOf("bulgarian"), R.string.bulgarian),
        LangRule(listOf("catalan"), R.string.catalan),
        LangRule(listOf("czech"), R.string.czech),
        LangRule(listOf("danish"), R.string.danish),
        LangRule(listOf("vietnamese"), R.string.vietnamese),
        LangRule(listOf("thai"), R.string.thai),
        LangRule(listOf("portuguese"), R.string.portuguese),
        LangRule(listOf("malay"), R.string.malay),
        LangRule(listOf("japanese"), R.string.japanese),
        LangRule(listOf("korean"), R.string.korean),
        LangRule(listOf("mandarin"), R.string.mandarin),
        LangRule(listOf("indonesian"), R.string.indonesian),
        LangRule(listOf("spanish"), R.string.spanish),

        LangRule(listOf("fil"), R.string.fil),
        LangRule(listOf("latin america"), R.string.latin_america),
        LangRule(listOf("english", "british"), R.string.english)
    )

    // ---------- 地区规则 ----------
    private val REGION_RULES = mapOf(
        "latin america" to R.string.latin_america,
        "united states" to R.string.united_states,
        "denmark" to R.string.denmark,
        "czechia" to R.string.czechia,
        "germany" to R.string.germany,
        "modern" to R.string.modern,
        "finland" to R.string.finland,
        "france" to R.string.france,
        "israel" to R.string.israel,
        "hungary" to R.string.hungary,
        "iceland" to R.string.iceland,
        "italy" to R.string.italy,
        "cyrillic" to R.string.cyrillic,
        "norway" to R.string.norway,
        "netherlands" to R.string.netherlands,
        "poland" to R.string.poland,
        "romania" to R.string.romania,
        "slovakia" to R.string.slovakia,
        "latin" to R.string.latin,
        "sweden" to R.string.sweden,
        "türkiye" to R.string.türkiye,
        "portugal" to R.string.portugal,
        "spain" to R.string.spain,
        "brazil" to R.string.brazil,
        "hong kong" to R.string.hong_kong,
        "world" to R.string.world,
        "india" to R.string.india,
        "thailand" to R.string.thailand,
        "malaysia" to R.string.malaysia
    )

    // ---------- 字幕特性 ----------
    private val FEATURE_RULES = listOf(
        FeatureRule("sdh", R.string.deaf_subtitles),
        FeatureRule("ad", R.string.audio_description),
        FeatureRule("stripped", R.string.stripped),
        FeatureRule("dubtitle", R.string.dubtitle),
        FeatureRule("default", R.string.default_name),
        FeatureRule("track", R.string.track)
    )

    // ---------- 对外 API ----------

    @JvmStatic
    fun normalize(context: Context, rawName: String): String {
        if (rawName.isBlank()) return rawName

        val name = rawName.lowercase()

        // 1️⃣ 语言
        val language = findLanguage(context, name, rawName)

        // 2️⃣ 地区
        val region = findRegion(context, name)

        val languageWithRegion = region?.let {
            "$language ($it)"
        } ?: language

        // 3️⃣ 特性
        val feature = findFeature(context, name)

        val result = feature?.let {
            "$languageWithRegion [$it]"
        } ?: languageWithRegion

        return result.trim()
    }

    // ---------- 内部方法 ----------

    private fun findLanguage(
        context: Context,
        name: String,
        rawName: String
    ): String {
        val rule = LANGUAGE_RULES.firstOrNull {
            it.keywords.any { key -> name.contains(key) }
        }
        return rule?.let { context.getString(it.resId) } ?: rawName
    }

    private fun findRegion(context: Context, name: String): String? {
        val entry = REGION_RULES.entries.firstOrNull {
            name.contains(it.key)
        }
        return entry?.let { context.getString(it.value) }
    }

    private fun findFeature(context: Context, name: String): String? {
        val rule = FEATURE_RULES.firstOrNull {
            name.contains(it.keyword)
        }
        return rule?.let { context.getString(it.resId) }
    }
}
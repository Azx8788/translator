package com.gameocr.app.translate

import com.gameocr.app.data.Settings
import com.gameocr.app.data.TranslatorEngine

/**
 * AI 翻译质量指令策略（P2）。
 *
 * 仅对指令跟随型 chat 后端（OpenAI 兼容 / Anthropic / 本地 llama）注入；
 * Sakura / Hy-MT2 等特化微调模型白名单排除——它们不是指令跟随型，
 * 往输入里拼"纠正错别字/译文要口语化"这类 directive 会破坏输出格式。
 */
object AiQualityPromptPolicy {

    private val BLACKLIST = setOf(
        TranslatorEngine.LOCAL_SAKURA,
        TranslatorEngine.LOCAL_HY_MT2,
    )

    /**
     * 根据用户设置构造 AI 质量指令字符串；黑名单引擎或无任何开关启用时返回空。
     * 调用方应将返回值 append 到 system prompt 末尾。
     */
    fun directive(settings: Settings): String {
        if (settings.translatorEngine in BLACKLIST) return ""
        val parts = buildList {
            if (settings.aiFixTypo) add("纠正原文中的错别字")
            if (settings.aiFixGrammar) add("修正原文的语法错误")
            if (settings.aiFluent) add("译文要自然流畅，不要机翻腔")
            if (settings.aiStyle.isNotBlank()) add("译文风格：${settings.aiStyle}")
            if (settings.aiPreserveNames) add("保留人名、地名、品牌等专有名词不翻译")
        }
        if (parts.isEmpty()) return ""
        return "\n\n--- AI 质量指令 ---\n" + parts.joinToString("；") + "。"
    }
}

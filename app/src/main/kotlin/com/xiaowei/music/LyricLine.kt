package com.xiaowei.music

data class LyricWord(
    val timeMs: Long,
    val text: String
)

data class LyricLine(
    val timeMs: Long,
    val text: String,
    val words: List<LyricWord> = emptyList(),
    val translation: String = ""
) {
    val isWordByWord: Boolean get() = words.isNotEmpty()
}

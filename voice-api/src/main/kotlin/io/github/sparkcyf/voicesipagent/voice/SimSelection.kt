package io.github.sparkcyf.voicesipagent.voice

object SimSelection {
    const val HEADER = "X-VSA-SIM"
    fun parse(values: List<String>): Int? = when {
        values.isEmpty() -> null
        values.size != 1 -> 0
        values.single().trim() == "1" -> 1
        values.single().trim() == "2" -> 2
        else -> 0
    }
}

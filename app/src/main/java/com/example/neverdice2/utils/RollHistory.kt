package com.example.neverdice2.utils

import com.example.neverdice2.mqtt.DiceResult
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Gerencia o histórico de rolagens de dados.
 * Armazena até 50 resultados em memória.
 */
class RollHistory {

    private val history = mutableListOf<HistoryEntry>()
    private val maxEntries = 50

    /**
     * Adiciona um novo resultado ao histórico.
     */
    fun add(diceResult: DiceResult) {
        val entry = HistoryEntry(
            timestamp = System.currentTimeMillis(),
            dice = diceResult.dice,
            result = diceResult.result,
            player = diceResult.player
        )

        history.add(0, entry) // Adiciona no início (mais recente primeiro)

        // Limita o tamanho do histórico
        if (history.size > maxEntries) {
            history.removeAt(history.size - 1)
        }
    }

    /**
     * Retorna o histórico formatado para exibição.
     */
    fun getFormattedHistory(): String {
        if (history.isEmpty()) {
            return "Nenhuma rolagem registrada ainda."
        }

        val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

        return history.joinToString("\n\n") { entry ->
            val time = dateFormat.format(Date(entry.timestamp))
            "🎲 ${entry.dice.uppercase()} → ${entry.result}\n" +
                    "👤 ${entry.player}\n" +
                    "🕐 $time"
        }
    }

    /**
     * Retorna a lista de entradas do histórico (para uso programático).
     */
    fun getEntries(): List<HistoryEntry> = history.toList()

    /**
     * Limpa o histórico.
     */
    fun clear() {
        history.clear()
    }

    /**
     * Retorna o número de entradas no histórico.
     */
    fun size(): Int = history.size

    /**
     * Classe que representa uma entrada no histórico.
     */
    data class HistoryEntry(
        val timestamp: Long,
        val dice: String,
        val result: Int,
        val player: String
    )
}
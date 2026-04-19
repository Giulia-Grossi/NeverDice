package com.example.neverdice2.mqtt

import android.util.Log
import com.hivemq.client.mqtt.MqttClient
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient
import org.json.JSONObject
import java.util.UUID

data class DiceResult(
    val dice: String,
    val result: Int,
    val player: String
)

class MqttManager {

    private var mqttClient: Mqtt3AsyncClient? = null

    var onResultReceived: ((DiceResult) -> Unit)? = null
    var onConnected: (() -> Unit)? = null
    var onConnectionLost: ((Throwable?) -> Unit)? = null

    companion object {
        private const val TAG = "MqttManager"
    }

    fun connect(
        brokerUrl: String,
        username: String? = null,
        password: String? = null,
        resultTopic: String = "dice/result"
    ) {
        val clientId = "NeverDice2_${UUID.randomUUID()}"
        val host = brokerUrl.substringAfter("://").substringBefore(":")
        val port = brokerUrl.substringAfterLast(":").toInt()

        val builder = MqttClient.builder()
            .useMqttVersion3()
            .identifier(clientId)
            .serverHost(host)
            .serverPort(port)

        // Se for SSL, usa configuração padrão (sem customização)
        if (brokerUrl.startsWith("ssl://") || brokerUrl.startsWith("mqtts://")) {
            builder.sslWithDefaultConfig()
        }

        mqttClient = builder.buildAsync()

        val connectBuilder = mqttClient?.connectWith()
        if (!username.isNullOrEmpty()) {
            connectBuilder?.simpleAuth()
                ?.username(username)
                ?.password(password?.toByteArray() ?: byteArrayOf())
                ?.applySimpleAuth()
        }

        connectBuilder?.send()?.whenComplete { _, throwable ->
            if (throwable != null) {
                Log.e(TAG, "Falha na conexão: ${throwable.message}", throwable)
                onConnectionLost?.invoke(throwable)
            } else {
                Log.i(TAG, "Conectado ao broker MQTT")
                onConnected?.invoke()
                subscribeToTopic(resultTopic)
            }
        }
    }

    private fun subscribeToTopic(topic: String) {
        mqttClient?.subscribeWith()
            ?.topicFilter(topic)
            ?.callback { publish ->
                val payload = String(publish.payloadAsBytes)
                Log.i(TAG, "Mensagem recebida: $payload")
                processIncomingMessage(payload)
            }
            ?.send()
            ?.whenComplete { _, throwable ->
                if (throwable != null) {
                    Log.e(TAG, "Falha ao inscrever: ${throwable.message}")
                } else {
                    Log.i(TAG, "Inscrito no tópico: $topic")
                }
            }
    }

    private fun processIncomingMessage(payload: String) {
        try {
            val json = JSONObject(payload)
            val dice = json.optString("dice", "desconhecido")
            val result = json.optInt("result", -1)
            val player = json.optString("player", "Anônimo")

            if (result != -1) {
                onResultReceived?.invoke(DiceResult(dice, result, player))
            }
        } catch (e: Exception) {
            val result = payload.trim().toIntOrNull()
            if (result != null) {
                onResultReceived?.invoke(DiceResult("desconhecido", result, "API"))
            }
        }
    }

    fun publish(topic: String, payload: String) {
        mqttClient?.publishWith()
            ?.topic(topic)
            ?.payload(payload.toByteArray())
            ?.send()
            ?.whenComplete { _, throwable ->
                if (throwable != null) {
                    Log.e(TAG, "Falha ao publicar: ${throwable.message}")
                } else {
                    Log.d(TAG, "Publicado em $topic: $payload")
                }
            }
    }

    fun disconnect() {
        mqttClient?.disconnect()
    }
}
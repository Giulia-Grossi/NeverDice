package com.example.neverdice2

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.neverdice2.mqtt.DiceResult
import com.example.neverdice2.mqtt.MqttManager
import com.example.neverdice2.render.DiceRenderer
import com.example.neverdice2.utils.RollHistory
import io.github.sceneview.SceneView

class MainActivity : AppCompatActivity() {

    private lateinit var sceneView: SceneView
    private lateinit var diceRenderer: DiceRenderer
    private lateinit var mqttManager: MqttManager
    private lateinit var rollButton: Button
    private lateinit var historyButton: Button
    private lateinit var resultTextView: TextView
    private lateinit var diceSpinner: Spinner

    // Histórico de rolagens
    private val rollHistory = RollHistory()

    // Tipos de dado disponíveis
    private val diceTypes = arrayOf("d4", "d6", "d8", "d10", "d12", "d20")
    private var currentDiceType = "d20"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sceneView = findViewById(R.id.sceneView)
        sceneView.lifecycle = lifecycle

        initializeViews()
        setupDiceSpinner()
        initializeComponents()
        setupListeners()

        connectToMqttBroker()
    }

    private fun initializeViews() {
        rollButton = findViewById(R.id.rollButton)
        historyButton = findViewById(R.id.historyButton)
        resultTextView = findViewById(R.id.resultTextView)
        diceSpinner = findViewById(R.id.diceSpinner)
    }

    /**
     * Configura o Spinner (menu suspenso) para seleção do tipo de dado.
     */
    private fun setupDiceSpinner() {
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            diceTypes.map { it.uppercase() }
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        diceSpinner.adapter = adapter

        // Define o D20 como padrão (índice 5)
        diceSpinner.setSelection(5)

        diceSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                currentDiceType = diceTypes[position]
                Toast.makeText(this@MainActivity, "Dado selecionado: ${currentDiceType.uppercase()}", Toast.LENGTH_SHORT).show()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Mantém o D20 como padrão
                currentDiceType = "d20"
            }
        }
    }

    private fun initializeComponents() {
        diceRenderer = DiceRenderer(sceneView)
        mqttManager = MqttManager()

        setupMqttCallbacks()
    }

    private fun setupMqttCallbacks() {
        mqttManager.onResultReceived = { diceResult ->
            runOnUiThread {
                // Para a animação
                diceRenderer.showResult(diceResult.result)

                // Exibe o número grande centralizado
                resultTextView.text = diceResult.result.toString()
                resultTextView.visibility = View.VISIBLE

                // Adiciona ao histórico
                rollHistory.add(diceResult)

                // Exibe Toast com informações
                Toast.makeText(
                    this,
                    "${diceResult.player} rolou ${diceResult.dice}: ${diceResult.result}",
                    Toast.LENGTH_LONG
                ).show()

                // Esconde o número após 3 segundos
                Handler(Looper.getMainLooper()).postDelayed({
                    resultTextView.visibility = View.INVISIBLE
                }, 3000)
            }
        }

        mqttManager.onConnected = {
            runOnUiThread {
                Toast.makeText(this, "Conectado ao broker MQTT", Toast.LENGTH_SHORT).show()
            }
        }

        mqttManager.onConnectionLost = { cause ->
            runOnUiThread {
                Toast.makeText(this, "Conexão perdida. Tentando reconectar...", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupListeners() {
        rollButton.setOnClickListener {
            onRollButtonClicked()
        }

        historyButton.setOnClickListener {
            showHistoryDialog()
        }
    }

    private fun onRollButtonClicked() {
        // Carrega o modelo 3D do dado selecionado
        diceRenderer.loadDice(currentDiceType)

        // Inicia a animação de rolagem
        diceRenderer.startRollingAnimation()

        // Esconde o resultado anterior
        resultTextView.visibility = View.INVISIBLE

        // Publica a solicitação MQTT
        val requestPayload = """
            {
                "command": "roll",
                "dice": "$currentDiceType"
            }
        """.trimIndent()

        mqttManager.publish(
            topic = "dice/request",
            payload = requestPayload
        )
    }

    /**
     * Exibe um diálogo com o histórico de rolagens.
     */
    private fun showHistoryDialog() {
        val history = rollHistory.getFormattedHistory()

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("📜 Histórico de Rolagens")
            .setMessage(history)
            .setPositiveButton("Limpar") { _, _ ->
                rollHistory.clear()
                Toast.makeText(this, "Histórico limpo!", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Fechar", null)
            .show()
    }

    private fun connectToMqttBroker() {
        val brokerUrl = "ssl://mqtt.astrum.app.br:8883"
        val username = "esp32"
        val password = "esp32"
        val resultTopic = "dice/result"

        mqttManager.connect(
            brokerUrl = brokerUrl,
            username = username,
            password = password,
            resultTopic = resultTopic
        )
    }

    override fun onDestroy() {
        mqttManager.disconnect()
        diceRenderer.destroy()
        sceneView.destroy()
        super.onDestroy()
    }
}
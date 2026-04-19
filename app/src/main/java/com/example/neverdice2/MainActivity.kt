package com.example.neverdice2

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.neverdice2.mqtt.DiceResult
import com.example.neverdice2.mqtt.MqttManager
import com.example.neverdice2.render.DiceRenderer
import io.github.sceneview.SceneView

/**
 * Activity principal do aplicativo.
 * Coordena a comunicação MQTT e a renderização 3D.
 */
class MainActivity : AppCompatActivity() {

    // View que exibe a cena 3D
    private lateinit var sceneView: SceneView

    // Gerenciador de renderização dos dados
    private lateinit var diceRenderer: DiceRenderer

    // Gerenciador de conexão MQTT
    private lateinit var mqttManager: MqttManager

    // Botão que inicia a rolagem
    private lateinit var rollButton: Button

    // TextView que exibe o resultado centralizado
    private lateinit var resultTextView: TextView

    // Dado atualmente selecionado (padrão: D20)
    private var currentDiceType = "d20"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Configura o ciclo de vida do SceneView
        sceneView = findViewById(R.id.sceneView)
        sceneView.lifecycle = lifecycle

        // Inicializa as views e componentes
        initializeViews()
        initializeComponents()
        setupListeners()

        // Conecta ao broker MQTT
        connectToMqttBroker()
    }

    /**
     * Inicializa as referências das views do layout.
     */
    private fun initializeViews() {
        rollButton = findViewById(R.id.rollButton)
        resultTextView = findViewById(R.id.resultTextView)
    }

    /**
     * Inicializa os gerenciadores de renderização e MQTT.
     */
    private fun initializeComponents() {
        // Cria o renderizador 3D associado ao SceneView
        diceRenderer = DiceRenderer(sceneView)

        // Cria o gerenciador MQTT
        mqttManager = MqttManager()

        // Configura os callbacks do MQTT
        setupMqttCallbacks()
    }

    /**
     * Configura os callbacks que serão chamados quando eventos MQTT ocorrerem.
     */
    private fun setupMqttCallbacks() {
        // Callback chamado quando um resultado de rolagem é recebido
        mqttManager.onResultReceived = { diceResult ->
            // Este código é executado em uma thread de background.
            // Precisamos mudar para a UI thread para atualizar a interface.
            runOnUiThread {
                // Para a animação no resultado recebido
                diceRenderer.showResult(diceResult.result)

                // Exibe o número grande centralizado
                resultTextView.text = diceResult.result.toString()
                resultTextView.visibility = View.VISIBLE

                // Exibe um Toast com informações completas
                Toast.makeText(
                    this,
                    "${diceResult.player} rolou ${diceResult.dice}: ${diceResult.result}",
                    Toast.LENGTH_LONG
                ).show()

                // Esconde o número após 10 segundos
                Handler(Looper.getMainLooper()).postDelayed({
                    resultTextView.visibility = View.INVISIBLE
                }, 10000)
            }
        }

        // Callback chamado quando a conexão MQTT é estabelecida
        mqttManager.onConnected = {
            runOnUiThread {
                Toast.makeText(this, "Conectado ao broker MQTT", Toast.LENGTH_SHORT).show()
            }
        }

        // Callback chamado quando a conexão é perdida
        mqttManager.onConnectionLost = { cause ->
            runOnUiThread {
                Toast.makeText(
                    this,
                    "Conexão perdida. Tentando reconectar...",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    /**
     * Configura os listeners dos botões.
     */
    private fun setupListeners() {
        rollButton.setOnClickListener {
            onRollButtonClicked()
        }
    }

    /**
     * Chamado quando o botão de rolagem é pressionado.
     */
    private fun onRollButtonClicked() {
        // Carrega o modelo 3D do dado selecionado
        diceRenderer.loadDice(currentDiceType)

        // Inicia a animação de rolagem
        diceRenderer.startRollingAnimation()

        // Esconde o resultado anterior
        resultTextView.visibility = View.INVISIBLE

        // Publica uma mensagem MQTT solicitando a rolagem
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
     * Conecta ao broker MQTT.
     */
    private fun connectToMqttBroker() {
        // Configurações do broker MQTT
        val brokerUrl = "ssl://mqtt.astrum.app.br:8883"
        val username = "esp32"
        val password = "esp32"  // Preencha se houver senha
        val resultTopic = "dice/result"

        // Inicia a conexão
        mqttManager.connect(
            brokerUrl = brokerUrl,
            username = username,
            password = password,
            resultTopic = resultTopic
        )
    }

    override fun onDestroy() {
        // Libera os recursos antes da Activity ser destruída
        mqttManager.disconnect()
        diceRenderer.destroy()
        sceneView.destroy()
        super.onDestroy()
    }
}
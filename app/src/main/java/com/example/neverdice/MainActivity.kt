package com.example.neverdice

import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope

import info.mqtt.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.IMqttActionListener
import org.eclipse.paho.client.mqttv3.IMqttToken
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.MqttCallback
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.MqttClient

import io.github.sceneview.SceneView
import io.github.sceneview.math.Rotation
import io.github.sceneview.node.ModelNode

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject


class MainActivity : AppCompatActivity() {

    lateinit var client: MqttAndroidClient
    lateinit var textoValor: TextView
    lateinit var diceType: TextView
    lateinit var connectionStatus: TextView
    lateinit var btnSaveResult: Button
    lateinit var btnHistory: Button
    lateinit var sceneView: SceneView
    lateinit var finalResultText: TextView

    private var currentDiceModelNode: ModelNode? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        textoValor = findViewById(R.id.textoValor)
        diceType = findViewById(R.id.diceType)
        connectionStatus = findViewById(R.id.connectionStatus)
        btnSaveResult = findViewById(R.id.btnSaveResult)
        btnHistory = findViewById(R.id.btnHistory)
        sceneView = findViewById(R.id.diceDisplay)
        finalResultText = findViewById(R.id.finalResultText)

        // Configura listeners para os botões
        btnSaveResult.setOnClickListener {
            Toast.makeText(this, "Funcionalidade de salvar resultado em desenvolvimento!", Toast.LENGTH_SHORT).show()
        }
        btnHistory.setOnClickListener {
            Toast.makeText(this, "Funcionalidade de histórico em desenvolvimento!", Toast.LENGTH_SHORT).show()
        }


        conectarMQTT()
    }

    fun conectarMQTT() {
        val serverURI = "mqtts://mqtt.astrum.app.br:8883"
        val clientId = MqttClient.generateClientId()

        client = MqttAndroidClient(applicationContext, serverURI, clientId)

        val options = MqttConnectOptions()
        options.isCleanSession = true
        options.userName = "esp32" // Substitua pelo seu username
        options.password = "esp32".toCharArray() // Substitua pela sua senha

        connectionStatus.text = "Status: Conectando..."

        client.connect(options, null, object : IMqttActionListener {
            override fun onSuccess(asyncActionToken: IMqttToken?) {
                connectionStatus.text = "Status: Conectado!"
                inscreverTopico()
            }

            override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                connectionStatus.text = "Status: Erro ao conectar"
                Toast.makeText(this@MainActivity, "Erro ao conectar ao MQTT: ${exception?.message}", Toast.LENGTH_LONG).show()
            }
        })
    }

    fun inscreverTopico() {
        val topic = "teste/dado"

        client.subscribe(topic, 0)

        client.setCallback(object : MqttCallback{
            override fun connectionLost(cause: Throwable?) {
                connectionStatus.text = "Status: Conexão perdida"
                Toast.makeText(this@MainActivity, "Conexão MQTT perdida: ${cause?.message}", Toast.LENGTH_LONG).show()
            }

            override fun messageArrived(topic: String?, message: MqttMessage?) {
                val payload = message.toString()
                runOnUiThread{
                    try {
                        val jsonObject = JSONObject(payload)
                        val tipoDado = jsonObject.getString("tipoDado")
                        val resultado = jsonObject.getInt("resultado")

                        diceType.text = "Tipo de Dado: $tipoDado"
                        textoValor.text = "Resultado: $resultado"

                        // Esconde o resultado anterior e inicia a animação
                        finalResultText.visibility = View.GONE
                        loadAndAnimateDice(tipoDado, resultado)

                    } catch (e: Exception) {
                        textoValor.text = "Erro ao processar mensagem: ${e.message}"
                        Toast.makeText(this@MainActivity, "Erro ao processar mensagem MQTT: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }

            override fun deliveryComplete(token: IMqttDeliveryToken?) {
                // Não é necessário implementar para este caso
            }
        })
    }

    private fun loadAndAnimateDice(diceType: String, result: Int) {
        val modelPath = when (diceType.lowercase()) {
            "d4" -> "models/d4.glb"
            "d6" -> "models/d6.glb"
            "d8" -> "models/d8.glb"
            "d10" -> "models/d10.glb"
            "d12" -> "models/d12.glb"
            "d20" -> "models/d20.glb"
            else -> "models/d6.glb" // Modelo padrão
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Carrega o modelo de forma assíncrona
                val model = sceneView.modelLoader.loadModel(modelPath)

                withContext(Dispatchers.Main) {
                    model?.let { loadedModel ->
                        // Remove o dado anterior se existir
                        currentDiceModelNode?.let { sceneView.removeNode(it) }

                        // Cria um novo ModelNode
                        val modelNode = ModelNode(
                            engine = sceneView.engine,
                            modelInstance = loadedModel.instance
                        ).apply {
                            scale = io.github.sceneview.math.Scale(0.5f)
                        }

                        // Adiciona à cena
                        sceneView.addNode(modelNode)
                        currentDiceModelNode = modelNode

                        // Animação de rotação simples
                        val animator = ObjectAnimator.ofFloat(modelNode, "rotation", 0f, 360f * 5)
                        animator.duration = 2000
                        animator.interpolator = AccelerateDecelerateInterpolator()
                        animator.addUpdateListener { animation ->  ->
                            val value = animation.animatedValue as Float
                            modelNode.rotation = Rotation(y = value)
                        }
                        animator.start()

                        // Exibe o resultado após a animação
                        sceneView.postDelayed({
                            finalResultText.text = result.toString()
                            finalResultText.visibility = View.VISIBLE
                        }, 2000)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
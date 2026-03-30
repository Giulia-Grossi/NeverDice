package com.example.neverdice

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.airbnb.lottie.LottieAnimationView
import info.mqtt.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.IMqttActionListener
import org.eclipse.paho.client.mqttv3.IMqttToken
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.MqttCallback
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.MqttClient
import org.json.JSONObject

class MainActivity : AppCompatActivity() {

    lateinit var client: MqttAndroidClient
    lateinit var textoValor: TextView
    lateinit var diceType: TextView
    lateinit var connectionStatus: TextView
    lateinit var btnSaveResult: Button
    lateinit var btnHistory: Button
    lateinit var lottieAnimationView: LottieAnimationView
    lateinit var finalResultText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        textoValor = findViewById(R.id.textoValor)
        diceType = findViewById(R.id.diceType)
        connectionStatus = findViewById(R.id.connectionStatus)
        btnSaveResult = findViewById(R.id.btnSaveResult)
        btnHistory = findViewById(R.id.btnHistory)
        lottieAnimationView = findViewById(R.id.diceDisplay) // Usando o mesmo ID para o LottieAnimationView
        finalResultText = findViewById(R.id.finalResultText) // Inicializa o novo TextView

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
        options.userName = "esp32"
        options.password = "esp32".toCharArray()

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

        client.subscribe(topic,0)

        client.setCallback(object : MqttCallback{
            override fun connectionLost(cause: Throwable?) {
                connectionStatus.text = "Status: Conexão perdida"
                Toast.makeText(this@MainActivity, "Conexão MQTT perdida: ${cause?.message}", Toast.LENGTH_LONG).show()
            }

            override fun messageArrived(topic: String?, message: MqttMessage?) {
                val payload = message.toString()
                runOnUiThread {
                    try {
                        val jsonObject = JSONObject(payload)
                        val tipoDado = jsonObject.getString("tipoDado")
                        val resultado = jsonObject.getInt("resultado")

                        diceType.text = "Tipo de Dado: $tipoDado"
                        textoValor.text = "Resultado: $resultado"

                        // Exibe o resultado final sobre o dado
                        finalResultText.text = resultado.toString()
                        finalResultText.visibility = TextView.VISIBLE

                        // Lógica para exibir animação do dado
                        // Você precisará de arquivos Lottie JSON para cada tipo de dado
                        when (tipoDado) {
                            "d4" -> lottieAnimationView.setAnimation(R.raw.dice_d4) // Exemplo, você precisará criar R.raw.dice_d4.json
                            "d6" -> lottieAnimationView.setAnimation(R.raw.dice_d6)
                            "d8" -> lottieAnimationView.setAnimation(R.raw.dice_d8)
                            "d10" -> lottieAnimationView.setAnimation(R.raw.dice_d10)
                            "d12" -> lottieAnimationView.setAnimation(R.raw.dice_d12)
                            "d20" -> lottieAnimationView.setAnimation(R.raw.dice_d20)
                            else -> lottieAnimationView.setAnimation(R.raw.dice_default) // Animação padrão
                    }
                    lottieAnimationView.playAnimation()
                } catch (e: Exception){
                    textoValor.text = "Erro ao processar mensagem: ${e.message}"
                    Toast.makeText(this@MainActivity, "Erro ao processar mensagem MQTT: ${e.message}", Toast.LENGTH_LONG).show()
                }
                }
            }

            override fun deliveryComplete(token: IMqttDeliveryToken?) {

            }
        })
    }
}
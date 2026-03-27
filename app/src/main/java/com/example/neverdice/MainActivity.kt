package com.example.neverdice

import android.os.Bundle
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.neverdice.ui.theme.NeverDiceTheme
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.IMqttActionListener
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.IMqttToken
import org.eclipse.paho.client.mqttv3.MqttCallback
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttMessage

class MainActivity : ComponentActivity() {

    lateinit var client: MqttAndroidClient
    lateinit var texto: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        texto = findViewById(R.id.textoValor)

        conectarMQTT()
    }
    fun conectarMQTT() {
        val serverURI = "tcp://broker.hivemq.com:1883"
        val clientId = MqttClient.generateClientId()

        client = MqttAndroidClient(applicationContext, serverURI, clientId)

        val options = MqttConnectOptions()
        options.isCleanSession = true

        client.connect(options, null, object : IMqttActionListener {
            override fun onSuccess(asyncActionToken: IMqttToken?) {
                texto.text = "Conectado!"
                inscreverTopico()
            }

            override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                texto.text = "Erro ao conectar"
            }
        })
    }

    fun inscreverTopico() {
        val topic = "teste/dado"

        client.subscribe(topic,0)

        client.setCallback(object : MqttCallback{
            override fun connectionLost(cause: Throwable?) {
                texto.text = "Conexão perdida"
            }

            override fun messageArrived(topic: String?, message: MqttMessage?) {
                val valor = message.toString()

                runOnUiThread{
                    texto.text = "Valor recebido: $valor"
                }
            }

            override fun deliveryComplete(token: IMqttDeliveryToken?) {

            }
        })
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    NeverDiceTheme {
        Greeting("Android")
    }
}
package com.example.neverdice2.render

import android.os.Handler
import android.os.Looper
import android.util.Log
import io.github.sceneview.SceneView
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.math.Scale
import io.github.sceneview.node.ModelNode
import kotlin.random.Random

/**
 * Gerenciador de renderização 3D dos dados.
 * Responsável por carregar os modelos .glb e executar as animações.
 */
class DiceRenderer(private val sceneView: SceneView) {

    // Referência ao nó (node) do modelo 3D atualmente carregado
    private var currentModelNode: ModelNode? = null

    // Handler para executar código na thread principal (UI thread)
    private val mainHandler = Handler(Looper.getMainLooper())

    // Flag para controlar se a animação está rodando
    private var isAnimating = false

    private var baseRotation: Rotation = Rotation(0f, 0f, 0f)

    // Runnable que será executado repetidamente para criar a animação
    private val animationRunnable = object : Runnable {
        override fun run() {
            if (isAnimating && currentModelNode != null) {
                // Aplica uma rotação aleatória a cada frame
                // CORREÇÃO: A API usa propriedades diretamente, não um lambda transform
                currentModelNode?.rotation = Rotation(
                    x = Random.nextFloat() * 360f,
                    y = Random.nextFloat() * 360f,
                    z = Random.nextFloat() * 360f
                )
                // Agenda o próximo frame (~60 FPS = a cada 16ms)
                mainHandler.postDelayed(this, 16)
            }
        }
    }

    companion object {
        private const val TAG = "DiceRenderer"
    }

    /**
     * Carrega um modelo de dado a partir dos assets.
     * @param diceType Nome do arquivo sem extensão (ex: "d6", "d20")
     */
    fun loadDice(diceType: String) {
        // Remove o modelo anterior da cena, se existir
        currentModelNode?.let {
            sceneView.removeChildNode(it)
        }

        try {
            // Constrói o caminho para o arquivo .glb dentro da pasta assets
            val assetPath = "$diceType.glb"

            // Carrega o modelo usando o SceneView
            val modelInstance = sceneView.modelLoader.createModelInstance(assetPath)

            baseRotation = when (diceType) {
                "d6" -> Rotation(0f, 0f, 0f)
                "d8" -> Rotation(0f, 0f, 0f)
                "d10" -> Rotation(0f, 0f, 0f)
                "d12" -> Rotation(0f, 0f, 0f)
                "d20" -> Rotation(0f, 0f, 0f)  // Seus valores do Blender
                else -> Rotation(0f, 0f, 0f)
            }

            // CORREÇÃO: A API do ModelNode aceita scaleToUnits como parâmetro no construtor
            // e centerOrigin como Position, não como função separada [citation:1]
            currentModelNode = ModelNode(
                modelInstance = modelInstance,
                scaleToUnits = 0.5f,        // Escala o modelo para 0.5 unidades
                centerOrigin = Position(0f, 0f, 0f)  // Centraliza o pivô do modelo
            ).apply {
                // Ajusta a posição inicial do dado
                position = Position(0f, 0f, 0f)
                rotation = baseRotation  // Aplica a rotação base imediatamente
            }

            // Adiciona o nó à cena para que seja renderizado
            sceneView.addChildNode(currentModelNode!!)

            Log.i(TAG, "Modelo $diceType carregado com sucesso")

        } catch (e: Exception) {
            Log.e(TAG, "Erro ao carregar modelo $diceType: ${e.message}", e)
        }
    }

    /**
     * Inicia a animação de rolagem aleatória.
     * O dado girará continuamente até que showResult() seja chamado.
     */
    fun startRollingAnimation() {
        if (currentModelNode == null) {
            Log.w(TAG, "Nenhum modelo carregado para animar")
            return
        }

        isAnimating = true
        mainHandler.post(animationRunnable)
        Log.d(TAG, "Animação de rolagem iniciada")
    }

    /**
     * Para a animação de rolagem e exibe uma face específica.
     * @param result Número sorteado (1 a 20, dependendo do dado)
     */
    fun showResult(result: Int) {
        // Para a animação contínua
        isAnimating = false
        mainHandler.removeCallbacks(animationRunnable)

        currentModelNode ?: return

        // NÃO sobrescreve a rotação base!
        // Apenas mantém a rotação atual (que já inclui a base)
        Log.i(TAG, "Animação finalizada. Resultado: $result")
    }

    /**
     * Libera os recursos do modelo atual.
     * Deve ser chamado quando a Activity for destruída.
     */
    fun destroy() {
        isAnimating = false
        mainHandler.removeCallbacksAndMessages(null)

        currentModelNode?.let {
            sceneView.removeChildNode(it)
            currentModelNode = null
        }

        Log.d(TAG, "Recursos liberados")
    }
}
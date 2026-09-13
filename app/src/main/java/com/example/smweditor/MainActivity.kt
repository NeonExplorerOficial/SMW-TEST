package com.example.smweditor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.smweditor.rom.LevelReader
import com.example.smweditor.rom.SmwRom

/**
 * Smoke-test screen: pick a ROM file, load it, and show the primary
 * header of level 0 to confirm the parsing pipeline works end to end.
 * No rendering yet -- this is v0.1, see README for the roadmap.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    RomPickerScreen()
                }
            }
        }
    }
}

@Composable
fun RomPickerScreen() {
    var status by remember { mutableStateOf("Ningún ROM cargado todavía.") }

    val context = androidx.compose.ui.platform.LocalContext.current

    val pickRomLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) {
            status = "No se seleccionó ningún archivo."
            return@rememberLauncherForActivityResult
        }
        try {
            val bytes = context.contentResolver.openInputStream(uri)?.readBytes()
                ?: throw IllegalStateException("No se pudo leer el archivo")

            val rom = SmwRom.fromBytes(bytes)

            // --- Diagnóstico: mostramos esto SIEMPRE, sin importar si
            // la descompresión funciona o no, para poder ver qué
            // dirección se está calculando realmente. ---
            val pointerAddr = com.example.smweditor.rom.LevelTables.LAYER1_POINTERS
            val pointerFileOffset = rom.toFileOffset(pointerAddr)
            val target = rom.readPointer24(pointerAddr)
            val targetFileOffset = rom.toFileOffset(target)
            val rawAtPointer = rom.readBytes(pointerAddr, 3)
            val rawAtTarget = if (targetFileOffset + 8 <= bytes.size)
                bytes.copyOfRange(targetFileOffset, targetFileOffset + 8)
            else
                ByteArray(0)

            val diag = buildString {
                appendLine("Tamaño archivo: ${bytes.size} (0x${bytes.size.toString(16)})")
                appendLine("Cabecera de copiadora: ${rom.hasCopierHeader}")
                appendLine("Tabla L1 (SNES $05E000) -> offset archivo: 0x${pointerFileOffset.toString(16)}")
                appendLine("Bytes crudos ahí (lo,hi,bank): ${rawAtPointer.joinToString(" ") { "%02X".format(it) }}")
                appendLine("Target decodificado (SNES): 0x${target.toString(16)}")
                appendLine("Target -> offset archivo: 0x${targetFileOffset.toString(16)}")
                appendLine("Primeros 8 bytes ahí: ${rawAtTarget.joinToString(" ") { "%02X".format(it) }}")
            }

            val reader = LevelReader(rom)
            val header = reader.readPrimaryHeader(0)

            status = buildString {
                appendLine(diag)
                appendLine("--- Nivel 0 (cabecera primaria) ---")
                appendLine("Tileset: ${header.tileset}")
                appendLine("Modo de nivel: ${header.levelMode}")
                appendLine("Música: ${header.music}")
                appendLine("Sprite set: ${header.spriteSet}")
                appendLine("Paleta BG: ${header.bgPalette}")
                appendLine("Paleta FG: ${header.fgPalette}")
                appendLine("Longitud (pantallas): ${header.levelLengthScreens}")
            }
        } catch (e: Exception) {
            // Igual mostramos el diagnóstico aunque falle la descompresión,
            // reconstruyéndolo aparte porque el bloque de arriba puede
            // haber fallado antes de armar `diag`.
            status = try {
                val bytes = context.contentResolver.openInputStream(uri)?.readBytes()!!
                val rom = SmwRom.fromBytes(bytes)
                val pointerAddr = com.example.smweditor.rom.LevelTables.LAYER1_POINTERS
                val pointerFileOffset = rom.toFileOffset(pointerAddr)
                val target = rom.readPointer24(pointerAddr)
                val targetFileOffset = rom.toFileOffset(target)
                val rawAtPointer = rom.readBytes(pointerAddr, 3)
                val rawAtTarget = if (targetFileOffset + 8 <= bytes.size)
                    bytes.copyOfRange(targetFileOffset, targetFileOffset + 8)
                else
                    ByteArray(0)
                buildString {
                    appendLine("Error al leer el ROM: ${e.message}")
                    appendLine()
                    appendLine("Tamaño archivo: ${bytes.size} (0x${bytes.size.toString(16)})")
                    appendLine("Cabecera de copiadora: ${rom.hasCopierHeader}")
                    appendLine("Tabla L1 -> offset archivo: 0x${pointerFileOffset.toString(16)}")
                    appendLine("Bytes crudos (lo,hi,bank): ${rawAtPointer.joinToString(" ") { "%02X".format(it) }}")
                    appendLine("Target decodificado (SNES): 0x${target.toString(16)}")
                    appendLine("Target -> offset archivo: 0x${targetFileOffset.toString(16)}")
                    appendLine("Primeros 8 bytes ahí: ${rawAtTarget.joinToString(" ") { "%02X".format(it) }}")
                }
            } catch (e2: Exception) {
                "Error al leer el ROM: ${e.message}\n(y no se pudo generar diagnóstico: ${e2.message})"
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("SMW Editor — v0.1 (núcleo de datos)", style = MaterialTheme.typography.titleLarge)

        Button(onClick = { pickRomLauncher.launch(arrayOf("*/*")) }) {
            Text("Elegir ROM (.smc / .sfc)")
        }

        Text(status)
    }
}

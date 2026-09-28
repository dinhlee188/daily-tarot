package com.dinhlee188.dailytarot

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

data class TarotCard(val id: Int, val name: String, val reversed: Boolean)
data class PickedCard(val order: Int, val card: TarotCard)

enum class SpreadType(val label: String, val slots: List<String>) {
    DAILY("Daily", listOf("Năng lượng chính", "Điều cần chú ý", "Lời khuyên")),
    SITUATION("Situation", listOf("Tình huống", "Điều ẩn", "Hướng xử lý")),
    CUSTOM("Custom", listOf("Lá 1", "Lá 2", "Lá 3"))
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { TarotApp() } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TarotApp() {
    val context = LocalContext.current
    var spread by remember { mutableStateOf(SpreadType.DAILY) }
    var deck by remember { mutableStateOf(emptyList<TarotCard>()) }
    var picks by remember { mutableStateOf(listOf<PickedCard>()) }
    var shuffled by remember { mutableStateOf(false) }
    var question by remember { mutableStateOf("") }
    var copied by remember { mutableStateOf(false) }

    fun shuffleDeck() {
        deck = allCardNames.shuffled().mapIndexed { index, name ->
            TarotCard(index, name, Random.nextBoolean())
        }
        picks = emptyList()
        copied = false
        shuffled = true
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Daily Tarot") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            Text("Chọn spread", fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SpreadType.values().forEach { item ->
                    FilterChip(
                        selected = spread == item,
                        onClick = {
                            spread = item
                            picks = emptyList()
                            shuffled = false
                            copied = false
                        },
                        label = { Text(item.label) }
                    )
                }
            }

            if (spread == SpreadType.CUSTOM) {
                OutlinedTextField(
                    value = question,
                    onValueChange = { question = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Câu hỏi") }
                )
            }

            Spacer(Modifier.height(12.dp))

            if (!shuffled) {
                Button(onClick = { shuffleDeck() }, modifier = Modifier.fillMaxWidth()) {
                    Text("XÁO BÀI")
                }
                Text(
                    "Deck đủ 78 lá. Thứ tự + xuôi/ngược được khóa ngay lúc xáo.",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.weight(1f))
            } else {
                Text("Đã xáo. Chọn 3 lá theo đúng thứ tự mày muốn rút.")
                Spacer(Modifier.height(8.dp))

                LazyVerticalGrid(
                    columns = GridCells.Fixed(6),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(4.dp)
                ) {
                    itemsIndexed(deck) { index, card ->
                        val alreadyPicked = picks.any { it.card.id == card.id }
                        Card(
                            modifier = Modifier
                                .padding(3.dp)
                                .aspectRatio(0.68f)
                                .clickable(enabled = !alreadyPicked && picks.size < 3) {
                                    picks = picks + PickedCard(picks.size + 1, card)
                                }
                        ) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(if (alreadyPicked) "✓" else (index + 1).toString())
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                picks.forEach { picked ->
                    val slot = spread.slots.getOrElse(picked.order - 1) { "Lá " + picked.order }
                    Text(
                        picked.order.toString() + ". " + slot + " — " + picked.card.name + " — " + orientationText(picked.card),
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (picks.size == 3) {
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            val date = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
                            val lines = mutableListOf<String>()
                            lines += spread.label + " — " + date
                            if (spread == SpreadType.CUSTOM && question.isNotBlank()) {
                                lines += "Question: " + question
                            }
                            picks.forEach { p ->
                                val slot = spread.slots.getOrElse(p.order - 1) { "Lá " + p.order }
                                lines += p.order.toString() + ". " + slot + " — " + p.card.name + " — " + orientationText(p.card)
                            }
                            val result = lines.joinToString("\\n")
                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            cm.setPrimaryClip(ClipData.newPlainText("Tarot result", result))
                            saveHistory(context, result)
                            copied = true
                        }) { Text(if (copied) "ĐÃ COPY" else "COPY KẾT QUẢ") }

                        OutlinedButton(onClick = { shuffleDeck() }) {
                            Text("NEW READING")
                        }
                    }
                }
            }

            HistoryBlock(context)
        }
    }
}

fun saveHistory(context: Context, result: String) {
    val prefs = context.getSharedPreferences("tarot_history", Context.MODE_PRIVATE)
    val old = prefs.getString("items", "") ?: ""
    val merged = if (old.isBlank()) result else result + "\\n---\\n" + old
    prefs.edit().putString("items", merged.take(20000)).apply()
}

@Composable
fun HistoryBlock(context: Context) {
    var open by remember { mutableStateOf(false) }
    val prefs = context.getSharedPreferences("tarot_history", Context.MODE_PRIVATE)
    val history = prefs.getString("items", "") ?: ""
    TextButton(onClick = { open = !open }) {
        Text(if (open) "Ẩn history" else "Xem history")
    }
    if (open) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 220.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(if (history.isBlank()) "Chưa có reading nào." else history)
        }
    }
}

fun orientationText(card: TarotCard): String = if (card.reversed) "Ngược" else "Xuôi"

val allCardNames = listOf(
    "The Fool","The Magician","The High Priestess","The Empress","The Emperor","The Hierophant",
    "The Lovers","The Chariot","Strength","The Hermit","Wheel of Fortune","Justice","The Hanged Man",
    "Death","Temperance","The Devil","The Tower","The Star","The Moon","The Sun","Judgement","The World",
    "Ace of Wands","Two of Wands","Three of Wands","Four of Wands","Five of Wands","Six of Wands",
    "Seven of Wands","Eight of Wands","Nine of Wands","Ten of Wands","Page of Wands","Knight of Wands",
    "Queen of Wands","King of Wands",
    "Ace of Cups","Two of Cups","Three of Cups","Four of Cups","Five of Cups","Six of Cups","Seven of Cups",
    "Eight of Cups","Nine of Cups","Ten of Cups","Page of Cups","Knight of Cups","Queen of Cups","King of Cups",
    "Ace of Swords","Two of Swords","Three of Swords","Four of Swords","Five of Swords","Six of Swords",
    "Seven of Swords","Eight of Swords","Nine of Swords","Ten of Swords","Page of Swords","Knight of Swords",
    "Queen of Swords","King of Swords",
    "Ace of Pentacles","Two of Pentacles","Three of Pentacles","Four of Pentacles","Five of Pentacles",
    "Six of Pentacles","Seven of Pentacles","Eight of Pentacles","Nine of Pentacles","Ten of Pentacles",
    "Page of Pentacles","Knight of Pentacles","Queen of Pentacles","King of Pentacles"
)

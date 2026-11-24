package ir.farhadkamali.sarresid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

private val Blue = Color(0xFF1769AA)
private val PaleBlue = Color(0xFFEAF4FC)
private val Ink = Color(0xFF183247)
private val Green = Color(0xFF16845B)
private val Red = Color(0xFFD34C55)

data class Entry(
    val id: Long,
    val type: String,
    val title: String,
    val amount: Long,
    val day: Int,
    val month: Int,
    val year: Int
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("sarresid_data", MODE_PRIVATE)
        setContent {
            SarresidApp(
                loadEntries = {
                    val array = JSONArray(prefs.getString("entries", "[]"))
                    List(array.length()) { i ->
                        val o = array.getJSONObject(i)
                        Entry(o.getLong("id"), o.getString("type"), o.getString("title"),
                            o.getLong("amount"), o.getInt("day"), o.getInt("month"), o.getInt("year"))
                    }
                },
                saveEntries = { list ->
                    val array = JSONArray()
                    list.forEach { e ->
                        array.put(JSONObject().apply {
                            put("id", e.id); put("type", e.type); put("title", e.title)
                            put("amount", e.amount); put("day", e.day)
                            put("month", e.month); put("year", e.year)
                        })
                    }
                    prefs.edit().putString("entries", array.toString()).apply()
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SarresidApp(loadEntries: () -> List<Entry>, saveEntries: (List<Entry>) -> Unit) {
    var entries by remember { mutableStateOf(loadEntries()) }
    val today = remember { JalaliDate.today() }
    var year by remember { mutableIntStateOf(today.year) }
    var month by remember { mutableIntStateOf(today.month) }
    var selectedTab by remember { mutableStateOf("کارکرد") }
    var showDialog by remember { mutableStateOf(false) }

    val monthEntries = entries.filter { it.year == year && it.month == month }
    val income = monthEntries.filter { it.type == "کارکرد" }.sumOf { it.amount }
    val expenses = monthEntries.filter { it.type == "خرید" }.sumOf { it.amount }
    val visible = monthEntries.filter { it.type == selectedTab }.sortedWith(compareByDescending<Entry> { it.day }.thenByDescending { it.id })

    MaterialTheme(
        colorScheme = lightColorScheme(primary = Blue, background = Color(0xFFF6F9FC), surface = Color.White)
    ) {
        Scaffold(
            containerColor = Color(0xFFF6F9FC),
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("سررسید", fontWeight = FontWeight.Bold, color = Ink) },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White)
                )
            },
            floatingActionButton = {
                FloatingActionButton(onClick = { showDialog = true }, containerColor = Blue, contentColor = Color.White) {
                    Icon(Icons.Default.Add, contentDescription = "ثبت مورد")
                }
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 90.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        IconButton(onClick = { if (month == 1) { month = 12; year-- } else month-- }) {
                            Icon(Icons.Default.ArrowForward, contentDescription = "ماه قبل", tint = Blue)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${JalaliDate.monthName(month)} $year", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Ink)
                            Text("گزارش ماهانه", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                        IconButton(onClick = { if (month == 12) { month = 1; year++ } else month++ }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "ماه بعد", tint = Blue)
                        }
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        SummaryCard("مجموع کارکرد", income, Green, Modifier.weight(1f))
                        SummaryCard("مجموع خرید", expenses, Red, Modifier.weight(1f))
                    }
                }
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = PaleBlue), shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.fillMaxWidth().padding(18.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text("مانده این ماه", color = Ink, style = MaterialTheme.typography.titleMedium)
                                Text("کارکرد منهای خرید", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                            }
                            Text(money(income - expenses) + " تومان", color = if (income - expenses >= 0) Blue else Red, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                        }
                    }
                }
                item {
                    Row(Modifier.fillMaxWidth().background(Color(0xFFE9EEF4), RoundedCornerShape(14.dp)).padding(4.dp)) {
                        listOf("کارکرد", "خرید").forEach { tab ->
                            Box(Modifier.weight(1f).background(if (selectedTab == tab) Color.White else Color.Transparent, RoundedCornerShape(11.dp)).clickable { selectedTab = tab }.padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                                Text(tab, color = if (selectedTab == tab) Blue else Color.Gray, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
                if (visible.isEmpty()) {
                    item {
                        Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) {
                            Column(Modifier.fillMaxWidth().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(if (selectedTab == "کارکرد") "هنوز کارکردی ثبت نشده" else "هنوز خریدی ثبت نشده", color = Ink, fontWeight = FontWeight.Medium)
                                Text("برای شروع، دکمه + را بزن", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                } else {
                    items(visible, key = { it.id }) { entry ->
                        EntryRow(entry) {
                            entries = entries.filterNot { it.id == entry.id }
                            saveEntries(entries)
                        }
                    }
                }
            }
        }
        if (showDialog) {
            AddEntryDialog(
                initialType = selectedTab, today = today,
                onDismiss = { showDialog = false },
                onSave = { title, amount, day, entryMonth, entryYear, type ->
                    val newList = entries + Entry(System.currentTimeMillis(), type, title, amount, day, entryMonth, entryYear)
                    entries = newList
                    saveEntries(newList)
                    year = entryYear; month = entryMonth; selectedTab = type
                    showDialog = false
                }
            )
        }
    }
}

@Composable
private fun SummaryCard(title: String, amount: Long, color: Color, modifier: Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.padding(15.dp)) {
            Text(title, color = Color.Gray, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(10.dp))
            Text(money(amount), color = color, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
            Text("تومان", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun EntryRow(entry: Entry, onDelete: () -> Unit) {
    Card(shape = RoundedCornerShape(15.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(entry.title, color = Ink, fontWeight = FontWeight.SemiBold)
                Text("${entry.day} ${JalaliDate.monthName(entry.month)} ${entry.year}", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(money(entry.amount) + " تومان", color = if (entry.type == "کارکرد") Green else Red, fontWeight = FontWeight.Bold)
                IconButton(onClick = onDelete, modifier = Modifier.size(30.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Color.LightGray, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun AddEntryDialog(
    initialType: String, today: JalaliDate, onDismiss: () -> Unit,
    onSave: (String, Long, Int, Int, Int, String) -> Unit
) {
    var type by remember { mutableStateOf(initialType) }
    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var day by remember { mutableStateOf(today.day.toString()) }
    var month by remember { mutableStateOf(today.month.toString()) }
    var year by remember { mutableStateOf(today.year.toString()) }
    val parsedAmount = amount.filter { it.isDigit() }.toLongOrNull()
    val valid = title.isNotBlank() && parsedAmount != null && parsedAmount > 0 &&
        (day.toIntOrNull() ?: 0) in 1..31 && (month.toIntOrNull() ?: 0) in 1..12 && (year.toIntOrNull() ?: 0) > 1300
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ثبت مورد جدید", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("کارکرد", "خرید").forEach { t ->
                        FilterChip(selected = type == t, onClick = { type = t }, label = { Text(t) })
                    }
                }
                OutlinedTextField(title, { title = it }, label = { Text("شرح") }, singleLine = true)
                OutlinedTextField(amount, { amount = it.filter { c -> c.isDigit() } }, label = { Text("مبلغ به تومان") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(day, { day = it.filter(Char::isDigit).take(2) }, label = { Text("روز") }, modifier = Modifier.weight(1f), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    OutlinedTextField(month, { month = it.filter(Char::isDigit).take(2) }, label = { Text("ماه") }, modifier = Modifier.weight(1f), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    OutlinedTextField(year, { year = it.filter(Char::isDigit).take(4) }, label = { Text("سال") }, modifier = Modifier.weight(1.4f), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                }
            }
        },
        confirmButton = { TextButton(enabled = valid, onClick = { onSave(title.trim(), parsedAmount!!, day.toInt(), month.toInt(), year.toInt(), type) }) { Text("ثبت") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
    )
}

private fun money(value: Long): String = "%,d".format(value)

private data class JalaliDate(val year: Int, val month: Int, val day: Int) {
    companion object {
        private val names = listOf("فروردین","اردیبهشت","خرداد","تیر","مرداد","شهریور","مهر","آبان","آذر","دی","بهمن","اسفند")
        fun monthName(month: Int) = names.getOrElse(month - 1) { "ماه" }
        fun today(): JalaliDate {
            val c = Calendar.getInstance()
            val gy = c.get(Calendar.YEAR); val gm = c.get(Calendar.MONTH) + 1; val gd = c.get(Calendar.DAY_OF_MONTH)
            val gDays = intArrayOf(0,31, if (isLeap(gy)) 29 else 28,31,30,31,30,31,31,30,31,30,31)
            var jy = gy - 621
            var days = 0
            for (m in 1 until gm) days += gDays[m]
            days += gd
            val start = if (isLeap(gy - 1)) 366 else 365
            val marchDay = if (isLeap(gy)) 20 else 21
            val passed = if (gm > 3 || (gm == 3 && gd >= marchDay)) days - (31 + 28 + if (isLeap(gy)) 1 else 0) - (marchDay - 1) else days + (if (isLeap(gy - 1)) 10 else 11)
            if (passed <= 0) jy--
            val jDay = if (passed > 0) passed else passed + 365 + if (isLeap(gy - 1)) 1 else 0
            val jm = if (jDay <= 186) (jDay - 1) / 31 + 1 else (jDay - 187) / 30 + 7
            val jd = if (jDay <= 186) (jDay - 1) % 31 + 1 else (jDay - 187) % 30 + 1
            return JalaliDate(jy, jm.coerceIn(1,12), jd.coerceIn(1,31))
        }
        private fun isLeap(y: Int) = (y % 4 == 0 && y % 100 != 0) || y % 400 == 0
    }
}

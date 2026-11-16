package com.bittupandit.astraquantumai

import android.content.Context
import android.util.AtomicFile
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID
import kotlin.math.abs

private data class BQuestion(val id: String, val module: String, val prompt: String, val rubric: String, val version: Int = 1)
private data class BRecord(
    val id: String, val questionId: String, val module: String, val answer: String,
    val confidence: Double?, val humanScore: Int?, val timestamp: Long, val model: String = "Groq (server-configured)"
)

private object BQuestionBank {
    private val modules = linkedMapOf(
        "Knowledge Intelligence" to listOf(
            "DNA और RNA में चार अंतर बताइए।", "प्रकाश-संश्लेषण का समीकरण और प्रक्रिया समझाइए।",
            "न्यूटन के तीनों गति नियम उदाहरण सहित बताइए।", "मौलिक अधिकार और नीति-निदेशक तत्वों में अंतर बताइए।",
            "CPI और WPI क्या मापते हैं? उनकी सीमाएँ बताइए।", "जल चक्र के चरण और जलवायु पर प्रभाव समझाइए।",
            "RAM, ROM और Storage की तुलना कीजिए।", "टीकाकरण में प्रतिरक्षा स्मृति की भूमिका बताइए।",
            "भूकंप की तीव्रता और परिमाण में अंतर बताइए।", "एंटीबायोटिक और एंटीवायरल में अंतर समझाइए।",
            "खाद्य-जाल और खाद्य-श्रृंखला की तुलना कीजिए।", "वैज्ञानिक सहमति का अर्थ और सीमा बताइए।",
            "प्राथमिक और द्वितीयक स्रोतों में अंतर बताइए।", "ऊर्जा संरक्षण का नियम उदाहरण सहित समझाइए।",
            "समुद्री ज्वार पर चंद्रमा और सूर्य का प्रभाव बताइए।", "प्रशिक्षण और परीक्षण डेटा अलग क्यों रखते हैं?",
            "मुद्रास्फीति का आम परिवार पर प्रभाव समझाइए।", "लोकतंत्र में नियंत्रण-संतुलन का अर्थ बताइए।",
            "पारिस्थितिकी तंत्र में जैव-विविधता क्यों महत्वपूर्ण है?", "समाचार में दिए आँकड़ों की विश्वसनीयता कैसे जाँचेंगे?"
        ),
        "Deep Reasoning" to listOf(
            "सभी A, B हैं और कुछ B, C हैं। क्या कुछ A, C होना आवश्यक है? तर्क दें।",
            "60 किमी/घंटा से 2 घंटे और 90 किमी/घंटा से 1 घंटे की औसत गति निकालें।",
            "अपराध 20% घटा और रिपोर्ट 30% बढ़ी। क्या निष्कर्ष नहीं निकाल सकते?",
            "कम prevalence वाले रोग में test sensitivity और specificity का अर्थ समझाएँ।",
            "दो परिकल्पनाओं में भेद करने वाला प्रयोग कैसे बनाएँगे?", "सहसंबंध को कारण मानने की गलती समझाइए।",
            "लाभ 100, हानि 80 और सफलता 0.4 हो तो expected value निकालें।",
            "किसी तर्क में छिपी मान्यता पहचानने का तरीका बताइए।",
            "एक प्रक्रिया का 40% हिस्सा दोगुना तेज हो तो कुल गति कितनी बढ़ेगी?",
            "केवल ऑनलाइन सर्वेक्षण में कौन-सा sampling bias हो सकता है?",
            "तीन संभावित कारणों में कम लागत का सर्वश्रेष्ठ परीक्षण कैसे चुनेंगे?",
            "10% वृद्धि और अगले वर्ष 10% कमी का कुल प्रभाव निकालें।",
            "Outlier औसत बदले लेकिन median नहीं—किसे चुनेंगे और क्यों?",
            "योजना सफल हुई; कैसे जाँचेंगे कि सफलता उसी के कारण हुई?",
            "अपर्याप्त जानकारी वाली पहेली में ईमानदार उत्तर क्या होगा?",
            "सीमित संसाधनों में परियोजनाओं की प्राथमिकता कैसे तय करेंगे?",
            "अपने निष्कर्ष के विरुद्ध सबसे मजबूत तर्क बनाकर जाँचिए।",
            "कारण-परिणाम के दावे में confounder कैसे पहचानेंगे?",
            "किसी निष्कर्ष के लिए आवश्यक और पर्याप्त शर्तों का अंतर बताइए।",
            "यदि दो समाधान समान हों तो निर्णय के मानदंड तय कीजिए।"
        ),
        "Creative Intelligence" to listOf(
            "ग्रामीण क्षेत्र के लिए कम लागत वाला वर्षा जल-संग्रह डिजाइन करें।",
            "बिना इंटरनेट वाले स्कूल के लिए डिजिटल शिक्षा मॉडल बनाएँ।",
            "पुरानी प्लास्टिक बोतल के दस सुरक्षित उपयोग बताइए।",
            "छोटे शहर के लिए कम लागत वाला सार्वजनिक परिवहन मॉडल सुझाएँ।",
            "स्थानीय भाषा में विज्ञान सिखाने वाली कहानी की रूपरेखा दें।",
            "बिजली कटौती में अस्पताल की ऊर्जा प्राथमिकता योजना बनाएँ।",
            "किसान के लिए मौसम, मिट्टी और बाजार सूचना जोड़ने वाला उपकरण सोचें।",
            "मोहल्ले में कचरा घटाने के लिए व्यवहार परिवर्तन प्रयोग बनाएँ।",
            "कम दृष्टि वाले व्यक्ति के लिए उपयोगी मोबाइल फीचर डिजाइन करें।",
            "आपदा के बाद अफवाह रोकने की सूचना प्रणाली सोचें।",
            "युवाओं को पुस्तकालय से जोड़ने के पाँच नए तरीके दें।",
            "शून्य बजट में छोटे व्यवसाय के लिए ग्राहक प्रतिक्रिया प्रणाली बनाएँ।",
            "बच्चों को probability सिखाने वाला खेल डिजाइन करें।",
            "घर में पानी की बर्बादी घटाने वाला feedback तंत्र सुझाएँ।",
            "स्थानीय कारीगरों को बाजार से जोड़ने का कम-तकनीकी मॉडल दें।",
            "सौर ऊर्जा से चलने वाला पोर्टेबल अध्ययन स्टेशन सोचें।",
            "Fake news पहचानने की 15 मिनट की कक्षा गतिविधि बनाएँ।",
            "सरकारी कार्यालय में प्रतीक्षा समय कम करने का उपाय दें।",
            "बुजुर्गों और बच्चों दोनों के लिए उपयोगी उत्पाद सोचें।",
            "अपने प्रस्ताव के तीन दुष्प्रभाव और समाधान बताइए।"
        ),
        "Self-Correction" to listOf(
            "दावा 'सभी बैक्टीरिया हानिकारक हैं' जाँचें और सुधारें।",
            "औसत गति को साधारण औसत मानने की गलती सुधारें।",
            "कथन 'दो घटनाएँ साथ हुईं, इसलिए पहली कारण है' की त्रुटि बताएँ।",
            "उत्तर में तथ्य, अनुमान और राय को अलग करें।",
            "1/2 + 1/3 = 2/5 में गलती समझाकर सही हल दें।",
            "स्रोत पुराना निकले तो पहले उत्तर को कैसे अपडेट करेंगे?",
            "अपने तर्क की सीमा और अपवाद पहचानने की checklist बनाएँ।",
            "उपयोगकर्ता प्रमाणित विरोधाभास दिखाए तो सुधार कैसे करेंगे?",
            "दो अलग इकाइयों की तुलना की गलती सुधारें।",
            "अनुमान का आधार गलत हो तो प्रभाव और संशोधन बताइए।",
            "अपने पिछले उत्तर के सबसे कमजोर दावे को सुधारें।",
            "प्रश्न अस्पष्ट हो तो कौन-सा स्पष्टीकरण माँगेंगे?",
            "विश्वसनीय स्रोत आपके निष्कर्ष से असहमत हो तो पुनर्मूल्यांकन करें।",
            "AI hallucination के तीन संकेत बताइए।",
            "असंभव गणना परिणाम आए तो debugging क्रम बताइए।",
            "समाधान में सुरक्षा जोखिम मिले तो संशोधित समाधान दें।",
            "अपने उत्तर की स्वतंत्र जाँच का तरीका बनाइए।",
            "अधिक confidence के बावजूद उत्तर गलत हो तो क्या बदलेंगे?",
            "गलत सामान्यीकरण को सीमित दावे में बदलें।",
            "बताएँ क्या सत्यापित हुआ, क्या अनिश्चित है और क्या सुधारना है।"
        ),
        "Scientific Verification" to listOf(
            "दावा 'घरेलू पेय सात दिन में मधुमेह ठीक करता है' जाँचने का तरीका दें।",
            "नए उपचार के लिए randomized controlled trial डिजाइन करें।",
            "p-value 0.03 का सही अर्थ और गलत व्याख्या बताइए।",
            "12 लोगों के अध्ययन को सामान्य आबादी पर लागू करने की सीमा बताइए।",
            "AI benchmark में data leakage कैसे रोकेंगे?",
            "ऐतिहासिक दावे को प्राथमिक स्रोतों से कैसे जाँचेंगे?",
            "प्रयोग में control group क्यों आवश्यक हो सकता है?",
            "केवल testimonials से क्या निष्कर्ष निकलेगा?",
            "Relative risk 50% घटा, absolute risk 2% से 1% हुआ—समझाएँ।",
            "लेखक, विधि और conflicts of interest से स्रोत जाँचें।",
            "वैज्ञानिक मॉडल की falsifiability समझाइए।",
            "Meta-analysis और single study की तुलना करें।",
            "खबर के 'वैज्ञानिकों ने साबित किया' दावे को कैसे जाँचेंगे?",
            "Measurement error और sampling error में अंतर बताइए।",
            "Correlation को causation कहने से पहले confounders जाँचें।",
            "नकली DOI या URL सत्यापित करने की प्रक्रिया दें।",
            "अध्ययन दोहराने में विफलता का अर्थ क्या है और क्या नहीं?",
            "वैज्ञानिक consensus बदलने का सही अर्थ बताइए।",
            "Sensitivity और specificity के बीच trade-off समझाइए।",
            "उत्तर के प्रमाण-स्तर और अपर्याप्त प्रमाण को स्पष्ट करें।"
        ),
        "Uncertainty Calibration" to listOf(
            "स्रोत न मिले तो उत्तर और confidence कैसे व्यक्त करेंगे?",
            "दो plausible उत्तर हों तो confidence कैसे सीमित करेंगे?",
            "0 से 1 confidence दें और अनिश्चितता के स्रोत बताएँ।",
            "किस स्थिति में 'मुझे नहीं पता' कहना सही है?",
            "60% संभावना को निश्चित तथ्य की तरह क्यों नहीं कहेंगे?",
            "Confidence और correctness में अंतर उदाहरण सहित बताइए।",
            "पुराने ज्ञान और बदलते तथ्य की अनिश्चितता कैसे सँभालेंगे?",
            "छोटे sample से निकले निष्कर्ष पर भरोसा कितना होना चाहिए?",
            "Prediction में base rate न हो तो क्या करेंगे?",
            "एक बिंदु अनुमान के बजाय संभावित range कैसे देंगे?",
            "तीन स्वतंत्र स्रोत असहमत हों तो confidence कैसे तय करेंगे?",
            "अनुमानित inputs वाली गणना में uncertainty कैसे दिखाएँगे?",
            "आधा ज्ञात और आधा अज्ञात प्रश्न कैसे उत्तर देंगे?",
            "खराब calibration के संकेत क्या हैं?",
            "Brier Score क्या मापता है?",
            "छोटे dataset में ECE की सीमाएँ क्या हैं?",
            "गलत उत्तर पर 0.95 confidence देने की समस्या कैसे मापेंगे?",
            "विशेषज्ञ या वेब सत्यापन चाहिए तो सीमा कैसे बताएँगे?",
            "Confidence interval और subjective confidence में अंतर बताइए।",
            "तथ्य, संभावना, अज्ञात और अगला सत्यापन कदम अलग लिखें।"
        )
    )
    val moduleNames = modules.keys.toList()
    val all = modules.flatMap { (module, prompts) ->
        prompts.mapIndexed { i, prompt ->
            BQuestion(module.lowercase().replace(" ", "-") + "-v1-" + (i + 1), module, prompt,
                "तथ्यात्मक शुद्धता, तर्क, पूर्णता, प्रमाण/उदाहरण और सीमाओं की ईमानदार पहचान।")
        }
    }
}

private object BStore {
    private fun file(context: Context) = AtomicFile(File(context.filesDir, "astra_benchmark_results.json"))
    fun load(context: Context): List<BRecord> = runCatching {
        val array = JSONArray(file(context).openRead().bufferedReader().use { it.readText() })
        List(array.length()) { i ->
            val o = array.getJSONObject(i)
            BRecord(o.getString("id"), o.getString("questionId"), o.getString("module"), o.optString("answer"),
                if (o.isNull("confidence")) null else o.optDouble("confidence"),
                if (o.isNull("humanScore")) null else o.optInt("humanScore"), o.optLong("timestamp"), o.optString("model"))
        }
    }.getOrDefault(emptyList())
    fun save(context: Context, records: List<BRecord>) {
        val array = JSONArray()
        records.forEach { r -> array.put(JSONObject().put("id", r.id).put("questionId", r.questionId)
            .put("module", r.module).put("answer", r.answer).put("confidence", r.confidence ?: JSONObject.NULL)
            .put("humanScore", r.humanScore ?: JSONObject.NULL).put("timestamp", r.timestamp).put("model", r.model)) }
        val atomic = file(context)
        val stream = atomic.startWrite()
        try { stream.write(array.toString().toByteArray(Charsets.UTF_8)); atomic.finishWrite(stream) }
        catch (e: Exception) { atomic.failWrite(stream); throw e }
    }
}

private object BMetrics {
    fun brier(rows: List<BRecord>): Double? {
        val valid = rows.filter { it.confidence != null && it.humanScore != null }
        return valid.takeIf { it.isNotEmpty() }?.map {
            val outcome = if (it.humanScore!! >= 3) 1.0 else 0.0
            (it.confidence!!.coerceIn(0.0, 1.0) - outcome).let { p -> p * p }
        }?.average()
    }
    fun falseConfidence(rows: List<BRecord>): Double? {
        val wrong = rows.filter { it.confidence != null && it.humanScore != null && it.humanScore < 3 }
        return wrong.takeIf { it.isNotEmpty() }?.map { it.confidence!!.coerceIn(0.0, 1.0) }?.average()
    }
    fun ece(rows: List<BRecord>, bins: Int = 5): Double? {
        val valid = rows.filter { it.confidence != null && it.humanScore != null }
        if (valid.isEmpty()) return null
        return (0 until bins).sumOf { b ->
            val lo = b.toDouble() / bins; val hi = (b + 1).toDouble() / bins
            val group = valid.filter { val p = it.confidence!!.coerceIn(0.0, 1.0); p >= lo && (p < hi || (b == bins - 1 && p <= hi)) }
            if (group.isEmpty()) 0.0 else group.size.toDouble() / valid.size *
                abs(group.count { it.humanScore!! >= 3 }.toDouble() / group.size - group.map { it.confidence!! }.average())
        }
    }
}

@Composable
internal fun BenchmarkScreen(backendUrl: String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var module by remember { mutableStateOf(BQuestionBank.moduleNames.first()) }
    var index by remember { mutableIntStateOf(0) }
    var records by remember { mutableStateOf(BStore.load(context)) }
    var latestId by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("120 प्रश्न तैयार हैं। परिणाम फोन के निजी ऐप स्टोरेज में सुरक्षित होंगे।") }
    val questions = remember(module) { BQuestionBank.all.filter { it.module == module } }
    val question = questions[index]
    val latest = records.firstOrNull { it.id == latestId }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("ASTRA INTELLIGENCE BENCHMARK", fontSize = 17.sp, fontWeight = FontWeight.Black)
        Text("6 मॉड्यूल · 120 प्रश्न · Question version 1", fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
        BQuestionBank.moduleNames.forEach { name ->
            FilterChip(selected = module == name, onClick = { module = name; index = 0; latestId = null },
                label = { Text(name + " · 20") })
        }
        HorizontalDivider()
        Text("प्रश्न " + (index + 1) + "/20 · " + question.id, fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
        Text(question.prompt, fontSize = 15.sp, lineHeight = 22.sp)
        Text("Rubric: " + question.rubric, fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
        Button(enabled = !busy && backendUrl.isNotBlank(), modifier = Modifier.fillMaxWidth(), onClick = {
            busy = true
            status = "Groq से उत्तर और confidence लिया जा रहा है…"
            scope.launch {
                val prompt = "ASTRA benchmark. उत्तर दें और केवल JSON लौटाएँ: {\"answer\":\"उत्तर\",\"confidence\":0.0,\"uncertainty\":\"अनिश्चितता\"}. confidence 0 से 1 के बीच इस rubric के अनुसार सही/पर्याप्त होने की आपकी subjective संभावना है। प्रश्न: " +
                    question.prompt + " Rubric: " + question.rubric
                val raw = withContext(Dispatchers.IO) { askBackend(backendUrl, prompt) }
                val parsed = runCatching {
                    val a = raw.indexOf('{'); val b = raw.lastIndexOf('}')
                    JSONObject(raw.substring(a, b + 1))
                }.getOrNull()
                val answer = parsed?.optString("answer").orEmpty().ifBlank { raw }
                val confidence = if (parsed != null && parsed.has("confidence")) parsed.optDouble("confidence").takeIf { it.isFinite() }?.coerceIn(0.0, 1.0) else null
                val record = BRecord(UUID.randomUUID().toString(), question.id, module, answer, confidence, null, System.currentTimeMillis())
                records = records + record
                latestId = record.id
                runCatching { BStore.save(context, records) }
                status = if (confidence == null) "उत्तर सेव हुआ; confidence parse नहीं हुआ।" else "उत्तर सेव हुआ। अब मानव समीक्षा में 0–4 अंक दें।"
                busy = false
            }
        }) { Text(if (busy) "उत्तर तैयार हो रहा है…" else "इस प्रश्न का परीक्षण चलाएँ") }
        if (latest != null && latest.questionId == question.id) {
            Card(shape = RoundedCornerShape(14.dp)) {
                Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("ASTRA का उत्तर", fontWeight = FontWeight.Bold)
                    Text(latest.answer, fontSize = 13.sp, lineHeight = 19.sp)
                    Text("Self-confidence: " + (latest.confidence?.let { (it * 100).toInt().toString() + "%" } ?: "उपलब्ध नहीं"), fontSize = 12.sp)
                    Text("मानवीय समीक्षा: 0 = गलत/अप्रासंगिक, 4 = उत्कृष्ट", fontSize = 11.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        (0..4).forEach { score ->
                            OutlinedButton(onClick = {
                                val changed = records.map { if (it.id == latest.id) it.copy(humanScore = score) else it }
                                records = changed; latestId = latest.id
                                runCatching { BStore.save(context, changed) }
                            }, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)) { Text(score.toString()) }
                        }
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            OutlinedButton(enabled = index > 0, onClick = { index--; latestId = null }) { Text("पिछला") }
            OutlinedButton(enabled = index < questions.lastIndex, onClick = { index++; latestId = null }) { Text("अगला प्रश्न") }
        }
        Text(status, fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
        HorizontalDivider()
        val moduleRows = records.filter { it.module == module }
        Text("रिकॉर्ड: " + moduleRows.size + "/20 · Human reviewed: " + moduleRows.count { it.humanScore != null }, fontWeight = FontWeight.Bold)
        Text("Brier Score: " + (BMetrics.brier(moduleRows)?.let { "%.3f".format(it) } ?: "अभी उपलब्ध नहीं"))
        Text("False Confidence: " + (BMetrics.falseConfidence(moduleRows)?.let { "%.3f".format(it) } ?: "अभी उपलब्ध नहीं"))
        Text("ECE (5 bins): " + (BMetrics.ece(moduleRows)?.let { "%.3f".format(it) } ?: "अभी उपलब्ध नहीं"))
        Text("0–4 में 3 या 4 को फिलहाल 'सही/पर्याप्त' माना जाता है। ECE छोटे नमूने में अस्थिर हो सकता है।", fontSize = 10.sp, color = MaterialTheme.colorScheme.secondary)
    }
}

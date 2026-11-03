package com.cannatrace.presentation.batch

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cannatrace.data.local.database.entities.*
import com.cannatrace.domain.model.*
import com.cannatrace.presentation.scanner.QrScannerScreen
import com.cannatrace.utils.QrCodeGenerator
import java.text.DateFormat
import java.util.Date
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TraceBetaScreen(vm: TraceViewModel = hiltViewModel()) {
    val role by vm.store.role.collectAsState()
    val batches by vm.store.batches.collectAsState(initial = emptyList())
    val metadata by vm.store.metadata.collectAsState(initial = emptyList())
    val events by vm.store.events.collectAsState(initial = emptyList())
    val busy by vm.busy.collectAsState()
    val message by vm.message.collectAsState()
    var route by rememberSaveable { mutableStateOf("home") }
    var selected by rememberSaveable { mutableStateOf("") }
    var query by rememberSaveable { mutableStateOf("") }
    var showArchived by rememberSaveable { mutableStateOf(false) }
    val contentScroll = rememberScrollState()
    val keyboard = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    LaunchedEffect(route, selected) { contentScroll.scrollTo(0); keyboard?.hide() }
    BackHandler(enabled = route != "home") { route = "home" }

    if (message != null) AlertDialog(onDismissRequest = vm::dismiss, confirmButton = {
        TextButton(onClick = vm::dismiss) { Text("Fermer") }
    }, title = { Text("CannaTrace") }, text = { Text(message.orEmpty()) })

    if (role == null) {
        Column(Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("CannaTrace V1 bêta", style = MaterialTheme.typography.headlineMedium)
            Text("Démonstration locale — données fictives uniquement. Aucune synchronisation. Les profils ci-dessous simulent les permissions ; ils ne sont pas des comptes authentifiés.")
            DemoRole.entries.forEach { r -> Button(onClick = { vm.store.enterDemo(r); route = "home" }, modifier = Modifier.fillMaxWidth()) { Text("Démonstration ${r.name.lowercase()}") } }
            Text("Base chiffrée sur cet appareil. Aucun statut de conformité réglementaire démontré.")
        }
        return
    }
    if (route == "scan") {
        QrScannerScreen(onScanResult = { selected = it; route = "detail" }, onNavigateBack = { route = "home" })
        return
    }
    Scaffold(topBar = {
        TopAppBar(title = { Text("CannaTrace · ${role!!.name.lowercase()}") }, navigationIcon = {
            if (route != "home") TextButton(onClick = { route = "home" }) { Text("Retour") }
        }, actions = { TextButton(onClick = { vm.store.logout(); route = "home" }, enabled = !busy) { Text("Quitter") } })
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(contentScroll).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("BÊTA LOCALE · FICTIF · HORS LIGNE", style = MaterialTheme.typography.labelMedium)
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            when (route) {
                "create" -> CreateTraceForm(vm, busy, onCreated = { selected = it; route = "detail" })
                "detail" -> {
                    val batch = batches.find { it.id == selected }
                    val meta = metadata.find { it.batchId == selected }
                    if (batch == null || meta == null) Text("Lot inconnu ou chargement en cours.")
                    else TraceDetail(batch, meta, events.filter { it.batchId == selected }, vm, busy)
                }
                "history" -> {
                    Text("Journal des opérations", style = MaterialTheme.typography.titleLarge)
                    if (events.isEmpty()) Text("Aucune opération enregistrée.")
                    events.forEach { TraceEventCard(it) }
                }
                else -> {
                    Text("${batches.count { !it.isClosed }} lots actifs · ${batches.count { it.isClosed }} archivés", style = MaterialTheme.typography.titleLarge)
                    val empty = batches.count { !it.isClosed && it.weight == 0.0 }
                    if (empty > 0) Text("Alerte : $empty lot(s) actif(s) sans stock")
                    if (role != DemoRole.LECTEUR) Button(onClick = { route = "create" }, enabled = !busy) { Text("Créer un lot fictif") }
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { route = "scan" }) { Text("Scanner QR") }
                        OutlinedButton(onClick = { route = "history" }) { Text("Historique") }
                    }
                    OutlinedButton(onClick = { vm.submit { check(vm.store.verifyChain()) { "Chaîne invalide" }; vm.inform("Chaîne locale vérifiée. Cette vérification ne prouve pas une authenticité externe.") } }, enabled = !busy) { Text("Vérifier le journal") }
                    OutlinedTextField(query, { query = it }, label = { Text("Rechercher : identifiant ou désignation") }, modifier = Modifier.fillMaxWidth())
                    Row { Checkbox(showArchived, { showArchived = it }); Text("Afficher les lots archivés", modifier = Modifier.padding(top = 12.dp)) }
                    val visible = batches.filter { (showArchived || !it.isClosed) && (it.id.contains(query, true) || it.batchNumber.contains(query, true) || it.strainName.contains(query, true)) }
                    if (visible.isEmpty()) Text("Aucun lot. Créez votre premier lot fictif.")
                    visible.forEach { b ->
                        val m = metadata.find { it.batchId == b.id }
                        OutlinedCard(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp)) {
                                Text(b.strainName, style = MaterialTheme.typography.titleMedium)
                                Text("${b.weight} ${m?.unit.orEmpty()} · ${b.locationId} · ${b.status}")
                                Text(b.id, style = MaterialTheme.typography.bodySmall)
                                TextButton(onClick = { selected = b.id; route = "detail" }) { Text("Ouvrir la fiche") }
                            }
                        }
                    }
                    Text("Dernières opérations", style = MaterialTheme.typography.titleMedium)
                    events.take(5).forEach { TraceEventCard(it) }
                }
            }
        }
    }
}


@Composable
private fun CreateTraceForm(vm: TraceViewModel, busy: Boolean, onCreated: (String) -> Unit) {
    val id = rememberSaveable { UUID.randomUUID().toString() }
    val op = rememberSaveable { UUID.randomUUID().toString() }
    var name by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var origin by rememberSaveable { mutableStateOf("") }
    var supplier by rememberSaveable { mutableStateOf("") }
    var amount by rememberSaveable { mutableStateOf("0") }
    var unit by rememberSaveable { mutableStateOf("g") }
    var location by rememberSaveable { mutableStateOf("") }
    Text("Créer un lot", style = MaterialTheme.typography.titleLarge)
    TraceField("Désignation", name, { name = it })
    TraceField("Catégorie", category, { category = it })
    TraceField("Description", description, { description = it })
    TraceField("Origine (facultatif)", origin, { origin = it })
    TraceField("Fournisseur (facultatif)", supplier, { supplier = it })
    TraceField("Quantité initiale", amount, { amount = it }, numeric = true)
    Row { TraceRules.units.forEach { u -> TextButton(onClick = { unit = u }) { Text(if (u == unit) "[$u]" else u) } } }
    TraceField("Emplacement", location, { location = it })
    Text("Identifiant définitif : $id", style = MaterialTheme.typography.bodySmall)
    Button(onClick = { vm.submit(success = { onCreated(id) }) { vm.store.create(op, id, name, category, description, origin, supplier, amount, unit, location) } }, enabled = !busy) { Text("Enregistrer le lot") }
}

@Composable
private fun TraceDetail(batch: BatchEntity, meta: TraceMetadata, events: List<TraceEvent>, vm: TraceViewModel, busy: Boolean) {
    val role by vm.store.role.collectAsState()
    var action by rememberSaveable(batch.id) { mutableStateOf("ENTREE") }
    var value by rememberSaveable(batch.id) { mutableStateOf("") }
    var reason by rememberSaveable(batch.id) { mutableStateOf("") }
    var op by rememberSaveable(batch.id) { mutableStateOf(UUID.randomUUID().toString()) }
    val qr = remember(batch.id) { QrCodeGenerator.generate(TraceRules.qr(batch.id)) }
    Text(batch.strainName, style = MaterialTheme.typography.headlineSmall)
    Text("${batch.weight} ${meta.unit} · ${batch.status} · ${batch.locationId}")
    Text("Catégorie : ${meta.category}\n${meta.description}\nOrigine : ${meta.origin}\nFournisseur : ${meta.supplier}")
    Text("Créé : ${date(batch.createdAt)}\nID : ${batch.id}\nRévision : ${meta.revision}", style = MaterialTheme.typography.bodySmall)
    qr?.let { Image(it.asImageBitmap(), "QR du lot", modifier = Modifier.fillMaxWidth().height(240.dp)) }
    Text(TraceRules.qr(batch.id), style = MaterialTheme.typography.bodySmall)
    Text("Le QR identifie ce lot local ; il ne prouve pas son authenticité.")
    if (!batch.isClosed && role != DemoRole.LECTEUR) {
        Text("Enregistrer une opération", style = MaterialTheme.typography.titleMedium)
        Row(Modifier.horizontalScroll(rememberScrollState())) {
            (listOf("ENTREE", "SORTIE", "TRANSFERT", "CORRECTION", "STATUT", "QUALITE", "DESIGNATION", "DESCRIPTION", "CATEGORIE", "ORIGINE", "FOURNISSEUR") + if (role == DemoRole.ADMIN) listOf("ARCHIVE") else emptyList()).forEach { a ->
                TextButton(onClick = { action = a; value = ""; op = UUID.randomUUID().toString() }, enabled = !busy) { Text(if (action == a) "[$a]" else a) }
            }
        }
        if (action == "STATUT") {
            Text("Statuts internes de démonstration, sans validation réglementaire.")
            Row(Modifier.horizontalScroll(rememberScrollState())) {
                BatchStatus.entries.filter { it !in listOf(BatchStatus.DISPENSATION, BatchStatus.DETRUIT, BatchStatus.CLOTURE) }.forEach { s ->
                    TextButton(onClick = { value = s.name }) { Text(if (value == s.name) "[${s.displayName}]" else s.displayName) }
                }
            }
        } else if (action != "ARCHIVE") TraceField(when(action) {
            "TRANSFERT" -> "Nouvel emplacement (lot entier)"
            "CORRECTION" -> "Nouvelle quantité totale (${meta.unit})"
            "ENTREE", "SORTIE" -> "Quantité (${meta.unit})"
            "QUALITE" -> "Résultat du contrôle"
            else -> "Nouvelle valeur"
        }, value, { value = it }, numeric = action in listOf("ENTREE", "SORTIE", "CORRECTION"))
        if (action == "ARCHIVE") Text("Archivage définitif. Les données et l'historique sont conservés.")
        TraceField("Motif (5 caractères minimum)", reason, { reason = it })
        Button(onClick = { vm.submit(success = { value = ""; reason = ""; op = UUID.randomUUID().toString() }) { vm.store.change(op, batch.id, meta.revision, action, value, reason) } }, enabled = !busy) { Text("Valider $action") }
    } else Text(if (batch.isClosed) "Lot archivé : lecture seule" else "Profil lecteur : lecture seule")
    Text("Historique (${events.size})", style = MaterialTheme.typography.titleLarge)
    events.forEach { TraceEventCard(it) }
}

@Composable
private fun TraceField(label: String, value: String, change: (String) -> Unit, numeric: Boolean = false) {
    OutlinedTextField(value, { if (it.length <= 2000) change(it) }, label = { Text(label) }, modifier = Modifier.fillMaxWidth(),
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = if (numeric) androidx.compose.ui.text.input.KeyboardType.Decimal else androidx.compose.ui.text.input.KeyboardType.Text))
}

@Composable
private fun TraceEventCard(event: TraceEvent) {
    var expanded by rememberSaveable(event.operationId) { mutableStateOf(false) }
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("${event.action} · ${date(event.timestamp)}", style = MaterialTheme.typography.titleSmall)
            Text("${event.actor}\n${event.reason}")
            Text("Lot : ${event.batchId}\nRéférence : ${event.operationId}", style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = { expanded = !expanded }) { Text(if (expanded) "Réduire" else "Anciennes / nouvelles valeurs") }
            if (expanded) Text("Avant : ${event.beforeValue}\nAprès : ${event.afterValue}\nSHA-256 : ${event.hash}", style = MaterialTheme.typography.bodySmall)
        }
    }
}

private fun date(time: Long): String = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM).format(Date(time))

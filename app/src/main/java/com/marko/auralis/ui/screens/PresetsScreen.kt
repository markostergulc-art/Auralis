package com.marko.auralis.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.marko.auralis.data.LanguageMode
import com.marko.auralis.model.AudioConfig
import com.marko.auralis.model.BuiltInPresetDefinition
import com.marko.auralis.model.Preset

/** v1.9 preset-first UX with always-visible wrapping category chips. */
@Composable
fun PresetsScreen(
    config: AudioConfig,
    presets: List<Preset>,
    builtIns: List<BuiltInPresetDefinition>,
    language: LanguageMode,
    initialCategory: String,
    onCategoryChange: (String) -> Unit,
    onLoadBuiltIn: (BuiltInPresetDefinition) -> Unit,
    onCopyBuiltIn: (BuiltInPresetDefinition) -> Unit,
    onBack: () -> Unit,
    onSave: (String) -> Unit,
    onLoad: (Preset) -> Unit,
    onRename: (Long, String) -> Unit,
    onDuplicate: (Long) -> Unit,
    onUpdate: (Long) -> Unit,
    onDelete: (Long) -> Unit
) {
    var category by remember(initialCategory) { mutableStateOf(initialCategory) }
    var search by remember { mutableStateOf("") }
    var detail by remember { mutableStateOf<BuiltInPresetDefinition?>(null) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var renamePreset by remember { mutableStateOf<Preset?>(null) }
    var deletePreset by remember { mutableStateOf<Preset?>(null) }

    fun label(p: BuiltInPresetDefinition): String = when(language){LanguageMode.GERMAN->p.nameDe;LanguageMode.CROATIAN->p.nameHr;else->p.nameEn}
    fun sub(p: BuiltInPresetDefinition): String = when(language){LanguageMode.GERMAN->p.subtitleDe;LanguageMode.CROATIAN->p.subtitleHr;else->p.subtitleEn}
    val filtered=builtIns.filter { p ->
        (category=="all" || p.category==category) && (search.isBlank() || (label(p)+" "+sub(p)).contains(search,ignoreCase=true))
    }
    val cats=listOf("all" to "All","focus" to "Focus","relax" to "Relax","breathing" to "Breathe","sleep" to "Sleep","spatial" to "Spatial","beats" to "Beats","experimental" to "Experimental")

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth()) { TextButton(onClick=onBack){Text("‹ Back")}; Text("Presets",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold) }
        Text("Choose by goal. Technical parameters remain available in Details.",color=MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(value=search,onValueChange={search=it},label={Text("Search presets")},singleLine=true,modifier=Modifier.fillMaxWidth())

        // Two rows avoid the hidden horizontal-scroll problem and keep all 8 groups visible.
        cats.chunked(4).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                row.forEach { (id,title) ->
                    FilterChip(
                        selected=category==id,
                        onClick={ category=id; onCategoryChange(id) },
                        label={Text(if(category==id) "✓ $title" else title)},
                        modifier=Modifier.weight(1f).height(48.dp)
                    )
                }
            }
        }
        Text("${filtered.size} presets",style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.onSurfaceVariant)

        filtered.forEach { p ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(label(p),style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold)
                    Text(sub(p),color=MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${p.evidence} • ${p.suggestedMinutes} min • ${p.headphones}",color=MaterialTheme.colorScheme.primary)
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        Button(onClick={onLoadBuiltIn(p)},modifier=Modifier.weight(1.3f).height(48.dp)){Text("Start")}
                        OutlinedButton(onClick={detail=p},modifier=Modifier.weight(1f).height(48.dp)){Text("Details")}
                    }
                }
            }
        }

        Text("My Presets",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)
        Button(onClick={showSaveDialog=true},modifier=Modifier.fillMaxWidth().height(48.dp)){Text("Save current as My Preset")}
        if(presets.isEmpty()) Text("No personal presets yet.",color=MaterialTheme.colorScheme.onSurfaceVariant)
        presets.forEach { preset ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                    Text(preset.name,fontWeight=FontWeight.Bold)
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        Button(onClick={onLoad(preset)},modifier=Modifier.weight(1f)){Text("Load")}
                        OutlinedButton(onClick={renamePreset=preset},modifier=Modifier.weight(1f)){Text("Rename")}
                    }
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                        TextButton(onClick={onUpdate(preset.id)},modifier=Modifier.weight(1f)){Text("Update")}
                        TextButton(onClick={onDuplicate(preset.id)},modifier=Modifier.weight(1f)){Text("Duplicate")}
                        TextButton(onClick={deletePreset=preset},modifier=Modifier.weight(1f)){Text("Delete")}
                    }
                }
            }
        }
    }

    detail?.let { p ->
        AlertDialog(
            onDismissRequest={detail=null},
            title={Text(label(p))},
            text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)){Text(sub(p));Text("Evidence: ${p.evidence}");Text("Suggested session: ${p.suggestedMinutes} min");Text("Headphones: ${p.headphones}");Text("Technical parameters remain unchanged from the built-in definition.")}},
            confirmButton={TextButton(onClick={detail=null;onLoadBuiltIn(p)}){Text("Start")}},
            dismissButton={TextButton(onClick={onCopyBuiltIn(p);detail=null}){Text("Save as My Preset")}}
        )
    }
    if(showSaveDialog) NameDialog("Save Preset","","Save",{showSaveDialog=false}){onSave(it);showSaveDialog=false}
    renamePreset?.let { p -> NameDialog("Rename Preset",p.name,"Rename",{renamePreset=null}){onRename(p.id,it);renamePreset=null} }
    deletePreset?.let { p -> AlertDialog(onDismissRequest={deletePreset=null},title={Text("Delete preset?")},text={Text("Delete \"${p.name}\"?")},confirmButton={TextButton(onClick={onDelete(p.id);deletePreset=null}){Text("Delete")}},dismissButton={TextButton(onClick={deletePreset=null}){Text("Cancel")}}) }
}

@Composable private fun NameDialog(title:String,initial:String,confirm:String,onDismiss:()->Unit,onApply:(String)->Unit){
    var name by remember(initial){mutableStateOf(initial)}
    AlertDialog(onDismissRequest=onDismiss,title={Text(title)},text={OutlinedTextField(value=name,onValueChange={if(it.length<=40)name=it},label={Text("Preset name")},singleLine=true)},confirmButton={TextButton(onClick={onApply(name.trim())},enabled=name.isNotBlank()){Text(confirm)}},dismissButton={TextButton(onClick=onDismiss){Text("Cancel")}})
}

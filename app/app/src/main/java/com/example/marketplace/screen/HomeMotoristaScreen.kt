package com.example.marketplace.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.marketplace.controller.UsuarioViewModel
import com.example.marketplace.controller.UsuarioViewModelFactory
import com.example.marketplace.controller.VendaListViewModel
import com.example.marketplace.controller.VendaListViewModelFactory
import com.example.marketplace.model.enums.StatusEntrega
import com.example.marketplace.model.Usuario
import com.example.marketplace.model.Veiculo
import com.example.marketplace.model.Venda

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeMotoristaScreen(
    usuario: Usuario,
    veiculos: List<Veiculo>,
    onCadastrarVeiculo: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val usuarioViewModel: UsuarioViewModel = viewModel(
        factory = UsuarioViewModelFactory(context)
    )
    val vendaViewModel: VendaListViewModel = viewModel(
        factory = VendaListViewModelFactory(context)
    )

    val negociantes by usuarioViewModel.negociantes.collectAsState()
    val vinculando by usuarioViewModel.vinculando.collectAsState()
    val vendas by vendaViewModel.vendas.collectAsState()

    var negocianteIdAtual by remember { mutableStateOf(usuario.negocianteId) }

    LaunchedEffect(Unit) {
        usuarioViewModel.carregarNegociantes()
    }

    // Filtra entregas relevantes para o motorista:
    // - Pedidos do negociante vinculado em PRONTO_PARA_ENTREGA
    // - Pedidos atribuídos a este motorista em A_CAMINHO ou ENTREGUE
    val entregasMotorista = vendas.filter { venda ->
        val status = venda.statusEntrega
        val ehDoNegociante = negocianteIdAtual != null && venda.vendedorId == negocianteIdAtual
        val ehDesteMotorista = venda.motoristaId == usuario.uid

        (ehDoNegociante && status == StatusEntrega.PRONTO_PARA_ENTREGA) ||
                (ehDesteMotorista && (status == StatusEntrega.A_CAMINHO || status == StatusEntrega.ENTREGUE)) ||
                (ehDoNegociante && (status == StatusEntrega.A_CAMINHO || status == StatusEntrega.ENTREGUE))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Área do Motorista") },
                actions = {
                    TextButton(onClick = onLogout) { Text("Sair") }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCadastrarVeiculo) {
                Text("+")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text("Olá, ${usuario.nome}!", style = MaterialTheme.typography.headlineSmall)
            }

            // ------------------------------------------------
            // VÍNCULO COM NEGOCIANTE
            // ------------------------------------------------
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Negociante parceiro", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))

                        if (negocianteIdAtual == null) {
                            Text("Você ainda não está vinculado a nenhum negociante.")
                            Spacer(Modifier.height(8.dp))

                            if (negociantes.isEmpty()) {
                                Text(
                                    "Nenhum negociante disponível no momento.",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            } else {
                                negociantes.forEach { negociante ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(negociante.nome)
                                        Button(
                                            enabled = !vinculando,
                                            onClick = {
                                                usuarioViewModel.vincularNegociante(
                                                    motoristaUid = usuario.uid,
                                                    negocianteId = negociante.uid
                                                ) {
                                                    negocianteIdAtual = negociante.uid
                                                }
                                            }
                                        ) {
                                            Text("Vincular")
                                        }
                                    }
                                }
                            }
                        } else {
                            val nomeNegociante = negociantes
                                .find { it.uid == negocianteIdAtual }
                                ?.nome ?: negocianteIdAtual

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Vinculado a: $nomeNegociante")
                                OutlinedButton(
                                    enabled = !vinculando,
                                    onClick = {
                                        usuarioViewModel.desvincularNegociante(
                                            motoristaUid = usuario.uid
                                        ) {
                                            negocianteIdAtual = null
                                        }
                                    }
                                ) {
                                    Text("Desvincular")
                                }
                            }
                        }
                    }
                }
            }

            // ------------------------------------------------
            // MEUS VEÍCULOS
            // ------------------------------------------------
            item {
                Text("Meus veículos", style = MaterialTheme.typography.titleMedium)
            }

            if (veiculos.isEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Nenhum veículo cadastrado ainda (toque no + para cadastrar)")
                        }
                    }
                }
            } else {
                items(veiculos, key = { it.id }) { veiculo ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(veiculo.modelo, style = MaterialTheme.typography.titleSmall)
                            Text("Placa: ${veiculo.placa} • Ano: ${veiculo.ano}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            // ------------------------------------------------
            // ENTREGAS / PEDIDOS
            // ------------------------------------------------
            item {
                Spacer(Modifier.height(8.dp))
                Text("Entregas de Pedidos", style = MaterialTheme.typography.titleMedium)
            }

            if (negocianteIdAtual == null) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Box(Modifier.padding(16.dp)) {
                            Text("Vincule-se a um negociante acima para visualizar pedidos para entrega.")
                        }
                    }
                }
            } else if (entregasMotorista.isEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Box(Modifier.padding(16.dp), contentAlignment = Alignment.Center) {
                            Text("Nenhuma entrega disponível no momento.")
                        }
                    }
                }
            } else {
                items(entregasMotorista, key = { it.id }) { venda ->
                    MotoristaEntregaCard(
                        venda = venda,
                        usuario = usuario,
                        veiculos = veiculos,
                        onAtualizarStatus = { novoStatus, veiculoId ->
                            vendaViewModel.avancarStatus(
                                vendaId = venda.id,
                                novoStatus = novoStatus,
                                perfil = "motorista",
                                motoristaId = usuario.uid,
                                veiculoId = veiculoId
                            )
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MotoristaEntregaCard(
    venda: Venda,
    usuario: Usuario,
    veiculos: List<Veiculo>,
    onAtualizarStatus: (String, String?) -> Unit
) {
    val status = venda.statusEntrega
    var veiculoSelecionado by remember(venda.id) { mutableStateOf<Veiculo?>(null) }
    var menuVeiculoAberto by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Pedido #${venda.id.take(8)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                StatusEntregaBadge(status = status)
            }

            Spacer(Modifier.height(6.dp))
            Text("Produto ID: ${venda.produtoId}", style = MaterialTheme.typography.bodyMedium)
            Text("Quantidade: ${venda.quantidade}", style = MaterialTheme.typography.bodySmall)
            Text("Valor Total: R$ %.2f".format(venda.valorTotal), style = MaterialTheme.typography.bodyMedium)

            Spacer(Modifier.height(12.dp))

            when (status) {
StatusEntrega.PENDENTE -> {
Text(
"Pedido pendente. Aguardando liberação para entrega.",
style = MaterialTheme.typography.bodySmall
)
}

StatusEntrega.PRONTO_PARA_ENTREGA -> {
Column(
verticalArrangement = Arrangement.spacedBy(8.dp)
) {
if (veiculos.isEmpty()) {
Text(
"Cadastre um veículo para poder iniciar a entrega.",
style = MaterialTheme.typography.bodySmall,
color = MaterialTheme.colorScheme.error
)
} else {
ExposedDropdownMenuBox(
expanded = menuVeiculoAberto,
onExpandedChange = { menuVeiculoAberto = it }
) {
OutlinedTextField(
value = veiculoSelecionado?.let { "${it.modelo} - ${it.placa}" } ?: "",
onValueChange = {},
readOnly = true,
label = { Text("Veículo para a entrega") },
trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = menuVeiculoAberto) },
modifier = Modifier
.fillMaxWidth()
.menuAnchor()
)
ExposedDropdownMenu(
expanded = menuVeiculoAberto,
onDismissRequest = { menuVeiculoAberto = false }
) {
veiculos.forEach { veiculo ->
DropdownMenuItem(
text = { Text("${veiculo.modelo} - ${veiculo.placa}") },
onClick = {
veiculoSelecionado = veiculo
menuVeiculoAberto = false
}
)
}
}
}
}

Button(
onClick = {
onAtualizarStatus(StatusEntrega.A_CAMINHO.name, veiculoSelecionado?.id)
},
enabled = veiculoSelecionado != null,
modifier = Modifier.fillMaxWidth()
) {
Text("Coletar e Iniciar Entrega (A caminho)")
}
}
}

StatusEntrega.A_CAMINHO -> {
Column(
verticalArrangement = Arrangement.spacedBy(6.dp)
) {
val veiculoDaEntrega = veiculos.find { it.id == venda.veiculoId }
if (veiculoDaEntrega != null) {
Text(
"Veículo: ${veiculoDaEntrega.modelo} - ${veiculoDaEntrega.placa}",
style = MaterialTheme.typography.bodySmall
)
}

Button(
onClick = {
onAtualizarStatus(StatusEntrega.ENTREGUE.name, venda.veiculoId)
},
modifier = Modifier.fillMaxWidth()
) {
Text("Confirmar Entrega ao Cliente")
}

OutlinedButton(
onClick = {
onAtualizarStatus(StatusEntrega.PRONTO_PARA_ENTREGA.name, venda.veiculoId)
},
modifier = Modifier.fillMaxWidth()
) {
Text("Cancelar Coleta / Devolver para Loja")
}
}
}

StatusEntrega.ENTREGUE -> {
Row(
modifier = Modifier.fillMaxWidth(),
horizontalArrangement = Arrangement.SpaceBetween,
verticalAlignment = Alignment.CenterVertically
) {
Text(
"Entrega Concluída ✓",
style = MaterialTheme.typography.bodyMedium,
fontWeight = FontWeight.Bold,
color = MaterialTheme.colorScheme.primary
)

OutlinedButton(
onClick = {
onAtualizarStatus(StatusEntrega.A_CAMINHO.name, venda.veiculoId)
}
) {
Text("Desfazer Entrega")
}
}
}

StatusEntrega.CANCELADA -> {
Text(
"Pedido cancelado. Nenhuma ação de entrega disponível.",
style = MaterialTheme.typography.bodySmall,
color = MaterialTheme.colorScheme.error
)
}
}
}
}
}


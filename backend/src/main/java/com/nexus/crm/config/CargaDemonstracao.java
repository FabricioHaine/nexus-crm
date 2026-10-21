package com.nexus.crm.config;

import com.nexus.crm.model.Cliente;
import com.nexus.crm.model.EtapaOportunidade;
import com.nexus.crm.model.Oportunidade;
import com.nexus.crm.model.Papel;
import com.nexus.crm.model.Usuario;
import com.nexus.crm.repository.ClienteRepository;
import com.nexus.crm.repository.OportunidadeRepository;
import com.nexus.crm.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Carga opcional de dados de DEMONSTRACAO.
 *
 * <p>Desativada por padrao. Para habilitar, use o perfil {@code demo}
 * ({@code --spring.profiles.active=demo}) ou a propriedade {@code app.demo.habilitado=true}.
 * Nao executa nada quando o banco ja possui clientes.</p>
 */
@Component
@ConditionalOnProperty(name = "app.demo.habilitado", havingValue = "true")
public class CargaDemonstracao implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(CargaDemonstracao.class);

    private final ClienteRepository clienteRepository;
    private final OportunidadeRepository oportunidadeRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public CargaDemonstracao(ClienteRepository clienteRepository,
                              OportunidadeRepository oportunidadeRepository,
                              UsuarioRepository usuarioRepository,
                              PasswordEncoder passwordEncoder) {
        this.clienteRepository = clienteRepository;
        this.oportunidadeRepository = oportunidadeRepository;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        carregarUsuarios();

        if (clienteRepository.count() > 0) {
            log.info("Carga de demonstracao ignorada: ja existem clientes cadastrados.");
            return;
        }

        List<Cliente> clientes = new ArrayList<>();
        clientes.add(cliente("Ana Ribeiro", "ana.ribeiro@orbitatelecom.com.br", "(11) 98877-1020",
                "Orbitatelecom", "Gerente de Compras", "Retorno frequente de proposta."));
        clientes.add(cliente("Bruno Carvalho", "bruno.carvalho@vetalum.com", "(21) 97654-8871",
                "Vetalum Distribuidora", "Diretor Comercial", null));
        clientes.add(cliente("Camila Souza", "camila.souza@nutrivida.com.br", "(31) 91234-5567",
                "Nutrivida Alimentos", "Coordenadora de Suprimentos", "decisao em comite."));
        clientes.add(cliente("Diego Martins", "diego.martins@logisticapraia.com", "(85) 99812-3344",
                "Logistica Praia", "Gerente de Operacoes", null));
        clientes.add(cliente("Elaine Torres", "elaine.torres@educare.com", "(41) 98765-4321",
                "Educare Cursos", "Diretora Financeira", "Contratacao anual."));
        clientes.add(cliente("Felipe Andrade", "felipe.andrade@agrovet.com.br", "(62) 98811-2233",
                "Agrovet Saude Animal", "Proprietario", "Conta pequena, decisao rapida."));
        clienteRepository.saveAll(clientes);

        List<Oportunidade> oportunidades = new ArrayList<>();
        oportunidades.add(oportunidade(clientes.get(0), "Renovacao do contrato de licencas",
                "Contrato anual de licencas para 120 usuarios.", 48000.00, EtapaOportunidade.PROPOSTA, 65, 25));
        oportunidades.add(oportunidade(clientes.get(0), "Expansao para unidades de atendimento",
                "Novos espacos de atendimento em 6 unidades.", 96000.00, EtapaOportunidade.QUALIFICACAO, 35, 40));
        oportunidades.add(oportunidade(clientes.get(1), "Implantacao de CRM comercial",
                "Implantacao completa do funil comercial.", 145000.00, EtapaOportunidade.GANHO, 100, 15));
        oportunidades.add(oportunidade(clientes.get(1), "Integracao com ERP",
                "Integracao do CRM com o ERP legado.", 62000.00, EtapaOportunidade.PERDIDO, 0, 30,
                "Projeto adiado por reestruturacao interna."));
        oportunidades.add(oportunidade(clientes.get(2), "Contrato de suporte premium",
                "Suporte premium com SLA de 4 horas.", 38000.00, EtapaOportunidade.NOVO_CONTATO, 10, 60));
        oportunidades.add(oportunidade(clientes.get(3), "Modulo de roteirizacao",
                "Otimizacao de rotas da frota propria.", 210000.00, EtapaOportunidade.PROPOSTA, 65, 20));
        oportunidades.add(oportunidade(clientes.get(3), "Painel de indicadores logisticos",
                "Painel com SLA de entregas.", 27500.00, EtapaOportunidade.PERDIDO, 0, 10,
                "Escolheu solucao interna."));
        oportunidades.add(oportunidade(clientes.get(4), "Assinatura da plataforma educacional",
                "Plataforma para 2500 alunos.", 72000.00, EtapaOportunidade.QUALIFICACAO, 35, 35));
        oportunidades.add(oportunidade(clientes.get(5), "Automacao dePedidos",
                "Pedidos automaticos via portal B2B.", 18500.00, EtapaOportunidade.GANHO, 100, 5));
        oportunidades.add(oportunidade(clientes.get(2), "Treinamento das equipes de compras",
                "Trilha de treinamento para 40 colaboradores.", 12000.00, EtapaOportunidade.NOVO_CONTATO, 10, 45));
        oportunidadeRepository.saveAll(oportunidades);

        log.info("Carga de demonstracao concluida: {} clientes e {} oportunidades.",
                clientes.size(), oportunidades.size());
    }

    /**
     * Cria os usuarios de demonstracao, um para cada perfil de acesso, para que
     * as regras de permissao possam ser exercitadas na interface.
     */
    private void carregarUsuarios() {
        criarUsuarioSeAusente("Gerente Comercial", "gerente@nexuscrm.local", "Vendas@2026", Papel.GERENTE);
        criarUsuarioSeAusente("Vendedor", "vendedor@nexuscrm.local", "Vendas@2026", Papel.VENDEDOR);
    }

    private void criarUsuarioSeAusente(String nome, String email, String senha, Papel papel) {
        if (usuarioRepository.existsByEmailIgnoreCase(email)) {
            return;
        }
        Usuario usuario = new Usuario();
        usuario.setNome(nome);
        usuario.setEmail(email);
        usuario.setSenhaHash(passwordEncoder.encode(senha));
        usuario.setPapel(papel);
        usuario.setAtivo(true);
        usuarioRepository.save(usuario);
        log.info("Usuario de demonstracao criado: {} ({})", email, papel);
    }

    private Cliente cliente(String nome, String email, String telefone, String empresa, String cargo,
                            String observacoes) {
        Cliente cliente = new Cliente();
        cliente.setNome(nome);
        cliente.setEmail(email);
        cliente.setTelefone(telefone);
        cliente.setEmpresa(empresa);
        cliente.setCargo(cargo);
        cliente.setObservacoes(observacoes);
        return cliente;
    }

    private Oportunidade oportunidade(Cliente cliente, String titulo, String descricao, double valor,
                                      EtapaOportunidade etapa, int probabilidade, int diasFechamento) {
        return oportunidade(cliente, titulo, descricao, valor, etapa, probabilidade, diasFechamento, null);
    }

    private Oportunidade oportunidade(Cliente cliente, String titulo, String descricao, double valor,
                                      EtapaOportunidade etapa, int probabilidade, int diasFechamento,
                                      String motivoPerda) {
        Oportunidade oportunidade = new Oportunidade();
        oportunidade.setCliente(cliente);
        oportunidade.setTitulo(titulo);
        oportunidade.setDescricao(descricao);
        oportunidade.setValorEstimado(BigDecimal.valueOf(valor));
        oportunidade.setEtapa(etapa);
        oportunidade.setProbabilidade(probabilidade);
        oportunidade.setDataPrevistaFechamento(LocalDate.now().plusDays(diasFechamento));
        oportunidade.setMotivoPerda(motivoPerda);
        if (etapa == EtapaOportunidade.GANHO || etapa == EtapaOportunidade.PERDIDO) {
            oportunidade.setDataFechamento(LocalDateTime.now().minusDays(Math.max(1, 60 - diasFechamento)));
        }
        return oportunidade;
    }
}

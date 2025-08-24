package br.com.alura.service;

import br.com.alura.domain.Agencia;
import br.com.alura.domain.exceptions.AgenciaNaoAtivaOuNaoEncontradaException;
import br.com.alura.domain.http.AgenciaHttp;
import br.com.alura.domain.http.SituacaoCadastral;
import br.com.alura.repository.AgenciaRepository;
import br.com.alura.service.http.SituacaoCadastralHttpService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.rest.client.inject.RestClient;


@ApplicationScoped
public class AgenciaService {

    @RestClient
    SituacaoCadastralHttpService situacaoCadastralHttpService;

    private final AgenciaRepository agenciaRepository;
    private final MeterRegistry meterRegistry;

    AgenciaService(AgenciaRepository agenciaRepository, MeterRegistry meterRegistry) {
        this.agenciaRepository = agenciaRepository;
        this.meterRegistry = meterRegistry;
    }

    /**
     * Definindo esse método como reativo utilizando o Uni<Void>
     * É necessário manter a transação aberta (Mesmo que tenha a anotação no controller) por ser um método reativo
     * Por isso, foi adicionado o @WithTransaction
     */
    @WithTransaction
    public Uni<Void> cadastrar(Agencia agencia) {
        Timer.Sample sample = Timer.start(meterRegistry);

        return situacaoCadastralHttpService.buscarPorCnpj(agencia.getCnpj())
                .onItem().ifNull().failWith(AgenciaNaoAtivaOuNaoEncontradaException::new)
                .onItem().transformToUni(item -> persistirSeAtiva(agencia, item))
                .onTermination().invoke((ignored, failure, cancelled) -> {
                    sample.stop(meterRegistry.timer("cadastrar_agencia_timer"));
                });
    }

    /**
     * Definindo esse método como reativo utilizando o Uni<Void>
     */
    private Uni<Void> persistirSeAtiva(Agencia agencia, AgenciaHttp agenciaHttp) {
        if (agenciaHttp.getSituacaoCadastral().equals(SituacaoCadastral.ATIVO)) {
            meterRegistry.counter("agencia_adicionada_counter").increment();
            Log.info("A agencia com o CNPJ " + agencia.getCnpj() + " foi cadastrada");
            /* Transformando o retorno em reativo utilizando replaceWithVoid() */
            return agenciaRepository.persist(agencia).replaceWithVoid();
        } else {
            Log.info("A agencia com o CNPJ " + agencia.getCnpj() + " não foi cadastrada");
            meterRegistry.counter("agencia_nao_adicionada_counter").increment();
            return Uni.createFrom().failure(AgenciaNaoAtivaOuNaoEncontradaException::new);
        }
    }

    /**
     * Definindo esse método como reativo utilizando o Uni<Agencia>
     * É necessário manter a sessão aberta por ser um método reativo de leitura de dados
     * Por isso, foi adicionado o @WithSession
     */
    @WithSession
    public Uni<Agencia> buscarPorId(Long id) {
        return agenciaRepository.findById(id);
    }

    @WithTransaction
    public Uni<Void> deletar(Long id) {
        Log.info("A agencia com o id " + id + " foi deletada");
        return agenciaRepository
                .deleteById(id)
                .replaceWithVoid();
    }

    @WithTransaction
    public Uni<Void> alterar(Agencia agencia) {
        Log.info("A agencia com o CNPJ " + agencia.getCnpj() + " foi alterada");
        return agenciaRepository
                .update("nome = ?1, razaoSocial = ?2, cnpj = ?3 where id = ?4", agencia.getNome(), agencia.getRazaoSocial(), agencia.getCnpj(), agencia.getId())
                .replaceWithVoid();
    }
}

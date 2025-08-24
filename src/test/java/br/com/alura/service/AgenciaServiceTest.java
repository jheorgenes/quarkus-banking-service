package br.com.alura.service;

import br.com.alura.domain.Agencia;
import br.com.alura.domain.exceptions.AgenciaNaoAtivaOuNaoEncontradaException;
import br.com.alura.domain.http.AgenciaHttp;
import br.com.alura.repository.AgenciaRepository;
import br.com.alura.service.http.SituacaoCadastralHttpService;
import br.com.alura.utils.AgenciaFixture;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Vertx;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@QuarkusTest
public class AgenciaServiceTest {

    @InjectMock
    private AgenciaRepository agenciaRepository;

    @InjectMock
    @RestClient
    private SituacaoCadastralHttpService situacaoCadastralHttpService;

    @Inject
    private AgenciaService agenciaService;

    @Test
    public void deveNaoCadastrarQuandoClientRetornarNull() {
        // Construíndo estrutura dos dados mockados
        Agencia agencia = AgenciaFixture.criaAgencia();
        Mockito.when(situacaoCadastralHttpService.buscarPorCnpj("123")).thenReturn(Uni.createFrom().nullItem());

        Vertx.vertx().runOnContext(r -> {
            // Verifica se lancou a exception quando executado o cadastro
            Assertions.assertThrows(AgenciaNaoAtivaOuNaoEncontradaException.class, ()  ->
                    // Definindo uma espera pro teste (Após mudança para ser reativo/assíncrono) com .await.indefinitely()
                    agenciaService.cadastrar(agencia).await().indefinitely());

            // Verifica que o repository nunca foi chamado
            Mockito.verify(agenciaRepository, Mockito.never()).persist(agencia);
        });
    }

    @Test
    public void deveNaoCadastrarQuandoClientRetornarSituacaoCadastralInativo() {
        // Construíndo estrutura dos dados mockados
        Agencia agencia = AgenciaFixture.criaAgencia();
        Uni<AgenciaHttp> agenciaHttpInativa = AgenciaFixture.criarAgenciaHttp("INATIVO");
        Mockito.when(situacaoCadastralHttpService.buscarPorCnpj("123")).thenReturn(agenciaHttpInativa);

        Vertx.vertx().runOnContext(r -> {
            Assertions.assertThrows(AgenciaNaoAtivaOuNaoEncontradaException.class, () -> agenciaService.cadastrar(agencia).await().indefinitely());

            Mockito.verify(agenciaRepository, Mockito.never()).persist(agencia);
        });
    }

    @Test
    public void deveCadastrarQuandoClientRetornarSituacaoCadastralAtivo() {
        // Construíndo estrutura dos dados mockados
        Agencia agencia = AgenciaFixture.criaAgencia();
        Mockito.when(situacaoCadastralHttpService.buscarPorCnpj("123")).thenReturn(AgenciaFixture.criarAgenciaHttp("ATIVO"));

        Vertx.vertx().runOnContext(r -> {
            agenciaService.cadastrar(agencia).await().indefinitely();
            Mockito.verify(agenciaRepository).persist(agencia);
        });
    }
}

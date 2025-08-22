package br.com.alura.service;

import br.com.alura.domain.Agencia;
import br.com.alura.domain.Endereco;
import br.com.alura.domain.exceptions.AgenciaNaoAtivaOuNaoEncontradaException;
import br.com.alura.domain.http.AgenciaHttp;
import br.com.alura.repository.AgenciaRepository;
import br.com.alura.service.http.SituacaoCadastralHttpService;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
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
        Agencia agencia = criarAgencia();
        Mockito.when(situacaoCadastralHttpService.buscarPorCnpj("123")).thenReturn(null);

        // Verifica se lancou a exception quando executado o cadastro
        Assertions.assertThrows(AgenciaNaoAtivaOuNaoEncontradaException.class, ()  -> agenciaService.cadastrar(agencia));

        // Verifica que o repository nunca foi chamado
        Mockito.verify(agenciaRepository, Mockito.never()).persist(agencia);
    }

    @Test
    public void deveCadastrarQuandoClientRetornarSituacaoCadastralAtivo() {
        // Construíndo estrutura dos dados mockados
        Agencia agencia = criarAgencia();
        Mockito.when(situacaoCadastralHttpService.buscarPorCnpj("123")).thenReturn(criarAgenciaHttp());

        agenciaService.cadastrar(agencia);
        Mockito.verify(agenciaRepository).persist(agencia);
    }

    private Agencia criarAgencia() {
        Endereco endereco = new Endereco(1, "Quadra", "Teste", "Teste", 1);
        return new Agencia(1, "Agencia Teste", "Razao Agencia teste", "123", endereco);
    }

    private AgenciaHttp criarAgenciaHttp() {
        return new AgenciaHttp("Agencia Teste", "Razao Agencia teste", "123", "ATIVO");
    }
}

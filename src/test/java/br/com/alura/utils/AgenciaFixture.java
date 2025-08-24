package br.com.alura.utils;

import br.com.alura.domain.Agencia;
import br.com.alura.domain.Endereco;
import br.com.alura.domain.http.AgenciaHttp;
import io.smallrye.mutiny.Uni;

public class AgenciaFixture {

    public static Uni<AgenciaHttp> criarAgenciaHttp(String status) {
        return Uni.createFrom().item(new AgenciaHttp("Agencia Teste", "Razao Agencia teste", "123", status));
    }

    public static Agencia criaAgencia() {
        Endereco endereco = new Endereco(1, "Quadra", "Teste", "Teste", 1);
        return new Agencia(1L, "Agencia Teste", "Razao Agencia teste", "123", endereco);
    }
}

package br.com.alura.service.messaging;

import br.com.alura.domain.messaging.AgenciaMessage;
import br.com.alura.repository.AgenciaRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.reactive.messaging.Incoming;

@ApplicationScoped
public class RemoverAgenciaConsumer {

    private final ObjectMapper objectMapper;
    private final AgenciaRepository agenciaRepository;

    public RemoverAgenciaConsumer(ObjectMapper objectMapper, AgenciaRepository agenciaRepository) {
        this.objectMapper = objectMapper;
        this.agenciaRepository = agenciaRepository;
    }

    @WithTransaction //Mantém a trasação aberta
    @Incoming("banking-service-channel") //Recebe as mensagens do Kafka
    public Uni<Void> consumirMensagem(String mensagem) {
        // Gera uma Uni<Void> a partir de uma String, realizando uma transformação
        return Uni.createFrom().item(() -> {
           try {
               Log.info(mensagem);
               // Transforma a String (JSON) recebida do kafka em um AgenciaMessage
               return objectMapper.readValue(mensagem, AgenciaMessage.class);
           } catch (JsonProcessingException ex) {
               Log.error(ex.getMessage());
               throw new RuntimeException();
           }
        })
        .onItem().transformToUni(agenciaMensagem -> agenciaRepository.findByCnpj(agenciaMensagem.getCnpj()) //transforma a mensagem em uma agencia
        .onItem().ifNotNull().transformToUni(agencia -> agenciaRepository.deleteById(agencia.getId()))) //deleta a agencia se não for nula
        .replaceWithVoid(); //retorna Uni<Void>
    }
}

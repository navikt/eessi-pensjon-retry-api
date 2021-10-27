package no.nav.eessi.pensjon.retry

import org.springframework.kafka.core.KafkaTemplate
import org.springframework.messaging.Message
import org.springframework.messaging.MessageHeaders
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class RetryKafkaHandler(private val aivenKafkaTemplate: KafkaTemplate<String, String>) {


    fun publishRetryHendelsePaaKafka(hendlse: String) {
        aivenKafkaTemplate.send(RetryMessage(RetryMessagePayload("Hei"))).get()
    }
}

data class RetryMessagePayload(
    val data: String
)

class RetryMessage(
    private val payload: RetryMessagePayload
): Message<RetryMessagePayload> {
    override fun getPayload(): RetryMessagePayload = payload
    override fun getHeaders(): MessageHeaders = MessageHeaders(mapOf("hendelsetype" to "RETRY", "opprettet" to LocalDateTime.now()))
}
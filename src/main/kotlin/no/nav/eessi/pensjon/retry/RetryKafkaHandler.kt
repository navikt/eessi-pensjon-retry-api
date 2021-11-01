package no.nav.eessi.pensjon.retry

import org.springframework.kafka.core.KafkaTemplate
import org.springframework.messaging.Message
import org.springframework.messaging.MessageHeaders
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class RetryKafkaHandler(private val aivenKafkaTemplate: KafkaTemplate<HendelseModelRetry, String>) {

    fun publishRetryHendelsePaaKafka(hendelseModelRetry: HendelseModelRetry) {

        aivenKafkaTemplate.send(RetryMessage(hendelseModelRetry)).get()

    }
}

class RetryMessage(
    private val payload: HendelseModelRetry
): Message<HendelseModelRetry> {
    override fun getPayload(): HendelseModelRetry = payload
    override fun getHeaders(): MessageHeaders = MessageHeaders(mapOf( "opprettet" to LocalDateTime.now()))
}
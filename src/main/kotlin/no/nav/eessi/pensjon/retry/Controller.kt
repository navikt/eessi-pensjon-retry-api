package no.nav.eessi.pensjon.retry

import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class Controller(private val retryKafkaHandler: RetryKafkaHandler) {


    @PostMapping
    fun opprettValgtHendelsePaaTopic() {

        //do stuff here
        retryKafkaHandler.publishRetryHendelsePaaKafka("hei fra post")

    }

}
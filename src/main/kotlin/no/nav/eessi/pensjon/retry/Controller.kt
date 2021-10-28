package no.nav.eessi.pensjon.retry

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController

@RestController("/")
class Controller(private val retryKafkaHandler: RetryKafkaHandler, private val retryService: RetryService) {


    @PostMapping
    fun opprettValgtHendelsePaaTopic() {

        //do stuff here
        retryKafkaHandler.publishRetryHendelsePaaKafka("hei fra post")

    }

    @GetMapping("retrylist")
    fun henteListeAvFeiledeHendelser() : List<HendelsModelRetry>{
        return retryService.hentAlleFeiledeHendelser()
    }
}
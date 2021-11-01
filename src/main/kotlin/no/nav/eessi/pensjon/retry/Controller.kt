package no.nav.eessi.pensjon.retry

import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController("/")
class Controller(private val retryKafkaHandler: RetryKafkaHandler, private val retryService: RetryService) {

    private val logger = LoggerFactory.getLogger(Controller::class.java)


    @PostMapping("retry",  consumes = ["application/json"])
    fun opprettValgtHendelsePaaTopic( @RequestBody retry: HendelseModelRetry) {

        logger.debug("legger retry obj på kafka")
        retryKafkaHandler.publishRetryHendelsePaaKafka(retry)

        logger.debug("sletter retry obj fra s3")
        retryService.slettRetryHendelse(retry)

    }

    @GetMapping("retrylist")
    fun henteListeAvFeiledeHendelser() : List<HendelseModelRetry>{

        logger.debug("henter liste over alle retry hendelser")
        return retryService.hentAlleFeiledeHendelser()

    }

}
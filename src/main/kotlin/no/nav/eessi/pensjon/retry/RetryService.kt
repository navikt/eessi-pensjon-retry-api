package no.nav.eessi.pensjon.retry

import no.nav.eessi.pensjon.s3.S3StorageService
import no.nav.eessi.pensjon.utils.mapJsonToAny
import no.nav.eessi.pensjon.utils.typeRefs
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class RetryService(private val storageService: S3StorageService) {

    private val logger = LoggerFactory.getLogger(RetryService::class.java)

    companion object {
        const val RETRY_PATH = "/feilede/"

    }

    fun hentAlleFeiledeHendelser() : List<HendelseModelRetry>{
        val list = storageService.list(RETRY_PATH)

        logger.info("list size: ${list.size}")
        logger.debug("list: $list")

        val objlist = list.mapNotNull { storageService.get(RETRY_PATH+it)?.let {  mapJsonToAny(it, typeRefs<HendelseModelRetry>()) } }

        logger.info("objlist size: ${objlist.size}")

        return objlist
    }

    fun slettRetryHendelse(retry: HendelseModelRetry) {
        val filename = "${retry.hendelseType}-${retry.sedHendelseModel?.rinaSakId}-${retry.sedHendelseModel.rinaDokumentId}.json"
        logger.debug("slette følgende filname fra s3: $filename")

        storageService.delete(RETRY_PATH+filename)
    }

}
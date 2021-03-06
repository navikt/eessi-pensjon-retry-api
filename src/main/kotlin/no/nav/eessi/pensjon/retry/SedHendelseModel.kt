package no.nav.eessi.pensjon.retry

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import no.nav.eessi.pensjon.eux.model.sed.SedType
import no.nav.eessi.pensjon.utils.mapJsonToAny
import no.nav.eessi.pensjon.utils.typeRefs
import org.joda.time.LocalDateTime

@JsonIgnoreProperties(ignoreUnknown = true)
data class SedHendelseModel(
        val id: Long? = 0,
        val sedId: String? = null,
        val sektorKode: String,
        val bucType: BucType?,
        val rinaSakId: String,
        val avsenderId: String? = null,
        val avsenderNavn: String? = null,
        val avsenderLand: String? = null,
        val mottakerId: String? = null,
        val mottakerNavn: String? = null,
        val mottakerLand: String? = null,
        val rinaDokumentId: String,
        val rinaDokumentVersjon: String,
        val sedType: SedType? = null,
        val attemptedRetryTime : List<LocalDateTime> = emptyList()
) {
    companion object {
        fun fromJson(json: String): SedHendelseModel = mapJsonToAny(json, typeRefs())
    }
}

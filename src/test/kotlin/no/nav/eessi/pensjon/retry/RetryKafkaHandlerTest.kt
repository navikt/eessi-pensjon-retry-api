package no.nav.eessi.pensjon.retry

import io.mockk.verify
import no.nav.eessi.pensjon.s3.S3StorageService
import no.nav.eessi.pensjon.utils.mapJsonToAny
import no.nav.eessi.pensjon.utils.toJson
import no.nav.eessi.pensjon.utils.typeRefs
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers


@SpringBootTest(classes = [IntegrasjonsTestConfig::class], value = ["SPRING_PROFILES_ACTIVE", "integrationtest"])
@ActiveProfiles("integrationtest")
@EmbeddedKafka( topics = ["mostperfecttopicname"])
@AutoConfigureMockMvc
internal class RetryIntegrationTest() {

    @Autowired
    private lateinit var storageService: S3StorageService

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var retryService: RetryService

    @Autowired
    private lateinit var retryKafkaHandler: RetryKafkaHandler

    @Test
    fun `Rest-kall til kontroller skal gi en liste tilbake`() {
        lagreHendelsePaaS3(HendelseType.MOTTATT)
        lagreHendelsePaaS3(HendelseType.SENDT)

        val result = mockMvc.perform(
            MockMvcRequestBuilders.get("/retrylist")
                .contentType(MediaType.APPLICATION_JSON)
                .content(retryService.hentAlleFeiledeHendelser().toJson())
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
        val expected = """
            [{"sedHendelseModel":{"id":1869,"sedId":"P2000_b12e06dda2c7474b9998c7139c841646_2","sektorKode":"P","bucType":"P_BUC_01","rinaSakId":"147729","avsenderId":"NO:NAVT003","avsenderNavn":"NAVT003","avsenderLand":"NO","mottakerId":"NO:NAVT007","mottakerNavn":"NAV Test 07","mottakerLand":"NO","rinaDokumentId":"b12e06dda2c7474b9998c7139c841646","rinaDokumentVersjon":"2","sedType":"P2000","attemptedRetryTime":[]},"hendelseType":"MOTTATT"}
            ,{"sedHendelseModel":{"id":1869,"sedId":"P2000_b12e06dda2c7474b9998c7139c841646_2","sektorKode":"P","bucType":"P_BUC_01","rinaSakId":"147729","avsenderId":"NO:NAVT003","avsenderNavn":"NAVT003","avsenderLand":"NO","mottakerId":"NO:NAVT007","mottakerNavn":"NAV Test 07","mottakerLand":"NO","rinaDokumentId":"b12e06dda2c7474b9998c7139c841646","rinaDokumentVersjon":"2","sedType":"P2000","attemptedRetryTime":[]},"hendelseType":"SENDT"}]
            """.filter { !it.isWhitespace() }
        val resultContent = result.response.getContentAsString(charset("UTF-8")).filter { !it.isWhitespace() }
        Assertions.assertEquals(expected, resultContent)
    }

    @Test
    fun `Rest-kall til kontroller skal legge hendelse p?? topic`() {
        lagreHendelsePaaS3(HendelseType.MOTTATT)
        lagreHendelsePaaS3(HendelseType.SENDT)

        Assertions.assertEquals(2, retryService.hentAlleFeiledeHendelser().size)

        mockMvc.perform(
            MockMvcRequestBuilders.post("/retry")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mockHendelse(HendelseType.MOTTATT).toJson())
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()

        Assertions.assertEquals(1, retryService.hentAlleFeiledeHendelser().size)
        verify(exactly = 1) { retryKafkaHandler.publishRetryHendelsePaaKafka(any()) }

    }

    @Test
    fun `N??r vi har en hendelse lagret p?? s3 s?? skal den listes ut av retryService sin hentAlleFeiledeHendelser`() {
        val mockedHendelse = mockHendelse(HendelseType.SENDT)
        val hendelsePath = retryService.retryHendelseFileName(mockedHendelse)

        storageService.put(hendelsePath, mockedHendelse.toJson())
        Assertions.assertEquals(1, retryService.hentAlleFeiledeHendelser().size)
    }

    private fun lagreHendelsePaaS3(hendelseType: HendelseType) {
        val mockedHendelseSendt = mockHendelse(hendelseType)
        val hendelsePathSendt = retryService.retryHendelseFileName(mockedHendelseSendt)
        storageService.put(hendelsePathSendt, mockedHendelseSendt.toJson())
    }

    private fun mockHendelse(hendelseType: HendelseType): HendelseModelRetry {
        val mockhendelse = """
            {
              "id": 1869,
              "sedId": "P2000_b12e06dda2c7474b9998c7139c841646_2",
              "sektorKode": "P",
              "bucType": "P_BUC_01",
              "rinaSakId": "147729",
              "avsenderId": "NO:NAVT003",
              "avsenderNavn": "NAVT003",
              "avsenderLand": "NO",
              "mottakerId": "NO:NAVT007",
              "mottakerNavn": "NAV Test 07",
              "mottakerLand": "NO",
              "rinaDokumentId": "b12e06dda2c7474b9998c7139c841646",
              "rinaDokumentVersjon": "2",
              "sedType": "P2000",
              "attemptedRetryTime":[]
            }
        """.trimIndent()

        return HendelseModelRetry(
            mapJsonToAny(mockhendelse, typeRefs()),
            hendelseType = hendelseType
        )
    }
}
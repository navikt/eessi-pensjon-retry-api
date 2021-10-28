package no.nav.eessi.pensjon.retry

import com.ninjasquad.springmockk.MockkBean
import com.ninjasquad.springmockk.SpykBean
import io.mockk.every
import io.mockk.mockk
import no.nav.eessi.pensjon.personoppslag.pdl.PersonService
import no.nav.eessi.pensjon.security.sts.STSService
import no.nav.eessi.pensjon.security.sts.typeRef
import no.nav.eessi.pensjon.utils.mapAnyToJson
import no.nav.eessi.pensjon.utils.mapJsonToAny
import no.nav.eessi.pensjon.utils.toJson
import no.nav.eessi.pensjon.utils.typeRefs
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers


@SpringBootTest(value = ["SPRING_PROFILES_ACTIVE", "integrationtest"])
@ActiveProfiles("integrationtest")
@AutoConfigureMockMvc
internal class RetryIntegrationTest(){

    @MockkBean
    lateinit var stsService: STSService

    @MockkBean
    lateinit var personService: PersonService

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var retryService: RetryService

    @SpykBean
    private lateinit var controller: Controller

    @Test
    fun `Rest-kall til kontroller skal gi en liste tilbake`(){
        val retList = listOf<HendelsModelRetry>(
            mockHendelse(HendelseType.SENDT),
            mockHendelse(HendelseType.MOTTATT))

        every { retryService.hentAlleFeiledeHendelser() } returns retList

        val result = mockMvc.perform(
            MockMvcRequestBuilders.get("/retrylist")
            .contentType(MediaType.APPLICATION_JSON)
            .content(retList.toJson()))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()

        println(result.response.getContentAsString(charset("UTF-8")))

    }

    private fun mockHendelse(hendelseType: HendelseType) : HendelsModelRetry {
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
              "navBruker": "09035225916"
            }
        """.trimIndent()

        return HendelsModelRetry(mapJsonToAny(mockhendelse, typeRefs()),
        hendelseType = hendelseType)



    }


}
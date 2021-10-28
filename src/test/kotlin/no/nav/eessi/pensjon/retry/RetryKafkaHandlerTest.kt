package no.nav.eessi.pensjon.retry

import com.ninjasquad.springmockk.MockkBean
import com.ninjasquad.springmockk.SpykBean
import io.mockk.every
import io.mockk.mockk
import no.nav.eessi.pensjon.personoppslag.pdl.PersonService
import no.nav.eessi.pensjon.security.sts.STSService
import no.nav.eessi.pensjon.utils.toJson
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
        val retList = listOf<String>()

        val result = mockMvc.perform(
            MockMvcRequestBuilders.get("/retrylist")
            .contentType(MediaType.APPLICATION_JSON)
            .content(retList.toJson()))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()

        println(result.response.getContentAsString(charset("UTF-8")))

    }
}
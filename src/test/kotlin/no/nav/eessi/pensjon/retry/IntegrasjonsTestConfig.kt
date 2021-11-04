package no.nav.eessi.pensjon.retry

import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.AnonymousAWSCredentials
import com.amazonaws.client.builder.AwsClientBuilder
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import io.findify.s3mock.S3Mock
import io.mockk.mockk
import io.mockk.spyk
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory
import org.springframework.kafka.support.serializer.JsonSerializer
import org.springframework.kafka.test.EmbeddedKafkaBroker
import java.net.ServerSocket

@TestConfiguration
class IntegrasjonsTestConfig {
    @Value("\${" + EmbeddedKafkaBroker.SPRING_EMBEDDED_KAFKA_BROKERS + "}")
    private lateinit var brokerAddresses: String

    @Bean
    fun producerFactory(): ProducerFactory<HendelseModelRetry, String> {
        val configs = HashMap<String, Any>()
        configs[ProducerConfig.BOOTSTRAP_SERVERS_CONFIG] = this.brokerAddresses
        configs[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java
        configs[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = JsonSerializer::class.java
        return DefaultKafkaProducerFactory(configs)
    }

    @Bean
    fun consumerFactory(): ConsumerFactory<String, String> {
        val configs = HashMap<String, Any>()
        configs[ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG] = this.brokerAddresses
        configs[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
        configs[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
        configs[ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG] = false
        configs[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "earliest"
        configs[ConsumerConfig.GROUP_ID_CONFIG] = "eessi-pensjon-group-test"

        return DefaultKafkaConsumerFactory(configs)
    }

    @Bean
    @Primary
    fun aivenKafkaTemplate(): KafkaTemplate<HendelseModelRetry, String> {
        val kafka = KafkaTemplate(producerFactory())
        kafka.defaultTopic = "awsomesauce"
        return kafka
    }

    @Bean
    fun s3(): AmazonS3 {

        val s3Port = ServerSocket(0).use { it.localPort }

        S3Mock.Builder().withPort(s3Port).withInMemoryBackend()
            .build()
            .also { it.start() }

        return AmazonS3ClientBuilder.standard()
            .withPathStyleAccessEnabled(true)
            .withCredentials(AWSStaticCredentialsProvider(AnonymousAWSCredentials()))
            .withEndpointConfiguration(
                AwsClientBuilder.EndpointConfiguration("http://localhost:$s3Port", "us-east-1")
            )
            .build()
    }

    @Bean
    fun retryKafkaHandler(): RetryKafkaHandler {
        return spyk(RetryKafkaHandler(mockk(relaxed = true)))
    }
}
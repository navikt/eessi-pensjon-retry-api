package no.nav.eessi.pensjon.s3

import com.amazonaws.AmazonServiceException
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.*
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.annotation.Profile
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.stream.Collectors.joining

private val logger = LoggerFactory.getLogger(S3StorageService::class.java)

@Component
class S3StorageService(
    private val s3: AmazonS3,
    @Value("\${eessi.pensjon.retry.s3.bucket.name}") val bucketname: String,
    @Value("\${ENV}") val env: String)
{

    fun getBucketName(): String {
        return bucketname + postfixFasitEnv()
    }

    // Det settes nå kun dfault i prod, namespace brukes i alle andre miljø
    private fun postfixFasitEnv(): String  = if(env == "p") ""  else  "-$env"

    @EventListener(ApplicationReadyEvent::class)
    fun init() {
        try {
            ensureBucketExists()
            ensureVersioningIsEnabled()
            logger.debug("S3-storage ready with bucket: ${getBucketName()}")
        } catch (e: Exception) {
            logger.warn("Failed to connect to or create bucket ${getBucketName()}", e)
        }
    }

    private fun ensureVersioningIsEnabled() {
        val versionConfig = s3.getBucketVersioningConfiguration(getBucketName())
        logger.debug("Bucket versioning configuration status: ${versionConfig.status}")

        if (versionConfig.status == "Enabled")
            return

        logger.debug("Enabling versioning on bucket ${getBucketName()}")
        try {
            val versioningConfiguration = BucketVersioningConfiguration().withStatus("Enabled")
            val setBucketVersioningConfigurationRequest =
                SetBucketVersioningConfigurationRequest(getBucketName(), versioningConfiguration)
            s3.setBucketVersioningConfiguration(setBucketVersioningConfigurationRequest)
        } catch (e: Exception) {
            logger.error("Failed to create versioned S3 bucket: ${e.message}")
            throw e
        }
    }

    private fun ensureBucketExists() {
        logger.debug("Checking if bucket exists")
        val bucketExists = s3.listBuckets().stream()
            .anyMatch { it.name == getBucketName() }
        if (!bucketExists) {
            logger.debug("Bucket does not exist, creating new bucket: $bucketname")
            s3.createBucket(CreateBucketRequest(getBucketName()).withCannedAcl(CannedAccessControlList.Private))
        }
    }

    /**
     * Lister objekter med prefix $path, path må begynne med fnr/dnr dersom innlogget bruker er borger
     *
     * @param path
     */
    fun list(path: String): List<String> {
        return try {
            val list = mutableListOf<String>()
            val listObjectsRequest = populerListObjectRequest(path)
            val objectListing = s3.listObjectsV2(listObjectsRequest)
            objectListing.objectSummaries.mapTo(list) { it.key }
            list
        } catch (ex: AmazonServiceException) {
            logger.error("En feil oppstod under listing av bucket ex: $ex message: ${ex.errorMessage} errorcode: ${ex.errorCode}")
            throw ex
        } catch (ex: Exception) {
            logger.error("En feil oppstod under listing av bucket ex: $ex")
            throw ex
        }
    }

    fun get(path: String): String? {
        return try {
            val content: String
            logger.info("Getting plaintext path")
            val s3Object = s3.getObject(getBucketName(), path)
            content = readS3Stream(s3Object)
            content
        } catch (se: AmazonServiceException) {
            if (se.statusCode == 404) {
                logger.info("Objektet som forsøkes å hentes finnes ikke $se")
                return null
            } else {
                logger.error("En feil oppstod under henting av objekt ex: $se message: ${se.errorMessage} errorcode: ${se.errorCode}")
                throw se
            }
        } catch (ex: Exception) {
            logger.error("En feil oppstod under henting av objekt ex: $ex")
            throw ex
        }
    }

    fun delete(path: String) {
        try {
            s3.deleteObject(getBucketName(), path)
        } catch (ex: AmazonServiceException) {
            logger.error("En feil oppstod under sletting av objekt ex: $ex message: ${ex.errorMessage} errorcode: ${ex.errorCode}")
            throw ex
        } catch (ex: Exception) {
            logger.error("En feil oppstod under sletting av objekt ex: $ex")
            throw ex
        }
    }

    /**
     * Lagrer nytt S3 objekt.
     *
     * @param path <fnr/dnr/ad-bruker>___<valgfri filending>
     * @param content innholdet i objektet
     */
    fun put(path: String, content: String) {
        try {
            s3.putObject(getBucketName(), path, content)
        } catch (ex: AmazonServiceException) {
            logger.error("En feil oppstod under opprettelse av objekt ex: $ex message: ${ex.errorMessage} errorcode: ${ex.errorCode}")
            throw ex
        } catch (ex: Exception) {
            logger.error("En feil oppstod under opprettelse av objekt ex: $ex")
            throw ex
        }
    }

    fun multipleDelete(paths: List<String>) {
        throw RuntimeException("Not implemented")
    }

    private fun populerListObjectRequest(cipherPath: String?): ListObjectsV2Request? {
        return ListObjectsV2Request()
            .withBucketName(getBucketName())
            .withPrefix(cipherPath)
    }

    private fun readS3Stream(s3Object: S3Object): String {
        val inputStreamReader = InputStreamReader(s3Object.objectContent)
        val content = BufferedReader(inputStreamReader)
            .lines()
            .collect(joining("\n"))
        inputStreamReader.close()
        return content
    }
}

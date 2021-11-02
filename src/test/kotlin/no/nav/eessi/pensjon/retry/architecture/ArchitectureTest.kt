package no.nav.eessi.pensjon.retry.architecture

import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.core.importer.ImportOptions
import com.tngtech.archunit.library.Architectures.layeredArchitecture
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices
import no.nav.eessi.pensjon.EessiPensjonRetryApplication
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ArchitectureTest {

    private val root = EessiPensjonRetryApplication::class.qualifiedName!!
        .replace("." + EessiPensjonRetryApplication::class.simpleName, "")

    private val classesToAnalyze = ClassFileImporter()
        .importClasspath(
            ImportOptions()
                .with(ImportOption.DoNotIncludeJars())
                .with(ImportOption.DoNotIncludeArchives())
                .with(ImportOption.DoNotIncludeTests())
        )

    @BeforeAll
    fun beforeAll() {
        // Validate number of classes to analyze
        assertTrue(classesToAnalyze.size > 10, "Sanity check on no. of classes to analyze")
        assertTrue(classesToAnalyze.size < 500, "Sanity check on no. of classes to analyze")
    }

    @Test
    fun `Packages should not have cyclic dependencies`() {
        slices().matching("$root.(*)..").should().beFreeOfCycles().check(classesToAnalyze)
    }

    @Test
    fun `Services should not depend on each other`() {
        slices().matching("..$root.services.(**)").should().notDependOnEachOther().check(classesToAnalyze)
    }

    @Test
    fun `Check architecture`() {
        val ROOT = "root"
        val Retry = "retry"
        val Config = "config "
        val Health = "health"
        val S3Service = "services"

        layeredArchitecture()
            //Define components
            .layer(ROOT).definedBy(root)
            .layer(Retry).definedBy("$root.retry")
            .layer(Config).definedBy("$root.config")
            .layer(Health).definedBy("$root.health")
            .layer(S3Service).definedBy("$root.services")

            //define rules
            .whereLayer(ROOT).mayNotBeAccessedByAnyLayer()
            .whereLayer(Health).mayNotBeAccessedByAnyLayer()
            .whereLayer(Retry).mayNotBeAccessedByAnyLayer()
            .whereLayer(S3Service).mayOnlyBeAccessedByLayers(Retry)


            //Verify rules
            .check(classesToAnalyze)
    }
}

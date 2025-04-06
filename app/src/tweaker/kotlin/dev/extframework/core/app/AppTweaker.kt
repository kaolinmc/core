package dev.extframework.core.app

import com.durganmcbroom.jobs.Job
import com.durganmcbroom.jobs.job
import dev.extframework.core.app.api.ApplicationTarget
import dev.extframework.tooling.api.environment.*
import dev.extframework.tooling.api.tweaker.EnvironmentTweaker

public class AppTweaker : EnvironmentTweaker {
    override fun tweak(
        environment: ExtensionEnvironment
    ): Job<Unit> = job {
        // Target linker/resolver
        val linker = TargetLinker()

        linker.target = environment[ApplicationTarget]
        environment += linker

        val targetLinkerResolver = TargetLinkerResolver(linker)
        environment += targetLinkerResolver
        environment.archiveGraph.registerResolver(targetLinkerResolver)

        // Partition loaders
//        val partitionContainer = environment[partitionLoadersAttrKey].extract().container
//        partitionContainer.register(MainPartitionLoader.TYPE, MainPartitionLoader(environment))
//        partitionContainer.register(TargetPartitionLoader.TYPE, TargetPartitionLoader(environment))
//        partitionContainer.register(
//            FeaturePartitionLoader.TYPE,
//            FeaturePartitionLoader(environment[ExtensionResolver].extract().partitionResolver)
//        )

        // Extension pre init
//        environment += CoreExtensionPreInitializer(
//            environment.archiveGraph,
//            environment[ExtensionResolver].extract()
//        )

        // Extension init
//        environment += CoreExtensionInitializer(
//            environment[mixinAgentsAttrKey].extract().filterIsInstance<MixinSubsystem>(), linker
//        )

        // Annotation processing
//        environment += AnnotationProcessorImpl()

        // Delegation
//        environment += MutableObjectSetAttribute(delegationProvidersAttrKey)
//        val delegationProviders = environment[delegationProvidersAttrKey].extract()

//        environment += DelegationImpl(delegationProviders)
//
//        delegationProviders.add(FeatureDelegationProvider())
    }
}
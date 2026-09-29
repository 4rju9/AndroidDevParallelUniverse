import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication

apply(plugin = "maven-publish")

afterEvaluate {
    configure<PublishingExtension> {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])
                groupId = project.property("LIBRARY_GROUP").toString()
                artifactId = if (project.extra.has("ARTIFACT_ID")) {
                    project.extra.get("ARTIFACT_ID").toString()
                } else {
                    project.name
                }
                version = project.version.toString()
            }
        }
    }
}
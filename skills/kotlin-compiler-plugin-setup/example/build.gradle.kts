// Single source of truth for Maven coordinates: gradle.properties (GROUP / VERSION_NAME).
// Every module publishes as GROUP:<project.name>:VERSION_NAME, so modules never call
// `mavenPublishing { coordinates(...) }` themselves.
allprojects {
    group = providers.gradleProperty("GROUP").get()
    version = providers.gradleProperty("VERSION_NAME").get()
}

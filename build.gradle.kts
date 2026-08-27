plugins {
    id("com.gtnewhorizons.gtnhconvention")
}

version = "1.0.2"

tasks.test.configure {
    useJUnitPlatform()
}

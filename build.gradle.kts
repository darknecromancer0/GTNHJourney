plugins {
    id("com.gtnewhorizons.gtnhconvention")
}

version = "1.1.23"

tasks.test.configure {
    useJUnitPlatform()
}

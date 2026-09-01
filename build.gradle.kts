plugins {
    id("com.gtnewhorizons.gtnhconvention")
}

version = "1.1.18"

tasks.test.configure {
    useJUnitPlatform()
}

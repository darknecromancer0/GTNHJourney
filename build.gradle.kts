plugins {
    id("com.gtnewhorizons.gtnhconvention")
}

version = "1.1.10"

tasks.test.configure {
    useJUnitPlatform()
}

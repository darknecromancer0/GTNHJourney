plugins {
    id("com.gtnewhorizons.gtnhconvention")
}

version = "1.1.16"

tasks.test.configure {
    useJUnitPlatform()
}

plugins {
    id("com.gtnewhorizons.gtnhconvention")
}

version = "1.1.17"

tasks.test.configure {
    useJUnitPlatform()
}

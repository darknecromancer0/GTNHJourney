plugins {
    id("com.gtnewhorizons.gtnhconvention")
}

version = "1.1.20"

tasks.test.configure {
    useJUnitPlatform()
}

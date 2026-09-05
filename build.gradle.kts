plugins {
    id("com.gtnewhorizons.gtnhconvention")
}

version = "1.1.30"

tasks.test.configure {
    useJUnitPlatform()
}

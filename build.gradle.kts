plugins {
    id("com.gtnewhorizons.gtnhconvention")
}

version = "1.1.8"

tasks.test.configure {
    useJUnitPlatform()
}

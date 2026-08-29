plugins {
    id("com.gtnewhorizons.gtnhconvention")
}

version = "1.1.4"

tasks.test.configure {
    useJUnitPlatform()
}

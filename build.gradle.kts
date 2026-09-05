plugins {
    id("com.gtnewhorizons.gtnhconvention")
}

version = "1.1.26"

tasks.test.configure {
    useJUnitPlatform()
}

plugins {
    id("com.gtnewhorizons.gtnhconvention")
}

version = "1.1.24"

tasks.test.configure {
    useJUnitPlatform()
}

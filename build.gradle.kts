plugins {
    id("com.gtnewhorizons.gtnhconvention")
}

version = "1.1.22"

tasks.test.configure {
    useJUnitPlatform()
}

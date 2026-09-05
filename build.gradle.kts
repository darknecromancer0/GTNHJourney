plugins {
    id("com.gtnewhorizons.gtnhconvention")
}

version = "1.1.28"

tasks.test.configure {
    useJUnitPlatform()
}

plugins {
    id("com.gtnewhorizons.gtnhconvention")
}

version = "1.1.32"

tasks.test.configure {
    useJUnitPlatform()
}

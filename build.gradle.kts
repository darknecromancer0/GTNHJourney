plugins {
    id("com.gtnewhorizons.gtnhconvention")
}

version = "1.1.14"

tasks.test.configure {
    useJUnitPlatform()
}

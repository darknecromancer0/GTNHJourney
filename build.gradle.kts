plugins {
    id("com.gtnewhorizons.gtnhconvention")
}

version = "1.1.21"

tasks.test.configure {
    useJUnitPlatform()
}

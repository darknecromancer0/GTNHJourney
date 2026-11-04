plugins {
    id("com.gtnewhorizons.gtnhconvention")
}

version = "1.1.42"

tasks.test.configure {
    useJUnitPlatform()
}

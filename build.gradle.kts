plugins {
    id("com.gtnewhorizons.gtnhconvention")
}

version = "1.1.19"

tasks.test.configure {
    useJUnitPlatform()
}

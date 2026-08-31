// Modul :core — logika bisnis murni (Kotlin/JVM, tanpa dependensi Android),
// supaya bisa di-unit-test cepat tanpa emulator/perangkat Android.
plugins {
    id("org.jetbrains.kotlin.jvm")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    testImplementation("junit:junit:4.13.2")
}

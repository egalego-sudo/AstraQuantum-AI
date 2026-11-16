package com.bittupandit.astraquantumai

/**
 * Small, deterministic SI calculator layer.
 * Inputs are SI quantities unless a method explicitly says otherwise.
 * This layer does not use an LLM to perform arithmetic.
 */
data class ScienceCalculation(
    val name: String,
    val value: Double,
    val unit: String,
    val working: String,
    val assumptions: String
)

object ScienceCalculator {
    private fun finitePositive(name: String, value: Double) {
        require(value.isFinite() && value > 0.0) { "$name must be finite and greater than zero." }
    }
    private fun finite(name: String, value: Double) {
        require(value.isFinite()) { "$name must be finite." }
    }

    /** Newton's second law. Force in newtons, mass in kg, acceleration in m/s². */
    fun netForce(massKg: Double, accelerationMps2: Double): ScienceCalculation {
        finitePositive("Mass", massKg); finite("Acceleration", accelerationMps2)
        val result = massKg * accelerationMps2
        return ScienceCalculation("Net force", result, "N",
            "F = m × a = $massKg kg × $accelerationMps2 m/s²",
            "Constant-mass Newtonian model in an inertial reference frame.")
    }

    /** Translational kinetic energy. */
    fun kineticEnergy(massKg: Double, speedMps: Double): ScienceCalculation {
        finitePositive("Mass", massKg); finite("Speed", speedMps)
        require(speedMps >= 0.0) { "Speed cannot be negative." }
        val result = 0.5 * massKg * speedMps * speedMps
        return ScienceCalculation("Kinetic energy", result, "J",
            "K = ½ × m × v² = 0.5 × $massKg kg × ($speedMps m/s)²",
            "Non-relativistic translational kinetic energy; speed much smaller than c.")
    }

    /** Ohm's law. Voltage in volts and resistance in ohms. */
    fun ohmsLawCurrent(voltageV: Double, resistanceOhm: Double): ScienceCalculation {
        finite("Voltage", voltageV); finitePositive("Resistance", resistanceOhm)
        val result = voltageV / resistanceOhm
        return ScienceCalculation("Electric current", result, "A",
            "I = V / R = $voltageV V / $resistanceOhm Ω",
            "Ohmic component under conditions where resistance is constant.")
    }

    /** Ideal gas law. Pressure Pa, volume m³, amount mol, temperature K. */
    fun idealGasPressure(amountMol: Double, temperatureK: Double, volumeM3: Double): ScienceCalculation {
        finitePositive("Amount", amountMol); finitePositive("Temperature", temperatureK); finitePositive("Volume", volumeM3)
        val gasConstant = 8.31446261815324
        val result = amountMol * gasConstant * temperatureK / volumeM3
        return ScienceCalculation("Ideal-gas pressure", result, "Pa",
            "P = nRT/V = $amountMol mol × $gasConstant J/(mol·K) × $temperatureK K / $volumeM3 m³",
            "Ideal-gas approximation; deviations can be important at high pressure or near condensation.")
    }

    /** Molar concentration. Amount in mol, final solution volume in m³; output mol/m³. */
    fun molarConcentration(amountMol: Double, solutionVolumeM3: Double): ScienceCalculation {
        finitePositive("Amount", amountMol); finitePositive("Solution volume", solutionVolumeM3)
        val result = amountMol / solutionVolumeM3
        return ScienceCalculation("Amount concentration", result, "mol/m³",
            "c = n/V = $amountMol mol / $solutionVolumeM3 m³",
            "Volume is the final solution volume; convert units explicitly before calculation.")
    }

    /** Sensible heat. Specific heat J/(kg·K), mass kg, temperature change K. */
    fun sensibleHeat(massKg: Double, specificHeatJPerKgK: Double, deltaTemperatureK: Double): ScienceCalculation {
        finitePositive("Mass", massKg); finitePositive("Specific heat", specificHeatJPerKgK); finite("Temperature change", deltaTemperatureK)
        val result = massKg * specificHeatJPerKgK * deltaTemperatureK
        return ScienceCalculation("Sensible heat", result, "J",
            "Q = mcΔT = $massKg kg × $specificHeatJPerKgK J/(kg·K) × $deltaTemperatureK K",
            "Specific heat is treated as constant; no phase transition is included.")
    }

    /** Wave speed = frequency × wavelength. */
    fun waveSpeed(frequencyHz: Double, wavelengthM: Double): ScienceCalculation {
        finitePositive("Frequency", frequencyHz); finitePositive("Wavelength", wavelengthM)
        val result = frequencyHz * wavelengthM
        return ScienceCalculation("Wave speed", result, "m/s",
            "v = fλ = $frequencyHz Hz × $wavelengthM m",
            "For a monochromatic wave component in the stated medium.")
    }

    fun gravitationalForce(mass1Kg: Double, mass2Kg: Double, separationM: Double): ScienceCalculation {
        finitePositive("Mass 1", mass1Kg); finitePositive("Mass 2", mass2Kg); finitePositive("Separation", separationM)
        val g = 6.67430e-11
        val result = g * mass1Kg * mass2Kg / (separationM * separationM)
        require(result.isFinite()) { "Result is outside the supported numeric range." }
        return ScienceCalculation("Newtonian gravitational force", result, "N",
            "F = G m₁m₂/r² = $g × $mass1Kg × $mass2Kg / $separationM²",
            "Point masses or spherically symmetric bodies; weak-field Newtonian approximation.")
    }

    fun workByConstantForce(forceN: Double, displacementM: Double, angleDegrees: Double): ScienceCalculation {
        finite("Force", forceN); finite("Displacement", displacementM); finite("Angle", angleDegrees)
        require(forceN >= 0.0 && displacementM >= 0.0) { "Force magnitude and displacement must be non-negative." }
        require(angleDegrees in 0.0..180.0) { "Angle must be between 0 and 180 degrees." }
        val result = forceN * displacementM * kotlin.math.cos(Math.toRadians(angleDegrees))
        return ScienceCalculation("Work by constant force", result, "J",
            "W = F d cos θ = $forceN × $displacementM × cos($angleDegrees°)",
            "Force is constant; angle is between force and displacement vectors.")
    }

    fun density(massKg: Double, volumeM3: Double): ScienceCalculation {
        finitePositive("Mass", massKg); finitePositive("Volume", volumeM3)
        val result = massKg / volumeM3
        require(result.isFinite()) { "Result is outside the supported numeric range." }
        return ScienceCalculation("Mass density", result, "kg/m³", "ρ = m/V = $massKg / $volumeM3",
            "Bulk density; for nonuniform material this is an average over the stated volume.")
    }

    fun pressure(forceN: Double, areaM2: Double): ScienceCalculation {
        finite("Force", forceN); finitePositive("Area", areaM2)
        require(forceN >= 0.0) { "Force magnitude must be non-negative." }
        val result = forceN / areaM2
        require(result.isFinite()) { "Result is outside the supported numeric range." }
        return ScienceCalculation("Pressure", result, "Pa", "P = F/A = $forceN / $areaM2",
            "Force is normal to the stated area; average pressure over that area.")
    }

    fun electricPower(voltageV: Double, currentA: Double): ScienceCalculation {
        finite("Voltage", voltageV); finite("Current", currentA)
        val result = voltageV * currentA
        require(result.isFinite()) { "Result is outside the supported numeric range." }
        return ScienceCalculation("Electric power", result, "W", "P = VI = $voltageV × $currentA",
            "Instantaneous DC or resistive power; AC power requires phase and RMS treatment.")
    }

    fun photonEnergy(frequencyHz: Double): ScienceCalculation {
        finitePositive("Frequency", frequencyHz)
        val h = 6.62607015e-34
        val result = h * frequencyHz
        require(result.isFinite()) { "Result is outside the supported numeric range." }
        return ScienceCalculation("Photon energy", result, "J", "E = hf = $h × $frequencyHz",
            "Single photon; Planck constant is exact in the SI.")
    }
}

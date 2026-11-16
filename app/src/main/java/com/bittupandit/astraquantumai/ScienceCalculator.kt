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
}

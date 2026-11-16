package com.bittupandit.astraquantumai

import org.junit.Assert.assertEquals
import org.junit.Test

class ChemicalEquationBalancerTest {
    @Test fun balancesWater() {
        assertEquals("2H2 + O2 → 2H2O", ChemicalEquationBalancer.balance("H2 + O2 -> H2O").equation)
    }
    @Test fun balancesMethaneCombustion() {
        assertEquals("CH4 + 2O2 → CO2 + 2H2O", ChemicalEquationBalancer.balance("CH4 + O2 -> CO2 + H2O").equation)
    }
    @Test fun supportsNestedGroups() {
        assertEquals("2Al + 3CuSO4 → Al2(SO4)3 + 3Cu",
            ChemicalEquationBalancer.balance("Al + CuSO4 -> Al2(SO4)3 + Cu").equation)
    }
    @Test fun balancesIronOxide() {
        assertEquals("4Fe + 3O2 → 2Fe2O3", ChemicalEquationBalancer.balance("Fe + O2 -> Fe2O3").equation)
    }
    @Test(expected = IllegalArgumentException::class)
    fun rejectsMissingArrow() { ChemicalEquationBalancer.balance("H2 + O2 = H2O") }
}

package com.bittupandit.astraquantumai

/** Compact periodic-table records. Atomic masses are standard abridged values, not isotope masses. */
data class ElementRecord(
    val atomicNumber: Int,
    val symbol: String,
    val name: String,
    val abridgedAtomicMass: Double?,
    val group: Int?,
    val period: Int,
    val category: String
)

object PeriodicTableCatalog {
    const val VERSION = "0.1.0"
    val elements = listOf(
        ElementRecord(1,"H","Hydrogen",1.008,1,1,"Nonmetal"),
        ElementRecord(2,"He","Helium",4.002602,18,1,"Noble gas"),
        ElementRecord(3,"Li","Lithium",6.94,1,2,"Alkali metal"),
        ElementRecord(4,"Be","Beryllium",9.0121831,2,2,"Alkaline earth metal"),
        ElementRecord(5,"B","Boron",10.81,13,2,"Metalloid"),
        ElementRecord(6,"C","Carbon",12.011,14,2,"Nonmetal"),
        ElementRecord(7,"N","Nitrogen",14.007,15,2,"Nonmetal"),
        ElementRecord(8,"O","Oxygen",15.999,16,2,"Nonmetal"),
        ElementRecord(9,"F","Fluorine",18.998403163,17,2,"Halogen"),
        ElementRecord(10,"Ne","Neon",20.1797,18,2,"Noble gas"),
        ElementRecord(11,"Na","Sodium",22.98976928,1,3,"Alkali metal"),
        ElementRecord(12,"Mg","Magnesium",24.305,2,3,"Alkaline earth metal"),
        ElementRecord(13,"Al","Aluminium",26.9815385,13,3,"Post-transition metal"),
        ElementRecord(14,"Si","Silicon",28.085,14,3,"Metalloid"),
        ElementRecord(15,"P","Phosphorus",30.973761998,15,3,"Nonmetal"),
        ElementRecord(16,"S","Sulfur",32.06,16,3,"Nonmetal"),
        ElementRecord(17,"Cl","Chlorine",35.45,17,3,"Halogen"),
        ElementRecord(18,"Ar","Argon",39.948,18,3,"Noble gas"),
        ElementRecord(19,"K","Potassium",39.0983,1,4,"Alkali metal"),
        ElementRecord(20,"Ca","Calcium",40.078,2,4,"Alkaline earth metal"),
        ElementRecord(26,"Fe","Iron",55.845,8,4,"Transition metal"),
        ElementRecord(29,"Cu","Copper",63.546,11,4,"Transition metal"),
        ElementRecord(47,"Ag","Silver",107.8682,11,5,"Transition metal"),
        ElementRecord(79,"Au","Gold",196.966569,11,6,"Transition metal"),
        ElementRecord(80,"Hg","Mercury",200.592,12,6,"Transition metal"),
        ElementRecord(82,"Pb","Lead",207.2,14,6,"Post-transition metal"),
        ElementRecord(92,"U","Uranium",238.02891,null,7,"Actinide")
    )

    fun byAtomicNumber(number: Int): ElementRecord? = elements.firstOrNull { it.atomicNumber == number }
    fun search(query: String): List<ElementRecord> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return elements
        return elements.filter { it.name.lowercase().contains(q) || it.symbol.lowercase() == q || it.atomicNumber.toString() == q }
    }
}

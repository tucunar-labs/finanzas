package cl.nuxapower.finanzas.data.model

/** Categorías sembradas al crear el hogar. Son editables y archivables. */
val categoriasPorDefecto: List<Categoria> = listOf(
    Categoria("", "Supermercado", "🛒", 0xFF43A047, Tipo.GASTO),
    Categoria("", "Comida fuera", "🍽️", 0xFFFB8C00, Tipo.GASTO),
    Categoria("", "Transporte", "🚌", 0xFF1E88E5, Tipo.GASTO),
    Categoria("", "Cuentas", "🧾", 0xFF8E24AA, Tipo.GASTO),
    Categoria("", "Salud", "💊", 0xFFE53935, Tipo.GASTO),
    Categoria("", "Ocio", "🎉", 0xFFFDD835, Tipo.GASTO),
    Categoria("", "Hogar", "🏠", 0xFF6D4C41, Tipo.GASTO),
    Categoria("", "Sueldo", "💼", 0xFF00897B, Tipo.INGRESO),
    Categoria("", "Otros ingresos", "💰", 0xFF7CB342, Tipo.INGRESO),
).mapIndexed { i, c -> c.copy(orden = i) }

/** Colores para las personas: el primero para quien crea el hogar, el segundo para quien se une. */
val coloresPersona: List<Long> = listOf(0xFF00897B, 0xFFD81B60)

/** Paleta para elegir color de categorías y personas. */
val paleta: List<Long> = listOf(
    0xFFE53935, 0xFFD81B60, 0xFF8E24AA, 0xFF5E35B1, 0xFF3949AB, 0xFF1E88E5,
    0xFF039BE5, 0xFF00ACC1, 0xFF00897B, 0xFF43A047, 0xFF7CB342, 0xFFC0CA33,
    0xFFFDD835, 0xFFFFB300, 0xFFFB8C00, 0xFFF4511E, 0xFF6D4C41, 0xFF546E7A,
)

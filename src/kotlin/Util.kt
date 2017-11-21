fun Double.clamp(from: Double, to: Double): Double =
    when {
        this < from -> from
        this > to -> to
        else -> this
    }

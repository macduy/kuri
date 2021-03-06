import halite.*
import java.util.*

class Commander(override val gameMap: GameMap): Intelligence {
    override val self: Player get() = gameMap.myPlayer

    private var ownedPlanets = ArrayList<Planet>()

    override var freePlanets: Int = 0
    override var totalPlanets: Int = 0
    override var enemyPlanets: Int = 0
    override var unownedPlanets: Int = 0
    override val players: Int get() = this.gameMap.players.size
    override val turn: Int get() = gameMap.turn
    override val enemyShips: List<Ship> get() = gameMap.enemyShips

    override var kingdomCenter = Position(0.0, 0.0)

    private val objectives = LinkedList<Objective>()

    var availableObjectives = LinkedList<Objective>()

    override fun getPlanet(id: Int): Planet? {
        return gameMap.allPlanets[id]
    }

    override fun shipExists(id: Int) = this.self.ships.containsKey(id)

    override fun getShip(id: Int) = this.self.ships[id]

    override fun isOwn(planet: Planet) = planet.isOwned && planet.owner == this.self.id

    init {
        // Set up objectives for each planet
        this.objectives.add(EarlyAttackObjective())
        if (this.gameMap.players.size > 2) {
            this.objectives.add(TurtleShipObjective(GameMap.TOP_LEFT_CORNER))
            this.objectives.add(TurtleShipObjective(this.gameMap.topRightCorner))
            this.objectives.add(TurtleShipObjective(this.gameMap.bottomLeftCorner))
            this.objectives.add(TurtleShipObjective(this.gameMap.bottomRightCorner))
        }
        for (planet in this.gameMap.planets.values) {
            this.objectives.add(SettlePlanetObjective(planet))
            this.objectives.add(AttackPlanetObjective(planet))
        }
    }

    fun assignBestObjective(ship: Ship) {
        var bestObjective: Objective? = null
        var bestScore = Double.NEGATIVE_INFINITY

        for (objective in this.availableObjectives) {
            val score = objective.score - objective.distancePenalty(ship)
//            Log.log("${objective.score} - ${objective.distancePenalty(ship)}")
            if (score > bestScore) {
                bestObjective = objective
                bestScore = score
            }
        }

        if (bestObjective != null) {
//            Log.log("Assigning ${ship.id} to $bestObjective with score $bestScore")
            bestObjective.assign(ship)

            if (!bestObjective.isFree()) {
                this.availableObjectives.remove(bestObjective)
            }
        }
    }

    fun update() {
        // Find own planets
        this.updateOwnedPlanets()

        this.computeKingdomCenter()

        this.updateObjectives()

        this.availableObjectives.clear()
        this.objectives.filterTo(this.availableObjectives) { it.valid && it.score > 0 }

        this.updateNearbyEnemyShips()
    }

    fun updateNearbyEnemyShips() {
        for (planet in this.gameMap.planets.values) {
            this.gameMap.enemyShips.filterTo(planet.nearbyEnemyShips) { it.withinDistance(planet, 15.0) }
        }
    }

    fun updateObjectives() {
        // Loop through objectives and update them
        for (objective in this.objectives) {
            objective.update(this)
        }

        // Sort by score
        this.objectives.sortByDescending { it.score }
    }

    private fun computeKingdomCenter() {
        // If there at least 3 planets, use them.
        val planetCenter = if (this.ownedPlanets.size >= 3) this.ownedPlanets.center() else null
        if (planetCenter != null) {
            this.kingdomCenter = planetCenter
            return
        }

        // Otherwise use ships
        var shipCenter = this.self.ships.values.center()
        if (shipCenter != null) {
            this.kingdomCenter = shipCenter
            return
        }

        this.kingdomCenter = Position(0.0, 0.0)
    }

    private fun updateOwnedPlanets() {
        this.ownedPlanets.clear()
        this.gameMap.planets.values.filterTo(ownedPlanets) {
            it.isOwned && it.owner == this.self.id
        }

        this.totalPlanets = this.gameMap.planets.size
        this.freePlanets = this.gameMap.planets.values.count { !it.isOwned }
        this.enemyPlanets = this.totalPlanets - this.freePlanets - this.ownedPlanets.size
        this.unownedPlanets = this.totalPlanets - this.ownedPlanets.size
    }

    public fun logObjectives(count: Int = 5) {
        for (i in (0 until Integer.min(count, this.objectives.count()))) {
            Log.log(this.objectives[i].toString())
        }
    }
}

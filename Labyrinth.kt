fun main() {
    repeat(1) {
        val gameField = GameField(width = 50, height = 10)
        val labyrinth = Labyrinth(gameField = gameField)
        labyrinth.createLabyrinth()
        val pathfinderManager = PathfinderManager(labyrinth = labyrinth)
        pathfinderManager.createPath()
    }
}


fun <T> Array<Array<T>>.print() {
    this.forEach { hor ->
        println(hor.joinToString(""))
    }
}

data class Step(val hor: Int, val ver: Int)
open class Worker(open var ver: Int, open var hor: Int, open val moves: MutableSet<Step>)
data class GameField(val width: Int, val height: Int)
class Labyrinth(private val gameField: GameField) {

    private val map = Array(gameField.height) { Array(gameField.width) { FILLED_CELL } }.apply {
        this[1][0] = EMPTY_CELL
        this[1][1] = EMPTY_CELL
        this[this.size - 2][this[1].size - 1] = EMPTY_CELL
        this[this.size - 2][this[1].size - 2] = EMPTY_CELL
    }
    private val mapSize =
        (gameField.width * gameField.height) - (gameField.width + gameField.height) * 2
    private var countSteps = 0
    private val mainWorker = Worker(1, 1, mutableSetOf())
    private val passageWorker = Worker(map.size - 2, map[0].size - 2, mutableSetOf())

    fun createLabyrinth() {
        createMap()
        createPassage()
        map.print()
    }

    private fun createMap() {
        while (true) {
            nextStep(worker = mainWorker)
            if (isMapCreate()) break
        }
    }

    private fun createPassage() {
        while (true) {
            nextStep(worker = passageWorker)
            if (mainWorker.moves.contains(passageWorker.moves.last())) break
        }
    }

    private fun nextStep(worker: Worker) {
        val step = searchCorrectDirections(worker = worker)
        worker.moves.add(step)
        worker.ver = step.ver
        worker.hor = step.hor
        if (map[step.ver][step.hor] == FILLED_CELL) {
            map[step.ver][step.hor] = EMPTY_CELL
            countSteps++
        }
    }

    private fun searchCorrectDirections(worker: Worker): Step {
        val directions = mutableListOf<Step>()
        val right = Step(worker.hor + 1, worker.ver)
        val left = Step(worker.hor - 1, worker.ver)
        val up = Step(worker.hor, worker.ver + 1)
        val down = Step(worker.hor, worker.ver - 1)
        if (worker.hor > 1 && checkCorrectStep(left, worker)) directions.add(left)
        if (worker.hor < map[0].size - 2 && checkCorrectStep(right, worker)) directions.add(right)
        if (worker.ver > 1 && checkCorrectStep(down, worker)) directions.add(down)
        if (worker.ver < map.size - 2 && checkCorrectStep(up, worker)) directions.add(up)
        return directions.randomOrNull() ?: getRandomStep(worker = worker)
    }

    private fun checkCorrectStep(step: Step, worker: Worker): Boolean {
        val (hor, ver) = step
        if (hor + 1 != worker.hor && worker.moves.contains(Step(hor + 1, ver)) ||
            (hor - 1 != worker.hor && worker.moves.contains(Step(hor - 1, ver))) ||
            (ver + 1 != worker.ver && worker.moves.contains(Step(hor, ver + 1))) ||
            (ver - 1 != worker.ver && worker.moves.contains(Step(hor, ver - 1)))
        ) return false
        return true
    }

    private fun getRandomStep(worker: Worker): Step = worker.moves.random()

    private fun isMapCreate(): Boolean {
        return ((countSteps.toDouble() / mapSize) * 100) > PERCENT_OF_MAP
    }

    fun getMap(): Array<Array<String>> {
        return Array(map.size) { row -> map[row].copyOf() }
    }

    companion object {
        private const val PERCENT_OF_MAP = 55
        const val FILLED_CELL = "⿳"
        const val EMPTY_CELL = "ㆍ"
    }
}


class PathfinderWorker(
    override var ver: Int,
    override var hor: Int,
    override val moves: MutableSet<Step>,
    val crossroads: MutableList<MutableList<Step>>,
    val wrongStep: MutableSet<Step>
) : Worker(ver, hor, moves)

class PathfinderManager(val labyrinth: Labyrinth) {
    private var map = labyrinth.getMap().also { it[1][0] = PATH_CELL }
    private var worker =
        PathfinderWorker(1, 0, mutableSetOf(), mutableListOf(mutableListOf()), mutableSetOf())

    fun createPath() {
        while (true) {
            searchCorrectDirections(worker = worker)
            if (worker.moves.isNotEmpty() && worker.moves.last() == Step(
                    map[0].size - 1,
                    map.size - 2
                )
            ) {
                map.print()
                break
            }
        }
    }

    private fun searchCorrectDirections(worker: PathfinderWorker) {
        val directions = mutableListOf<Step>()
        val right = Step(worker.hor + 1, worker.ver)
        val left = Step(worker.hor - 1, worker.ver)
        val up = Step(worker.hor, worker.ver + 1)
        val down = Step(worker.hor, worker.ver - 1)
        if (worker.ver < map.size - 1 && map[up.ver][up.hor] ==
            Labyrinth.EMPTY_CELL && !worker.wrongStep.contains(up)) directions.add(up)
        if (worker.ver > 0 && map[down.ver][down.hor] ==
            Labyrinth.EMPTY_CELL && !worker.wrongStep.contains(down)) directions.add(down)
        if (worker.hor > 0 && map[left.ver][left.hor] ==
            Labyrinth.EMPTY_CELL && !worker.wrongStep.contains(left)) directions.add(left)
        if (worker.hor < map[0].size - 1 && map[right.ver][right.hor] ==
            Labyrinth.EMPTY_CELL && !worker.wrongStep.contains(right)) directions.add(right)
        chooseDirection(directions = directions)
    }

    private fun chooseDirection(directions: MutableList<Step>) {
        if (directions.isEmpty()) {
            restartSearch(wrongWorker = worker)
            return
        }
        if (directions.size > 1) {
            worker.crossroads.add(directions)
            createNextStep(worker = worker, directions.first())
        } else {
            createNextStep(worker = worker, step = directions.random())
        }
    }

    private fun createNextStep(worker: PathfinderWorker, step: Step) {
        worker.ver = step.ver
        worker.hor = step.hor
        worker.moves.add(step)
        map[worker.ver][worker.hor] = PATH_CELL
    }

    private fun restartSearch(wrongWorker: PathfinderWorker) {
        worker.wrongStep.add(wrongWorker.crossroads.last().first())
        worker = PathfinderWorker(
            1,
            0,
            mutableSetOf(),
            mutableListOf(mutableListOf()),
            wrongWorker.wrongStep
        )
        map = labyrinth.getMap().also { it[1][0] = PATH_CELL }
    }

    companion object {
        private const val PATH_CELL = "❌"
    }
}

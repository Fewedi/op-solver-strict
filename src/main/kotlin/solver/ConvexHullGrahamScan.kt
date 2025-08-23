package masterthesis.solver

import masterthesis.solver.model.Node
import java.util.Stack


object GraphUtils {
    // Cross product for orientation (ccw)
    fun ccw(a: Node, b: Node, c: Node): Int {
        val area = (b.x - a.x) * (c.y - a.y) - (b.y - a.y) * (c.x - a.x)
        return when {
            area > 0 -> 1    // Counter-clockwise
            area < 0 -> -1   // Clockwise
            else -> 0        // Collinear
        }
    }

    // Get the lowest (y), leftmost (x) node
    fun getMinY(nodes: List<Node>): Node {
        return nodes.minWith(compareBy({ it.y }, { it.x }))
    }
}

class ConvexHullGrahamScan {

    private fun sortByAngle(nodes: MutableList<Node>, ref: Node) {
        nodes.sortWith { b, c ->
            if (b == ref) return@sortWith -1
            if (c == ref) return@sortWith 1

            val ccw = GraphUtils.ccw(ref, b, c)

            if (ccw == 0) {
                if (b.x == c.x) {
                    if (b.y < c.y) -1 else 1
                } else {
                    if (b.x < c.x) -1 else 1
                }
            } else {
                -ccw
            }
        }
    }

    fun scan(nodes: List<Node>): List<Node> {
        if (nodes.size < 3) return nodes

        val pts = nodes.toMutableList()
        val minYNode = GraphUtils.getMinY(pts)
        sortByAngle(pts, minYNode)

        val stack = Stack<Node>()
        stack.push(pts[0])
        stack.push(pts[1])

        for (i in 2 until pts.size) {
            val next = pts[i]
            var p = stack.pop()

            while (stack.isNotEmpty() && GraphUtils.ccw(stack.peek(), p, next) <= 0) {
                p = stack.pop()
            }

            stack.push(p)
            stack.push(next)
        }

        val last = stack.pop()
        if (GraphUtils.ccw(stack.peek(), last, minYNode) > 0) {
            stack.push(last)
        }

        return stack.toList()
    }

    fun polygonArea(nodes: List<Node>): Double {
        var area = 0.0
        val n = nodes.size
        for (i in 0 until n) {
            val j = (i + 1) % n
            area += nodes[i].x * nodes[j].y
            area -= nodes[j].x * nodes[i].y
        }
        return kotlin.math.abs(area) / 2.0
    }

    fun polygonPerimeter(nodes: List<Node>): Double {
        var perimeter = 0.0
        val n = nodes.size
        for (i in 0 until n) {
            val j = (i + 1) % n
            val dx = nodes[j].x - nodes[i].x
            val dy = nodes[j].y - nodes[i].y
            perimeter += kotlin.math.sqrt(dx * dx + dy * dy)
        }
        return perimeter
    }
}

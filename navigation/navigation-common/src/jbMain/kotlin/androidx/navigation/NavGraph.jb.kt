/*
 * Copyright 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.navigation

import androidx.annotation.RestrictTo
import androidx.navigation.serialization.generateRoutePattern
import androidx.navigation.serialization.generateRouteWithArgs
import kotlin.jvm.JvmStatic
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer

public actual open class NavGraph actual constructor(
    navGraphNavigator: Navigator<out NavGraph>
) : NavDestination(navGraphNavigator), Iterable<NavDestination> {

    public val nodes = mutableMapOf<String, NavDestination>()
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        get

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public override fun matchDeepLink(route: String): DeepLinkMatch? {
        // First search through any deep links directly added to this NavGraph
        val bestMatch = super.matchDeepLink(route)
        // Then search through all child destinations for a matching deep link
        val bestChildMatch = mapNotNull { child ->
            child.matchDeepLink(route)
        }.maxOrNull()

        return listOfNotNull(bestMatch, bestChildMatch).maxOrNull()
    }

    public actual fun addDestination(node: NavDestination) {
        val innerRoute = node.route
        require(innerRoute != null) {
            "Destinations must have a route"
        }
        if (route != null) {
            require(innerRoute != route) {
                "Destination $node cannot have the same route as graph $this"
            }
        }
        val existingDestination = nodes[innerRoute]
        if (existingDestination === node) {
            return
        }
        check(node.parent == null) {
            "Destination already has a parent set. Call NavGraph.remove() to remove the previous " +
                "parent."
        }
        if (existingDestination != null) {
            existingDestination.parent = null
        }
        node.parent = this
        nodes.put(innerRoute, node)
    }

    public actual fun addDestinations(nodes: Collection<NavDestination?>) {
        for (node in nodes) {
            if (node == null) {
                continue
            }
            addDestination(node)
        }
    }

    public actual fun addDestinations(vararg nodes: NavDestination) {
        for (node in nodes) {
            addDestination(node)
        }
    }

    public actual fun findNode(route: String?): NavDestination? {
        return if (!route.isNullOrBlank()) findNode(route, true) else null
    }

    public actual inline fun <reified T> findNode(): NavDestination? {
        // TODO: Usage generateRoutePattern inside inline function forces publishing this API
        return findNode(serializer<T>().generateRoutePattern())
    }

    @OptIn(InternalSerializationApi::class)
    @Suppress("UNNECESSARY_NOT_NULL_ASSERTION")
    public actual fun <T> findNode(route: T?): NavDestination? {
        return if (route != null) {
            findNode(route!!::class.serializer().generateRoutePattern())
        } else null
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public actual fun findNode(route: String, searchParents: Boolean): NavDestination? {
        // first try matching with routePattern
        val destination = nodes[route] ?: nodes.values.firstOrNull {
            // if not found with routePattern, try matching with route args
            it.matchDeepLink(route) != null
        }

        // Search the parent for the NavDestination if it is not a child of this navigation graph
        // and searchParents is true
        return destination
            ?: if (searchParents && parent != null) parent!!.findNode(route) else null
    }

    public actual final override fun iterator(): MutableIterator<NavDestination> {
        val iterator = nodes.values.iterator()
        return object : MutableIterator<NavDestination> {
            private var current: NavDestination? = null

            override fun hasNext(): Boolean = iterator.hasNext()

            override fun next(): NavDestination = iterator.next().also {
                current = it
            }

            override fun remove() {
                val current = this.current ?:
                    error("You must call next() before you can remove an element")
                current.parent = null
                iterator.remove()
                this.current = null
            }
        }
    }

    public actual fun addAll(other: NavGraph) {
        val iterator = other.iterator()
        while (iterator.hasNext()) {
            val destination = iterator.next()
            iterator.remove()
            addDestination(destination)
        }
    }

    public actual fun remove(node: NavDestination) {
        nodes.remove(node.route)?.also {
            it.parent = null
        }
    }

    public actual fun clear() {
        val iterator = iterator()
        while (iterator.hasNext()) {
            iterator.next()
            iterator.remove()
        }
    }

    override val displayName: String
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        get() = if (!route.isNullOrBlank()) super.displayName else "the root navigation"

    public actual fun setStartDestination(startDestRoute: String) {
        startDestinationRoute = startDestRoute
    }

    public actual inline fun <reified T : Any> setStartDestination() {
        setStartDestination(serializer<T>()) { startDestination ->
            startDestination.route!!
        }
    }

    @OptIn(InternalSerializationApi::class)
    public actual fun <T : Any> setStartDestination(startDestRoute: T) {
        setStartDestination(startDestRoute::class.serializer()) { startDestination ->
            val args = startDestination.arguments.mapValues {
                it.value.type
            }
            startDestRoute.generateRouteWithArgs(args)
        }
    }

    // unfortunately needs to be public so reified setStartDestination can access this
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @OptIn(ExperimentalSerializationApi::class)
    public actual fun <T> setStartDestination(
        serializer: KSerializer<T>,
        parseRoute: (NavDestination) -> String,
    ) {
        val route = serializer.generateRoutePattern()
        val startDest = findNode(route)
        checkNotNull(startDest) {
            "Cannot find startDestination ${serializer.descriptor.serialName} from NavGraph. " +
                "Ensure the starting NavDestination was added with route from KClass."
        }
        // when dest id is based on serializer, we expect the dest route to have been generated
        // and set
        startDestinationRoute = parseRoute(startDest)
    }

    public actual var startDestinationRoute: String? = null

    public override fun toString(): String {
        val sb = StringBuilder()
        sb.append(super.toString())
        val startDestination = findNode(startDestinationRoute)
        sb.append(" startDestination=")
        if (startDestination == null) {
            sb.append(startDestinationRoute)
        } else {
            sb.append("{")
            sb.append(startDestination.toString())
            sb.append("}")
        }
        return sb.toString()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is NavGraph) return false
        return super.equals(other) &&
            nodes == other.nodes
    }

    override fun hashCode(): Int {
        var result = 0
        for ((key, value) in nodes.entries) {
            result = 31 * result + key.hashCode()
            result = 31 * result + value.hashCode()
        }
        return result
    }

    public actual companion object {
        @JvmStatic
        public actual fun NavGraph.findStartDestination(): NavDestination =
            generateSequence(findNode(startDestinationRoute)) {
                if (it is NavGraph) {
                    it.findNode(it.startDestinationRoute)
                } else {
                    null
                }
            }.last()
    }
}

package com.xfastgames.witness.screen.solver

import com.xfastgames.witness.items.data.Panel
import com.xfastgames.witness.screens.solver.PuzzleSolverData
import com.xfastgames.witness.screens.solver.PuzzleSolverDomain
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

@FlowPreview
@ExperimentalCoroutinesApi
class PuzzleSolverDomainTests {

    private val testDomain = PuzzleSolverDomain()
    private val testPanel: Panel = Panel.DEFAULT

    @Test
    fun startTrace() = runBlocking {
        assert(testDomain.state.value is PuzzleSolverData.InSolution)
    }
}
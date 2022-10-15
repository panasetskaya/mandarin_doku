package com.panasetskaia.charactersudoku.presentation.viewmodels

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.panasetskaia.charactersudoku.R
import com.panasetskaia.charactersudoku.domain.SUCCESS
import com.panasetskaia.charactersudoku.domain.entities.Board
import com.panasetskaia.charactersudoku.domain.entities.ChineseCharacter
import com.panasetskaia.charactersudoku.domain.usecases.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

class GameViewModel @Inject constructor(
    application: Application,
    private val getGameResult: GetResultUseCase,
    private val getRandomBoard: GetRandomBoard,
    private val getSavedGameUseCase: GetSavedGameUseCase,
    private val saveGameUseCase: SaveGameUseCase,
    private val getNewGameWithSel: GetNewGameUseCase,
) : AndroidViewModel(application) {

    private val _selectedCellFlow = MutableStateFlow(Pair(NO_SELECTION, NO_SELECTION))
    val selectedCellFlow: StateFlow<Pair<Int, Int>>
        get() = _selectedCellFlow

    private var selectedRow = NO_SELECTION
    private var selectedCol = NO_SELECTION
    private var currentBoard = Board(-1, 9, listOf(), listOf())
    private var nineChars = listOf<String>()

    private val _boardSharedFlow =
        MutableSharedFlow<Board>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val boardSharedFlow: SharedFlow<Board>
        get() = _boardSharedFlow

    private val _nineCharSharedFlow =
        MutableSharedFlow<List<String>>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val nineCharSharedFlow: SharedFlow<List<String>>
        get() = _nineCharSharedFlow


    private var _settingsFinishedLiveData = MutableLiveData<Boolean>()
    val settingsFinishedLiveData: LiveData<Boolean>
        get() = _settingsFinishedLiveData


    init {
        getSavedBoard()
        updateSelection(NO_SELECTION, NO_SELECTION)
        _settingsFinishedLiveData.postValue(true)
    }

    fun handleInput(number: Int) {
        if (selectedRow == NO_SELECTION || selectedCol == NO_SELECTION) return
        if (!currentBoard.getCell(selectedRow, selectedCol).isFixed) {
            val characterValue = nineChars[number]
            val selR = selectedCellFlow.value.first
            val selC = selectedCellFlow.value.second
            currentBoard.getCell(selR, selC).value = characterValue
            currentBoard.getCell(selR, selC).isDoubtful = false
            updateBoard(currentBoard)
        }
        checkForSolution()
    }

    private fun updateBoard(newBoard: Board) {
        currentBoard = newBoard
        _boardSharedFlow.tryEmit(newBoard)

    }

    private fun updateNineChars(newNineChars: List<String>) {
        nineChars = newNineChars
        _nineCharSharedFlow.tryEmit(newNineChars)
    }

    fun updateSelection(row: Int, col: Int) {
        selectedRow = row
        selectedCol = col
        viewModelScope.launch {
            _selectedCellFlow.emit(Pair(row, col))
        }
    }

    fun markSelectedAsDoubtful() {
        val board = currentBoard
        val isCellDoubtful = board.getCell(selectedRow, selectedCol).isDoubtful
        board.getCell(selectedRow, selectedCol).isDoubtful = !isCellDoubtful
        updateBoard(board)
    }

    fun clearSelected() {
        if (selectedRow == NO_SELECTION || selectedCol == NO_SELECTION) return
        val board = currentBoard
        if (!board.getCell(selectedRow, selectedCol).isFixed) {
            board.getCell(selectedRow, selectedCol).value = EMPTY_CELL
        }
        updateBoard(board)

    }

    private fun checkForSolution() {
        val boardCells = currentBoard.cells
        var count = 0
        for (i in boardCells) {
            if (i.value == EMPTY_CELL) {
                count++
            }
        }
        if (count < EMPTY_CELLS_MINIMUM) {
            viewModelScope.launch {

                val gameResult = getGameResult.invoke(currentBoard)
                if (gameResult is SUCCESS) {
                    Toast.makeText(
                        getApplication(),
                        getApplication<Application>().getString(R.string.game_succesful),
                        Toast.LENGTH_SHORT
                    )
                        .show()
                    updateBoard(gameResult.solution)
                } else {
                    Toast.makeText(
                        getApplication(),
                        getApplication<Application>().getString(R.string.check_again),
                        Toast.LENGTH_SHORT
                    )
                        .show()
                }

            }
        }
    }

    fun getNewRandomGame() {
        updateSelection(NO_SELECTION, NO_SELECTION)
        viewModelScope.launch {
            val randomBoard = getRandomBoard.invoke()
            updateNineChars(randomBoard.nineChars)
            updateBoard(randomBoard)
            setSettingsState(true)

        }
    }

    fun getGameWithSelected(selected: List<ChineseCharacter>) {
        updateSelection(NO_SELECTION, NO_SELECTION)
        viewModelScope.launch {
            val listString = mutableListOf<String>()
            for (i in selected) {
                listString.add(i.character)
            }
            updateNineChars(listString)
            val board = getNewGameWithSel(selected)
            updateBoard(board)
        }
    }

    fun setSettingsState(areSettingsDone: Boolean) {
        _settingsFinishedLiveData.postValue(areSettingsDone)
    }


    fun saveBoard() {

        viewModelScope.launch {
            saveGameUseCase(currentBoard)
        }

    }

    private fun getSavedBoard() {
        viewModelScope.launch {
            val savedBoard = getSavedGameUseCase()
            savedBoard?.let {
                updateNineChars(it.nineChars)
                updateBoard(it)
                updateSelection(0, 0)
            } ?: getNewRandomGame()
        }
    }

    companion object {
        internal const val EMPTY_CELLS_MINIMUM = 8
        private const val EMPTY_CELL = "0"
        private const val NO_SELECTION = -1
    }
}


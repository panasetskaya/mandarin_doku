package com.panasetskaia.charactersudoku.presentation.viewmodels

import android.app.Application
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.panasetskaia.charactersudoku.R
import com.panasetskaia.charactersudoku.domain.SUCCESS
import com.panasetskaia.charactersudoku.domain.entities.Board
import com.panasetskaia.charactersudoku.domain.entities.ChineseCharacter
import com.panasetskaia.charactersudoku.domain.entities.Level
import com.panasetskaia.charactersudoku.domain.entities.Record
import com.panasetskaia.charactersudoku.domain.usecases.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import javax.inject.Inject

class GameViewModel @Inject constructor(
    application: Application,
    private val getGameResult: GetResultUseCase,
    private val getRandomBoard: GetRandomBoard,
    private val getSavedGameUseCase: GetSavedGameUseCase,
    private val saveGameUseCase: SaveGameUseCase,
    private val getNewGameWithSel: GetNewGameUseCase,
    private val getRandomByCategory: GetRandomWithCategoryUseCase,
    private val supplyNewRecord: SupplyNewRecordUseCase,
    private val getTopFifteenRecords: GetTopFifteenRecordsUseCase
) : AndroidViewModel(application) {

    private var selectedRow = NO_SELECTION
    private var selectedCol = NO_SELECTION
    private lateinit var currentBoard: Board
    private lateinit var nineChars: List<String>
    private lateinit var selected: List<ChineseCharacter>


    private val levelFlow = MutableStateFlow(Level.MEDIUM)

    private val _timeSpentFlow = MutableStateFlow(0L)
    val timeSpentFlow: StateFlow<Long>
        get() = _timeSpentFlow

    private val _isWinFlow = MutableStateFlow(false)
    val isWinFlow: StateFlow<Boolean>
        get() = _isWinFlow

    private val _isNewFlow = MutableStateFlow(false)
    val isNewFlow: StateFlow<Boolean>
        get() = _isNewFlow

    private val _selectedCellFlow = MutableStateFlow(Pair(NO_SELECTION, NO_SELECTION))
    val selectedCellFlow: StateFlow<Pair<Int, Int>>
        get() = _selectedCellFlow

    private val _boardSharedFlow =
        MutableSharedFlow<Board>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val boardSharedFlow: SharedFlow<Board>
        get() = _boardSharedFlow

    private val _nineCharSharedFlow =
        MutableSharedFlow<List<String>>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val nineCharSharedFlow: SharedFlow<List<String>>
        get() = _nineCharSharedFlow

    private var _settingsFinishedStateFlow = MutableStateFlow(true)
    val settingsFinishedStateFlow: StateFlow<Boolean>
        get() = _settingsFinishedStateFlow

    private val _recordsFlow = MutableSharedFlow<List<Record>>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val recordsFlow: SharedFlow<List<Record>>
        get() = _recordsFlow

    init {
        getSavedBoard()
    }

    fun handleInput(number: Int) {
        if (selectedRow == NO_SELECTION || selectedCol == NO_SELECTION) return
        if (!currentBoard.getCell(selectedRow, selectedCol).isFixed) {
            val characterValue = nineChars[number]
            currentBoard.getCell(selectedRow, selectedCol).value = characterValue
            currentBoard.getCell(selectedRow, selectedCol).isDoubtful = false
            updateBoard(currentBoard)
        }
        checkForSolution()
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

    fun getNewRandomGame(diffLevel: Level) {
        _isNewFlow.value = false
        levelFlow.value = diffLevel
        updateSelection(NO_SELECTION, NO_SELECTION)
        viewModelScope.launch {
            val randomBoard = getRandomBoard.invoke(diffLevel)
            updateNineChars(randomBoard.nineChars)
            reset(randomBoard)
        }
    }

    fun getRandomGameWithCategory(category: String, diffLevel: Level) {
        _isNewFlow.value = false
        levelFlow.value = diffLevel
        updateSelection(NO_SELECTION, NO_SELECTION)
        viewModelScope.launch {
            val randomBoard = getRandomByCategory(category, diffLevel)
            updateNineChars(randomBoard.nineChars)
            reset(randomBoard)
        }
    }

    fun getGameWithSelected() {
        _isNewFlow.value = false
        updateSelection(NO_SELECTION, NO_SELECTION)
        viewModelScope.launch {
            val listString = mutableListOf<String>()
            for (i in selected) {
                listString.add(i.character)
            }
            updateNineChars(listString)
            val board = getNewGameWithSel(selected, levelFlow.value)
            reset(board)
        }
    }

    private fun reset(newBoard: Board) {
        updateBoard(newBoard)
        _timeSpentFlow.value = newBoard.timeSpent
        _isWinFlow.value = false
        setSettingsState(true)
        _isNewFlow.value = true

    }

    fun setSettingsState(areSettingsDone: Boolean) {
        _settingsFinishedStateFlow.value = areSettingsDone
    }

    fun saveBoard(timeSpent: Long) {
        _isNewFlow.value = false
        viewModelScope.launch {
            val boardToSave = if (isWinFlow.value) {
                currentBoard.copy(timeSpent = timeSpent, alreadyFinished = true)
            } else {
                currentBoard.copy(timeSpent = timeSpent, alreadyFinished = false)
            }
            saveGameUseCase(boardToSave)
        }
    }

    private fun getSavedBoard() {
        _isNewFlow.value = false
        viewModelScope.launch {
            val savedBoard = getSavedGameUseCase()
            savedBoard?.let {
                updateNineChars(it.nineChars)
                updateBoard(it)
                updateSelection(0, 0)
                _timeSpentFlow.value = it.timeSpent
                _isWinFlow.value = it.alreadyFinished
            } ?: getNewRandomGame(Level.MEDIUM)
        }
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
                    _isWinFlow.value = true
                    updateBoard(gameResult.solution.copy(alreadyFinished = true))
                    updateTimer(-1L)
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
        _selectedCellFlow.value = Pair(row, col)
    }

    fun updateTimer(timeWhenStopped: Long) {
        _timeSpentFlow.value = timeWhenStopped
    }

    fun setLevel(chosenLevel: Level) {
        levelFlow.value = chosenLevel
    }

    fun setSelected(newSelected: List<ChineseCharacter>) {
        selected = newSelected
    }

    fun saveRecord(recordTime: Long) {
        val current = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val formattedDate = current.format(formatter)
        val newRecord = Record(0, recordTime, levelFlow.value, formattedDate)
        viewModelScope.launch {
            supplyNewRecord(newRecord)
        }
    }

    fun getRecords() {
        viewModelScope.launch {
            _recordsFlow.tryEmit(
                getTopFifteenRecords()
            )
        }
    }

    companion object {
        internal const val EMPTY_CELLS_MINIMUM = 8
        private const val EMPTY_CELL = "0"
        private const val NO_SELECTION = -1
    }
}


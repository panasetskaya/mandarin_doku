package com.panasetskaia.charactersudoku.presentation.game_screen

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.res.ResourcesCompat
import com.panasetskaia.charactersudoku.R
import com.panasetskaia.charactersudoku.domain.entities.Cell
import java.util.*

class SudokuBoardView(context: Context, attributeSet: AttributeSet) : View(context, attributeSet) {

    private var sqrtSize = 3
    private var size = 9
    private var cellSizePixels = 0F
    private var selectedRow = -10
    private var selectedCol = -10
    private var listener: OnTouchListener? = null
    private var cells: List<Cell>? = null
    private var startCLickTime: Long = 0
    private var textSizeForCell = 80F

    private val thinLinePaint = Paint().apply {
        style = Paint.Style.STROKE
        color = resources.getColor(R.color.boardLineColor)
        strokeWidth = 2F
    }

    private val thickLinePaint = Paint().apply {
        style = Paint.Style.STROKE
        color = resources.getColor(R.color.boardLineColor)
        strokeWidth = 6F
    }

    private val notSelectedCellPaint = Paint().apply {
        style = Paint.Style.FILL_AND_STROKE
        color = resources.getColor(R.color.primaryTextColor)
    }

    private val selectedCellPaint = Paint().apply {
        style = Paint.Style.FILL_AND_STROKE
        color = resources.getColor(R.color.primaryDarkColor)
    }

    private val cellToWatchForPaint = Paint().apply {
        style = Paint.Style.FILL_AND_STROKE
        color = resources.getColor(R.color.veryTransparentGr)
    }

    private val notSelectedTextPaint = Paint().apply {
        style = Paint.Style.FILL_AND_STROKE
        color = resources.getColor(R.color.defaultBoardTextColor)
        textSize = textSizeForCell
        typeface =  ResourcesCompat.getFont(context, MY_FONT)
    }

    private val selectedTextPaint = Paint().apply {
        style = Paint.Style.FILL_AND_STROKE
        color = Color.WHITE
        textSize = textSizeForCell
        typeface =  ResourcesCompat.getFont(context, MY_FONT)
    }


    private val fixedTextPaint = Paint().apply {
        style = Paint.Style.FILL_AND_STROKE
        color = resources.getColor(R.color.primaryLightColor)
        textSize = textSizeForCell
        typeface =  ResourcesCompat.getFont(context, MY_FONT)
    }

    private val doubtfulTextPaint = Paint().apply {
        style = Paint.Style.FILL_AND_STROKE
        color = Color.BLACK
        typeface =  ResourcesCompat.getFont(context, MY_FONT)
        textSize = textSizeForCell
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val sizePixels = widthMeasureSpec.coerceAtMost(heightMeasureSpec)
        setMeasuredDimension(sizePixels, sizePixels)
    }

    override fun onDraw(canvas: Canvas) {
        cellSizePixels = (width / size).toFloat()
        fillCells(canvas)
        drawLines(canvas)
        drawText(canvas)
    }

    private fun fillCells(canvas: Canvas) {
        if (selectedRow == -1 || selectedCol == -1) return
        var currentValue = ""
        cells?.forEach {
            if (it.row == selectedRow && it.col == selectedCol) {
                currentValue = it.value
            }
        }
        cells?.forEach {
            val r = it.row
            val c = it.col
            if (r == selectedRow && c == selectedCol) {
                fillCell(canvas, r, c, selectedCellPaint)
            } else if(it.value == currentValue && currentValue!="0") {
                fillCell(canvas, r, c, selectedCellPaint)
            } else if (isCellInConflictingSelection(it)) {
                fillCell(canvas, r, c, cellToWatchForPaint)
            } else fillCell(canvas, r, c, notSelectedCellPaint)
        }
    }


    private fun fillCell(canvas: Canvas, r: Int, c: Int, paint: Paint) {
        canvas.drawRect(
            c * cellSizePixels, r * cellSizePixels, (c + 1) * cellSizePixels,
            (r + 1) * cellSizePixels, paint
        )
    }

    private fun drawLines(canvas: Canvas) {
        canvas.drawRect(
            0F, 0F,
            canvas.width.toFloat(), canvas.height.toFloat(), thickLinePaint
        )

        for (i in 1 until size) {
            val paintToUse = if (i % sqrtSize == 0) {
                thickLinePaint
            } else {
                thinLinePaint
            }
            canvas.drawLine(
                i * cellSizePixels,
                0F,
                i * cellSizePixels,
                height.toFloat(),
                paintToUse
            )

            canvas.drawLine(
                0F,
                i * cellSizePixels,
                width.toFloat(),
                i * cellSizePixels,
                paintToUse
            )
        }
    }

    private fun drawText(canvas: Canvas) {
        cells?.forEach {
            val row = it.row
            val col = it.col
            var valueString = it.value
            if (valueString == "0") {
                valueString = ""
            }
            val paintToUse =  when {
                it.isFixed -> fixedTextPaint
                it.isDoubtful -> doubtfulTextPaint
                else -> notSelectedTextPaint
            }
            val textBounds = Rect()
            paintToUse.getTextBounds(valueString, 0, valueString.length, textBounds)
            val textWidth = paintToUse.measureText(valueString)
            val textHeight = textBounds.height()
            canvas.drawText(
                valueString,
                (col * cellSizePixels) + cellSizePixels / 2 - textWidth / 2,
                (row * cellSizePixels) + cellSizePixels / 2 + textHeight / 2,
                paintToUse
            )
        }
    }

    private fun isCellInConflictingSelection(cell: Cell): Boolean {
        val r = cell.row
        val c = cell.col
        return r == selectedRow || c == selectedCol ||
                (r / sqrtSize == selectedRow / sqrtSize && c / sqrtSize == selectedCol / sqrtSize)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {

        return when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                startCLickTime = Calendar.getInstance().timeInMillis
                true
            }
            MotionEvent.ACTION_UP -> {
                val touchDuration = Calendar.getInstance().timeInMillis - startCLickTime
                if (touchDuration < 200) {
                    handleTouchEvent(event.x, event.y)
                } else {
                    handleLongTouchEvent(event.x, event.y)
                }
                true
            }
            else -> false
        }
    }


    private fun handleTouchEvent(x: Float, y: Float) {
        val possibleSelectedRow = (y / cellSizePixels).toInt()
        val possibleSelectedCol = (x / cellSizePixels).toInt()
        listener?.onCellTouched(possibleSelectedRow, possibleSelectedCol)
    }

    private fun handleLongTouchEvent(x: Float, y: Float) {
        val possibleSelectedRow = (y / cellSizePixels).toInt()
        val possibleSelectedCol = (x / cellSizePixels).toInt()
        listener?.onCellLongTouched(possibleSelectedRow, possibleSelectedCol)
    }

    fun updateSelectedCellUI(row: Int, col: Int) {
        selectedRow = row
        selectedCol = col
        invalidate()
    }

    fun updateCells(cells: List<Cell>) {
        this.cells = cells
        invalidate()
    }

    fun registerListener(listener: OnTouchListener) {
        this.listener = listener
    }

    interface OnTouchListener {
        fun onCellTouched(row: Int, col: Int)
        fun onCellLongTouched(row: Int, col: Int)
    }

    companion object {
        private const val MY_FONT = R.font.ma_shan_zheng
    }
}
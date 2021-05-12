package edu.rockvalleycollege.randomchess

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener


class MainActivity : AppCompatActivity() {
    private val lastTouchXY = FloatArray(2)
    private var lastLastTouchXY = FloatArray(2)

    val database = FirebaseDatabase.getInstance("https://randomchess-8e36c-default-rtdb.firebaseio.com/")
    val ref = database.getReference("Highscore")

    var pieces = arrayOfNulls<ImageView?>(64)
    var pieceType = arrayOfNulls<String>(64)
    var highlightedMoves = arrayListOf<ImageView>()
    var highlightedSquares = arrayListOf<Array<Int>>()

    var mutations = 10
    var currentScore = 0

    var isFinished = false

    var timer = object : CountDownTimer(120000, 1000) {
        override fun onTick(millisUntilFinished: Long) {}
        override fun onFinish() {}
    }

    @RequiresApi(Build.VERSION_CODES.N)
    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val intInput = findViewById<EditText>(R.id.mutations)
        val preferences = getSharedPreferences("data", Context.MODE_PRIVATE)
        intInput.setText(preferences.getString("mutations", "10"))
        mutations = Integer.parseInt(preferences.getString("mutations", "10"))


        val savePreferences = findViewById<Button>(R.id.savePreference)
        //When you press the “save preferences” button, what we do is to save the contents of
        //the EditText in a variable called “mail” in the preferences file:
        savePreferences.setOnClickListener {
            val editor = preferences.edit()
            editor.putString("mutations", Integer.parseInt(intInput.text.toString()).toString())
            mutations = Integer.parseInt(intInput.text.toString())

            editor.commit()
            //the finish method of the AppCompatActivity class ends the current activity
            // finish()
        }

        val highScore = findViewById<TextView>(R.id.highScore)


        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                highScore.text = "Global highscore: ${snapshot.value}"
            }

            override fun onCancelled(error: DatabaseError) {
                // Failed to read value
                Log.w("Message", "Failed to read value.", error.toException())
            }
        })


        val chessBoard = findViewById<ImageView>(R.id.chessBoard)
        val start = findViewById<Button>(R.id.button)

        chessBoard.setOnTouchListener { v: View, event: MotionEvent ->
            if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                lastLastTouchXY = lastTouchXY.clone()

                lastTouchXY[0] = event.x
                lastTouchXY[1] = event.y
            }

            false
        }

        chessBoard.setOnClickListener {
            if (isFinished) return@setOnClickListener

            val x = lastTouchXY[0]
            val y = lastTouchXY[1]

            val width = chessBoard.width
            val height = chessBoard.height

            val squareWidth = width / 8
            val squareHeight = height / 8

            val xSquareClicked = (x / width * 8).toInt()
            val ySquareClicked = (y / height * 8).toInt()

            val clickedPiece = pieces[getIndex(xSquareClicked, ySquareClicked)]

            var parentLinearLayout: ConstraintLayout? = null
            parentLinearLayout = findViewById(R.id.mainLayout)

            for (move in highlightedMoves) {
                parentLinearLayout.removeView(move)
            }

            highlightedMoves.clear()

            for (move in highlightedSquares) {
                if (move[0] == xSquareClicked && move[1] == ySquareClicked) {
                    val x2 = lastLastTouchXY[0]
                    val y2 = lastLastTouchXY[1]

                    val xLastClicked = (x2 / width * 8).toInt()
                    val yLastClicked = (y2 / height * 8).toInt()

                    val clickedPiece2 = pieces[getIndex(xLastClicked, yLastClicked)]

                    if (clickedPiece2 != null) {
                        clickedPiece2.x = (chessBoard.x + (xSquareClicked * squareWidth))
                        // Offset to get images to the bottom of squares
                        clickedPiece2.y = (chessBoard.y + (ySquareClicked * squareHeight)) + 15

                        if (pieceType[getIndex(move[0], move[1])] == "king1") {
                            val score = findViewById<TextView>(R.id.currentScore)
                            score.text = "YOU WON!"

                            trySetHighScore()

                            isFinished = true
                        }

                        // Remove the captured piece from the board
                        parentLinearLayout.removeView(
                            pieces[getIndex(
                                xSquareClicked,
                                ySquareClicked
                            )]
                        )

                        pieceType[getIndex(xSquareClicked, ySquareClicked)] =
                            pieceType[getIndex(xLastClicked, yLastClicked)]
                        pieceType[getIndex(xLastClicked, yLastClicked)] = null
                        pieces[getIndex(xSquareClicked, ySquareClicked)] = clickedPiece2
                        pieces[getIndex(xLastClicked, yLastClicked)] = null

                        highlightedSquares.clear()

                        if (pieceType[getIndex(
                                xSquareClicked,
                                ySquareClicked
                            )] == "pawn" && ySquareClicked == 0
                        ) {
                            parentLinearLayout.removeView(
                                pieces[getIndex(
                                    xSquareClicked,
                                    ySquareClicked
                                )]
                            )

                            createPiece(xSquareClicked, ySquareClicked, "queen")
                        }

                        if (pieceType[getIndex(
                                xSquareClicked,
                                ySquareClicked
                            )] == "pawn1" && ySquareClicked == 8
                        ) {
                            parentLinearLayout.removeView(
                                pieces[getIndex(
                                    xSquareClicked,
                                    ySquareClicked
                                )]
                            )

                            createPiece(xSquareClicked, ySquareClicked, "queen1")
                        }

                        makeComputerMove(false, true)

                        return@setOnClickListener
                    }
                }
            }

            if (clickedPiece != null) {
                val resource = resources.getIdentifier("highlight", "drawable", packageName)

                if (pieceType[getIndex(xSquareClicked, ySquareClicked)] == null || pieceType[getIndex(xSquareClicked, ySquareClicked)]!!.endsWith("1")) return@setOnClickListener

                val possibleMoves = getPossibleMoves(xSquareClicked, ySquareClicked)

                for (move in possibleMoves) {
                    val newX = move[0]
                    val newY = move[1]

                    val layout = RelativeLayout.LayoutParams(squareWidth, squareHeight)
                    val imageView = ImageView(this)
                    imageView.layoutParams = layout
                    imageView.setImageResource(resource)

                    imageView.x = (chessBoard.x + (newX * squareWidth))
                    imageView.y = (chessBoard.y + (newY * squareHeight))

                    parentLinearLayout.addView(imageView)
                    highlightedMoves.add(imageView)
                    highlightedSquares.add(arrayOf(newX, newY))
                }
            }
        }

        start.setOnClickListener() {
            val parentLinearLayout = findViewById<ConstraintLayout?>(R.id.mainLayout)

            for (piece in pieces) {
                parentLinearLayout?.removeView(piece)
            }

            for (move in highlightedMoves) {
                parentLinearLayout?.removeView(move)
            }

            pieces = arrayOfNulls<ImageView?>(64)
            pieceType = arrayOfNulls(64)
            highlightedMoves = arrayListOf()
            highlightedSquares = arrayListOf()

            createPiece(0, 0, "rook1")
            createPiece(1, 0, "knight1")
            createPiece(2, 0, "bishop1")
            createPiece(3, 0, "queen1")
            createPiece(4, 0, "king1")
            createPiece(5, 0, "bishop1")
            createPiece(6, 0, "knight1")
            createPiece(7, 0, "rook1")

            createPiece(0, 1, "pawn1")
            createPiece(1, 1, "pawn1")
            createPiece(2, 1, "pawn1")
            createPiece(3, 1, "pawn1")
            createPiece(4, 1, "pawn1")
            createPiece(5, 1, "pawn1")
            createPiece(6, 1, "pawn1")
            createPiece(7, 1, "pawn1")

            createPiece(0, 6, "pawn")
            createPiece(1, 6, "pawn")
            createPiece(2, 6, "pawn")
            createPiece(3, 6, "pawn")
            createPiece(4, 6, "pawn")
            createPiece(5, 6, "pawn")
            createPiece(6, 6, "pawn")
            createPiece(7, 6, "pawn")

            createPiece(0, 7, "rook")
            createPiece(1, 7, "knight")
            createPiece(2, 7, "bishop")
            createPiece(3, 7, "queen")
            createPiece(4, 7, "king")
            createPiece(5, 7, "bishop")
            createPiece(6, 7, "knight")
            createPiece(7, 7, "rook")

            for (i in 1..mutations) {
                makeComputerMove(true, false)
                makeComputerMove(false, false)
            }

            timer.cancel()

            timer = object : CountDownTimer(120000, 1000) {
                override fun onFinish() {
                    val score = findViewById<TextView>(R.id.currentScore)
                    score.text = "You ran out of time!!"
                }

                override fun onTick(millisUntilFinished: Long) {
                    if (isFinished) {
                        cancel()
                        return
                    }

                    currentScore = (millisUntilFinished / 1000).toInt()

                    val score = findViewById<TextView>(R.id.currentScore)
                    score.text = "Score: ${millisUntilFinished / 1000}"
                }
            }

            timer.start()

            isFinished = false
        }


        findViewById<View>(android.R.id.content).setOnTouchListener { _, _ ->
            hideKeyboard()
            false
        }
    }


    fun trySetHighScore() {
        ref.setValue(currentScore)
    }


    @RequiresApi(Build.VERSION_CODES.N)
    fun makeComputerMove(isWhite: Boolean, allowKingCapture : Boolean) {
        if (isFinished) return

        val xValues = IntRange(0, 7).toMutableList()
        val yValues = IntRange(0, 7).toMutableList()

        xValues.shuffle()
        yValues.shuffle()

        for (take in 0..1) {
            for (x in xValues) {
                for (y in yValues) {
                    val selectedType = pieceType[getIndex(x, y)]
                    if (selectedType != null) {
                        if (selectedType.endsWith("1") != isWhite) {
                            val moves = getPossibleMoves(x, y)
                            moves.shuffle()

                            for (move in moves) {
                                val clickedPiece2 = pieces[getIndex(x, y)] ?: continue

                                val chessBoard = findViewById<ImageView>(R.id.chessBoard)
                                var parentLinearLayout: ConstraintLayout? = null
                                parentLinearLayout = findViewById(R.id.mainLayout)

                                val width = chessBoard.width
                                val height = chessBoard.height

                                val squareWidth = width / 8
                                val squareHeight = height / 8

                                if (take == 0 && pieceType[getIndex(move[0], move[1])] == null) continue

                                // Don't allow the king to be captured before the game begins
                                if (!allowKingCapture && pieceType[getIndex(move[0], move[1])]?.contains("king") == true) {
                                    return
                                }


                                clickedPiece2.x = (chessBoard.x + (move[0] * squareWidth))
                                // Offset to get images to the bottom of squares
                                clickedPiece2.y = (chessBoard.y + (move[1] * squareHeight)) + 15

                                val capturedPiece = pieces[getIndex(move[0], move[1])]

                                if (pieceType[getIndex(move[0], move[1])] == "king") {
                                    val score = findViewById<TextView>(R.id.currentScore)
                                    score.text = "YOU LOST!"

                                    isFinished = true
                                }

                                // Remove the captured piece from the board
                                parentLinearLayout.removeView(capturedPiece)

                                pieceType[getIndex(move[0], move[1])] = pieceType[getIndex(x, y)]
                                pieceType[getIndex(x, y)] = null
                                pieces[getIndex(move[0], move[1])] = clickedPiece2
                                pieces[getIndex(x, y)] = null


                                if (pieceType[getIndex(x, y)] == "pawn" && y == 0) {
                                    parentLinearLayout.removeView(
                                        pieces[getIndex(
                                            move[0],
                                            move[1]
                                        )]
                                    )

                                    createPiece(x, y, "queen")
                                }

                                if (pieceType[getIndex(
                                        move[0],
                                        move[1]
                                    )] == "pawn1" && move[1] == 8
                                ) {
                                    parentLinearLayout.removeView(
                                        pieces[getIndex(
                                            move[0],
                                            move[1]
                                        )]
                                    )

                                    createPiece(x, y, "queen1")
                                }

                                return
                            }
                        }
                    }
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    fun getPossibleMoves(x: Int, y: Int): ArrayList<Array<Int>> {
        val moves = arrayListOf<Array<Int>>()
        val piece = pieceType[getIndex(x, y)]

        when (piece) {
            "pawn" -> {
                if (y == 6) {
                    if (pieceType[getIndex(x, y - 1)] == null) {
                        moves.add(arrayOf(x, y - 1))
                        if (pieceType[getIndex(x, y - 2)] == null)
                            moves.add(arrayOf(x, y - 2))
                    }
                } else {
                    if (pieceType[getIndex(x, y - 1)] == null)
                        moves.add(arrayOf(x, y - 1))
                }

                if (inBounds(x - 1, y - 1) && pieceType[getIndex(x - 1, y - 1)] != null)
                    moves.add(arrayOf(x - 1, y - 1))

                if (inBounds(x + 1, y - 1) && pieceType[getIndex(x + 1, y - 1)] != null)
                    moves.add(arrayOf(x + 1, y - 1))
            }
            "pawn1" -> {
                if (y == 1) {
                    if (inBounds(x, y + 1) && pieceType[getIndex(x, y + 1)] == null) {
                        moves.add(arrayOf(x, y + 1))
                        if (inBounds(x, y + 2) && pieceType[getIndex(x, y + 2)] == null)
                            moves.add(arrayOf(x, y + 2))
                    }

                } else {
                    if (inBounds(x, y + 1) && pieceType[getIndex(x, y + 1)] == null)
                        moves.add(arrayOf(x, y + 1))
                }

                if (inBounds(x - 1, y + 1) && pieceType[getIndex(x - 1, y + 1)] != null)
                    moves.add(arrayOf(x - 1, y + 1))

                if (inBounds(x + 1, y + 1) && pieceType[getIndex(x + 1, y + 1)] != null)
                    moves.add(arrayOf(x + 1, y + 1))
            }

            "rook", "rook1" -> {
                var tempX = x
                while (inBounds(++tempX, y) && pieces[getIndex(tempX, y)] == null) {
                    moves.add(arrayOf(tempX, y))
                }

                if (inBounds(tempX, y)) {
                    val last = pieceType[getIndex(tempX, y)]
                    if (last != null && last.endsWith('1') != piece.endsWith('1')) {
                        moves.add(arrayOf(tempX, y))
                    }
                }

                tempX = x
                while (inBounds(--tempX, y) && pieces[getIndex(tempX, y)] == null) {
                    moves.add(arrayOf(tempX, y))
                }

                if (inBounds(tempX, y)) {
                    val last = pieceType[getIndex(tempX, y)]
                    if (last != null && last.endsWith('1') != piece.endsWith('1')) {
                        moves.add(arrayOf(tempX, y))
                    }
                }

                var tempY = y
                while (inBounds(x, ++tempY) && pieces[getIndex(x, tempY)] == null) {
                    moves.add(arrayOf(x, tempY))
                }

                if (inBounds(x, tempY)) {
                    val last = pieceType[getIndex(x, tempY)]
                    if (last != null && last.endsWith('1') != piece.endsWith('1')) {
                        moves.add(arrayOf(x, tempY))
                    }
                }

                tempY = y
                while (inBounds(x, --tempY) && pieces[getIndex(x, tempY)] == null) {
                    moves.add(arrayOf(x, tempY))
                }

                if (inBounds(x, tempY)) {
                    val last = pieceType[getIndex(x, tempY)]
                    if (last != null && last.endsWith('1') != piece.endsWith('1')) {
                        moves.add(arrayOf(x, tempY))
                    }
                }
            }

            "knight", "knight1" -> {
                moves.add(arrayOf(x + 1, y + 2))
                moves.add(arrayOf(x + 2, y + 1))
                moves.add(arrayOf(x + 2, y - 1))
                moves.add(arrayOf(x + 1, y - 2))
                moves.add(arrayOf(x - 1, y - 2))
                moves.add(arrayOf(x - 2, y - 1))
                moves.add(arrayOf(x - 2, y + 1))
                moves.add(arrayOf(x - 1, y + 2))
            }

            "bishop", "bishop1" -> {
                var tempX = x
                var tempY = y
                while (inBounds(++tempX, ++tempY) && pieces[getIndex(tempX, tempY)] == null) {
                    moves.add(arrayOf(tempX, tempY))
                }

                if (inBounds(tempX, tempY)) {
                    val last = pieceType[getIndex(tempX, tempY)]
                    if (last != null && last.endsWith('1') != piece.endsWith('1')) {
                        moves.add(arrayOf(tempX, tempY))
                    }
                }

                tempX = x
                tempY = y
                while (inBounds(++tempX, --tempY) && pieces[getIndex(tempX, tempY)] == null) {
                    moves.add(arrayOf(tempX, tempY))
                }

                if (inBounds(tempX, tempY)) {
                    val last = pieceType[getIndex(tempX, tempY)]
                    if (last != null && last.endsWith('1') != piece.endsWith('1')) {
                        moves.add(arrayOf(tempX, tempY))
                    }
                }

                tempX = x
                tempY = y
                while (inBounds(--tempX, ++tempY) && pieces[getIndex(tempX, tempY)] == null) {
                    moves.add(arrayOf(tempX, tempY))
                }

                if (inBounds(tempX, tempY)) {
                    val last = pieceType[getIndex(tempX, tempY)]
                    if (last != null && last.endsWith('1') != piece.endsWith('1')) {
                        moves.add(arrayOf(tempX, tempY))
                    }
                }

                tempX = x
                tempY = y
                while (inBounds(--tempX, --tempY) && pieces[getIndex(tempX, tempY)] == null) {
                    moves.add(arrayOf(tempX, tempY))
                }

                if (inBounds(tempX, tempY)) {
                    val last = pieceType[getIndex(tempX, tempY)]
                    if (last != null && last.endsWith('1') != piece.endsWith('1')) {
                        moves.add(arrayOf(tempX, tempY))
                    }
                }
            }

            "queen", "queen1" -> {
                var tempX = x
                while (inBounds(++tempX, y) && pieces[getIndex(tempX, y)] == null) {
                    moves.add(arrayOf(tempX, y))
                }

                if (inBounds(tempX, y)) {
                    val last = pieceType[getIndex(tempX, y)]
                    if (last != null && last.endsWith('1') != piece.endsWith('1')) {
                        moves.add(arrayOf(tempX, y))
                    }
                }

                tempX = x
                while (inBounds(--tempX, y) && pieces[getIndex(tempX, y)] == null) {
                    moves.add(arrayOf(tempX, y))
                }

                if (inBounds(tempX, y)) {
                    val last = pieceType[getIndex(tempX, y)]
                    if (last != null && last.endsWith('1') != piece.endsWith('1')) {
                        moves.add(arrayOf(tempX, y))
                    }
                }

                var tempY = y
                while (inBounds(x, ++tempY) && pieces[getIndex(x, tempY)] == null) {
                    moves.add(arrayOf(x, tempY))
                }

                if (inBounds(x, tempY)) {
                    val last = pieceType[getIndex(x, tempY)]
                    if (last != null && last.endsWith('1') != piece.endsWith('1')) {
                        moves.add(arrayOf(x, tempY))
                    }
                }

                tempY = y
                while (inBounds(x, --tempY) && pieces[getIndex(x, tempY)] == null) {
                    moves.add(arrayOf(x, tempY))
                }

                if (inBounds(x, tempY)) {
                    val last = pieceType[getIndex(x, tempY)]
                    if (last != null && last.endsWith('1') != piece.endsWith('1')) {
                        moves.add(arrayOf(x, tempY))
                    }
                }

                tempX = x
                tempY = y
                while (inBounds(++tempX, ++tempY) && pieces[getIndex(tempX, tempY)] == null) {
                    moves.add(arrayOf(tempX, tempY))
                }

                if (inBounds(tempX, tempY)) {
                    val last = pieceType[getIndex(tempX, tempY)]
                    if (last != null && last.endsWith('1') != piece.endsWith('1')) {
                        moves.add(arrayOf(tempX, tempY))
                    }
                }

                tempX = x
                tempY = y
                while (inBounds(++tempX, --tempY) && pieces[getIndex(tempX, tempY)] == null) {
                    moves.add(arrayOf(tempX, tempY))
                }

                if (inBounds(tempX, tempY)) {
                    val last = pieceType[getIndex(tempX, tempY)]
                    if (last != null && last.endsWith('1') != piece.endsWith('1')) {
                        moves.add(arrayOf(tempX, tempY))
                    }
                }

                tempX = x
                tempY = y
                while (inBounds(--tempX, ++tempY) && pieces[getIndex(tempX, tempY)] == null) {
                    moves.add(arrayOf(tempX, tempY))
                }

                if (inBounds(tempX, tempY)) {
                    val last = pieceType[getIndex(tempX, tempY)]
                    if (last != null && last.endsWith('1') != piece.endsWith('1')) {
                        moves.add(arrayOf(tempX, tempY))
                    }
                }

                tempX = x
                tempY = y
                while (inBounds(--tempX, --tempY) && pieces[getIndex(tempX, tempY)] == null) {
                    moves.add(arrayOf(tempX, tempY))
                }

                if (inBounds(tempX, tempY)) {
                    val last = pieceType[getIndex(tempX, tempY)]
                    if (last != null && last.endsWith('1') != piece.endsWith('1')) {
                        moves.add(arrayOf(tempX, tempY))
                    }
                }
            }

            "king", "king1" -> {
                moves.add(arrayOf(x, y + 1))
                moves.add(arrayOf(x, y))
                moves.add(arrayOf(x, y - 1))

                moves.add(arrayOf(x + 1, y + 1))
                moves.add(arrayOf(x + 1, y))
                moves.add(arrayOf(x + 1, y - 1))

                moves.add(arrayOf(x - 1, y + 1))
                moves.add(arrayOf(x - 1, y))
                moves.add(arrayOf(x - 1, y - 1))
            }
        }

        moves.removeIf { arr -> !inBounds(arr[0], arr[1]) }

        val isWhite = piece != null && piece.endsWith('1')

        moves.removeIf { index ->
            val referencedPiece = pieceType[getIndex(index[0], index[1])]
            return@removeIf referencedPiece != null && referencedPiece.isNotEmpty() && (referencedPiece.endsWith(
                '1'
            ) == isWhite)
        }

        return moves
    }

    fun createPiece(x: Int, y: Int, name: String) {
        val resource = resources.getIdentifier(name, "drawable", packageName)
        val chessBoard = findViewById<ImageView>(R.id.chessBoard)
        val width = chessBoard.width
        val height = chessBoard.height
        val squareWidth = width / 8
        val squareHeight = height / 8

        // This is why I prefer explicit over implicit variable types
        // Didn't work without this hack. Wouldn't be an issue on Java.
        var parentLinearLayout: ConstraintLayout? = null
        parentLinearLayout = findViewById(R.id.mainLayout)

        val layout = RelativeLayout.LayoutParams(squareWidth - 20, squareHeight - 20)
        val imageView = ImageView(this)
        imageView.layoutParams = layout
        imageView.setImageResource(resource)
        imageView.x = (chessBoard.x + (x * squareWidth))
        // Offset to get images to the bottom of squares
        imageView.y = (chessBoard.y + (y * squareHeight)) + 15

        parentLinearLayout.addView(imageView)

        pieces[getIndex(x, y)] = imageView
        pieceType[getIndex(x, y)] = name
    }

    fun inBounds(x: Int, y: Int): Boolean {
        if (x < 0 || x > 7) return false
        if (y < 0 || y > 7) return false
        return true
    }

    fun getIndex(x: Int, y: Int): Int {
        return x * 8 + y
    }

    fun hideKeyboard() {
        try {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(currentFocus!!.windowToken, 0)
        } catch (e: Exception) {
            // TODO: handle exception
        }
    }
}
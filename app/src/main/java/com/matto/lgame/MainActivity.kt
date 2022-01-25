package com.matto.lgame

import android.media.Image
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import kotlinx.coroutines.*
import kotlin.math.abs

class MainActivity : AppCompatActivity() {

    private var gameOver = false
    private var turn = false
    //false - player1
    //true - player2
    private var phase = false
    //false - L phase
    //true - neutral tile phase
    private var restart1 = false
    private var restart2 = false
    private var selectedAmount = 0
    private var buttonMap = mutableMapOf<ImageButton,Byte>()
    private lateinit var buttonArray: Array<Array<ImageButton>>

    private lateinit var commitP1: Button
    private lateinit var commitP2: Button
    private lateinit var clearP1: Button
    private lateinit var clearP2: Button
    private lateinit var restartP1: Button
    private lateinit var textViewP1: TextView
    private lateinit var textViewP2: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val a1 = findViewById<ImageButton>(R.id.a1)
        val a2 = findViewById<ImageButton>(R.id.a2)
        val a3 = findViewById<ImageButton>(R.id.a3)
        val a4 = findViewById<ImageButton>(R.id.a4)
        val b1 = findViewById<ImageButton>(R.id.b1)
        val b2 = findViewById<ImageButton>(R.id.b2)
        val b3 = findViewById<ImageButton>(R.id.b3)
        val b4 = findViewById<ImageButton>(R.id.b4)
        val c1 = findViewById<ImageButton>(R.id.c1)
        val c2 = findViewById<ImageButton>(R.id.c2)
        val c3 = findViewById<ImageButton>(R.id.c3)
        val c4 = findViewById<ImageButton>(R.id.c4)
        val d1 = findViewById<ImageButton>(R.id.d1)
        val d2 = findViewById<ImageButton>(R.id.d2)
        val d3 = findViewById<ImageButton>(R.id.d3)
        val d4 = findViewById<ImageButton>(R.id.d4)

        commitP1 = findViewById(R.id.commitP1)
        commitP2 = findViewById(R.id.commitP2)
        clearP1 = findViewById(R.id.clearP1)
        clearP2 = findViewById(R.id.clearP2)
        restartP1 = findViewById(R.id.restartP1)
        textViewP1 = findViewById(R.id.textViewP1)
        textViewP2 = findViewById(R.id.textViewP2)

        if((0..1).random()==0){
            turn = false
            commitP2.isEnabled=false
            clearP2.isEnabled=false
            textViewP2.isEnabled=false
        } else{
            turn = true
            commitP1.isEnabled=false
            clearP1.isEnabled=false
            textViewP1.isEnabled=false
        }
        resetText()

        // 1 - player1 tile
        // 2 - player2 tile
        // 3 - neutral tile
        // 4 - empty tile
        // -(number) - selected tile
        
        val status = arrayOf(
            arrayOf<Byte>(3,2,2,4),
            arrayOf<Byte>(4,1,2,4),
            arrayOf<Byte>(4,1,2,4),
            arrayOf<Byte>(4,1,1,3)
        )

        buttonArray = arrayOf(
            arrayOf(a1,a2,a3,a4),
            arrayOf(b1,b2,b3,b4),
            arrayOf(c1,c2,c3,c4),
            arrayOf(d1,d2,d3,d4)
        )

        for(i in 3 downTo 0)
            for(a in 3 downTo 0)
                buttonMap[buttonArray[i][a]] = status[i][a]
    }

    private fun getPosition(imageButton: ImageButton):Pair<Int,Int>{
        for(i in 3 downTo 0)
            for(a in 3 downTo 0)
                if(imageButton == buttonArray[i][a])
                    return Pair(i,a)
        return Pair(-1,-1)
    }

    private fun resetText(){
        if(gameOver){
            if(turn) {
                textViewP2.text = getString(R.string.lost)
                textViewP1.text = getString(R.string.won)
            }
            else {
                textViewP2.text = getString(R.string.won)
                textViewP1.text = getString(R.string.lost)
            }
            textViewP1.isClickable=false
            textViewP2.isClickable=false
            clearP1.isClickable=false
            clearP2.isClickable=false
            commitP1.isClickable=false
            commitP2.isClickable=false
        } else when {
            restart1 -> textViewP1.text = getString(R.string.restart_vote)
            restart2 -> textViewP2.text = getString(R.string.restart_vote)
            else -> {
                if (turn) {
                    if (phase)
                        textViewP2.text = getString(R.string.move_neutral)
                    else
                        textViewP2.text = getString(R.string.move_shape)
                    textViewP1.text = getString(R.string.other_turn)
                } else {
                    if (phase)
                        textViewP1.text = getString(R.string.move_neutral)
                    else
                        textViewP1.text = getString(R.string.move_shape)
                    textViewP2.text = getString(R.string.other_turn)
                }
            }
        }
    }

    private fun changeTurn(){
        turn=(!turn)
        if(!turn){
            commitP2.isEnabled=false
            clearP2.isEnabled=false
            commitP1.isEnabled=true
            clearP1.isEnabled=true
            textViewP1.isEnabled=true
            textViewP2.isEnabled=false
        } else{
            commitP1.isEnabled=false
            clearP1.isEnabled=false
            commitP2.isEnabled=true
            clearP2.isEnabled=true
            textViewP2.isEnabled=true
            textViewP1.isEnabled=false
        }
        gameOver = isOver()
    }

    private suspend fun setText(player: Boolean, time: Long, text: String){
        val textview = when(player){
            true -> textViewP1
            false -> textViewP2
        }
        textview.text = text
        for(i in time downTo 0L step time/10){
            if(!textview.text.equals(text)) return
            delay(time/10)
        }
        resetText()
    }

    fun textViewClick(view: android.view.View){
        GlobalScope.launch(Dispatchers.Main) {
            val x = launch {
                setText(view as TextView == textViewP1, 2500L,getString(R.string.not_button))
            }
            x.join()
        }
    }

    fun selectedTile(view: android.view.View){
        val imageButton = view as ImageButton
        var value = 0.toByte()
        buttonMap[imageButton]?.let{value = it}

        if(!phase){
            if((turn&&value%2==0)||(!turn&&(value%4==0||value==1.toByte()||value==(-1).toByte()))) {
                if(buttonMap[imageButton]!!>0) {
                    if(selectedAmount<4) {
                        imageButton.background.setTint(resources.getColor(R.color.selected))
                        selectedAmount += 1
                        buttonMap[imageButton] = value.times(-1).toByte()
                    }
                    else{
                        if(turn) textViewP2.text=getString(R.string.no_more_4)
                        else textViewP1.text=getString(R.string.no_more_4)
                        GlobalScope.launch(Dispatchers.Main) {
                            val x = launch {  setText(turn,3000L, getString(R.string.no_more_4)) }
                            x.join()
                        }
                    }
                }
                else {
                    imageButton.background.setTint(resources.getColor(R.color.white))
                    selectedAmount-=1
                    buttonMap[imageButton] = value.times(-1).toByte()
                }
            }
        } else{
            if(value%4==0||value%3==0){
                if(buttonMap[imageButton]!!>0){
                    if(selectedAmount<2){
                        imageButton.background.setTint(resources.getColor(R.color.selected))
                        selectedAmount += 1
                        buttonMap[imageButton] = value.times(-1).toByte()
                    }
                    else{
                        if(turn) textViewP2.text=getString(R.string.no_more_2)
                        else textViewP1.text=getString(R.string.no_more_2)
                        GlobalScope.launch(Dispatchers.Main) {
                            val x = launch {  setText(turn,3000L,getString(R.string.no_more_2)) }
                            x.join()
                        }
                    }
                }
                else{
                    imageButton.background.setTint(resources.getColor(R.color.white))
                    selectedAmount-=1
                    buttonMap[imageButton] = value.times(-1).toByte()
                }
            }
        }
    }

    private fun byteAbs(b:Byte):Byte{
        return  if(b>0) b
                else b.times(-1).toByte()
    }

    private fun makeText(text:String){
        if(turn) textViewP2.text=text
        else textViewP1.text=text
        GlobalScope.launch(Dispatchers.Main) {
            val x = launch {  setText(turn,3000L,text) }
            x.join()
        }
    }

    fun clear(view: android.view.View) {
        buttonMap.forEach { (i, b) ->
            buttonMap[i] = byteAbs(b)
            i.background.setTint(resources.getColor(R.color.white))
            selectedAmount = 0
        }
    }

    fun commit(view: android.view.View){
        if(validMove()) {
            if (!phase && view.id == R.id.commitP1) {
                buttonMap.forEach { (i, b) ->
                    i.background.setTint(resources.getColor(R.color.white))
                    if(b.toInt() == 1){
                        i.setColorFilter(resources.getColor(R.color.white))
                        buttonMap[i] = 4
                    }
                    if (b.toInt() == -1 || b.toInt() == -4) {
                        buttonMap[i] = 1
                        i.setColorFilter(resources.getColor(R.color.red))
                    }
                }
            } else if(!phase){
                buttonMap.forEach { (i, b) ->
                    i.background.setTint(resources.getColor(R.color.white))
                    if(b.toInt() == 2) {
                        i.setColorFilter(resources.getColor(R.color.white))
                        buttonMap[i] = 4
                    }
                    if (b.toInt() == -2 || b.toInt() == -4) {
                        buttonMap[i] = 2
                        i.setColorFilter(resources.getColor(R.color.blue))
                    }
                }
            }
            selectedAmount=0
            if(phase) changeTurn()
            phase = !phase
            resetText()
        }
    }

    fun restart(view: android.view.View){
        if(view as Button == restartP1) restart1 = !restart1
        else restart2 = !restart2
        if(restart1 && restart2){
            val intent = intent
            finish()
            startActivity(intent)
        }
        resetText()
    }

    private fun validMove():Boolean{
        if(!phase) {
            val currentPosition = mutableListOf<Pair<Int,Int>>()
            val iList = mutableListOf<Int>()
            val sList = mutableListOf<Int>()
            buttonMap.forEach { (i, b) ->
                if (b < 0) {
                    iList.add(getPosition(i).first)
                    sList.add(getPosition(i).second)
                }
                if(!turn&& b.toInt() in listOf(1,-1)){
                    currentPosition.add(getPosition(i))
                }
                else if (turn && b.toInt() in listOf(2,-2)){
                    currentPosition.add(getPosition(i))
                }
            }
            val iListMost = findMostAppearances(iList)
            val sListMost = findMostAppearances(sList)
            Log.v("aeou", "$iListMost $sListMost")
            if (iListMost == -1 && sListMost == -1) {
                makeText(getString(R.string.not_the_shape))
                return false
            }
            val spareIndex: Int
            if (iListMost != -1) {
                spareIndex = iList.indexOf(iList.find { it != iListMost })
                if ((sList[spareIndex] in
                    listOf(
                        sList[iList.indexOf(iList.find { it == iListMost })],
                        sList[iList.lastIndexOf(iList.find { it == iListMost })])&&
                    abs(iList[spareIndex]-iListMost)==1
                    )
                ){ val c = currentPosition.contains(Pair(iList[0],sList[0]))&&
                        currentPosition.contains(Pair(iList[1],sList[1]))&&
                        currentPosition.contains(Pair(iList[2],sList[2]))&&
                        currentPosition.contains(Pair(iList[3],sList[3]))
                    if(c) makeText(getString(R.string.tiles_not_changed))
                    currentPosition.clear()
                    return !c}
            } else if (sListMost != -1) {
                Log.v("aeou", "$iList $sList")
                spareIndex = sList.indexOf(sList.find { it != sListMost })
                if ((iList[spareIndex] in
                    listOf(
                        iList[sList.indexOf(sList.find { it == sListMost })],
                        iList[sList.lastIndexOf(sList.find { it == sListMost })])&&
                    abs(sList[spareIndex]-sListMost)==1
                )
                ){ val c = currentPosition.contains(Pair(iList[0],sList[0]))&&
                        currentPosition.contains(Pair(iList[1],sList[1]))&&
                        currentPosition.contains(Pair(iList[2],sList[2]))&&
                        currentPosition.contains(Pair(iList[3],sList[3]))
                    if(c) makeText(getString(R.string.tiles_not_changed))
                    currentPosition.clear()
                    return !c}
            }
            makeText(getString(R.string.not_the_shape))
            return false
        }
        else{
            var newNeutral: ImageButton? = null
            var oldNeutral: ImageButton? = null
            buttonMap.forEach { (i, b) ->
                if(b.toInt()==-4){
                    if(newNeutral!=null){
                        makeText(getString(R.string.select_neutral))
                        return false
                    }
                    newNeutral = i
                }
                if(b.toInt()==-3){
                    if(oldNeutral!=null){
                        makeText(getString(R.string.select_dst_neutral))
                        return false
                    }
                    oldNeutral = i
                }
            }
            if(newNeutral==null&&oldNeutral==null){
                makeText(getString(R.string.select_whole_neutral))
                return false
            }
            else if(newNeutral==null){
                makeText(getString(R.string.select_neutral))
                return false
            }
            else if(oldNeutral==null){
                makeText(getString(R.string.select_dst_neutral))
                return false
            }
            buttonMap.forEach { (i, _) ->
                if(i==newNeutral){
                    buttonMap[i]=3
                    i.setColorFilter(resources.getColor(R.color.black))
                    i.background.setTint(resources.getColor(R.color.white))
                } else if (i==oldNeutral){
                    buttonMap[i]=4
                    i.setColorFilter(resources.getColor(R.color.white))
                    i.background.setTint(resources.getColor(R.color.white))
                }
            }
            return true
        }
    }

    private fun findMostAppearances(list:MutableList<Int>):Int{
        var max = list.count { i -> i==0 }
        var index = 0
        for(x in 3 downTo 1){
            val a = list.count { i -> i==x }
            if(a > max){
                max = a
                index = x
            }
        }
        if(max!=3) return -1
        return index
    }

    //true - vertical
    //false - horizontal
    //true => [0-3][i]
    //false => [i][0-3]

    private fun isOver(): Boolean{
        for(i in 3 downTo 0){
            val resultAvailableFalse = availableTiles(false,i)
            val resultAvailableTrue = availableTiles(true,i)
            Log.v("TAG false, $i", "${resultAvailableFalse.first.size} ${resultAvailableFalse.second>=3} ${isOneTileAvailable(false,i,resultAvailableFalse.first)}")
            Log.v("TAG true, $i", "${resultAvailableTrue.first.size} ${resultAvailableTrue.second>=3} ${isOneTileAvailable(true,i,resultAvailableTrue.first)}")
            if(resultAvailableFalse.second>=3 && isOneTileAvailable(false,i,resultAvailableFalse.first)) return false
            if(resultAvailableTrue.second>=3 && isOneTileAvailable(true,i,resultAvailableTrue.first)) return false
        }
        return true
    }

    private fun availableTiles(orientation: Boolean, line: Int): Pair<List<ImageButton>,Int>{
        var count = 0
        val list = mutableListOf<ImageButton>()
        var x = line
        var y: Int
        for(i in 3 downTo 0){
            if(orientation){
                x = i
                y = line
            } else y = i
            if(buttonMap[buttonArray[x][y]] == turn+1) {
                count++
                list.add(buttonArray[x][y])
            }
            else if(buttonMap[buttonArray[x][y]] == 4.toByte()) count++
            else{
                if(count>=3) return Pair<List<ImageButton>,Int>(list,count)
                list.clear()
                count = 0
            }
        }
        return Pair<List<ImageButton>,Int>(list,count)
    }

    private fun isOneTileAvailable(orientation: Boolean, line: Int, list: List<ImageButton>): Boolean{
        if(orientation) {
            if (buttonMap[buttonArray[0][line]] in listOf(turn + 1, 4)) {
                if (checkBorderingTiles(orientation, 0, line, list)) return true
                when(buttonMap[buttonArray[3][line]] in listOf(turn + 1, 4)){
                    true -> if (checkBorderingTiles(orientation, 3, line, list)) return true
                    false -> if (checkBorderingTiles(orientation, 2, line, list)) return true
                }
            } else if(buttonMap[buttonArray[1][line]] in listOf(turn + 1, 4)){
                if (checkBorderingTiles(orientation, 1, line, list)) return true
                if (checkBorderingTiles(orientation, 3, line, list)) return true
            }
        } else{
            if (buttonMap[buttonArray[line][0]] in listOf(turn + 1, 4)) {
                if (checkBorderingTiles(orientation, line,0, list)) return true
                when(buttonMap[buttonArray[line][3]] in listOf(turn + 1, 4)){
                    true -> if (checkBorderingTiles(orientation, line, 3, list)) return true
                    false -> if (checkBorderingTiles(orientation, line, 2, list)) return true
                }
            } else if(buttonMap[buttonArray[line][1]] in listOf(turn + 1, 4)){
                if (checkBorderingTiles(orientation, line, 1, list)) return true
                if (checkBorderingTiles(orientation, line, 3, list)) return true
            }
        }
        return false
    }

    private fun checkBorderingTiles(orientation: Boolean, x: Int, y:Int, list: List<ImageButton>): Boolean{
        if(list.size<3){
            Log.v("TAG", "size==3")
            if(!orientation){
                Log.v("TAG", "false")
                if(x>0 && buttonMap[buttonArray[x-1][y]] == turn+1) return true
                if(x<3 && buttonMap[buttonArray[x+1][y]] == turn+1) return true
            }
            else {
                Log.v("TAG", "true")
                if (y > 0 && buttonMap[buttonArray[x][y - 1]] == turn+1) return true
                if (y < 3 && buttonMap[buttonArray[x][y + 1]] == turn+1) return true
            }
        }
        if(!orientation){
            if(x>0 && buttonMap[buttonArray[x-1][y]] == 4.toByte()) return true
            if(x<3 && buttonMap[buttonArray[x+1][y]] == 4.toByte()) return true
        }
        else {
            if (y > 0 && buttonMap[buttonArray[x][y - 1]] == 4.toByte()) return true
            if (y < 3 && buttonMap[buttonArray[x][y + 1]] == 4.toByte()) return true
        }

        return false
    }
}

private operator fun Boolean.plus(i: Int): Byte {
    if(this) return (i+1).toByte()
    return i.toByte()
}

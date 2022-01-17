package com.example.gpssportmap

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class SessionsActivity: AppCompatActivity() {
    private lateinit var repository: MapRepository
    private lateinit var recyclerViewLeaderBoardData: RecyclerView
    private lateinit var adapter: SessionsRecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sessions)

        repository = MapRepository(applicationContext)
            .open()
        recyclerViewLeaderBoardData = findViewById(R.id.recyclerViewSessions)

        recyclerViewLeaderBoardData.layoutManager = LinearLayoutManager(this)
        adapter = SessionsRecyclerView(this, repository)
        recyclerViewLeaderBoardData.adapter = adapter
    }

    override fun onDestroy() {
        super.onDestroy()
        repository.close()
    }
}
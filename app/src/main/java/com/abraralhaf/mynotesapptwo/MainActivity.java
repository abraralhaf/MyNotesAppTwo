package com.abraralhaf.mynotesapptwo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.view.View;
import android.widget.ProgressBar;

import com.abraralhaf.mynotesapptwo.adapter.NoteAdapter;
import com.abraralhaf.mynotesapptwo.db.NoteHelper;
import com.abraralhaf.mynotesapptwo.entity.Note;
import com.abraralhaf.mynotesapptwo.helper.MappingHelper;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements LoadNotesCallback {
    private NoteHelper noteHelper;
    private ProgressBar progressBar;
    private RecyclerView rvNotes;
    private NoteAdapter adapter;
    private FloatingActionButton fabAdd;
    private static final String EXTRA_STATE = "extra_state";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (getSupportActionBar() != null)
            getSupportActionBar().setTitle("Notes");
        noteHelper = NoteHelper.getInstance(getApplicationContext());
        noteHelper.open();
        progressBar = findViewById(R.id.progressbar);
        rvNotes = findViewById(R.id.rv_notes);
        rvNotes.setLayoutManager(new LinearLayoutManager(this));
        rvNotes.setHasFixedSize(true);
        adapter = new NoteAdapter(this);
        rvNotes.setAdapter(adapter);
        fabAdd = findViewById(R.id.fab_add);

        fabAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, NoteAddUpdateActivity.class);
                startActivityForResult(intent, NoteAddUpdateActivity.REQUEST_ADD);
            }
        });
//proses ambil data sayang
        if (savedInstanceState == null) {
            //proses ambil data beb
            new LoadNotesAsync(noteHelper, this).execute();
        }else{
            ArrayList<Note> list = savedInstanceState.getParcelableArrayList(EXTRA_STATE);
            if (list != null) {
                adapter.setListNotes(list);
            }
        }


    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState){
        super.onSaveInstanceState(outState);
        outState.putParcelableArrayList(EXTRA_STATE, adapter.getListNotes());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data != null) {
            //akan dipanggil jika request codenya ADD
            if (requestCode == NoteAddUpdateActivity.REQUEST_ADD) {
                if (resultCode == NoteAddUpdateActivity.RESULT_ADD) {
                    Note note = data.getParcelableExtra(NoteAddUpdateActivity.EXTRA_NOTE);
                    adapter.addItem(note);
                    rvNotes.smoothScrollToPosition(adapter.getItemCount() - 1);
                    showSnackbarMessage("Satu item berhasil ditambahkan");
                }
            }
            //update dan delete memiliki request code sama kaan tetapi result codenya berbeda
            else if (resultCode == NoteAddUpdateActivity.RESULT_DELETE) {
                int position = data.getIntExtra(NoteAddUpdateActivity.EXTRA_POSITION, 0);
                adapter.removeItem(position);
                showSnackbarMessage("Satu item berhasil dihapus");
            }
        }
    }

    private void showSnackbarMessage(String message) {
        Snackbar.make(rvNotes, message, Snackbar.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        noteHelper.close();
    }

    @Override
    public void preExecute() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                progressBar.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    public void postExecute(ArrayList<Note> notes) {
        progressBar.setVisibility(View.INVISIBLE);
        if (notes.size() > 0) {
            adapter.setListNotes(notes);
        } else {
            adapter.setListNotes(new ArrayList<Note>());
            showSnackbarMessage("Tidak ada data saat ini");
        }
    }


    private static class LoadNotesAsync extends AsyncTask<Void, Void, ArrayList<Note>> {
        private final WeakReference<NoteHelper> weakNoteHelper;
        private final WeakReference<LoadNotesCallback> weakCallback;

        public LoadNotesAsync(NoteHelper noteHelper, LoadNotesCallback callback) {
            weakNoteHelper = new WeakReference<>(noteHelper);
            weakCallback = new WeakReference<>(callback);

        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            weakCallback.get().preExecute();
        }

        @Override
        protected ArrayList<Note> doInBackground(Void... voids) {
            Cursor dataCursor = weakNoteHelper.get().queryAll();
            return MappingHelper.mapCursorToArrayList(dataCursor);
        }

        @Override
        protected void onPostExecute(ArrayList<Note> notes) {
            super.onPostExecute(notes);
            weakCallback.get().postExecute(notes);

        }
    }
}

interface LoadNotesCallback {
    void preExecute();

    void postExecute(ArrayList<Note> notes);
}

package com.example.newsreader1;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    ArrayList<String> titles = new ArrayList<>();
    ArrayList<String> url = new ArrayList<>();

    ArrayAdapter arrayAdapter;
    SQLiteDatabase articlesDB;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        articlesDB = this.openOrCreateDatabase("Articles",MODE_PRIVATE,null);

        articlesDB.execSQL("CREATE TABLE IF NOT EXISTS articles (id INTEGER PRIMARY KEY, articleID INTEGER,title VARCHAR, url URL)");


        download task = new download();
        try{
            task.execute("https://hacker-news.firebaseio.com/v0/beststories.json?print=pretty");
        }catch (Exception e){
            e.printStackTrace();
        }

        ListView listView = findViewById(R.id.listView);
        arrayAdapter = new ArrayAdapter(this,android.R.layout.simple_list_item_1,titles);

        listView.setAdapter(arrayAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//                Toast.makeText(MainActivity.this, url.get(position), Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(getApplicationContext(),articleActivity.class);
                intent.putExtra("url",url.get(position));
                startActivity(intent);
            }
        });


        updateListView();



    }

    public void updateListView() {
        Cursor C = articlesDB.rawQuery("SELECT * FROM articles",null);
        int urlIndex = C.getColumnIndex("url");
        int titleIndex = C.getColumnIndex("title");

        if(C.moveToFirst()) {
            titles.clear();
            url.clear();
        }

        try{
            do{
                titles.add(C.getString(titleIndex));
                url.add(C.getString(urlIndex));
            } while(C.moveToNext());
        }
        catch (Exception e){
            updateListView();
            e.printStackTrace();
        }


        arrayAdapter.notifyDataSetChanged();

    }


        public class download extends AsyncTask<String, Void,String> {

        @Override
        protected String doInBackground(String... urls) {

            String result = "";
            URL url;
            HttpURLConnection urlConnection = null;

            try {
                url = new URL(urls[0]);

                urlConnection = (HttpURLConnection) url.openConnection();
                InputStream inputStream= urlConnection.getInputStream();

                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                int data = inputStreamReader.read();
                while(data!=-1){
                    char current = (char) data ;
                    result += current;
                    data = inputStreamReader.read();
                }

                JSONArray jsonArray = new JSONArray(result);

                int numberOfItems = 20;

                if(jsonArray.length()<numberOfItems){
                    numberOfItems = jsonArray.length();
                }

                articlesDB.execSQL("DELETE FROM articles");
                for (int i=0;i<numberOfItems;i++) {

                    String articleId = jsonArray.getString(i);
                    url = new URL("https://hacker-news.firebaseio.com/v0/item/" + articleId + ".json?print=pretty");
                    urlConnection = (HttpURLConnection) url.openConnection();

                    inputStream= urlConnection.getInputStream();

                    inputStreamReader = new InputStreamReader(inputStream);

                    String articleInfo = "";
                    data = inputStreamReader.read();
                    while(data!=-1){
                        char current = (char) data ;
                        articleInfo += current;
                        data = inputStreamReader.read();
                    }
//                    Log.i("Article",articleInfo);
                    JSONObject jsonObject = new JSONObject(articleInfo);

                    if(!jsonObject.isNull("title") && !jsonObject.isNull("url")) {
                        String articleTitle = jsonObject.getString("title");
                        String articleUrl = jsonObject.getString("url");

                        String sql = "INSERT INTO articles (articleID,title,url) VALUES (?,?,?)";
                        SQLiteStatement statement = articlesDB.compileStatement(sql);
                        statement.bindString(1,articleId);
                        statement.bindString(2,articleTitle);
                        statement.bindString(3,articleUrl);

                        statement.execute();
                    }

                    }

                return result;

            } catch (Exception e) {
                e.printStackTrace();
            }

            return null;
        }
    }
}

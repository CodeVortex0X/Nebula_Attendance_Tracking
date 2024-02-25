package com.example.nebula_attendance_tracking;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

public class MainActivity extends AppCompatActivity {
    DatabaseReference reference;
    EditText institute_id_edit_text;
    ImageButton next_image_btn;
    public static final String SHARED_PREFS = "shared_prefs";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        institute_id_edit_text = findViewById(R.id.Institute_id);
        next_image_btn = findViewById(R.id.Next_button);
        //to check whether user is already login or not
        checkbox();
        // Next button clicked
        next_image_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String Institute_id = institute_id_edit_text.getText().toString();
                if (Institute_id.equals("GGSP0369")){
                    Intent intent = new Intent(MainActivity.this,On_Boarding_2.class);
                    intent.putExtra("MainActivity_Institute_id",Institute_id);
                    startActivity(intent);
                    finish();
                }
                else{
                    Toast.makeText(MainActivity.this, "Invalid institute id.", Toast.LENGTH_SHORT).show();
                }

            }
        });

    }
    private void checkbox() {
        SharedPreferences sharedPref = getSharedPreferences(SHARED_PREFS,MODE_PRIVATE);
        boolean remember_me = sharedPref.getBoolean("Remember me",false);

        if (remember_me){
            Intent intent = new Intent(MainActivity.this, Nebula_dashboard.class);
            startActivity(intent);
            finish();
        }

    }
}